package com.halovoid.bunori.ui.feature.request

import com.halovoid.bunori.domain.models.Novel

/**
 * UI State model for novel preview and request submission operations.
 */
sealed interface PreviewUiState {
    data object Idle : PreviewUiState
    data object Loading : PreviewUiState
    data class Success(
        val novel: Novel,
        val similarNovels: List<Novel> = emptyList()
    ) : PreviewUiState
    data class Error(val message: String) : PreviewUiState
}

/**
 * One-shot UI events for navigation and user feedback in the Request feature.
 */
sealed interface RequestUiEvent {
    data object NavigateBack : RequestUiEvent
    data class NavigateToDetail(val crawlerName: String, val novelUrl: String) : RequestUiEvent
    data object NavigateToPreview : RequestUiEvent
    data class ShowToast(val message: String) : RequestUiEvent
}
