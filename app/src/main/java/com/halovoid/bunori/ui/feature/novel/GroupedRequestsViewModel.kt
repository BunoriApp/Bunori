package com.halovoid.bunori.ui.feature.novel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.halovoid.bunori.data.db.entities.JobStatus
import com.halovoid.bunori.data.repository.BatchRepository
import com.halovoid.bunori.domain.models.Batch
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

sealed interface RequestScope {
    data object All : RequestScope
    data class ByNovel(val novelUrl: String) : RequestScope
    data class ByDependency(val requestId: String) : RequestScope
}

class GroupedRequestsViewModel(
    application: Application,
    private val batchRepository: BatchRepository
) : AndroidViewModel(application) {

    private val _scope = MutableStateFlow<RequestScope?>(null)
    
    private val _statusFilters = MutableStateFlow<Map<JobStatus, FilterState>>(emptyMap())
    val statusFilters: StateFlow<Map<JobStatus, FilterState>> = _statusFilters.asStateFlow()

    fun setStatusFilter(status: JobStatus, state: FilterState) {
        val current = _statusFilters.value.toMutableMap()
        if (state == FilterState.NONE) {
            current.remove(status)
        } else {
            current[status] = state
        }
        _statusFilters.value = current
    }

    val cancellingRequestIds: StateFlow<Set<String>> = batchRepository.cancellingRequestIds
    val activeActionIds: StateFlow<Set<String>> = batchRepository.activeActionIds

    @OptIn(ExperimentalCoroutinesApi::class)
    val allRequests: StateFlow<List<Batch>> = _scope.filterNotNull()
        .flatMapLatest { scope ->
            when (scope) {
                is RequestScope.All -> batchRepository.getRootRequests()
                is RequestScope.ByNovel -> batchRepository.getRootRequestByNovelFlow(scope.novelUrl)
                is RequestScope.ByDependency -> batchRepository.getRequestsByDependenceFlow(scope.requestId)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val requests: StateFlow<List<Batch>> = combine(allRequests, _statusFilters) { list, statusMap ->
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
        _scope.value = when (contextType.uppercase()) {
            "ALL" -> RequestScope.All
            "NOVEL" -> RequestScope.ByNovel(contextValue)
            "DEPENDENCY" -> RequestScope.ByDependency(contextValue)
            else -> RequestScope.All
        }
    }

    fun loadScope(scope: RequestScope) {
        _scope.value = scope
    }

    fun replayRequest(requestId: String) {
        viewModelScope.launch {
            batchRepository.replayRequest(requestId)
        }
    }

    fun resumeRequest(requestId: String) {
        viewModelScope.launch {
            batchRepository.resumeRequest(requestId)
        }
    }

    fun cancelRequest(requestId: String) {
        viewModelScope.launch {
            batchRepository.cancelRequest(requestId)
        }
    }

    fun resolveCloudflare(requestId: String, url: String) {
        viewModelScope.launch {
            com.halovoid.bunori.api.core.scrapper.Scrapper.globalResolver?.resolve(url)
            batchRepository.replayRequest(requestId)
        }
    }
}
