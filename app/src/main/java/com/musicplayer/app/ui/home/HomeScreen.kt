package com.musicplayer.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Album
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.PlaylistAdd
import androidx.compose.material.icons.outlined.QueueMusic
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.RemoveCircleOutline
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.musicplayer.app.domain.model.Playlist
import com.musicplayer.app.domain.model.Song
import com.musicplayer.app.domain.model.Source
import com.musicplayer.app.ui.i18n.AppStrings
import com.musicplayer.app.ui.i18n.LocalStrings
import com.musicplayer.app.ui.components.AlbumCard
import com.musicplayer.app.ui.components.EditSongDialog
import com.musicplayer.app.ui.components.SongListItem
import com.musicplayer.app.domain.usecase.LibraryWorkType

@Composable
fun HomeScreen(
    onPlaySong: (Song, List<Song>) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val workState by viewModel.workState.collectAsStateWithLifecycle()
    val featuredSong by viewModel.featuredSong.collectAsStateWithLifecycle()
    val actionSong by viewModel.actionSong.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val editSong by viewModel.editSong.collectAsStateWithLifecycle()
    val strings = LocalStrings.current
    var removeSong by remember { mutableStateOf<Song?>(null) }
    var deleteFileSong by remember { mutableStateOf<Song?>(null) }

    LaunchedEffect(Unit) {
        viewModel.refreshFeatured()
    }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            HomeHero(
                featuredSong = featuredSong,
                songs = state.songs,
                onPlaySong = onPlaySong
            )
        }
        item {
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
            item {
                ScanStatus(workState = workState)
            }
        }
        item {
            SectionTitle(title = strings.recommendedSongs)
        }
        items(state.recommendedSongs, key = { "recommended-${it.id}" }) { song ->
            SongListItem(
                song = song,
                onClick = { viewModel.selectSong(song.id) },
                onDoubleClick = { onPlaySong(song, state.songs) },
                onMoreClick = { viewModel.showSongActions(song) }
            )
        }
        item {
            SectionTitle(title = strings.recentlyAdded)
        }
        items(state.recentSongs, key = { it.id }) { song ->
            SongListItem(
                song = song,
                onClick = { viewModel.selectSong(song.id) },
                onDoubleClick = { onPlaySong(song, state.songs) },
                onMoreClick = { viewModel.showSongActions(song) }
            )
        }
        item {
            SectionTitle(title = strings.albums)
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(state.albums, key = { it.id }) { album ->
                    AlbumCard(
                        album = album,
                        onClick = {},
                        modifier = Modifier.fillParentMaxWidth(0.46f)
                    )
                }
            }
        }
    }

    actionSong?.let { song ->
        SongActionSheet(
            song = song,
            playlists = playlists,
            strings = strings,
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
            },
            onEditInfo = {
                viewModel.showEditDialog(song)
            },
            onRemoveFromLibrary = {
                viewModel.clearActionSong()
                removeSong = song
            },
            onDeleteFile = {
                viewModel.clearActionSong()
                deleteFileSong = song
            },
            onOpenArtist = {},
            onOpenAlbum = {}
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

@Composable
private fun HomeHero(
    featuredSong: Song?,
    songs: List<Song>,
    onPlaySong: (Song, List<Song>) -> Unit
) {
    val strings = LocalStrings.current
    val song = featuredSong
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
    ) {
        if (song != null && !song.artworkUri.isNullOrBlank()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(song.artworkUri)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .clip(MaterialTheme.shapes.large)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.7f)
                            )
                        ),
                        shape = MaterialTheme.shapes.large
                    )
            )
        } else {
            Surface(
                modifier = Modifier.fillMaxSize(),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Album,
                        contentDescription = null,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(64.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.4f)
                    )
                }
            }
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = song?.title ?: strings.musicLibrary,
                style = MaterialTheme.typography.headlineSmall,
                color = if (song?.artworkUri != null) Color.White
                else MaterialTheme.colorScheme.onPrimaryContainer,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = song?.let { "${it.artistName} · ${it.albumTitle}" }
                    ?: strings.localAndWebDav,
                style = MaterialTheme.typography.bodyMedium,
                color = if (song?.artworkUri != null) Color.White.copy(alpha = 0.8f)
                else MaterialTheme.colorScheme.onPrimaryContainer,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (song != null) {
                Button(
                    onClick = { onPlaySong(song, songs) },
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Icon(imageVector = Icons.Outlined.PlayArrow, contentDescription = null)
                    Text(text = strings.playFeature)
                }
            }
        }
    }
}

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
    strings: AppStrings,
    onDismiss: () -> Unit,
    onPlayNext: () -> Unit,
    onCreatePlaylist: () -> Unit,
    onAddToPlaylist: (String) -> Unit,
    onToggleFavorite: () -> Unit,
    onEditInfo: () -> Unit,
    onRemoveFromLibrary: () -> Unit,
    onDeleteFile: () -> Unit,
    onOpenArtist: () -> Unit,
    onOpenAlbum: () -> Unit
) {
    val context = LocalContext.current
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
        ListItem(
            headlineContent = { Text(strings.addToNewPlaylist) },
            leadingContent = { Icon(Icons.Outlined.PlaylistAdd, contentDescription = null) },
            modifier = Modifier.clickable(onClick = onCreatePlaylist)
        )
        playlists.forEach { playlist ->
            ListItem(
                headlineContent = { Text("${strings.addToPlaylistPrefix}${playlist.name}") },
                leadingContent = { Icon(Icons.Outlined.PlaylistAdd, contentDescription = null) },
                modifier = Modifier.clickable { onAddToPlaylist(playlist.id) }
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
            modifier = Modifier.clickable {
                shareSong(context, song)
            }
        )
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
