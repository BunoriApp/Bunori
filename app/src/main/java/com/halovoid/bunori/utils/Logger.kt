package com.halovoid.bunori.utils

import android.util.Log
import com.halovoid.bunori.BuildConfig
import java.time.LocalTime
import java.time.format.DateTimeFormatter

object Logger {
    private const val TAG = "Bunori"
    private const val MAX_LOGS = 100
    private val logBuffer = ArrayDeque<String>(MAX_LOGS)
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS")

    private fun addLog(level: String, message: String) {
        val timestamp = LocalTime.now().format(timeFormatter)
        val logEntry = "[$timestamp] $level: $message"

        synchronized(logBuffer) {
            if (logBuffer.size >= MAX_LOGS) {
                logBuffer.removeFirst()
            }
            logBuffer.addLast(logEntry)
        }
    }

    fun getLogs(): String {
        return synchronized(logBuffer) {
            logBuffer.joinToString("\n")
        }
    }

    fun d(message: String) {
        addLog("D", message)
        if (BuildConfig.DEBUG) {
            Log.d(TAG, message)
        }
    }

    fun i(message: String) {
        addLog("I", message)
        Log.i(TAG, message)
    }

    fun w(message: String, throwable: Throwable? = null) {
        addLog("W", "$message ${throwable?.message ?: ""}")
        Log.w(TAG, message, throwable)
    }

    fun e(message: String, throwable: Throwable? = null) {
        addLog("E", "$message, ${throwable?.message ?: ""}")
        Log.e(TAG, message, throwable)
    }
}
