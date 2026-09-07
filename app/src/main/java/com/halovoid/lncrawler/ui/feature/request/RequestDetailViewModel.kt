package com.halovoid.lncrawler.ui.feature.request

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.halovoid.lncrawler.data.db.entities.RequestStatus
import com.halovoid.lncrawler.data.repository.ArtifactRepository
import com.halovoid.lncrawler.data.repository.ChapterRepository
import com.halovoid.lncrawler.data.repository.RequestRepository
import com.halovoid.lncrawler.domain.models.Artifact
import com.halovoid.lncrawler.domain.models.Chapter
import com.halovoid.lncrawler.domain.models.Request
import com.halovoid.lncrawler.ui.core.logging.AppLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class RequestDetailViewModel(
    application: Application,
    private val requestRepository: RequestRepository
) : AndroidViewModel(application) {
    private val chapterRepository = ChapterRepository.getInstance(application)
    private val artifactRepository = ArtifactRepository.getInstance(application)

    private val _requestId = MutableStateFlow<String?>(null)
    fun setRequestId(id: String) {
        _requestId.value = id
    }

    private val _statusFilter = MutableStateFlow<RequestStatus?>(null)
    val statusFilter: StateFlow<RequestStatus?> = _statusFilter.asStateFlow()

    fun setStatusFilter(status: RequestStatus?) {
        _statusFilter.value = status
    }

    val cancellingRequestIds: StateFlow<Set<String>> = requestRepository.cancellingRequestIds
    val activeActionIds: StateFlow<Set<String>> = requestRepository.activeActionIds

    fun resolveCloudflare(requestId: String, url: String) {
        viewModelScope.launch {
            AppLog.i("RequestDetailViewModel", "Starting Cloudflare resolution for $requestId at $url")
            val success = com.halovoid.lncrawler.api.core.scrapper.Scrapper.globalResolver?.resolve(url) ?: false
            AppLog.i("RequestDetailViewModel", "Resolution result: $success")
            if (success) {
                AppLog.i("RequestDetailViewModel", "Replaying request $requestId")
                requestRepository.replayRequest(requestId)
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val linkedRequests: StateFlow<List<Request>> = combine(_requestId.filterNotNull(), _statusFilter) { id, status ->
        id to status
    }
        .flatMapLatest { (id, status) ->
            requestRepository.getRequestsByDependenceFlow(id).map { requests ->
                if (status == null) requests else requests.filter { it.status == status }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val chapterMetadata: StateFlow<Chapter?> = _requestId
        .filterNotNull()
        .flatMapLatest { id ->
            requestRepository.requestDao.getRequestByIdFlow(id)
        }
        .filterNotNull()
        .map { request ->
            val chapters = chapterRepository.getChaptersByNovelUrl(request.novelUrl)
            chapters.find { it.url == request.url }
        }
        .flowOn(Dispatchers.IO)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val artifactMetadata: StateFlow<Artifact?> = _requestId
        .filterNotNull()
        .flatMapLatest { id ->
            requestRepository.requestDao.getRequestByIdFlow(id)
        }
        .filterNotNull()
        .map { request ->
            val artifacts = artifactRepository.getArtifactForRequest(request.id)
            artifacts.find { it.requestId == request.id }
        }
        .flowOn(Dispatchers.IO)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    fun getRequest(requestId: String): Flow<Request?> {
        return requestRepository.getRequestByIdFlow(requestId)
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

    fun copyArtifactToUri(artifact: Artifact, destinationUri: Uri, onComplete: (Uri?) -> Unit, onFileMissing: () -> Unit) {
        viewModelScope.launch {
            if (!artifactRepository.artifactExists(artifact)) {
                artifactRepository.removeArtifact(artifact)
                onFileMissing()
                return@launch
            }
            val result = artifactRepository.copyArtifactToUri(artifact, destinationUri)
            onComplete(result)
        }
    }
}
