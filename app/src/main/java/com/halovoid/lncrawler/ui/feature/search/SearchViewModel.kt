package com.halovoid.lncrawler.ui.feature.search

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.halovoid.lncrawler.data.factory.RequestFactory
import com.halovoid.lncrawler.data.repository.RequestRepository
import com.halovoid.lncrawler.data.repository.SearchRepository
import com.halovoid.lncrawler.data.scheduler.services.SchedulerService
import com.halovoid.lncrawler.domain.models.SearchItem
import com.halovoid.lncrawler.domain.models.SearchResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class SearchState {
    object Idle : SearchState()
    object Loading : SearchState()
    data class Success(val response: SearchResponse) : SearchState()
    data class Error(val message: String) : SearchState()
}

class SearchViewModel(
    application: Application,
    private val requestRepository: RequestRepository = RequestRepository.getInstance(application),
    private val searchRepository: SearchRepository = SearchRepository(),
    private val requestFactory: RequestFactory = RequestFactory()
) : AndroidViewModel(application) {

    private val _searchState = MutableStateFlow<SearchState>(SearchState.Idle)
    val searchState: StateFlow<SearchState> = _searchState.asStateFlow()

    fun search(query: String) {
        if (query.isBlank()) return
        
        viewModelScope.launch {
            _searchState.value = SearchState.Loading
            try {
                val response = searchRepository.search(query)
                _searchState.value = SearchState.Success(response)
            } catch (e: Exception) {
                val isNetworkOrServerIssue = e is java.io.IOException || 
                    e.message?.contains("failed", ignoreCase = true) == true ||
                    e.message?.contains("connect", ignoreCase = true) == true ||
                    e.message?.contains("timeout", ignoreCase = true) == true
                
                val userFriendlyMessage = if (isNetworkOrServerIssue) {
                    "Server is down or under maintenance. Please try again later."
                } else {
                    e.message ?: "Server is down or under maintenance. Please try again later."
                }
                _searchState.value = SearchState.Error(userFriendlyMessage)
            }
        }
    }
    
    fun resetState() {
        _searchState.value = SearchState.Idle
    }

    fun startCrawl(item: SearchItem) {
        viewModelScope.launch {
            val request = requestFactory.metadataFromSearchItem(item)

            requestRepository.insertRequests(listOf(request))
            SchedulerService.startService(getApplication())
        }
    }
}
