package com.halovoid.bunori.api.core.config

data class CrawlerConfig (
    val userFolderLocation: String,
    val maxAttempts: Int,
    val ignoreImages: Boolean = false, // Ignore image to save bandwidth turned off by default
    val runnerConcurrency: Int = 3, // How many crawler Jobs run at the same time
    val runnerCooldown: Int = 1, // Short breaks between scheduler checks so that system is not busy all the time
    val maxSessionPerExit: Int = 0
)