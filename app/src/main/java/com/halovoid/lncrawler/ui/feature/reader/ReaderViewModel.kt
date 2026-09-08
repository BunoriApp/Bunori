package com.halovoid.lncrawler.ui.feature.reader

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.halovoid.lncrawler.data.parser.HtmlDocumentParser
import com.halovoid.lncrawler.data.repository.ChapterRepository
import com.halovoid.lncrawler.data.repository.NovelRepository
import com.halovoid.lncrawler.data.repository.ReaderRepository
import com.halovoid.lncrawler.domain.models.Block
import com.halovoid.lncrawler.domain.models.Chapter
import com.halovoid.lncrawler.domain.models.ReaderDocument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * A single chapter loaded into the reading window. Holds a structured
 * ReaderDocument instead of a flattened List<String>, so headings,
 * emphasis, lists, images, etc. survive all the way to the renderer.
 */
data class LoadedChapter(
    val chapter: Chapter,
    val document: ReaderDocument
)

/**
 * One-shot navigation request consumed by the UI (e.g. after a Table of
 * Contents tap). Kept separate from `window` so it isn't replayed on
 * recomposition. `token` lets the UI ignore stale requests.
 */
data class ScrollRequest(val chapterId: Int, val token: Long)

class ReaderViewModel(
    application: Application,
    private val chapterRepository: ChapterRepository,
    private val novelRepository: NovelRepository,
    private val readerRepository: ReaderRepository
) : AndroidViewModel(application) {

    private var allChapters: List<Chapter> = emptyList()
    private var chapterIndexById: Map<Int, Int> = emptyMap()
    private var crawlerName: String = ""
    private var centerPos: Int = -1
    private val windowMutex = Mutex()
    private var windowJob: Job? = null
    private var scrollRequestSeq = 0L

    private val htmlParser = HtmlDocumentParser()
    private val contentCache = mutableMapOf<Int, ReaderDocument>()

    private val _window = MutableStateFlow<List<LoadedChapter>>(emptyList())
    val window: StateFlow<List<LoadedChapter>> = _window.asStateFlow()

    private val _currentChapter = MutableStateFlow<Chapter?>(null)
    val currentChapter: StateFlow<Chapter?> = _currentChapter.asStateFlow()

    private val _currentChapterNumber = MutableStateFlow(0)
    val currentChapterNumber: StateFlow<Int> = _currentChapterNumber.asStateFlow()

    private val _totalChapters = MutableStateFlow(0)
    val totalChapters: StateFlow<Int> = _totalChapters.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // --- Table of Contents ---
    private val _tocChapters = MutableStateFlow<List<Chapter>>(emptyList())
    val tocChapters: StateFlow<List<Chapter>> = _tocChapters.asStateFlow()

    private val _scrollRequest = MutableStateFlow<ScrollRequest?>(null)
    val scrollRequest: StateFlow<ScrollRequest?> = _scrollRequest.asStateFlow()

    // --- Block selection: foundation for bookmarking/highlighting/notes later ---
    private val _selectedBlockIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedBlockIds: StateFlow<Set<String>> = _selectedBlockIds.asStateFlow()

    fun start(novelUrl: String, initialChapterId: Int) {
        if (allChapters.isNotEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            crawlerName = novelRepository.getNovelDetails(novelUrl)?.crawlerName.orEmpty()
            allChapters = chapterRepository.getChaptersByNovelUrl(novelUrl).sortedBy { it.index }
            chapterIndexById = allChapters.withIndex().associate { (i, c) -> c.id to i }
            _tocChapters.value = allChapters

            val startPos = chapterIndexById[initialChapterId] ?: 0
            centerPos = startPos
            _currentChapter.value = allChapters.getOrNull(startPos)
            _currentChapterNumber.value = startPos + 1
            _totalChapters.value = allChapters.size
            shiftWindow(startPos)
            _isLoading.value = false
        }
    }

    fun onCenterChapterChanged(chapterId: Int) {
        val pos = chapterIndexById[chapterId] ?: return
        if (pos == centerPos) return
        centerPos = pos
        _currentChapter.value = allChapters.getOrNull(pos)
        _currentChapterNumber.value = pos + 1

        if (pos > 0) {
            viewModelScope.launch(Dispatchers.IO) {
                val previousChapters = allChapters.subList(0, pos).filter { !it.read }
                if (previousChapters.isNotEmpty()) {
                    val previousIds = previousChapters.map { it.id }
                    chapterRepository.updateChaptersReadStatus(previousIds, true)
                    allChapters = allChapters.map { chapter ->
                        if (chapter.id in previousIds) chapter.apply { read = true } else chapter
                    }
                    _tocChapters.value = allChapters
                }
            }
        }

        windowJob?.cancel()
        windowJob = viewModelScope.launch(Dispatchers.IO) {
            shiftWindow(pos)
        }
    }

    /** Called when the user taps a chapter in the Table of Contents. */
    fun jumpToChapter(chapterId: Int) {
        val pos = chapterIndexById[chapterId] ?: return
        clearSelection()
        centerPos = pos
        _currentChapter.value = allChapters.getOrNull(pos)
        _currentChapterNumber.value = pos + 1
        scrollRequestSeq += 1
        _scrollRequest.value = ScrollRequest(chapterId, scrollRequestSeq)

        windowJob?.cancel()
        windowJob = viewModelScope.launch(Dispatchers.IO) {
            shiftWindow(pos)
        }
    }

    /** UI calls this once it has acted on a scroll request, so it isn't replayed. */
    fun consumeScrollRequest(token: Long) {
        if (_scrollRequest.value?.token == token) {
            _scrollRequest.value = null
        }
    }

    fun reloadChapter(chapterId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            contentCache.remove(chapterId)
            shiftWindow(centerPos)
        }
    }

    // --- Selection ---

    fun toggleBlockSelection(blockId: String) {
        _selectedBlockIds.value = _selectedBlockIds.value.toMutableSet().apply {
            if (!add(blockId)) remove(blockId)
        }
    }

    fun clearSelection() {
        if (_selectedBlockIds.value.isNotEmpty()) _selectedBlockIds.value = emptySet()
    }

    private suspend fun shiftWindow(centerPosition: Int) {
        windowMutex.withLock {
            if (centerPosition != centerPos) return@withLock
            val positions = (centerPosition - 1..centerPosition + 1).filter { it in allChapters.indices }

            val loaded = positions.map { pos ->
                kotlin.coroutines.coroutineContext.ensureActive()
                val chapter = allChapters[pos]
                val document = contentCache.getOrPut(chapter.id) { loadDocument(chapter) }
                LoadedChapter(chapter, document)
            }

            if (centerPosition != centerPos) return@withLock
            _window.value = loaded

            val keep = positions.mapNotNull { allChapters.getOrNull(it)?.id }.toSet()
            contentCache.keys.retainAll { it in keep }
        }
    }

    private suspend fun loadDocument(chapter: Chapter): ReaderDocument {
        return try {
            // NOTE: ReaderRepository.getChapterContent is expected to return the raw
            // chapter HTML (String) rather than a pre-split List<String>. Update the
            // repository/data-source signature to match before wiring this in.
            val html = readerRepository.getChapterContent(chapter, crawlerName)
            htmlParser.parse(html, chapter.id)
        } catch (e: Exception) {
            ReaderDocument(
                listOf(Block.ErrorPlaceholder(id = "c${chapter.id}-err", message = e.message ?: "Couldn't load chapter"))
            )
        }
    }
}