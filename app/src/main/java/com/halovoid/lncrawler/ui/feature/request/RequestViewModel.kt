package com.halovoid.lncrawler.ui.feature.request

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.halovoid.lncrawler.api.core.crawler.CrawlerFactory
import com.halovoid.lncrawler.api.core.scrapper.Scrapper
import com.halovoid.lncrawler.data.factory.RequestFactory
import com.halovoid.lncrawler.data.repository.IndexRepository
import com.halovoid.lncrawler.data.repository.NovelRepository
import com.halovoid.lncrawler.data.repository.RequestRepository
import com.halovoid.lncrawler.domain.models.Novel
import com.halovoid.lncrawler.domain.usecase.SaveNovelResult
import com.halovoid.lncrawler.domain.usecase.SaveNovelUseCase
import com.halovoid.lncrawler.domain.usecase.StartNovelCrawlUseCase
import com.halovoid.lncrawler.ui.core.logging.AppLog
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class RequestViewModel(
    application: Application,
    private val requestRepository: RequestRepository,
    private val novelRepository: NovelRepository = NovelRepository.getInstance(application),
    private val requestFactory: RequestFactory = RequestFactory(),
    private val saveNovelUseCase: SaveNovelUseCase = SaveNovelUseCase(novelRepository),
    private val startNovelCrawlUseCase: StartNovelCrawlUseCase = StartNovelCrawlUseCase(requestRepository, requestFactory)
) : AndroidViewModel(application) {

    private val indexRepository: IndexRepository = IndexRepository()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _novelPreview = MutableStateFlow<Novel?>(null)
    val novelPreview: StateFlow<Novel?> = _novelPreview.asStateFlow()

    private val _previewUrl = MutableStateFlow<String?>(null)
    val previewUrl: StateFlow<String?> = _previewUrl.asStateFlow()

    private val _similarNovels = MutableStateFlow<List<Novel>>(emptyList())
    val similarNovels: StateFlow<List<Novel>> = _similarNovels.asStateFlow()

    val libraryUrls: StateFlow<Set<String>> = novelRepository.getAllNovels()
        .map { novels -> novels.map { it.url }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    private val _addSuccess = MutableSharedFlow<Unit>()
    val addSuccess = _addSuccess.asSharedFlow()

    private val _uiEvents = Channel<RequestUiEvent>()
    val uiEvents = _uiEvents.receiveAsFlow()

    val cancellingRequestIds: StateFlow<Set<String>> = requestRepository.cancellingRequestIds
    val activeActionIds: StateFlow<Set<String>> = requestRepository.activeActionIds

    fun resolveCloudflare(requestId: String, url: String) {
        viewModelScope.launch {
            AppLog.i("RequestViewModel", "Starting Cloudflare resolution for $requestId at $url")
            val success = Scrapper.globalResolver?.resolve(url) ?: false
            AppLog.i("RequestViewModel", "Resolution result: $success")
            if (success) {
                AppLog.i("RequestViewModel", "Replaying request $requestId")
                requestRepository.replayRequest(requestId)
            }
        }
    }

    fun validateUrl(url: String): String? {
        val crawler = CrawlerFactory.getCrawlerByUrl(url)
        return if (crawler != null) {
            _error.value = null
            crawler.name
        } else {
            _error.value = "URL not supported or invalid"
            null
        }
    }

    fun setPreviewUrl(url: String) {
        _previewUrl.value = url
    }

    fun setPreviewNovel(novel: Novel?) {
        _novelPreview.value = novel
    }

    fun fetchNovelPreview(url: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val crawler = CrawlerFactory.getCrawlerByUrl(url)
                if (crawler != null) {
                    val novel = crawler.getNovelMetadata(url)
                    _novelPreview.value = novel
                } else {
                    _error.value = "URL not supported"
                }
            } catch (e: Exception) {
                _error.value = "Failed to fetch preview: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearPreview() {
        _novelPreview.value = null
        _previewUrl.value = null
        _error.value = null
        _similarNovels.value = emptyList()
    }

    fun addNovelDirectly(novel: Novel) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                when (val result = saveNovelUseCase.checkAndSave(novel)) {
                    is SaveNovelResult.SimilarFound -> {
                        _similarNovels.value = result.similarNovels
                    }
                    is SaveNovelResult.Saved -> {
                        _similarNovels.value = emptyList()
                        _addSuccess.emit(Unit)
                        _uiEvents.send(RequestUiEvent.NavigateBack)
                    }
                }
            } catch (e: Exception) {
                _error.value = "Failed to check similarity: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun saveNovel(novel: Novel) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                saveNovelUseCase.saveDirectly(novel)
                _similarNovels.value = emptyList()
                _addSuccess.emit(Unit)
                _uiEvents.send(RequestUiEvent.NavigateBack)
            } catch (e: Exception) {
                _error.value = "Failed to add to library: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearSimilarNovels() {
        _similarNovels.value = emptyList()
    }

    fun pushToRedis(url: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                indexRepository.index(url)
                onSuccess()
            } catch (e: Exception) {
                _error.value = "Server is down or under maintenance. Please try again later."
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun startNovelCrawl(crawlerName: String, url: String, title: String) {
        viewModelScope.launch {
            _isLoading.value = true
            startNovelCrawlUseCase(getApplication(), crawlerName, url, title)
            _isLoading.value = false
        }
    }

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

    fun deleteRequestRecord(id: String, requestId: Int) {
        viewModelScope.launch {
            requestRepository.requestDao.deleteById(id)
        }
    }
}
