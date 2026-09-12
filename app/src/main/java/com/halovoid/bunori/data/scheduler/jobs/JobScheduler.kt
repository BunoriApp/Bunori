package com.halovoid.bunori.data.scheduler.jobs

import com.halovoid.bunori.data.config.SchedulerConfig
import com.halovoid.bunori.data.db.dao.RequestDao
import com.halovoid.bunori.data.db.entities.RequestStatus
import com.halovoid.bunori.api.core.crawler.CrawlerFactory
import com.halovoid.bunori.data.handlers.utility.parsedMetadata
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import com.halovoid.bunori.data.repository.PreferenceRepository
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.milliseconds

/**
 * Main scheduler that manages the lifecycle of jobs.
 * Polls the database for runnable requests and manages concurrent execution.
 */
class JobScheduler(
    private val requestDao: RequestDao,
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

    fun cancelActiveJob(requestId: String) {
        scope.launch {
            val graph = JobGraph.build(requestDao.getAllRequests().first())
            val subtree = graph.subtreeOf(requestId)

            // Write CANCELLED to the DB *before* cancelling coroutines, for every node in the
            // subtree — including ones currently RUNNING. This is the fix for the old "stuck
            // pending" bug: previously we only cancelled the coroutine and waited for the running
            // JobRunner to notice and persist it, which raced against lease-based recovery and
            // could reset an orphaned child back to PENDING before it ever saw CANCELLED.
            // requestDao.cancelRequest() already exists and does status + rstatus + progress +
            // propagation in one transaction, so we just reuse it here.
            subtree.forEach { node ->
                if (node.status == RequestStatus.PENDING || node.status == RequestStatus.RUNNING) {
                    requestDao.cancelRequest(node.id)
                }
            }

            // Now stop the actual work. JobRunner's retry loop checks DB status on every
            // iteration and after every backoff delay, so even without this the loop would stop
            // on its own — this just interrupts it immediately instead of waiting out the delay.
            subtree.forEach { node ->
                activeJobs[node.id]?.cancel()
            }
        }
    }

    private suspend fun schedule() {
        val prefMaxJobs = preferenceRepository?.maxConcurrentJobs?.first() ?: config.maxConcurrentJobs
        if (prefMaxJobs != currentGlobalLimit) {
            currentGlobalLimit = prefMaxJobs
            globalPool = WorkerPool(prefMaxJobs)
            crawlerPools.clear()
        }

        val graph = JobGraph.build(requestDao.getAllRequests().first())
        recoverAbandoned(graph)
        cancelChildrenOfCancelledParents(graph)

        val readyQueue = ReadyQueue()
        readyQueue.pushAll(graph.runnableJobs())
        if (activeJobs.isEmpty() && readyQueue.isEmpty()) {
            onEmptyListener?.invoke()
            return
        }
        launchReadyJobs(readyQueue)
    }

    private fun launchReadyJobs(readyQueue: ReadyQueue) {
        while (true) {
            val request = readyQueue.pop() ?: break
            if (activeJobs.containsKey(request.id)) continue

            val crawlerName = request.parsedMetadata.crawlerName
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
                // If we can't acquire for this specific crawler, we skip it for this tick,
                // but we keep looking in the readyQueue for other crawlers
                continue
            }

            val job = scope.launch {
                try {
                    val runner = JobRunner(requestDao, handlerRegistry, retryPolicy, config)
                    runner.run(request) {
                        activeJobs.remove(request.id)
                    }
                } catch (e: Exception) {
                    activeJobs.remove(request.id)
                } finally {
                    pool.release()
                }
            }
            activeJobs[request.id] = job
        }
    }

    private suspend fun recoverAbandoned(graph: JobGraph) {
        val now = System.currentTimeMillis()
        val abandoned = graph.runningJobs().filter {
            leaseMonitor.isExpired(it, now) && !activeJobs.containsKey(it.id)
        }

        for (request in abandoned) {
            val parentCancelled = graph.parentOf(request)?.status == RequestStatus.CANCELLED
            if (parentCancelled) {
                requestDao.cancelRequest(request.id)
            } else {
                requestDao.resetStatus(request.id, RequestStatus.PENDING, RequestStatus.RUNNING)
            }
        }
    }

    private fun cancelChildrenOfCancelledParents(graph: JobGraph) {
        graph.allNodes()
            .filter { it.status == RequestStatus.PENDING && it.dependsOn != null }
            .forEach { req ->
                val parentCancelled = graph.parentOf(req)?.status == RequestStatus.CANCELLED
                if (parentCancelled) {
                    scope.launch { requestDao.cancelRequest(req.id) }
                }
            }
    }


}

internal class WorkerPool(maxConcurrent: Int) {
    private val semaphore = kotlinx.coroutines.sync.Semaphore(maxConcurrent)
    fun tryAcquire(): Boolean = semaphore.tryAcquire()
    fun release() = semaphore.release()
}

internal class LeaseMonitor(private val leaseDurationMs: Long) {
    fun isExpired(request: com.halovoid.bunori.data.db.entities.RequestEntity, now: Long = System.currentTimeMillis()): Boolean =
        request.status == com.halovoid.bunori.data.db.entities.RequestStatus.RUNNING &&
                (now - request.updatedAt) > leaseDurationMs
}

internal class ReadyQueue(
    comparator: Comparator<com.halovoid.bunori.data.db.entities.RequestEntity> =
        compareByDescending<com.halovoid.bunori.data.db.entities.RequestEntity> { it.priority }
            .thenBy { it.createdAt }
) {
    private val heap = java.util.PriorityQueue(comparator)
    fun pushAll(jobs: Collection<com.halovoid.bunori.data.db.entities.RequestEntity>) { heap.addAll(jobs) }
    fun pop(): com.halovoid.bunori.data.db.entities.RequestEntity? = heap.poll()
    fun isEmpty() = heap.isEmpty()
}

internal class JobGraph private constructor(
    private val nodes: Map<String, com.halovoid.bunori.data.db.entities.RequestEntity>,
    private val childrenOf: Map<String, List<String>>
) {
    fun get(id: String): com.halovoid.bunori.data.db.entities.RequestEntity? = nodes[id]
    fun allNodes(): Collection<com.halovoid.bunori.data.db.entities.RequestEntity> = nodes.values
    fun childrenOf(id: String): List<com.halovoid.bunori.data.db.entities.RequestEntity> =
        childrenOf[id]?.mapNotNull { nodes[it] } ?: emptyList()

    fun parentOf(entity: com.halovoid.bunori.data.db.entities.RequestEntity): com.halovoid.bunori.data.db.entities.RequestEntity? =
        entity.dependsOn?.let { nodes[it] }

    fun subtreeOf(rootId: String): List<com.halovoid.bunori.data.db.entities.RequestEntity> {
        val visited = LinkedHashSet<String>()
        val stack = ArrayDeque<String>()
        stack.addLast(rootId)
        while (stack.isNotEmpty()) {
            val currId = stack.removeLast()
            if (!visited.add(currId)) continue
            childrenOf[currId]?.forEach { stack.addLast(it) }
        }
        return visited.mapNotNull { nodes[it] }
    }

    fun runnableJobs(): List<com.halovoid.bunori.data.db.entities.RequestEntity> =
        nodes.values.filter { it.isRunnable() }

    fun runningJobs(): List<com.halovoid.bunori.data.db.entities.RequestEntity> =
        nodes.values.filter { it.status == com.halovoid.bunori.data.db.entities.RequestStatus.RUNNING }

    private fun com.halovoid.bunori.data.db.entities.RequestEntity.isRunnable(): Boolean {
        if (status != com.halovoid.bunori.data.db.entities.RequestStatus.PENDING) return false
        if (dependsOn != null) {
            val dependency = nodes[dependsOn] ?: return false
            if (dependency.status == com.halovoid.bunori.data.db.entities.RequestStatus.CANCELLED) return false
            if (dependency.status != com.halovoid.bunori.data.db.entities.RequestStatus.SUCCESS) return false
        }
        return true
    }

    companion object {
        fun build(allRequests: List<com.halovoid.bunori.data.db.entities.RequestEntity>): JobGraph {
            val byId = allRequests.associateBy { it.id }
            val children = allRequests
                .filter { it.dependsOn != null }
                .groupBy { it.dependsOn!! }
                .mapValues { (_, kids) -> kids.map { it.id } }
            return JobGraph(byId, children)
        }
    }
}
