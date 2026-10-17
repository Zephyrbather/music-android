package com.musicplayer.app.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.PlaylistPlay
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import com.musicplayer.app.domain.repository.TabletLayoutMode
import com.musicplayer.app.domain.repository.ThemeStyle
import com.musicplayer.app.ui.adaptive.LocalPadLayout
import com.musicplayer.app.ui.adaptive.PadLayoutBreakpoint
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
import com.musicplayer.app.ui.settings.AiLabSettingsScreen
import com.musicplayer.app.ui.settings.CacheSettingsScreen
import com.musicplayer.app.ui.settings.DataManagementSettingsScreen
import com.musicplayer.app.ui.settings.LocalLibrarySettingsScreen
import com.musicplayer.app.ui.settings.MetadataHealthScreen
import com.musicplayer.app.ui.settings.ScrapingSettingsScreen
import com.musicplayer.app.ui.settings.SettingsScreen
import com.musicplayer.app.ui.settings.SettingsViewModel
import com.musicplayer.app.ui.settings.WebDavSettingsScreen
import com.musicplayer.app.ui.settings.WebDavBrowserScreen
import com.musicplayer.app.ui.settings.WebDavScreen
import com.musicplayer.app.ui.stats.ListeningStatsScreen
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
private const val SETTINGS_METADATA_HEALTH_ROUTE = "settings/metadata-health"
private const val SETTINGS_AI_LAB_ROUTE = "settings/ai-lab"
private const val SETTINGS_CACHE_ROUTE = "settings/cache"
private const val SETTINGS_DATA_ROUTE = "settings/data"
private const val SETTINGS_WEBDAV_ROUTE = "settings/webdav"
private const val SETTINGS_WEBDAV_MANAGE_ROUTE = "settings/webdav/manage"
private const val SETTINGS_WEBDAV_BROWSE_ROUTE = "settings/webdav-browse/{serverId}"
private const val EQUALIZER_ROUTE = "equalizer"
private const val LISTENING_STATS_ROUTE = "listening-stats"

@Composable
fun AppNavigation() {
    val settingsViewModel: SettingsViewModel = hiltViewModel()
    val settings by settingsViewModel.uiState.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    var showNowPlaying by rememberSaveable { mutableStateOf(false) }
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
    val showPlayerChrome = !showNowPlaying

    BackHandler(enabled = showNowPlaying) { showNowPlaying = false }

    MusicPlayerTheme(
        themeMode = settings.themeMode,
        themeStyle = settings.themeStyle,
        dynamicColor = settings.dynamicColor,
        currentArtworkUri = currentArtworkUri,
        fontFamilyOption = settings.fontFamilyOption
    ) {
        ProvideLocalizedStrings(settings.appLanguage) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val isPadLayout = when (settings.tabletLayoutMode) {
                    TabletLayoutMode.AUTO -> maxWidth >= PadLayoutBreakpoint
                    TabletLayoutMode.ENABLED -> true
                    TabletLayoutMode.DISABLED -> false
                }
                CompositionLocalProvider(LocalPadLayout provides isPadLayout) {
            val immersive = settings.themeStyle == ThemeStyle.IMMERSIVE
            val neonStyle = settings.themeStyle == ThemeStyle.CYBERPUNK ||
                settings.themeStyle == ThemeStyle.PANDA ||
                settings.themeStyle == ThemeStyle.SAKURA ||
                settings.themeStyle == ThemeStyle.AURORA ||
                settings.themeStyle == ThemeStyle.SUNSET ||
                settings.themeStyle == ThemeStyle.LAVA
            val dynamicBackdrop = immersive || neonStyle
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        when {
                            immersive -> Modifier.background(immersiveGradientBrush())
                            neonStyle -> Modifier.background(themeBackdropBrush(settings.themeStyle))
                            else -> Modifier
                        }
                    )
            ) {
                if (immersive) {
                    ImmersiveFeedbackLayer(isPlaying = isPlaying)
                } else if (settings.themeStyle == ThemeStyle.CYBERPUNK) {
                    CyberpunkBackdrop(isPlaying = isPlaying)
                } else if (settings.themeStyle == ThemeStyle.PANDA) {
                    PandaBackdrop(isPlaying = isPlaying)
                } else if (settings.themeStyle == ThemeStyle.SAKURA) {
                    SakuraBackdrop(isPlaying = isPlaying)
                } else if (settings.themeStyle == ThemeStyle.AURORA) {
                    AuroraBackdrop(isPlaying = isPlaying)
                } else if (settings.themeStyle == ThemeStyle.SUNSET) {
                    SunsetBackdrop(isPlaying = isPlaying)
                } else if (settings.themeStyle == ThemeStyle.LAVA) {
                    LavaBackdrop(isPlaying = isPlaying)
                }
                val navHostContent: @Composable (Modifier) -> Unit = { hostModifier ->
                    NavHost(
                        navController = navController,
                        startDestination = AppDestination.Home.route,
                        modifier = hostModifier
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
                                onOpenPlaylist = { playlistId -> navController.navigate("playlist/$playlistId") },
                                onPlaySong = playerViewModel::play,
                                onOpenListeningStats = { navController.navigate(LISTENING_STATS_ROUTE) }
                            )
                        }
                        composable(LISTENING_STATS_ROUTE) {
                            ListeningStatsScreen(onBack = navController::navigateUp)
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
                                    onNavigateToMetadataHealth = { navController.navigate(SETTINGS_METADATA_HEALTH_ROUTE) },
                                    onNavigateToAiLab = { navController.navigate(SETTINGS_AI_LAB_ROUTE) },
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
                            composable(SETTINGS_METADATA_HEALTH_ROUTE) {
                                MetadataHealthScreen(onBack = navController::navigateUp)
                            }
                            composable(SETTINGS_AI_LAB_ROUTE) {
                                AiLabSettingsScreen(
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
                    }
                }

                if (isPadLayout) {
                    Scaffold(
                        containerColor = if (dynamicBackdrop) Color.Transparent else MaterialTheme.colorScheme.background,
                        contentColor = MaterialTheme.colorScheme.onBackground
                    ) { innerPadding ->
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            PadNavigationRail(
                                currentDestination = currentDestination,
                                navController = navController,
                                logoUri = settings.customLogoUri,
                                onNavigate = { showNowPlaying = false }
                            )
                            Box(modifier = Modifier.weight(1f)) {
                                Column(modifier = Modifier.fillMaxSize()) {
                                    navHostContent(Modifier.weight(1f))
                                    if (showPlayerChrome) {
                                        MiniPlayerRoute(
                                            playerViewModel = playerViewModel,
                                            onOpenNowPlaying = { showNowPlaying = true },
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                            shape = RoundedCornerShape(18.dp)
                                        )
                                    }
                                }
                                if (showNowPlaying) {
                                    Surface(
                                        modifier = Modifier.fillMaxSize(),
                                        color = MaterialTheme.colorScheme.background
                                    ) {
                                        NowPlayingRoute(
                                            playerViewModel = playerViewModel,
                                            onClose = { showNowPlaying = false },
                                            onOpenScrapeDetail = {
                                                showNowPlaying = false
                                                navController.navigate("scrape-detail")
                                            },
                                            onOpenEqualizer = {
                                                showNowPlaying = false
                                                navController.navigate(EQUALIZER_ROUTE)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Scaffold(
                        containerColor = if (dynamicBackdrop) Color.Transparent else MaterialTheme.colorScheme.background,
                        contentColor = MaterialTheme.colorScheme.onBackground,
                        bottomBar = {
                            if (showPlayerChrome) {
                                Column {
                                    MiniPlayerRoute(
                                        playerViewModel = playerViewModel,
                                        onOpenNowPlaying = { showNowPlaying = true }
                                    )
                                    AppBottomNavigationBar(
                                        currentDestination = currentDestination,
                                        navController = navController,
                                        onNavigate = { showNowPlaying = false }
                                    )
                                }
                            }
                        }
                    ) { innerPadding ->
                        Box(modifier = Modifier.fillMaxSize()) {
                            navHostContent(
                                if (showPlayerChrome) Modifier.padding(innerPadding) else Modifier
                            )
                            if (showNowPlaying) {
                                Surface(
                                    modifier = Modifier.fillMaxSize(),
                                    color = MaterialTheme.colorScheme.background
                                ) {
                                    NowPlayingRoute(
                                        playerViewModel = playerViewModel,
                                        onClose = { showNowPlaying = false },
                                        onOpenScrapeDetail = {
                                            showNowPlaying = false
                                            navController.navigate("scrape-detail")
                                        },
                                        onOpenEqualizer = {
                                            showNowPlaying = false
                                            navController.navigate(EQUALIZER_ROUTE)
                                        }
                                    )
                                }
                            }
                        }
                    }
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
    onOpenNowPlaying: () -> Unit,
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape = androidx.compose.ui.graphics.RectangleShape
) {
    val playbackState by playerViewModel.playbackState.collectAsStateWithLifecycle()
    val lyricOffsetMs by playerViewModel.lyricOffsetMs.collectAsStateWithLifecycle()
    MiniPlayerBar(
        playbackState = playbackState,
        lyricOffsetMs = lyricOffsetMs,
        onClick = onOpenNowPlaying,
        onTogglePlayPause = playerViewModel::togglePlayPause,
        onSkipNext = playerViewModel::skipNext,
        onQueueClick = onOpenNowPlaying,
        modifier = modifier,
        shape = shape
    )
}

@Composable
private fun PadNavigationRail(
    currentDestination: NavDestination?,
    navController: NavHostController,
    logoUri: String,
    onNavigate: () -> Unit
) {
    val strings = LocalStrings.current
    NavigationRail(
        modifier = Modifier.fillMaxHeight(),
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        windowInsets = WindowInsets(0, 0, 0, 0),
        header = {
            Surface(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 4.dp)
                    .size(48.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Box(contentAlignment = androidx.compose.ui.Alignment.Center) {
                    if (logoUri.isNotBlank()) {
                        AsyncImage(
                            model = logoUri,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.MusicNote,
                            contentDescription = null,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }
        }
    ) {
        Spacer(modifier = Modifier.height(8.dp))
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
            NavigationRailItem(
                selected = selected,
                onClick = {
                    onNavigate()
                    navController.navigate(destination.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = {
                    Icon(imageVector = destination.icon, contentDescription = label)
                },
                label = { Text(label) }
            )
        }
        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun AppBottomNavigationBar(
    currentDestination: NavDestination?,
    navController: NavHostController,
    onNavigate: () -> Unit
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
                    onNavigate()
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
    onClose: () -> Unit,
    onOpenScrapeDetail: () -> Unit,
    onOpenEqualizer: () -> Unit
) {
    val playbackState by playerViewModel.playbackState.collectAsStateWithLifecycle()
    val desktopLyricsEnabled by playerViewModel.desktopLyricsEnabled.collectAsStateWithLifecycle()
    val lyricOffsetMs by playerViewModel.lyricOffsetMs.collectAsStateWithLifecycle()
    val lyricFontScale by playerViewModel.lyricFontScale.collectAsStateWithLifecycle()
    val comments by playerViewModel.comments.collectAsStateWithLifecycle()
    val showCommentDialog by playerViewModel.showCommentDialog.collectAsStateWithLifecycle()
    val candidates by playerViewModel.candidates.collectAsStateWithLifecycle()
    val showCandidateDialog by playerViewModel.showCandidateDialog.collectAsStateWithLifecycle()
    NowPlayingScreen(
        playbackState = playbackState,
        onClose = onClose,
        onSkipPrevious = playerViewModel::skipPrevious,
        onTogglePlayPause = playerViewModel::togglePlayPause,
        onSkipNext = playerViewModel::skipNext,
        onSeekTo = playerViewModel::seekTo,
        onQueueSongSelected = playerViewModel::playQueueSong,
        onPlaybackModeChange = playerViewModel::setPlaybackMode,
        desktopLyricsEnabled = desktopLyricsEnabled,
        onToggleDesktopLyrics = playerViewModel::toggleDesktopLyrics,
        lyricOffsetMs = lyricOffsetMs,
        onAdjustLyricOffset = playerViewModel::adjustLyricOffset,
        onResetLyricOffset = playerViewModel::resetLyricOffset,
        lyricFontScale = lyricFontScale,
        onAdjustLyricFontScale = playerViewModel::adjustLyricFontScale,
        onResetLyricFontScale = playerViewModel::resetLyricFontScale,
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
private fun themeBackdropBrush(style: ThemeStyle): Brush {
    val scheme = MaterialTheme.colorScheme
    return when (style) {
        ThemeStyle.CYBERPUNK -> Brush.verticalGradient(
            listOf(
                scheme.background,
                Color(0xFF1A0A2A),
                scheme.background
            )
        )
        ThemeStyle.PANDA -> Brush.verticalGradient(
            listOf(
                scheme.background,
                scheme.surfaceVariant.copy(alpha = 0.8f),
                scheme.background
            )
        )
        ThemeStyle.SAKURA -> Brush.verticalGradient(
            listOf(
                scheme.background,
                scheme.primaryContainer.copy(alpha = 0.6f),
                scheme.secondaryContainer.copy(alpha = 0.4f),
                scheme.background
            )
        )
        ThemeStyle.AURORA -> Brush.verticalGradient(
            listOf(
                scheme.background,
                Color(0xFF0A2A33),
                Color(0xFF101A3A),
                scheme.background
            )
        )
        ThemeStyle.SUNSET -> Brush.verticalGradient(
            listOf(
                Color(0xFF150A10),
                Color(0xFF3A1524),
                Color(0xFF4A2410),
                scheme.background
            )
        )
        ThemeStyle.LAVA -> Brush.verticalGradient(
            listOf(
                scheme.background,
                Color(0xFF33110A),
                Color(0xFF3A1E00),
                scheme.background
            )
        )
        else -> Brush.verticalGradient(listOf(scheme.background, scheme.background))
    }
}

@Composable
private fun CyberpunkBackdrop(
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val transition = rememberInfiniteTransition(label = "cyberpunk")
    val scan by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(7000, easing = LinearEasing), RepeatMode.Restart),
        label = "cyber-scan"
    )
    val pulse by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2200, easing = LinearEasing), RepeatMode.Reverse),
        label = "cyber-pulse"
    )
    Canvas(modifier = modifier.fillMaxSize()) {
        drawCircle(
            brush = Brush.radialGradient(
                listOf(scheme.primary.copy(alpha = 0.16f), Color.Transparent),
                center = Offset(size.width * 0.86f, size.height * 0.08f),
                radius = size.width * 0.42f
            ),
            radius = size.width * 0.42f,
            center = Offset(size.width * 0.86f, size.height * 0.08f)
        )
        drawCircle(
            brush = Brush.radialGradient(
                listOf(scheme.secondary.copy(alpha = 0.10f), Color.Transparent),
                center = Offset(size.width * 0.1f, size.height * 0.62f),
                radius = size.width * 0.34f
            ),
            radius = size.width * 0.34f,
            center = Offset(size.width * 0.1f, size.height * 0.62f)
        )
        val horizon = size.height * 0.66f
        for (index in 0..10) {
            val bottomX = size.width * index / 10f
            drawLine(
                color = scheme.secondary.copy(alpha = 0.09f),
                start = Offset(size.width / 2f + (bottomX - size.width / 2f) * 0.22f, horizon),
                end = Offset(bottomX, size.height),
                strokeWidth = 1f
            )
        }
        for (index in 1..8) {
            val fraction = index / 8f
            val y = horizon + (size.height - horizon) * fraction * fraction
            drawLine(
                color = scheme.primary.copy(alpha = 0.08f),
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1f
            )
        }
        val scanY = size.height * scan
        drawRect(
            brush = Brush.verticalGradient(
                listOf(
                    Color.Transparent,
                    scheme.primary.copy(alpha = 0.10f * pulse),
                    Color.Transparent
                ),
                startY = scanY - 90f,
                endY = scanY + 90f
            ),
            topLeft = Offset(0f, scanY - 90f),
            size = Size(size.width, 180f)
        )
        drawLine(
            color = scheme.secondary.copy(alpha = (if (isPlaying) 0.45f else 0.22f) * pulse),
            start = Offset(0f, size.height * 0.1f),
            end = Offset(size.width * 0.16f, size.height * 0.1f),
            strokeWidth = 2f
        )
        drawLine(
            color = scheme.primary.copy(alpha = (if (isPlaying) 0.45f else 0.22f) * pulse),
            start = Offset(size.width * 0.84f, size.height * 0.84f),
            end = Offset(size.width, size.height * 0.84f),
            strokeWidth = 2f
        )
    }
}

@Composable
private fun PandaBackdrop(
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val transition = rememberInfiniteTransition(label = "panda")
    val drift by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(24000, easing = LinearEasing), RepeatMode.Restart),
        label = "panda-drift"
    )
    val inkAlpha = if (isPlaying) 0.07f else 0.045f
    Canvas(modifier = modifier.fillMaxSize()) {
        drawCircle(
            color = scheme.onSurface.copy(alpha = inkAlpha),
            radius = size.width * 0.30f,
            center = Offset(
                size.width * (0.22f + 0.04f * sin(drift)),
                size.height * (0.28f + 0.03f * sin(drift * 0.7f))
            )
        )
        drawCircle(
            color = scheme.onSurface.copy(alpha = inkAlpha * 0.85f),
            radius = size.width * 0.22f,
            center = Offset(
                size.width * (0.82f + 0.05f * sin(drift * 0.8f + 1.6f)),
                size.height * (0.70f + 0.04f * sin(drift * 0.6f))
            )
        )
        drawCircle(
            color = scheme.tertiary.copy(alpha = 0.10f),
            radius = size.width * 0.12f,
            center = Offset(
                size.width * (0.55f + 0.06f * sin(drift * 1.2f + 2.4f)),
                size.height * (0.86f + 0.02f * sin(drift))
            )
        )
        repeat(9) { index ->
            val x = size.width * ((index * 23) % 100) / 100f + sin(drift + index) * 10.dp.toPx()
            val y = size.height * (0.2f + ((index * 31) % 60) / 100f)
            drawCircle(
                color = scheme.tertiary.copy(alpha = 0.10f),
                radius = (2.2f + index % 3).dp.toPx(),
                center = Offset(x, y)
            )
        }
    }
}

@Composable
private fun SakuraBackdrop(
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val transition = rememberInfiniteTransition(label = "sakura")
    val fall by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(16000, easing = LinearEasing), RepeatMode.Restart),
        label = "sakura-fall"
    )
    Canvas(modifier = modifier.fillMaxSize()) {
        drawCircle(
            brush = Brush.radialGradient(
                listOf(scheme.primary.copy(alpha = 0.10f), Color.Transparent),
                center = Offset(size.width * 0.15f, size.height * 0.08f),
                radius = size.width * 0.32f
            ),
            radius = size.width * 0.32f,
            center = Offset(size.width * 0.15f, size.height * 0.08f)
        )
        repeat(34) { index ->
            val phase = (fall + index / 34f) % 1f
            val sway = sin((phase * 3f + index * 0.7f) * PI).toFloat() * 26.dp.toPx()
            val x = size.width * ((index * 37) % 100) / 100f + sway
            val y = phase * (size.height + 60.dp.toPx()) - 30.dp.toPx()
            val color = if (index % 3 == 0) scheme.secondary else scheme.primary
            val alpha = (if (isPlaying) 0.24f else 0.16f) * (1f - phase * 0.5f)
            drawCircle(
                color = color.copy(alpha = alpha),
                radius = (1.8f + (index % 4) * 0.6f).dp.toPx(),
                center = Offset(x, y)
            )
        }
    }
}

@Composable
private fun AuroraBackdrop(
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val transition = rememberInfiniteTransition(label = "aurora")
    val drift by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(28000, easing = LinearEasing), RepeatMode.Restart),
        label = "aurora-drift"
    )
    val pulse by transition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3600, easing = LinearEasing), RepeatMode.Reverse),
        label = "aurora-pulse"
    )
    val ribbons = listOf(
        scheme.primary,
        scheme.secondary,
        scheme.tertiary,
        scheme.primary
    )
    Canvas(modifier = modifier.fillMaxSize()) {
        drawCircle(
            brush = Brush.radialGradient(
                listOf(scheme.primary.copy(alpha = 0.14f * pulse), Color.Transparent),
                center = Offset(size.width * 0.2f, size.height * 0.12f),
                radius = size.width * 0.5f
            ),
            radius = size.width * 0.5f,
            center = Offset(size.width * 0.2f, size.height * 0.12f)
        )
        drawCircle(
            brush = Brush.radialGradient(
                listOf(scheme.secondary.copy(alpha = 0.12f), Color.Transparent),
                center = Offset(size.width * 0.85f, size.height * 0.42f),
                radius = size.width * 0.44f
            ),
            radius = size.width * 0.44f,
            center = Offset(size.width * 0.85f, size.height * 0.42f)
        )
        ribbons.forEachIndexed { band, color ->
            val baseY = size.height * (0.16f + band * 0.12f)
            val path = Path()
            var x = -60f
            path.moveTo(x, baseY)
            while (x <= size.width + 60f) {
                val wave = sin((x / size.width * 2f * PI + drift + band * 0.9f).toFloat()) * 46.dp.toPx() +
                    sin((x / size.width * 4.4f * PI - drift * 1.4f + band * 2.1f).toFloat()) * 20.dp.toPx()
                path.lineTo(x, baseY + wave)
                x += 28f
            }
            val alpha = (if (isPlaying) 0.26f else 0.16f) * pulse
            drawPath(
                path = path,
                brush = Brush.verticalGradient(
                    listOf(Color.Transparent, color.copy(alpha = alpha), Color.Transparent),
                    startY = baseY - 130.dp.toPx(),
                    endY = baseY + 130.dp.toPx()
                ),
                style = Stroke(width = 52.dp.toPx())
            )
        }
        repeat(30) { index ->
            val twinkle = 0.12f + 0.18f * (((index * 7) % 10) / 10f) * pulse
            drawCircle(
                color = Color.White.copy(alpha = twinkle),
                radius = (0.9f + index % 3 * 0.5f).dp.toPx(),
                center = Offset(
                    size.width * ((index * 37) % 100) / 100f,
                    size.height * 0.55f * ((index * 53) % 100) / 100f
                )
            )
        }
    }
}

@Composable
private fun SunsetBackdrop(
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val transition = rememberInfiniteTransition(label = "sunset")
    val drift by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(30000, easing = LinearEasing), RepeatMode.Restart),
        label = "sunset-drift"
    )
    val glow by transition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(4200, easing = LinearEasing), RepeatMode.Reverse),
        label = "sunset-glow"
    )
    Canvas(modifier = modifier.fillMaxSize()) {
        val sunCenter = Offset(size.width * 0.5f, size.height * 0.86f)
        val sunRadius = size.width * 0.34f
        drawCircle(
            brush = Brush.radialGradient(
                listOf(
                    scheme.tertiary.copy(alpha = 0.32f * glow),
                    scheme.primary.copy(alpha = 0.18f * glow),
                    Color.Transparent
                ),
                center = sunCenter,
                radius = sunRadius
            ),
            radius = sunRadius,
            center = sunCenter
        )
        drawCircle(
            color = scheme.primary.copy(alpha = 0.26f * glow),
            radius = size.width * 0.13f,
            center = Offset(size.width * 0.5f, size.height * 0.7f)
        )
        repeat(5) { band ->
            val y = size.height * (0.58f + band * 0.055f)
            val offset = sin((drift + band * 0.8f)) * 24.dp.toPx()
            drawRect(
                brush = Brush.verticalGradient(
                    listOf(
                        Color.Transparent,
                        if (band % 2 == 0) scheme.secondary.copy(alpha = 0.12f)
                        else scheme.primary.copy(alpha = 0.10f),
                        Color.Transparent
                    ),
                    startY = y - 26.dp.toPx(),
                    endY = y + 26.dp.toPx()
                ),
                topLeft = Offset(offset, y - 26.dp.toPx()),
                size = Size(size.width, 52.dp.toPx())
            )
        }
        repeat(4) { index ->
            val phase = (drift + index * 1.6f) / (2f * PI).toFloat()
            val x = size.width * ((index * 29 + 8) % 100) / 100f + phase * 40.dp.toPx()
            val y = size.height * (0.16f + index * 0.11f)
            drawOval(
                brush = Brush.radialGradient(
                    listOf(
                        scheme.secondary.copy(alpha = 0.10f),
                        scheme.secondary.copy(alpha = 0.04f),
                        Color.Transparent
                    ),
                    center = Offset(x, y),
                    radius = 150.dp.toPx()
                ),
                topLeft = Offset(x - 170.dp.toPx(), y - 34.dp.toPx()),
                size = Size(340.dp.toPx(), 68.dp.toPx())
            )
        }
    }
}

@Composable
private fun LavaBackdrop(
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val transition = rememberInfiniteTransition(label = "lava")
    val rise by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(9000, easing = LinearEasing), RepeatMode.Restart),
        label = "lava-rise"
    )
    val glow by transition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2600, easing = LinearEasing), RepeatMode.Reverse),
        label = "lava-glow"
    )
    Canvas(modifier = modifier.fillMaxSize()) {
        drawCircle(
            brush = Brush.radialGradient(
                listOf(
                    scheme.primary.copy(alpha = 0.22f * glow),
                    scheme.secondary.copy(alpha = 0.10f),
                    Color.Transparent
                ),
                center = Offset(size.width * 0.5f, size.height * 1.02f),
                radius = size.width * 0.6f
            ),
            radius = size.width * 0.6f,
            center = Offset(size.width * 0.5f, size.height * 1.02f)
        )
        repeat(5) { crack ->
            val baseY = size.height * (0.94f + crack * 0.012f)
            drawLine(
                color = scheme.primary.copy(alpha = 0.16f * glow),
                start = Offset(size.width * crack / 5f, baseY),
                end = Offset(size.width * (crack + 1) / 5f, size.height),
                strokeWidth = 1.5f
            )
        }
        repeat(44) { index ->
            val phase = (rise + index / 44f) % 1f
            val sway = sin((phase * 4f * PI + index * 0.9f).toFloat()) * 22.dp.toPx()
            val x = size.width * ((index * 41) % 100) / 100f + sway
            val y = size.height * (1.02f - phase * 0.92f)
            val color = when (index % 3) {
                0 -> scheme.primary
                1 -> scheme.secondary
                else -> scheme.tertiary
            }
            val alpha = (if (isPlaying) 0.55f else 0.36f) * (1f - phase * 0.75f) * glow
            drawCircle(
                color = color.copy(alpha = alpha),
                radius = (1.4f + index % 4 * 0.7f).dp.toPx(),
                center = Offset(x, y)
            )
        }
    }
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
