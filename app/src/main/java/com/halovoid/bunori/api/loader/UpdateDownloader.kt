package com.halovoid.bunori.api.loader

import android.app.DownloadManager
import android.content.Context
import android.os.Environment
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import androidx.core.net.toUri

class UpdateDownloader(private val context: Context) {
    private val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

    fun downloadUpdate(url: String, fileName: String): Long {
        val request = DownloadManager.Request(url.toUri())
            .setTitle("Bunori Update")
            .setDescription("Downloading latest version...")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        return downloadManager.enqueue(request)
    }

    fun getDownloadStatus(downloadId: Long): Flow<DownloadStatus> = flow {
        var isDownloading = true
        while (isDownloading) {
            val query = DownloadManager.Query().setFilterById(downloadId)
            val cursor = downloadManager.query(query)
            if (cursor != null && cursor.moveToFirst()) {
                val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                when (status) {
                    DownloadManager.STATUS_SUCCESSFUL -> {
                        val uri = cursor.getString(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI))
                        emit(DownloadStatus.Success(uri))
                        isDownloading = false
                    }
                    DownloadManager.STATUS_FAILED -> {
                        emit(DownloadStatus.Error("Download failed"))
                        isDownloading = false
                    }
                }
            } else {
                isDownloading = false
            }
            cursor?.close()
            if (isDownloading) delay(1000)
        }
    }

    sealed class DownloadStatus {
        data class Success(val uri: String) : DownloadStatus()
        data class Error(val message: String) : DownloadStatus()
    }
}
