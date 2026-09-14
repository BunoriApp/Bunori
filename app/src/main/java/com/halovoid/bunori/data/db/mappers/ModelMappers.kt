package com.halovoid.bunori.data.db.mappers

import com.halovoid.bunori.data.db.entities.ChapterEntity
import com.halovoid.bunori.data.db.entities.NovelEntity
import com.halovoid.bunori.domain.models.Chapter
import com.halovoid.bunori.domain.models.Novel

fun NovelEntity.toDomain(): Novel = Novel(
    url = url,
    title = title,
    author = author,
    coverUrl = coverUrl,
    description = description,
    crawlerName = crawlerName,
    alternativeNames = alternativeNames,
    chapters = emptyList(), // Chapters are usually loaded separately
    titleHash = titleHash,
    coverHttpsUrl = coverHttpsUrl
)


fun Novel.toEntity() = NovelEntity(
    url = url,
    title = title,
    author = author,
    coverUrl = coverUrl,
    description = description,
    crawlerName = crawlerName,
    alternativeNames = alternativeNames,
    titleHash = titleHash,
    coverHttpsUrl = coverHttpsUrl
)

// --- Chapter Mappings ---

fun ChapterEntity.toDomain(): Chapter = Chapter(
    id = id,
    url = url,
    title = title,
    index = index,
    novelUrl = novelUrl
).apply {
    sourceUrl = this@toDomain.sourceUrl
    scanlationSource = this@toDomain.scanlationSource
    read = this@toDomain.read
}

fun Chapter.toEntity(): ChapterEntity = ChapterEntity(
    id = id,
    url = url,
    sourceUrl = sourceUrl,
    scanlationSource = scanlationSource,
    title = title,
    index = index,
    novelUrl = novelUrl,
    read = read
)
