package com.halovoid.bunori.ui.feature.request

/**
 * One-shot UI events for navigation and user feedback in the Batch feature.
 */
sealed interface RequestUiEvent {
    data object NavigateBack : RequestUiEvent
    data class NavigateToDetail(val crawlerName: String, val novelUrl: String) : RequestUiEvent
    data class ShowToast(val message: String) : RequestUiEvent
}
