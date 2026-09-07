package com.halovoid.lncrawler.api.backup

import android.content.Context
import com.halovoid.lncrawler.data.db.AppDatabase
import com.halovoid.lncrawler.data.db.entities.RequestEntity
import com.halovoid.lncrawler.data.scheduler.jobs.JobHandler
import com.halovoid.lncrawler.data.scheduler.jobs.JobResult
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * add this on the jobs to create it as a job
 * it takes the current database + novels (no artifacts are saved)
 * manifest.json is created
 *  - formatVersion - 1
 *  - appVersion
 *  - databaseVersion
 *  - createdAt
 *  - contents - {"database": true, "chapters": true, "covers": true, "artifacts": false}
 * database.db
 * novels/
 * packaged into .lnbak file stored inside backup/directory
 */
@Suppress("RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
class BackupService(private val context: Context): JobHandler {
    override suspend fun handle(request: RequestEntity): JobResult {
        return try {
            createBackup()
            JobResult.Success
        } catch (e: Exception) {
            JobResult.Failure(e, isRecoverable = true)
        }
    }

    fun createBackup(): File {
        val backupDir = File(context.filesDir, "backup").apply { mkdirs() }
        val timestamp = System.currentTimeMillis()
        val backupFile = File(backupDir, "backup_$timestamp.lnback")

        val dbFile = context.getDatabasePath("lncrawler.db")
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        val appVersion = packageInfo.versionName ?: "1.0"
        val databaseVersion = AppDatabase.getDatabase(context).openHelper.readableDatabase.version

        val manifestJson = JSONObject().apply {
            put("formatVersion", 1)
            put("appVersion", appVersion)
            put("databaseVersion", databaseVersion)
            put("createdAt", timestamp)
            put("contents", JSONObject().apply {
                put("database", true)
                put("chapters", true)
                put("covers", true)
                put("artifacts", false)
            })
        }

        FileOutputStream(backupFile).use { fos ->
            ZipOutputStream(fos).use {zos ->
                zos.putNextEntry(ZipEntry("mainfest.json"))
                zos.write(manifestJson.toString(2).toByteArray())
                zos.closeEntry()

                if (dbFile.exists()) {
                    zos.putNextEntry(ZipEntry("database.db"))
                    dbFile.inputStream().use { it.copyTo(zos) }
                    zos.closeEntry()
                }

                val novelsDir = File(context.filesDir, "novels")
                if (novelsDir.exists() && novelsDir.isDirectory) {
                    zipDirectory(novelsDir, "novels", zos)
                }
            }
        }

        return backupFile
    }

    private fun zipDirectory(dir: File, baseName: String, zos: ZipOutputStream) {
        dir.listFiles().forEach { file ->
            val entryName = "$baseName/${file.name}"
            if (file.isDirectory) {
                zipDirectory(file, entryName, zos)
            } else {
                zos.putNextEntry(ZipEntry(entryName))
                file.inputStream().use { it.copyTo(zos) }
                zos.closeEntry()
            }
        }
    }
}