package com.halovoid.bunori.data.scheduler

import com.halovoid.bunori.data.db.entities.RequestStatus
import com.halovoid.bunori.data.scheduler.jobs.JobEvent
import com.halovoid.bunori.data.scheduler.jobs.JobStateMachine
import org.junit.Assert.assertEquals
import org.junit.Test

class JobStateMachineTest {

    @Test
    fun testPauseAndResumeTransitions() {
        // PENDING can be paused
        assertEquals(RequestStatus.PAUSED, JobStateMachine.transition(RequestStatus.PENDING, JobEvent.PAUSE_REQUESTED))

        // RUNNING can be paused
        assertEquals(RequestStatus.PAUSED, JobStateMachine.transition(RequestStatus.RUNNING, JobEvent.PAUSE_REQUESTED))

        // PAUSED can be resumed back to PENDING
        assertEquals(RequestStatus.PENDING, JobStateMachine.transition(RequestStatus.PAUSED, JobEvent.RESUME_REQUESTED))

        // PAUSED can be claimed directly to RUNNING
        assertEquals(RequestStatus.RUNNING, JobStateMachine.transition(RequestStatus.PAUSED, JobEvent.CLAIMED))

        // PAUSED can be cancelled
        assertEquals(RequestStatus.CANCELLED, JobStateMachine.transition(RequestStatus.PAUSED, JobEvent.CANCEL_REQUESTED))

        // Idempotent pause/resume
        assertEquals(RequestStatus.PAUSED, JobStateMachine.transition(RequestStatus.PAUSED, JobEvent.PAUSE_REQUESTED))
        assertEquals(RequestStatus.PENDING, JobStateMachine.transition(RequestStatus.PENDING, JobEvent.RESUME_REQUESTED))
        assertEquals(RequestStatus.RUNNING, JobStateMachine.transition(RequestStatus.RUNNING, JobEvent.RESUME_REQUESTED))
    }

    @Test
    fun testStandardTransitionsPreserved() {
        assertEquals(RequestStatus.RUNNING, JobStateMachine.transition(RequestStatus.PENDING, JobEvent.CLAIMED))
        assertEquals(RequestStatus.CANCELLED, JobStateMachine.transition(RequestStatus.PENDING, JobEvent.CANCEL_REQUESTED))
        assertEquals(RequestStatus.SUCCESS, JobStateMachine.transition(RequestStatus.RUNNING, JobEvent.HANDLER_SUCCESS))
        assertEquals(RequestStatus.RUNNING, JobStateMachine.transition(RequestStatus.RUNNING, JobEvent.HANDLER_FAILURE_RETRYABLE))
        assertEquals(RequestStatus.FAILED, JobStateMachine.transition(RequestStatus.RUNNING, JobEvent.HANDLER_FAILURE_FINAL))
        assertEquals(RequestStatus.CANCELLED, JobStateMachine.transition(RequestStatus.RUNNING, JobEvent.CANCEL_REQUESTED))
        assertEquals(RequestStatus.BLOCKED, JobStateMachine.transition(RequestStatus.RUNNING, JobEvent.BLOCKED_BY_PROTECTION))
    }
}
