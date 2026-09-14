package com.halovoid.bunori.data.handlers

import com.halovoid.bunori.data.db.dao.RequestDao
import com.halovoid.bunori.data.db.entities.RequestEntity
import com.halovoid.bunori.data.db.entities.RequestStatus
import com.halovoid.bunori.data.db.entities.RequestType
import com.halovoid.bunori.data.handlers.utility.parsedMetadata
import com.halovoid.bunori.data.repository.ChapterRepository
import com.halovoid.bunori.data.scheduler.jobs.JobHandler
import com.halovoid.bunori.data.scheduler.jobs.JobResult
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import org.json.JSONObject

/**
 * Handler for [RequestType.RANGE_DOWNLOAD] requests.
 * Spawns CHAPTER requests directly for a specific range.
 * This ensures chapter lists are visible in the request details.
 */
class RangeDownloadHandler(
    private val chapterRepository: ChapterRepository,
    private val requestDao: RequestDao
) : JobHandler {
    override suspend fun handle(request: RequestEntity): JobResult {
        val metadata = request.parsedMetadata
        val startIndex = metadata.startIndex ?: 1
        val endIndex = metadata.endIndex ?: Int.MAX_VALUE

        // 1. Fetch all chapters for this novel from the local database
        val chapters = chapterRepository.getChaptersByNovelUrl(request.novelUrl)
            .filter { it.index in startIndex..endIndex }
        
        if (chapters.isEmpty()) {
            return JobResult.Failure(Exception("No chapters found in the specified range ($startIndex-$endIndex)"))
        }

        // 2. Set the total progress to the number of chapters we're about to download
        requestDao.updateProgressTotal(request.id, chapters.size)

        // 3. Create CHAPTER requests directly under this range request
        val chapterRequests = chapters.map { chapter ->
            currentCoroutineContext().ensureActive()

            val requestId = "${request.id}_chapter_${chapter.index}_${chapter.id}"
            val existing = requestDao.getRequestById(requestId)
            
            // If it already exists and is finished, don't recreate it
            if (existing != null && (existing.status == RequestStatus.SUCCESS || existing.status == RequestStatus.RUNNING || existing.status == RequestStatus.PENDING)) {
                return@map null
            }
            
            val chapterMetadata = JSONObject().apply {
                put("chapterId", chapter.id)
                put("crawlerName", metadata.crawlerName)
            }.toString()

            RequestEntity(
                id = requestId,
                type = RequestType.CHAPTER,
                parentNovel = request.novelUrl,
                dependsOn = request.id,
                priority = request.priority,
                name = "Chapter: ${chapter.title}",
                status = RequestStatus.PENDING,
                rstatus = RequestStatus.PENDING,
                completedAt = null,
                metadata = chapterMetadata,
                url = chapter.url,
                novelUrl = chapter.novelUrl,
                progressTotal = 1,
                progressSuccess = 0,
            )
        }.filterNotNull()

        // 4. Batch insert and start tracking progress
        if (chapterRequests.isNotEmpty()) {
            requestDao.insertRequests(chapterRequests)
        }
        
        // Force a progress sync to ensure the parent reflects any existing successful chapters
        requestDao.syncProgress(request.id)
        requestDao.propagateProgress(request.id)

        return JobResult.Success
    }
}
