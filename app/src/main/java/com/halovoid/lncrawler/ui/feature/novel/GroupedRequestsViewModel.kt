package com.halovoid.lncrawler.ui.feature.novel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.halovoid.lncrawler.data.db.entities.RequestStatus
import com.halovoid.lncrawler.data.repository.RequestRepository
import com.halovoid.lncrawler.domain.models.Request
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class FilterState {
    NONE, INCLUDE, EXCLUDE;

    fun next(): FilterState = when (this) {
        NONE -> INCLUDE
        INCLUDE -> EXCLUDE
        EXCLUDE -> NONE
    }
}

class GroupedRequestsViewModel(
    application: Application,
    private val requestRepository: RequestRepository
) : AndroidViewModel(application) {

    private val _context = MutableStateFlow<Pair<String, String>?>(null)
    
    private val _statusFilters = MutableStateFlow<Map<RequestStatus, FilterState>>(emptyMap())
    val statusFilters: StateFlow<Map<RequestStatus, FilterState>> = _statusFilters.asStateFlow()

    fun setStatusFilter(status: RequestStatus, state: FilterState) {
        val current = _statusFilters.value.toMutableMap()
        if (state == FilterState.NONE) {
            current.remove(status)
        } else {
            current[status] = state
        }
        _statusFilters.value = current
    }

    val cancellingRequestIds: StateFlow<Set<String>> = requestRepository.cancellingRequestIds
    val activeActionIds: StateFlow<Set<String>> = requestRepository.activeActionIds

    @OptIn(ExperimentalCoroutinesApi::class)
    val allRequests: StateFlow<List<Request>> = _context.filterNotNull()
        .flatMapLatest { (type, value) ->
            when (type) {
                "ALL" -> requestRepository.getRootRequests()
                "NOVEL" -> requestRepository.getRootRequestByNovelFlow(value)
                "DEPENDENCY" -> requestRepository.getRequestsByDependenceFlow(value)
                else -> flowOf(emptyList())
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val requests: StateFlow<List<Request>> = combine(allRequests, _statusFilters) { list, statusMap ->
        var filtered = list

        val excludedStatuses = statusMap.filter { it.value == FilterState.EXCLUDE }.keys
        if (excludedStatuses.isNotEmpty()) {
            filtered = filtered.filter { it.status !in excludedStatuses }
        }

        val includedStatuses = statusMap.filter { it.value == FilterState.INCLUDE }.keys
        if (includedStatuses.isNotEmpty()) {
            filtered = filtered.filter { it.status in includedStatuses }
        }

        filtered
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun loadRequests(contextType: String, contextValue: String) {
        _context.value = contextType to contextValue
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

    fun cancelRequest(requestId: String) {
        viewModelScope.launch {
            requestRepository.cancelRequest(requestId)
        }
    }

    fun resolveCloudflare(requestId: String, url: String) {
        viewModelScope.launch {
            com.halovoid.lncrawler.api.core.scrapper.Scrapper.globalResolver?.resolve(url)
            requestRepository.replayRequest(requestId)
        }
    }
}
