package com.halovoid.lncrawler.data.repository

import android.annotation.SuppressLint
import android.content.Context
import androidx.core.net.toUri
import com.halovoid.lncrawler.api.core.crawler.CrawlerFactory
import com.halovoid.lncrawler.domain.models.Chapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ReaderRepository private constructor(
    private val context: Context
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
            val html = readDownloaded(chapter.fileLocation) ?: fetchLive(chapter, crawlerName)
            html ?: "<p>Couldn't load this chapter. Check your connection and try again</p>"
        }

    private fun readDownloaded(fileLocation: String?): String? {
        if (fileLocation.isNullOrBlank() || !fileLocation.startsWith("content://")) return null
        return try {
            context.contentResolver.openInputStream(fileLocation.toUri())
                ?.bufferedReader()
                ?.use { it.readText() }
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun fetchLive(chapter: Chapter, crawlerName: String): String? {
        val crawler = CrawlerFactory.getCrawler(crawlerName) ?: return null
        val url = chapter.sourceUrl?.takeIf { it.isNotBlank() } ?: chapter.url
        return crawler.getChapterContent(url)
    }
}
