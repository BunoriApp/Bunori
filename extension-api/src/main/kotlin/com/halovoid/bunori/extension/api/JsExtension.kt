package com.halovoid.bunori.extension.api

import com.halovoid.bunori.extension.api.models.ChapterDto
import com.halovoid.bunori.extension.api.models.ExtensionMetadata
import com.halovoid.bunori.extension.api.models.ListingDto
import com.halovoid.bunori.extension.api.models.NovelDto
import com.halovoid.bunori.extension.api.models.SearchResultDto

/**
 * Standard interface for interacting with an extension instance from the app.
 */
interface JsExtension {
    /**
     * Source metadata (id, name, version, lang, baseUrl).
     */
    val metadata: ExtensionMetadata

    /**
     * Searches novels matching [query] at [page].
     */
    suspend fun search(query: String, page: Int = 1): List<SearchResultDto>

    /**
     * Fetches novel metadata and the full chapter list.
     */
    suspend fun getNovelDetails(novelUrl: String): NovelDto

    /**
     * Fetches raw or cleaned HTML/text content of a specific chapter.
     */
    suspend fun getChapterContent(chapterUrl: String): String?

    /**
     * Optional: returns available explore/browse categories (e.g. "Latest Updates", "Popular").
     */
    suspend fun getListings(): List<ListingDto> = emptyList()

    /**
     * Optional: fetches novels from a specific listing category.
     */
    suspend fun getListingNovels(listingId: String, page: Int = 1): List<SearchResultDto> = emptyList()
}
