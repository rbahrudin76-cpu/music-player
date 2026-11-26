package com.example.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddToDrive
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.rounded.AddToDrive
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.R
import com.example.ui.components.MiniPlayerBar
import com.example.ui.screens.DriveScreen
import com.example.ui.screens.EqualizerScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.NowPlayingScreen
import com.example.ui.screens.SearchScreen
import com.example.viewmodel.MainViewModel
import kotlinx.serialization.Serializable

@Serializable
object LibraryRoute

@Serializable
object SearchRoute

@Serializable
object DriveRoute

@Serializable
object EqualizerRoute

private data class NavTab(
    val route: Any,
    val routeClass: kotlin.reflect.KClass<*>,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private val navTabs = listOf(
    NavTab(LibraryRoute, LibraryRoute::class, R.string.nav_library, Icons.Rounded.LibraryMusic, Icons.Outlined.LibraryMusic, "nav_library"),
    NavTab(SearchRoute, SearchRoute::class, R.string.nav_search, Icons.Rounded.Search, Icons.Outlined.Search, "nav_search"),
    NavTab(DriveRoute, DriveRoute::class, R.string.nav_drive, Icons.Rounded.AddToDrive, Icons.Outlined.AddToDrive, "nav_drive"),
    NavTab(EqualizerRoute, EqualizerRoute::class, R.string.nav_equalizer, Icons.Rounded.GraphicEq, Icons.Outlined.GraphicEq, "nav_equalizer")
)

@Composable
fun AuraPlayRoot(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDest = backStackEntry?.destination

    val libraryState by viewModel.libraryUiState.collectAsStateWithLifecycle()
    val searchState by viewModel.searchUiState.collectAsStateWithLifecycle()
    val driveState by viewModel.driveUiState.collectAsStateWithLifecycle()
    val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()
    val activePlayer by viewModel.activePlayer.collectAsStateWithLifecycle()
    val eqState by viewModel.equalizerState.collectAsStateWithLifecycle()
    val customPresets by viewModel.customEqPresets.collectAsStateWithLifecycle()
    val isFullPlayerOpen by viewModel.isFullPlayerOpen.collectAsStateWithLifecycle()
    val videoAspectMode by viewModel.videoAspectMode.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val notificationPermissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
            onResult = { }
        )
        LaunchedEffect(playbackState.isPlaying) {
            if (playbackState.isPlaying) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    LaunchedEffect(playbackState.playbackError) {
        val err = playbackState.playbackError
        if (!err.isNullOrBlank()) {
            snackbarHostState.showSnackbar(err)
            viewModel.playerController.clearError()
        }
    }

    fun navigateToTab(route: Any) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isExpandedWidth = maxWidth >= 640.dp

        Scaffold(
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                if (!isExpandedWidth) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        MiniPlayerBar(
                            playbackState = playbackState,
                            onClickExpand = { viewModel.openFullPlayer() },
                            onTogglePlayPause = { viewModel.playerController.togglePlayPause() },
                            onSkipNext = { viewModel.playerController.skipToNext() }
                        )
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface
                        ) {
                            navTabs.forEach { tab ->
                                val selected = currentDest?.hasRoute(tab.routeClass) == true
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { navigateToTab(tab.route) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = stringResource(tab.labelRes)
                                        )
                                    },
                                    label = { Text(stringResource(tab.labelRes)) },
                                    modifier = Modifier.testTag(tab.testTag)
                                )
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isExpandedWidth) {
                    NavigationRail(
                        modifier = Modifier
                            .fillMaxHeight()
                            .statusBarsPadding()
                            .navigationBarsPadding(),
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        navTabs.forEach { tab ->
                            val selected = currentDest?.hasRoute(tab.routeClass) == true
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateToTab(tab.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = stringResource(tab.labelRes)
                                    )
                                },
                                label = { Text(stringResource(tab.labelRes)) },
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .statusBarsPadding()
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = LibraryRoute,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        composable<LibraryRoute> {
                            LibraryScreen(
                                state = libraryState,
                                playbackState = playbackState,
                                onSelectFilter = viewModel::setLibraryFilter,
                                onFolderPicked = viewModel::linkAndScanFolder,
                                onFilesPicked = { viewModel.importPickedFiles(it, autoPlayFirst = true) },
                                onAddDirectUrl = viewModel::addAndPlayDirectUrl,
                                onRescanFolders = viewModel::rescanFolders,
                                onRemoveFolder = viewModel::removeLinkedFolder,
                                onPlayTracks = { list, idx -> viewModel.playTracks(list, idx) },
                                onAddToQueue = viewModel::addToQueue,
                                onToggleFavorite = viewModel::toggleFavorite,
                                onDismissStatus = viewModel::clearLibraryMessage
                            )
                        }

                        composable<SearchRoute> {
                            SearchScreen(
                                state = searchState,
                                playbackState = playbackState,
                                onQueryChange = viewModel::onSearchQueryChanged,
                                onSubmitSearch = viewModel::executeSearchNow,
                                onDeleteHistory = viewModel::deleteSearchHistory,
                                onPlayTracks = { list, idx -> viewModel.playTracks(list, idx) },
                                onAddToQueue = viewModel::addToQueue,
                                onToggleFavorite = viewModel::toggleFavorite
                            )
                        }

                        composable<DriveRoute> {
                            DriveScreen(
                                state = driveState,
                                playbackState = playbackState,
                                onConnectClick = viewModel::connectGoogleDrive,
                                onResolutionConsumed = viewModel::consumePendingDriveResolution,
                                onResolutionResult = viewModel::onDriveResolutionResult,
                                onConnectManualToken = viewModel::connectDriveWithManualToken,
                                onSyncClick = { viewModel.syncGoogleDrive(autoStartIfEnabled = false) },
                                onAutoPlayAllClick = viewModel::autoPlayAllDriveMedia,
                                onSetAutoPlay = viewModel::setDriveAutoPlay,
                                onSetFilter = viewModel::setDriveFilter,
                                onDisconnectClick = viewModel::disconnectGoogleDrive,
                                onPlayTracks = { list, idx -> viewModel.playTracks(list, idx) },
                                onAddToQueue = viewModel::addToQueue,
                                onToggleFavorite = viewModel::toggleFavorite
                            )
                        }

                        composable<EqualizerRoute> {
                            EqualizerScreen(
                                eqState = eqState,
                                playbackState = playbackState,
                                builtInPresets = viewModel.audioEffectEngine.builtInPresets,
                                customPresets = customPresets,
                                onToggleEnabled = viewModel.audioEffectEngine::setEnabled,
                                onBandChanged = viewModel.audioEffectEngine::setBandLevel,
                                onApplyPreset = viewModel.audioEffectEngine::applyPreset,
                                onSaveCustomPreset = viewModel::saveCurrentAsCustomPreset,
                                onDeleteCustomPreset = viewModel::deleteCustomPreset,
                                onBassBoostChanged = viewModel.audioEffectEngine::setBassBoost,
                                onVirtualizerChanged = viewModel.audioEffectEngine::setVirtualizer,
                                onLoudnessChanged = viewModel.audioEffectEngine::setLoudnessGain,
                                onReverbChanged = viewModel.audioEffectEngine::setReverbPreset,
                                onSpeedPitchChanged = viewModel.playerController::applyPlaybackParameters,
                                onSleepTimerSelected = viewModel.playerController::setSleepTimerMinutes
                            )
                        }
                    }

                    if (isExpandedWidth) {
                        MiniPlayerBar(
                            playbackState = playbackState,
                            onClickExpand = { viewModel.openFullPlayer() },
                            onTogglePlayPause = { viewModel.playerController.togglePlayPause() },
                            onSkipNext = { viewModel.playerController.skipToNext() },
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .navigationBarsPadding()
                        )
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = isFullPlayerOpen,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it })
        ) {
            NowPlayingScreen(
                playbackState = playbackState,
                player = activePlayer,
                videoAspectMode = videoAspectMode,
                onClose = { viewModel.closeFullPlayer() },
                onTogglePlayPause = { viewModel.playerController.togglePlayPause() },
                onSeekTo = { viewModel.playerController.seekTo(it) },
                onSeekBy = { viewModel.playerController.seekBy(it) },
                onNext = { viewModel.playerController.skipToNext() },
                onPrevious = { viewModel.playerController.skipToPrevious() },
                onToggleShuffle = { viewModel.playerController.toggleShuffle() },
                onCycleRepeat = { viewModel.playerController.cycleRepeatMode() },
                onCycleAspectMode = { viewModel.cycleVideoAspectMode() },
                onToggleFavorite = {
                    playbackState.currentTrack?.let { viewModel.toggleFavorite(it) }
                },
                onSkipToQueueIndex = { viewModel.playerController.skipToQueueIndex(it) },
                onOpenEqualizer = { navigateToTab(EqualizerRoute) }
            )
        }
    }
}
