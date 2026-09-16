package com.halovoid.bunori.extension.api.pkg

import com.halovoid.bunori.extension.api.models.ExtensionManifest

/**
 * In-memory representation of a Bunori Extension (.bext) package.
 *
 * @property manifest Metadata loaded from manifest.json.
 * @property wasmBytes Compiled WebAssembly bytecode (source.wasm).
 * @property iconBytes Optional image bytes for the source icon.
 * @property extraFiles Optional auxiliary assets bundled within the archive.
 */
data class BextPackage(
    val manifest: ExtensionManifest,
    val wasmBytes: ByteArray,
    val iconBytes: ByteArray? = null,
    val extraFiles: Map<String, ByteArray> = emptyMap()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BextPackage) return false
        if (manifest != other.manifest) return false
        if (!wasmBytes.contentEquals(other.wasmBytes)) return false
        return true
    }

    override fun hashCode(): Int {
        var result = manifest.hashCode()
        result = 31 * result + wasmBytes.contentHashCode()
        return result
    }
}
