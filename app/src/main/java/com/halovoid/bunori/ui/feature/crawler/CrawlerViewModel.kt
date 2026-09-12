package com.halovoid.bunori.ui.feature.crawler

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.halovoid.bunori.api.core.crawler.Crawler
import com.halovoid.bunori.api.core.crawler.CrawlerFactory
import com.halovoid.bunori.api.loader.SourceLoader
import com.halovoid.bunori.data.repository.PreferenceRepository
import com.halovoid.bunori.data.repository.UpdateRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class SyncState {
    object Idle : SyncState()
    object Loading : SyncState()
    data class Success(val message: String) : SyncState()
    data class Incompatible(val minVersion: String) : SyncState()
    data class Error(val error: String) : SyncState()
}

class CrawlerViewModel(
    application: Application,
    private val preferenceRepository: PreferenceRepository
) : AndroidViewModel(application) {
    private val sourceLoader = SourceLoader(application)
    
    val crawlers: StateFlow<List<Crawler>> = CrawlerFactory.crawlersFlow

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    val isUpdateAvailable: StateFlow<Boolean> = UpdateRepository.getInstance(application)
        .isCrawlerUpdateAvailable

    val showSyncOption: StateFlow<Boolean> = combine(
        preferenceRepository.currentDexTag,
        isUpdateAvailable,
        crawlers
    ) { currentTag, updateAvailable, crawlerList ->
        currentTag == null || updateAvailable || crawlerList.isEmpty()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    private var latestReleaseInfo: SourceLoader.ReleaseInfo? = null

    init {
        viewModelScope.launch {
            sourceLoader.loadLocalSources()
        }
        checkForUpdates()
    }

    fun checkForUpdates() {
        viewModelScope.launch {
            try {
                UpdateRepository.getInstance(getApplication()).checkForUpdates()
            } catch (e: Exception) {
                android.util.Log.e("CrawlerViewModel", "Failed to check for updates: ${e.message}", e)
            }
        }
    }

    fun syncCrawlers() {
        viewModelScope.launch {
            _syncState.value = SyncState.Loading
            try {
                sourceLoader.loadSources(latestReleaseInfo)
                _syncState.value = SyncState.Success("Crawlers updated successfully")
                UpdateRepository.getInstance(getApplication()).setCrawlerUpdateAvailable(false)
            } catch (e: SourceLoader.IncompatibleAppException) {
                _syncState.value = SyncState.Incompatible(e.minVersion)
            } catch (e: Exception) {
                _syncState.value = SyncState.Error(e.message ?: "Failed to sync crawlers")
            }
        }
    }

    fun resetSyncState() {
        _syncState.value = SyncState.Idle
    }
}
