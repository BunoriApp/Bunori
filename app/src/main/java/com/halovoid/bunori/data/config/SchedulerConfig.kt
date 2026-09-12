package com.halovoid.bunori.data.config

/**
 * Configuration for the [com.halovoid.bunori.data.scheduler.jobs.JobScheduler].
 */
data class SchedulerConfig(
    /** Maximum number of jobs to run concurrently. */
    val maxConcurrentJobs: Int = 3,
    /** Interval between polling for new jobs in milliseconds. */
    val pollingIntervalMs: Long = 5000,
    /** Time after which a RUNNING job is considered abandoned and recovered. */
    val abandonedTimeoutMs: Long = 300000, // 5 minutes
    /** Maximum number of retries for a job. */
    val maxRetries: Int = 3
)