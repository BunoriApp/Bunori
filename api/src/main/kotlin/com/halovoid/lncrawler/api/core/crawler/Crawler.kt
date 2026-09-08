package com.halovoid.lncrawler.api.core.crawler

import android.net.Uri
import com.halovoid.lncrawler.api.core.config.CrawlerConfig
import com.halovoid.lncrawler.api.core.scrapper.Scrapper
import com.halovoid.lncrawler.domain.models.Novel
import com.halovoid.lncrawler.domain.models.Volume
import okhttp3.RequestBody
import org.jsoup.nodes.Document
import kotlin.math.ceil
import kotlin.math.max

/**
 * Base abstract class for all source crawlers in the Data layer.
 * Inspired by the logic in lightnovel-crawler (Python).
 *
 * Provides utility methods for fetching HTML, resolving absolute URLs,
 * and defines the interface for site-specific implementations using Jsoup for scraping.
 */
abstract class Crawler {
    protected var _config: CrawlerConfig? = null

    /** Current configuration for the crawler. */
    open val config: CrawlerConfig
        get() = _config ?: CrawlerConfig(userFolderLocation = "", maxAttempts = 3)

    /** Initializes the crawler with its configuration. */
    fun initialize(config: CrawlerConfig) {
        this._config = config
    }
    private val version = 1
    /** The display name of the source (e.g., "NovelBin") */
    abstract val name: String

    /** The base URL of the source (e.g., "https://novelbins.com") */
    abstract val baseUrl: String

    /** Language of the novels on this site (e.g., "en") */
    open val language: String = "en"

    /** Volume size limit for how many chapters to go in one volume
     */
    open val chapterPerVolume: Int = 100

    /** Opens a webview on phone to extract all the cookies and website headers
     * This is only required if the crawler can't crawl the website normally and needs
     * to bypass Cloudflare or some other protection
     */
    abstract val webviewNeeded: Boolean?

    /** Generic HTTP and scraping utility */
    protected val scrapper = Scrapper()

    protected fun maxWorkers(): Int {
        return max(1, config.maxSessionPerExit) + 1
    }

    /**
     * Determines if this crawler can handle the given URL.
     * @param url The URL to check.
     * @return true if the URL belongs to this source.
     */
    abstract fun canHandle(url: String): Boolean

    /**
     * Scrapes only the novel metadata (title, author, cover, description) from the source.
     * @param novelUrl The URL of the novel landing page.
     * @return A [com.halovoid.lncrawler.domain.models.Novel] object without chapters.
     */
    open suspend fun getNovelMetadata(novelUrl: String): Novel {
        return getNovelDetails(novelUrl).copy(chapters = emptyList())
    }

    /**
     * Scrapes only the chapter list for a novel.
     * @param novelUrl The URL of the novel landing page.
     * @return A list of [com.halovoid.lncrawler.domain.models.Chapter] objects.
     */
    open suspend fun getChapterList(novelUrl: String): List<com.halovoid.lncrawler.domain.models.Chapter> {
        return getNovelDetails(novelUrl).chapters
    }

    /**
     * Scrapes the novel details (metadata and chapter list) from the source.
     * @param novelUrl The URL of the novel landing page.
     * @return A [com.halovoid.lncrawler.domain.models.Novel] object populated with metadata and chapters.
     */
    abstract suspend fun getNovelDetails(novelUrl: String): Novel

    /**
     * Scrapes the content of a specific chapter.
     * @param chapterUrl The URL of the chapter page.
     * @return The HTML content of the chapter body.
     */
    abstract suspend fun getChapterContent(chapterUrl: String): String?

    abstract suspend fun getSearchResults(query: String): List<Novel>

    /**
     * Fetches HTML from a URL with a standard User-Agent.
     * @param response Name of the Response.
     * @param body Response body for requests.
     * @return Whether to stop the crawler ot not.
     */
    open fun checkResponse(response: String = "Response", body: String) {}

    /**
     * Fetches HTML from a URL with a standard User-Agent.
     * @param usernameOrEmail username or email for website login.
     * @param passwordOrToken password or token for the website login.
     * @return Logs into the website.
     */
    open fun login(usernameOrEmail: String, passwordOrToken: String) {}

    open fun downloadImage(url: String, outputFile: Uri) {}

    open suspend fun downloadCover(url: String) : ByteArray? {
        if (url.isBlank()) {
            throw Exception("No Download URL provided for Cover")
        }

        return scrapper.download(url)
    }

    open fun formatTitle(title: String): String {
        return title.trim().replace(Regex("\\s+"), " ")
    }

    open fun getNovelKey(url: String): String {
        val slug = url.trimEnd('/').split('/').last()
        return "${name.lowercase()}_$slug".filter { it.isLetterOrDigit() || it == '_' || it == '-' }
    }
    /**
     * Prepares a novel by formatting its title and author names,
     * and organizing chapters into volumes.
     */
    open fun prepareNovel(novel: Novel): Novel {
        val formattedTitle = formatTitle(novel.title)
        val formattedAuthor = novel.author
            ?.split(",")
            ?.map { formatTitle(it.trim()) }
            ?.filter { it.isNotBlank() }
            ?.joinToString(", ")

        val volumes = createVolumes(novel)

        // Enforce formatting and volume assignment on domain chapters without re-indexing
        val chapters = novel.chapters.map { chapter ->
            val volumeIndex = ((chapter.index - 1).coerceAtLeast(0) / chapterPerVolume) + 1
            chapter.copy(
                title = formatTitle(chapter.title).ifBlank { "Chapter ${chapter.index}" },
                index = chapter.index,
                volumeId = "${novel.url}_vol_${volumeIndex}"
            ).apply {
                sourceUrl = chapter.sourceUrl ?: chapter.url
                scanlationSource = chapter.scanlationSource
            }
        }

        return novel.copy(
            title = formattedTitle,
            author = formattedAuthor,
            volumes = volumes,
            chapters = chapters
        )
    }

    fun createVolumes(novel: Novel): List<Volume> {
        val totalChapters = novel.chapters.size

        if (totalChapters == 0) {
            return emptyList()
        }

        val totalVolumes =
            ceil(totalChapters.toDouble() / chapterPerVolume).toInt()

        return (1..totalVolumes).map { volumeIndex ->
            Volume(
                id = "${novel.url}_vol_${volumeIndex}",
                volumeIndex = volumeIndex,
                novelUrl = novel.url
            )
        }
    }

    /**
     * Fetches HTML from a URL with a standard User-Agent.
     * @param url The target URL.
     * @param headers Optional headers to add to the request.
     * @param body Optional request body for POST requests.
     * @return The HTML string or null if the request fails.
     */
    protected suspend fun fetchHtml(
        url: String,
        headers: Map<String, String> = emptyMap(),
        body: RequestBody? = null
    ): String? = scrapper.fetch(url, headers, body, webviewNeeded = webviewNeeded ?: false)

    /**
     * Supports compatibility for older crawlers where webview is not given
     */
    protected suspend fun getDocument(url: String): Document? {
        return getDocument(url, webviewNeeded = false)
    }

    /**
     * Newer crawlers with webview parameters
     */
    protected suspend fun getDocument(url: String, webviewNeeded: Boolean): Document? {
        return scrapper.document(url, webviewNeeded = webviewNeeded)
    }

    /**
     * Utility to resolve a relative URL to an absolute one.
     */
    protected fun absoluteUrl(relativeUrl: String, base: String = baseUrl): String {
        if (relativeUrl.startsWith("http")) return relativeUrl
        return if (relativeUrl.startsWith("/")) {
            base.trimEnd('/') + relativeUrl
        } else {
            base.trimEnd('/') + "/" + relativeUrl
        }
    }

    /**
     * Clean chapter content by removing scripts, styles, and ads.
     */
    protected fun cleanHtml(doc: Document, selector: String): String {
        val content = doc.select(selector).first() ?: return ""

        // Generic cleaning logic
        content.select("script, style, ins, .adsbygoogle, .hidden, [style*='display:none']").remove()
        content.select("div:not(:has(p))").remove()

        return content.html().trim()
    }

    /**
     * Downloads an image and returns its bytes.
     */
    suspend fun downloadImage(url: String): ByteArray? = scrapper.download(url)
}