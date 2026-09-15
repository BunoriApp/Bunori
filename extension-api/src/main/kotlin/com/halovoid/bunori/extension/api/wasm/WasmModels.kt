package com.halovoid.bunori.extension.api.wasm

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WasmHttpRequest(
    val url: String,
    val method: String = "GET",
    val headers: Map<String, String> = emptyMap(),
    val body: String? = null
)

@Serializable
data class WasmHttpResponse(
    @SerialName("status_code")
    val statusCode: Int,
    val headers: Map<String, String> = emptyMap(),
    val body: String = ""
)
