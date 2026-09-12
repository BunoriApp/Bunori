package com.halovoid.bunori.domain.models

import com.halovoid.bunori.data.db.entities.ArtifactEntity

data class Artifact(
    val id: Int,
    val novelUrl: String,
    val requestId: String,
    val artifactDestination: String,
    val artifactName: String
)

fun ArtifactEntity.toDomain() : Artifact = Artifact(
    id = id,
    novelUrl = novelUrl,
    requestId = requestId,
    artifactDestination = artifactDestination,
    artifactName = artifactName
)

fun Artifact.toEntity() : ArtifactEntity = ArtifactEntity(
    id = id,
    novelUrl = novelUrl,
    requestId = requestId,
    artifactDestination = artifactDestination,
    artifactName = artifactName
)