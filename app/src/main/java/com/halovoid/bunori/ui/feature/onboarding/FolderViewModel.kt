package com.halovoid.bunori.ui.feature.onboarding

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.halovoid.bunori.data.repository.PreferenceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class FolderViewModel(
    application: Application,
    private val preferenceRepository: PreferenceRepository
) : AndroidViewModel(application) {

    val exportFolderUri =
        preferenceRepository.exportFolderUri

    val friendlyPath: Flow<String> = exportFolderUri.map { uri ->
        UriUtils.getFriendlyPath(getApplication(), uri)
    }

    fun setExportFolder(uri: Uri) {
        viewModelScope.launch {
            preferenceRepository.setExportFolder(uri)
        }
    }
}
