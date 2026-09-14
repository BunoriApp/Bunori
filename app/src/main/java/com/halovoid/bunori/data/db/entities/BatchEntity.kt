package com.halovoid.bunori.data.db.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "batches",
    foreignKeys = [
        ForeignKey(
            entity = NovelEntity::class,
            parentColumns = ["url"],
            childColumns = ["novelUrl"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("novelUrl"),
        Index("status"),
        Index("createdAt")
    ]
)
data class BatchEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val novelUrl: String,
    val type: JobType,
    val priority: Int = 0,
    val status: JobStatus = JobStatus.PENDING,
    val error: String? = null,
    val metadata: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null
)
