package com.halovoid.bunori.extension.api

import com.halovoid.bunori.extension.api.models.ExtensionManifest
import com.halovoid.bunori.extension.api.pkg.BextPackage
import com.halovoid.bunori.extension.api.pkg.BextUtils
import com.halovoid.bunori.extension.api.pkg.IncompatibleApiException
import com.halovoid.bunori.extension.api.pkg.InvalidBextPackageException
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class BextPackageTest {

    @Test
    fun testPackAndUnpackBextPackage() {
        val manifest = ExtensionManifest(
            id = "novelfull",
            name = "NovelFull",
            version = "2.0.0",
            apiVersion = 1,
            lang = "en",
            baseUrl = "https://novelfull.com",
            iconPath = "assets/icon.png"
        )
        val dummyWasm = "WASM_DUMMY_BYTECODE_DATA".toByteArray(Charsets.UTF_8)
        val dummyIcon = "PNG_ICON_BYTES".toByteArray(Charsets.UTF_8)

        val originalPkg = BextPackage(
            manifest = manifest,
            wasmBytes = dummyWasm,
            iconBytes = dummyIcon
        )

        val baos = ByteArrayOutputStream()
        BextUtils.writePackage(originalPkg, baos)

        val zipBytes = baos.toByteArray()
        val unpackedPkg = BextUtils.readPackage(ByteArrayInputStream(zipBytes))

        assertEquals("novelfull", unpackedPkg.manifest.id)
        assertEquals("NovelFull", unpackedPkg.manifest.name)
        assertEquals("2.0.0", unpackedPkg.manifest.version)
        assertEquals(1, unpackedPkg.manifest.apiVersion)
        assertEquals("https://novelfull.com", unpackedPkg.manifest.baseUrl)

        assertArrayEquals(dummyWasm, unpackedPkg.wasmBytes)
        assertNotNull(unpackedPkg.iconBytes)
        assertArrayEquals(dummyIcon, unpackedPkg.iconBytes)
    }

    @Test
    fun testMissingWasmThrowsException() {
        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zos ->
            zos.putNextEntry(ZipEntry("manifest.json"))
            val json = """
                {
                    "id": "test",
                    "name": "Test",
                    "version": "1.0.0",
                    "apiVersion": 1,
                    "baseUrl": "https://example.com"
                }
            """.trimIndent()
            zos.write(json.toByteArray())
            zos.closeEntry()
        }

        assertThrows(InvalidBextPackageException::class.java) {
            BextUtils.readPackage(ByteArrayInputStream(baos.toByteArray()))
        }
    }

    @Test
    fun testMissingManifestThrowsException() {
        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zos ->
            zos.putNextEntry(ZipEntry("source.wasm"))
            zos.write("WASM".toByteArray())
            zos.closeEntry()
        }

        assertThrows(InvalidBextPackageException::class.java) {
            BextUtils.readPackage(ByteArrayInputStream(baos.toByteArray()))
        }
    }

    @Test
    fun testIncompatibleApiVersionThrowsException() {
        val manifest = ExtensionManifest(
            id = "future_source",
            name = "Future Source",
            version = "1.0.0",
            apiVersion = 999, // Future API version
            lang = "en",
            baseUrl = "https://example.com"
        )
        val baos = ByteArrayOutputStream()
        BextUtils.writePackage(BextPackage(manifest, "WASM".toByteArray()), baos)

        assertThrows(IncompatibleApiException::class.java) {
            BextUtils.readPackage(ByteArrayInputStream(baos.toByteArray()))
        }
    }
}

