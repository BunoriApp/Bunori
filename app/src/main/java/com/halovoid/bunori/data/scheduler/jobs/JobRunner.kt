package com.halovoid.bunori.data.scheduler.jobs

import com.halovoid.bunori.api.core.crawler.CrawlerFactory
import com.halovoid.bunori.data.config.SchedulerConfig
import com.halovoid.bunori.data.db.dao.BatchDao
import com.halovoid.bunori.data.db.dao.TaskDao
import com.halovoid.bunori.data.db.entities.RequestStatus
import com.halovoid.bunori.data.db.entities.TaskEntity
import com.halovoid.bunori.data.handlers.utility.parsedMetadata
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

class JobRunner(
    private val batchDao: BatchDao,
    private val taskDao: TaskDao,
    private val handlerRegistry: JobHandlerRegistry,
    private val retryPolicy: RetryPolicy,
    private val config: SchedulerConfig
) {
    companion object {
        private const val DEFAULT_MAX_ATTEMPTS = 3
    }

    suspend fun run(task: TaskEntity, onComplete: suspend () -> Unit) {
        var currentTask = task
        try {
            val preClaim = taskDao.getTaskById(currentTask.id)
            if (preClaim == null || preClaim.status == RequestStatus.CANCELLED || preClaim.status == RequestStatus.PAUSED) {
                return
            }

            taskDao.updateStatus(currentTask.id, RequestStatus.RUNNING)
            batchDao.updateStatus(currentTask.batchId, RequestStatus.RUNNING)

            val handler = handlerRegistry.getHandler(currentTask.type)
            if (handler == null) {
                fail(currentTask, "No handler found for ${currentTask.type}", currentTask.attemptCount)
                return
            }

            val maxAttempts = maxAttemptsFor(currentTask)

            while (true) {
                val result = handler.handle(currentTask)

                val latest = taskDao.getTaskById(currentTask.id) ?: currentTask
                if (latest.status == RequestStatus.CANCELLED || latest.status == RequestStatus.PAUSED) {
                    return
                }

                when (result) {
                    is JobResult.Success -> {
                        markSuccess(latest)
                        return
                    }

                    is JobResult.Cancelled -> {
                        markCancelled(latest)
                        return
                    }

                    is JobResult.Blocked -> {
                        markBlocked(latest)
                        return
                    }

                    is JobResult.Failure -> {
                        val attemptsSoFar = latest.attemptCount + 1
                        val canRetry = result.isRecoverable && attemptsSoFar < maxAttempts

                        if (!canRetry) {
                            fail(latest, result.error.message ?: "Execution Failed", attemptsSoFar)
                            return
                        }

                        taskDao.markRetrying(latest.id, attemptsSoFar, result.error.message)
                        val delayMs = retryPolicy.getNextDelay(attemptsSoFar)
                        delay(delayMs.milliseconds)

                        val postDelay = taskDao.getTaskById(currentTask.id)
                        if (postDelay == null || postDelay.status == RequestStatus.CANCELLED || postDelay.status == RequestStatus.PAUSED) {
                            return
                        }
                        currentTask = postDelay
                    }
                }
            }
        } catch (e: CancellationException) {
            runCatching {
                val latestTask = taskDao.getTaskById(task.id)
                val batch = batchDao.getBatchById(task.batchId)

                if (batch?.status == RequestStatus.PAUSED || latestTask?.status == RequestStatus.PAUSED) {
                    taskDao.updateStatus(task.id, RequestStatus.PAUSED)
                } else if (batch?.status == RequestStatus.CANCELLED || latestTask?.status == RequestStatus.CANCELLED) {
                    taskDao.updateStatus(task.id, RequestStatus.CANCELLED)
                } else {
                    taskDao.updateStatus(task.id, RequestStatus.PENDING)
                }
                syncBatchCompletion(task.batchId)
            }
            throw e
        } catch (e: Exception) {
            fail(task, e.message ?: "Unexpected error during execution", currentTask.attemptCount + 1)
        } finally {
            onComplete()
        }
    }

    private fun maxAttemptsFor(task: TaskEntity): Int {
        val crawlerName = task.parsedMetadata.crawlerName
        val crawlerMax = crawlerName?.let { CrawlerFactory.getCrawler(it)?.config?.maxAttempts }
        return crawlerMax ?: task.maxAttempts.takeIf { it > 0 } ?: DEFAULT_MAX_ATTEMPTS
    }

    private suspend fun markSuccess(task: TaskEntity) {
        taskDao.markSuccess(task.id)
        syncBatchCompletion(task.batchId)
    }

    private suspend fun fail(task: TaskEntity, errorMessage: String, attempts: Int) {
        taskDao.markFailed(task.id, errorMessage, attempts)
        syncBatchCompletion(task.batchId)
    }

    private suspend fun markCancelled(task: TaskEntity) {
        taskDao.updateStatus(task.id, RequestStatus.CANCELLED)
        syncBatchCompletion(task.batchId)
    }

    private suspend fun markBlocked(task: TaskEntity) {
        taskDao.updateStatus(task.id, RequestStatus.BLOCKED)
        batchDao.updateStatus(task.batchId, RequestStatus.BLOCKED)
    }

    private suspend fun syncBatchCompletion(batchId: String) {
        val tasks = taskDao.getTasksByBatchId(batchId)
        if (tasks.isEmpty()) return

        val batch = batchDao.getBatchById(batchId) ?: return
        if (batch.status == RequestStatus.CANCELLED || batch.status == RequestStatus.PAUSED) {
            return
        }

        val allCompleted = tasks.all { 
            it.status == RequestStatus.SUCCESS || 
            it.status == RequestStatus.FAILED || 
            it.status == RequestStatus.CANCELLED 
        }

        if (allCompleted) {
            val hasFailed = tasks.any { it.status == RequestStatus.FAILED }
            val allCancelled = tasks.all { it.status == RequestStatus.CANCELLED }

            val finalStatus = when {
                allCancelled -> RequestStatus.CANCELLED
                hasFailed -> RequestStatus.FAILED
                else -> RequestStatus.SUCCESS
            }
            batchDao.markCompleted(batchId, finalStatus)
        } else {
            val anyRunning = tasks.any { it.status == RequestStatus.RUNNING }
            if (anyRunning && batch.status != RequestStatus.RUNNING) {
                batchDao.updateStatus(batchId, RequestStatus.RUNNING)
            }
        }
    }
}
