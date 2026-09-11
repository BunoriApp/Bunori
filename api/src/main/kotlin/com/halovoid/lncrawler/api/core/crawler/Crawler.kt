package com.halovoid.lncrawler.api.core.crawler

import android.net.Uri
import com.halovoid.lncrawler.api.core.config.CrawlerConfig
import com.halovoid.lncrawler.api.core.scrapper.Scrapper
import com.halovoid.lncrawler.domain.models.Chapter
import com.halovoid.lncrawler.domain.models.Novel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import okhttp3.RequestBody
import org.jsoup.nodes.Document
import java.io.IOException
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
     * and formatting chapter titles.
     */
    open fun prepareNovel(novel: Novel): Novel {
        val formattedTitle = formatTitle(novel.title)
        val formattedAuthor = novel.author
            ?.split(",")
            ?.map { formatTitle(it.trim()) }
            ?.filter { it.isNotBlank() }
            ?.joinToString(", ")

        // Enforce formatting on domain chapters without re-indexing
        val chapters = novel.chapters.map { chapter ->
            chapter.copy(
                title = formatTitle(chapter.title).ifBlank { "Chapter ${chapter.index}" },
                index = chapter.index
            ).apply {
                sourceUrl = chapter.sourceUrl ?: chapter.url
                scanlationSource = chapter.scanlationSource
            }
        }

        return novel.copy(
            title = formattedTitle,
            author = formattedAuthor,
            chapters = chapters
        )
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
     * Helper to fetch chapters across multiple pages, tabs, or subgroups concurrently with concurrency control.
     * Throws an exception if any group fails, avoiding partial/cut-off chapter lists.
     */
    protected suspend fun <T> fetchGroupedChapters(
        groups: List<T>,
        concurrency: Int = config.runnerConcurrency,
        fetchGroup: suspend (group: T) -> List<Chapter>
    ): List<Chapter> {
        if (groups.isEmpty()) return emptyList()

        val semaphore = Semaphore(concurrency.coerceAtLeast(1))

        val groupedResults = coroutineScope {
            groups.map { group ->
                async(Dispatchers.IO) {
                    semaphore.withPermit {
                        fetchGroup(group)
                    }
                }
            }.awaitAll()
        }

        return groupedResults.flatten()
    }

    /**
     * Convenience wrapper around [fetchGroupedChapters] for numbered pages (1..N).
     * Composes directly on top of [fetchGroupedChapters] using an IntRange.
     *
     * @param totalPages The total number of pages to fetch.
     * @param startPage The first page number (default 1).
     * @param initialChapters Optional list of chapters already obtained from the first page (e.g. from novel landing page).
     *                        If provided, startPage will automatically advance to 2 so page 1 is not re-fetched.
     * @param concurrency The maximum number of concurrent HTTP requests (defaults to config.runnerConcurrency).
     * @param fetchPage A suspend lambda returning the chapters for a given page index.
     * @return Ordered, flattened list of chapters. Throws an IOException if any page fails.
     */
    protected suspend fun fetchPaginatedChapters(
        totalPages: Int,
        startPage: Int = 1,
        initialChapters: List<Chapter> = emptyList(),
        concurrency: Int = config.runnerConcurrency,
        fetchPage: suspend (page: Int) -> List<Chapter>
    ): List<Chapter> {
        val actualStart = if (initialChapters.isNotEmpty() && startPage == 1) 2 else startPage
        if (actualStart > totalPages) return initialChapters

        val pageNumbers = (actualStart..totalPages).toList()
        return initialChapters + fetchGroupedChapters(pageNumbers, concurrency, fetchPage)
    }

    /**
     * Helper to fetch chapters sequentially when pagination is cursor-based or requires following a "Next" page URL.
     */
    protected suspend fun fetchCursorChapters(
        initialUrl: String,
        maxPages: Int = 1000,
        fetchNext: suspend (currentUrl: String) -> Pair<List<Chapter>, String?>
    ): List<Chapter> {
        val allChapters = mutableListOf<Chapter>()
        var currentUrl: String? = initialUrl
        var pageCount = 0
        val visited = mutableSetOf<String>()

        while (!currentUrl.isNullOrBlank() && pageCount < maxPages) {
            if (!visited.add(currentUrl)) break // Prevent circular loops
            pageCount++
            val (chapters, nextUrl) = fetchNext(currentUrl)
            allChapters.addAll(chapters)
            currentUrl = nextUrl
        }

        return allChapters
    }

    /**
     * Sanitizes, deduplicates by URL, and numbers a list of chapters sequentially (1..N).
     * Automatically assigns scanlationSource if not already provided.
     */
    protected fun finalizeChapterList(chapters: List<Chapter>): List<Chapter> {
        return chapters.distinctBy { it.url }.mapIndexed { index, chapter ->
            chapter.copy(
                index = index + 1,
            ).apply {
                if (scanlationSource.isBlank() || scanlationSource == "Not Provided") {
                    scanlationSource = this@Crawler.name
                }
            }
        }
    }

    /**
     * Downloads an image and returns its bytes.
     */
    suspend fun downloadImage(url: String): ByteArray? = scrapper.download(url)
}