package com.halovoid.lncrawler.domain.usecase

import com.halovoid.lncrawler.data.repository.NovelRepository
import com.halovoid.lncrawler.domain.models.Novel
import com.halovoid.lncrawler.utils.SimhashUtils

sealed interface SaveNovelResult {
    data object Saved : SaveNovelResult
    data class SimilarFound(val similarNovels: List<Novel>) : SaveNovelResult
}

/**
 * Single business action for saving a novel to the user's library with similarity check.
 */
class SaveNovelUseCase(
    private val novelRepository: NovelRepository
) {
    suspend fun checkAndSave(novel: Novel): SaveNovelResult {
        val hash = novel.titleHash ?: SimhashUtils.generateSimhash(novel.title)
        val similar = novelRepository.getSimilarNovels(hash, 3)

        return if (similar.isNotEmpty()) {
            SaveNovelResult.SimilarFound(similar)
        } else {
            novelRepository.saveNovelMetadata(novel)
            SaveNovelResult.Saved
        }
    }

    suspend fun saveDirectly(novel: Novel) {
        novelRepository.saveNovelMetadata(novel)
    }
}
