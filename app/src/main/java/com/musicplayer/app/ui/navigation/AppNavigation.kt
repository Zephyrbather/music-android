package com.musicplayer.app.ui.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.PlaylistPlay
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.musicplayer.app.ui.home.HomeScreen
import com.musicplayer.app.ui.i18n.LocalStrings
import com.musicplayer.app.ui.i18n.ProvideLocalizedStrings
import com.musicplayer.app.ui.library.AlbumDetailScreen
import com.musicplayer.app.ui.library.ArtistDetailScreen
import com.musicplayer.app.ui.library.LibraryScreen
import com.musicplayer.app.ui.player.MiniPlayerBar
import com.musicplayer.app.ui.player.NowPlayingScreen
import com.musicplayer.app.ui.player.PlayerViewModel
import com.musicplayer.app.ui.player.ScrapeDetailScreen
import com.musicplayer.app.ui.playlists.PlaylistDetailScreen
import com.musicplayer.app.ui.playlists.PlaylistsScreen
import com.musicplayer.app.ui.settings.SettingsScreen
import com.musicplayer.app.ui.settings.SettingsViewModel
import com.musicplayer.app.ui.settings.WebDavBrowserScreen
import com.musicplayer.app.ui.settings.WebDavScreen
import com.musicplayer.app.ui.theme.MusicPlayerTheme

private sealed class AppDestination(
    val route: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    data object Home : AppDestination("home", "Home", Icons.Outlined.Home)
    data object Library : AppDestination("library", "Library", Icons.Outlined.LibraryMusic)
    data object Playlists : AppDestination("playlists", "Playlists", Icons.Outlined.PlaylistPlay)
    data object Settings : AppDestination("settings", "Settings", Icons.Outlined.Settings)
    data object NowPlaying : AppDestination("now-playing", "Playing", Icons.Outlined.LibraryMusic)

    companion object {
        val bottomItems = listOf(Home, Library, Playlists, Settings)
    }
}

@Composable
fun AppNavigation() {
    val settingsViewModel: SettingsViewModel = hiltViewModel()
    val settings by settingsViewModel.uiState.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val playerViewModel: PlayerViewModel = hiltViewModel()
    val playbackState by playerViewModel.playbackState.collectAsStateWithLifecycle()
    val controlsState by playerViewModel.controlsState.collectAsStateWithLifecycle()
    val comments by playerViewModel.comments.collectAsStateWithLifecycle()
    val showCommentDialog by playerViewModel.showCommentDialog.collectAsStateWithLifecycle()
    val candidates by playerViewModel.candidates.collectAsStateWithLifecycle()
    val showCandidateDialog by playerViewModel.showCandidateDialog.collectAsStateWithLifecycle()
    val scrapeCandidates by playerViewModel.scrapeCandidates.collectAsStateWithLifecycle()
    val selectedScrapeCandidate by playerViewModel.selectedScrapeCandidate.collectAsStateWithLifecycle()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val currentRoute = currentDestination?.route
    val nowPlayingRoute = AppDestination.NowPlaying.route
    val showPlayerChrome = currentRoute != nowPlayingRoute

    MusicPlayerTheme(
        themeMode = settings.themeMode,
        themeStyle = settings.themeStyle,
        dynamicColor = settings.dynamicColor,
        currentArtworkUri = playbackState.currentSong?.artworkUri,
        fontFamilyOption = settings.fontFamilyOption
    ) {
        ProvideLocalizedStrings(settings.appLanguage) {
            val strings = LocalStrings.current
            Scaffold(
        bottomBar = {
            if (showPlayerChrome) {
                Column {
                    MiniPlayerBar(
                        playbackState = playbackState,
                        onClick = { navController.navigate(nowPlayingRoute) },
                        onTogglePlayPause = playerViewModel::togglePlayPause,
                        onSkipNext = playerViewModel::skipNext,
                        onQueueClick = { navController.navigate(nowPlayingRoute) }
                    )
                    NavigationBar(
                        modifier = Modifier.height(60.dp)
                    ) {
                        AppDestination.bottomItems.forEach { destination ->
                            val selected = currentDestination?.hierarchy?.any { it.route == destination.route } == true
                            val label = when (destination) {
                                AppDestination.Home -> strings.home
                                AppDestination.Library -> strings.library
                                AppDestination.Playlists -> strings.playlists
                                AppDestination.Settings -> strings.settings
                                AppDestination.NowPlaying -> strings.nowPlaying
                            }
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(destination.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = destination.icon,
                                        contentDescription = label
                                    )
                                },
                                label = { Text(label) }
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = AppDestination.Home.route,
            modifier = if (showPlayerChrome) Modifier.padding(innerPadding) else Modifier
        ) {
            composable(AppDestination.Home.route) {
                HomeScreen(
                    onPlaySong = playerViewModel::play
                )
            }
            composable(AppDestination.Library.route) {
                LibraryScreen(
                    onPlaySong = playerViewModel::play,
                    onOpenArtist = { artistId -> navController.navigate("artist/$artistId") },
                    onOpenAlbum = { albumId -> navController.navigate("album/$albumId") },
                    currentSong = playbackState.currentSong
                )
            }
            composable("artist/{artistId}") { entry ->
                ArtistDetailScreen(
                    artistId = entry.arguments?.getString("artistId").orEmpty(),
                    onBack = navController::navigateUp,
                    onPlaySong = playerViewModel::play
                )
            }
            composable("album/{albumId}") { entry ->
                AlbumDetailScreen(
                    albumId = entry.arguments?.getString("albumId").orEmpty(),
                    onBack = navController::navigateUp,
                    onPlaySong = playerViewModel::play
                )
            }
            composable(AppDestination.Playlists.route) {
                PlaylistsScreen(
                    onOpenPlaylist = { playlistId -> navController.navigate("playlist/$playlistId") }
                )
            }
            composable("playlist/{playlistId}") { entry ->
                val playlistId = entry.arguments?.getString("playlistId").orEmpty()
                PlaylistDetailScreen(
                    playlistId = playlistId,
                    onPlaySong = playerViewModel::play
                )
            }
            composable(AppDestination.Settings.route) {
                SettingsScreen(
                    onNavigateToWebDav = { navController.navigate("webdav") }
                )
            }
            composable("webdav") {
                WebDavScreen(
                    onBack = navController::navigateUp,
                    onBrowseServer = { serverId -> navController.navigate("webdav-browse/$serverId") }
                )
            }
            composable("webdav-browse/{serverId}") { entry ->
                WebDavBrowserScreen(
                    serverId = entry.arguments?.getString("serverId").orEmpty(),
                    onBack = navController::navigateUp
                )
            }
            composable("scrape-detail") {
                ScrapeDetailScreen(
                    playbackState = playbackState,
                    controlsState = controlsState,
                    scrapeCandidates = scrapeCandidates,
                    selectedScrapeCandidate = selectedScrapeCandidate,
                    onBack = navController::navigateUp,
                    onSearchKeywordChange = playerViewModel::setSearchKeyword,
                    onSearchModeChange = playerViewModel::setScrapeSearchMode,
                    onPrepareScrape = playerViewModel::prepareScrapeDetail,
                    onScrapeCurrent = playerViewModel::scrapeCurrent,
                    onAiScrapeCurrent = playerViewModel::aiScrapeCurrent,
                    onSelectScrapeCandidate = playerViewModel::selectScrapeCandidate,
                    onApplyScrapeSelection = playerViewModel::applyScrapeSelection
                )
            }
            composable(AppDestination.NowPlaying.route) {
                NowPlayingScreen(
                    playbackState = playbackState,
                    controlsState = controlsState,
                    onSkipPrevious = playerViewModel::skipPrevious,
                    onTogglePlayPause = playerViewModel::togglePlayPause,
                    onSkipNext = playerViewModel::skipNext,
                    onSeekTo = playerViewModel::seekTo,
                    onQueueSongSelected = playerViewModel::playQueueSong,
                    onPlaybackModeChange = playerViewModel::setPlaybackMode,
                    onPresetSelected = playerViewModel::selectPreset,
                    onDraftNameChange = playerViewModel::setDraftName,
                    onBandLevelChange = playerViewModel::setBandLevel,
                    onCreatePreset = playerViewModel::createPreset,
                    onSavePreset = playerViewModel::savePreset,
                    onDeletePreset = playerViewModel::deletePreset,
                    onCloseEqualizer = playerViewModel::closeEqualizer,
                    onOpenScrapeDetail = { navController.navigate("scrape-detail") },
                    showCommentDialog = showCommentDialog,
                    comments = comments,
                    onOpenComments = playerViewModel::openComments,
                    onSaveComment = playerViewModel::saveComment,
                    onDismissComments = playerViewModel::dismissComments,
                    showCandidateDialog = showCandidateDialog,
                    candidates = candidates,
                    onOpenCandidatePicker = playerViewModel::openCandidatePicker,
                    onApplyCandidateResult = playerViewModel::applyCandidateResult,
                    onDismissCandidatePicker = playerViewModel::dismissCandidatePicker
                )
            }
        }
    }
        }
    }
}
