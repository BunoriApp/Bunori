package com.halovoid.bunori.data.db.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "artifacts",
    foreignKeys = [
        ForeignKey(
            entity = RequestEntity::class,
            parentColumns = ["id"],
            childColumns = ["requestId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("requestId")
    ]
)
data class ArtifactEntity (
    @PrimaryKey(autoGenerate = true)
    val id: Int,
    val novelUrl: String,
    val requestId: String,
    val artifactDestination: String,
    val artifactName: String
)