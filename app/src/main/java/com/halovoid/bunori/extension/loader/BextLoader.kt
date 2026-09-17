package com.halovoid.bunori.extension.loader

import android.content.Context
import android.util.Log
import com.halovoid.bunori.extension.api.IExtension
import com.halovoid.bunori.extension.api.pkg.BextPackage
import com.halovoid.bunori.extension.api.pkg.BextUtils
import com.halovoid.bunori.wasm.WamrExtension
import java.io.File
import java.io.FileOutputStream

/**
 * Unpacks .bext packages into app storage and instantiates native [WamrExtension] runners.
 * Automatically selects AOT machine code for device CPU architecture if present.
 */
class BextLoader(private val context: Context) {
    companion object {
        private const val TAG = "BextLoader"
    }

    /**
     * Unpacks a .bext archive file, saves assets to isolated storage,
     * and instantiates the [WamrExtension].
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
     * Installs in-memory [BextPackage] into isolated storage and loads it into [WamrExtension].
     * Prioritizes native AOT machine code matching device ABI, with fallback to source.wasm.
     */
    fun loadPackage(pkg: BextPackage, sourceBextFile: File): LoadedExtension {
        val extensionId = pkg.manifest.id
        val targetDir = File(File(context.filesDir, "installed_extensions"), extensionId)
        if (!targetDir.exists()) {
            targetDir.mkdirs()
        }

        // 1. Pick the best binary: Native AOT matching device ABI, or portable source.wasm
        var selectedBytes = pkg.wasmBytes
        var binaryName = BextUtils.WASM_FILE_NAME
        var isAot = false

        for (abi in android.os.Build.SUPPORTED_ABIS) {
            val aotPath = "artifacts/$abi/extension.aot"
            val aotBytes = pkg.extraFiles[aotPath]
            if (aotBytes != null && aotBytes.isNotEmpty()) {
                selectedBytes = aotBytes
                binaryName = "extension_$abi.aot"
                isAot = true
                Log.i(TAG, "Selected native AOT binary for ABI '$abi' (${aotBytes.size} bytes) for ${pkg.manifest.name}")
                break
            }
        }

        val binaryFile = File(targetDir, binaryName)
        FileOutputStream(binaryFile).use { fos ->
            fos.write(selectedBytes)
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

        // 3. Instantiate native WamrExtension
        val mode = if (isAot) "AOT Native Machine Code" else "Fast Interpreter"
        Log.i(TAG, "Initializing WamrExtension [$mode] for ${pkg.manifest.name} from ${binaryFile.name}")
        val extensionInstance: IExtension = WamrExtension(
            manifest = pkg.manifest,
            binaryBytes = selectedBytes
        )

        Log.i(TAG, "Successfully loaded extension: ${pkg.manifest.name} (v${pkg.manifest.version}) in $mode mode")

        return LoadedExtension(
            manifest = pkg.manifest,
            extension = extensionInstance,
            bextFile = sourceBextFile,
            iconFile = iconFile
        )
    }
}
