package com.halovoid.bunori.extension.api.http

import okhttp3.RequestBody
import okhttp3.Response
import org.jsoup.nodes.Document

/**
 * Interface for network and HTML scraping operations passed to extensions.
 *
 * Provides safe HTTP execution (connection pooling, User-Agent, and Cloudflare cookie handling)
 * and Jsoup document parsing.
 */
interface ExtensionHttpClient {

    /**
     * Executes a GET request and returns the raw OkHttp [Response].
     */
    suspend fun get(
        url: String,
        headers: Map<String, String> = emptyMap()
    ): Response

    /**
     * Executes a POST request and returns the raw OkHttp [Response].
     */
    suspend fun post(
        url: String,
        headers: Map<String, String> = emptyMap(),
        body: RequestBody? = null
    ): Response

    /**
     * Fetches the response body from [url] as a [String], or null if failed.
     */
    suspend fun fetch(
        url: String,
        headers: Map<String, String> = emptyMap()
    ): String?

    /**
     * Fetches and parses the HTML from [url] into a Jsoup [Document], or null if failed.
     */
    suspend fun document(
        url: String,
        headers: Map<String, String> = emptyMap()
    ): Document?

    /**
     * Downloads binary content (e.g. cover images) as a [ByteArray].
     */
    suspend fun download(url: String): ByteArray?

    /**
     * Cleans an HTML element by removing script, style, and ad tags, returning sanitized HTML.
     */
    fun cleanHtml(
        doc: Document,
        selector: String,
        removeSelectors: List<String> = listOf("script", "style", "ins", ".adsbygoogle", ".hidden", "[style*='display:none']")
    ): String {
        val element = doc.select(selector).first() ?: return ""
        val clone = element.clone()
        for (removeSelector in removeSelectors) {
            clone.select(removeSelector).remove()
        }
        return clone.html().trim()
    }

    /**
     * Resolves a relative URL to an absolute URL given a base URL.
     */
    fun absoluteUrl(relativeUrl: String, baseUrl: String): String {
        if (relativeUrl.startsWith("http://") || relativeUrl.startsWith("https://")) {
            return relativeUrl
        }
        val cleanBase = baseUrl.trimEnd('/')
        return if (relativeUrl.startsWith("/")) {
            cleanBase + relativeUrl
        } else {
            "$cleanBase/$relativeUrl"
        }
    }
}
