package com.halovoid.lncrawler.ui

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.halovoid.lncrawler.data.repository.ChapterRepository
import com.halovoid.lncrawler.data.repository.NovelRepository
import com.halovoid.lncrawler.data.repository.PreferenceRepository
import com.halovoid.lncrawler.data.repository.ReaderRepository
import com.halovoid.lncrawler.data.repository.RequestRepository
import com.halovoid.lncrawler.ui.feature.crawler.CrawlerViewModel
import com.halovoid.lncrawler.ui.feature.downloads.DownloadViewModel
import com.halovoid.lncrawler.ui.feature.library.LibraryViewModel
import com.halovoid.lncrawler.ui.feature.novel.GroupedRequestsViewModel
import com.halovoid.lncrawler.ui.feature.novel.NovelDetailViewModel
import com.halovoid.lncrawler.ui.feature.onboarding.FolderViewModel
import com.halovoid.lncrawler.ui.feature.reader.ReaderViewModel
import com.halovoid.lncrawler.ui.feature.request.RequestDetailViewModel
import com.halovoid.lncrawler.ui.feature.request.RequestViewModel
import com.halovoid.lncrawler.ui.feature.search.GlobalSearchViewModel
import com.halovoid.lncrawler.ui.feature.search.SearchViewModel
import com.halovoid.lncrawler.ui.feature.settings.SettingsViewModel

class ViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(RequestViewModel::class.java) -> {
                RequestViewModel(application, RequestRepository.getInstance(application)) as T
            }
            modelClass.isAssignableFrom(SearchViewModel::class.java) -> {
                SearchViewModel(application) as T
            }
            modelClass.isAssignableFrom(GlobalSearchViewModel::class.java) -> {
                GlobalSearchViewModel(application) as T
            }
            modelClass.isAssignableFrom(RequestDetailViewModel::class.java) -> {
                RequestDetailViewModel(application, RequestRepository.getInstance(application)) as T
            }
            modelClass.isAssignableFrom(NovelDetailViewModel::class.java) -> {
                NovelDetailViewModel(
                    application,
                    RequestRepository.getInstance(application),
                    preferenceRepository = PreferenceRepository.getInstance(application)
                ) as T
            }
            modelClass.isAssignableFrom(FolderViewModel::class.java) -> {
                FolderViewModel(application, PreferenceRepository.getInstance(application)) as T
            }
            modelClass.isAssignableFrom(LibraryViewModel::class.java) -> {
                LibraryViewModel(
                    application,
                    NovelRepository.getInstance(application),
                    PreferenceRepository.getInstance(application)
                ) as T
            }
            modelClass.isAssignableFrom(CrawlerViewModel::class.java) -> {
                CrawlerViewModel(application, PreferenceRepository.getInstance(application)) as T
            }
            modelClass.isAssignableFrom(SettingsViewModel::class.java) -> {
                SettingsViewModel(application) as T
            }
            modelClass.isAssignableFrom(GroupedRequestsViewModel::class.java) -> {
                GroupedRequestsViewModel(application, RequestRepository.getInstance(application)) as T
            }
            modelClass.isAssignableFrom(DownloadViewModel::class.java) -> {
                DownloadViewModel(application, RequestRepository.getInstance(application)) as T
            }
            modelClass.isAssignableFrom(ReaderViewModel::class.java) -> {
                ReaderViewModel(
                    application,
                    ChapterRepository.getInstance(application),
                    NovelRepository.getInstance(application),
                    ReaderRepository.getInstance(application)
                ) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
