package com.halovoid.lncrawler.data.db.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "chapters",
    foreignKeys = [
        ForeignKey(
            entity = NovelEntity::class,
            parentColumns = ["url"],
            childColumns = ["novelUrl"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = VolumeEntity::class,
            parentColumns = ["id"],
            childColumns = ["volumeId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("novelUrl"),
        Index("volumeId")
    ]
)
data class ChapterEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val novelUrl: String,
    val volumeId: String,
    val url: String,
    val sourceUrl: String? = null,
    val title: String,
    val index: Int,
    val fileLocation: String?,
    val read: Boolean = false
)
