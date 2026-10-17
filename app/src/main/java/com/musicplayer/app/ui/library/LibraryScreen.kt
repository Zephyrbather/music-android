package com.musicplayer.app.ui.library

import android.content.Context
import android.content.Intent
import android.os.Environment
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Album
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Label
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PlaylistAdd
import androidx.compose.material.icons.outlined.QueueMusic
import androidx.compose.material.icons.outlined.RemoveCircleOutline
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.foundation.background
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.musicplayer.app.domain.model.Album
import com.musicplayer.app.domain.model.Artist
import com.musicplayer.app.domain.model.Playlist
import com.musicplayer.app.domain.model.Song
import com.musicplayer.app.domain.model.Source
import com.musicplayer.app.domain.model.albumGroupingId
import com.musicplayer.app.domain.model.artistGroupingId
import com.musicplayer.app.domain.model.formatReleaseDate
import com.musicplayer.app.ui.adaptive.LocalPadLayout
import com.musicplayer.app.ui.components.AlbumCard
import com.musicplayer.app.ui.components.EditSongDialog
import com.musicplayer.app.ui.components.FastScrollIndex
import com.musicplayer.app.ui.components.SongActionHeader
import com.musicplayer.app.ui.components.SongArtwork
import com.musicplayer.app.ui.components.SongTagsDialog
import com.musicplayer.app.ui.components.TagSongsDialog
import com.musicplayer.app.ui.components.shareSongs
import com.musicplayer.app.ui.components.collectSongTags
import com.musicplayer.app.ui.components.libraryDetailLabel
import com.musicplayer.app.ui.i18n.LocalExtraStrings
import com.musicplayer.app.ui.i18n.LocalStrings
import com.musicplayer.app.domain.usecase.SongTagGenerationResult
import java.io.File
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random
import kotlinx.coroutines.launch

@Composable
fun LibraryScreen(
    onPlaySong: (Song, List<Song>) -> Unit,
    onOpenArtist: (String) -> Unit,
    onOpenAlbum: (String) -> Unit,
    currentSong: Song? = null,
    modifier: Modifier = Modifier,
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val downloadProgress by viewModel.downloadProgress.collectAsStateWithLifecycle()
    val strings = LocalStrings.current
    val extra = LocalExtraStrings.current
    val downloadDir = remember {
        File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), "WebDAV")
    }

    var showSearch by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, top = 8.dp, end = 12.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = strings.library,
                    style = MaterialTheme.typography.headlineSmall
                )
                FilterChip(
                    selected = state.showOnlineLibrary,
                    onClick = { viewModel.toggleLibraryMode() },
                    label = {
                        Text(
                            if (state.showOnlineLibrary) strings.onlineLibrary
                            else strings.localLibrary
                        )
                    }
                )
                if (state.showOnlineLibrary && state.songs.isNotEmpty()) {
                    val dp = downloadProgress
                    val isDownloading = dp.total > 0 && dp.completed < dp.total
                    IconButton(
                        onClick = { viewModel.downloadAllSongs(downloadDir) },
                        enabled = !isDownloading,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Outlined.Download,
                            contentDescription = strings.downloadAll,
                            tint = if (isDownloading) MaterialTheme.colorScheme.onSurfaceVariant
                                   else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    if (dp.total > 0) {
                        val failedText = if (dp.failed > 0) " · ${strings.downloadFailed}: ${dp.failed}" else ""
                        Text(
                            text = "${strings.download}: ${dp.completed}/${dp.total}$failedText",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (dp.failed > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            maxLines = 1
                        )
                    }
                }
            }
            IconButton(onClick = { showSearch = true }, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Outlined.Search, contentDescription = "Search")
            }
        }
        Text(
            text = "${state.songs.size}${strings.songsCountSuffix} · ${state.albums.size}${strings.albumsCountSuffix} · ${state.artists.size}${strings.artistsCountSuffix}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 12.dp)
        )
        TabRow(selectedTabIndex = state.selectedTab.ordinal) {
            LibraryTab.values().forEach { tab ->
                Tab(
                    selected = state.selectedTab == tab,
                    onClick = { viewModel.selectTab(tab) },
                    text = {
                        Text(
                            when (tab) {
                                LibraryTab.SONGS -> strings.songs
                                LibraryTab.ALBUMS -> strings.albums
                                LibraryTab.ARTISTS -> strings.artists
                                LibraryTab.TAGS -> extra.tags
                            }
                        )
                    }
                )
            }
        }
        when (state.selectedTab) {
            LibraryTab.SONGS -> SongList(
                state = state,
                onPlaySong = onPlaySong,
                onPlayNext = viewModel::playNext,
                onToggleFavorite = viewModel::toggleFavorite,
                onCreatePlaylistWith = viewModel::createPlaylistWith,
                onAddToPlaylist = viewModel::addToPlaylist,
                onOpenArtist = onOpenArtist,
                onOpenAlbum = onOpenAlbum,
                onUpdateMetadata = viewModel::updateSongMetadata,
                onRemoveFromLibrary = viewModel::removeFromLibrary,
                onDeleteFile = viewModel::deleteLocalFile,
                onDownload = { viewModel.downloadSong(it, downloadDir) },
                onUpdateTags = viewModel::updateSongTags,
                onGenerateTags = viewModel::generateSongTags,
                downloadProgress = downloadProgress,
                currentSong = currentSong
            )

            LibraryTab.ALBUMS -> AlbumList(
                state = state,
                onOpenAlbum = onOpenAlbum,
                onSortModeChange = viewModel::setAlbumSortMode
            )
            LibraryTab.ARTISTS -> ArtistList(state = state, onOpenArtist = onOpenArtist)
            LibraryTab.TAGS -> TagBrowser(
                songs = state.songs,
                onPlaySong = onPlaySong,
                onUpdateTags = viewModel::updateSongTags,
                onGenerateTags = viewModel::generateSongTags,
                onAddTagToSongs = viewModel::addTagToSongs,
                onDeleteTag = viewModel::deleteTag
            )
        }
    }

    if (showSearch) {
        SearchDialog(
            query = searchQuery,
            onQueryChange = { searchQuery = it },
            songs = state.songs,
            albums = state.albums,
            artists = state.artists,
            strings = strings,
            onDismiss = { showSearch = false; searchQuery = "" },
            onOpenSong = { showSearch = false; searchQuery = ""; onPlaySong(it, state.songs) },
            onOpenAlbum = { showSearch = false; searchQuery = ""; onOpenAlbum(it) },
            onOpenArtist = { showSearch = false; searchQuery = ""; onOpenArtist(it) }
        )
    }
}

@Composable
private fun SongList(
    state: LibraryUiState,
    onPlaySong: (Song, List<Song>) -> Unit,
    onPlayNext: (Song) -> Unit,
    onToggleFavorite: (Song) -> Unit,
    onCreatePlaylistWith: (Song) -> Unit,
    onAddToPlaylist: (String, Song) -> Unit,
    onOpenArtist: (String) -> Unit,
    onOpenAlbum: (String) -> Unit,
    onUpdateMetadata: (String, String, String, String, String?, String?) -> Unit = { _, _, _, _, _, _ -> },
    onRemoveFromLibrary: (Song) -> Unit = {},
    onDeleteFile: (Song) -> Unit = {},
    onDownload: (Song) -> Unit = {},
    onUpdateTags: (String, List<String>) -> Unit = { _, _ -> },
    onGenerateTags: suspend (Song) -> SongTagGenerationResult = { SongTagGenerationResult.Failed },
    downloadProgress: DownloadProgress = DownloadProgress(),
    currentSong: Song? = null
) {
    val context = LocalContext.current
    val strings = LocalStrings.current
    val extra = LocalExtraStrings.current
    val isPadLayout = LocalPadLayout.current
    val listState = rememberLazyGridState()
    val scope = rememberCoroutineScope()
    var menuSong by remember { mutableStateOf<Song?>(null) }
    var editSong by remember { mutableStateOf<Song?>(null) }
    var tagSong by remember { mutableStateOf<Song?>(null) }
    var removeSong by remember { mutableStateOf<Song?>(null) }
    var deleteFileSong by remember { mutableStateOf<Song?>(null) }
    val allTags = remember(state.songs) { collectSongTags(state.songs) }
    val alphabet = remember { ('A'..'Z').map { it.toString() } + "#" }
    val songLetters = state.songIndexLetters
    val firstIndexByLetter = state.firstSongIndexByLetter
    val currentLetter by remember(listState, songLetters) {
        derivedStateOf {
            songLetters.getOrNull(listState.firstVisibleItemIndex)
        }
    }
    Row(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier.weight(1f),
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(if (isPadLayout) 2 else 1),
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 20.dp,
                    end = if (isPadLayout) 24.dp else 20.dp,
                    top = 20.dp,
                    bottom = 20.dp
                ),
                horizontalArrangement = Arrangement.spacedBy(if (isPadLayout) 16.dp else 0.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(
                    items = state.songs,
                    key = { it.id },
                    contentType = { "library-song" }
                ) { song ->
                    LibrarySongRow(
                        song = song,
                        onDoubleTap = { onPlaySong(song, state.songs) },
                        onMoreClick = { menuSong = song }
                    )
                }
            }
            if (listState.isScrollInProgress && currentLetter != null) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 20.dp, end = 8.dp),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Text(
                        text = currentLetter.orEmpty(),
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
            if (currentSong != null) {
                SmallFloatingActionButton(
                    onClick = {
                        val targetIndex = state.songs.indexOfFirst { it.id == currentSong.id }
                        scope.launch {
                            listState.animateScrollToItem(targetIndex.coerceAtLeast(0))
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 4.dp, bottom = 4.dp),
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    val iconColor = MaterialTheme.colorScheme.onPrimaryContainer
                    androidx.compose.foundation.Canvas(modifier = Modifier.size(20.dp)) {
                        val strokeWidth = 2.dp.toPx()
                        drawCircle(
                            color = iconColor,
                            radius = size.minDimension / 2f,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(strokeWidth)
                        )
                        drawCircle(
                            color = iconColor,
                            radius = size.minDimension / 6f
                        )
                    }
                }
            }
        }
        FastScrollIndex(
            letters = alphabet,
            selectedLetter = currentLetter,
            onLetterSelected = { letter ->
                firstIndexByLetter[letter]?.let { index ->
                    scope.launch { listState.animateScrollToItem(index) }
                }
            },
            modifier = Modifier
                .fillMaxHeight()
                .padding(start = 2.dp, end = 0.dp, top = 20.dp, bottom = 20.dp)
        )
    }

    menuSong?.let { song ->
        SongActionSheet(
            song = song,
            playlists = state.playlists,
            onDismiss = { menuSong = null },
            onPlayNext = {
                onPlayNext(song)
                menuSong = null
            },
            onCreatePlaylist = {
                onCreatePlaylistWith(song)
                menuSong = null
            },
            onAddToPlaylist = { playlistId ->
                onAddToPlaylist(playlistId, song)
                menuSong = null
            },
            onToggleFavorite = {
                onToggleFavorite(song)
                menuSong = null
            },
            onShare = {
                shareSong(context, song)
                menuSong = null
            },
            onOpenArtist = {
                song.artistId?.let(onOpenArtist)
                menuSong = null
            },
            onOpenAlbum = {
                song.albumId?.let(onOpenAlbum)
                menuSong = null
            },
            onEditInfo = {
                menuSong = null
                editSong = song
            },
            onEditTags = {
                menuSong = null
                tagSong = song
            },
            onRemoveFromLibrary = {
                menuSong = null
                removeSong = song
            },
            onDeleteFile = {
                menuSong = null
                deleteFileSong = song
            },
            onDownload = {
                onDownload(song)
                menuSong = null
            },
            isDownloading = song.id in downloadProgress.downloadingIds,
            downloadProgress = downloadProgress
        )
    }

    editSong?.let { song ->
        EditSongDialog(
            song = song,
            onSave = { title, artist, album, artworkUri, artworkProvider ->
                onUpdateMetadata(song.id, title, artist, album, artworkUri, artworkProvider)
                editSong = null
            },
            onDismiss = { editSong = null }
        )
    }

    tagSong?.let { song ->
        SongTagsDialog(
            song = song,
            allTags = allTags,
            onSave = { tags ->
                onUpdateTags(song.id, tags)
                tagSong = null
            },
            onGenerateWithAi = onGenerateTags,
            onDismiss = { tagSong = null }
        )
    }

    removeSong?.let { song ->
        ConfirmSongActionDialog(
            title = strings.removeFromLibrary,
            body = strings.removeFromLibraryConfirm.format(song.title),
            confirmText = strings.removeFromLibrary,
            onConfirm = {
                onRemoveFromLibrary(song)
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
                onDeleteFile(song)
                deleteFileSong = null
            },
            onDismiss = { deleteFileSong = null }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LibrarySongRow(
    song: Song,
    onDoubleTap: () -> Unit,
    onMoreClick: (() -> Unit)? = null
) {
    val detailLabel = remember(
        song.id,
        song.qualityLabel,
        song.artistName,
        song.albumTitle,
        song.durationMs,
        song.categoryTag
    ) {
        song.libraryDetailLabel()
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Transparent, MaterialTheme.shapes.medium)
            .clip(MaterialTheme.shapes.medium)
            .combinedClickable(
                onClick = {},
                onDoubleClick = onDoubleTap
            )
            .padding(horizontal = 2.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SongArtwork(song = song, modifier = Modifier.size(56.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = detailLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (onMoreClick != null) {
            IconButton(onClick = onMoreClick) {
                Icon(
                    imageVector = Icons.Outlined.MoreVert,
                    contentDescription = "More"
                )
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun SongActionSheet(
    song: Song,
    playlists: List<Playlist>,
    onDismiss: () -> Unit,
    onPlayNext: () -> Unit,
    onCreatePlaylist: () -> Unit,
    onAddToPlaylist: (String) -> Unit,
    onToggleFavorite: () -> Unit,
    onShare: () -> Unit,
    onOpenArtist: () -> Unit,
    onOpenAlbum: () -> Unit,
    onEditInfo: () -> Unit = {},
    onEditTags: () -> Unit = {},
    onRemoveFromLibrary: () -> Unit = {},
    onDeleteFile: () -> Unit = {},
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
            val dp = downloadProgress
            val progressText = if (dp.total > 1) " (${dp.completed}/${dp.total})" else ""
            val failed = song.id in dp.failedIds
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
            headlineContent = { Text(strings.editInfo) },
            leadingContent = { Icon(Icons.Outlined.MoreVert, contentDescription = null) },
            modifier = Modifier.clickable(onClick = onEditInfo)
        )
        ListItem(
            headlineContent = { Text(LocalExtraStrings.current.tags) },
            supportingContent = {
                val tagText = song.semanticTags.takeIf { it.isNotEmpty() }?.joinToString(" ") { "#$it" }
                if (tagText != null) Text(tagText, maxLines = 1, overflow = TextOverflow.Ellipsis)
            },
            leadingContent = { Icon(Icons.Outlined.Label, contentDescription = null) },
            modifier = Modifier.clickable(onClick = onEditTags)
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

@Composable
private fun AlbumList(
    state: LibraryUiState,
    onOpenAlbum: (String) -> Unit,
    onSortModeChange: (AlbumSortMode) -> Unit
) {
    val isPadLayout = LocalPadLayout.current
    LazyVerticalGrid(
        columns = if (isPadLayout) {
            GridCells.Adaptive(minSize = 200.dp)
        } else {
            GridCells.Fixed(1)
        },
        contentPadding = PaddingValues(
            start = 20.dp,
            end = if (isPadLayout) 24.dp else 20.dp,
            top = 8.dp,
            bottom = 20.dp
        ),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            val strings = LocalStrings.current
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AlbumSortChip(
                    label = strings.albumSortDate,
                    mode = AlbumSortMode.RELEASE_DATE,
                    selectedMode = state.albumSortMode,
                    direction = state.albumSortDirection,
                    onSortModeChange = onSortModeChange,
                    modifier = Modifier.weight(1f)
                )
                AlbumSortChip(
                    label = strings.albumSortTitle,
                    mode = AlbumSortMode.ALBUM_TITLE,
                    selectedMode = state.albumSortMode,
                    direction = state.albumSortDirection,
                    onSortModeChange = onSortModeChange,
                    modifier = Modifier.weight(1f)
                )
                AlbumSortChip(
                    label = strings.albumSortArtist,
                    mode = AlbumSortMode.ARTIST_NAME,
                    selectedMode = state.albumSortMode,
                    direction = state.albumSortDirection,
                    onSortModeChange = onSortModeChange,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        items(
            items = state.albums,
            key = { it.id },
            contentType = { "library-album" }
        ) { album ->
            AlbumCard(
                album = album,
                onClick = { onOpenAlbum(album.id) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun AlbumSortChip(
    label: String,
    mode: AlbumSortMode,
    selectedMode: AlbumSortMode,
    direction: AlbumSortDirection,
    onSortModeChange: (AlbumSortMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val selected = selectedMode == mode
    FilterChip(
        selected = selected,
        onClick = { onSortModeChange(mode) },
        modifier = modifier.heightIn(min = 40.dp),
        label = {
            Text(
                text = label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        trailingIcon = if (selected) {
            {
                Icon(
                    imageVector = if (direction == AlbumSortDirection.ASCENDING) {
                        Icons.Outlined.ArrowUpward
                    } else {
                        Icons.Outlined.ArrowDownward
                    },
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
        } else {
            null
        }
    )
}

@Composable
private fun ArtistList(
    state: LibraryUiState,
    onOpenArtist: (String) -> Unit
) {
    val strings = LocalStrings.current
    val isPadLayout = LocalPadLayout.current
    val listState = rememberLazyGridState()
    val scope = rememberCoroutineScope()
    val alphabet = remember { ('A'..'Z').map { it.toString() } + "#" }
    val artistLetters = state.artistIndexLetters
    val firstIndexByLetter = state.firstArtistIndexByLetter
    val currentLetter by remember(listState, artistLetters) {
        derivedStateOf { artistLetters.getOrNull(listState.firstVisibleItemIndex) }
    }
    Row(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1f)) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(if (isPadLayout) 2 else 1),
                state = listState,
                contentPadding = PaddingValues(
                    start = 20.dp,
                    end = if (isPadLayout) 24.dp else 20.dp,
                    top = 20.dp,
                    bottom = 20.dp
                ),
                horizontalArrangement = Arrangement.spacedBy(if (isPadLayout) 16.dp else 0.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(
                    items = state.artists,
                    key = { it.id },
                    contentType = { "library-artist" }
                ) { artist ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenArtist(artist.id) }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        ArtistAvatar(artworkUri = artist.artworkUri, modifier = Modifier.size(48.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = artist.name,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${artist.albumCount}${strings.albumsCountSuffix} - ${artist.songCount}${strings.songsCountSuffix}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            if (listState.isScrollInProgress && currentLetter != null) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 20.dp, end = 8.dp),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Text(
                        text = currentLetter.orEmpty(),
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
        FastScrollIndex(
            letters = alphabet,
            selectedLetter = currentLetter,
            onLetterSelected = { letter ->
                firstIndexByLetter[letter]?.let { index ->
                    scope.launch { listState.animateScrollToItem(index) }
                }
            },
            modifier = Modifier
                .fillMaxHeight()
                .padding(start = 2.dp, end = 0.dp, top = 20.dp, bottom = 20.dp)
        )
    }
}

@Composable
fun ArtistDetailScreen(
    artistId: String,
    onBack: () -> Unit,
    onPlaySong: (Song, List<Song>) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LibraryViewModel = hiltViewModel(),
    artistInfoViewModel: ArtistInfoViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val artistInfoState by artistInfoViewModel.uiState.collectAsStateWithLifecycle()
    val artist = remember(state.artists, artistId) {
        state.artists.firstOrNull { it.id == artistId }
    }
    val songs = remember(state.songs, artistId) {
        state.songs.filter { it.artistGroupingId() == artistId }
    }

    LaunchedEffect(artist?.name) {
        artist?.name?.let { artistInfoViewModel.load(it) }
    }

    DetailContent(
        isPadLayout = LocalPadLayout.current,
        topBar = {
            DetailTopBar(
                title = artist?.name ?: LocalStrings.current.artists,
                onBack = onBack
            )
        },
        header = {
            ArtistHeader(
                artist = artist,
                onlineInfo = artistInfoState.info,
                loading = artistInfoState.loading,
                onScrapeAvatar = {
                    artist?.name?.let { artistInfoViewModel.scrapeAvatar(it) }
                }
            )
        },
        songs = songs,
        onPlaySong = onPlaySong,
        modifier = modifier
    )
}
@Composable
fun AlbumDetailScreen(
    albumId: String,
    onBack: () -> Unit,
    onPlaySong: (Song, List<Song>) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val album = remember(state.albums, albumId) {
        state.albums.firstOrNull { it.id == albumId }
    }
    val songs = remember(state.songs, albumId) {
        state.songs.filter { it.albumGroupingId() == albumId }
    }

    DetailContent(
        isPadLayout = LocalPadLayout.current,
        topBar = {
            DetailTopBar(
                title = album?.title ?: LocalStrings.current.albums,
                onBack = onBack
            )
        },
        header = {
            AlbumHeader(album = album, songs = songs)
        },
        songs = songs,
        onPlaySong = onPlaySong,
        modifier = modifier
    )
}

@Composable
private fun DetailContent(
    isPadLayout: Boolean,
    topBar: @Composable () -> Unit,
    header: @Composable () -> Unit,
    songs: List<Song>,
    onPlaySong: (Song, List<Song>) -> Unit,
    modifier: Modifier = Modifier
) {
    if (isPadLayout) {
        Row(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            Column(
                modifier = Modifier.width(340.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                topBar()
                header()
            }
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(
                    items = songs,
                    key = { it.id },
                    contentType = { "detail-song" }
                ) { song ->
                    DetailSongRow(song = song, onDoubleTap = { onPlaySong(song, songs) })
                }
            }
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { topBar() }
            item { header() }
            items(
                items = songs,
                key = { it.id },
                contentType = { "detail-song" }
            ) { song ->
                DetailSongRow(song = song, onDoubleTap = { onPlaySong(song, songs) })
            }
        }
    }
}

@Composable
private fun DetailTopBar(
    title: String,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        IconButton(onClick = onBack) {
            Icon(imageVector = Icons.Outlined.ArrowBack, contentDescription = null)
        }
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ArtistHeader(
    artist: Artist?,
    onlineInfo: com.musicplayer.app.domain.model.ArtistOnlineInfo?,
    loading: Boolean,
    onScrapeAvatar: () -> Unit
) {
    val strings = LocalStrings.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ArtistAvatar(
            artworkUri = onlineInfo?.avatarUri ?: artist?.artworkUri,
            modifier = Modifier.size(88.dp),
            imageSize = 88.dp
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = artist?.name ?: strings.artists,
                    style = MaterialTheme.typography.headlineSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = onScrapeAvatar,
                    enabled = artist != null && !loading
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = strings.scrapeArtistAvatar
                    )
                }
            }
            if (artist != null) {
                Text(
                    text = "${artist.albumCount}${strings.albumsCountSuffix} - ${artist.songCount}${strings.songsCountSuffix}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (loading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            }
        }
    }
    onlineInfo?.bio?.takeIf { it.isNotBlank() }?.let { bio ->
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = bio,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 5,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun AlbumHeader(
    album: Album?,
    songs: List<Song>
) {
    val strings = LocalStrings.current
    val artworkSize = 88.dp
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(artworkSize)
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            if (!album?.artworkUri.isNullOrBlank()) {
                val context = LocalContext.current
                val imageSizePx = with(LocalDensity.current) { artworkSize.roundToPx().coerceAtLeast(1) }
                val imageRequest = remember(context, album?.artworkUri, imageSizePx) {
                    ImageRequest.Builder(context)
                        .data(album?.artworkUri)
                        .size(imageSizePx, imageSizePx)
                        .crossfade(false)
                        .build()
                }
                AsyncImage(
                    model = imageRequest,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Outlined.Album,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(40.dp)
                )
            }
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = album?.title ?: strings.albums,
                style = MaterialTheme.typography.headlineSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            val dateText = formatReleaseDate(album?.releaseYear, album?.releaseMonth)
            Text(
                text = listOfNotNull(
                    album?.artistName,
                    dateText,
                    "${songs.size}${strings.songsCountSuffix}"
                ).joinToString(" - "),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ArtistAvatar(
    artworkUri: String?,
    modifier: Modifier = Modifier,
    imageSize: Dp = 48.dp
) {
    Box(
        modifier = modifier
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.secondaryContainer),
        contentAlignment = Alignment.Center
    ) {
        if (!artworkUri.isNullOrBlank()) {
            val context = LocalContext.current
            val imageSizePx = with(LocalDensity.current) { imageSize.roundToPx().coerceAtLeast(1) }
            val imageRequest = remember(context, artworkUri, imageSizePx) {
                ImageRequest.Builder(context)
                    .data(artworkUri)
                    .size(imageSizePx, imageSizePx)
                    .crossfade(false)
                    .build()
            }
            AsyncImage(
                model = imageRequest,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                imageVector = Icons.Outlined.Person,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
private fun DetailSongRow(
    song: Song,
    onDoubleTap: () -> Unit
) {
    LibrarySongRow(
        song = song,
        onDoubleTap = onDoubleTap
    )
}

private fun shareSong(context: Context, song: Song) {
    val text = buildString {
        append(song.title)
        if (song.artistName.isNotBlank()) append(" - ").append(song.artistName)
        if (song.albumTitle.isNotBlank()) append("\n").append(song.albumTitle)
    }
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, song.title)
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, song.title))
}

@Composable
private fun SearchDialog(
    query: String,
    onQueryChange: (String) -> Unit,
    songs: List<Song>,
    albums: List<Album>,
    artists: List<Artist>,
    strings: com.musicplayer.app.ui.i18n.AppStrings,
    onDismiss: () -> Unit,
    onOpenSong: (Song) -> Unit,
    onOpenAlbum: (String) -> Unit,
    onOpenArtist: (String) -> Unit
) {
    val filteredSongs = remember(query, songs) {
        if (query.isBlank()) emptyList()
        else songs.filter { it.matchesLibrarySearch(query) }.take(10)
    }
    val filteredAlbums = remember(query, albums) {
        if (query.isBlank()) emptyList()
        else albums.filter { it.title.contains(query, ignoreCase = true) || it.artistName.contains(query, ignoreCase = true) }.take(5)
    }
    val filteredArtists = remember(query, artists) {
        if (query.isBlank()) emptyList()
        else artists.filter { it.name.contains(query, ignoreCase = true) }.take(5)
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 480.dp),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("Search songs, albums, artists...") },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) }
                )
                if (filteredSongs.isNotEmpty()) {
                    Text(strings.songs, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 12.dp))
                    filteredSongs.forEach { song ->
                        Surface(
                            modifier = Modifier.fillMaxWidth().clickable { onOpenSong(song) },
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Row(modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                SongArtwork(song = song, modifier = Modifier.size(36.dp), imageSize = 36.dp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(song.title, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(
                                        song.searchSubtitle(),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
                if (filteredAlbums.isNotEmpty()) {
                    Text(strings.albums, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 12.dp))
                    filteredAlbums.forEach { album ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { onOpenAlbum(album.id) }.padding(vertical = 8.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Outlined.Album, null, modifier = Modifier.size(36.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(album.title, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(album.artistName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
                if (filteredArtists.isNotEmpty()) {
                    Text(strings.artists, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 12.dp))
                    filteredArtists.forEach { artist ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { onOpenArtist(artist.id) }.padding(vertical = 8.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Outlined.Person, null, modifier = Modifier.size(36.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(artist.name, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                if (query.isNotBlank() && filteredSongs.isEmpty() && filteredAlbums.isEmpty() && filteredArtists.isEmpty()) {
                    Text("No results", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 12.dp))
                }
            }
        }
    }
}

private fun Song.matchesLibrarySearch(query: String): Boolean {
    val normalizedQuery = query.trim()
    if (normalizedQuery.isBlank()) return false
    return title.contains(normalizedQuery, ignoreCase = true) ||
        artistName.contains(normalizedQuery, ignoreCase = true) ||
        albumTitle.contains(normalizedQuery, ignoreCase = true) ||
        categoryTag.orEmpty().contains(normalizedQuery, ignoreCase = true) ||
        semanticTags.any { tag ->
            tag.contains(normalizedQuery, ignoreCase = true) ||
                tag.replace('-', ' ').contains(normalizedQuery, ignoreCase = true)
        }
}

private fun Song.searchSubtitle(): String {
    val details = buildList {
        add(artistName)
        categoryTag?.takeIf { it.isNotBlank() }?.let { add(it) }
        semanticTags.takeIf { it.isNotEmpty() }?.take(3)?.joinToString(" ") { "#$it" }?.let { add(it) }
    }
    return details.joinToString(" · ")
}

private data class TagCount(val name: String, val count: Int)

private data class PlanetNode(
    val tag: TagCount,
    val color: Color,
    val widthDp: Float,
    val heightDp: Float,
    val x: Float,
    val y: Float,
    val depth: Float
)

@Composable
private fun TagBrowser(
    songs: List<Song>,
    onPlaySong: (Song, List<Song>) -> Unit,
    onUpdateTags: (String, List<String>) -> Unit,
    onGenerateTags: suspend (Song) -> SongTagGenerationResult,
    onAddTagToSongs: (String, Set<String>) -> Unit,
    onDeleteTag: (String) -> Unit
) {
    val strings = LocalStrings.current
    val extra = LocalExtraStrings.current
    val isPadLayout = LocalPadLayout.current
    val allTags = remember(songs) { collectSongTags(songs) }
    val tagCounts = remember(songs) {
        val counts = LinkedHashMap<String, Pair<String, Int>>()
        songs.forEach { song ->
            song.semanticTags.forEach { raw ->
                val tag = raw.trim()
                if (tag.isBlank()) return@forEach
                val key = tag.lowercase()
                val current = counts[key]
                counts[key] = if (current == null) tag to 1 else current.first to current.second + 1
            }
        }
        counts.values
            .map { TagCount(it.first, it.second) }
            .sortedWith(compareByDescending<TagCount> { it.count }.thenBy { it.name.lowercase() })
    }
    var selectedTag by remember { mutableStateOf<String?>(null) }
    var tagSong by remember { mutableStateOf<Song?>(null) }
    val activeTag = selectedTag?.let { key ->
        tagCounts.firstOrNull { it.name.equals(key, ignoreCase = true) }
    }
    val visibleSongs = remember(songs, activeTag) {
        if (activeTag == null) {
            emptyList()
        } else {
            songs.filter { song ->
                song.semanticTags.any { it.equals(activeTag.name, ignoreCase = true) }
            }
        }
    }

    val context = LocalContext.current
    var showNewTagDialog by remember { mutableStateOf(false) }
    var addSongsTargetTag by remember { mutableStateOf<String?>(null) }
    var deleteTagTarget by remember { mutableStateOf<String?>(null) }

    if (tagCounts.isEmpty()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = extra.noTags,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            TextButton(onClick = { showNewTagDialog = true }) {
                Icon(Icons.Outlined.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text(extra.newTag)
            }
        }
    } else if (activeTag == null) {
        TagPlanet(
            tags = tagCounts,
            songCount = songs.size,
            onSelectTag = { selectedTag = it },
            onCreateTag = { showNewTagDialog = true }
        )
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 20.dp, top = 8.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { selectedTag = null }) {
                    Icon(Icons.Outlined.ArrowBack, contentDescription = extra.backToPlanet)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "#${activeTag.name}",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "${visibleSongs.size}${strings.songsCountSuffix}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = {
                    shareSongs(context, "#${activeTag.name}", visibleSongs)
                }) {
                    Icon(Icons.Outlined.Share, contentDescription = strings.share)
                }
                IconButton(onClick = { addSongsTargetTag = activeTag.name }) {
                    Icon(Icons.Outlined.PlaylistAdd, contentDescription = extra.addSongsToTag)
                }
                IconButton(onClick = { deleteTagTarget = activeTag.name }) {
                    Icon(Icons.Outlined.DeleteForever, contentDescription = extra.deleteTag)
                }
                TextButton(onClick = { selectedTag = null }) {
                    Text(extra.backToPlanet)
                }
            }
            LazyVerticalGrid(
                columns = GridCells.Fixed(if (isPadLayout) 2 else 1),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(
                    start = 20.dp,
                    end = if (isPadLayout) 24.dp else 20.dp,
                    top = 8.dp,
                    bottom = 20.dp
                ),
                horizontalArrangement = Arrangement.spacedBy(if (isPadLayout) 16.dp else 0.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(
                    items = visibleSongs,
                    key = { it.id },
                    contentType = { "tag-song" }
                ) { song ->
                    LibrarySongRow(
                        song = song,
                        onDoubleTap = { onPlaySong(song, visibleSongs) },
                        onMoreClick = { tagSong = song }
                    )
                }
                if (visibleSongs.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            text = extra.noTaggedSongs,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    }
                }
            }
        }
    }

    if (showNewTagDialog) {
        TagSongsDialog(
            title = extra.newTag,
            fixedTagName = null,
            songs = songs,
            onConfirm = { name, ids ->
                onAddTagToSongs(name, ids)
                selectedTag = name
                showNewTagDialog = false
            },
            onDismiss = { showNewTagDialog = false }
        )
    }

    addSongsTargetTag?.let { tag ->
        TagSongsDialog(
            title = "#$tag",
            fixedTagName = tag,
            songs = songs.filterNot { song ->
                song.semanticTags.any { it.equals(tag, ignoreCase = true) }
            },
            onConfirm = { _, ids ->
                onAddTagToSongs(tag, ids)
                addSongsTargetTag = null
            },
            onDismiss = { addSongsTargetTag = null }
        )
    }

    deleteTagTarget?.let { tag ->
        AlertDialog(
            onDismissRequest = { deleteTagTarget = null },
            title = { Text(extra.deleteTag) },
            text = { Text(extra.deleteTagConfirmFormat.format(tag)) },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteTag(tag)
                    if (selectedTag?.equals(tag, ignoreCase = true) == true) selectedTag = null
                    deleteTagTarget = null
                }) {
                    Text(extra.deleteTag)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTagTarget = null }) {
                    Text(strings.cancel)
                }
            }
        )
    }

    tagSong?.let { song ->
        SongTagsDialog(
            song = song,
            allTags = allTags,
            onSave = { tags ->
                onUpdateTags(song.id, tags)
                tagSong = null
            },
            onGenerateWithAi = onGenerateTags,
            onDismiss = { tagSong = null }
        )
    }
}

@Composable
private fun TagPlanet(
    tags: List<TagCount>,
    songCount: Int,
    onSelectTag: (String) -> Unit,
    onCreateTag: () -> Unit
) {
    val extra = LocalExtraStrings.current
    val densityInfo = LocalDensity.current
    val spinTransition = rememberInfiniteTransition(label = "tag-planet")
    val autoSpin by spinTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(120_000, easing = LinearEasing)),
        label = "tag-planet-spin"
    )
    var dragSpin by remember { mutableFloatStateOf(0f) }
    var dragTilt by remember { mutableFloatStateOf(0f) }
    val colors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.tertiary
    )
    val surfaceColor = MaterialTheme.colorScheme.surface
    val starPositions = remember {
        val random = Random(7)
        List(46) { Offset(random.nextFloat(), random.nextFloat()) }
    }
    val maxCount = tags.maxOf { it.count }.coerceAtLeast(1)

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    dragSpin = (dragSpin + dragAmount.x * 0.22f) % 360f
                    dragTilt = (dragTilt + dragAmount.y * 0.14f).coerceIn(-55f, 55f)
                }
            }
    ) {
        val widthPx = with(densityInfo) { maxWidth.toPx() }
        val heightPx = with(densityInfo) { maxHeight.toPx() }
        val center = Offset(widthPx / 2f, heightPx * 0.47f)
        val radius = min(widthPx, heightPx) * 0.43f
        val coreRadius = radius * 0.2f

        Canvas(modifier = Modifier.fillMaxSize()) {
            starPositions.forEachIndexed { index, star ->
                drawCircle(
                    color = colors[index % colors.size].copy(alpha = 0.22f),
                    radius = (1.2f + (index % 3)) * densityInfo.density,
                    center = Offset(star.x * size.width, star.y * size.height)
                )
            }
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        colors[0].copy(alpha = 0.20f),
                        colors[2].copy(alpha = 0.08f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = radius * 1.08f
                ),
                radius = radius * 1.08f,
                center = center
            )
            val ringRadii = listOf(0.68f, 0.9f, 1.1f)
            ringRadii.forEachIndexed { index, factor ->
                val r = radius * factor
                drawOval(
                    color = colors[index % colors.size].copy(alpha = 0.24f),
                    topLeft = Offset(center.x - r, center.y - r * 0.42f),
                    size = Size(r * 2f, r * 0.84f),
                    style = Stroke(width = 1.dp.toPx())
                )
            }
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        colors[0].copy(alpha = 0.75f),
                        colors[1].copy(alpha = 0.45f),
                        colors[2].copy(alpha = 0.18f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = coreRadius * 2.4f
                ),
                radius = coreRadius * 2.4f,
                center = center
            )
        }

        val goldenAngle = 2.39996323f
        val tiltRad = Math.toRadians(dragTilt.toDouble())
        val nodes = tags.mapIndexed { index, tag ->
            val fraction = (index + 0.5f) / tags.size
            val latitude = acos((1f - 2f * fraction).coerceIn(-1f, 1f).toDouble())
            val azimuth = goldenAngle * index + Math.toRadians((autoSpin + dragSpin).toDouble())
            val ringX = sin(latitude) * cos(azimuth)
            val ringZ = sin(latitude) * sin(azimuth)
            val y = cos(latitude) * cos(tiltRad) - ringZ * sin(tiltRad)
            val z = cos(latitude) * sin(tiltRad) + ringZ * cos(tiltRad)
            val depth = ((z + 1.0) / 2.0).toFloat().coerceIn(0f, 1f)
            val scale = 0.82f + 0.30f * depth
            val heightDp = (58f + 26f * kotlin.math.sqrt(tag.count.toFloat() / maxCount)) * scale
            val widthDp = maxOf(heightDp, tag.name.length * 12.5f + 28f)
            val widthPx = widthDp * densityInfo.density
            val heightPx = heightDp * densityInfo.density
            PlanetNode(
                tag = tag,
                color = colors[index % colors.size],
                widthDp = widthDp,
                heightDp = heightDp,
                x = center.x + (ringX * radius).toFloat() - widthPx / 2f,
                y = center.y + (y * radius * 0.82).toFloat() - heightPx / 2f,
                depth = depth
            )
        }.sortedBy { it.depth }

        nodes.forEach { node ->
            PlanetTagNode(
                tag = node.tag,
                color = node.color,
                surfaceColor = surfaceColor,
                width = node.widthDp.dp,
                height = node.heightDp.dp,
                x = node.x,
                yPx = node.y,
                depth = node.depth,
                onClick = { onSelectTag(node.tag.name) }
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Text(
                text = extra.tagPlanetTitle,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = extra.tagPlanetCountFormat.format(tags.size, songCount),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Surface(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
            tonalElevation = 0.dp
        ) {
            TextButton(onClick = onCreateTag) {
                Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(extra.newTag)
            }
        }

        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 14.dp),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
            tonalElevation = 0.dp
        ) {
            Text(
                text = extra.tagPlanetHint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }
}

@Composable
private fun PlanetTagNode(
    tag: TagCount,
    color: Color,
    surfaceColor: Color,
    width: Dp,
    height: Dp,
    x: Float,
    yPx: Float,
    depth: Float,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(percent = 50)
    Box(
        modifier = Modifier
            .offset { IntOffset(x.roundToInt(), yPx.roundToInt()) }
            .size(width = width, height = height)
            .graphicsLayer { alpha = 0.4f + 0.6f * depth }
            .shadow(elevation = (4f + 10f * depth).dp, shape = shape, ambientColor = color, spotColor = color)
            .clip(shape)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        color.copy(alpha = 0.55f),
                        surfaceColor.copy(alpha = 0.92f)
                    )
                )
            )
            .border(width = 1.dp, color = color.copy(alpha = 0.75f), shape = shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "#${tag.name}",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            Text(
                text = tag.count.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = color,
                textAlign = TextAlign.Center
            )
        }
    }
}
