package com.halovoid.bunori.data.repository

import android.annotation.SuppressLint
import android.content.Context
import androidx.core.net.toUri
import com.halovoid.bunori.api.core.crawler.CrawlerFactory
import com.halovoid.bunori.domain.models.Chapter
import com.halovoid.bunori.domain.models.Download
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ReaderRepository private constructor(
    private val context: Context,
    private val downloadRepository: DownloadRepository = DownloadRepositoryImpl.getInstance(context)
) {
    companion object {
        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var INSTANCE: ReaderRepository? = null

        fun getInstance(context: Context): ReaderRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ReaderRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    suspend fun getChapterContent(chapter: Chapter, crawlerName: String): String =
        withContext(Dispatchers.IO) {
            val download = downloadRepository.getDownload(chapter.novelUrl, chapter.url)
            val html = if (download != null) {
                readDownloaded(download) ?: fetchLive(chapter, crawlerName)
            } else {
                fetchLive(chapter, crawlerName)
            }
            html ?: "<p>Couldn't load this chapter. Check your connection and try again</p>"
        }

    private suspend fun readDownloaded(download: Download): String? {
        val fileLocation = download.fileLocation
        if (fileLocation.isBlank() || !fileLocation.startsWith("content://")) return null
        return try {
            context.contentResolver.openInputStream(fileLocation.toUri())
                ?.bufferedReader()
                ?.use { it.readText() }
        } catch (e: Exception) {
            downloadRepository.deleteDownload(download.novelUrl, download.chapterUrl)
            null
        }
    }

    private suspend fun fetchLive(chapter: Chapter, crawlerName: String): String? {
        val crawler = CrawlerFactory.getCrawler(crawlerName) ?: return null
        val url = chapter.sourceUrl?.takeIf { it.isNotBlank() } ?: chapter.url
        return crawler.getChapterContent(url)
    }
}
