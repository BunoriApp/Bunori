package com.halovoid.bunori.extension.api

import kotlinx.serialization.json.Json

/**
 * Standard JSON serializer configuration for Bunori extension payloads.
 * Permissive configuration: ignores unknown keys, handles leniency, encodes defaults.
 */
object ExtensionJson {
    val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        coerceInputValues = true
        prettyPrint = false
    }
}
