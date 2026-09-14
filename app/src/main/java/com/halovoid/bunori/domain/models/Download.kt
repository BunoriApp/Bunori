package com.halovoid.bunori.domain.models

data class Download(
    val id: Long = 0,
    val novelUrl: String,
    val chapterUrl: String,
    val fileLocation: String,
    val chapterIndex: Int,
    val chapterTitle: String,
    val scanlationSource: String = "Not Provided",
    val novelTitle: String,
    val sizeBytes: Long = 0,
    val downloadedAt: Long = System.currentTimeMillis()
)
