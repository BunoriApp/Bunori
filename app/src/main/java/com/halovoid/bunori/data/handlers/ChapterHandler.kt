package com.halovoid.bunori.data.handlers

import android.net.Uri
import com.halovoid.bunori.api.core.crawler.Crawler
import com.halovoid.bunori.api.core.crawler.CrawlerFactory
import com.halovoid.bunori.api.core.scrapper.CloudflareBlockedException
import com.halovoid.bunori.api.core.scrapper.Scrapper
import com.halovoid.bunori.data.db.dao.RequestDao
import com.halovoid.bunori.data.db.entities.RequestEntity
import com.halovoid.bunori.data.handlers.utility.parsedMetadata
import com.halovoid.bunori.data.repository.ChapterRepository
import com.halovoid.bunori.data.repository.DownloadRepository
import com.halovoid.bunori.data.repository.NovelRepository
import com.halovoid.bunori.data.repository.StorageRepository
import com.halovoid.bunori.data.scheduler.jobs.JobHandler
import com.halovoid.bunori.data.scheduler.jobs.JobResult
import com.halovoid.bunori.domain.models.Chapter
import com.halovoid.bunori.domain.models.Download

class ChapterHandler(
    private val requestDao: RequestDao,
    private val scrapper: Scrapper,
    private val chapterRepository: ChapterRepository,
    private val storageRepository: StorageRepository,
    private val crawlerFactory: CrawlerFactory,
    private val downloadRepository: DownloadRepository,
    private val novelRepository: NovelRepository
) : JobHandler {
    override suspend fun handle(request: RequestEntity): JobResult {
        val metadata = request.parsedMetadata
        if (metadata.chapterId == null || request.url == null) {
            return JobResult.Failure(Exception("Failure to complete request"))
        }
        if (metadata.crawlerName == null) {
            return JobResult.Failure(Exception("No Crawler Found"))
        }
        val crawler = crawlerFactory.getCrawler(metadata.crawlerName)
            ?: return JobResult.Failure(Exception("No Crawler Found"))

        val chapter = chapterRepository.getChapterById(metadata.chapterId)
        val novel = novelRepository.getNovelDetails(chapter.novelUrl)
        val novelTitle = novel?.title ?: "Novel"

        // 1. Load the Chapter and Save it
        try {
            val fileLocation = loadAndSaveFile(request.url, crawler, chapter)
                ?: return JobResult.Failure(Exception("Failed to Load Content"))

            // 2. Save into the Download table
            downloadRepository.saveDownload(
                Download(
                    novelUrl = chapter.novelUrl,
                    chapterUrl = chapter.url,
                    fileLocation = fileLocation.toString(),
                    chapterIndex = chapter.index,
                    chapterTitle = chapter.title,
                    scanlationSource = chapter.scanlationSource,
                    novelTitle = novelTitle
                )
            )

            return JobResult.Success
        } catch (e: CloudflareBlockedException) {
            return JobResult.Blocked
        } catch (e: Exception) {
            return JobResult.Failure(e)
        }
    }

    suspend fun loadAndSaveFile(url: String, crawler: Crawler, chapter: Chapter): Uri? {
        if (!url.startsWith("content://")) {
            try {
                val chapterContent = crawler.getChapterContent(url)
                if (!chapterContent.isNullOrBlank() && chapterContent.trim().length > 100) {
                    val novelKey = crawler.getNovelKey(chapter.novelUrl)
                    val fileName = "${chapter.index.toString().padStart(4, '0')}_${chapter.id}.html"
                    val relativePath = "novels/$novelKey/chapters"

                    val localUri = storageRepository.saveText(
                        relativePath = relativePath,
                        fileName = fileName,
                        mimeType = "text/html",
                        content = chapterContent
                    )

                    return localUri
                } else {
                    val errorMsg = if (chapterContent.isNullOrBlank()) "Empty content" else "Content too short (${chapterContent.length} chars)"
                    throw Exception("Failed to fetch valid content: $errorMsg")
                }
            } catch (e: Exception) {
                return null
            }
        }
        return null
    }
}