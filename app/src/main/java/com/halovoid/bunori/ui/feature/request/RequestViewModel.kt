package com.halovoid.bunori.ui.feature.request

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.halovoid.bunori.api.core.crawler.CrawlerFactory
import com.halovoid.bunori.api.core.scrapper.Scrapper
import com.halovoid.bunori.data.factory.RequestFactory
import com.halovoid.bunori.data.repository.NovelRepository
import com.halovoid.bunori.data.repository.BatchRepository
import com.halovoid.bunori.domain.models.Chapter
import com.halovoid.bunori.domain.models.Novel
import com.halovoid.bunori.domain.usecase.SaveNovelResult
import com.halovoid.bunori.domain.usecase.SaveNovelUseCase
import com.halovoid.bunori.domain.usecase.StartNovelCrawlUseCase
import com.halovoid.bunori.ui.core.logging.AppLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class RequestViewModel(
    application: Application,
    private val batchRepository: BatchRepository,
    private val novelRepository: NovelRepository = NovelRepository.getInstance(application),
    private val requestFactory: RequestFactory = RequestFactory(),
    private val saveNovelUseCase: SaveNovelUseCase = SaveNovelUseCase(novelRepository),
    private val startNovelCrawlUseCase: StartNovelCrawlUseCase = StartNovelCrawlUseCase(batchRepository, requestFactory)
) : AndroidViewModel(application) {

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _novelPreview = MutableStateFlow<Novel?>(null)
    val novelPreview: StateFlow<Novel?> = _novelPreview.asStateFlow()

    private val _isChaptersLoading = MutableStateFlow(false)
    val isChaptersLoading: StateFlow<Boolean> = _isChaptersLoading.asStateFlow()

    private val _readingChapter = MutableStateFlow<Chapter?>(null)
    val readingChapter: StateFlow<Chapter?> = _readingChapter.asStateFlow()

    private val _chapterContent = MutableStateFlow<String?>(null)
    val chapterContent: StateFlow<String?> = _chapterContent.asStateFlow()

    private val _isChapterContentLoading = MutableStateFlow(false)
    val isChapterContentLoading: StateFlow<Boolean> = _isChapterContentLoading.asStateFlow()

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

    val cancellingRequestIds: StateFlow<Set<String>> = batchRepository.cancellingRequestIds
    val activeActionIds: StateFlow<Set<String>> = batchRepository.activeActionIds

    fun resolveWebView(requestId: String, url: String) {
        viewModelScope.launch {
            AppLog.i("RequestViewModel", "Starting WebView resolution for $requestId at $url")
            val success = Scrapper.globalResolver?.resolve(url) ?: false
            AppLog.i("RequestViewModel", "Resolution result: $success")
            if (success) {
                AppLog.i("RequestViewModel", "Resuming request $requestId")
                batchRepository.resumeRequest(requestId)
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
            val start = System.currentTimeMillis()
            AppLog.i("RequestViewModel", "[TIMING] fetchNovelPreview started for: $url")
            try {
                val crawler = CrawlerFactory.getCrawlerByUrl(url)
                if (crawler != null) {
                    val novel = crawler.getNovelDetails(url)
                    val duration = System.currentTimeMillis() - start
                    AppLog.i("RequestViewModel", "[TIMING] fetchNovelPreview finished in ${duration}ms, loaded ${novel.chapters.size} chapters")
                    _novelPreview.value = novel
                } else {
                    _error.value = "URL not supported"
                }
            } catch (e: Exception) {
                AppLog.e("RequestViewModel", "[TIMING] fetchNovelPreview failed in ${System.currentTimeMillis() - start}ms: ${e.message}")
                _error.value = "Failed to fetch preview: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun openChapter(chapter: Chapter, crawlerName: String) {
        _readingChapter.value = chapter
        _isChapterContentLoading.value = true
        _chapterContent.value = null
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val crawler = CrawlerFactory.getCrawler(crawlerName)
                val url = chapter.sourceUrl?.takeIf { it.isNotBlank() } ?: chapter.url
                val content = crawler?.getChapterContent(url)
                _chapterContent.value = content ?: "<p>Couldn't load this chapter. Check your connection and try again.</p>"
            } catch (e: Exception) {
                _chapterContent.value = "<p>Failed to load chapter: ${e.message}</p>"
            } finally {
                _isChapterContentLoading.value = false
            }
        }
    }

    fun closeChapter() {
        _readingChapter.value = null
        _chapterContent.value = null
        _isChapterContentLoading.value = false
    }

    fun clearPreview() {
        _novelPreview.value = null
        _previewUrl.value = null
        _error.value = null
        _similarNovels.value = emptyList()
        _isChaptersLoading.value = false
        closeChapter()
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
                        _uiEvents.send(RequestUiEvent.NavigateToDetail(novel.crawlerName, novel.url))
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
                _uiEvents.send(RequestUiEvent.NavigateToDetail(novel.crawlerName, novel.url))
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

    fun startNovelCrawl(crawlerName: String, url: String, title: String) {
        viewModelScope.launch {
            _isLoading.value = true
            startNovelCrawlUseCase(getApplication(), crawlerName, url, title)
            _isLoading.value = false
        }
    }

    fun cancelRequest(requestId: String) {
        viewModelScope.launch {
            batchRepository.cancelRequest(requestId)
        }
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

    fun deleteRequestRecord(id: String, requestId: Int) {
        viewModelScope.launch {
            batchRepository.deleteRequest(id)
        }
    }
}
