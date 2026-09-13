package com.halovoid.bunori.data.repository

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.flow.Flow
import com.halovoid.bunori.data.preferences.appDataStore
import kotlinx.coroutines.flow.map

private val EXPORT_FOLDER_URI = stringPreferencesKey("export_folder_uri")
private val ONBOARDING_COMPLETED = stringPreferencesKey("onboarding_completed")
private val LAST_SYNC_TIME = stringPreferencesKey("last_sync_time")
private val CURRENT_DEX_TAG = stringPreferencesKey("current_dex_tag")
private val BETA_MODE_APP = booleanPreferencesKey("beta_mode_app")
private val BETA_MODE_CRAWLERS = booleanPreferencesKey("beta_mode_crawlers")
private val IGNORE_IMAGES = booleanPreferencesKey("ignore_images")
private val MAX_CONCURRENT_JOBS = intPreferencesKey("max_concurrent_jobs")
private val SEARCH_COMPACT_VIEW = booleanPreferencesKey("search_compact_view")
private val LIBRARY_COMPACT_VIEW = booleanPreferencesKey("library_compact_view")
private val DEFAULT_CHAPTER_DOWNLOAD_FILTER = stringPreferencesKey("default_chapter_download_filter")
private val DEFAULT_CHAPTER_SORT_TYPE = stringPreferencesKey("default_chapter_sort_type")
private val DEFAULT_CHAPTER_SORT_ORDER = stringPreferencesKey("default_chapter_sort_order")
private val DEFAULT_SOURCE_FILTER = stringPreferencesKey("default_source_filter")
private val BACKUP_FREQUENCY = stringPreferencesKey("backup_frequency")
private val THEME_MODE = stringPreferencesKey("theme_mode")
private val SELECTED_THEME_ID = stringPreferencesKey("selected_theme_id")
private val IS_AMOLED_MODE = booleanPreferencesKey("is_amoled_mode")
private val EXTENSION_REPO_URL = stringPreferencesKey("extension_repo_url")
const val DEFAULT_EXTENSION_REPO_URL = "https://bunoriapp.github.io/BunoriSources/index.min.json"

class PreferenceRepository private constructor(
    private val context: Context
) {
    companion object {
        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var INSTANCE: PreferenceRepository? = null

        fun getInstance(context: Context): PreferenceRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: PreferenceRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    val exportFolderUri: Flow<Uri?> =
        context.appDataStore.data.map { preferences ->
            preferences[EXPORT_FOLDER_URI]?.let(Uri::parse)
        }

    val isOnboardingCompleted: Flow<Boolean> =
        context.appDataStore.data.map { preferences ->
            preferences[ONBOARDING_COMPLETED]?.toBoolean() ?: false
        }

    val currentDexTag: Flow<String?> =
        context.appDataStore.data.map { preferences ->
            preferences[CURRENT_DEX_TAG]
        }

    val betaModeApp: Flow<Boolean> =
        context.appDataStore.data.map { preferences ->
            preferences[BETA_MODE_APP] ?: false
        }

    val betaModeCrawlers: Flow<Boolean> =
        context.appDataStore.data.map { preferences ->
            preferences[BETA_MODE_CRAWLERS] ?: false
        }

    val ignoreImages: Flow<Boolean> =
        context.appDataStore.data.map { preferences ->
            preferences[IGNORE_IMAGES] ?: false
        }

    val maxConcurrentJobs: Flow<Int> =
        context.appDataStore.data.map { preferences ->
            preferences[MAX_CONCURRENT_JOBS] ?: 3
        }

    val searchCompactView: Flow<Boolean> =
        context.appDataStore.data.map { preferences ->
            preferences[SEARCH_COMPACT_VIEW] ?: false
        }

    val libraryCompactView: Flow<Boolean> =
        context.appDataStore.data.map { preferences ->
            preferences[LIBRARY_COMPACT_VIEW] ?: false
        }

    val defaultChapterDownloadFilter: Flow<String> =
        context.appDataStore.data.map { preferences ->
            preferences[DEFAULT_CHAPTER_DOWNLOAD_FILTER] ?: "ALL"
        }

    val defaultChapterSortType: Flow<String> =
        context.appDataStore.data.map { preferences ->
            preferences[DEFAULT_CHAPTER_SORT_TYPE] ?: "CHAPTER_NUMBER"
        }

    val defaultChapterSortOrder: Flow<String> =
        context.appDataStore.data.map { preferences ->
            preferences[DEFAULT_CHAPTER_SORT_ORDER] ?: "ASCENDING"
        }

    val defaultSourceFilter: Flow<String> =
        context.appDataStore.data.map { preferences ->
            preferences[DEFAULT_SOURCE_FILTER] ?: "ALL"
        }

    val backupFrequency: Flow<String> =
        context.appDataStore.data.map { preferences ->
            preferences[BACKUP_FREQUENCY] ?: "Off"
        }

    val themeMode: Flow<String> =
        context.appDataStore.data.map { preferences ->
            preferences[THEME_MODE] ?: "SYSTEM"
        }

    val selectedThemeId: Flow<String> =
        context.appDataStore.data.map { preferences ->
            preferences[SELECTED_THEME_ID] ?: "DEFAULT"
        }

    val isAmoledMode: Flow<Boolean> =
        context.appDataStore.data.map { preferences ->
            preferences[IS_AMOLED_MODE] ?: false
        }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.appDataStore.edit { preferences ->
            preferences[ONBOARDING_COMPLETED] = completed.toString()
        }
    }

    suspend fun setExportFolder(uri: Uri) {
        context.appDataStore.edit { preferences ->
            preferences[EXPORT_FOLDER_URI] = uri.toString()
        }
    }

    suspend fun clearExportFolder() {
        context.appDataStore.edit { preferences ->
            preferences.remove(EXPORT_FOLDER_URI)
        }
    }

    suspend fun setCurrentDexTag(tag: String) {
        context.appDataStore.edit { preferences ->
            preferences[CURRENT_DEX_TAG] = tag
        }
    }

    suspend fun setBetaModeApp(enabled: Boolean) {
        context.appDataStore.edit { preferences ->
            preferences[BETA_MODE_APP] = enabled
        }
    }

    suspend fun setBetaModeCrawlers(enabled: Boolean) {
        context.appDataStore.edit { preferences ->
            preferences[BETA_MODE_CRAWLERS] = enabled
        }
    }

    suspend fun setIgnoreImages(enabled: Boolean) {
        context.appDataStore.edit { preferences ->
            preferences[IGNORE_IMAGES] = enabled
        }
    }

    suspend fun setMaxConcurrentJobs(jobs: Int) {
        context.appDataStore.edit { preferences ->
            preferences[MAX_CONCURRENT_JOBS] = jobs
        }
    }

    suspend fun setSearchCompactView(compact: Boolean) {
        context.appDataStore.edit { preferences ->
            preferences[SEARCH_COMPACT_VIEW] = compact
        }
    }

    suspend fun setLibraryCompactView(compact: Boolean) {
        context.appDataStore.edit { preferences ->
            preferences[LIBRARY_COMPACT_VIEW] = compact
        }
    }

    suspend fun setDefaultChapterDownloadFilter(filter: String) {
        context.appDataStore.edit { preferences ->
            preferences[DEFAULT_CHAPTER_DOWNLOAD_FILTER] = filter
        }
    }

    suspend fun setDefaultChapterSortType(type: String) {
        context.appDataStore.edit { preferences ->
            preferences[DEFAULT_CHAPTER_SORT_TYPE] = type
        }
    }

    suspend fun setDefaultChapterSortOrder(order: String) {
        context.appDataStore.edit { preferences ->
            preferences[DEFAULT_CHAPTER_SORT_ORDER] = order
        }
    }

    suspend fun setDefaultSourceFilter(filter: String) {
        context.appDataStore.edit { preferences ->
            preferences[DEFAULT_SOURCE_FILTER] = filter
        }
    }

    suspend fun setBackupFrequency(frequency: String) {
        context.appDataStore.edit { preferences ->
            preferences[BACKUP_FREQUENCY] = frequency
        }
    }

    suspend fun setThemeMode(mode: String) {
        context.appDataStore.edit { preferences ->
            preferences[THEME_MODE] = mode
        }
    }

    suspend fun setSelectedThemeId(themeId: String) {
        context.appDataStore.edit { preferences ->
            preferences[SELECTED_THEME_ID] = themeId
        }
    }

    suspend fun setAmoledMode(enabled: Boolean) {
        context.appDataStore.edit { preferences ->
            preferences[IS_AMOLED_MODE] = enabled
        }
    }

    val extensionRepoUrl: Flow<String> =
        context.appDataStore.data.map { preferences ->
            val stored = preferences[EXTENSION_REPO_URL]
            if (stored.isNullOrBlank() || isDeprecatedRepoUrl(stored)) {
                DEFAULT_EXTENSION_REPO_URL
            } else {
                stored
            }
        }

    suspend fun setExtensionRepoUrl(url: String) {
        context.appDataStore.edit { preferences ->
            val trimmed = url.trim()
            if (trimmed.isEmpty() || trimmed == DEFAULT_EXTENSION_REPO_URL || isDeprecatedRepoUrl(trimmed)) {
                preferences.remove(EXTENSION_REPO_URL)
            } else {
                preferences[EXTENSION_REPO_URL] = trimmed
            }
        }
    }
}

private fun isDeprecatedRepoUrl(url: String): Boolean {
    return url.contains("Binit06") ||
           url.contains("/releases/download/") ||
           url.endsWith("/repo/index.json") ||
           url.endsWith("/repo/index.min.json")
}

