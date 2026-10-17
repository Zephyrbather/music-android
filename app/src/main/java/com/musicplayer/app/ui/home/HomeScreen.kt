package com.musicplayer.app.ui.home

import android.os.Environment
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Album
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.FormatQuote
import androidx.compose.material.icons.outlined.Label
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.PlaylistAdd
import androidx.compose.material.icons.outlined.QueueMusic
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.RemoveCircleOutline
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.musicplayer.app.domain.model.Playlist
import com.musicplayer.app.domain.model.Song
import com.musicplayer.app.domain.model.Source
import com.musicplayer.app.ui.adaptive.LocalPadLayout
import com.musicplayer.app.ui.i18n.LocalExtraStrings
import com.musicplayer.app.ui.i18n.LocalStrings
import com.musicplayer.app.ui.components.AlbumCard
import com.musicplayer.app.ui.components.EditSongDialog
import com.musicplayer.app.ui.components.SongActionHeader
import com.musicplayer.app.ui.components.SongListItem
import com.musicplayer.app.ui.components.SongTagsDialog
import com.musicplayer.app.ui.components.collectSongTags
import com.musicplayer.app.domain.usecase.LibraryWorkType
import com.musicplayer.app.ui.library.DownloadProgress
import java.io.File
import kotlin.math.abs
import kotlin.random.Random

@Composable
fun HomeScreen(
    onPlaySong: (Song, List<Song>) -> Unit,
    onOpenArtist: (String) -> Unit = {},
    onOpenAlbum: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val workState by viewModel.workState.collectAsStateWithLifecycle()
    val featuredSong by viewModel.featuredSong.collectAsStateWithLifecycle()
    val actionSong by viewModel.actionSong.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val editSong by viewModel.editSong.collectAsStateWithLifecycle()
    val downloadProgress by viewModel.downloadProgress.collectAsStateWithLifecycle()
    val strings = LocalStrings.current
    val context = LocalContext.current
    val downloadDir = remember {
        File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), "WebDAV")
    }
    var removeSong by remember { mutableStateOf<Song?>(null) }
    var deleteFileSong by remember { mutableStateOf<Song?>(null) }
    var tagSong by remember { mutableStateOf<Song?>(null) }
    val allTags = remember(state.songs) { collectSongTags(state.songs) }
    val isPadLayout = LocalPadLayout.current
    val heroHeight = if (isPadLayout) 360.dp else 320.dp

    LaunchedEffect(Unit) {
        viewModel.refreshFeatured()
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(if (isPadLayout) 2 else 1),
        modifier = modifier,
        contentPadding = PaddingValues(
            start = if (isPadLayout) 28.dp else 20.dp,
            end = if (isPadLayout) 28.dp else 20.dp,
            top = 8.dp,
            bottom = 24.dp
        ),
        horizontalArrangement = Arrangement.spacedBy(if (isPadLayout) 20.dp else 0.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            HomeHero(
                featuredSong = featuredSong,
                songs = state.songs,
                onPlaySong = onPlaySong,
                heroHeight = heroHeight,
                isPadLayout = isPadLayout
            )
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            SectionTitle(
                title = strings.recentlyAdded,
                action = {
                    Button(
                        onClick = viewModel::scan,
                        enabled = !workState.running
                    ) {
                        Icon(imageVector = Icons.Outlined.Refresh, contentDescription = null)
                        Text(text = strings.scan)
                    }
                }
            )
        }
        if (workState.type == LibraryWorkType.SCAN && (workState.running || workState.lastResult != null)) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                ScanStatus(workState = workState)
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            SectionTitle(title = strings.recommendedSongs)
        }
        items(
            items = state.recommendedSongs,
            key = { "recommended-${it.id}" },
            contentType = { "home-song" }
        ) { song ->
            SongListItem(
                song = song,
                onClick = { viewModel.selectSong(song.id) },
                onDoubleClick = { onPlaySong(song, state.songs) },
                onMoreClick = { viewModel.showSongActions(song) }
            )
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            SectionTitle(title = strings.recentlyAdded)
        }
        items(
            items = state.recentSongs,
            key = { it.id },
            contentType = { "home-song" }
        ) { song ->
            SongListItem(
                song = song,
                onClick = { viewModel.selectSong(song.id) },
                onDoubleClick = { onPlaySong(song, state.songs) },
                onMoreClick = { viewModel.showSongActions(song) }
            )
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            SectionTitle(title = strings.albums)
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(
                    items = state.albums,
                    key = { it.id },
                    contentType = { "home-album" }
                ) { album ->
                    AlbumCard(
                        album = album,
                        onClick = {},
                        modifier = Modifier.fillParentMaxWidth(
                            if (isPadLayout) 0.24f else 0.46f
                        )
                    )
                }
            }
        }
    }

    actionSong?.let { song ->
        SongActionSheet(
            song = song,
            playlists = playlists,
            onDismiss = { viewModel.clearActionSong() },
            onPlayNext = {
                viewModel.playNext(song)
                viewModel.clearActionSong()
            },
            onCreatePlaylist = {
                viewModel.createPlaylistWith(song)
                viewModel.clearActionSong()
            },
            onAddToPlaylist = { playlistId ->
                viewModel.addToPlaylist(playlistId, song)
                viewModel.clearActionSong()
            },
            onToggleFavorite = {
                viewModel.toggleFavorite(song)
                viewModel.clearActionSong()
            },
            onShare = {
                shareSong(context, song)
                viewModel.clearActionSong()
            },
            onEditInfo = {
                viewModel.showEditDialog(song)
            },
            onEditTags = {
                viewModel.clearActionSong()
                tagSong = song
            },
            onRemoveFromLibrary = {
                viewModel.clearActionSong()
                removeSong = song
            },
            onDeleteFile = {
                viewModel.clearActionSong()
                deleteFileSong = song
            },
            onOpenArtist = {
                song.artistId?.let(onOpenArtist)
                viewModel.clearActionSong()
            },
            onOpenAlbum = {
                song.albumId?.let(onOpenAlbum)
                viewModel.clearActionSong()
            },
            onDownload = {
                viewModel.downloadSong(song, downloadDir)
                viewModel.clearActionSong()
            },
            isDownloading = song.id in downloadProgress.downloadingIds,
            downloadProgress = downloadProgress
        )
    }

    editSong?.let { song ->
        EditSongDialog(
            song = song,
            onSave = { title, artist, album, artworkUri, artworkProvider ->
                viewModel.updateSongMetadata(song.id, title, artist, album, artworkUri, artworkProvider)
                viewModel.dismissEditDialog()
            },
            onDismiss = { viewModel.dismissEditDialog() }
        )
    }

    tagSong?.let { song ->
        SongTagsDialog(
            song = song,
            allTags = allTags,
            onSave = { tags ->
                viewModel.updateSongTags(song.id, tags)
                tagSong = null
            },
            onGenerateWithAi = viewModel::generateSongTags,
            onDismiss = { tagSong = null }
        )
    }

    removeSong?.let { song ->
        ConfirmSongActionDialog(
            title = strings.removeFromLibrary,
            body = strings.removeFromLibraryConfirm.format(song.title),
            confirmText = strings.removeFromLibrary,
            onConfirm = {
                viewModel.removeFromLibrary(song)
                removeSong = null
            },
            onDismiss = { removeSong = null }
        )
    }

    deleteFileSong?.let { song ->
        ConfirmSongActionDialog(
            title = strings.deleteFromFile,
            body = strings.deleteFromFileConfirm.format(song.title),
            confirmText = strings.deleteFromFile,
            onConfirm = {
                viewModel.deleteLocalFile(song)
                deleteFileSong = null
            },
            onDismiss = { deleteFileSong = null }
        )
    }
}

@Composable
private fun ScanStatus(workState: com.musicplayer.app.domain.usecase.LibraryWorkState) {
    val strings = LocalStrings.current
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (workState.running) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Text(
                text = strings.scanRunning,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Text(
                text = "${strings.scanFinishedPrefix}${workState.discovered}${strings.scanFinishedSuffix}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private val heroLrcTimestamp = Regex("""\[\d{1,2}:\d{2}(?:[.:]\d{1,3})?]""")
private val heroLrcMetadata =
    Regex("""^\[(ar|ti|al|by|offset|re|ve|length|kana|language):.*]$""", RegexOption.IGNORE_CASE)
private val heroLyricCredits = listOf(
    "作词", "作曲", "编曲", "制作人", "混音", "母带", "录音", "出品", "监制", "和声",
    "Lyricist", "Composer", "Arranger", "Producer", "Mixed by", "Mastered by"
)

private fun heroLyricSnippet(song: Song, count: Int): List<String> {
    val raw = song.lyrics?.takeIf { it.isNotBlank() } ?: return emptyList()
    val lines = raw.lineSequence()
        .map { it.replace(heroLrcTimestamp, "").trim() }
        .filter { it.isNotEmpty() && !heroLrcMetadata.matches(it) }
        .filterNot { line -> heroLyricCredits.any { line.startsWith(it) } }
        .toList()
    if (lines.isEmpty()) return emptyList()
    val windowStart = Math.floorMod(song.id.hashCode(), maxOf(1, lines.size - count + 1))
    return lines.drop(windowStart).take(count)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HomeHero(
    featuredSong: Song?,
    songs: List<Song>,
    onPlaySong: (Song, List<Song>) -> Unit,
    heroHeight: Dp = 280.dp,
    isPadLayout: Boolean = false
) {
    val strings = LocalStrings.current
    val heroSongs = remember(featuredSong, songs) { buildHeroSongs(featuredSong, songs) }
    val shape = RoundedCornerShape(28.dp)
    val pagerState = rememberPagerState(pageCount = { heroSongs.size })

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(heroHeight)
            .clip(shape)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
                    )
                )
            )
    ) {
        if (heroSongs.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Album,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.4f)
                )
            }
        } else {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val song = heroSongs[page]
                val lyricLines = remember(song.id, song.lyrics) { heroLyricSnippet(song, 3) }
                val slide = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(if (isPadLayout) 32.dp else 20.dp)
                        .graphicsLayer {
                            translationX = slide * 90f
                            alpha = 1f - (abs(slide) * 0.35f).coerceIn(0f, 0.6f)
                        },
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1.25f)) {
                        Text(
                            text = strings.heroPicks,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = song.title,
                            style = if (isPadLayout) MaterialTheme.typography.headlineMedium
                            else MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "${song.artistName} · ${song.albumTitle}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (lyricLines.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Row(verticalAlignment = Alignment.Top) {
                                Icon(
                                    imageVector = Icons.Outlined.FormatQuote,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                    modifier = Modifier
                                        .padding(top = 2.dp)
                                        .size(if (isPadLayout) 20.dp else 16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    lyricLines.forEach { line ->
                                        Text(
                                            text = line,
                                            style = if (isPadLayout) MaterialTheme.typography.bodyLarge
                                            else MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.82f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { onPlaySong(song, songs) }) {
                            Icon(imageVector = Icons.Outlined.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = strings.playFeature)
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        HeroPagerDots(count = heroSongs.size, currentPage = pagerState.currentPage)
                    }
                    Spacer(modifier = Modifier.width(if (isPadLayout) 20.dp else 12.dp))
                    CoverCollage(
                        songs = heroSongs,
                        startIndex = page,
                        modifier = Modifier
                            .weight(if (isPadLayout) 1.5f else 1.2f)
                            .fillMaxHeight()
                    )
                }
            }
        }
    }
}

private data class CoverFanSpec(
    val rotation: Float,
    val xFraction: Float,
    val yFraction: Float,
    val scale: Float,
    val alpha: Float
)

private val coverFanSpecs = listOf(
    CoverFanSpec(rotation = -22f, xFraction = -0.92f, yFraction = -0.06f, scale = 0.60f, alpha = 0.34f),
    CoverFanSpec(rotation = 20f, xFraction = 0.86f, yFraction = -0.14f, scale = 0.64f, alpha = 0.42f),
    CoverFanSpec(rotation = -16f, xFraction = -0.66f, yFraction = 0.10f, scale = 0.72f, alpha = 0.56f),
    CoverFanSpec(rotation = 13f, xFraction = 0.58f, yFraction = -0.02f, scale = 0.78f, alpha = 0.68f),
    CoverFanSpec(rotation = -8f, xFraction = -0.34f, yFraction = -0.10f, scale = 0.86f, alpha = 0.82f),
    CoverFanSpec(rotation = 5f, xFraction = 0.24f, yFraction = 0.10f, scale = 0.93f, alpha = 0.92f),
    CoverFanSpec(rotation = 0f, xFraction = 0f, yFraction = 0.02f, scale = 1f, alpha = 1f)
)

// Offsets borrow the next songs for the fan, but the front card (spec index 6)
// must always be the song whose info is shown on the left.
private val coverFanSongOffsets = listOf(-3, 3, -2, 2, -1, 1, 0)

@Composable
private fun CoverCollage(
    songs: List<Song>,
    startIndex: Int,
    modifier: Modifier = Modifier
) {
    if (songs.isEmpty()) return
    val covers = remember(songs, startIndex) {
        coverFanSongOffsets.map { offset ->
            songs[Math.floorMod(startIndex + offset, songs.size)]
        }
    }
    val context = LocalContext.current
    val coverShape = RoundedCornerShape(18.dp)

    BoxWithConstraints(modifier = modifier) {
        val coverSize = minOf(maxWidth * 0.56f, maxHeight * 0.76f)
        covers.forEachIndexed { index, cover ->
            val spec = coverFanSpecs[index]
            val transition = rememberInfiniteTransition(label = "hero-cover-$index")
            val drift by transition.animateFloat(
                initialValue = -1f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = 3600 + index * 650,
                        easing = FastOutSlowInEasing
                    ),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "hero-cover-drift-$index"
            )
            val imageRequest = remember(context, cover.artworkUri) {
                ImageRequest.Builder(context)
                    .data(cover.artworkUri)
                    .crossfade(true)
                    .build()
            }
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(x = coverSize * spec.xFraction, y = coverSize * spec.yFraction)
                    .size(coverSize)
                    .graphicsLayer {
                        rotationZ = spec.rotation + drift * (1.1f + index * 0.45f)
                        translationY = drift * 5.dp.toPx() * (1f + index * 0.2f)
                        scaleX = spec.scale
                        scaleY = spec.scale
                        alpha = spec.alpha
                    }
                    .shadow(
                        elevation = if (index == covers.lastIndex) 22.dp else 8.dp,
                        shape = coverShape,
                        clip = false
                    )
                    .clip(coverShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                AsyncImage(
                    model = imageRequest,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun HeroPagerDots(count: Int, currentPage: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(count) { index ->
            val selected = index == currentPage
            Box(
                modifier = Modifier
                    .size(width = if (selected) 18.dp else 6.dp, height = 6.dp)
                    .clip(CircleShape)
                    .background(
                        if (selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    )
            )
        }
    }
}

private fun buildHeroSongs(featured: Song?, songs: List<Song>): List<Song> {
    val withArtwork = songs.filter { !it.artworkUri.isNullOrBlank() }
    if (withArtwork.isEmpty()) return emptyList()
    val seed = withArtwork.fold(0) { acc, song -> acc * 31 + song.id.hashCode() }
    val picks = withArtwork
        .filter { it.id != featured?.id }
        .shuffled(Random(seed))
        .take(HERO_SONG_COUNT)
    return (listOfNotNull(featured?.takeIf { !it.artworkUri.isNullOrBlank() }) + picks)
        .distinctBy { it.id }
        .take(HERO_SONG_COUNT)
}

private const val HERO_SONG_COUNT = 10

@Composable
private fun SectionTitle(
    title: String,
    action: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(modifier = Modifier.weight(1f))
        action?.invoke()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SongActionSheet(
    song: Song,
    playlists: List<Playlist>,
    onDismiss: () -> Unit,
    onPlayNext: () -> Unit,
    onCreatePlaylist: () -> Unit,
    onAddToPlaylist: (String) -> Unit,
    onToggleFavorite: () -> Unit,
    onShare: () -> Unit,
    onEditInfo: () -> Unit,
    onEditTags: () -> Unit,
    onRemoveFromLibrary: () -> Unit,
    onDeleteFile: () -> Unit,
    onOpenArtist: () -> Unit,
    onOpenAlbum: () -> Unit,
    onDownload: () -> Unit = {},
    isDownloading: Boolean = false,
    downloadProgress: DownloadProgress = DownloadProgress()
) {
    val strings = LocalStrings.current
    ModalBottomSheet(onDismissRequest = onDismiss) {
        SongActionHeader(song = song)
        ListItem(
            headlineContent = { Text(strings.addToNext) },
            leadingContent = { Icon(Icons.Outlined.QueueMusic, contentDescription = null) },
            modifier = Modifier.clickable(onClick = onPlayNext)
        )
        var showPlaylistMenu by remember { mutableStateOf(false) }
        ListItem(
            headlineContent = { Text(strings.addToPlaylist) },
            leadingContent = { Icon(Icons.Outlined.PlaylistAdd, contentDescription = null) },
            modifier = Modifier.clickable { showPlaylistMenu = !showPlaylistMenu }
        )
        if (showPlaylistMenu) {
            ListItem(
                headlineContent = { Text(strings.addToNewPlaylist) },
                leadingContent = { Icon(Icons.Outlined.Add, contentDescription = null) },
                modifier = Modifier
                    .clickable {
                        showPlaylistMenu = false
                        onCreatePlaylist()
                    }
                    .padding(start = 32.dp)
            )
            playlists.forEach { playlist ->
                ListItem(
                    headlineContent = { Text(playlist.name) },
                    leadingContent = { Icon(Icons.Outlined.PlaylistAdd, contentDescription = null) },
                    modifier = Modifier
                        .clickable {
                            showPlaylistMenu = false
                            onAddToPlaylist(playlist.id)
                        }
                        .padding(start = 32.dp)
                )
            }
        }
        if (song.source == Source.WEBDAV) {
            Divider()
            val progress = downloadProgress
            val progressText = if (progress.total > 1) " (${progress.completed}/${progress.total})" else ""
            val failed = song.id in progress.failedIds
            ListItem(
                headlineContent = { Text(if (failed) strings.downloadFailed else strings.download + progressText) },
                leadingContent = {
                    if (isDownloading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(
                            Icons.Outlined.Download,
                            contentDescription = null,
                            tint = if (failed) MaterialTheme.colorScheme.error else LocalContentColor.current
                        )
                    }
                },
                modifier = Modifier.clickable(enabled = !isDownloading) { onDownload() }
            )
        }
        Divider()
        ListItem(
            headlineContent = { Text(LocalExtraStrings.current.tags) },
            supportingContent = {
                Text(
                    text = song.semanticTags.takeIf { it.isNotEmpty() }?.joinToString(" · ") { "#$it" }
                        ?: LocalExtraStrings.current.noTags,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            leadingContent = { Icon(Icons.Outlined.Label, contentDescription = null) },
            modifier = Modifier.clickable(onClick = onEditTags)
        )
        ListItem(
            headlineContent = { Text(strings.editInfo) },
            leadingContent = { Icon(Icons.Outlined.MoreVert, contentDescription = null) },
            modifier = Modifier.clickable(onClick = onEditInfo)
        )
        ListItem(
            headlineContent = { Text(strings.removeFromLibrary) },
            leadingContent = { Icon(Icons.Outlined.RemoveCircleOutline, contentDescription = null) },
            modifier = Modifier.clickable(onClick = onRemoveFromLibrary)
        )
        if (song.source == Source.LOCAL) {
            ListItem(
                headlineContent = { Text(strings.deleteFromFile, color = MaterialTheme.colorScheme.error) },
                leadingContent = {
                    Icon(
                        Icons.Outlined.DeleteForever,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                },
                modifier = Modifier.clickable(onClick = onDeleteFile)
            )
        }
        ListItem(
            headlineContent = { Text(if (song.isFavorite) strings.removeFavorite else strings.addFavorite) },
            leadingContent = {
                Icon(
                    if (song.isFavorite) Icons.Outlined.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = null
                )
            },
            modifier = Modifier.clickable(onClick = onToggleFavorite)
        )
        ListItem(
            headlineContent = { Text(strings.share) },
            leadingContent = { Icon(Icons.Outlined.Share, contentDescription = null) },
            modifier = Modifier.clickable(onClick = onShare)
        )
        if (!song.artistId.isNullOrBlank()) {
            ListItem(
                headlineContent = { Text(strings.viewArtist) },
                supportingContent = { Text(song.artistName) },
                leadingContent = { Icon(Icons.Outlined.Person, contentDescription = null) },
                modifier = Modifier.clickable(onClick = onOpenArtist)
            )
        }
        if (!song.albumId.isNullOrBlank()) {
            ListItem(
                headlineContent = { Text(strings.viewAlbum) },
                supportingContent = { Text(song.albumTitle) },
                leadingContent = { Icon(Icons.Outlined.Album, contentDescription = null) },
                modifier = Modifier.clickable(onClick = onOpenAlbum)
            )
        }
        TextButton(
            onClick = onDismiss,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(strings.cancel)
        }
    }
}

@Composable
private fun ConfirmSongActionDialog(
    title: String,
    body: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalStrings.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(body) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmText, color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancel)
            }
        }
    )
}

private fun shareSong(context: android.content.Context, song: Song) {
    val text = buildString {
        append(song.title)
        if (song.artistName.isNotBlank()) append(" - ").append(song.artistName)
        if (song.albumTitle.isNotBlank()) append("\n").append(song.albumTitle)
    }
    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(android.content.Intent.EXTRA_SUBJECT, song.title)
        putExtra(android.content.Intent.EXTRA_TEXT, text)
    }
    context.startActivity(android.content.Intent.createChooser(intent, song.title))
}
