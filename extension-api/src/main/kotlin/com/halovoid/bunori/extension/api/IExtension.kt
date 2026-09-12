package com.halovoid.bunori.extension.api

import com.halovoid.bunori.extension.api.models.ExtensionMetadata
import com.halovoid.bunori.extension.api.models.ListingDto
import com.halovoid.bunori.extension.api.models.NovelDto
import com.halovoid.bunori.extension.api.models.SearchResultDto

/**
 * The standard interface implemented by all Bunori extensions.
 */
interface IExtension {
    /**
     * Source metadata (id, name, version, lang, baseUrl).
     */
    val metadata: ExtensionMetadata

    /**
     * Searches novels matching [query] at [page].
     */
    suspend fun search(query: String, page: Int = 1): List<SearchResultDto>

    /**
     * Fetches complete novel details (metadata and full chapter list).
     */
    suspend fun getNovelDetails(novelUrl: String): NovelDto

    /**
     * Fetches the text/HTML body content of a specific chapter.
     */
    suspend fun getChapterContent(chapterUrl: String): String?

    /**
     * Returns available explore/browse categories (e.g. "Popular", "Latest Updates").
     */
    fun getListings(): List<ListingDto> = emptyList()

    /**
     * Fetches novels for a specific listing category at [page].
     */
    suspend fun getListingNovels(listingId: String, page: Int = 1): List<SearchResultDto> = emptyList()
}
