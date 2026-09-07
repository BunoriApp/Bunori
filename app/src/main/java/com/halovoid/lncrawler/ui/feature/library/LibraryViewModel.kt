package com.halovoid.lncrawler.ui.feature.library

import android.app.Application
import com.halovoid.lncrawler.domain.models.Novel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.halovoid.lncrawler.data.repository.NovelRepository

class LibraryViewModel(
    application: Application,
    private val novelRepository: NovelRepository
) : AndroidViewModel(application) {
    val novels: StateFlow<List<Novel>> = novelRepository.getAllNovels()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
}
