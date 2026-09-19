package com.halovoid.bunori.extension.api.models

import kotlinx.serialization.Serializable

/**
 * Data transfer object representing a chapter parsed by an extension.
 *
 * @property url Unique URL or path of the chapter page.
 * @property title Display title of the chapter (e.g. "Chapter 1: The Beginning").
 * @property index Ordered 0-based or 1-based index representing the chapter number. Defaults to -1 if unassigned.
 * @property releaseDate Optional release timestamp or human-readable date.
 * @property scanlation Optional scanlation team or source attribution.
 */
@Serializable
data class ChapterDto(
    val url: String,
    val title: String,
    val index: Int = -1,
    val releaseDate: String? = null,
    val scanlation: String? = null
)
