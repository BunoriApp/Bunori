package com.halovoid.lncrawler.ui.feature.novel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.halovoid.lncrawler.data.db.entities.RequestEntity
import com.halovoid.lncrawler.data.db.entities.RequestStatus
import com.halovoid.lncrawler.data.db.entities.RequestType
import com.halovoid.lncrawler.data.repository.ArtifactRepository
import com.halovoid.lncrawler.data.repository.ChapterRepository
import com.halovoid.lncrawler.data.repository.NovelRepository
import com.halovoid.lncrawler.data.repository.VolumeRepository
import com.halovoid.lncrawler.data.scheduler.services.SchedulerService
import com.halovoid.lncrawler.data.repository.RequestRepository
import com.halovoid.lncrawler.data.repository.StorageRepositoryImpl
import com.halovoid.lncrawler.domain.models.Artifact
import com.halovoid.lncrawler.domain.models.Chapter
import com.halovoid.lncrawler.domain.models.Novel
import com.halovoid.lncrawler.domain.models.Request
import com.halovoid.lncrawler.ui.feature.novel.components.artifact.ExportFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONObject

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
    private val requestRepository: RequestRepository
) : AndroidViewModel(application) {
    private val novelRepository = NovelRepository.getInstance(application)
    private val volumeRepository = VolumeRepository.getInstance(application)

    private val artifactRepository = ArtifactRepository.getInstance(application)
    private val chapterRepository = ChapterRepository.getInstance(application)
    private val storageRepository = StorageRepositoryImpl.getInstance(application)

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

    private val _sortState = MutableStateFlow(ChapterSortState())
    val sortState: StateFlow<ChapterSortState> = _sortState.asStateFlow()

    init {
        viewModelScope.launch {
            novel.collect { currentNovel ->
                if (currentNovel != null) {
                    if (currentNovel.chapters.isNotEmpty()) {
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
    }

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
        _sortState
    ) { rawChapters, filter, sort ->
        val filtered = when (filter) {
            DownloadFilter.ALL -> rawChapters
            DownloadFilter.DOWNLOADED -> rawChapters.filter { it.fileLocation?.startsWith("content://") == true }
            DownloadFilter.NOT_DOWNLOADED -> rawChapters.filter { it.fileLocation?.startsWith("content://") != true }
        }

        when (sort.type) {
            SortType.CHAPTER_NUMBER -> {
                if (sort.order == SortOrder.ASCENDING) {
                    filtered.sortedWith(compareBy({ it.index }, { it.id }))
                } else {
                    filtered.sortedWith(compareByDescending<Chapter> { it.index }.thenByDescending { it.id })
                }
            }
            SortType.ALPHABETICAL -> {
                if (sort.order == SortOrder.ASCENDING) filtered.sortedBy { it.title }
                else filtered.sortedByDescending { it.title }
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

            val metadata = JSONObject().apply {
                put("format", format.toString())
                put("crawlerName", novel.crawlerName)
                put("startIndex", start)
                put("endIndex", end)
            }.toString()

            val request = RequestEntity(
                id = "${novel.url}_export_${format}_${start}_${end}_${System.nanoTime()}",
                type = RequestType.ARTIFACT,
                novelUrl = novel.url,
                name = "Export: ${novel.title} ($format) [$start-$end]",
                metadata = metadata,
                parentNovel = novel.url,
                status = RequestStatus.PENDING,
                rstatus = RequestStatus.PENDING,
                url = null,
                dependsOn = null,
                completedAt = null
            )

            requestRepository.insertRequests(listOf(request))
            SchedulerService.startService(getApplication())
        }
    }

    fun fetchNovelMetadata(novel: Novel) {
        viewModelScope.launch {
            val metadata = JSONObject().apply {
                put("crawlerName", novel.crawlerName)
            }.toString()

            val request = RequestEntity(
                id = "${novel.url}_metadata",
                type = RequestType.NOVEL_METADATA,
                novelUrl = novel.url,
                name = "Metadata: ${novel.title}",
                metadata = metadata,
                status = RequestStatus.PENDING,
                rstatus = RequestStatus.PENDING,
                dependsOn = null,
                url = novel.url,
                priority = 0,
                completedAt = null,
                parentNovel = novel.url
            )

            requestRepository.insertRequests(listOf(request))

            SchedulerService.startService(getApplication())
        }
    }

    fun fetchRange(novel: Novel) {
        viewModelScope.launch {
            val start = _chapterRange.value.start.toInt()
            val end = _chapterRange.value.endInclusive.toInt()
            
            val rangeChapters = novel.chapters.filter { it.index in start..end }
            
            val requestId = "${novel.url}_download_${start}_${end}"
            val metadata = JSONObject().apply {
                put("crawlerName", novel.crawlerName)
                put("startIndex", start)
                put("endIndex", end)
            }.toString()

            val request = RequestEntity(
                id = requestId,
                type = RequestType.RANGE_DOWNLOAD,
                novelUrl = novel.url,
                name = "Download: ${novel.title} ($start-$end)",
                metadata = metadata,
                parentNovel = novel.url,
                url = novel.url,
                status = RequestStatus.PENDING,
                rstatus = RequestStatus.PENDING,
                completedAt = null,
                progressTotal = rangeChapters.size
            )

            requestRepository.insertRequests(listOf(request))

            SchedulerService.startService(getApplication())
        }
    }

    fun downloadAllChapters(novel: Novel) {
        viewModelScope.launch {
            val allChapters = chapterRepository.getChaptersByNovelUrl(novel.url)
            if (allChapters.isEmpty()) return@launch
            
            val start = 1
            val end = allChapters.size
            
            val requestId = "${novel.url}_download_all"
            val metadata = JSONObject().apply {
                put("crawlerName", novel.crawlerName)
                put("startIndex", start)
                put("endIndex", end)
            }.toString()

            val request = RequestEntity(
                id = requestId,
                type = RequestType.RANGE_DOWNLOAD,
                novelUrl = novel.url,
                name = "Download All: ${novel.title}",
                metadata = metadata,
                parentNovel = novel.url,
                url = novel.url,
                status = RequestStatus.PENDING,
                rstatus = RequestStatus.PENDING,
                completedAt = null,
                progressTotal = allChapters.size
            )

            requestRepository.insertRequests(listOf(request))
            SchedulerService.startService(getApplication())
        }
    }

    fun downloadVolume(novel: Novel, volumeId: String, volumeIndex: Int) {
        viewModelScope.launch {
            val allChapters = chapterRepository.getChaptersByNovelUrl(novel.url)
            val volumeChapters = allChapters.filter { it.volumeId == volumeId }
            if (volumeChapters.isEmpty()) return@launch
            
            val start = volumeChapters.minOf { it.index }
            val end = volumeChapters.maxOf { it.index }
            
            val requestId = "${novel.url}_download_vol_${volumeIndex}"
            val metadata = JSONObject().apply {
                put("crawlerName", novel.crawlerName)
                put("startIndex", start)
                put("endIndex", end)
            }.toString()

            val request = RequestEntity(
                id = requestId,
                type = RequestType.RANGE_DOWNLOAD,
                novelUrl = novel.url,
                name = "Download: ${novel.title} Vol $volumeIndex",
                metadata = metadata,
                parentNovel = novel.url,
                url = novel.url,
                status = RequestStatus.PENDING,
                rstatus = RequestStatus.PENDING,
                completedAt = null,
                progressTotal = volumeChapters.size
            )

            requestRepository.insertRequests(listOf(request))
            SchedulerService.startService(getApplication())
        }
    }

    fun fetchChapter(novel: Novel, chapter: Chapter) {
        viewModelScope.launch {
            val chapterMetadata = JSONObject().apply {
                put("chapterId", chapter.id)
                put("crawlerName", novel.crawlerName)
            }.toString()

            val request = RequestEntity(
                id = "${novel.url}_chapter_${chapter.index}",
                type = RequestType.CHAPTER,
                parentNovel = novel.url,
                dependsOn = null,
                priority = 10,
                name = "Chapter: ${chapter.title}",
                status = RequestStatus.PENDING,
                rstatus = RequestStatus.PENDING,
                completedAt = null,
                metadata = chapterMetadata,
                url = chapter.url,
                novelUrl = novel.url,
                progressTotal = 1,
                progressSuccess = 0,
            )

            requestRepository.insertRequests(listOf(request))
            SchedulerService.startService(getApplication())
        }
    }

    fun deleteChapter(chapter: Chapter) {
        viewModelScope.launch(Dispatchers.IO) {
            chapter.fileLocation?.let { location ->
                try {
                    storageRepository.delete(Uri.parse(location))
                } catch (e: Exception) {}
            }
            chapterRepository.updateChapter(chapter.copy(fileLocation = null))
        }
    }

    fun replayChapter(novel: Novel, chapter: Chapter) {
        viewModelScope.launch(Dispatchers.IO) {
            chapter.fileLocation?.let { location ->
                try {
                    storageRepository.delete(Uri.parse(location))
                } catch (e: Exception) {}
            }
            chapterRepository.updateChapter(chapter.copy(fileLocation = null))
            fetchChapter(novel, chapter)
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
