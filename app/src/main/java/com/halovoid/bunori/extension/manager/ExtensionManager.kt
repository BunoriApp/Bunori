package com.halovoid.bunori.extension.manager

import android.content.Context
import android.net.Uri
import android.util.Log
import com.halovoid.bunori.api.core.crawler.CrawlerFactory
import com.halovoid.bunori.extension.adapter.ExtensionCrawlerAdapter
import com.halovoid.bunori.extension.api.IExtension
import com.halovoid.bunori.extension.api.models.ExtensionMetadata
import com.halovoid.bunori.extension.api.models.ExtensionRepoEntry
import com.halovoid.bunori.extension.api.pkg.BextUtils
import com.halovoid.bunori.extension.loader.BextLoader
import com.halovoid.bunori.extension.loader.LoadedExtension
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Manages the lifecycle, storage, installation, catalog retrieval, and querying of Bunori extensions.
 */
class ExtensionManager private constructor(private val context: Context) {

    private val bextLoader = BextLoader(context)
    private val _installedExtensions = MutableStateFlow<Map<String, LoadedExtension>>(emptyMap())
    val installedExtensions: StateFlow<Map<String, LoadedExtension>> = _installedExtensions.asStateFlow()

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    companion object {
        private const val TAG = "ExtensionManager"

        @Volatile
        private var instance: ExtensionManager? = null

        fun getInstance(context: Context): ExtensionManager {
            return instance ?: synchronized(this) {
                instance ?: ExtensionManager(context.applicationContext).also { instance = it }
            }
        }
    }

    private val extensionsDir: File
        get() = File(context.filesDir, "installed_extensions").also {
            if (!it.exists()) it.mkdirs()
        }

    /**
     * Scans app storage, initializes all previously installed extensions,
     * and synchronizes them with [CrawlerFactory].
     */
    suspend fun loadInstalledExtensions() = withContext(Dispatchers.IO) {
        val loaded = mutableMapOf<String, LoadedExtension>()
        val dirs = extensionsDir.listFiles { file -> file.isDirectory } ?: emptyArray()

        for (dir in dirs) {
            val bextFile = File(dir, "package.bext")
            if (bextFile.exists() && bextFile.length() > 0) {
                try {
                    val loadedExt = bextLoader.loadFromBextFile(bextFile)
                    loaded[loadedExt.manifest.id] = loadedExt
                    Log.i(TAG, "Loaded extension on startup: ${loadedExt.manifest.name}")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to load extension from ${bextFile.absolutePath}", e)
                }
            }
        }

        _installedExtensions.value = loaded
        syncWithCrawlerFactory(loaded)
    }

    /**
     * Fetches and parses an extension catalog/repository index from [repoUrl].
     * Resolves any relative package URLs to absolute URLs.
     */
    suspend fun fetchRepoCatalog(repoUrl: String): Result<List<ExtensionRepoEntry>> = withContext(Dispatchers.IO) {
        try {
            val jsonString = if (repoUrl.startsWith("file://")) {
                val localPath = repoUrl.removePrefix("file://")
                File(localPath).readText(Charsets.UTF_8)
            } else {
                val request = Request.Builder()
                    .url(repoUrl)
                    .header("User-Agent", "Bunori/1.0")
                    .build()

                val response = httpClient.newCall(request).execute()
                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        IOException("Failed to fetch repository index: HTTP ${response.code}")
                    )
                }
                response.body?.string() ?: return@withContext Result.failure(
                    IOException("Empty response body from repository")
                )
            }

            val entries = ExtensionRepoEntry.parseIndex(jsonString)

            // Resolve relative bextUrl against repoUrl
            val resolvedEntries = entries.map { entry ->
                val resolvedUrl = resolveUrl(repoUrl, entry.bextUrl)
                entry.copy(bextUrl = resolvedUrl)
            }

            Result.success(resolvedEntries)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching repository catalog from $repoUrl", e)
            Result.failure(e)
        }
    }

    /**
     * Downloads and installs an extension from a repository entry.
     */
    suspend fun downloadAndInstall(entry: ExtensionRepoEntry): Result<LoadedExtension> = withContext(Dispatchers.IO) {
        val tempFile = File(context.cacheDir, "download_${entry.id}_${System.currentTimeMillis()}.bext")
        try {
            if (entry.bextUrl.startsWith("file://")) {
                val localPath = entry.bextUrl.removePrefix("file://")
                File(localPath).copyTo(tempFile, overwrite = true)
            } else {
                val request = Request.Builder()
                    .url(entry.bextUrl)
                    .header("User-Agent", "Bunori/1.0")
                    .build()

                val response = httpClient.newCall(request).execute()
                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        IOException("Failed to download extension: HTTP ${response.code}")
                    )
                }
                val body = response.body ?: return@withContext Result.failure(
                    IOException("Empty body downloading extension")
                )

                FileOutputStream(tempFile).use { fos ->
                    body.byteStream().copyTo(fos)
                }
            }

            val result = installFromFile(tempFile)
            tempFile.delete()
            result
        } catch (e: Exception) {
            tempFile.delete()
            Log.e(TAG, "Failed to download and install extension: ${entry.id}", e)
            Result.failure(e)
        }
    }

    /**
     * Installs or updates an extension from a local .bext file.
     *
     * @param sourceFile The .bext archive.
     * @return [Result] containing [LoadedExtension] on success.
     */
    suspend fun installFromFile(sourceFile: File): Result<LoadedExtension> = withContext(Dispatchers.IO) {
        try {
            // 1. Verify and read package
            val pkg = sourceFile.inputStream().use { stream ->
                BextUtils.readPackage(stream, validateApiVersion = true)
            }

            val targetDir = File(extensionsDir, pkg.manifest.id)
            if (!targetDir.exists()) targetDir.mkdirs()

            val targetBext = File(targetDir, "package.bext")
            if (targetBext.exists()) {
                targetBext.delete()
            }
            sourceFile.copyTo(targetBext, overwrite = true)

            // 2. Load into memory
            val loaded = bextLoader.loadPackage(pkg, targetBext)

            // 3. Update registry and sync CrawlerFactory
            val current = _installedExtensions.value.toMutableMap()
            current[loaded.manifest.id] = loaded
            _installedExtensions.value = current
            syncWithCrawlerFactory(current)

            Log.i(TAG, "Successfully installed extension: ${loaded.manifest.name} (id: ${loaded.manifest.id})")
            Result.success(loaded)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to install extension from ${sourceFile.absolutePath}", e)
            Result.failure(e)
        }
    }

    /**
     * Installs or updates an extension selected via Android file picker [Uri].
     */
    suspend fun installFromUri(uri: Uri): Result<LoadedExtension> = withContext(Dispatchers.IO) {
        val tempFile = File(context.cacheDir, "temp_${System.currentTimeMillis()}.bext")
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return@withContext Result.failure(IllegalArgumentException("Could not open URI: $uri"))

            val result = installFromFile(tempFile)
            tempFile.delete()
            result
        } catch (e: Exception) {
            tempFile.delete()
            Result.failure(e)
        }
    }

    /**
     * Uninstalls and removes an extension by its identifier.
     */
    suspend fun uninstall(extensionId: String): Boolean = withContext(Dispatchers.IO) {
        val current = _installedExtensions.value.toMutableMap()
        val loaded = current.remove(extensionId) ?: return@withContext false

        _installedExtensions.value = current
        syncWithCrawlerFactory(current)

        // Delete installed directory
        val targetDir = File(extensionsDir, extensionId)
        if (targetDir.exists()) {
            val dexFile = File(targetDir, BextUtils.DEX_FILE_NAME)
            if (dexFile.exists()) {
                dexFile.delete()
            }
            targetDir.deleteRecursively()
        }

        Log.i(TAG, "Uninstalled extension: $extensionId")
        true
    }

    /**
     * Returns whether an extension with [id] is installed.
     */
    fun isInstalled(id: String): Boolean {
        return _installedExtensions.value.containsKey(id)
    }

    /**
     * Returns installed release version for extension [id], or null if not installed.
     */
    fun getInstalledVersion(id: String): String? {
        return _installedExtensions.value[id]?.manifest?.version
    }

    /**
     * Retrieves an active [IExtension] by source id.
     */
    fun getExtension(id: String): IExtension? {
        return _installedExtensions.value[id]?.extension
    }

    /**
     * Returns all active [IExtension] implementations.
     */
    fun getAllExtensions(): List<IExtension> {
        return _installedExtensions.value.values.map { it.extension }
    }

    /**
     * Finds an extension capable of handling [url] by domain/baseUrl matching.
     */
    fun findExtensionForUrl(url: String): IExtension? {
        val cleanUrl = url.lowercase().removePrefix("https://").removePrefix("http://").removePrefix("www.")
        val domain = cleanUrl.substringBefore('/')

        return _installedExtensions.value.values.map { it.extension }.firstOrNull { ext ->
            val extDomain = ext.metadata.baseUrl.lowercase()
                .removePrefix("https://").removePrefix("http://").removePrefix("www.")
                .substringBefore('/')
            domain.contains(extDomain) || extDomain.contains(domain)
        }
    }

    private fun syncWithCrawlerFactory(extensions: Map<String, LoadedExtension>) {
        val adapters = extensions.values.map { ExtensionCrawlerAdapter(it.extension) }
        CrawlerFactory.registerCrawlers(adapters)
    }

    private fun resolveUrl(base: String, relative: String): String {
        if (relative.startsWith("http://") || relative.startsWith("https://") || relative.startsWith("file://")) {
            return relative
        }
        return if (base.startsWith("file://")) {
            val parent = File(base.removePrefix("file://")).parentFile
            "file://" + File(parent, relative).absolutePath
        } else {
            val lastSlash = base.lastIndexOf('/')
            if (lastSlash != -1) {
                base.substring(0, lastSlash + 1) + relative
            } else {
                "$base/$relative"
            }
        }
    }
}
