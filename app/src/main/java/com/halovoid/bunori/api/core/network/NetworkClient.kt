package com.halovoid.bunori.api.core.network

import android.content.Context
import com.halovoid.bunori.ui.feature.crawler.cloudflare.CloudflareResolverImpl
import okhttp3.Cache
import okhttp3.Dns
import okhttp3.OkHttpClient
import java.io.File
import java.net.Inet4Address
import java.util.concurrent.TimeUnit

object NetworkClient {
    private var cache: Cache? = null

    val fastDns: Dns = object : Dns {
        override fun lookup(hostname: String): List<java.net.InetAddress> {
            return try {
                Dns.SYSTEM.lookup(hostname).sortedBy { if (it is Inet4Address) 0 else 1 }
            } catch (_: Exception) {
                Dns.SYSTEM.lookup(hostname)
            }
        }
    }

    fun init(context: Context) {
        val cacheSize = 5 * 1024 * 1024L // 5 MiB
        val cacheDirectory = File(context.cacheDir, "http_cache")
        cache = Cache(cacheDirectory, cacheSize)
    }

    val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .cache(cache)
            .dns(fastDns)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val originalRequest = chain.request()
                val request = if (originalRequest.header("User-Agent").isNullOrBlank()) {
                    originalRequest.newBuilder()
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                        .build()
                } else {
                    originalRequest
                }
                chain.proceed(request)
            }
            .addInterceptor(CloudflareInterceptor(CloudflareResolverImpl.getInstance()))
            .build()
    }
}
