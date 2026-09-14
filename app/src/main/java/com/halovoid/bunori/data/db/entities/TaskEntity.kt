package com.halovoid.bunori.data.db.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tasks",
    foreignKeys = [
        ForeignKey(
            entity = BatchEntity::class,
            parentColumns = ["id"],
            childColumns = ["batchId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("batchId"),
        Index("status"),
        Index("priority")
    ]
)
data class TaskEntity(
    @PrimaryKey
    val id: String,
    val batchId: String,
    val name: String,
    val url: String?,
    val novelUrl: String,
    val type: JobType,
    val priority: Int = 0,
    val status: JobStatus = JobStatus.PENDING,
    val attemptCount: Int = 0,
    val maxAttempts: Int = 3,
    val error: String? = null,
    val metadata: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null
)
