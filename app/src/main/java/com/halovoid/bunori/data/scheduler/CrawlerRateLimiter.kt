package com.halovoid.bunori.data.scheduler

import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds

class CrawlerRateLimiter {
    private val mutexes = ConcurrentHashMap<String, Mutex>()
    private val lastAccessTimes = ConcurrentHashMap<String, Long>()

    suspend fun acquire(crawlerName: String, cooldownMs: Long, maxJitterMs: Long = 0L) {
        if (cooldownMs <= 0) return
        val mutex = mutexes.computeIfAbsent(crawlerName) { Mutex() }
        mutex.withLock {
            val now = System.currentTimeMillis()
            val last = lastAccessTimes[crawlerName] ?: 0L
            val jitter = if (maxJitterMs > 0) Random.nextLong(0, maxJitterMs + 1) else 0L
            val targetInterval = cooldownMs + jitter
            val elapsed = now - last
            if (elapsed < targetInterval) {
                delay((targetInterval - elapsed).milliseconds)
            }
            lastAccessTimes[crawlerName] = System.currentTimeMillis()
        }
    }
}
