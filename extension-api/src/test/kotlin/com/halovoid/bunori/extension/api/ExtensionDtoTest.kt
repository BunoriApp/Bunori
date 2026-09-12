package com.halovoid.bunori.extension.api

import com.halovoid.bunori.extension.api.bridge.HtmlElementDto
import com.halovoid.bunori.extension.api.bridge.HttpResponseDto
import com.halovoid.bunori.extension.api.models.ChapterDto
import com.halovoid.bunori.extension.api.models.ExtensionMetadata
import com.halovoid.bunori.extension.api.models.ListingDto
import com.halovoid.bunori.extension.api.models.NovelDto
import com.halovoid.bunori.extension.api.models.SearchResultDto
import kotlinx.serialization.encodeToString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ExtensionDtoTest {

    @Test
    fun testMetadataSerialization() {
        val meta = ExtensionMetadata(
            id = "novelfull",
            name = "NovelFull",
            version = 1,
            apiVersion = 1,
            lang = "en",
            baseUrl = "https://novelfull.com"
        )

        val jsonStr = ExtensionJson.json.encodeToString(meta)
        val decoded = ExtensionJson.json.decodeFromString<ExtensionMetadata>(jsonStr)

        assertEquals(meta.id, decoded.id)
        assertEquals(meta.name, decoded.name)
        assertEquals(meta.version, decoded.version)
        assertEquals(meta.apiVersion, decoded.apiVersion)
        assertEquals(meta.lang, decoded.lang)
        assertEquals(meta.baseUrl, decoded.baseUrl)
        assertNull(decoded.iconUrl)
    }

    @Test
    fun testNovelDtoSerializationWithUnknownKeys() {
        val rawJson = """
            {
                "url": "https://novelbins.com/b/test-novel",
                "title": "Test Novel",
                "author": "Test Author",
                "coverUrl": "https://novelbins.com/cover.jpg",
                "description": "Synopsis here",
                "status": "Ongoing",
                "genres": ["Action", "Fantasy"],
                "unknown_extra_field": 12345,
                "chapters": [
                    {
                        "url": "https://novelbins.com/b/test-novel/c1",
                        "title": "Chapter 1: The Beginning",
                        "index": 1,
                        "releaseDate": "2026-01-01",
                        "random_field": true
                    }
                ]
            }
        """.trimIndent()

        val decoded = ExtensionJson.json.decodeFromString<NovelDto>(rawJson)

        assertEquals("https://novelbins.com/b/test-novel", decoded.url)
        assertEquals("Test Novel", decoded.title)
        assertEquals("Test Author", decoded.author)
        assertEquals(2, decoded.genres.size)
        assertEquals("Action", decoded.genres[0])
        assertEquals(1, decoded.chapters.size)
        assertEquals("Chapter 1: The Beginning", decoded.chapters[0].title)
        assertEquals(1, decoded.chapters[0].index)
    }

    @Test
    fun testSearchResultAndListingDto() {
        val searchItem = SearchResultDto(
            url = "https://example.com/novel-1",
            title = "Sample Title",
            coverUrl = "https://example.com/cover.png",
            author = "Author Name"
        )
        val searchJson = ExtensionJson.json.encodeToString(searchItem)
        val decodedSearch = ExtensionJson.json.decodeFromString<SearchResultDto>(searchJson)
        assertEquals(searchItem, decodedSearch)

        val listing = ListingDto(id = "latest", name = "Latest Novels")
        val listingJson = ExtensionJson.json.encodeToString(listing)
        val decodedListing = ExtensionJson.json.decodeFromString<ListingDto>(listingJson)
        assertEquals(listing, decodedListing)
    }

    @Test
    fun testBridgeDtos() {
        val httpResponse = HttpResponseDto(
            statusCode = 200,
            body = "<html><body><h1>Hello</h1></body></html>",
            headers = mapOf("content-type" to "text/html")
        )
        val httpJson = ExtensionJson.json.encodeToString(httpResponse)
        val decodedHttp = ExtensionJson.json.decodeFromString<HttpResponseDto>(httpJson)
        assertEquals(200, decodedHttp.statusCode)
        assertEquals("text/html", decodedHttp.headers["content-type"])

        val htmlElement = HtmlElementDto(
            text = "Hello",
            html = "<h1>Hello</h1>",
            outerHtml = "<div><h1>Hello</h1></div>",
            attributes = mapOf("class" to "greeting")
        )
        val htmlJson = ExtensionJson.json.encodeToString(htmlElement)
        val decodedHtml = ExtensionJson.json.decodeFromString<HtmlElementDto>(htmlJson)
        assertEquals("Hello", decodedHtml.text)
        assertEquals("greeting", decodedHtml.attributes["class"])
    }
}
