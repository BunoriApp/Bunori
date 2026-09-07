package com.halovoid.lncrawler.data.factory

import com.halovoid.lncrawler.data.db.entities.RequestEntity
import com.halovoid.lncrawler.data.db.entities.RequestStatus
import com.halovoid.lncrawler.data.db.entities.RequestType
import com.halovoid.lncrawler.domain.models.Chapter
import com.halovoid.lncrawler.domain.models.Novel
import com.halovoid.lncrawler.domain.models.SearchItem
import com.halovoid.lncrawler.ui.feature.novel.components.artifact.ExportFormat
import org.json.JSONObject

/**
 * Single source of truth for constructing [RequestEntity] objects.
 * Encapsulates JSON metadata serialization, ID generation, and type defaults.
 */
class RequestFactory {

    fun metadata(novel: Novel): RequestEntity {
        val metadata = JSONObject().apply {
            put("crawlerName", novel.crawlerName)
        }.toString()

        return RequestEntity(
            id = "${novel.url}_metadata",
            type = RequestType.NOVEL_METADATA,
            novelUrl = novel.url,
            name = "Metadata: ${novel.title}",
            metadata = metadata,
            status = RequestStatus.PENDING,
            rstatus = RequestStatus.PENDING,
            dependsOn = null,
            url = novel.url,
            priority = 0,
            completedAt = null,
            parentNovel = novel.url
        )
    }

    fun metadataFromSearchItem(item: SearchItem): RequestEntity {
        val metadata = JSONObject().apply {
            put("crawlerName", item.source)
        }.toString()

        return RequestEntity(
            id = "${item.url}_metadata",
            type = RequestType.NOVEL_METADATA,
            novelUrl = item.url,
            name = "Metadata: ${item.title}",
            metadata = metadata,
            status = RequestStatus.PENDING,
            rstatus = RequestStatus.PENDING,
            dependsOn = null,
            url = item.url,
            priority = 0,
            completedAt = null,
            parentNovel = null
        )
    }

    fun metadataFromUrl(crawlerName: String, url: String, title: String): RequestEntity {
        val metadata = JSONObject().apply {
            put("crawlerName", crawlerName)
        }.toString()

        return RequestEntity(
            id = "${url}_metadata",
            type = RequestType.NOVEL_METADATA,
            novelUrl = url,
            name = "Metadata: $title",
            metadata = metadata,
            status = RequestStatus.PENDING,
            rstatus = RequestStatus.PENDING,
            dependsOn = null,
            url = url,
            priority = 0,
            completedAt = null,
            parentNovel = null
        )
    }

    fun rangeDownload(novel: Novel, start: Int, end: Int, chapterCount: Int): RequestEntity {
        val metadata = JSONObject().apply {
            put("crawlerName", novel.crawlerName)
            put("startIndex", start)
            put("endIndex", end)
        }.toString()

        return RequestEntity(
            id = "${novel.url}_download_${start}_${end}",
            type = RequestType.RANGE_DOWNLOAD,
            novelUrl = novel.url,
            name = "Download: ${novel.title} ($start-$end)",
            metadata = metadata,
            parentNovel = novel.url,
            url = novel.url,
            status = RequestStatus.PENDING,
            rstatus = RequestStatus.PENDING,
            completedAt = null,
            progressTotal = chapterCount
        )
    }

    fun downloadAll(novel: Novel, totalCount: Int): RequestEntity {
        val metadata = JSONObject().apply {
            put("crawlerName", novel.crawlerName)
            put("startIndex", 1)
            put("endIndex", totalCount)
        }.toString()

        return RequestEntity(
            id = "${novel.url}_download_all",
            type = RequestType.RANGE_DOWNLOAD,
            novelUrl = novel.url,
            name = "Download All: ${novel.title}",
            metadata = metadata,
            parentNovel = novel.url,
            url = novel.url,
            status = RequestStatus.PENDING,
            rstatus = RequestStatus.PENDING,
            completedAt = null,
            progressTotal = totalCount
        )
    }

    fun downloadVolume(novel: Novel, volumeIndex: Int, start: Int, end: Int, totalCount: Int): RequestEntity {
        val metadata = JSONObject().apply {
            put("crawlerName", novel.crawlerName)
            put("startIndex", start)
            put("endIndex", end)
        }.toString()

        return RequestEntity(
            id = "${novel.url}_download_vol_${volumeIndex}",
            type = RequestType.RANGE_DOWNLOAD,
            novelUrl = novel.url,
            name = "Download: ${novel.title} Vol $volumeIndex",
            metadata = metadata,
            parentNovel = novel.url,
            url = novel.url,
            status = RequestStatus.PENDING,
            rstatus = RequestStatus.PENDING,
            completedAt = null,
            progressTotal = totalCount
        )
    }

    fun chapter(novel: Novel, chapter: Chapter): RequestEntity {
        val metadata = JSONObject().apply {
            put("chapterId", chapter.id)
            put("crawlerName", novel.crawlerName)
        }.toString()

        return RequestEntity(
            id = "${novel.url}_chapter_${chapter.index}",
            type = RequestType.CHAPTER,
            parentNovel = novel.url,
            dependsOn = null,
            priority = 10,
            name = "Chapter: ${chapter.title}",
            status = RequestStatus.PENDING,
            rstatus = RequestStatus.PENDING,
            completedAt = null,
            metadata = metadata,
            url = chapter.url,
            novelUrl = novel.url,
            progressTotal = 1,
            progressSuccess = 0
        )
    }

    fun export(novel: Novel, format: ExportFormat, start: Int, end: Int): RequestEntity {
        val metadata = JSONObject().apply {
            put("format", format.toString())
            put("crawlerName", novel.crawlerName)
            put("startIndex", start)
            put("endIndex", end)
        }.toString()

        return RequestEntity(
            id = "${novel.url}_export_${format}_${start}_${end}_${System.nanoTime()}",
            type = RequestType.ARTIFACT,
            novelUrl = novel.url,
            name = "Export: ${novel.title} ($format) [$start-$end]",
            metadata = metadata,
            parentNovel = novel.url,
            status = RequestStatus.PENDING,
            rstatus = RequestStatus.PENDING,
            url = null,
            dependsOn = null,
            completedAt = null
        )
    }
}
