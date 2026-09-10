package com.halovoid.lncrawler.data.scheduler.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.halovoid.lncrawler.api.backup.BackupService

class BackupWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val backupService = BackupService(applicationContext)
            backupService.createBackup(
                backupDatabase = true,
                backupChapters = true,
                backupCovers = true,
                backupArtifacts = false
            )
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}
