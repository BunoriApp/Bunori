package com.halovoid.bunori.data.repository

import android.content.Context
import com.halovoid.bunori.data.db.AppDatabase
import com.halovoid.bunori.data.db.mappers.toDomain
import com.halovoid.bunori.data.db.mappers.toEntity
import com.halovoid.bunori.domain.models.Chapter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ChapterRepository private constructor(context: Context) {
    private val db = AppDatabase.getDatabase(context)
    private val chapterDao = db.chapterDao()

    companion object {
        @Volatile
        private var INSTANCE: ChapterRepository? = null

        fun getInstance(context: Context): ChapterRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ChapterRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    fun getChaptersByNovelUrl(url: String): List<Chapter> {
        return chapterDao.getChapterFromNovel(url).map { it -> it.toDomain() }
    }

    fun getChaptersFlow(url: String): Flow<List<Chapter>> {
        return chapterDao.getChaptersFlow(url).map { list -> list.map { it.toDomain() } }
    }

    suspend fun insertChapters(chapters : List<Chapter>) {
        chapterDao.insertChapters(chapters.map { it -> it.toEntity() })
    }

    fun getChapterById(id: Int) : Chapter {
        return chapterDao.getChapterById(id).toDomain()
    }

    suspend fun updateChapter(chapter: Chapter) {
        chapterDao.updateChapter(chapter = chapter.toEntity())
    }

    suspend fun updateChapterReadStatus(chapterId: Int, isRead: Boolean) {
        chapterDao.updateChapterReadStatus(chapterId, isRead)
    }

    suspend fun updateChapterReadStatus(chapterUrl: String, isRead: Boolean) {
        chapterDao.updateChapterReadStatusByUrl(chapterUrl, isRead)
    }

    suspend fun updateChaptersReadStatus(chapterIds: List<Int>, isRead: Boolean) {
        chapterDao.updateChaptersReadStatus(chapterIds, isRead)
    }

    fun getChapterCount(novelUrl: String): Flow<Int> =
        chapterDao.getChapterCountFlow(novelUrl)
}