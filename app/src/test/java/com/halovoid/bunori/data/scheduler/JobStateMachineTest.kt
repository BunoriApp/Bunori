package com.halovoid.bunori.data.scheduler

import com.halovoid.bunori.data.db.entities.JobStatus
import com.halovoid.bunori.data.scheduler.jobs.JobEvent
import com.halovoid.bunori.data.scheduler.jobs.JobStateMachine
import org.junit.Assert.assertEquals
import org.junit.Test

class JobStateMachineTest {

    @Test
    fun testPauseAndResumeTransitions() {
        // PENDING can be paused
        assertEquals(JobStatus.PAUSED, JobStateMachine.transition(JobStatus.PENDING, JobEvent.PAUSE_REQUESTED))

        // RUNNING can be paused
        assertEquals(JobStatus.PAUSED, JobStateMachine.transition(JobStatus.RUNNING, JobEvent.PAUSE_REQUESTED))

        // PAUSED can be resumed back to PENDING
        assertEquals(JobStatus.PENDING, JobStateMachine.transition(JobStatus.PAUSED, JobEvent.RESUME_REQUESTED))

        // PAUSED can be claimed directly to RUNNING
        assertEquals(JobStatus.RUNNING, JobStateMachine.transition(JobStatus.PAUSED, JobEvent.CLAIMED))

        // PAUSED can be cancelled
        assertEquals(JobStatus.CANCELLED, JobStateMachine.transition(JobStatus.PAUSED, JobEvent.CANCEL_REQUESTED))

        // Idempotent pause/resume
        assertEquals(JobStatus.PAUSED, JobStateMachine.transition(JobStatus.PAUSED, JobEvent.PAUSE_REQUESTED))
        assertEquals(JobStatus.PENDING, JobStateMachine.transition(JobStatus.PENDING, JobEvent.RESUME_REQUESTED))
        assertEquals(JobStatus.RUNNING, JobStateMachine.transition(JobStatus.RUNNING, JobEvent.RESUME_REQUESTED))
    }

    @Test
    fun testStandardTransitionsPreserved() {
        assertEquals(JobStatus.RUNNING, JobStateMachine.transition(JobStatus.PENDING, JobEvent.CLAIMED))
        assertEquals(JobStatus.CANCELLED, JobStateMachine.transition(JobStatus.PENDING, JobEvent.CANCEL_REQUESTED))
        assertEquals(JobStatus.SUCCESS, JobStateMachine.transition(JobStatus.RUNNING, JobEvent.HANDLER_SUCCESS))
        assertEquals(JobStatus.RUNNING, JobStateMachine.transition(JobStatus.RUNNING, JobEvent.HANDLER_FAILURE_RETRYABLE))
        assertEquals(JobStatus.FAILED, JobStateMachine.transition(JobStatus.RUNNING, JobEvent.HANDLER_FAILURE_FINAL))
        assertEquals(JobStatus.CANCELLED, JobStateMachine.transition(JobStatus.RUNNING, JobEvent.CANCEL_REQUESTED))
        assertEquals(JobStatus.BLOCKED, JobStateMachine.transition(JobStatus.RUNNING, JobEvent.BLOCKED_BY_PROTECTION))
    }
}
