package com.halovoid.bunori.api.core.network

import android.content.Context
import okhttp3.Cache
import okhttp3.OkHttpClient
import java.io.File
import java.util.concurrent.TimeUnit

object NetworkClient {
    private var cache: Cache? = null

    fun init(context: Context) {
        val cacheSize = 5 * 1024 * 1024L // 5 MiB
        val cacheDirectory = File(context.cacheDir, "http_cache")
        cache = Cache(cacheDirectory, cacheSize)
    }

    val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .cache(cache) // Enables caching automatically!
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", "Bunori")
                    .build()
                chain.proceed(request)
            }
            .build()
    }
}
