package com.halovoid.bunori.api.core.network

import android.webkit.CookieManager
import com.halovoid.bunori.api.core.scrapper.CloudflareBlockedException
import com.halovoid.bunori.api.core.scrapper.CloudflareResolver
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

/**
 * Interceptor that detects Cloudflare challenges and triggers a resolver.
 */
class CloudflareInterceptor(
    private val cloudflareResolver: CloudflareResolver
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val urlString = originalRequest.url.toString()

        // 1. Attach cached domain-specific User-Agent if available
        val cachedUa = cloudflareResolver.getUserAgent(urlString)
        val requestWithUa = originalRequest.newBuilder()
            .header("User-Agent", cachedUa)
            .build()

        var response = chain.proceed(requestWithUa)

        // 2. Detect Cloudflare challenge (403 or 503 with CF server header)
        if (isCloudflareChallenge(response)) {
            response.close() // Close response body before retrying

            // Synchronously resolve via Coroutine block (this is safe because interceptor runs on background thread)
            val resolved = try {
                runBlocking {
                    cloudflareResolver.resolve(urlString)
                }
            } catch (e: Exception) {
                false
            }

            if (resolved) {
                // 3. Extract fresh cookies set by WebView for this domain
                val cookies = CookieManager.getInstance().getCookie(urlString)
                val freshUa = cloudflareResolver.getUserAgent(urlString)

                val newRequestBuilder = originalRequest.newBuilder()
                    .header("User-Agent", freshUa)

                if (!cookies.isNullOrEmpty()) {
                    newRequestBuilder.header("Cookie", cookies)
                }

                // Retry original request with solved Cloudflare credentials
                return chain.proceed(newRequestBuilder.build())
            } else {
                // If resolution failed or was cancelled, throw exception to block the request
                throw IOException("Cloudflare verification required", CloudflareBlockedException(urlString))
            }
        }

        return response
    }

    private fun isCloudflareChallenge(response: Response): Boolean {
        val code = response.code
        val serverHeader = response.header("Server") ?: ""
        
        // Typical Cloudflare challenge signatures
        val isCode = code == 403 || code == 503 || code == 429
        val isCfServer = serverHeader.contains("cloudflare", ignoreCase = true)
        
        if (isCode && isCfServer) return true
        
        // Fallback: peek body for CF markers if code is suspicious
        if (isCode) {
            val bodyPreview = response.peekBody(1024).string()
            return bodyPreview.contains("cf-ray", ignoreCase = true) || 
                   bodyPreview.contains("__cf_chl_opt", ignoreCase = true) ||
                   bodyPreview.contains("just a moment", ignoreCase = true)
        }
        
        return false
    }
}
