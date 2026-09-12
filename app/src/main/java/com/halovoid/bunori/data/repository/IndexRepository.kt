package com.halovoid.bunori.data.repository

import android.util.Log
import com.halovoid.bunori.api.core.network.NetworkClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class IndexRepository(
    private val client: OkHttpClient = NetworkClient.okHttpClient
) {
    suspend fun index(url: String) = withContext(Dispatchers.IO) {
        val requestBody = """
            {
                "url": "$url"
            }
        """.trimIndent()
            .toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("https://lncs.up.railway.app/api/index/request")
            .post(requestBody)
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                Log.e("RedisManager", "Failed to push $url to redis")
                throw Exception("Server is down or under maintenance")
            }
            Log.i("RedisManager", "Pushed $url to redis")
        }
    }
}