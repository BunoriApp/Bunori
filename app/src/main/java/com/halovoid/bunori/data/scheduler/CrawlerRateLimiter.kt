package com.halovoid.bunori.data.scheduler

import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

class CrawlerRateLimiter {
    private val mutexes = ConcurrentHashMap<String, Mutex>()
    private val lastAccessTimes = ConcurrentHashMap<String, Long>()

    suspend fun acquire(crawlerName: String, cooldownMs: Long) {
        if (cooldownMs <= 0) return
        val mutex = mutexes.computeIfAbsent(crawlerName) { Mutex() }
        mutex.withLock {
            val now = System.currentTimeMillis()
            val last = lastAccessTimes[crawlerName] ?: 0L
            val elapsed = now - last
            if (elapsed < cooldownMs) {
                delay(cooldownMs - elapsed)
            }
            lastAccessTimes[crawlerName] = System.currentTimeMillis()
        }
    }
}
