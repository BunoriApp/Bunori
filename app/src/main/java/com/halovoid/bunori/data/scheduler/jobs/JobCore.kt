package com.halovoid.bunori.data.scheduler.jobs

import com.halovoid.bunori.data.db.entities.RequestEntity
import com.halovoid.bunori.data.db.entities.RequestStatus
import com.halovoid.bunori.data.db.entities.RequestType
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.pow

enum class JobEvent {
    CLAIMED,
    HANDLER_SUCCESS,
    HANDLER_FAILURE_RETRYABLE,
    HANDLER_FAILURE_FINAL,
    CANCEL_REQUESTED,
    BLOCKED_BY_PROTECTION
}

sealed class JobResult {
    object Success : JobResult()

    data class Failure(
        val error: Throwable,
        val isRecoverable: Boolean = true
    ) : JobResult()

    object Cancelled : JobResult()

    object Blocked : JobResult()
}

object JobStateMachine {
    fun transition(current: RequestStatus, event: JobEvent): RequestStatus =
        when (current to event) {
            RequestStatus.PENDING to JobEvent.CLAIMED -> RequestStatus.RUNNING
            RequestStatus.PENDING to JobEvent.CANCEL_REQUESTED -> RequestStatus.CANCELLED

            RequestStatus.RUNNING to JobEvent.HANDLER_SUCCESS -> RequestStatus.SUCCESS
            RequestStatus.RUNNING to JobEvent.HANDLER_FAILURE_RETRYABLE -> RequestStatus.RUNNING
            RequestStatus.RUNNING to JobEvent.HANDLER_FAILURE_FINAL -> RequestStatus.FAILED
            RequestStatus.RUNNING to JobEvent.CANCEL_REQUESTED -> RequestStatus.CANCELLED
            RequestStatus.RUNNING to JobEvent.BLOCKED_BY_PROTECTION -> RequestStatus.BLOCKED

            else -> error("Illegal Transaction: $current -> $event")
        }
}

interface JobHandler {
    suspend fun handle(request: RequestEntity): JobResult
}

class JobHandlerRegistry {
    private val handlers = ConcurrentHashMap<RequestType, JobHandler>()

    fun register(type: RequestType, handler: JobHandler) {
        handlers[type] = handler
    }

    fun getHandler(type: RequestType): JobHandler? = handlers[type]
}

interface RetryPolicy {
    fun getNextDelay(retryCount: Int): Long
}

class ExponentialBackoffPolicy(
    private val initialDelay: Long = 1000,
    private val factor: Double = 2.0,
    private val maxDelay: Long = 60000
) : RetryPolicy {
    override fun getNextDelay(retryCount: Int): Long {
        return (initialDelay * factor.pow(retryCount.toDouble())).toLong().coerceAtMost(maxDelay)
    }
}
