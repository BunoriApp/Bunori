package com.halovoid.bunori.ui.feature.crawler.cloudflare

import android.content.Context
import android.content.Intent
import android.util.Log
import android.webkit.WebSettings
import com.halovoid.bunori.api.core.scrapper.CloudflareResolver
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import androidx.core.content.edit
import androidx.core.net.toUri
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.milliseconds

class CloudflareResolverImpl(private val context: Context): CloudflareResolver {
    private val sharedPrefs = context.getSharedPreferences("cloudfare_prefs", Context.MODE_PRIVATE)

    companion object {
        private val activeResolutions = ConcurrentHashMap<String, CompletableDeferred<Boolean>>()
        private var instance: CloudflareResolverImpl? = null

        fun initialize(context: Context) {
            if (instance == null) {
                instance = CloudflareResolverImpl(context)
            }
        }

        fun getInstance(): CloudflareResolverImpl {
            return instance ?: throw IllegalStateException("CloudflareResolverImpl not initialized")
        }

        fun onResolutionResult(host: String, success: Boolean) {
            activeResolutions.remove(host)?.complete(success)
        }
    }

    override suspend fun resolve(url: String): Boolean = withContext(Dispatchers.IO) {
        val host = url.toUri().host ?: return@withContext false

        Log.i("CloudflareResolver", "Resolution requested for host: $host ($url)")
        activeResolutions[host]?.let { existingDeferred ->
            Log.i("CloudflarResolver", "Waiting for ongoing resolution for $host")
            return@withContext existingDeferred.await()
        }

        val deferred = CompletableDeferred<Boolean>()
        val previous = activeResolutions.putIfAbsent(host, deferred)
        if (previous != null) {
            return@withContext previous.await()
        }

        try {
            val intent = Intent(context, CloudflareActivity::class.java).apply {
                putExtra("url", url)
                putExtra("host", host)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(intent)

            val result = deferred.await()
            if (result) {
                delay(500.milliseconds)
            }
            result
        } catch (e: Exception) {
            Log.e("CloudflareResolver", "Failed to launch Cloudflare Activity for $host", e)
            activeResolutions.remove(host)
            false
        }
    }

    override fun getUserAgent(url: String?): String {
        val host = url?.toUri()?.host
        if (host != null) {
            val domainUa = sharedPrefs.getString("ua_$host", null)
            if (!domainUa.isNullOrBlank()) return domainUa
        }
        return WebSettings.getDefaultUserAgent(context)
    }

    fun saveUserAgent(host: String, userAgent: String) {
        sharedPrefs.edit {putString("ua_$host", userAgent)}
    }
}
