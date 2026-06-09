package com.musicplayer.app.ui.playlists

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Merge
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.musicplayer.app.domain.model.Song
import com.musicplayer.app.ui.components.SongListItem
import com.musicplayer.app.ui.i18n.LocalStrings

@Composable
fun PlaylistDetailScreen(
    playlistId: String,
    onPlaySong: (Song, List<Song>) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PlaylistsViewModel = hiltViewModel()
) {
    val songsFlow = remember(playlistId) { viewModel.observePlaylistSongs(playlistId) }
    val songs by songsFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val playlist = state.playlists.find { it.id == playlistId }
    val otherPlaylists = state.playlists.filter { it.id != playlistId }
    val strings = LocalStrings.current

    var showMergeDialog by remember { mutableStateOf(false) }
    var deleteSongTarget by remember { mutableStateOf<Song?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(top = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = playlist?.name ?: strings.playlist,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (otherPlaylists.isNotEmpty()) {
                IconButton(onClick = { showMergeDialog = true }) {
                    Icon(Icons.Outlined.Merge, contentDescription = strings.mergeFromPlaylist)
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        if (songs.isEmpty()) {
            Text(
                text = strings.addSongsHint,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(20.dp)
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                    items = songs,
                    key = { it.id },
                    contentType = { "playlist-song" }
                ) { song ->
                    SongListItem(
                        song = song,
                        onClick = { onPlaySong(song, songs) },
                        onDoubleClick = { onPlaySong(song, songs) },
                        onFavoriteClick = {},
                        onMoreClick = { deleteSongTarget = song }
                    )
                }
            }
        }
    }

    if (showMergeDialog) {
        AlertDialog(
            onDismissRequest = { showMergeDialog = false },
            title = { Text(strings.mergeFromPlaylist) },
            text = {
                Column {
                    otherPlaylists.forEach { pl ->
                        Button(
                            onClick = {
                                viewModel.addSongsFromPlaylist(playlistId, pl.id)
                                showMergeDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("${pl.name} (${pl.songCount})")
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showMergeDialog = false }) { Text(strings.close) }
            }
        )
    }

    deleteSongTarget?.let { song ->
        AlertDialog(
            onDismissRequest = { deleteSongTarget = null },
            title = { Text(song.title) },
            text = { Text("Remove from this playlist?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.removeSong(playlistId, song.id)
                    deleteSongTarget = null
                }) { Text(strings.remove) }
            },
            dismissButton = {
                TextButton(onClick = { deleteSongTarget = null }) { Text(strings.cancel) }
            }
        )
    }
}
