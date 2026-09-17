package com.halovoid.bunori.api.core.scrapper

import android.util.Log
import com.halovoid.bunori.api.core.network.NetworkClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.io.IOException
import java.util.concurrent.TimeUnit

class CloudflareBlockedException(val url: String) : Exception("Cloudflare blocked the request to $url")

/**
 * Interface for resolving Cloudflare challenges.
 * Implemented in the :app module to show a WebView.
 */
interface CloudflareResolver {
    /**
     * Attempts to solve Cloudflare challenge for the given URL.
     * @return true if challenge was likely solved, false otherwise.
     */
    suspend fun resolve(url: String): Boolean

    /**
     * Returns the current User-Agent used by the resolver (e.g. from WebView).
     */
    fun getUserAgent(url: String?): String
}

/**
 * Handles the generic mechanics of communicating with websites.
 * Responsible for HTTP requests, session management (cookies), and HTML parsing.
 */
class Scrapper(
    private var client: OkHttpClient = NetworkClient.okHttpClient
) {
    companion object {
        var globalResolver: CloudflareResolver? = null
    }

    private var userAgent = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"

    /**
     * Fetches the content of a URL as a String with automatic retries for transient errors.
     * @param url The target URL.
     * @param headers Optional headers to add to the request.
     * @param body Optional request body for POST requests.
     * @param attempt Maximum number of attempts for retryable failures.
     * @param webviewNeeded DEPRECATED: Automatic detection handled by Interceptor.
     * @return The response body as a String, or null if the request fails.
     */
    suspend fun fetch(
        url: String,
        headers: Map<String, String> = emptyMap(),
        body: RequestBody? = null,
        attempt: Int = 3,
        webviewNeeded: Boolean = false
    ): String? = withContext(Dispatchers.IO) {
        if (url.isEmpty()) return@withContext null

        val maxAttempts = attempt.coerceAtLeast(1)
        var currentAttempt = 1

        while (currentAttempt <= maxAttempts) {
            // Sync with global resolver's User-Agent if available
            globalResolver?.getUserAgent(url)?.let {
                if (it.isNotEmpty()) userAgent = it
            }

            val builder = Request.Builder()
                .url(url)
                .header("User-Agent", userAgent)

            headers.forEach { (k, v) -> builder.header(k, v) }

            if (body != null) {
                builder.post(body)
            }

            val request = builder.build()

            try {
                client.newCall(request).execute().use { response ->
                    val responseBody = response.body?.string()
                    
                    if (response.isSuccessful) {
                        if (responseBody.isNullOrEmpty()) {
                            Log.w("Scrapper", "Empty successful response from $url")
                        }
                        return@withContext responseBody
                    } else if (response.code in listOf(429, 500, 502, 503, 504) && currentAttempt < maxAttempts) {
                        Log.w("Scrapper", "HTTP ${response.code} for $url. Retrying attempt ${currentAttempt + 1}/$maxAttempts...")
                        delay(currentAttempt * 1000L)
                        currentAttempt++
                    } else {
                        Log.e("Scrapper", "HTTP Error ${response.code} for $url. Body: $responseBody")
                        return@withContext null
                    }
                }
            } catch (e: IOException) {
                if (currentAttempt < maxAttempts) {
                    Log.w("Scrapper", "IO Error fetching from $url. Retrying attempt ${currentAttempt + 1}/$maxAttempts...", e)
                    delay(currentAttempt * 1000L)
                    currentAttempt++
                } else {
                    Log.e("Scrapper", "IO Error fetching from $url after $maxAttempts attempts", e)
                    return@withContext null
                }
            } catch (e: Exception) {
                Log.e("Scrapper", "Error fetching from $url", e)
                return@withContext null
            }
        }
        null
    }

    /**
     * Fetches and parses a URL into a Jsoup Document.
     * @param url The target URL.
     * @param headers Optional headers to add to the request.
     * @return A Jsoup Document or null if the request fails.
     */
    suspend fun document(
        url: String,
        headers: Map<String, String> = emptyMap(),
        webviewNeeded: Boolean = false
    ): Document? {
        val html = fetch(url, headers, webviewNeeded = webviewNeeded) ?: return null
        return Jsoup.parse(html, url)
    }

    /**
     * Downloads a resource from a URL as a ByteArray.
     * @param url The target URL.
     * @return The resource bytes, or null if the download fails.
     */
    suspend fun download(url: String): ByteArray? = withContext(Dispatchers.IO) {
        if (url.isEmpty()) return@withContext null

        val request = Request.Builder()
            .url(url)
            .header("User-Agent", userAgent)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    response.body?.bytes()
                } else {
                    Log.e("Scrapper", "HTTP Error ${response.code} downloading $url")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e("Scrapper", "Error downloading from $url", e)
            null
        }
    }
}