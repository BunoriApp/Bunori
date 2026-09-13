package com.halovoid.bunori.ui.feature.settings

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.halovoid.bunori.BuildConfig
import com.halovoid.bunori.data.scheduler.workers.BackupScheduler
import com.halovoid.bunori.api.loader.AppUpdateManager
import com.halovoid.bunori.api.loader.UpdateDownloader
import com.halovoid.bunori.api.loader.UpdateInstaller
import com.halovoid.bunori.data.repository.PreferenceRepository
import com.halovoid.bunori.data.repository.UpdateRepository
import com.halovoid.bunori.extension.manager.ExtensionManager
import com.halovoid.bunori.ui.core.theme.ThemeMode
import com.halovoid.bunori.ui.feature.novel.DownloadFilter
import com.halovoid.bunori.ui.feature.novel.SortOrder
import com.halovoid.bunori.ui.feature.novel.SortType
import com.halovoid.bunori.ui.feature.onboarding.UriUtils
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed class AppUpdateState {
    object Idle : AppUpdateState()
    object Loading : AppUpdateState()
    data class UpdateAvailable(
        val tagName: String,
        val releaseUrl: String,
        val apkDownloadUrl: String?,
        val releaseNotes: String? = null,
        val publishedAt: String? = null
    ) : AppUpdateState()
    object Downloading : AppUpdateState()
    data class ReadyToInstall(val uri: String) : AppUpdateState()
    object Installing : AppUpdateState()
    object UpToDate : AppUpdateState()
    data class Error(val message: String) : AppUpdateState()
}

class SettingsViewModel(
    application: Application,
    private val preferenceRepository: PreferenceRepository = PreferenceRepository.getInstance(application),
    private val updateRepository: UpdateRepository = UpdateRepository.getInstance(application)
) : AndroidViewModel(application) {
    private val updateDownloader = UpdateDownloader(application)

    private val _refreshing = MutableStateFlow(false)
    private val _downloadUri = MutableStateFlow<String?>(null)
    private val _isDownloading = MutableStateFlow(false)
    private val _isInstalling = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)

    val betaModeApp: StateFlow<Boolean> = preferenceRepository.betaModeApp.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    val betaModeCrawlers: StateFlow<Boolean> = preferenceRepository.betaModeCrawlers.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    val extensionRepoUrl: StateFlow<String> = preferenceRepository.extensionRepoUrl.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = com.halovoid.bunori.data.repository.DEFAULT_EXTENSION_REPO_URL
    )

    val ignoreImages: StateFlow<Boolean> = preferenceRepository.ignoreImages.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    val maxConcurrentJobs: StateFlow<Int> = preferenceRepository.maxConcurrentJobs.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 3
    )

    val searchCompactView: StateFlow<Boolean> = preferenceRepository.searchCompactView.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    val libraryCompactView: StateFlow<Boolean> = preferenceRepository.libraryCompactView.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    val defaultChapterDownloadFilter: StateFlow<DownloadFilter> = preferenceRepository.defaultChapterDownloadFilter
        .map { filterName ->
            runCatching { DownloadFilter.valueOf(filterName) }.getOrDefault(DownloadFilter.ALL)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = DownloadFilter.ALL
        )

    val defaultChapterSortType: StateFlow<SortType> = preferenceRepository.defaultChapterSortType
        .map { sortTypeName ->
            runCatching { SortType.valueOf(sortTypeName) }.getOrDefault(SortType.CHAPTER_NUMBER)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SortType.CHAPTER_NUMBER
        )

    val defaultChapterSortOrder: StateFlow<SortOrder> = preferenceRepository.defaultChapterSortOrder
        .map { orderName ->
            runCatching { SortOrder.valueOf(orderName) }.getOrDefault(SortOrder.ASCENDING)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SortOrder.ASCENDING
        )

    val defaultSourceFilter: StateFlow<String> = preferenceRepository.defaultSourceFilter.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = "ALL"
    )

    val backupFrequency: StateFlow<String> = preferenceRepository.backupFrequency.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = "Off"
    )

    val themeMode: StateFlow<ThemeMode> = preferenceRepository.themeMode
        .map { modeName ->
            runCatching { ThemeMode.valueOf(modeName) }.getOrDefault(ThemeMode.SYSTEM)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ThemeMode.SYSTEM
        )

    val selectedThemeId: StateFlow<String> = preferenceRepository.selectedThemeId.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = "DEFAULT"
    )

    val isAmoledMode: StateFlow<Boolean> = preferenceRepository.isAmoledMode.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    val exportFolderUri: StateFlow<Uri?> = preferenceRepository.exportFolderUri.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    val friendlyPath: Flow<String> = exportFolderUri.map { uri ->
        if (uri == null) "" else UriUtils.getFriendlyPath(getApplication(), uri)
    }

    private val localUpdateState: Flow<AppUpdateState?> = combine(
        _isDownloading, _downloadUri, _isInstalling, _error
    ) { downloading, uri, installing, error ->
        when {
            error != null -> AppUpdateState.Error(error)
            installing -> AppUpdateState.Installing
            uri != null -> AppUpdateState.ReadyToInstall(uri)
            downloading -> AppUpdateState.Downloading
            else -> null
        }
    }

    val updateState: StateFlow<AppUpdateState> = combine(
        updateRepository.latestAppRelease,
        _refreshing,
        localUpdateState
    ) { latest, refreshing, localState ->
        localState ?: when {
            refreshing -> AppUpdateState.Loading
            latest != null -> {
                val currentVersion = BuildConfig.VERSION_NAME
                if (AppUpdateManager.isUpdateAvailable(currentVersion, latest.tagName)) {
                    AppUpdateState.UpdateAvailable(
                        latest.tagName,
                        latest.releaseUrl,
                        latest.apkDownloadUrl,
                        latest.body,
                        latest.publishedAt
                    )
                } else {
                    AppUpdateState.UpToDate
                }
            }
            else -> AppUpdateState.Idle
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AppUpdateState.Idle
    )

    init {
        checkForUpdates()
    }

    fun checkForUpdates() {
        viewModelScope.launch {
            _refreshing.value = true
            _error.value = null
            try {
                updateRepository.checkForUpdates()
            } catch (e: Exception) {
                _error.value = "Failed to check for updates"
            } finally {
                _refreshing.value = false
            }
        }
    }

    fun startUpdateDownload(url: String) {
        viewModelScope.launch {
            _isDownloading.value = true
            _error.value = null
            try {
                val downloadId = updateDownloader.downloadUpdate(url, "Bunori_update.apk")
                updateDownloader.getDownloadStatus(downloadId).collect { status ->
                    when (status) {
                        is UpdateDownloader.DownloadStatus.Success -> {
                            _downloadUri.value = status.uri
                            _isDownloading.value = false
                        }
                        is UpdateDownloader.DownloadStatus.Error -> {
                            _error.value = status.message
                            _isDownloading.value = false
                        }
                    }
                }
            } catch (e: Exception) {
                _error.value = "Download failed: ${e.message}"
                _isDownloading.value = false
            }
        }
    }

    fun installUpdate(context: Context, uri: String) {
        try {
            _error.value = null
            UpdateInstaller.installApk(context, uri)
            _isInstalling.value = true
        } catch (e: Exception) {
            _error.value = "Failed to start installation: ${e.message}"
        }
    }

    fun setBetaModeApp(enabled: Boolean) {
        viewModelScope.launch {
            preferenceRepository.setBetaModeApp(enabled)
            checkForUpdates()
        }
    }

    fun setBetaModeCrawlers(enabled: Boolean) {
        viewModelScope.launch {
            preferenceRepository.setBetaModeCrawlers(enabled)
            checkForUpdates()
        }
    }

    fun setIgnoreImages(enabled: Boolean) {
        viewModelScope.launch {
            preferenceRepository.setIgnoreImages(enabled)
        }
    }

    fun setMaxConcurrentJobs(jobs: Int) {
        viewModelScope.launch {
            preferenceRepository.setMaxConcurrentJobs(jobs)
        }
    }

    fun setExportFolder(uri: Uri) {
        viewModelScope.launch {
            preferenceRepository.setExportFolder(uri)
        }
    }

    fun resetOnboarding() {
        viewModelScope.launch {
            preferenceRepository.setOnboardingCompleted(false)
        }
    }

    fun setSearchCompactView(compact: Boolean) {
        viewModelScope.launch {
            preferenceRepository.setSearchCompactView(compact)
        }
    }

    fun setLibraryCompactView(compact: Boolean) {
        viewModelScope.launch {
            preferenceRepository.setLibraryCompactView(compact)
        }
    }

    fun setDefaultChapterDownloadFilter(filter: DownloadFilter) {
        viewModelScope.launch {
            preferenceRepository.setDefaultChapterDownloadFilter(filter.name)
        }
    }

    fun setDefaultChapterSortType(sortType: SortType) {
        viewModelScope.launch {
            preferenceRepository.setDefaultChapterSortType(sortType.name)
        }
    }

    fun setDefaultChapterSortOrder(sortOrder: SortOrder) {
        viewModelScope.launch {
            preferenceRepository.setDefaultChapterSortOrder(sortOrder.name)
        }
    }

    fun setDefaultSourceFilter(sourceFilter: String) {
        viewModelScope.launch {
            preferenceRepository.setDefaultSourceFilter(sourceFilter)
        }
    }

    fun setBackupFrequency(frequency: String) {
        viewModelScope.launch {
            preferenceRepository.setBackupFrequency(frequency)
            BackupScheduler.scheduleBackupWork(getApplication(), frequency)
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            preferenceRepository.setThemeMode(mode.name)
        }
    }

    fun setSelectedThemeId(themeId: String) {
        viewModelScope.launch {
            preferenceRepository.setSelectedThemeId(themeId)
        }
    }

    fun setAmoledMode(enabled: Boolean) {
        viewModelScope.launch {
            preferenceRepository.setAmoledMode(enabled)
        }
    }

    fun setExtensionRepoUrl(url: String) {
        viewModelScope.launch {
            preferenceRepository.setExtensionRepoUrl(url)
        }
    }

    fun installExtensionFromUri(uri: Uri, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val extensionManager = ExtensionManager.getInstance(getApplication())
            val result = extensionManager.installFromUri(uri)
            result.onSuccess { loaded ->
                onResult(true, "Installed ${loaded.manifest.name}")
            }.onFailure { err ->
                onResult(false, "Failed to install: ${err.message}")
            }
        }
    }
}
