package com.halovoid.bunori.api.core.network

import android.util.Log
import okhttp3.CookieJar
import android.webkit.CookieManager
import okhttp3.Cookie
import okhttp3.HttpUrl

/**
 * Ensures all OkHttp calls (including WAMR Host calls and redirects across different domains)
 * automatically share cookies with webview
 */
class WebKitCookieJar: CookieJar {
    private val cookieManager: CookieManager
        get() = CookieManager.getInstance()

    companion object {
        private const val TAG = "WebKitCookieJar"
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val urlString = url.toString()
        val cookieHeader = cookieManager.getCookie(urlString)
        if (cookieHeader.isNullOrBlank()) {
            Log.d(TAG, "loadForRequest($url): CookieManager has NO cookies for this URL")
            return emptyList()
        }

        val cookies = cookieHeader.split(";").mapNotNull { raw ->
            val trimmed = raw.trim()
            if (trimmed.isEmpty()) null
            else Cookie.parse(url, trimmed)
        }
        Log.d(TAG, "loadForRequest($url): CookieManager raw='$cookieHeader' -> Parsed ${cookies.size} cookies: ${cookies.map { "${it.name}=${it.value}" }}")
        return cookies
    }

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        val urlString = url.toString()
        Log.d(TAG, "saveFromResponse($url): Saving ${cookies.size} cookies: ${cookies.map { "${it.name}=${it.value}" }}")
        for (cookie in cookies) {
            cookieManager.setCookie(urlString, cookie.toString())
        }
    }
}