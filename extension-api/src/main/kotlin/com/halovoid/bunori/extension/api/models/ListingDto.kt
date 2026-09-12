package com.halovoid.bunori.extension.api.models

import kotlinx.serialization.Serializable

/**
 * Represents a browse listing category provided by an extension (e.g. "Latest Updates", "Popular", "Top Rated").
 *
 * @property id Identifier for the listing (passed to getListingNovels).
 * @property name User-facing title of the listing.
 */
@Serializable
data class ListingDto(
    val id: String,
    val name: String
)
