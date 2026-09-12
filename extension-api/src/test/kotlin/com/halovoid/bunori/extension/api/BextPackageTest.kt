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
            entryClass = "com.halovoid.bunori.extension.novelfull.NovelFullExtension",
            iconPath = "assets/icon.png"
        )
        val dummyDex = "DEX_DUMMY_BYTECODE_DATA".toByteArray(Charsets.UTF_8)
        val dummyIcon = "PNG_ICON_BYTES".toByteArray(Charsets.UTF_8)

        val originalPkg = BextPackage(
            manifest = manifest,
            dexBytes = dummyDex,
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
        assertEquals("com.halovoid.bunori.extension.novelfull.NovelFullExtension", unpackedPkg.manifest.entryClass)

        assertArrayEquals(dummyDex, unpackedPkg.dexBytes)
        assertNotNull(unpackedPkg.iconBytes)
        assertArrayEquals(dummyIcon, unpackedPkg.iconBytes)
    }

    @Test
    fun testMissingDexThrowsException() {
        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zos ->
            zos.putNextEntry(ZipEntry("manifest.json"))
            val json = """
                {
                    "id": "test",
                    "name": "Test",
                    "version": "1.0.0",
                    "apiVersion": 1,
                    "baseUrl": "https://example.com",
                    "entryClass": "com.example.Test"
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
            zos.putNextEntry(ZipEntry("classes.dex"))
            zos.write("DEX".toByteArray())
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
            baseUrl = "https://example.com",
            entryClass = "com.example.Future"
        )
        val baos = ByteArrayOutputStream()
        BextUtils.writePackage(BextPackage(manifest, "DEX".toByteArray()), baos)

        assertThrows(IncompatibleApiException::class.java) {
            BextUtils.readPackage(ByteArrayInputStream(baos.toByteArray()))
        }
    }

    @Test
    fun testRealPackagedBextFromSourcesIfPresent() {
        val repoFile = java.io.File("/home/zenit/Projects/LNCrawlerSources/repo/novelfull.bext")
        if (repoFile.exists()) {
            val pkg = repoFile.inputStream().use { BextUtils.readPackage(it) }
            assertEquals("novelfull", pkg.manifest.id)
            assertEquals("Novel Full", pkg.manifest.name)
            assertEquals("com.halovoid.bunorisources.crawler.NovelFull", pkg.manifest.entryClass)
            assert(pkg.dexBytes.isNotEmpty())
        }
    }
}

