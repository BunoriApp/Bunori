package com.halovoid.bunori.data.config

/**
 * Configuration for the [com.halovoid.bunori.data.scheduler.jobs.JobScheduler].
 */
data class SchedulerConfig(
    val maxConcurrentJobs: Int = 3,
    val pollingIntervalMs: Long = 30000,
    val abandonedTimeoutMs: Long = 300000,
    val maxRetries: Int = 3
)