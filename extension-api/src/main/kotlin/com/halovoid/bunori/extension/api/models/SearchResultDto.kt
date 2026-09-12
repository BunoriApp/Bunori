package com.halovoid.bunori.extension.api.models

import kotlinx.serialization.Serializable

/**
 * Data transfer object representing a novel search result or listing entry.
 *
 * @property url URL of the novel landing page.
 * @property title Title of the novel.
 * @property coverUrl URL for the novel cover image.
 * @property author Optional author name.
 */
@Serializable
data class SearchResultDto(
    val url: String,
    val title: String,
    val coverUrl: String? = null,
    val author: String? = null
)
