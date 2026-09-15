package com.halovoid.bunori.extension.loader

import android.content.Context
import android.util.Log
import com.halovoid.bunori.extension.api.IExtension
import com.halovoid.bunori.extension.api.http.ExtensionHttpClient
import com.halovoid.bunori.extension.api.pkg.BextPackage
import com.halovoid.bunori.extension.api.pkg.BextUtils
import com.halovoid.bunori.extension.api.wasm.WasmExtension
import com.halovoid.bunori.extension.http.ExtensionHttpClientImpl
import java.io.File
import java.io.FileOutputStream

/**
 * Loads WebAssembly bytecode from a .bext package and instantiates the [WasmExtension].
 */
class BextLoader(
    private val context: Context,
    private val httpClient: ExtensionHttpClient = ExtensionHttpClientImpl()
) {
    companion object {
        private const val TAG = "BextLoader"
    }

    /**
     * Unpacks a .bext archive file, saves assets to isolated storage,
     * and instantiates the [WasmExtension].
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
     * Installs in-memory [BextPackage] into isolated storage and loads it into a [WasmExtension].
     */
    fun loadPackage(pkg: BextPackage, sourceBextFile: File): LoadedExtension {
        val extensionId = pkg.manifest.id
        val targetDir = File(File(context.filesDir, "installed_extensions"), extensionId)
        if (!targetDir.exists()) {
            targetDir.mkdirs()
        }

        // 1. Write source.wasm
        val wasmFile = File(targetDir, BextUtils.WASM_FILE_NAME)
        FileOutputStream(wasmFile).use { fos ->
            fos.write(pkg.wasmBytes)
        }

        // 2. Write icon if present
        var iconFile: File? = null
        if (pkg.iconBytes != null) {
            val file = File(targetDir, "icon.png")
            FileOutputStream(file).use { fos ->
                fos.write(pkg.iconBytes)
            }
            iconFile = file
        } else {
            val existing = File(targetDir, "icon.png")
            if (existing.exists() && existing.length() > 0) {
                iconFile = existing
            }
        }

        // 3. Instantiate WasmExtension
        Log.i(TAG, "Initializing WasmExtension for ${pkg.manifest.name} from ${wasmFile.absolutePath}")
        val extensionInstance: IExtension = WasmExtension(
            manifest = pkg.manifest,
            wasmSource = wasmFile,
            httpClient = httpClient,
            logger = { level, tag, msg ->
                when (level) {
                    1 -> Log.v(tag, msg)
                    2 -> Log.d(tag, msg)
                    3 -> Log.i(tag, msg)
                    4 -> Log.w(tag, msg)
                    5 -> Log.e(tag, msg)
                    else -> Log.d(tag, msg)
                }
            }
        )

        Log.i(TAG, "Successfully loaded WASM extension: ${pkg.manifest.name} (v${pkg.manifest.version})")

        return LoadedExtension(
            manifest = pkg.manifest,
            extension = extensionInstance,
            bextFile = sourceBextFile,
            iconFile = iconFile
        )
    }
}
