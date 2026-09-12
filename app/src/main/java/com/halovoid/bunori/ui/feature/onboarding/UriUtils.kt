package com.halovoid.bunori.ui.feature.onboarding

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import java.net.URLDecoder

object UriUtils {
    /**
     * Attempts to convert a SAF tree URI to a human-readable path.
     * Falls back to the display name if a filesystem path cannot be resolved.
     */
    fun getFriendlyPath(context: Context, uri: Uri?): String {
        if (uri == null) return ""
        
        return try {
            val docId = DocumentsContract.getTreeDocumentId(uri)
            val split = docId.split(":")
            val type = split[0]
            val path = if (split.size > 1) split[1] else ""

            if ("primary".equals(type, ignoreCase = true)) {
                "/storage/emulated/0/$path".trimEnd('/')
            } else {
                val volumeName = type.replace("_", " ").replaceFirstChar { it.uppercase() }
                "/$volumeName/$path".trimEnd('/')
            }
        } catch (e: Exception) {
            try {
                val projection = arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        return cursor.getString(0)
                    }
                }
            } catch (_: Exception) {}
            
            val decoded = URLDecoder.decode(uri.toString(), "UTF-8")
            decoded.substringAfterLast("/")
        }
    }
}
