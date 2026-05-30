package com.musicplayer.app.ui.library

import android.content.Context
import android.content.Intent
import android.os.Environment
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.musicplayer.app.domain.model.Album
import com.musicplayer.app.domain.model.Artist
import com.musicplayer.app.domain.model.Playlist
import com.musicplayer.app.domain.model.Song
import com.musicplayer.app.domain.model.Source
import com.musicplayer.app.domain.model.albumGroupingId
import com.musicplayer.app.domain.model.artistGroupingId
import com.musicplayer.app.domain.model.formatReleaseDate
import com.musicplayer.app.ui.components.AlbumCard
import com.musicplayer.app.ui.components.EditSongDialog
import com.musicplayer.app.ui.components.FastScrollIndex
import com.musicplayer.app.ui.components.SongArtwork
import com.musicplayer.app.ui.components.libraryDetailLabel
import com.musicplayer.app.ui.i18n.LocalStrings
import java.io.File
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
                        Text(
                            text = "${strings.download}: ${dp.completed}/${dp.total}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
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
                downloadProgress = downloadProgress,
                currentSong = currentSong
            )

            LibraryTab.ALBUMS -> AlbumList(
                state = state,
                onOpenAlbum = onOpenAlbum,
                onSortModeChange = viewModel::setAlbumSortMode
            )
            LibraryTab.ARTISTS -> ArtistList(state = state, onOpenArtist = onOpenArtist)
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
    downloadProgress: DownloadProgress = DownloadProgress(),
    currentSong: Song? = null
) {
    val context = LocalContext.current
    val strings = LocalStrings.current
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var menuSong by remember { mutableStateOf<Song?>(null) }
    var editSong by remember { mutableStateOf<Song?>(null) }
    var removeSong by remember { mutableStateOf<Song?>(null) }
    var deleteFileSong by remember { mutableStateOf<Song?>(null) }
    val alphabet = remember { ('A'..'Z').map { it.toString() } + "#" }
    val songLetters = remember(state.songs) {
        state.songs.map { it.title.indexLetter() }
    }
    val firstIndexByLetter = remember(state.songs) {
        state.songs.mapIndexed { index, song -> song.title.indexLetter() to index }
            .groupBy({ it.first }, { it.second })
            .mapValues { it.value.first() }
    }
    val currentLetter by remember(listState, songLetters) {
        derivedStateOf {
            songLetters.getOrNull(listState.firstVisibleItemIndex)
        }
    }
    Row(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier.weight(1f),
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, top = 20.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(state.songs, key = { it.id }) { song ->
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

@Composable
private fun LibrarySongRow(
    song: Song,
    onDoubleTap: () -> Unit,
    onMoreClick: (() -> Unit)? = null
) {
    var pressed by remember { mutableStateOf(false) }
    var tapped by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = tween(120),
        label = "press"
    )
    val bgColor by animateColorAsState(
        targetValue = if (tapped) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surface,
        animationSpec = tween(300),
        label = "tap"
    )

    LaunchedEffect(tapped) {
        if (tapped) {
            kotlinx.coroutines.delay(400)
            tapped = false
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .background(bgColor, MaterialTheme.shapes.medium)
            .clip(MaterialTheme.shapes.medium)
            .pointerInput(song.id) {
                detectTapGestures(
                    onTap = { tapped = true },
                    onDoubleTap = { onDoubleTap() },
                    onPress = {
                        pressed = true
                        tryAwaitRelease()
                        pressed = false
                    }
                )
            }
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
                text = song.libraryDetailLabel(),
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
    onRemoveFromLibrary: () -> Unit = {},
    onDeleteFile: () -> Unit = {},
    onDownload: () -> Unit = {},
    isDownloading: Boolean = false,
    downloadProgress: DownloadProgress = DownloadProgress()
) {
    val strings = LocalStrings.current
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Text(
            text = song.title,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
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
            ListItem(
                headlineContent = { Text(strings.download + progressText) },
                leadingContent = {
                    if (isDownloading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Outlined.Download, contentDescription = null)
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
    LazyColumn(
        contentPadding = PaddingValues(start = 20.dp, top = 8.dp, end = 20.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
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
        items(state.albums, key = { it.id }) { album ->
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

private fun String.indexLetter(): String {
    val trimmed = trim()
    val first = trimmed.firstOrNull() ?: return "#"
    if (first in '0'..'9') return "#"
    if (first in 'A'..'Z') return first.toString()
    if (first in 'a'..'z') return first.uppercaseChar().toString()
    return PinyinHelper.toPinyinInitial(first) ?: "#"
}

@Composable
private fun ArtistList(
    state: LibraryUiState,
    onOpenArtist: (String) -> Unit
) {
    val strings = LocalStrings.current
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val alphabet = remember { ('A'..'Z').map { it.toString() } + "#" }
    val artistLetters = remember(state.artists) { state.artists.map { it.name.indexLetter() } }
    val firstIndexByLetter = remember(state.artists) {
        state.artists.mapIndexed { index, artist -> artist.name.indexLetter() to index }
            .groupBy({ it.first }, { it.second })
            .mapValues { it.value.first() }
    }
    val currentLetter by remember(listState, artistLetters) {
        derivedStateOf { artistLetters.getOrNull(listState.firstVisibleItemIndex) }
    }
    Row(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1f)) {
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(start = 20.dp, top = 20.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(state.artists, key = { it.id }) { artist ->
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
    val artist = state.artists.firstOrNull { it.id == artistId }
    val songs = state.songs.filter { it.artistGroupingId() == artistId }

    LaunchedEffect(artist?.name) {
        artist?.name?.let { artistInfoViewModel.load(it) }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            DetailTopBar(
                title = artist?.name ?: LocalStrings.current.artists,
                onBack = onBack
            )
        }
        item {
            ArtistHeader(
                artist = artist,
                onlineInfo = artistInfoState.info,
                loading = artistInfoState.loading,
                onScrapeAvatar = {
                    artist?.name?.let { artistInfoViewModel.scrapeAvatar(it) }
                }
            )
        }
        items(songs, key = { it.id }) { song ->
            DetailSongRow(song = song, onDoubleTap = { onPlaySong(song, songs) })
        }
    }
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
    val album = state.albums.firstOrNull { it.id == albumId }
    val songs = state.songs.filter { it.albumGroupingId() == albumId }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            DetailTopBar(
                title = album?.title ?: LocalStrings.current.albums,
                onBack = onBack
            )
        }
        item {
            AlbumHeader(album = album, songs = songs)
        }
        items(songs, key = { it.id }) { song ->
            DetailSongRow(song = song, onDoubleTap = { onPlaySong(song, songs) })
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
            modifier = Modifier.size(88.dp)
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
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Surface(
            modifier = Modifier
                .size(88.dp)
                .clip(MaterialTheme.shapes.medium),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            if (!album?.artworkUri.isNullOrBlank()) {
                AsyncImage(
                    model = album?.artworkUri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.Album,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(40.dp)
                    )
                }
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
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.clip(MaterialTheme.shapes.large),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        if (!artworkUri.isNullOrBlank()) {
            AsyncImage(
                model = artworkUri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Outlined.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(28.dp)
                )
            }
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
        else songs.filter { it.title.contains(query, ignoreCase = true) || it.artistName.contains(query, ignoreCase = true) }.take(10)
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
                                SongArtwork(song = song, modifier = Modifier.size(36.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(song.title, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(song.artistName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
