package com.halovoid.bunori.domain.models

import kotlinx.serialization.Serializable

@Serializable
data class SearchItem(
    val title: String,
    val source: String,
    val url: String,
    val description: String,
    val score: Double,
    @kotlinx.serialization.SerialName("image_url") val imageUrl: String? = null
)
