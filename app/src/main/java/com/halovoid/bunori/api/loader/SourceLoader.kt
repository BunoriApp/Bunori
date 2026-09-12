package com.halovoid.bunori.api.loader

import android.content.Context
import android.util.Log
import com.halovoid.bunori.api.core.crawler.Crawler
import com.halovoid.bunori.api.core.crawler.CrawlerFactory
import com.halovoid.bunori.api.core.network.NetworkClient
import com.halovoid.bunori.data.repository.PreferenceRepository
import com.halovoid.bunori.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONObject
import kotlinx.coroutines.flow.first
import java.io.File
import java.io.FileOutputStream

/**
 * Manages loading crawlers from a remote DEX file.
 */
class SourceLoader(private val context: Context) {
    private val dexLoader = DexLoader(context)
    private val preferenceRepository = PreferenceRepository.getInstance(context)
    private val client = NetworkClient.okHttpClient

    private val GITHUB_API_URL = "https://api.github.com/repos/Binit06/LNCrawlerSources/releases/latest"
    private val AGGREGATOR_CLASS = "com.halovoid.lncrawlersources.CrawlerSourceAggregator"

    data class ReleaseInfo(val tagName: String, val downloadUrl: String)

    class IncompatibleAppException(val minVersion: String) : Exception("This crawler bundle requires Bunori version $minVersion or higher.")

    /**
     * Downloads the latest DEX file and loads crawlers into the Factory.
     */
    suspend fun loadSources(
        releaseInfo: ReleaseInfo? = null,
        onProgress: (String) -> Unit = {}
    ) = withContext(Dispatchers.IO) {
        try {
            onProgress("Starting source synchronization...")
            Log.i("SourceLoader", "Starting to load sources...")

            val info = releaseInfo ?: run {
                val enableBeta = preferenceRepository.betaModeCrawlers.first()
                fetchLatestReleaseInfo(enableBeta)
            }
            
            onProgress("Downloading crawler DEX bundle (${info.tagName})...")
            val dexFile = downloadDex(info.downloadUrl)
            
            loadFromDex(dexFile, onProgress)
            
            // Save the tag name upon successful sync
            preferenceRepository.setCurrentDexTag(info.tagName)
            
        } catch (e: Exception) {
            onProgress("Error: ${e.message ?: "Sync failed"}")
            Log.e("SourceLoader", "Failed to load sources", e)
            throw e
        }
    }

    /**
     * Attempts to load crawlers from the locally stored DEX file.
     */
    suspend fun loadLocalSources() = withContext(Dispatchers.IO) {
        val dexFile = File(File(context.filesDir, "sources"), "sources.dex")
        if (dexFile.exists()) {
            Log.i("SourceLoader", "Loading sources from local DEX...")
            try {
                loadFromDex(dexFile)
            } catch (e: IncompatibleAppException) {
                Log.e("SourceLoader", "Local DEX is incompatible with this app version.", e)
                // Optionally clear the tag so the user is prompted to sync/update
                preferenceRepository.setCurrentDexTag("") 
            } catch (e: Exception) {
                Log.e("SourceLoader", "Failed to load local sources", e)
            }
        } else {
            Log.i("SourceLoader", "No local DEX found to load.")
        }
    }

    private fun loadFromDex(dexFile: File, onProgress: (String) -> Unit = {}) {
        Log.i("SourceLoader", "Loading from DEX: ${dexFile.absolutePath} (Size: ${dexFile.length()})")
        
        // Android 14+ security requirement: Dynamically loaded files must be read-only
        if ((android.os.Build.VERSION.SDK_INT >= 34) && dexFile.canWrite()) {
            dexFile.setReadOnly()
            Log.i("SourceLoader", "Set DEX file to read-only for API 34+ compliance")
        }

        onProgress("Initializing DexClassLoader...")
        val aggregatorClass = try {
            dexLoader.load(dexFile, AGGREGATOR_CLASS)
        } catch (e: Exception) {
            Log.e("SourceLoader", "Failed to load aggregator class: $AGGREGATOR_CLASS", e)
            onProgress("Error: Failed to load aggregator class")
            throw e
        }
        
        onProgress("Instantiating Crawler Aggregator...")
        val aggregator = aggregatorClass.getDeclaredConstructor().newInstance()

        // Version Check
        try {
            val getMinVersionMethod = aggregatorClass.getMethod("getMinAppVersion")
            val minVersion = getMinVersionMethod.invoke(aggregator) as String
            val currentVersion = BuildConfig.VERSION_NAME
            
            if (VersionUtils.isUpdateAvailable(currentVersion, minVersion)) {
                Log.e("SourceLoader", "Incompatible App: Current $currentVersion, Required $minVersion")
                throw IncompatibleAppException(minVersion)
            }
        } catch (_: NoSuchMethodException) {
            Log.w("SourceLoader", "Aggregator does not provide getMinAppVersion. Skipping check.")
        }

        val getCrawlersMethod = aggregatorClass.getMethod("getCrawlers")
        
        onProgress("Extracting crawler definitions...")
        val rawCrawlers = getCrawlersMethod.invoke(aggregator) as? List<*>
        Log.i("SourceLoader", "Aggregator returned ${rawCrawlers?.size ?: 0} raw crawlers")
        
        val validCrawlers = mutableListOf<Crawler>()

        rawCrawlers?.forEach { item ->
            if (item is Crawler) {
                Log.d("SourceLoader", "Found valid crawler: ${item.name}")
                onProgress("Loaded Crawler: ${item.name}")
                validCrawlers.add(item)
            } else {
                val className = item?.javaClass?.name ?: "null"
                Log.w("SourceLoader", "Incompatible source found: $className")
                onProgress("Skipping incompatible source: $className")
            }
        }
        
        Log.i("SourceLoader", "Successfully registered ${validCrawlers.size} crawlers")
        onProgress("Finalizing: ${validCrawlers.size} crawlers registered.")
        CrawlerFactory.registerCrawlers(validCrawlers)
    }

    suspend fun fetchLatestReleaseInfo(enableBeta: Boolean = false): ReleaseInfo = withContext(Dispatchers.IO) {
        val url = if (enableBeta) {
            "https://api.github.com/repos/Binit06/LNCrawlerSources/releases"
        } else {
            "https://api.github.com/repos/Binit06/LNCrawlerSources/releases/latest"
        }
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw Exception("Failed to fetch release info: $response")
            
            val body = response.body?.string() ?: throw Exception("Empty response body")
            val json = if (enableBeta) {
                val array = org.json.JSONArray(body)
                if (array.length() == 0) throw Exception("No releases found")
                array.getJSONObject(0)
            } else {
                JSONObject(body)
            }
            val tagName = json.getString("tag_name")
            val assets = json.getJSONArray("assets")
            
            var downloadUrl: String? = null
            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                if (asset.getString("name") == "classes.dex") {
                    downloadUrl = asset.getString("browser_download_url")
                    break
                }
            }
            
            ReleaseInfo(
                tagName = tagName,
                downloadUrl = downloadUrl ?: throw Exception("classes.dex not found in release")
            )
        }
    }

    private fun downloadDex(url: String): File {
        val dexDir = File(context.filesDir, "sources")
        if (!dexDir.exists()) dexDir.mkdirs()
        
        val targetFile = File(dexDir, "sources.dex")
        
        // If file exists, delete it first to handle potential read-only state from previous runs
        if (targetFile.exists()) {
            targetFile.delete()
        }
        
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw Exception("Failed to download DEX: $response")
            
            response.body?.byteStream()?.use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            }
        }
        
        // Ensure read-only for Android 14+
        if (android.os.Build.VERSION.SDK_INT >= 34) {
            targetFile.setReadOnly()
        }
        
        return targetFile
    }
}
