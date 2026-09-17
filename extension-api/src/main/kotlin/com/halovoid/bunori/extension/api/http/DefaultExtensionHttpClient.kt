package com.halovoid.bunori.extension.api.http

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.milliseconds

/**
 * Standard JVM implementation of [ExtensionHttpClient] backed by OkHttp.
 */
class DefaultExtensionHttpClient(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()
) : ExtensionHttpClient {

    private val defaultUserAgent =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

    private fun buildRequest(
        url: String,
        headers: Map<String, String>,
        builderAction: (Request.Builder) -> Unit = {}
    ): Request {
        val builder = Request.Builder().url(url)
        builder.header("User-Agent", defaultUserAgent)
        headers.forEach { (k, v) -> builder.header(k, v) }
        builderAction(builder)
        return builder.build()
    }

    override suspend fun get(
        url: String,
        headers: Map<String, String>
    ): Response = withContext(Dispatchers.IO) {
        val request = buildRequest(url, headers) { it.get() }
        client.newCall(request).execute()
    }

    override suspend fun post(
        url: String,
        headers: Map<String, String>,
        body: RequestBody?
    ): Response = withContext(Dispatchers.IO) {
        val request = buildRequest(url, headers) {
            if (body != null) it.post(body)
        }
        client.newCall(request).execute()
    }

    override suspend fun fetch(
        url: String,
        headers: Map<String, String>
    ): String? = withContext(Dispatchers.IO) {
        if (url.isBlank()) return@withContext null

        val maxAttempts = 3
        var currentAttempt = 1

        while (currentAttempt <= maxAttempts) {
            try {
                get(url, headers).use { response ->
                    val responseBody = response.body?.string()
                    if (response.isSuccessful) {
                        return@withContext responseBody
                    } else if (response.code in listOf(429, 500, 502, 503, 504) && currentAttempt < maxAttempts) {
                        delay((currentAttempt * 1000L).milliseconds)
                        currentAttempt++
                    } else {
                        return@withContext null
                    }
                }
            } catch (e: IOException) {
                if (currentAttempt < maxAttempts) {
                    delay((currentAttempt * 1000L).milliseconds)
                    currentAttempt++
                } else {
                    return@withContext null
                }
            } catch (e: Exception) {
                return@withContext null
            }
        }
        null
    }

    override suspend fun document(
        url: String,
        headers: Map<String, String>
    ): Document? {
        val html = fetch(url, headers) ?: return null
        return Jsoup.parse(html, url)
    }

    override suspend fun download(url: String): ByteArray? = withContext(Dispatchers.IO) {
        if (url.isBlank()) return@withContext null

        try {
            get(url).use { response ->
                if (response.isSuccessful) {
                    response.body?.bytes()
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            null
        }
    }
}
