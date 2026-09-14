package com.halovoid.bunori.data.scheduler.jobs

import com.halovoid.bunori.api.core.crawler.CrawlerFactory
import com.halovoid.bunori.data.config.SchedulerConfig
import com.halovoid.bunori.data.db.dao.BatchDao
import com.halovoid.bunori.data.db.dao.TaskDao
import com.halovoid.bunori.data.db.entities.RequestStatus
import com.halovoid.bunori.data.db.entities.TaskEntity
import com.halovoid.bunori.data.handlers.utility.parsedMetadata
import com.halovoid.bunori.data.repository.PreferenceRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Semaphore
import java.util.PriorityQueue
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.milliseconds

class JobScheduler(
    private val batchDao: BatchDao,
    private val taskDao: TaskDao,
    private val handlerRegistry: JobHandlerRegistry,
    private val config: SchedulerConfig = SchedulerConfig(),
    private val retryPolicy: RetryPolicy = ExponentialBackoffPolicy(),
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob()),
    private val preferenceRepository: PreferenceRepository? = null
) {
    private var pollingJob: Job? = null
    private val activeJobs = ConcurrentHashMap<String, Job>()
    private val crawlerPools = ConcurrentHashMap<String, WorkerPool>()
    private var currentGlobalLimit = config.maxConcurrentJobs
    private var globalPool = WorkerPool(currentGlobalLimit)
    private val leaseMonitor = LeaseMonitor(config.abandonedTimeoutMs)
    private var onEmptyListener: (() -> Unit)? = null

    fun setOnEmptyListener(listener: () -> Unit) {
        this.onEmptyListener = listener
    }

    fun start() {
        if (pollingJob?.isActive == true) return
        pollingJob = scope.launch {
            while (isActive) {
                try {
                    schedule()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                delay(config.pollingIntervalMs.milliseconds)
            }
        }
    }

    fun stop() {
        pollingJob?.cancel()
        pollingJob = null
    }

    fun pauseJob(batchId: String) {
        scope.launch {
            batchDao.updateStatus(batchId, RequestStatus.PAUSED)
            taskDao.updateUnfinishedStatusForBatch(batchId, RequestStatus.PAUSED)
            val tasks = taskDao.getTasksByBatchId(batchId)
            tasks.forEach { activeJobs[it.id]?.cancel() }
        }
    }

    fun resumeJob(batchId: String) {
        scope.launch {
            batchDao.updateStatus(batchId, RequestStatus.RUNNING)
            taskDao.updateStatusForBatch(batchId, RequestStatus.PAUSED, RequestStatus.PENDING)
            start()
        }
    }

    fun replayJob(batchId: String) {
        scope.launch {
            batchDao.updateStatus(batchId, RequestStatus.PENDING)
            taskDao.resetAllTasksForBatch(batchId)
            start()
        }
    }

    fun cancelActiveJob(batchId: String) {
        scope.launch {
            batchDao.updateStatus(batchId, RequestStatus.CANCELLED)
            taskDao.updateUnfinishedStatusForBatch(batchId, RequestStatus.CANCELLED)
            val tasks = taskDao.getTasksByBatchId(batchId)
            tasks.forEach { activeJobs[it.id]?.cancel() }
        }
    }

    private suspend fun schedule() {
        val prefMaxJobs = preferenceRepository?.maxConcurrentJobs?.first() ?: config.maxConcurrentJobs
        if (prefMaxJobs != currentGlobalLimit) {
            currentGlobalLimit = prefMaxJobs
            globalPool = WorkerPool(prefMaxJobs)
            crawlerPools.clear()
        }

        // 1. Recover abandoned / crashed tasks (e.g. app force closed while tasks were running)
        recoverAbandoned()

        // 2. Fetch runnable tasks
        val runnableTasks = taskDao.getRunnableTasks()

        val readyQueue = ReadyQueue()
        readyQueue.pushAll(runnableTasks)

        if (activeJobs.isEmpty() && readyQueue.isEmpty()) {
            val hasActive = batchDao.hasActiveOrPendingBatches()
            if (!hasActive) {
                onEmptyListener?.invoke()
            }
            return
        }

        launchReadyJobs(readyQueue)
    }

    private fun launchReadyJobs(readyQueue: ReadyQueue) {
        while (true) {
            val task = readyQueue.pop() ?: break
            if (activeJobs.containsKey(task.id)) continue

            val crawlerName = task.parsedMetadata.crawlerName
            val pool = if (crawlerName != null) {
                crawlerPools.getOrPut(crawlerName) {
                    val crawler = CrawlerFactory.getCrawler(crawlerName)
                    val limit = crawler?.config?.runnerConcurrency ?: currentGlobalLimit
                    WorkerPool(limit)
                }
            } else {
                globalPool
            }

            if (!pool.tryAcquire()) {
                continue
            }

            val job = scope.launch {
                try {
                    val runner = JobRunner(batchDao, taskDao, handlerRegistry, retryPolicy, config)
                    runner.run(task) {
                        activeJobs.remove(task.id)
                    }
                } catch (e: Exception) {
                    activeJobs.remove(task.id)
                } finally {
                    pool.release()
                }
            }
            activeJobs[task.id] = job
        }
    }

    private suspend fun recoverAbandoned() {
        val now = System.currentTimeMillis()
        val runningTasks = taskDao.getRunningTasks()

        for (task in runningTasks) {
            if (!activeJobs.containsKey(task.id) && leaseMonitor.isExpired(task, now)) {
                val batch = batchDao.getBatchById(task.batchId)
                when (batch?.status) {
                    RequestStatus.CANCELLED -> taskDao.updateStatus(task.id, RequestStatus.CANCELLED)
                    RequestStatus.PAUSED -> taskDao.updateStatus(task.id, RequestStatus.PAUSED)
                    else -> taskDao.updateStatus(task.id, RequestStatus.PENDING)
                }
            }
        }
    }
}

internal class WorkerPool(maxConcurrent: Int) {
    private val semaphore = Semaphore(maxConcurrent)
    fun tryAcquire(): Boolean = semaphore.tryAcquire()
    fun release() = semaphore.release()
}

internal class LeaseMonitor(private val leaseDurationMs: Long) {
    fun isExpired(task: TaskEntity, now: Long = System.currentTimeMillis()): Boolean =
        task.status == RequestStatus.RUNNING && (now - task.updatedAt) > leaseDurationMs
}

internal class ReadyQueue {
    private val buckets = java.util.TreeMap<Int, LinkedHashMap<String, ArrayDeque<TaskEntity>>>(reverseOrder())

    fun pushAll(jobs: Collection<TaskEntity>) {
        for (job in jobs) {
            val novelMap = buckets.getOrPut(job.priority) { LinkedHashMap() }
            val queue = novelMap.getOrPut(job.novelUrl ?: "") { ArrayDeque() }
            queue.add(job)
        }
    }

    fun pop(): TaskEntity? {
        val iterator = buckets.iterator()
        while (iterator.hasNext()) {
            val (_, novelMap) = iterator.next()
            if (novelMap.isEmpty()) {
                iterator.remove()
                continue
            }

            val novelIterator = novelMap.entries.iterator()
            if (!novelIterator.hasNext()) {
                iterator.remove()
                continue
            }

            val (novelUrl, queue) = novelIterator.next()
            val task = queue.removeFirstOrNull()

            novelIterator.remove()
            if (queue.isNotEmpty()) {
                novelMap[novelUrl] = queue
            }

            if (novelMap.isEmpty()) {
                iterator.remove()
            }

            if (task != null) return task
        }
        return null
    }

    fun isEmpty(): Boolean = buckets.isEmpty() || buckets.values.all { novelMap -> novelMap.values.all { it.isEmpty() } }
}
