package com.halovoid.bunori.ui.feature.library

import android.app.Application
import com.halovoid.bunori.domain.models.Novel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.halovoid.bunori.data.repository.NovelRepository
import com.halovoid.bunori.data.repository.PreferenceRepository
import kotlinx.coroutines.launch

class LibraryViewModel(
    application: Application,
    private val novelRepository: NovelRepository = NovelRepository.getInstance(application),
    private val preferenceRepository: PreferenceRepository = PreferenceRepository.getInstance(application)
) : AndroidViewModel(application) {
    val novels: StateFlow<List<Novel>> = novelRepository.getAllNovels()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val libraryCompactView: StateFlow<Boolean> = preferenceRepository.libraryCompactView
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    fun setLibraryCompactView(compact: Boolean) {
        viewModelScope.launch {
            preferenceRepository.setLibraryCompactView(compact)
        }
    }
}
