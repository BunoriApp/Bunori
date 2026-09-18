package com.halovoid.bunori.data.handlers.utility

import com.halovoid.bunori.api.core.crawler.CrawlerFactory
import com.halovoid.bunori.data.db.entities.BatchEntity
import com.halovoid.bunori.data.db.entities.TaskEntity
import com.halovoid.bunori.domain.models.Batch
import com.halovoid.bunori.data.scheduler.RequestMetadata
import org.json.JSONObject

val Batch.parsedMetadata: RequestMetadata
    get() {
        if (this.metadata.isNullOrBlank()) return RequestMetadata()
        return try {
            val json = JSONObject(this.metadata)
            RequestMetadata(
                crawlerName = json.optString("crawlerName", null),
                artifactFormat = json.optString("artifactFormat", null),
                chapterId = json.optInt("chapterId", -1).takeIf { it != -1 },
                format = json.optString("format", null),
                startIndex = json.optInt("startIndex", -1).takeIf { it != -1 },
                endIndex = json.optInt("endIndex", -1).takeIf { it != -1 },
            )
        } catch (e: Exception) {
            RequestMetadata()
        }
    }

val Batch.crawlerName: String?
    get() = parsedMetadata.crawlerName?.takeIf { it.isNotBlank() }
        ?: novelUrl.let { CrawlerFactory.getCrawlerByUrl(it)?.name }

val BatchEntity.parsedMetadata: RequestMetadata
    get() {
        if (this.metadata.isNullOrBlank()) return RequestMetadata()
        return try {
            val json = JSONObject(this.metadata)
            RequestMetadata(
                crawlerName = json.optString("crawlerName", null),
                artifactFormat = json.optString("artifactFormat", null),
                chapterId = json.optInt("chapterId", -1).takeIf { it != -1 },
                format = json.optString("format", null),
                startIndex = json.optInt("startIndex", -1).takeIf { it != -1 },
                endIndex = json.optInt("endIndex", -1).takeIf { it != -1 },
            )
        } catch (e: Exception) {
            RequestMetadata()
        }
    }

val BatchEntity.crawlerName: String?
    get() = parsedMetadata.crawlerName?.takeIf { it.isNotBlank() }
        ?: novelUrl.let { CrawlerFactory.getCrawlerByUrl(it)?.name }

val TaskEntity.parsedMetadata: RequestMetadata
    get() {
        if (this.metadata.isNullOrBlank()) return RequestMetadata()
        return try {
            val json = JSONObject(this.metadata)
            RequestMetadata(
                crawlerName = json.optString("crawlerName", null),
                artifactFormat = json.optString("artifactFormat", null),
                chapterId = json.optInt("chapterId", -1).takeIf { it != -1 },
                format = json.optString("format", null),
                startIndex = json.optInt("startIndex", -1).takeIf { it != -1 },
                endIndex = json.optInt("endIndex", -1).takeIf { it != -1 },
            )
        } catch (e: Exception) {
            RequestMetadata()
        }
    }

val TaskEntity.crawlerName: String?
    get() = parsedMetadata.crawlerName?.takeIf { it.isNotBlank() }
        ?: url?.let { CrawlerFactory.getCrawlerByUrl(it)?.name }
        ?: novelUrl.let { CrawlerFactory.getCrawlerByUrl(it)?.name }