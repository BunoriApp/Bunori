package com.halovoid.lncrawler.data.db.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class RequestType {
    NOVEL_METADATA,
    CHAPTER,
    ARTIFACT,
    RANGE_DOWNLOAD,
    BACKUP
}

enum class RequestStatus {
    PENDING,
    RUNNING,
    SUCCESS,
    FAILED,
    CANCELLED,
    CANCELLING,
    PAUSED,
    BLOCKED
}

@Entity(
    tableName = "requests",
    foreignKeys = [
        ForeignKey(
            entity = NovelEntity::class,
            parentColumns = ["url"],
            childColumns = ["parentNovel"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = RequestEntity::class,
            parentColumns = ["id"],
            childColumns = ["dependsOn"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("parentNovel"),
        Index("dependsOn"),
        Index("status")
    ]
)
data class RequestEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val parentNovel: String?, // which novel does the request belong to
    val dependsOn: String? = null, // set null for root node
    val url: String?, //Url that you actually want to work with
    val novelUrl: String, //Novel URL it is linked to
    val priority: Int = 0,
    val type: RequestType,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val completedAt: Long?,
    val progressTotal: Int = 1,
    val progressSuccess: Int = 0,
    val progressFailed: Int = 0,
    val progressCancelled: Int = 0,
    val status: RequestStatus = RequestStatus.PENDING,
    val rstatus: RequestStatus = RequestStatus.PENDING,
    val metadata: String? = null, //JSON String for containing extra data
    val error: String? = null,
    val attemptCount: Int = 0
)