package com.halovoid.bunori.wasm

import android.util.Log
import com.halovoid.bunori.api.core.network.NetworkClient
import com.halovoid.bunori.extension.api.ExtensionJson
import com.halovoid.bunori.extension.api.wasm.WasmHttpRequest
import com.halovoid.bunori.extension.api.wasm.WasmHttpResponse
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

object WamrHttpBridge {
    private const val TAG = "WamrHttpBridge"

    @JvmStatic
    fun execute(requestJson: String): ByteArray {
        return try {
            val req = ExtensionJson.json.decodeFromString<WasmHttpRequest>(requestJson)
            val requestBuilder = Request.Builder().url(req.url)

            req.headers.forEach { (key, value) ->
                requestBuilder.header(key, value)
            }

            if (req.method.equals("POST", ignoreCase = true)) {
                val contentType = req.headers["Content-Type"]
                    ?: req.headers["content-type"]
                    ?: "application/x-www-form-urlencoded"
                val body = (req.body ?: "").toRequestBody(contentType.toMediaTypeOrNull())
                requestBuilder.post(body)
            } else {
                requestBuilder.get()
            }

            NetworkClient.okHttpClient.newCall(requestBuilder.build()).execute().use { response ->
                val responseHeaders = mutableMapOf<String, String>()
                for (i in 0 until response.headers.size) {
                    responseHeaders[response.headers.name(i)] = response.headers.value(i)
                }

                val httpResponse = WasmHttpResponse(
                    statusCode = response.code,
                    headers = responseHeaders,
                    body = response.body?.string() ?: ""
                )
                ExtensionJson.json.encodeToString(WasmHttpResponse.serializer(), httpResponse).toByteArray(Charsets.UTF_8)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error executing HTTP request in WamrHttpBridge: ${e.message}", e)
            val errorResponse = WasmHttpResponse(
                statusCode = 500,
                headers = emptyMap(),
                body = "Host HTTP bridge error: ${e.message}"
            )
            ExtensionJson.json.encodeToString(WasmHttpResponse.serializer(), errorResponse).toByteArray(Charsets.UTF_8)
        }
    }
}
