package com.halovoid.lncrawler.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.halovoid.lncrawler.data.db.entities.ChapterEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChapterDao {
    @Query("SELECT * FROM chapters WHERE novelUrl = :url")
    fun getChapterFromNovel(url: String): List<ChapterEntity>

    @Query("SELECT * FROM chapters WHERE novelUrl = :url AND volumeId = :id")
    fun getChapterFromNovelAndVolume(url: String, id: String): List<ChapterEntity>

    @Query("SELECT * FROM chapters WHERE novelUrl = :url")
    fun getChaptersFlow(url: String): Flow<List<ChapterEntity>>

    @Query("SELECT * FROM chapters WHERE id = :id")
    fun getChapterById(id: Int): ChapterEntity

    @Query("SELECT COUNT(*) FROM chapters WHERE novelUrl = :novelUrl")
    fun getChapterCountFlow(novelUrl: String): Flow<Int>

    @Query("UPDATE chapters SET read = :read WHERE id = :chapterId")
    suspend fun updateChapterReadStatus(chapterId: Int, read: Boolean)

    @Query("UPDATE chapters SET read = :read WHERE id IN (:chapterIds)")
    suspend fun updateChaptersReadStatus(chapterIds: List<Int>, read: Boolean)

    @Query("UPDATE chapters SET read = :read WHERE url = :chapterUrl")
    suspend fun updateChapterReadStatusByUrl(chapterUrl: String, read: Boolean)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChapters(chapters: List<ChapterEntity>)

    @Update
    suspend fun updateChapter(chapter: ChapterEntity)
}