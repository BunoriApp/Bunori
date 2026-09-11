package com.halovoid.lncrawler.data.handlers.utility

import com.halovoid.lncrawler.data.db.entities.RequestEntity
import com.halovoid.lncrawler.domain.models.Request
import com.halovoid.lncrawler.data.scheduler.RequestMetadata
import org.json.JSONObject

val Request.parsedMetadata: RequestMetadata
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

val RequestEntity.parsedMetadata: RequestMetadata
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