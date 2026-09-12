package com.halovoid.bunori.extension.api.bridge

import kotlinx.serialization.Serializable

/**
 * Host HTML/DOM parser bridge exposed to extension scripts.
 * Enables ultra-fast native Jsoup CSS selector queries without running a bulky JS DOM parser.
 */
interface HtmlBridge {
    /**
     * Queries all elements matching [selector] in [html].
     */
    fun select(html: String, selector: String): List<HtmlElementDto>

    /**
     * Queries the first element matching [selector] in [html].
     */
    fun selectFirst(html: String, selector: String): HtmlElementDto?

    /**
     * Extracts text content of the first element matching [selector].
     */
    fun text(html: String, selector: String): String?

    /**
     * Extracts an attribute value of the first element matching [selector].
     */
    fun attr(html: String, selector: String, attribute: String): String?

    /**
     * Cleans an HTML string by removing unwanted elements (ads, scripts, styles).
     */
    fun clean(html: String, removeSelectors: List<String> = listOf("script", "style", "ins", ".adsbygoogle")): String
}

/**
 * Represents a DOM element extracted by [HtmlBridge].
 */
@Serializable
data class HtmlElementDto(
    val text: String,
    val html: String,
    val outerHtml: String,
    val attributes: Map<String, String> = emptyMap()
)
