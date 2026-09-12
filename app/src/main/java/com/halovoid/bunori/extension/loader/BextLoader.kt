package com.halovoid.bunori.extension.loader

import android.content.Context
import android.os.Build
import android.util.Log
import com.halovoid.bunori.extension.api.IExtension
import com.halovoid.bunori.extension.api.http.ExtensionHttpClient
import com.halovoid.bunori.extension.api.pkg.BextPackage
import com.halovoid.bunori.extension.api.pkg.BextUtils
import com.halovoid.bunori.extension.http.ExtensionHttpClientImpl
import dalvik.system.DexClassLoader
import java.io.File
import java.io.FileOutputStream

/**
 * Loads Dalvik bytecode from a .bext package and instantiates the [IExtension] implementation.
 */
class BextLoader(
    private val context: Context,
    private val httpClient: ExtensionHttpClient = ExtensionHttpClientImpl()
) {
    companion object {
        private const val TAG = "BextLoader"
    }

    /**
     * Unpacks a .bext file into app-private storage, initializes DexClassLoader,
     * and instantiates the entry [IExtension].
     *
     * @param bextFile The .bext archive file.
     * @return [LoadedExtension] with initialized instance and metadata.
     */
    fun loadFromBextFile(bextFile: File): LoadedExtension {
        if (!bextFile.exists() || bextFile.length() == 0L) {
            throw IllegalArgumentException("File does not exist or is empty: ${bextFile.absolutePath}")
        }

        Log.i(TAG, "Unpacking .bext archive: ${bextFile.name}")
        val pkg = bextFile.inputStream().use { stream ->
            BextUtils.readPackage(stream, validateApiVersion = true)
        }

        return loadPackage(pkg, bextFile)
    }

    /**
     * Installs in-memory [BextPackage] into isolated storage and loads it.
     */
    fun loadPackage(pkg: BextPackage, sourceBextFile: File): LoadedExtension {
        val extensionId = pkg.manifest.id
        val targetDir = File(File(context.filesDir, "installed_extensions"), extensionId)
        if (!targetDir.exists()) {
            targetDir.mkdirs()
        }

        // 1. Write classes.dex
        val dexFile = File(targetDir, BextUtils.DEX_FILE_NAME)
        if (dexFile.exists()) {
            dexFile.delete() // Reset read-only permissions if present
        }
        FileOutputStream(dexFile).use { fos ->
            fos.write(pkg.dexBytes)
        }

        // Android 14+ (API 34+) security requirement: Dynamically loaded code must be read-only
        if (Build.VERSION.SDK_INT >= 34 && dexFile.canWrite()) {
            dexFile.setReadOnly()
            Log.i(TAG, "Set DEX file to read-only for Android API 34+ compliance")
        }

        // 2. Write icon if present
        var iconFile: File? = null
        pkg.iconBytes?.let { bytes ->
            val file = File(targetDir, "icon.png")
            FileOutputStream(file).use { fos ->
                fos.write(bytes)
            }
            iconFile = file
        }

        // 3. Initialize DexClassLoader
        Log.i(TAG, "Loading entry class ${pkg.manifest.entryClass} from ${dexFile.absolutePath}")
        val classLoader = DexClassLoader(
            dexFile.absolutePath,
            null,
            null,
            context.classLoader
        )

        val entryClass = try {
            classLoader.loadClass(pkg.manifest.entryClass)
        } catch (e: ClassNotFoundException) {
            Log.e(TAG, "Entry class '${pkg.manifest.entryClass}' not found in DEX", e)
            throw IllegalStateException("Entry class '${pkg.manifest.entryClass}' not found in DEX", e)
        }

        // 4. Instantiate entry class
        val rawInstance = try {
            // Try constructor with ExtensionHttpClient parameter
            val constructor = entryClass.getDeclaredConstructor(ExtensionHttpClient::class.java)
            constructor.newInstance(httpClient)
        } catch (_: NoSuchMethodException) {
            try {
                // Fall back to no-arg constructor
                val noArgConstructor = entryClass.getDeclaredConstructor()
                noArgConstructor.newInstance()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to instantiate ${entryClass.name}", e)
                throw IllegalStateException("Could not find a valid constructor for ${entryClass.name}", e)
            }
        }

        val extensionInstance = rawInstance as? IExtension
            ?: throw IllegalStateException(
                "Class ${entryClass.name} does not implement ${IExtension::class.java.name}"
            )

        Log.i(TAG, "Successfully loaded extension: ${pkg.manifest.name} (v${pkg.manifest.version})")

        return LoadedExtension(
            manifest = pkg.manifest,
            extension = extensionInstance,
            bextFile = sourceBextFile,
            iconFile = iconFile,
            classLoader = classLoader
        )
    }
}
