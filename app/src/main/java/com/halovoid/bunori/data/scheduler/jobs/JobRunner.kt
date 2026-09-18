package com.halovoid.bunori.data.scheduler.jobs

import com.halovoid.bunori.api.core.crawler.CrawlerFactory
import com.halovoid.bunori.data.config.SchedulerConfig
import com.halovoid.bunori.data.db.dao.BatchDao
import com.halovoid.bunori.data.db.dao.TaskDao
import com.halovoid.bunori.data.db.entities.JobStatus
import com.halovoid.bunori.data.db.entities.TaskEntity
import com.halovoid.bunori.data.handlers.utility.crawlerName
import com.halovoid.bunori.data.handlers.utility.parsedMetadata
import com.halovoid.bunori.data.scheduler.CrawlerRateLimiter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

class JobRunner(
    private val batchDao: BatchDao,
    private val taskDao: TaskDao,
    private val handlerRegistry: JobHandlerRegistry,
    private val retryPolicy: RetryPolicy,
    private val config: SchedulerConfig,
    private val rateLimiter: CrawlerRateLimiter? = null,
    private val onCrawlerBlocked: (suspend (crawlerName: String, task: TaskEntity) -> Unit)? = null
) {
    companion object {
        private const val DEFAULT_MAX_ATTEMPTS = 3
    }

    suspend fun run(task: TaskEntity, onComplete: suspend () -> Unit) {
        var currentTask = task
        try {
            val preClaim = taskDao.getTaskById(currentTask.id)
            if (preClaim == null || preClaim.status == JobStatus.CANCELLED || preClaim.status == JobStatus.PAUSED) {
                return
            }

            taskDao.updateStatus(currentTask.id, JobStatus.RUNNING)
            batchDao.updateStatus(currentTask.batchId, JobStatus.RUNNING)

            val handler = handlerRegistry.getHandler(currentTask.type)
            if (handler == null) {
                fail(currentTask, "No handler found for ${currentTask.type}", currentTask.attemptCount)
                return
            }

            val maxAttempts = maxAttemptsFor(currentTask)

            while (true) {
                val crawlerName = currentTask.crawlerName
                if (crawlerName != null && rateLimiter != null) {
                    val crawler = CrawlerFactory.getCrawler(crawlerName)
                    val cooldownMs = crawler?.config?.runnerCooldownMs ?: 1000L
                    rateLimiter.acquire(crawlerName, cooldownMs, maxJitterMs = 250L)
                }

                val preExec = taskDao.getTaskById(currentTask.id) ?: currentTask
                if (preExec.status == JobStatus.CANCELLED || preExec.status == JobStatus.PAUSED) {
                    return
                }

                val result = handler.handle(currentTask)

                val latest = taskDao.getTaskById(currentTask.id) ?: currentTask
                if (latest.status == JobStatus.CANCELLED || latest.status == JobStatus.PAUSED) {
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
                        if (postDelay == null || postDelay.status == JobStatus.CANCELLED || postDelay.status == JobStatus.PAUSED) {
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

                if (batch?.status == JobStatus.PAUSED || latestTask?.status == JobStatus.PAUSED) {
                    taskDao.updateStatus(task.id, JobStatus.PAUSED)
                } else if (batch?.status == JobStatus.CANCELLED || latestTask?.status == JobStatus.CANCELLED) {
                    taskDao.updateStatus(task.id, JobStatus.CANCELLED)
                } else {
                    taskDao.updateStatus(task.id, JobStatus.PENDING)
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
        val crawlerName = task.crawlerName
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
        taskDao.updateStatus(task.id, JobStatus.CANCELLED)
        syncBatchCompletion(task.batchId)
    }

    private suspend fun markBlocked(task: TaskEntity) {
        taskDao.updateStatus(task.id, JobStatus.BLOCKED)
        batchDao.updateStatus(task.batchId, JobStatus.BLOCKED)
        val crawler = task.crawlerName
        if (crawler != null) {
            onCrawlerBlocked?.invoke(crawler, task)
        }
    }

    private suspend fun syncBatchCompletion(batchId: String) {
        val tasks = taskDao.getTasksByBatchId(batchId)
        if (tasks.isEmpty()) return

        val batch = batchDao.getBatchById(batchId) ?: return
        if (batch.status == JobStatus.CANCELLED || batch.status == JobStatus.PAUSED || batch.status == JobStatus.BLOCKED) {
            return
        }

        val allCompleted = tasks.all { 
            it.status == JobStatus.SUCCESS || 
            it.status == JobStatus.FAILED || 
            it.status == JobStatus.CANCELLED 
        }

        if (allCompleted) {
            val hasFailed = tasks.any { it.status == JobStatus.FAILED }
            val allCancelled = tasks.all { it.status == JobStatus.CANCELLED }

            val finalStatus = when {
                allCancelled -> JobStatus.CANCELLED
                hasFailed -> JobStatus.FAILED
                else -> JobStatus.SUCCESS
            }
            batchDao.markCompleted(batchId, finalStatus)
        } else {
            val anyRunning = tasks.any { it.status == JobStatus.RUNNING }
            if (anyRunning && batch.status != JobStatus.RUNNING) {
                batchDao.updateStatus(batchId, JobStatus.RUNNING)
            }
        }
    }
}
