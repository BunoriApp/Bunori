package com.halovoid.bunori.api.loader

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File
import androidx.core.net.toUri

object UpdateInstaller {
    fun installApk(context: Context, apkUriString: String) {
        val apkUri = apkUriString.toUri()
        
        val contentUri = if (apkUri.scheme == "file") {
            val file = File(apkUri.path!!)
            FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        } else {
            apkUri
        }

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(contentUri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        context.startActivity(intent)
    }
}
