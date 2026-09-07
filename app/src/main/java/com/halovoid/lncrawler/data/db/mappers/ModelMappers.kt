package com.halovoid.lncrawler.data.db.mappers

import com.halovoid.lncrawler.data.db.entities.ChapterEntity
import com.halovoid.lncrawler.data.db.entities.NovelEntity
import com.halovoid.lncrawler.data.db.entities.VolumeEntity
import com.halovoid.lncrawler.domain.models.Chapter
import com.halovoid.lncrawler.domain.models.Novel
import com.halovoid.lncrawler.domain.models.Volume

fun NovelEntity.toDomain(): Novel = Novel(
    url = url,
    title = title,
    author = author,
    coverUrl = coverUrl,
    description = description,
    crawlerName = crawlerName,
    alternativeNames = alternativeNames,
    chapters = emptyList(), // Chapters are usually loaded separately
    volumes = emptyList(),   // Volumes are usually loaded separately
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
    novelUrl = novelUrl,
    volumeId = volumeId,
    fileLocation = fileLocation
).apply {
    sourceUrl = this@toDomain.sourceUrl
    read = this@toDomain.read
}

fun Chapter.toEntity(): ChapterEntity = ChapterEntity(
    id = id,
    url = url,
    sourceUrl = sourceUrl,
    title = title,
    index = index,
    novelUrl = novelUrl,
    volumeId = volumeId,
    fileLocation = fileLocation,
    read = read
)

// --- Volume Mappings ---

fun VolumeEntity.toDomain(): Volume = Volume(
    id = id,
    volumeIndex = volumeIndex,
    novelUrl = novelUrl
)

fun Volume.toEntity(): VolumeEntity = VolumeEntity(
    id = id,
    volumeIndex = volumeIndex,
    novelUrl = novelUrl
)