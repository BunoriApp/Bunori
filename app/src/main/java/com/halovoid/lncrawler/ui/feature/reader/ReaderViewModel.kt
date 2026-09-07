package com.halovoid.lncrawler.ui.feature.reader

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.halovoid.lncrawler.data.repository.ChapterRepository
import com.halovoid.lncrawler.data.repository.NovelRepository
import com.halovoid.lncrawler.data.repository.ReaderRepository
import com.halovoid.lncrawler.domain.models.Chapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class LoadedChapter(
    val chapter: Chapter,
    val paragraph: List<String>
)

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

    private val contentCache = mutableMapOf<Int, List<String>>()

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

    fun start(novelUrl: String, initialChapterId: Int) {
        if (allChapters.isNotEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            crawlerName = novelRepository.getNovelDetails(novelUrl)?.crawlerName.orEmpty()
            allChapters = chapterRepository.getChaptersByNovelUrl(novelUrl).sortedBy { it.index }
            chapterIndexById = allChapters.withIndex().associate { (i, c) -> c.id to i }

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
                        if (chapter.id in previousIds) {
                            chapter.read = true
                            chapter
                        } else {
                            chapter
                        }
                    }
                }
            }
        }

        windowJob?.cancel()
        windowJob = viewModelScope.launch(Dispatchers.IO) {
            shiftWindow(pos)
        }
    }

    fun reloadChapter(chapterId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            contentCache.remove(chapterId)
            shiftWindow(centerPos)
        }
    }

    private suspend fun shiftWindow(centerPosition: Int) {
        windowMutex.withLock {
            if (centerPosition != centerPos) return@withLock
            val position = (centerPosition - 1..centerPosition + 1).filter { it in allChapters.indices }

            val loaded = position.map { pos ->
                kotlin.coroutines.coroutineContext.ensureActive()
                val chapter = allChapters[pos]
                val paragraphs = contentCache.getOrPut(chapter.id) {
                    readerRepository.getChapterContent(chapter, crawlerName)
                }
                LoadedChapter(chapter, paragraphs)
            }

            if (centerPosition != centerPos) return@withLock
            _window.value = loaded

            val keep = position.mapNotNull { allChapters.getOrNull(it)?.id }.toSet()
            contentCache.keys.retainAll { it in keep }
        }
    }
}
