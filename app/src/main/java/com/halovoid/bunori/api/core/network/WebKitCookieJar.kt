package com.halovoid.bunori.api.core.network

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

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val urlString = url.toString()
        val cookieHeader = cookieManager.getCookie(urlString) ?: return emptyList()

        return cookieHeader.split(";").mapNotNull { raw ->
            val trimmed = raw.trim()
            if (trimmed.isEmpty()) null
            else Cookie.parse(url, trimmed)
        }
    }
    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        val urlString = url.toString()
        for (cookie in cookies) {
            cookieManager.setCookie(urlString, cookie.toString())
        }
    }
}