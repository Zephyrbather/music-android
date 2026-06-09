package com.musicplayer.app.ui.navigation

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.PlaylistPlay
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import com.musicplayer.app.domain.repository.ThemeStyle
import com.musicplayer.app.ui.home.HomeScreen
import com.musicplayer.app.ui.i18n.LocalStrings
import com.musicplayer.app.ui.i18n.ProvideLocalizedStrings
import com.musicplayer.app.ui.library.AlbumDetailScreen
import com.musicplayer.app.ui.library.ArtistDetailScreen
import com.musicplayer.app.ui.library.LibraryScreen
import com.musicplayer.app.ui.player.EqualizerScreen
import com.musicplayer.app.ui.player.MiniPlayerBar
import com.musicplayer.app.ui.player.NowPlayingScreen
import com.musicplayer.app.ui.player.PlayerViewModel
import com.musicplayer.app.ui.player.ScrapeDetailScreen
import com.musicplayer.app.ui.playlists.PlaylistDetailScreen
import com.musicplayer.app.ui.playlists.PlaylistsScreen
import com.musicplayer.app.ui.settings.AppearanceSettingsScreen
import com.musicplayer.app.ui.settings.CacheSettingsScreen
import com.musicplayer.app.ui.settings.DataManagementSettingsScreen
import com.musicplayer.app.ui.settings.LocalLibrarySettingsScreen
import com.musicplayer.app.ui.settings.ScrapingSettingsScreen
import com.musicplayer.app.ui.settings.SettingsScreen
import com.musicplayer.app.ui.settings.SettingsViewModel
import com.musicplayer.app.ui.settings.WebDavSettingsScreen
import com.musicplayer.app.ui.settings.WebDavBrowserScreen
import com.musicplayer.app.ui.settings.WebDavScreen
import com.musicplayer.app.ui.theme.MusicPlayerTheme
import kotlin.math.PI
import kotlin.math.sin
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

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

private const val SETTINGS_MAIN_ROUTE = "settings/main"
private const val SETTINGS_APPEARANCE_ROUTE = "settings/appearance"
private const val SETTINGS_LOCAL_LIBRARY_ROUTE = "settings/local-library"
private const val SETTINGS_SCRAPING_ROUTE = "settings/scraping"
private const val SETTINGS_CACHE_ROUTE = "settings/cache"
private const val SETTINGS_DATA_ROUTE = "settings/data"
private const val SETTINGS_WEBDAV_ROUTE = "settings/webdav"
private const val SETTINGS_WEBDAV_MANAGE_ROUTE = "settings/webdav/manage"
private const val SETTINGS_WEBDAV_BROWSE_ROUTE = "settings/webdav-browse/{serverId}"
private const val EQUALIZER_ROUTE = "equalizer"

@Composable
fun AppNavigation() {
    val settingsViewModel: SettingsViewModel = hiltViewModel()
    val settings by settingsViewModel.uiState.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val playerViewModel: PlayerViewModel = hiltViewModel()
    val currentArtworkUriFlow = remember(playerViewModel) {
        playerViewModel.playbackState
            .map { it.currentSong?.artworkUri }
            .distinctUntilChanged()
    }
    val currentArtworkUri by currentArtworkUriFlow.collectAsStateWithLifecycle(
        initialValue = playerViewModel.playbackState.value.currentSong?.artworkUri
    )
    val isPlayingFlow = remember(playerViewModel) {
        playerViewModel.playbackState
            .map { it.isPlaying }
            .distinctUntilChanged()
    }
    val isPlaying by isPlayingFlow.collectAsStateWithLifecycle(
        initialValue = playerViewModel.playbackState.value.isPlaying
    )
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val currentRoute = currentDestination?.route
    val nowPlayingRoute = AppDestination.NowPlaying.route
    val showPlayerChrome = currentRoute != nowPlayingRoute

    MusicPlayerTheme(
        themeMode = settings.themeMode,
        themeStyle = settings.themeStyle,
        dynamicColor = settings.dynamicColor,
        currentArtworkUri = currentArtworkUri,
        fontFamilyOption = settings.fontFamilyOption
    ) {
        ProvideLocalizedStrings(settings.appLanguage) {
            val immersive = settings.themeStyle == ThemeStyle.IMMERSIVE
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (immersive) {
                            Modifier.background(immersiveGradientBrush())
                        } else {
                            Modifier
                        }
                    )
            ) {
                if (immersive) {
                    ImmersiveFeedbackLayer(isPlaying = isPlaying)
                }
                Scaffold(
                    containerColor = if (immersive) Color.Transparent else MaterialTheme.colorScheme.background,
                    bottomBar = {
                        if (showPlayerChrome) {
                            Column {
                                MiniPlayerRoute(
                                    playerViewModel = playerViewModel,
                                    onOpenNowPlaying = { navController.navigate(nowPlayingRoute) }
                                )
                                AppBottomNavigationBar(
                                    currentDestination = currentDestination,
                                    navController = navController
                                )
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
                                onPlaySong = playerViewModel::play,
                                onOpenArtist = { artistId -> navController.navigate("artist/$artistId") },
                                onOpenAlbum = { albumId -> navController.navigate("album/$albumId") }
                            )
                        }
                        composable(AppDestination.Library.route) {
                            LibraryRoute(
                                playerViewModel = playerViewModel,
                                onPlaySong = playerViewModel::play,
                                onOpenArtist = { artistId -> navController.navigate("artist/$artistId") },
                                onOpenAlbum = { albumId -> navController.navigate("album/$albumId") }
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
                        navigation(
                            startDestination = SETTINGS_MAIN_ROUTE,
                            route = AppDestination.Settings.route
                        ) {
                            composable(SETTINGS_MAIN_ROUTE) {
                                SettingsScreen(
                                    onNavigateToAppearance = { navController.navigate(SETTINGS_APPEARANCE_ROUTE) },
                                    onNavigateToLocalLibrary = { navController.navigate(SETTINGS_LOCAL_LIBRARY_ROUTE) },
                                    onNavigateToScraping = { navController.navigate(SETTINGS_SCRAPING_ROUTE) },
                                    onNavigateToCache = { navController.navigate(SETTINGS_CACHE_ROUTE) },
                                    onNavigateToDataManagement = { navController.navigate(SETTINGS_DATA_ROUTE) },
                                    onNavigateToWebDav = { navController.navigate(SETTINGS_WEBDAV_ROUTE) }
                                )
                            }
                            composable(SETTINGS_APPEARANCE_ROUTE) {
                                AppearanceSettingsScreen(
                                    onBack = navController::navigateUp,
                                    viewModel = settingsViewModel
                                )
                            }
                            composable(SETTINGS_LOCAL_LIBRARY_ROUTE) {
                                LocalLibrarySettingsScreen(
                                    onBack = navController::navigateUp,
                                    viewModel = settingsViewModel
                                )
                            }
                            composable(SETTINGS_SCRAPING_ROUTE) {
                                ScrapingSettingsScreen(
                                    onBack = navController::navigateUp,
                                    viewModel = settingsViewModel
                                )
                            }
                            composable(SETTINGS_CACHE_ROUTE) {
                                CacheSettingsScreen(
                                    onBack = navController::navigateUp,
                                    viewModel = settingsViewModel
                                )
                            }
                            composable(SETTINGS_DATA_ROUTE) {
                                DataManagementSettingsScreen(
                                    onBack = navController::navigateUp,
                                    viewModel = settingsViewModel
                                )
                            }
                            composable(SETTINGS_WEBDAV_ROUTE) {
                                WebDavSettingsScreen(
                                    onBack = navController::navigateUp,
                                    onManageWebDav = { navController.navigate(SETTINGS_WEBDAV_MANAGE_ROUTE) }
                                )
                            }
                            composable(SETTINGS_WEBDAV_MANAGE_ROUTE) {
                                WebDavScreen(
                                    onBack = navController::navigateUp,
                                    onBrowseServer = { serverId -> navController.navigate("settings/webdav-browse/$serverId") }
                                )
                            }
                            composable(SETTINGS_WEBDAV_BROWSE_ROUTE) { entry ->
                                WebDavBrowserScreen(
                                    serverId = entry.arguments?.getString("serverId").orEmpty(),
                                    onBack = navController::navigateUp
                                )
                            }
                        }
                        composable("scrape-detail") {
                            ScrapeDetailRoute(
                                playerViewModel = playerViewModel,
                                onBack = navController::navigateUp,
                            )
                        }
                        composable(EQUALIZER_ROUTE) {
                            EqualizerRoute(
                                playerViewModel = playerViewModel,
                                onBack = navController::navigateUp
                            )
                        }
                        composable(AppDestination.NowPlaying.route) {
                            NowPlayingRoute(
                                playerViewModel = playerViewModel,
                                onOpenScrapeDetail = { navController.navigate("scrape-detail") },
                                onOpenEqualizer = { navController.navigate(EQUALIZER_ROUTE) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniPlayerRoute(
    playerViewModel: PlayerViewModel,
    onOpenNowPlaying: () -> Unit
) {
    val playbackState by playerViewModel.playbackState.collectAsStateWithLifecycle()
    MiniPlayerBar(
        playbackState = playbackState,
        onClick = onOpenNowPlaying,
        onTogglePlayPause = playerViewModel::togglePlayPause,
        onSkipNext = playerViewModel::skipNext,
        onQueueClick = onOpenNowPlaying
    )
}

@Composable
private fun AppBottomNavigationBar(
    currentDestination: NavDestination?,
    navController: NavHostController
) {
    val strings = LocalStrings.current
    NavigationBar(
        modifier = Modifier.height(60.dp)
    ) {
        AppDestination.bottomItems.forEach { destination ->
            val selected = currentDestination?.hierarchy
                ?.any { it.route == destination.route } == true
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

@Composable
private fun LibraryRoute(
    playerViewModel: PlayerViewModel,
    onPlaySong: (com.musicplayer.app.domain.model.Song, List<com.musicplayer.app.domain.model.Song>) -> Unit,
    onOpenArtist: (String) -> Unit,
    onOpenAlbum: (String) -> Unit
) {
    val currentSongFlow = remember(playerViewModel) {
        playerViewModel.playbackState
            .map { it.currentSong }
            .distinctUntilChanged { old, new -> old?.id == new?.id }
    }
    val currentSong by currentSongFlow.collectAsStateWithLifecycle(
        initialValue = playerViewModel.playbackState.value.currentSong
    )
    LibraryScreen(
        onPlaySong = onPlaySong,
        onOpenArtist = onOpenArtist,
        onOpenAlbum = onOpenAlbum,
        currentSong = currentSong
    )
}

@Composable
private fun ScrapeDetailRoute(
    playerViewModel: PlayerViewModel,
    onBack: () -> Unit
) {
    val playbackState by playerViewModel.playbackState.collectAsStateWithLifecycle()
    val controlsState by playerViewModel.controlsState.collectAsStateWithLifecycle()
    val scrapeCandidates by playerViewModel.scrapeCandidates.collectAsStateWithLifecycle()
    val selectedScrapeCandidate by playerViewModel.selectedScrapeCandidate.collectAsStateWithLifecycle()
    ScrapeDetailScreen(
        playbackState = playbackState,
        controlsState = controlsState,
        scrapeCandidates = scrapeCandidates,
        selectedScrapeCandidate = selectedScrapeCandidate,
        onBack = onBack,
        onSearchKeywordChange = playerViewModel::setSearchKeyword,
        onSearchModeChange = playerViewModel::setScrapeSearchMode,
        onPrepareScrape = playerViewModel::prepareScrapeDetail,
        onScrapeCurrent = playerViewModel::scrapeCurrent,
        onAiScrapeCurrent = playerViewModel::aiScrapeCurrent,
        onAiGenerateCategory = playerViewModel::aiGenerateCategoryForCurrentSong,
        onSelectScrapeCandidate = playerViewModel::selectScrapeCandidate,
        onApplyScrapeSelection = playerViewModel::applyScrapeSelection
    )
}

@Composable
private fun NowPlayingRoute(
    playerViewModel: PlayerViewModel,
    onOpenScrapeDetail: () -> Unit,
    onOpenEqualizer: () -> Unit
) {
    val playbackState by playerViewModel.playbackState.collectAsStateWithLifecycle()
    val desktopLyricsEnabled by playerViewModel.desktopLyricsEnabled.collectAsStateWithLifecycle()
    val comments by playerViewModel.comments.collectAsStateWithLifecycle()
    val showCommentDialog by playerViewModel.showCommentDialog.collectAsStateWithLifecycle()
    val candidates by playerViewModel.candidates.collectAsStateWithLifecycle()
    val showCandidateDialog by playerViewModel.showCandidateDialog.collectAsStateWithLifecycle()
    NowPlayingScreen(
        playbackState = playbackState,
        onSkipPrevious = playerViewModel::skipPrevious,
        onTogglePlayPause = playerViewModel::togglePlayPause,
        onSkipNext = playerViewModel::skipNext,
        onSeekTo = playerViewModel::seekTo,
        onQueueSongSelected = playerViewModel::playQueueSong,
        onPlaybackModeChange = playerViewModel::setPlaybackMode,
        desktopLyricsEnabled = desktopLyricsEnabled,
        onToggleDesktopLyrics = playerViewModel::toggleDesktopLyrics,
        onOpenEqualizer = onOpenEqualizer,
        onOpenScrapeDetail = onOpenScrapeDetail,
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

@Composable
private fun EqualizerRoute(
    playerViewModel: PlayerViewModel,
    onBack: () -> Unit
) {
    val playbackState by playerViewModel.playbackState.collectAsStateWithLifecycle()
    val controlsState by playerViewModel.controlsState.collectAsStateWithLifecycle()
    EqualizerScreen(
        playbackState = playbackState,
        controlsState = controlsState,
        onBack = onBack,
        onPresetSelected = playerViewModel::selectPreset,
        onDraftNameChange = playerViewModel::setDraftName,
        onBandLevelChange = playerViewModel::setBandLevel,
        onAdaptiveEqualizerChange = playerViewModel::setAdaptiveEqualizerEnabled,
        onCategoryPresetChange = playerViewModel::setCategoryPreset,
        onCreatePreset = playerViewModel::createPreset,
        onSavePreset = playerViewModel::savePreset,
        onDeletePreset = playerViewModel::deletePreset
    )
}

@Composable
private fun immersiveGradientBrush(): Brush {
    val scheme = MaterialTheme.colorScheme
    return Brush.verticalGradient(
        colors = listOf(
            scheme.background,
            scheme.primaryContainer,
            scheme.secondaryContainer,
            scheme.tertiaryContainer,
            scheme.surface
        )
    )
}

@Composable
private fun ImmersiveFeedbackLayer(
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val progress = if (isPlaying) {
        val transition = rememberInfiniteTransition(label = "immersive-feedback")
        val animatedProgress by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1800, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "particle-progress"
        )
        animatedProgress
    } else {
        0f
    }
    Canvas(modifier = modifier.fillMaxSize()) {
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    scheme.primary.copy(alpha = if (isPlaying) 0.16f else 0.08f),
                    Color.Transparent,
                    scheme.tertiary.copy(alpha = if (isPlaying) 0.14f else 0.06f)
                ),
                start = Offset(0f, 0f),
                end = Offset(size.width, size.height)
            )
        )
        if (!isPlaying) return@Canvas
        val particleCount = 30
        repeat(particleCount) { index ->
            val baseX = ((index * 37) % 100) / 100f
            val phase = (progress + index / particleCount.toFloat()) % 1f
            val wave = sin((phase * 2f * PI + index).toFloat()) * 18.dp.toPx()
            val x = (baseX * size.width + wave).coerceIn(0f, size.width)
            val y = size.height * (0.9f - phase * 0.34f)
            val radius = (1.6f + (index % 4) * 0.45f).dp.toPx()
            val alpha = (1f - phase).coerceIn(0f, 1f) * 0.34f
            val color = when (index % 3) {
                0 -> scheme.primary
                1 -> scheme.secondary
                else -> scheme.tertiary
            }
            drawCircle(
                color = color.copy(alpha = alpha),
                radius = radius,
                center = Offset(x, y)
            )
        }
    }
}
