package com.halovoid.lncrawler.ui.feature.novel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.halovoid.lncrawler.data.factory.RequestFactory
import com.halovoid.lncrawler.data.db.entities.RequestStatus
import com.halovoid.lncrawler.data.repository.ArtifactRepository
import com.halovoid.lncrawler.data.repository.ChapterRepository
import com.halovoid.lncrawler.data.repository.NovelRepository
import com.halovoid.lncrawler.data.repository.PreferenceRepository
import com.halovoid.lncrawler.data.repository.VolumeRepository
import com.halovoid.lncrawler.data.scheduler.services.SchedulerService
import com.halovoid.lncrawler.data.repository.RequestRepository
import com.halovoid.lncrawler.data.repository.StorageRepositoryImpl
import com.halovoid.lncrawler.domain.models.Artifact
import com.halovoid.lncrawler.domain.models.Chapter
import com.halovoid.lncrawler.domain.models.Novel
import com.halovoid.lncrawler.domain.models.Request
import com.halovoid.lncrawler.domain.usecase.DeleteChapterUseCase
import com.halovoid.lncrawler.domain.usecase.ReplayChapterUseCase
import com.halovoid.lncrawler.ui.core.theme.PrimaryText
import com.halovoid.lncrawler.ui.feature.novel.components.artifact.ExportFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class DownloadFilter {
    ALL, DOWNLOADED, NOT_DOWNLOADED
}

enum class SortType {
    CHAPTER_NUMBER, ALPHABETICAL
}

enum class SortOrder {
    ASCENDING, DESCENDING
}

data class ChapterSortState(
    val type: SortType = SortType.CHAPTER_NUMBER,
    val order: SortOrder = SortOrder.ASCENDING
)

class NovelDetailViewModel(
    application: Application,
    private val requestRepository: RequestRepository,
    private val requestFactory: RequestFactory = RequestFactory(),
    private val deleteChapterUseCase: DeleteChapterUseCase = DeleteChapterUseCase(
        ChapterRepository.getInstance(application),
        StorageRepositoryImpl.getInstance(application)
    ),
    private val replayChapterUseCase: ReplayChapterUseCase = ReplayChapterUseCase(
        ChapterRepository.getInstance(application),
        StorageRepositoryImpl.getInstance(application),
        requestRepository,
        requestFactory
    ),
    private val preferenceRepository: PreferenceRepository = PreferenceRepository.getInstance(application)
) : AndroidViewModel(application) {
    private val novelRepository = NovelRepository.getInstance(application)
    private val volumeRepository = VolumeRepository.getInstance(application)

    private val artifactRepository = ArtifactRepository.getInstance(application)
    private val chapterRepository = ChapterRepository.getInstance(application)

    private val _novelUrl = MutableStateFlow<String?>(null)
    private val _requestedUrls = mutableSetOf<String>()

    @OptIn(ExperimentalCoroutinesApi::class)
    val novel: StateFlow<Novel?> = _novelUrl
        .filterNotNull()
        .flatMapLatest { url ->
            combine(
                novelRepository.getNovelByUrlFlow(url),
                volumeRepository.getVolumeByNovelUrlFlow(url),
                chapterRepository.getChaptersFlow(url)
            ) { details, volumes, chapters ->
                details?.copy(
                    volumes = volumes,
                    chapters = chapters
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    private val _chapterRange = MutableStateFlow<ClosedFloatingPointRange<Float>>(1f..1f)
    val chapterRange: StateFlow<ClosedFloatingPointRange<Float>> = _chapterRange.asStateFlow()

    val cancellingRequestIds: StateFlow<Set<String>> = requestRepository.cancellingRequestIds
    val activeActionIds: StateFlow<Set<String>> = requestRepository.activeActionIds

    private val _downloadFilter = MutableStateFlow(DownloadFilter.ALL)
    val downloadFilter: StateFlow<DownloadFilter> = _downloadFilter.asStateFlow()

    private val _selectedSources = MutableStateFlow<Set<String>>(emptySet())
    val selectedSources: StateFlow<Set<String>> = _selectedSources.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val availableSources: StateFlow<List<String>> = novel
        .filterNotNull()
        .map { nov -> nov.chapters.map { it.scanlationSource }.distinct() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _sortState = MutableStateFlow(ChapterSortState())
    val sortState: StateFlow<ChapterSortState> = _sortState.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val rootRequests: StateFlow<List<Request>> = novel
        .filterNotNull()
        .flatMapLatest { nov ->
            requestRepository.getRootRequestByNovelFlow(nov.url)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val chapters: StateFlow<List<Chapter>> = combine(
        novel.filterNotNull().flatMapLatest { nov ->
            chapterRepository.getChaptersFlow(nov.url)
        },
        _downloadFilter,
        _selectedSources,
        _sortState
    ) { rawChapters, filter, selectedSources, sort ->
        val filteredByDownload = when (filter) {
            DownloadFilter.ALL -> rawChapters
            DownloadFilter.DOWNLOADED -> rawChapters.filter { it.fileLocation?.startsWith("content://") == true }
            DownloadFilter.NOT_DOWNLOADED -> rawChapters.filter { it.fileLocation?.startsWith("content://") != true }
        }

        val filteredBySource = if (selectedSources.isEmpty()) {
            filteredByDownload
        } else {
            filteredByDownload.filter { selectedSources.contains(it.scanlationSource) }
        }

        when (sort.type) {
            SortType.CHAPTER_NUMBER -> {
                if (sort.order == SortOrder.ASCENDING) {
                    filteredBySource.sortedWith(compareBy({ it.index }, { it.id }))
                } else {
                    filteredBySource.sortedWith(compareByDescending<Chapter> { it.index }.thenByDescending { it.id })
                }
            }
            SortType.ALPHABETICAL -> {
                if (sort.order == SortOrder.ASCENDING) filteredBySource.sortedBy { it.title }
                else filteredBySource.sortedByDescending { it.title }
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val artifacts: StateFlow<List<Artifact>> = novel
        .filterNotNull()
        .flatMapLatest { nov ->
            artifactRepository.getArtifactsByNovelFlow(nov.url)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        viewModelScope.launch {
            val defaultFilterStr = preferenceRepository.defaultChapterDownloadFilter.firstOrNull()
            if (defaultFilterStr != null) {
                _downloadFilter.value = runCatching { DownloadFilter.valueOf(defaultFilterStr) }.getOrDefault(DownloadFilter.ALL)
            }

            val defaultSortTypeStr = preferenceRepository.defaultChapterSortType.firstOrNull()
            val defaultSortOrderStr = preferenceRepository.defaultChapterSortOrder.firstOrNull()
            if (defaultSortTypeStr != null || defaultSortOrderStr != null) {
                val sortType = runCatching { SortType.valueOf(defaultSortTypeStr ?: "") }.getOrDefault(SortType.CHAPTER_NUMBER)
                val sortOrder = runCatching { SortOrder.valueOf(defaultSortOrderStr ?: "") }.getOrDefault(SortOrder.ASCENDING)
                _sortState.value = ChapterSortState(type = sortType, order = sortOrder)
            }
        }

        viewModelScope.launch {
            novel.collect { currentNovel ->
                if (currentNovel != null) {
                    if (currentNovel.chapters.isNotEmpty()) {
                        val sources = currentNovel.chapters.map { it.scanlationSource }.distinct()
                        if (_selectedSources.value.isEmpty()) {
                            _selectedSources.value = sources.toSet()
                        }
                        val currentRange = _chapterRange.value
                        if (currentRange.start == 1f && currentRange.endInclusive == 1f) {
                            _chapterRange.value = 1f..currentNovel.chapters.size.toFloat()
                        }
                    } else {
                        val url = currentNovel.url
                        if (!_requestedUrls.contains(url)) {
                            val hasMetadataRequest = requestRepository.requestDao.getRequestById("${url}_metadata")?.let {
                                it.rstatus == RequestStatus.PENDING || it.rstatus == RequestStatus.RUNNING
                            } ?: false
                            
                            if (!hasMetadataRequest) {
                                _requestedUrls.add(url)
                                fetchNovelMetadata(currentNovel)
                            }
                        }
                    }
                }
            }
        }

        viewModelScope.launch {
            chapters.collect { filteredChapters ->
                if (filteredChapters.isNotEmpty()) {
                    val minIndex = filteredChapters.minOf { it.index }.toFloat()
                    val maxIndex = filteredChapters.maxOf { it.index }.toFloat()
                    val currentRange = _chapterRange.value
                    if (currentRange.start < minIndex || currentRange.endInclusive > maxIndex || (currentRange.start == 1f && currentRange.endInclusive == 1f)) {
                        _chapterRange.value = minIndex..maxIndex
                    }
                }
            }
        }
    }

    fun loadNovel(novelUrl: String) {
        clearSelection()
        _novelUrl.value = novelUrl
    }

    private val _isSelectionMode = MutableStateFlow(false)
    val isSelectionMode: StateFlow<Boolean> = _isSelectionMode.asStateFlow()

    private val _selectedChapterIds = MutableStateFlow<Set<Int>>(emptySet())
    val selectedChapterIds: StateFlow<Set<Int>> = _selectedChapterIds.asStateFlow()

    fun toggleChapterSelection(chapterId: Int) {
        val current = _selectedChapterIds.value
        val updated = if (current.contains(chapterId)) current - chapterId else current + chapterId
        _selectedChapterIds.value = updated
        _isSelectionMode.value = updated.isNotEmpty()
    }

    fun selectChapter(chapterId: Int) {
        _isSelectionMode.value = true
        _selectedChapterIds.value = _selectedChapterIds.value + chapterId
    }

    fun clearSelection() {
        _selectedChapterIds.value = emptySet()
        _isSelectionMode.value = false
    }

    fun selectAllChapters(chapters: List<Chapter>) {
        _selectedChapterIds.value = chapters.map { it.id }.toSet()
        _isSelectionMode.value = true
    }

    fun toggleSourceSelection(source: String) {
        val current = _selectedSources.value
        _selectedSources.value = if (current.contains(source)) {
            if (current.size > 1) current - source else current
        } else {
            current + source
        }
    }

    fun selectAllSources(sources: List<String>) {
        _selectedSources.value = sources.toSet()
    }
    fun markSelectedChaptersRead(isRead: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val ids = _selectedChapterIds.value.toList()
            if (ids.isNotEmpty()) {
                chapterRepository.updateChaptersReadStatus(ids, isRead)
            }
            clearSelection()
        }
    }

    fun updateChapterRange(range: ClosedFloatingPointRange<Float>) {
        _chapterRange.value = range
    }

    fun startBackgroundExport(novel: Novel, format: ExportFormat) {
        viewModelScope.launch {
            val start = _chapterRange.value.start.toInt()
            val end = _chapterRange.value.endInclusive.toInt()
            val request = requestFactory.export(novel, format, start, end)

            requestRepository.insertRequests(listOf(request))
            SchedulerService.startService(getApplication())
        }
    }

    fun fetchNovelMetadata(novel: Novel) {
        viewModelScope.launch {
            val request = requestFactory.metadata(novel)

            requestRepository.insertRequests(listOf(request))
            SchedulerService.startService(getApplication())
        }
    }

    fun fetchRange(novel: Novel) {
        viewModelScope.launch {
            val start = _chapterRange.value.start.toInt()
            val end = _chapterRange.value.endInclusive.toInt()
            val rangeChapters = chapters.value.filter { it.index in start..end }
            val request = requestFactory.rangeDownload(novel, start, end, rangeChapters.size)

            requestRepository.insertRequests(listOf(request))
            SchedulerService.startService(getApplication())
        }
    }

    fun fetchChapter(novel: Novel, chapter: Chapter) {
        viewModelScope.launch {
            val request = requestFactory.chapter(novel, chapter)

            requestRepository.insertRequests(listOf(request))
            SchedulerService.startService(getApplication())
        }
    }

    fun deleteChapter(chapter: Chapter) {
        viewModelScope.launch(Dispatchers.IO) {
            deleteChapterUseCase(chapter)
        }
    }

    fun replayChapter(novel: Novel, chapter: Chapter) {
        viewModelScope.launch(Dispatchers.IO) {
            replayChapterUseCase(novel, chapter)
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

    fun deleteNovelPermanently(novel: Novel) {
        viewModelScope.launch {
            novelRepository.deleteNovel(novel)
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

    fun cancelRequest(requestId: String) {
        viewModelScope.launch {
            requestRepository.cancelRequest(requestId)
        }
    }

    fun toggleAlphabeticalSort() {
        val current = _sortState.value
        _sortState.value = if (current.type != SortType.ALPHABETICAL) {
            ChapterSortState(SortType.ALPHABETICAL, SortOrder.ASCENDING)
        } else {
            when (current.order) {
                SortOrder.ASCENDING -> ChapterSortState(SortType.ALPHABETICAL, SortOrder.DESCENDING)
                SortOrder.DESCENDING -> ChapterSortState(SortType.CHAPTER_NUMBER, SortOrder.ASCENDING)
            }
        }
    }

    fun toggleChapterNumberSort() {
        val current = _sortState.value
        _sortState.value = if (current.type != SortType.CHAPTER_NUMBER) {
            ChapterSortState(SortType.CHAPTER_NUMBER, SortOrder.ASCENDING)
        } else {
            when (current.order) {
                SortOrder.ASCENDING -> ChapterSortState(SortType.CHAPTER_NUMBER, SortOrder.DESCENDING)
                SortOrder.DESCENDING -> ChapterSortState(SortType.CHAPTER_NUMBER, SortOrder.ASCENDING)
            }
        }
    }

    fun setDownloadFilter(filter: DownloadFilter) {
        _downloadFilter.value = filter
    }
}
