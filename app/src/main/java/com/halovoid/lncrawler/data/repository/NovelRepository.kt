package com.halovoid.lncrawler.data.repository

import android.content.Context
import com.halovoid.lncrawler.data.db.AppDatabase
import com.halovoid.lncrawler.data.db.mappers.toDomain
import com.halovoid.lncrawler.data.db.mappers.toEntity
import com.halovoid.lncrawler.domain.models.Novel
import com.halovoid.lncrawler.utils.SimhashUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Main repository for managing novel data in the Data layer.
 * Coordinates between the [com.halovoid.lncrawler.api.core.crawler.Crawler]s (Network)
 * and [com.halovoid.lncrawler.data.db.AppDatabase] (Local Storage).
 */
class NovelRepository private constructor(context: Context) {
    /** Access to the Room database instance. */
    private val db = AppDatabase.getDatabase(context)
    /** Data Access Object for novel-related database operations. */
    private val novelDao = db.novelDao()
    private val volumeDao = db.volumeDao()
    private val chapterDao = db.chapterDao()

    companion object {
        @Volatile
        private var INSTANCE: NovelRepository? = null

        fun getInstance(context: Context): NovelRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: NovelRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    /**
     * Retrieves all novels saved in the local database.
     * @return A [Flow] emitting the latest list of [com.halovoid.lncrawler.domain.models.Novel]s.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun getAllNovels(): Flow<List<Novel>> {
        return novelDao.getAllNovels().flatMapLatest { novelEntities ->
            if (novelEntities.isEmpty()) {
                flowOf(emptyList())
            } else {
                val flows = novelEntities.map { novelEntity ->
                    combine(
                        chapterDao.getChaptersFlow(novelEntity.url),
                        volumeDao.getVolumesForNovelFlow(novelEntity.url)
                    ) { chapterEntities, volumeEntities ->
                        novelEntity.toDomain().copy(
                            chapters = chapterEntities.map { it.toDomain() },
                            volumes = volumeEntities.map { it.toDomain() }
                        )
                    }
                }
                combine(*flows.toTypedArray()) { novels ->
                    novels.toList()
                }
            }
        }.flowOn(Dispatchers.IO)
    }

    fun getNovelByUrlFlow(url: String): Flow<Novel?> {
        return novelDao.getNovelByUrlFlow(url).map { it?.toDomain() }
    }

    /**
     * Retrieves full details for a novel.
     *
     * @param novelUrl The URL of the novel.
     * @return The populated [com.halovoid.lncrawler.domain.models.Novel] or null if not found.
     */
    suspend fun getNovelDetails(novelUrl: String): Novel? {
        return novelDao.getNovelByUrl(novelUrl)?.toDomain()
    }

    /**
     * Persists a novel and its chapters to the local Room database.
     * @param novel The novel to save.
     */
    suspend fun saveNovelMetadata(novel: Novel) = withContext(Dispatchers.IO) {
        val novelToSave = if (novel.titleHash == null) {
            novel.copy(titleHash = SimhashUtils.generateSimhash(novel.title))
        } else {
            novel
        }
        
        novelDao.upsertNovel(novelToSave.toEntity())
        
        // Save volumes and chapters if present
        if (novel.volumes.isNotEmpty()) {
            volumeDao.insertVolumes(novel.volumes.map { it.toEntity() })
        }
        if (novel.chapters.isNotEmpty()) {
            chapterDao.insertChapters(novel.chapters.map { it.toEntity() })
        }
    }

    suspend fun getSimilarNovels(hash: Long, threshold: Int): List<Novel> = withContext(Dispatchers.IO) {
        novelDao.getAllNovelsOnce().map { it.toDomain() }.filter { existingNovel ->
            existingNovel.titleHash?.let { existingHash ->
                SimhashUtils.hammingDistance(hash, existingHash) <= threshold
            } ?: false
        }
    }

    /**
     * Deletes a novel and its chapters from the local database.
     * @param novel The novel to delete.
     */
    suspend fun deleteNovel(novel: Novel) {
        val entity = novel.toEntity()
        novelDao.deleteNovel(entity)
    }
}
