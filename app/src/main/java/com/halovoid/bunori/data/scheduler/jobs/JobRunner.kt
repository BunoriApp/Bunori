package com.halovoid.bunori.data.scheduler.jobs

import com.halovoid.bunori.api.core.crawler.CrawlerFactory
import com.halovoid.bunori.data.config.SchedulerConfig
import com.halovoid.bunori.data.db.dao.RequestDao
import com.halovoid.bunori.data.db.entities.RequestEntity
import com.halovoid.bunori.data.db.entities.RequestStatus
import com.halovoid.bunori.data.handlers.utility.parsedMetadata
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

class JobRunner(
    private val requestDao: RequestDao,
    private val handlerRegistry: JobHandlerRegistry,
    private val retryPolicy: RetryPolicy,
    private val config: SchedulerConfig
) {
    companion object {
        // Fallback when a request has no associated crawler (e.g. ARTIFACT jobs)
        // or the crawler doesn't define maxAttempts. Wire this to SchedulerConfig
        // if you'd rather have one global default instead of a hardcoded fallback.
        private const val DEFAULT_MAX_ATTEMPTS = 3
    }
    suspend fun run(request: RequestEntity, onComplete: suspend () -> Unit) {
        var currRequest = request
        try {
            val hasChildren = requestDao.hasChildren(currRequest.id)
            val preClaimStatus = requestDao.getRequestById(currRequest.id)?.status
            if (preClaimStatus == RequestStatus.CANCELLED) return

            currRequest = applyEvent(currRequest, JobEvent.CLAIMED, hasChildren)
            requestDao.updateRequest(currRequest)

            val handler = handlerRegistry.getHandler(currRequest.type)
            if (handler == null) {
                fail(currRequest, "No handler found for ${currRequest.type}", hasChildren)
                return
            }

            val maxAttempts = maxAttemptsFor(currRequest)
            while (true) {
                val result = handler.handle(currRequest)

                // Fetch latest state as the handler might have updated the DB (progress, rstatus via syncProgress)
                val latestRequest = requestDao.getRequestById(currRequest.id) ?: currRequest
                if (latestRequest.status == RequestStatus.CANCELLED) {
                    // Already persisted as canceled (e.g. by JobScheduler.cancelActiveJob).
                    // Nothing left for us to write.
                    return
                }
                val hasChildrenAfter = requestDao.hasChildren(latestRequest.id)

                when (result) {
                    is JobResult.Success -> {
                        markSuccess(latestRequest, hasChildrenAfter)
                        return
                    }

                    is JobResult.Cancelled -> {
                        markCancelled(latestRequest, hasChildrenAfter)
                        return
                    }

                    is JobResult.Blocked -> {
                        markBlocked(latestRequest, hasChildrenAfter)
                        return
                    }

                    is JobResult.Failure -> {
                        val attemptsSoFar = latestRequest.attemptCount + 1
                        val canRetry = result.isRecoverable && attemptsSoFar < maxAttempts
                        if (!canRetry) {
                            fail(latestRequest, result.error.message ?: "Execution Failed", hasChildrenAfter, attemptsSoFar)
                            return
                        }
                        currRequest = markRetrying(latestRequest, hasChildrenAfter, attemptsSoFar, result.error.message)
                        val delayMs = retryPolicy.getNextDelay(attemptsSoFar)
                        delay(delayMs.milliseconds)

                        val postDelay = requestDao.getRequestById(currRequest.id)
                        if (postDelay == null || postDelay.status == RequestStatus.CANCELLED) {
                            return // cancelled while we were backing off; already persisted
                        }
                        currRequest = postDelay
                    }
                }
            }
        } catch (e: CancellationException) {
            runCatching {
                val latest = requestDao.getRequestById(request.id)
                if (latest != null && latest.status != RequestStatus.CANCELLED) {
                    markCancelled(latest, requestDao.hasChildren(request.id))
                }
            }
            throw e
        } catch (e: Exception) {
            val hasChildren = requestDao.hasChildren(request.id)
            fail(request, e.message ?: "Unexpected error during execution", hasChildren, currRequest.attemptCount)
        } finally {
            onComplete()
        }
    }

    private fun maxAttemptsFor(request: RequestEntity): Int {
        val crawlerName = request.parsedMetadata.crawlerName
        val crawlerMax = crawlerName?.let { CrawlerFactory.getCrawler(it)?.config?.maxAttempts }
        return crawlerMax ?: DEFAULT_MAX_ATTEMPTS
    }

    private fun applyEvent(request: RequestEntity, event: JobEvent, hasChildren: Boolean): RequestEntity {
        val nextStatus = JobStateMachine.transition(request.status, event)
        return request.copy(
            status = nextStatus,
            rstatus = if (!hasChildren) nextStatus else request.rstatus,
            updatedAt = System.currentTimeMillis()
        )
    }

    private suspend fun markSuccess(request: RequestEntity, hasChildren: Boolean) {
        val updated = applyEvent(request, JobEvent.HANDLER_SUCCESS, hasChildren).copy(
            rstatus = if (!hasChildren) RequestStatus.SUCCESS else request.rstatus,
            completedAt = System.currentTimeMillis(),
            progressSuccess = if (hasChildren) request.progressSuccess else request.progressTotal,
            error = null
        )

        requestDao.updateRequest(updated)
        requestDao.propagateProgress(request.id)
    }

    private suspend fun fail(request: RequestEntity, errorMessage: String, hasChildren: Boolean, attemptCount: Int = request.attemptCount) {
        val updated = applyEvent(request, JobEvent.HANDLER_FAILURE_FINAL, hasChildren).copy(
            rstatus = if (!hasChildren) RequestStatus.FAILED else request.rstatus,
            progressFailed = if (hasChildren) request.progressFailed else request.progressTotal,
            error = errorMessage,
            attemptCount = attemptCount
        )

        requestDao.updateRequest(updated)
        requestDao.propagateProgress(request.id)
    }

    private suspend fun markCancelled(request: RequestEntity, hasChildren: Boolean) {
        if (request.status == RequestStatus.CANCELLED) return // idempotent
        val updated = applyEvent(request, JobEvent.CANCEL_REQUESTED, hasChildren).copy(
            rstatus = if (hasChildren) RequestStatus.CANCELLING else RequestStatus.CANCELLED,
            progressCancelled = if (hasChildren) request.progressCancelled else request.progressTotal
        )
        requestDao.updateRequest(updated)
        requestDao.propagateProgress(request.id)
    }
    private suspend fun markRetrying(request: RequestEntity, hasChildren: Boolean, attemptCount: Int, errorMessage: String?): RequestEntity {
        val updated = applyEvent(request, JobEvent.HANDLER_FAILURE_RETRYABLE, hasChildren).copy(
            attemptCount = attemptCount,
            error = errorMessage
        )
        requestDao.updateRequest(updated)
        return updated
    }


    private suspend fun markBlocked(request: RequestEntity, hasChildren: Boolean) {
        val updated = applyEvent(request, JobEvent.BLOCKED_BY_PROTECTION, hasChildren)
            .copy(error = "Security check required")

        requestDao.updateRequest(updated)
    }
}
