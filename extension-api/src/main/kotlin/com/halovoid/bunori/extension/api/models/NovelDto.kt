package com.halovoid.bunori.extension.api.models

import kotlinx.serialization.Serializable

/**
 * Data transfer object representing novel details scraped by an extension.
 *
 * @property url Canonical URL of the novel landing page.
 * @property title Title of the novel.
 * @property author Author name(s).
 * @property coverUrl URL for the novel cover image.
 * @property description Full synopsis or description of the novel.
 * @property status Publication status (e.g. "Ongoing", "Completed", "Hiatus").
 * @property genres List of associated genres or tags.
 * @property chapters List of chapters belonging to this novel.
 * @property extra Additional arbitrary key-value metadata.
 */
@Serializable
data class NovelDto(
    val url: String,
    val title: String,
    val author: String? = null,
    val coverUrl: String? = null,
    val description: String? = null,
    val status: String? = null,
    val genres: List<String> = emptyList(),
    val chapters: List<ChapterDto> = emptyList(),
    val extra: Map<String, String> = emptyMap()
)
