package com.halovoid.lncrawler.ui.core.logging

import android.util.Log

/**
 * Central facade for application logging.
 * Replaces direct calls to [android.util.Log] to ensure unified tagging, filtering,
 * and future crash/telemetry reporting.
 */
object AppLog {
    fun d(tag: String, message: String) {
        Log.d(tag, message)
    }

    fun i(tag: String, message: String) {
        Log.i(tag, message)
    }

    fun w(tag: String, message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Log.w(tag, message, throwable)
        } else {
            Log.w(tag, message)
        }
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Log.e(tag, message, throwable)
        } else {
            Log.e(tag, message)
        }
    }
}
