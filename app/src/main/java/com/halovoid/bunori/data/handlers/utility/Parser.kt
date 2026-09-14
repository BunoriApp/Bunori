package com.halovoid.bunori.data.handlers.utility

import com.halovoid.bunori.data.db.entities.BatchEntity
import com.halovoid.bunori.data.db.entities.TaskEntity
import com.halovoid.bunori.domain.models.Request
import com.halovoid.bunori.data.scheduler.RequestMetadata
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