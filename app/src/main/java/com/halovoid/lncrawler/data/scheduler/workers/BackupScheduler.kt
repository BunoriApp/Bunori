package com.halovoid.lncrawler.data.scheduler.workers

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.time.Duration
import java.util.Calendar
import java.util.concurrent.TimeUnit

object BackupScheduler {
    private const val WORK_NAME = "lncrawler_backup_work"

    fun scheduleBackupWork(context: Context, frequency: String) {
        val workManager = WorkManager.getInstance(context)
        if (frequency == "Off") {
            workManager.cancelUniqueWork(WORK_NAME)
            return
        }

        val repeatInterval = when (frequency) {
            "Daily" -> Duration.ofDays(1)
            "Weekly" -> Duration.ofDays(7)
            "Monthly" -> Duration.ofDays(30)
            else -> {
                workManager.cancelUniqueWork(WORK_NAME)
                return
            }
        }

        val initialDelay = calculateInitialDelayTo530PM()

        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build()

        val periodicWorkRequest = PeriodicWorkRequestBuilder<BackupWorker>(
            repeatInterval
        )
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .setConstraints(constraints)
            .build()

        workManager.enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            periodicWorkRequest
        )
    }

    private fun calculateInitialDelayTo530PM(): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 17)
            set(Calendar.MINUTE, 30)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (now.after(target)) {
            target.add(Calendar.DAY_OF_YEAR, 1)
        }
        return target.timeInMillis - now.timeInMillis
    }
}
