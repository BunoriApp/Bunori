package com.halovoid.bunori.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.halovoid.bunori.data.db.dao.ArtifactDao
import com.halovoid.bunori.data.db.dao.BatchDao
import com.halovoid.bunori.data.db.dao.ChapterDao
import com.halovoid.bunori.data.db.dao.NovelDao
import com.halovoid.bunori.data.db.dao.TaskDao
import com.halovoid.bunori.data.db.entities.ArtifactEntity
import com.halovoid.bunori.data.db.entities.BatchEntity
import com.halovoid.bunori.data.db.entities.ChapterEntity
import com.halovoid.bunori.data.db.entities.NovelEntity
import com.halovoid.bunori.data.db.entities.TaskEntity
import com.halovoid.bunori.data.db.migrations.DatabaseMigrations

/**
 * Main Room database for the application.
 * Part of the Data layer, responsible for local persistence.
 */
@Database(
    entities = [NovelEntity::class, ChapterEntity::class, BatchEntity::class, TaskEntity::class, ArtifactEntity::class],
    version = 21,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun novelDao(): NovelDao
    abstract fun chapterDao(): ChapterDao
    abstract fun batchDao(): BatchDao
    abstract fun taskDao(): TaskDao
    abstract fun artifactDao(): ArtifactDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "bunori.db"
                )
                    .addMigrations(
                        DatabaseMigrations.MIGRATION_5_6,
                        DatabaseMigrations.MIGRATION_6_7,
                        DatabaseMigrations.MIGRATION_7_8,
                        DatabaseMigrations.MIGRATION_8_9,
                        DatabaseMigrations.MIGRATION_10_11,
                        DatabaseMigrations.MIGRATION_11_12,
                        DatabaseMigrations.MIGRATION_12_13,
                        DatabaseMigrations.MIGRATION_13_14,
                        DatabaseMigrations.MIGRATION_14_15,
                        DatabaseMigrations.MIGRATION_15_16,
                        DatabaseMigrations.MIGRATION_16_17,
                        DatabaseMigrations.MIGRATION_17_18,
                        DatabaseMigrations.MIGRATION_16_18
                    )
                    .fallbackToDestructiveMigration(true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
