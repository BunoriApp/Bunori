package com.halovoid.lncrawler.ui.navigation

import android.app.Application
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.halovoid.lncrawler.data.db.entities.RequestType
import com.halovoid.lncrawler.data.repository.PreferenceRepository
import com.halovoid.lncrawler.ui.ViewModelFactory
import com.halovoid.lncrawler.ui.feature.crawler.CrawlerScreen
import com.halovoid.lncrawler.ui.feature.crawler.CrawlerViewModel
import com.halovoid.lncrawler.ui.feature.downloads.DownloadScreen
import com.halovoid.lncrawler.ui.feature.downloads.DownloadViewModel
import com.halovoid.lncrawler.ui.feature.library.LibraryScreen
import com.halovoid.lncrawler.ui.feature.library.LibraryViewModel
import com.halovoid.lncrawler.ui.feature.novel.GroupedRequestsScreen
import com.halovoid.lncrawler.ui.feature.novel.GroupedRequestsViewModel
import com.halovoid.lncrawler.ui.feature.novel.NovelActivityScreen
import com.halovoid.lncrawler.ui.feature.novel.NovelArtifactsScreen
import com.halovoid.lncrawler.ui.feature.novel.NovelDetailScreen
import com.halovoid.lncrawler.ui.feature.novel.NovelDetailViewModel
import com.halovoid.lncrawler.ui.feature.onboarding.FolderScreen
import com.halovoid.lncrawler.ui.feature.onboarding.FolderViewModel
import com.halovoid.lncrawler.ui.feature.onboarding.PermissionScreen
import com.halovoid.lncrawler.ui.feature.onboarding.SourceSyncScreen
import com.halovoid.lncrawler.ui.feature.onboarding.WelcomeScreen
import com.halovoid.lncrawler.ui.feature.reader.ReaderScreen
import com.halovoid.lncrawler.ui.feature.reader.ReaderViewModel
import com.halovoid.lncrawler.ui.feature.request.ManualRequestScreen
import com.halovoid.lncrawler.ui.feature.request.NovelPreviewScreen
import com.halovoid.lncrawler.ui.feature.request.RequestDetailScreen
import com.halovoid.lncrawler.ui.feature.request.RequestScreen
import com.halovoid.lncrawler.ui.feature.request.RequestViewModel
import com.halovoid.lncrawler.ui.feature.search.ExperimentalSearchScreen
import com.halovoid.lncrawler.ui.feature.search.SearchViewModel
import com.halovoid.lncrawler.ui.feature.settings.AdvancedSettingsScreen
import com.halovoid.lncrawler.ui.feature.settings.BackupSettingsScreen
import com.halovoid.lncrawler.ui.feature.settings.DownloadPreferencesScreen
import com.halovoid.lncrawler.ui.feature.settings.MoreScreen
import com.halovoid.lncrawler.ui.feature.settings.SettingsViewModel
import com.halovoid.lncrawler.ui.feature.settings.SupportSettingsScreen
import com.halovoid.lncrawler.ui.feature.settings.UpdateDetailScreen
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.net.URLDecoder
import java.net.URLEncoder

sealed class Screen(val route: String) {
    object Welcome : Screen("welcome")
    object Permissions : Screen("permissions")
    object FolderSelection: Screen("folder_selection")
    object SourceSync: Screen("source_sync")
    object Request : Screen("request")
    object Library : Screen("library")
    object Downloads : Screen("downloads")
    object Crawlers : Screen("crawlers")
    object Support : Screen("support")
    object DownloadPreferences : Screen("download_preferences")
    object AdvancedSettings : Screen("advanced_settings")
    object SupportSettings : Screen("support_settings")
    object BackupSettings : Screen("backup_settings")
    object UpdateDetail : Screen("update_detail")
    object RequestDetail : Screen("request_detail/{requestId}") {
        fun createRoute(requestId: String) = "request_detail/${URLEncoder.encode(requestId, "UTF-8")}"
    }
    object NovelDetail : Screen("novel_detail/{crawlerName}/{novelUrl}") {
        fun createRoute(crawlerName: String, novelUrl: String) = "novel_detail/$crawlerName/${URLEncoder.encode(novelUrl, "UTF-8")}"
    }
    object NovelActivity : Screen("novel_activity/{novelUrl}") {
        fun createRoute(novelUrl: String) = "novel_activity/${URLEncoder.encode(novelUrl, "UTF-8")}"
    }
    object NovelArtifacts : Screen("novel_artifacts/{novelUrl}") {
        fun createRoute(novelUrl: String) = "novel_artifacts/${URLEncoder.encode(novelUrl, "UTF-8")}"
    }
    object NovelPreview : Screen("novel_preview")
    object GroupedRequests : Screen("grouped_requests/{contextType}/{contextValue}/{type}") {
        fun createRoute(contextType: String, contextValue: String, type: String) = 
            "grouped_requests/$contextType/${URLEncoder.encode(contextValue, "UTF-8")}/$type"
    }
    object Reader : Screen("reader/{novelUrl}/{initialChapterId}") {
        fun createRoute(novelUrl: String, initialChapterId: Int) = 
            "reader/${URLEncoder.encode(novelUrl, "UTF-8")}/$initialChapterId"
    }
    object ExperimentalSearch : Screen("experimental_search")
    object ManualRequest : Screen("manual_request")
}

@Composable
fun NavGraph(navController: NavHostController) {
    val application = LocalContext.current.applicationContext as Application
    val preferenceRepository = remember { PreferenceRepository.getInstance(application) }
    val scope = rememberCoroutineScope()
    
    var startRoute by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val folderUri = preferenceRepository.exportFolderUri.first()
        val onboardingCompleted = preferenceRepository.isOnboardingCompleted.first()

        startRoute = if (onboardingCompleted && folderUri != null) {
            Screen.Request.route
        } else if (!onboardingCompleted) {
            Screen.Welcome.route
        } else {
            Screen.FolderSelection.route
        }
    }
    startRoute?.let { route ->
        NavHost(
            navController = navController,
            startDestination = route,
            enterTransition = {
                fadeIn(animationSpec = tween(200))
            },
            exitTransition = {
                fadeOut(animationSpec = tween(200))
            },
            popEnterTransition = {
                fadeIn(animationSpec = tween(200))
            },
            popExitTransition = {
                fadeOut(animationSpec = tween(200))
            }
        ) {
            composable(Screen.Welcome.route) {
                WelcomeScreen(
                    onNext = {
                        navController.navigate(Screen.FolderSelection.route)
                    }
                )
            }
            composable(Screen.FolderSelection.route) {
                val folderViewModel: FolderViewModel = viewModel(
                    factory = remember { ViewModelFactory(application) }
                )
                FolderScreen(
                    folderViewModel,
                    onNext = {
                        navController.navigate(Screen.Permissions.route)
                    }
                )
            }
            composable(Screen.Permissions.route) {
                PermissionScreen(
                    onNext = {
                        navController.navigate(Screen.SourceSync.route)
                    }
                )
            }
            composable(Screen.SourceSync.route) {
                SourceSyncScreen(
                    onComplete = {
                        scope.launch {
                            preferenceRepository.setOnboardingCompleted(true)
                            navController.navigate(Screen.Request.route) {
                                popUpTo(Screen.FolderSelection.route) {
                                    inclusive = true
                                }
                            }
                        }
                    }
                )
            }
            composable(Screen.NovelPreview.route) { backStackEntry ->
                val parentEntry = remember(backStackEntry) {
                    navController.getBackStackEntry(navController.graph.id)
                }
                val requestViewModel: RequestViewModel = viewModel(
                    viewModelStoreOwner = parentEntry,
                    factory = remember { ViewModelFactory(application) }
                )
                NovelPreviewScreen(
                    viewModel = requestViewModel,
                    onBack = {
                        requestViewModel.clearPreview()
                        navController.popBackStack()
                    },
                    onConfirm = { novel ->
                        requestViewModel.addNovelDirectly(novel)
                    },
                    onCrawlManually = { crawlerName, url, title ->
                        requestViewModel.startNovelCrawl(crawlerName, url, title)
                        requestViewModel.clearPreview()
                        navController.popBackStack()
                    }
                )
            }
            composable(Screen.Request.route) { backStackEntry ->
                val parentEntry = remember(backStackEntry) {
                    navController.getBackStackEntry(navController.graph.id)
                }
                val requestViewModel: RequestViewModel = viewModel(
                    viewModelStoreOwner = parentEntry,
                    factory = remember { ViewModelFactory(application) }
                )
                val crawlerViewModel: CrawlerViewModel = viewModel(
                    factory = remember { ViewModelFactory(application) }
                )
                RequestScreen(
                    viewModel = requestViewModel,
                    crawlerViewModel = crawlerViewModel,
                    onNavigateToPreview = {
                        navController.navigate(Screen.NovelPreview.route)
                    },
                    onNavigateToDetail = { crawlerName, novelUrl ->
                        navController.navigate(
                            Screen.NovelDetail.createRoute(
                                crawlerName,
                                novelUrl
                            )
                        )
                    },
                    onNavigateToRequest = {
                        navController.navigate(Screen.ManualRequest.route)
                    }
                )
            }
            composable(Screen.ManualRequest.route) { backStackEntry ->
                val parentEntry = remember(backStackEntry) {
                    navController.getBackStackEntry(navController.graph.id)
                }
                val requestViewModel: RequestViewModel = viewModel(
                    viewModelStoreOwner = parentEntry,
                    factory = remember { ViewModelFactory(application) }
                )
                ManualRequestScreen(
                    viewModel = requestViewModel,
                    searchUrl = null,
                    onBack = { navController.popBackStack() },
                    onNavigateToPreview = {
                        navController.navigate(Screen.NovelPreview.route)
                    },
                    onNavigateToDetail = { crawlerName, novelUrl ->
                        navController.navigate(
                            Screen.NovelDetail.createRoute(
                                crawlerName,
                                novelUrl
                            )
                        )
                    }
                )
            }
            composable(Screen.Library.route) {
                val libraryViewModel: LibraryViewModel = viewModel(
                    factory = remember { ViewModelFactory(application) }
                )
                LibraryScreen(
                    viewModel = libraryViewModel,
                    onNovelClick = { crawlerName, novelUrl ->
                        navController.navigate(
                            Screen.NovelDetail.createRoute(
                                crawlerName,
                                novelUrl
                            )
                        )
                    },
                    onBackClick = {
                        navController.popBackStack()
                    }
                )
            }
            composable(Screen.Crawlers.route) {
                val crawlerViewModel: CrawlerViewModel = viewModel(
                    factory = remember { ViewModelFactory(application) }
                )
                CrawlerScreen(
                    viewModel = crawlerViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Downloads.route) {
                val downloadViewModel: DownloadViewModel = viewModel(
                    factory = remember { ViewModelFactory(application) }
                )
                DownloadScreen(
                    viewModel = downloadViewModel,
                    onRequestClick = { requestId: String ->
                        navController.navigate(Screen.RequestDetail.createRoute(requestId))
                    },
                    onGroupClick = { type: RequestType ->
                        navController.navigate(Screen.GroupedRequests.createRoute("ALL", "all", type.name))
                    }
                )
            }
            composable(Screen.Support.route) {
                val settingsViewModel: SettingsViewModel = viewModel(
                    factory = remember { ViewModelFactory(application) }
                )
                MoreScreen(
                    viewModel = settingsViewModel,
                    onNavigateToDownloadsPref = {
                        navController.navigate(Screen.DownloadPreferences.route)
                    },
                    onNavigateToAdvanced = {
                        navController.navigate(Screen.AdvancedSettings.route)
                    },
                    onNavigateToSupportSettings = {
                        navController.navigate(Screen.SupportSettings.route)
                    },
                    onNavigateToBackupSettings = {
                        navController.navigate(Screen.BackupSettings.route)
                    },
                    onNavigateToUpdate = {
                        navController.navigate(Screen.UpdateDetail.route)
                    }
                )
            }
            composable(Screen.BackupSettings.route) {
                BackupSettingsScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.UpdateDetail.route) { backStackEntry ->
                val supportEntry = remember(backStackEntry) {
                    navController.getBackStackEntry(Screen.Support.route)
                }
                val settingsViewModel: SettingsViewModel = viewModel(
                    viewModelStoreOwner = supportEntry,
                    factory = remember { ViewModelFactory(application) }
                )
                UpdateDetailScreen(
                    viewModel = settingsViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.DownloadPreferences.route) { backStackEntry ->
                val supportEntry = remember(backStackEntry) {
                    navController.getBackStackEntry(Screen.Support.route)
                }
                val settingsViewModel: SettingsViewModel = viewModel(
                    viewModelStoreOwner = supportEntry,
                    factory = remember { ViewModelFactory(application) }
                )
                DownloadPreferencesScreen(
                    viewModel = settingsViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.AdvancedSettings.route) { backStackEntry ->
                val supportEntry = remember(backStackEntry) {
                    navController.getBackStackEntry(Screen.Support.route)
                }
                val settingsViewModel: SettingsViewModel = viewModel(
                    viewModelStoreOwner = supportEntry,
                    factory = remember { ViewModelFactory(application) }
                )
                AdvancedSettingsScreen(
                    viewModel = settingsViewModel,
                    onBack = { navController.popBackStack() },
                    onNavigateToExperimentalSearch = {
                        navController.navigate(Screen.ExperimentalSearch.route)
                    }
                )
            }
            composable(Screen.ExperimentalSearch.route) { backStackEntry ->
                val parentEntry = remember(backStackEntry) {
                    navController.getBackStackEntry(navController.graph.id)
                }
                val searchViewModel: SearchViewModel = viewModel(
                    factory = remember { ViewModelFactory(application) }
                )
                val requestViewModel: RequestViewModel = viewModel(
                    viewModelStoreOwner = parentEntry,
                    factory = remember { ViewModelFactory(application) }
                )
                ExperimentalSearchScreen(
                    searchViewModel = searchViewModel,
                    requestViewModel = requestViewModel,
                    onBack = { navController.popBackStack() },
                    onNavigateToPreview = {
                        navController.navigate(Screen.NovelPreview.route)
                    },
                    onNavigateToDetail = { crawlerName, novelUrl ->
                        navController.navigate(
                            Screen.NovelDetail.createRoute(
                                crawlerName,
                                novelUrl
                            )
                        )
                    }
                )
            }
            composable(Screen.SupportSettings.route) {
                SupportSettingsScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.RequestDetail.route) { backStackEntry ->
                val encodedId = backStackEntry.arguments?.getString("requestId") ?: ""
                val requestId = URLDecoder.decode(encodedId, "UTF-8")

                RequestDetailScreen(
                    requestId = requestId,
                    onBackClick = {
                        navController.popBackStack()
                    },
                    onGroupClick = { type ->
                        navController.navigate(Screen.GroupedRequests.createRoute("DEPENDENCY", requestId, type.name))
                    },
                    onRequestClick = { id ->
                        navController.navigate(Screen.RequestDetail.createRoute(id))
                    }
                )
            }
            composable(Screen.NovelDetail.route) { backStackEntry ->
                val crawlerName = backStackEntry.arguments?.getString("crawlerName") ?: ""
                val novelUrl = URLDecoder.decode(
                    backStackEntry.arguments?.getString("novelUrl") ?: "",
                    "UTF-8"
                )
                NovelDetailScreen(
                    novelUrl = novelUrl,
                    onRequestClick = { requestId ->
                        navController.navigate(Screen.RequestDetail.createRoute(requestId))
                    },
                    onChapterClick = { url, chapterId ->
                        navController.navigate(Screen.Reader.createRoute(url, chapterId))
                    },
                    onBack = {
                        navController.popBackStack()
                    },
                    onActivityClick = {
                        navController.navigate(Screen.NovelActivity.createRoute(novelUrl))
                    },
                    onArtifactsClick = {
                        navController.navigate(Screen.NovelArtifacts.createRoute(novelUrl))
                    }
                )
            }
            composable(Screen.NovelActivity.route) { backStackEntry ->
                val novelUrl = URLDecoder.decode(
                    backStackEntry.arguments?.getString("novelUrl") ?: "",
                    "UTF-8"
                )
                val viewModel: GroupedRequestsViewModel = viewModel(
                    factory = remember { ViewModelFactory(application) }
                )
                LaunchedEffect(novelUrl) {
                    viewModel.loadRequests("NOVEL", novelUrl)
                }
                val requests by viewModel.requests.collectAsStateWithLifecycle()
                val cancellingRequestIds by viewModel.cancellingRequestIds.collectAsStateWithLifecycle()
                val activeActionIds by viewModel.activeActionIds.collectAsStateWithLifecycle()

                NovelActivityScreen(
                    requests = requests,
                    onBack = { navController.popBackStack() },
                    onRequestClick = { requestId ->
                        navController.navigate(Screen.RequestDetail.createRoute(requestId))
                    },
                    onReplay = { viewModel.replayRequest(it) },
                    onCancel = { viewModel.cancelRequest(it) },
                    onContinue = { viewModel.resumeRequest(it) },
                    onResolveCloudflare = { requestId, url ->
                        viewModel.resolveCloudflare(requestId, url)
                    },
                    cancellingRequestIds = cancellingRequestIds,
                    activeActionIds = activeActionIds
                )
            }
            composable(Screen.NovelArtifacts.route) { backStackEntry ->
                val novelUrl = URLDecoder.decode(
                    backStackEntry.arguments?.getString("novelUrl") ?: "",
                    "UTF-8"
                )
                val viewModel: NovelDetailViewModel = viewModel(
                    factory = remember { ViewModelFactory(application) }
                )
                LaunchedEffect(novelUrl) {
                    viewModel.loadNovel(novelUrl)
                }
                val artifacts by viewModel.artifacts.collectAsStateWithLifecycle()
                val novel by viewModel.novel.collectAsStateWithLifecycle()

                NovelArtifactsScreen(
                    novel = novel,
                    artifacts = artifacts,
                    onBack = { navController.popBackStack() },
                    onDownload = { _ -> }
                )
            }
            composable(Screen.GroupedRequests.route) { backStackEntry ->
                val contextType = backStackEntry.arguments?.getString("contextType") ?: ""
                val contextValue = URLDecoder.decode(
                    backStackEntry.arguments?.getString("contextValue") ?: "",
                    "UTF-8"
                )
                val typeName = backStackEntry.arguments?.getString("type") ?: ""
                val type = RequestType.valueOf(typeName)

                val viewModel: GroupedRequestsViewModel = viewModel(
                    factory = remember { ViewModelFactory(application) }
                )

                LaunchedEffect(contextType, contextValue) {
                    viewModel.loadRequests(contextType, contextValue)
                }

                val requests by viewModel.requests.collectAsStateWithLifecycle()
                val allRequests by viewModel.allRequests.collectAsStateWithLifecycle()
                val cancellingRequestIds by viewModel.cancellingRequestIds.collectAsStateWithLifecycle()
                val activeActionIds by viewModel.activeActionIds.collectAsStateWithLifecycle()
                val statusFilters by viewModel.statusFilters.collectAsStateWithLifecycle()

                GroupedRequestsScreen(
                    type = type,
                    requests = requests,
                    allRequests = allRequests,
                    statusFilters = statusFilters,
                    onStatusFilterChange = { status, state -> viewModel.setStatusFilter(status, state) },
                    onBack = { navController.popBackStack() },
                    onRequestClick = { requestId ->
                        navController.navigate(Screen.RequestDetail.createRoute(requestId))
                    },
                    onReplay = { viewModel.replayRequest(it) },
                    onCancel = { viewModel.cancelRequest(it) },
                    onContinue = { viewModel.resumeRequest(it) },
                    onResolveCloudflare = { requestId, url ->
                        viewModel.resolveCloudflare(requestId, url)
                    },
                    cancellingRequestIds = cancellingRequestIds,
                    activeActionIds = activeActionIds,
                    allowAction = contextType == "ALL" || contextType == "DEPENDENCY"
                )
            }
            composable(Screen.Reader.route) { backStackEntry ->
                val novelUrl = URLDecoder.decode(
                    backStackEntry.arguments?.getString("novelUrl") ?: "",
                    "UTF-8"
                )
                val initialChapterId = backStackEntry.arguments?.getString("initialChapterId")?.toIntOrNull() ?: -1

                val viewModel: ReaderViewModel = viewModel(
                    factory = remember { ViewModelFactory(application) }
                )

                ReaderScreen(
                    novelUrl = novelUrl,
                    initialChapterId = initialChapterId,
                    onBack = { navController.popBackStack() },
                    viewModel = viewModel
                )
            }
        }
    }
}
