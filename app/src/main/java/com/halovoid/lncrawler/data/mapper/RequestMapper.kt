package com.halovoid.lncrawler.data.mapper

import com.halovoid.lncrawler.data.db.entities.RequestEntity
import com.halovoid.lncrawler.domain.models.Request

/**
 * Maps a database [RequestEntity] to a domain [Request] model.
 */
fun RequestEntity.toDomain(): Request = Request(
    id = id,
    type = type,
    novelUrl = novelUrl,
    name = name,
    metadata = metadata,
    status = status,
    rstatus = rstatus,
    dependsOn = dependsOn,
    url = url,
    priority = priority,
    completedAt = completedAt,
    createdAt = createdAt,
    updatedAt = updatedAt,
    parentNovel = parentNovel,
    progressTotal = progressTotal,
    progressSuccess = progressSuccess,
    progressFailed = progressFailed,
    progressCancelled = progressCancelled,
    error = error
)

/**
 * Maps a list of database [RequestEntity] objects to a list of domain [Request] models.
 */
fun List<RequestEntity>.toDomain(): List<Request> = map { it.toDomain() }
