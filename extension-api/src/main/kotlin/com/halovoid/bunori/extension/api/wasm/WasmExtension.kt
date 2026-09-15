package com.halovoid.bunori.extension.api.wasm

import com.dylibso.chicory.runtime.HostFunction
import com.dylibso.chicory.runtime.Instance
import com.dylibso.chicory.runtime.Store
import com.dylibso.chicory.wasm.Parser
import com.dylibso.chicory.wasm.types.FunctionType
import com.dylibso.chicory.wasm.types.ValType
import com.halovoid.bunori.extension.api.ExtensionJson
import com.halovoid.bunori.extension.api.IExtension
import com.halovoid.bunori.extension.api.http.DefaultExtensionHttpClient
import com.halovoid.bunori.extension.api.http.ExtensionHttpClient
import com.halovoid.bunori.extension.api.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream
import java.util.logging.Level
import java.util.logging.Logger

/**
 * Universal WebAssembly runner for Bunori extensions.
 *
 * Implements [IExtension] for typed access and provides raw JSON methods
 * (`searchJson`, `getNovelDetailsJson`, etc.) for third-party apps that map
 * results to their own domain models.
 */
class WasmExtension(
    val manifest: ExtensionManifest,
    wasmSource: Any, // File, InputStream, or ByteArray
    private val httpClient: ExtensionHttpClient = DefaultExtensionHttpClient(),
    private val logger: ((level: Int, tag: String, message: String) -> Unit)? = null
) : IExtension {

    companion object {
        private const val TAG = "WasmExtension"
        private val jvmLogger = Logger.getLogger(TAG)
    }

    override val metadata: ExtensionMetadata = manifest.toMetadata()

    private val mutex = Mutex()
    private val instance: Instance

    init {
        val wasmModule = when (wasmSource) {
            is File -> Parser.parse(wasmSource)
            is InputStream -> Parser.parse(wasmSource)
            is ByteArray -> Parser.parse(ByteArrayInputStream(wasmSource))
            else -> throw IllegalArgumentException("Unsupported WASM source type: ${wasmSource.javaClass}")
        }

        val hostHttp = HostFunction(
            "bunori",
            "host_http",
            FunctionType.of(listOf(ValType.I32, ValType.I32), listOf(ValType.I64))
        ) { inst, args ->
            val reqPtr = args[0].toInt()
            val reqLen = args[1].toInt()
            if (reqLen <= 0 || reqPtr == 0) {
                return@HostFunction longArrayOf(0L)
            }

            try {
                val reqBytes = inst.memory().readBytes(reqPtr, reqLen)
                val reqJson = String(reqBytes, Charsets.UTF_8)
                val request = ExtensionJson.json.decodeFromString<WasmHttpRequest>(reqJson)

                val response = executeHttpRequest(request)
                val resJson = ExtensionJson.json.encodeToString(WasmHttpResponse.serializer(), response)
                val resBytes = resJson.toByteArray(Charsets.UTF_8)

                val allocFn = inst.export("alloc")
                val allocRes = allocFn.apply(resBytes.size.toLong())
                val resPtr = allocRes[0].toInt()
                inst.memory().write(resPtr, resBytes)

                val packed = (resBytes.size.toLong() shl 32) or (resPtr.toLong() and 0xFFFF_FFFFL)
                longArrayOf(packed)
            } catch (e: Exception) {
                logMessage(4, TAG, "[${metadata.id}] Error in host_http: ${e.message}")
                longArrayOf(0L)
            }
        }

        val hostLog = HostFunction(
            "bunori",
            "host_log",
            FunctionType.of(listOf(ValType.I32, ValType.I32, ValType.I32), emptyList())
        ) { inst, args ->
            val level = args[0].toInt()
            val msgPtr = args[1].toInt()
            val msgLen = args[2].toInt()
            if (msgLen > 0 && msgPtr != 0) {
                try {
                    val bytes = inst.memory().readBytes(msgPtr, msgLen)
                    val msg = String(bytes, Charsets.UTF_8)
                    logMessage(level, TAG, "[${metadata.id}] $msg")
                } catch (_: Exception) {}
            }
            longArrayOf()
        }

        val store = Store()
        store.addFunction(hostHttp)
        store.addFunction(hostLog)

        instance = Instance.builder(wasmModule)
            .withImportValues(store.toImportValues())
            .build()

        logMessage(3, TAG, "Initialized WASM extension: ${manifest.name} (id: ${manifest.id})")
    }

    private fun logMessage(level: Int, tag: String, message: String) {
        if (logger != null) {
            logger.invoke(level, tag, message)
        } else {
            when (level) {
                1 -> jvmLogger.finest("[$tag] $message")
                2 -> jvmLogger.fine("[$tag] $message")
                3 -> jvmLogger.info("[$tag] $message")
                4 -> jvmLogger.warning("[$tag] $message")
                5 -> jvmLogger.severe("[$tag] $message")
                else -> jvmLogger.info("[$tag] $message")
            }
        }
    }

    private fun executeHttpRequest(req: WasmHttpRequest): WasmHttpResponse {
        return runBlocking(Dispatchers.IO) {
            try {
                val response = if (req.method.equals("POST", ignoreCase = true)) {
                    val mediaType = req.headers["Content-Type"]?.toMediaTypeOrNull()
                        ?: req.headers["content-type"]?.toMediaTypeOrNull()
                        ?: "application/x-www-form-urlencoded".toMediaTypeOrNull()
                    val body = req.body?.toRequestBody(mediaType)
                    httpClient.post(req.url, req.headers, body)
                } else {
                    httpClient.get(req.url, req.headers)
                }

                response.use { res ->
                    val body = res.body?.string() ?: ""
                    val headers = mutableMapOf<String, String>()
                    for (i in 0 until res.headers.size) {
                        headers[res.headers.name(i)] = res.headers.value(i)
                    }
                    WasmHttpResponse(
                        statusCode = res.code,
                        headers = headers,
                        body = body
                    )
                }
            } catch (e: Exception) {
                logMessage(4, TAG, "HTTP error for ${req.url}: ${e.message}")
                WasmHttpResponse(
                    statusCode = 500,
                    body = "Host request error: ${e.message}"
                )
            }
        }
    }

    private fun writeString(str: String): Pair<Int, Int> {
        val bytes = str.toByteArray(Charsets.UTF_8)
        if (bytes.isEmpty()) return Pair(0, 0)
        val allocFn = instance.export("alloc")
        val res = allocFn.apply(bytes.size.toLong())
        val ptr = res[0].toInt()
        instance.memory().write(ptr, bytes)
        return Pair(ptr, bytes.size)
    }

    private fun deallocate(ptr: Int, size: Int) {
        if (ptr != 0 && size > 0) {
            try {
                instance.export("dealloc").apply(ptr.toLong(), size.toLong())
            } catch (e: Exception) {
                logMessage(4, TAG, "Error during dealloc: ${e.message}")
            }
        }
    }

    private fun readPackedString(packed: Long): String {
        if (packed == 0L) return ""
        val ptr = (packed and 0xFFFF_FFFFL).toInt()
        val len = (packed ushr 32).toInt()
        if (ptr == 0 || len <= 0) return ""
        return try {
            val bytes = instance.memory().readBytes(ptr, len)
            String(bytes, Charsets.UTF_8)
        } finally {
            deallocate(ptr, len)
        }
    }

    // =========================================================================
    // Raw JSON API (For third-party apps that use custom domain models)
    // =========================================================================

    suspend fun searchJson(query: String, page: Int = 1): String = withContext(Dispatchers.IO) {
        mutex.withLock {
            val (ptr, len) = writeString(query)
            try {
                val exportFn = instance.export("search")
                val result = exportFn.apply(ptr.toLong(), len.toLong(), page.toLong())
                readPackedString(result[0])
            } finally {
                deallocate(ptr, len)
            }
        }
    }

    suspend fun getNovelDetailsJson(novelUrl: String): String = withContext(Dispatchers.IO) {
        mutex.withLock {
            val (ptr, len) = writeString(novelUrl)
            try {
                val exportFn = instance.export("get_novel_details")
                val result = exportFn.apply(ptr.toLong(), len.toLong())
                readPackedString(result[0])
            } finally {
                deallocate(ptr, len)
            }
        }
    }

    fun getListingsJson(): String {
        return try {
            val exportFn = instance.export("get_listings")
            val result = exportFn.apply()
            readPackedString(result[0])
        } catch (e: Exception) {
            logMessage(4, TAG, "Error getting listings: ${e.message}")
            ""
        }
    }

    suspend fun getListingNovelsJson(listingId: String, page: Int = 1): String = withContext(Dispatchers.IO) {
        mutex.withLock {
            val (ptr, len) = writeString(listingId)
            try {
                val exportFn = instance.export("get_listing_novels")
                val result = exportFn.apply(ptr.toLong(), len.toLong(), page.toLong())
                readPackedString(result[0])
            } finally {
                deallocate(ptr, len)
            }
        }
    }

    // =========================================================================
    // Typed IExtension API (For apps using standard Bunori DTOs)
    // =========================================================================

    override suspend fun search(query: String, page: Int): List<SearchResultDto> {
        val json = searchJson(query, page)
        return if (json.isBlank()) emptyList()
        else ExtensionJson.json.decodeFromString(json)
    }

    override suspend fun getNovelDetails(novelUrl: String): NovelDto {
        val json = getNovelDetailsJson(novelUrl)
        if (json.isBlank()) {
            throw IllegalStateException("Failed to load novel details for $novelUrl (empty WASM response)")
        }
        return ExtensionJson.json.decodeFromString(json)
    }

    override suspend fun getChapterContent(chapterUrl: String): String? = withContext(Dispatchers.IO) {
        mutex.withLock {
            val (ptr, len) = writeString(chapterUrl)
            try {
                val exportFn = instance.export("get_chapter_content")
                val result = exportFn.apply(ptr.toLong(), len.toLong())
                val content = readPackedString(result[0])
                content.takeIf { it.isNotBlank() }
            } finally {
                deallocate(ptr, len)
            }
        }
    }

    override fun getListings(): List<ListingDto> {
        val json = getListingsJson()
        return if (json.isBlank()) emptyList()
        else ExtensionJson.json.decodeFromString(json)
    }

    override suspend fun getListingNovels(listingId: String, page: Int): List<SearchResultDto> {
        val json = getListingNovelsJson(listingId, page)
        return if (json.isBlank()) emptyList()
        else ExtensionJson.json.decodeFromString(json)
    }
}
