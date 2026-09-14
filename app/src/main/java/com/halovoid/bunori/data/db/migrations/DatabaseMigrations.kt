package com.halovoid.bunori.data.db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object DatabaseMigrations {
    val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // 1. Update the 'requests' table
            db.execSQL("ALTER TABLE requests ADD COLUMN progressSuccess INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE requests ADD COLUMN progressFailed INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE requests ADD COLUMN progressCancelled INTEGER NOT NULL DEFAULT 0")

            // 2. Drop and Recreate since SQLite sometimes does not support Rename WELL
            db.execSQL("UPDATE requests SET progressSuccess = progressCurrent")
        }
    }

    val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // No changes between 6 and 7
        }
    }

    val MIGRATION_7_8 = object : Migration(7, 8) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("DELETE FROM requests WHERE type = 'EXPORT';")
        }
    }

    val MIGRATION_8_9 = object : Migration(8, 9) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("PRAGMA foreign_keys = OFF")

            // 1. Recreate artifacts table with CASCADE constraint
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `artifacts_new` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                    `novelUrl` TEXT NOT NULL, 
                    `requestId` TEXT NOT NULL, 
                    `artifactDestination` TEXT NOT NULL, 
                    `artifactName` TEXT NOT NULL, 
                    FOREIGN KEY(`requestId`) REFERENCES `requests`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE 
                )
            """.trimIndent())
            db.execSQL("INSERT INTO artifacts_new (id, novelUrl, requestId, artifactDestination, artifactName) SELECT id, novelUrl, requestId, artifactDestination, artifactName FROM artifacts")
            db.execSQL("DROP TABLE artifacts")
            db.execSQL("ALTER TABLE artifacts_new RENAME TO artifacts")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_artifacts_requestId` ON `artifacts` (`requestId`)")

            // 2. Recreate chapters table with CASCADE constraint on volumeId
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `chapters_new` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                    `novelUrl` TEXT NOT NULL, 
                    `volumeId` TEXT NOT NULL, 
                    `url` TEXT NOT NULL, 
                    `title` TEXT NOT NULL, 
                    `index` INTEGER NOT NULL, 
                    `fileLocation` TEXT, 
                    FOREIGN KEY(`novelUrl`) REFERENCES `novels`(`url`) ON UPDATE NO ACTION ON DELETE CASCADE , 
                    FOREIGN KEY(`volumeId`) REFERENCES `volumes`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE 
                )
            """.trimIndent())
            db.execSQL("INSERT INTO chapters_new (id, novelUrl, volumeId, url, title, `index`, fileLocation) SELECT id, novelUrl, volumeId, url, title, `index`, fileLocation FROM chapters")
            db.execSQL("DROP TABLE chapters")
            db.execSQL("ALTER TABLE chapters_new RENAME TO chapters")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_chapters_novelUrl` ON `chapters` (`novelUrl`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_chapters_volumeId` ON `chapters` (`volumeId`)")

            db.execSQL("PRAGMA foreign_keys = ON")
        }
    }

    val MIGRATION_10_11 = object : Migration(9, 10) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE requests ADD COLUMN rstatus TEXT NOT NULL DEFAULT 'PENDING'")
            db.execSQL("UPDATE requests SET rstatus = status")
        }
    }

    val MIGRATION_11_12 = object : Migration(11, 12) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE novels ADD column titleHash INTEGER DEFAULT NULL")
        }
    }

    val MIGRATION_12_13 = object : Migration(12, 13) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE requests ADD COLUMN attemptCount INTEGER NOT NULL DEFAULT 0")
            db.execSQL("DELETE FROM requests WHERE type IN ('CHAPTER','FULL_NOVEL','VOLUME'");
        }
    }

    val MIGRATION_13_14 = object : Migration(13, 14) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE novels ADD COLUMN coverHttpsUrl TEXT DEFAULT NULL")
        }
    }

    val MIGRATION_14_15 = object : Migration(14, 15) {
        override fun migrate(db: SupportSQLiteDatabase) {
            var columnExists = false
            val cursor = db.query("PRAGMA table_info(chapters)")
            while (cursor.moveToNext()) {
                val nameIndex = cursor.getColumnIndex("name")
                if (nameIndex != -1 && cursor.getString(nameIndex) == "read") {
                    columnExists = true
                    break
                }
            }
            cursor.close()

            if (!columnExists) {
                db.execSQL("ALTER TABLE chapters ADD COLUMN read INTEGER NOT NULL DEFAULT 0")
            }
        }
    }

    val MIGRATION_15_16 = object : Migration(15, 16) {
        override fun migrate(db: SupportSQLiteDatabase) {
            var columnExists = false
            val cursor = db.query("PRAGMA table_info(chapters)")
            while (cursor.moveToNext()) {
                val nameIndex = cursor.getColumnIndex("name")
                if (nameIndex != -1 && cursor.getString(nameIndex) == "scanlationSource") {
                    columnExists = true
                    break
                }
            }
            cursor.close()

            if (!columnExists) {
                db.execSQL("ALTER TABLE chapters ADD COLUMN scanlationSource TEXT NOT NULL DEFAULT 'NotProvided'")
            }
        }
    }

    val MIGRATION_16_17 = object : Migration(16, 17) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("PRAGMA foreign_keys = OFF")
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `chapters_new` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                    `novelUrl` TEXT NOT NULL, 
                    `volumeId` TEXT NOT NULL, 
                    `url` TEXT NOT NULL, 
                    `sourceUrl` TEXT, 
                    `scanlationSource` TEXT NOT NULL, 
                    `title` TEXT NOT NULL, 
                    `index` INTEGER NOT NULL, 
                    `fileLocation` TEXT, 
                    `read` INTEGER NOT NULL, 
                    FOREIGN KEY(`novelUrl`) REFERENCES `novels`(`url`) ON UPDATE NO ACTION ON DELETE CASCADE 
                )
            """.trimIndent())
            db.execSQL("INSERT INTO chapters_new (id, novelUrl, volumeId, url, sourceUrl, scanlationSource, title, `index`, fileLocation, read) SELECT id, novelUrl, volumeId, url, sourceUrl, scanlationSource, title, `index`, fileLocation, read FROM chapters")
            db.execSQL("DROP TABLE chapters")
            db.execSQL("ALTER TABLE chapters_new RENAME TO chapters")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_chapters_novelUrl` ON `chapters` (`novelUrl`)")
            db.execSQL("PRAGMA foreign_keys = ON")
        }
    }

    val MIGRATION_17_18 = object : Migration(17, 18) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("PRAGMA foreign_keys = OFF")
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `chapters_new` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                    `novelUrl` TEXT NOT NULL, 
                    `url` TEXT NOT NULL, 
                    `sourceUrl` TEXT, 
                    `scanlationSource` TEXT NOT NULL, 
                    `title` TEXT NOT NULL, 
                    `index` INTEGER NOT NULL, 
                    `fileLocation` TEXT, 
                    `read` INTEGER NOT NULL, 
                    FOREIGN KEY(`novelUrl`) REFERENCES `novels`(`url`) ON UPDATE NO ACTION ON DELETE CASCADE 
                )
            """.trimIndent())
            db.execSQL("INSERT INTO chapters_new (id, novelUrl, url, sourceUrl, scanlationSource, title, `index`, fileLocation, read) SELECT id, novelUrl, url, sourceUrl, scanlationSource, title, `index`, fileLocation, read FROM chapters")
            db.execSQL("DROP TABLE chapters")
            db.execSQL("ALTER TABLE chapters_new RENAME TO chapters")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_chapters_novelUrl` ON `chapters` (`novelUrl`)")
            db.execSQL("DROP TABLE IF EXISTS `volumes`")
            db.execSQL("PRAGMA foreign_keys = ON")
        }
    }

    val MIGRATION_16_18 = object : Migration(16, 18) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("PRAGMA foreign_keys = OFF")
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `chapters_new` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                    `novelUrl` TEXT NOT NULL, 
                    `url` TEXT NOT NULL, 
                    `sourceUrl` TEXT, 
                    `scanlationSource` TEXT NOT NULL, 
                    `title` TEXT NOT NULL, 
                    `index` INTEGER NOT NULL, 
                    `fileLocation` TEXT, 
                    `read` INTEGER NOT NULL, 
                    FOREIGN KEY(`novelUrl`) REFERENCES `novels`(`url`) ON UPDATE NO ACTION ON DELETE CASCADE 
                )
            """.trimIndent())
            db.execSQL("INSERT INTO chapters_new (id, novelUrl, url, sourceUrl, scanlationSource, title, `index`, fileLocation, read) SELECT id, novelUrl, url, sourceUrl, scanlationSource, title, `index`, fileLocation, read FROM chapters")
            db.execSQL("DROP TABLE chapters")
            db.execSQL("ALTER TABLE chapters_new RENAME TO chapters")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_chapters_novelUrl` ON `chapters` (`novelUrl`)")
            db.execSQL("DROP TABLE IF EXISTS `volumes`")
            db.execSQL("PRAGMA foreign_keys = ON")
        }
    }

    val MIGRATION_18_19 = object : Migration(18, 19) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("""
                UPDATE chapters SET fileLocation = (
                    SELECT c2.fileLocation FROM chapters c2 
                    WHERE c2.novelUrl = chapters.novelUrl AND c2.url = chapters.url AND c2.fileLocation IS NOT NULL 
                    LIMIT 1
                ) WHERE fileLocation IS NULL
            """.trimIndent())
            db.execSQL("""
                DELETE FROM chapters WHERE id NOT IN (
                    SELECT MIN(id) FROM chapters GROUP BY novelUrl, url
                )
            """.trimIndent())
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_chapters_novelUrl_url` ON `chapters` (`novelUrl`, `url`)")
        }
    }

    val MIGRATION_19_20 = object : Migration(19, 20) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("PRAGMA foreign_keys = OFF")

            // 1. Create downloads table
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `downloads` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `novelUrl` TEXT NOT NULL,
                    `chapterUrl` TEXT NOT NULL,
                    `fileLocation` TEXT NOT NULL,
                    `chapterIndex` INTEGER NOT NULL,
                    `chapterTitle` TEXT NOT NULL,
                    `scanlationSource` TEXT NOT NULL,
                    `novelTitle` TEXT NOT NULL,
                    `sizeBytes` INTEGER NOT NULL,
                    `downloadedAt` INTEGER NOT NULL
                )
            """.trimIndent())

            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_downloads_novelUrl_chapterUrl` ON `downloads` (`novelUrl`, `chapterUrl`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_downloads_novelUrl` ON `downloads` (`novelUrl`)")

            // 2. Migrate existing downloaded chapters into downloads table
            db.execSQL("""
                INSERT OR IGNORE INTO `downloads` (
                    `novelUrl`, `chapterUrl`, `fileLocation`, `chapterIndex`, `chapterTitle`,
                    `scanlationSource`, `novelTitle`, `sizeBytes`, `downloadedAt`
                )
                SELECT 
                    c.novelUrl,
                    c.url,
                    c.fileLocation,
                    c.`index`,
                    c.title,
                    c.scanlationSource,
                    COALESCE((SELECT n.title FROM novels n WHERE n.url = c.novelUrl LIMIT 1), ''),
                    0,
                    strftime('%s', 'now') * 1000
                FROM chapters c
                WHERE c.fileLocation IS NOT NULL AND c.fileLocation != ''
            """.trimIndent())

            // 3. Recreate chapters table without fileLocation column
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `chapters_new` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `novelUrl` TEXT NOT NULL,
                    `url` TEXT NOT NULL,
                    `sourceUrl` TEXT,
                    `scanlationSource` TEXT NOT NULL,
                    `title` TEXT NOT NULL,
                    `index` INTEGER NOT NULL,
                    `read` INTEGER NOT NULL,
                    FOREIGN KEY(`novelUrl`) REFERENCES `novels`(`url`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
            """.trimIndent())

            db.execSQL("""
                INSERT INTO `chapters_new` (`id`, `novelUrl`, `url`, `sourceUrl`, `scanlationSource`, `title`, `index`, `read`)
                SELECT `id`, `novelUrl`, `url`, `sourceUrl`, `scanlationSource`, `title`, `index`, `read`
                FROM `chapters`
            """.trimIndent())

            db.execSQL("DROP TABLE `chapters`")
            db.execSQL("ALTER TABLE `chapters_new` RENAME TO `chapters`")

            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_chapters_novelUrl_url` ON `chapters` (`novelUrl`, `url`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_chapters_novelUrl` ON `chapters` (`novelUrl`)")

            db.execSQL("PRAGMA foreign_keys = ON")
        }
    }
}
