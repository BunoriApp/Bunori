package com.halovoid.bunori.ui.feature.downloads

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.halovoid.bunori.api.core.scrapper.Scrapper
import com.halovoid.bunori.data.db.entities.RequestStatus
import com.halovoid.bunori.data.repository.RequestRepository
import com.halovoid.bunori.domain.models.Request
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DownloadViewModel(
    application: Application,
    private val requestRepository: RequestRepository
) : AndroidViewModel(application) {

    val requestHistory: StateFlow<List<Request>> = requestRepository.getRootRequests()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    data class GlobalDownloadStats(val completed: Int, val total: Int)

    val globalStats: StateFlow<GlobalDownloadStats> = requestHistory.map { list ->
        val active = list.filter { 
            it.status == RequestStatus.RUNNING || 
            it.status == RequestStatus.PAUSED || 
            it.status == RequestStatus.PENDING 
        }
        GlobalDownloadStats(
            completed = active.sumOf { it.progressSuccess },
            total = active.sumOf { it.progressTotal }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = GlobalDownloadStats(0, 0)
    )

    val cancellingRequestIds: StateFlow<Set<String>> = requestRepository.cancellingRequestIds
    val activeActionIds: StateFlow<Set<String>> = requestRepository.activeActionIds

    fun cancelRequest(requestId: String) {
        viewModelScope.launch {
            requestRepository.cancelRequest(requestId)
        }
    }

    fun replayRequest(requestId: String) {
        viewModelScope.launch {
            requestRepository.replayRequest(requestId)
        }
    }

    fun resumeRequest(requestId: String) {
        viewModelScope.launch {
            requestRepository.resumeRequest(requestId)
        }
    }

    fun resolveCloudflare(requestId: String, url: String) {
        viewModelScope.launch {
            val success = Scrapper.globalResolver?.resolve(url) ?: false
            if (success) {
                requestRepository.replayRequest(requestId)
            }
        }
    }
}
