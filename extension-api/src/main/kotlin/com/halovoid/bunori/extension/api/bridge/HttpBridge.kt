package com.halovoid.bunori.extension.api.bridge

import kotlinx.serialization.Serializable

/**
 * Host HTTP bridge exposed to extension scripts.
 */
interface HttpBridge {
    /**
     * Performs an asynchronous GET request.
     */
    suspend fun get(url: String, headers: Map<String, String> = emptyMap()): HttpResponseDto

    /**
     * Performs an asynchronous POST request.
     */
    suspend fun post(url: String, headers: Map<String, String> = emptyMap(), body: String? = null): HttpResponseDto
}

/**
 * Encapsulates the HTTP response returned to the extension.
 */
@Serializable
data class HttpResponseDto(
    val statusCode: Int,
    val body: String,
    val headers: Map<String, String> = emptyMap()
)
