package com.halovoid.lncrawler.domain.usecase

import android.net.Uri
import com.halovoid.lncrawler.data.repository.ChapterRepository
import com.halovoid.lncrawler.data.repository.StorageRepository
import com.halovoid.lncrawler.domain.models.Chapter
import com.halovoid.lncrawler.ui.core.logging.AppLog

/**
 * Single business action for deleting a downloaded chapter's file and updating its database state.
 */
class DeleteChapterUseCase(
    private val chapterRepository: ChapterRepository,
    private val storageRepository: StorageRepository
) {
    suspend operator fun invoke(chapter: Chapter) {
        chapter.fileLocation?.let { location ->
            try {
                storageRepository.delete(Uri.parse(location))
            } catch (e: Exception) {
                AppLog.w("DeleteChapterUseCase", "Failed to delete chapter file at $location", e)
            }
        }
        chapterRepository.updateChapter(chapter.copy(fileLocation = null).apply {
            sourceUrl = chapter.sourceUrl
            scanlationSource = chapter.scanlationSource
            read = chapter.read
        })
    }
}
