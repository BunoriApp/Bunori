package com.halovoid.bunori.data.handlers

import android.net.Uri
import com.halovoid.bunori.api.core.crawler.Crawler
import com.halovoid.bunori.api.core.crawler.CrawlerFactory
import com.halovoid.bunori.api.core.scrapper.CloudflareBlockedException
import com.halovoid.bunori.data.db.dao.RequestDao
import com.halovoid.bunori.data.db.entities.RequestEntity
import com.halovoid.bunori.data.db.entities.RequestType
import com.halovoid.bunori.data.handlers.utility.parsedMetadata
import com.halovoid.bunori.data.repository.ChapterRepository
import com.halovoid.bunori.data.repository.NovelRepository
import com.halovoid.bunori.data.repository.StorageRepository
import com.halovoid.bunori.data.scheduler.jobs.JobHandler
import com.halovoid.bunori.data.scheduler.jobs.JobResult

/**
 * Handler for [RequestType.NOVEL_METADATA] requests.
 * Responsible for refreshing novel metadata and chapter lists from the source.
 */
class NovelMetadataHandler(
    private val crawlerFactory: CrawlerFactory,
    private val novelRepository: NovelRepository,
    private val chapterRepository: ChapterRepository,
    private val storageRepository: StorageRepository,
    private val requestDao: RequestDao
) : JobHandler {

    override suspend fun handle(request: RequestEntity): JobResult {
        val metadata = request.parsedMetadata
        val crawlerName = metadata.crawlerName
            ?: return JobResult.Failure(Exception("No Crawler Provided"))

        val crawler = crawlerFactory.getCrawler(crawlerName)
            ?: return JobResult.Failure(Exception("No Crawler Found for name: $crawlerName"))

        return try {
            // 1. Fetch latest details from the source
            val novel = crawler.getNovelDetails(request.novelUrl)

            // 2. Refresh cover image if available
            val coverUri = downloadAndSaveCover(novel.coverUrl, crawler, novel.url)

            // 3. Prepare the updated novel domain model (formats titles)
            val updatedNovel = crawler.prepareNovel(novel).let {
                val coverLocalUrl = if (coverUri != null) coverUri.toString() else it.coverUrl
                it.copy(
                    coverUrl = coverLocalUrl,
                    coverHttpsUrl = novel.coverUrl
                )
            }

            // 4. Fetch existing chapters to preserve local state (like downloaded fileLocation)
            val existingChapters = chapterRepository.getChaptersByNovelUrl(request.novelUrl)
            val existingChapterMap = existingChapters.associateBy { it.url }

            val mergedChapters = updatedNovel.chapters.map { chapter ->
                val existing = existingChapterMap[chapter.url]
                val effectiveScanlation = chapter.scanlationSource.takeIf {
                    it.isNotBlank() && it != "NotProvided" && it != "Not Provided"
                } ?: existing?.scanlationSource?.takeIf {
                    it.isNotBlank() && it != "NotProvided" && it != "Not Provided"
                } ?: updatedNovel.crawlerName

                if (existing != null) {
                    chapter.copy(
                        id = existing.id,
                        fileLocation = existing.fileLocation
                    ).apply {
                        sourceUrl = existing.sourceUrl ?: chapter.url
                        scanlationSource = effectiveScanlation
                        read = existing.read
                    }
                } else {
                    chapter.copy(id = 0).apply {
                        sourceUrl = sourceUrl ?: url
                        scanlationSource = effectiveScanlation
                    }
                }
            }

            // 5. Persist the updated data to the database
            val novelToSave = updatedNovel.copy(chapters = mergedChapters)
            novelRepository.saveNovelMetadata(novelToSave)

            // Metadata for totalProgressUpdate is not changed in this request
            // Currently user would need to manually do a full novel fetch
            JobResult.Success
        } catch (e: CloudflareBlockedException) {
            JobResult.Blocked
        } catch (e: Exception) {
            JobResult.Failure(e)
        }
    }

    private suspend fun downloadAndSaveCover(url: String?, crawler: Crawler, novelUrl: String): Uri? {
        if (url.isNullOrBlank() || url.startsWith("content://")) return null

        return try {
            val bytes = crawler.downloadCover(url) ?: return null
            val extension = if (url.contains(".png", ignoreCase = true)) "png" else "jpg"
            val novelKey = crawler.getNovelKey(novelUrl)
            val fileName = "cover.$extension"

            storageRepository.saveFile(
                relativePath = "novels/$novelKey/covers",
                fileName = fileName,
                mimeType = "image/$extension",
                data = bytes
            )
        } catch (e: Exception) {
            null
        }
    }
}
