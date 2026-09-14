package com.halovoid.bunori.data.db.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "downloads",
    indices = [
        Index(value = ["novelUrl", "chapterUrl"], unique = true),
        Index(value = ["novelUrl"])
    ]
)
data class DownloadEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val novelUrl: String,
    val chapterUrl: String,
    val fileLocation: String,
    val chapterIndex: Int,
    val chapterTitle: String,
    val scanlationSource: String = "Not Provided",
    val novelTitle: String,
    val sizeBytes: Long = 0,
    val downloadedAt: Long = System.currentTimeMillis()
)
