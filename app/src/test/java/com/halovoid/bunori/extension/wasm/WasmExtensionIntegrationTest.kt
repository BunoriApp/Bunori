package com.halovoid.bunori.extension.wasm

import com.halovoid.bunori.extension.api.http.ExtensionHttpClient
import com.halovoid.bunori.extension.api.models.ExtensionManifest
import com.halovoid.bunori.extension.api.pkg.BextPackage
import com.halovoid.bunori.extension.api.pkg.BextUtils
import com.halovoid.bunori.extension.api.wasm.WasmExtension
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File

class WasmExtensionIntegrationTest {

    private class MockHttpClient(val mockHtml: String = "") : ExtensionHttpClient {
        override suspend fun get(url: String, headers: Map<String, String>): Response {
            return Response.Builder()
                .request(Request.Builder().url(url).build())
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(mockHtml.toResponseBody("text/html".toMediaTypeOrNull()))
                .build()
        }

        override suspend fun post(url: String, headers: Map<String, String>, body: RequestBody?): Response {
            return get(url, headers)
        }

        override suspend fun fetch(url: String, headers: Map<String, String>): String? = mockHtml
        override suspend fun document(url: String, headers: Map<String, String>): Document? = Jsoup.parse(mockHtml, url)
        override suspend fun download(url: String): ByteArray? = mockHtml.toByteArray()
    }

    @Test
    fun testWasmExtensionMetadataAndListings() {
        val wasmFile = File("/home/zenit/Projects/BunoriExtensions/target/wasm32-unknown-unknown/release/novelfull.wasm")
        if (!wasmFile.exists()) {
            println("Skipping test: novelfull.wasm not found at ${wasmFile.absolutePath}")
            return
        }

        val manifest = ExtensionManifest(
            id = "novelfull",
            name = "NovelFull",
            version = "1.0.0",
            apiVersion = 1,
            lang = "en",
            baseUrl = "https://novelfull.com"
        )

        // 1. Pack into .bext
        val bextBaos = ByteArrayOutputStream()
        val originalPkg = BextPackage(manifest = manifest, wasmBytes = wasmFile.readBytes())
        BextUtils.writePackage(originalPkg, bextBaos)

        // 2. Read back from .bext
        val unpackedPkg = BextUtils.readPackage(ByteArrayInputStream(bextBaos.toByteArray()))
        assertEquals("novelfull", unpackedPkg.manifest.id)
        assertEquals(manifest.name, unpackedPkg.manifest.name)

        // 3. Initialize WasmExtension
        val extension = WasmExtension(
            manifest = unpackedPkg.manifest,
            wasmSource = unpackedPkg.wasmBytes,
            httpClient = MockHttpClient()
        )

        // 4. Verify metadata
        assertEquals("novelfull", extension.metadata.id)
        assertEquals("NovelFull", extension.metadata.name)
        assertEquals("https://novelfull.com", extension.metadata.baseUrl)

        // 5. Test getListings()
        val listings = extension.getListings()
        assertNotNull(listings)
        assertTrue("NovelFull should provide listings", listings.isNotEmpty())
        println("NovelFull listings loaded: ${listings.map { it.name }}")
    }

    @Test
    fun testWasmExtensionSearchExecution() = runBlocking {
        val wasmFile = File("/home/zenit/Projects/BunoriExtensions/target/wasm32-unknown-unknown/release/royalroad.wasm")
        if (!wasmFile.exists()) {
            println("Skipping test: royalroad.wasm not found")
            return@runBlocking
        }

        val manifest = ExtensionManifest(
            id = "royalroad",
            name = "Royal Road",
            version = "1.0.0",
            apiVersion = 1,
            lang = "en",
            baseUrl = "https://www.royalroad.com"
        )

        val sampleRoyalRoadHtml = """
            <div class="fiction-list">
                <div class="row fiction-list-item">
                    <h2 class="fiction-title"><a href="/fiction/12345/primal-hunter">The Primal Hunter</a></h2>
                    <span class="author"><a href="/author/123">Zogarth</a></span>
                    <img class="img-responsive" src="https://example.com/cover.jpg" />
                </div>
            </div>
        """.trimIndent()

        val extension = WasmExtension(
            manifest = manifest,
            wasmSource = wasmFile.readBytes(),
            httpClient = MockHttpClient(mockHtml = sampleRoyalRoadHtml)
        )

        val results = extension.search("hunter", page = 1)
        assertNotNull(results)
        assertTrue("Expected search results from mock HTML", results.isNotEmpty())
        assertEquals("The Primal Hunter", results[0].title)
        assertTrue(results[0].url.contains("primal-hunter"))
        assertEquals("Zogarth", results[0].author)
        println("Successfully searched WASM extension: ${results[0].title} by ${results[0].author}")
    }

    @Test
    fun testWasmExtensionRawJsonForThirdPartyApps() = runBlocking {
        val wasmFile = File("/home/zenit/Projects/BunoriExtensions/target/wasm32-unknown-unknown/release/royalroad.wasm")
        if (!wasmFile.exists()) return@runBlocking

        val manifest = ExtensionManifest(
            id = "royalroad",
            name = "Royal Road",
            baseUrl = "https://www.royalroad.com"
        )

        val sampleRoyalRoadHtml = """
            <div class="fiction-list">
                <div class="row fiction-list-item">
                    <h2 class="fiction-title"><a href="/fiction/999/shadow-slave">Shadow Slave</a></h2>
                    <span class="author"><a href="/author/999">Guiltythree</a></span>
                </div>
            </div>
        """.trimIndent()

        val extension = WasmExtension(
            manifest = manifest,
            wasmSource = wasmFile.readBytes(),
            httpClient = MockHttpClient(mockHtml = sampleRoyalRoadHtml)
        )

        // Third-party developer calls searchJson -> gets pure JSON without using Bunori DTOs!
        val rawJson: String = extension.searchJson("shadow", page = 1)
        println("Raw JSON returned from WASM: $rawJson")
        assertTrue("Raw JSON should contain fiction title", rawJson.contains("Shadow Slave"))
        assertTrue("Raw JSON should contain author", rawJson.contains("Guiltythree"))
    }
}
