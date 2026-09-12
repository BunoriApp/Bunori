package com.halovoid.bunori.extension.api

import com.halovoid.bunori.extension.api.models.ExtensionMetadata
import java.io.Closeable

/**
 * Contract for the runtime engine capable of executing JavaScript extension scripts.
 */
interface JsExtensionEngine : Closeable {
    /**
     * Initializes and loads an extension script, returning an executable [JsExtension] instance.
     *
     * @param script The raw JavaScript content.
     * @param metadata Optional pre-parsed metadata; if null, extracted from script metadata function/header.
     */
    suspend fun loadExtension(script: String, metadata: ExtensionMetadata? = null): JsExtension
}
