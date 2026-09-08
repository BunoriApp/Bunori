package com.halovoid.lncrawler.domain.usecase

import android.net.Uri
import com.halovoid.lncrawler.data.factory.RequestFactory
import com.halovoid.lncrawler.data.repository.ChapterRepository
import com.halovoid.lncrawler.data.repository.RequestRepository
import com.halovoid.lncrawler.data.repository.StorageRepository
import com.halovoid.lncrawler.domain.models.Chapter
import com.halovoid.lncrawler.domain.models.Novel
import com.halovoid.lncrawler.ui.core.logging.AppLog

/**
 * Single business action for deleting an existing chapter download and re-queuing a fetch request.
 */
class ReplayChapterUseCase(
    private val chapterRepository: ChapterRepository,
    private val storageRepository: StorageRepository,
    private val requestRepository: RequestRepository,
    private val requestFactory: RequestFactory = RequestFactory()
) {
    suspend operator fun invoke(novel: Novel, chapter: Chapter) {
        chapter.fileLocation?.let { location ->
            try {
                storageRepository.delete(Uri.parse(location))
            } catch (e: Exception) {
                AppLog.w("ReplayChapterUseCase", "Failed to delete chapter file at $location on replay", e)
            }
        }
        chapterRepository.updateChapter(chapter.copy(fileLocation = null).apply {
            sourceUrl = chapter.sourceUrl
            scanlationSource = chapter.scanlationSource
            read = chapter.read
        })
        val request = requestFactory.chapter(novel, chapter)
        requestRepository.insertRequests(listOf(request))
    }
}
