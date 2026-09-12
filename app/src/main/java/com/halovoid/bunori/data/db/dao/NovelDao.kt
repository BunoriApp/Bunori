package com.halovoid.bunori.data.db.dao

import androidx.room.*
import com.halovoid.bunori.data.db.entities.NovelEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NovelDao {
    @Query("SELECT * FROM novels")
    fun getAllNovels(): Flow<List<NovelEntity>>

    @Query("SELECT * FROM novels")
    suspend fun getAllNovelsOnce(): List<NovelEntity>

    @Query("SELECT * FROM novels WHERE url = :url")
    fun getNovelByUrl(url: String): NovelEntity?

    @Query("SELECT * FROM novels WHERE url = :url")
    fun getNovelByUrlFlow(url: String): Flow<NovelEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertNovel(novel: NovelEntity)

    @Upsert
    suspend fun upsertNovel(novel: NovelEntity)

    @Delete
    suspend fun deleteNovel(novel: NovelEntity)
}