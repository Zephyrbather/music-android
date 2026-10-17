package com.musicplayer.app.ui.playlists

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Label
import androidx.compose.material.icons.outlined.Merge
import androidx.compose.material.icons.outlined.NewReleases
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.musicplayer.app.data.recommend.RecommendedPlaylist
import com.musicplayer.app.data.recommend.RecommendedPlaylistKind
import com.musicplayer.app.domain.model.Playlist
import com.musicplayer.app.domain.model.Song
import com.musicplayer.app.ui.adaptive.LocalPadLayout
import com.musicplayer.app.ui.components.PlaylistCard
import com.musicplayer.app.ui.components.SongListItem
import com.musicplayer.app.ui.components.shareSongs
import com.musicplayer.app.ui.i18n.LocalExtraStrings
import com.musicplayer.app.ui.i18n.LocalStrings
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PlaylistsScreen(
    onOpenPlaylist: (String) -> Unit,
    onPlaySong: (Song, List<Song>) -> Unit,
    onOpenListeningStats: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PlaylistsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val aiPlaylistState by viewModel.aiPlaylistState.collectAsStateWithLifecycle()
    val recommended by viewModel.recommendedPlaylists.collectAsStateWithLifecycle()
    val refreshing by viewModel.recommendationsRefreshing.collectAsStateWithLifecycle()
    val librarySongs by viewModel.librarySongs.collectAsStateWithLifecycle()
    val strings = LocalStrings.current
    val extra = LocalExtraStrings.current
    val isPadLayout = LocalPadLayout.current
    val context = LocalContext.current

    var showCreateDialog by rememberSaveable { mutableStateOf(false) }
    var showAiDialog by rememberSaveable { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<Playlist?>(null) }
    var deleteTarget by remember { mutableStateOf<Playlist?>(null) }
    var mergeTarget by remember { mutableStateOf<Playlist?>(null) }
    var contextPlaylist by remember { mutableStateOf<Playlist?>(null) }
    var openMix by remember { mutableStateOf<RecommendedPlaylist?>(null) }
    val songsById = remember(librarySongs) { librarySongs.associateBy { it.id } }
    val updatedText = recommended.maxOfOrNull { it.updatedAtMs }?.takeIf { it > 0L }?.let { time ->
        extra.recommendedUpdatedFormat.format(
            SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(time))
        )
    }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, top = 8.dp, end = 12.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = strings.playlists,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = { showAiDialog = true },
                enabled = state.aiPlaylistAvailable
            ) {
                Icon(imageVector = Icons.Outlined.AutoAwesome, contentDescription = null)
                Text(text = strings.aiCreatePlaylist)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = { showCreateDialog = true }) {
                Icon(imageVector = Icons.Outlined.Add, contentDescription = null)
                Text(text = strings.newPlaylist)
            }
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(if (isPadLayout) 2 else 1),
            contentPadding = PaddingValues(
                horizontal = if (isPadLayout) 24.dp else 20.dp,
                vertical = 4.dp
            ),
            horizontalArrangement = Arrangement.spacedBy(if (isPadLayout) 16.dp else 0.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Card(
                    onClick = onOpenListeningStats,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Headphones,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = extra.listeningStats, style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = extra.listeningStatsEntryDesc,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = extra.recommendedPlaylists,
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.weight(1f)
                        )
                        if (refreshing) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .padding(8.dp)
                                    .size(18.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            IconButton(onClick = { viewModel.refreshRecommendations(force = true) }) {
                                Icon(
                                    imageVector = Icons.Outlined.Refresh,
                                    contentDescription = extra.refreshRecommendations
                                )
                            }
                        }
                    }
                    Text(
                        text = listOfNotNull(extra.recommendationsHint, updatedText).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
            itemsIndexed(
                items = recommended,
                key = { _, rec -> "rec-${rec.kind.name}-${rec.tag.orEmpty()}" },
                contentType = { _, _ -> "recommended" }
            ) { _, rec ->
                val songCount = rec.songIds.count { it in songsById }
                RecommendedMixCard(
                    recommended = rec,
                    songCount = songCount,
                    onClick = { openMix = rec }
                )
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = strings.playlists,
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
            itemsIndexed(
                items = state.playlists,
                key = { _, p -> p.id },
                contentType = { _, _ -> "playlist" }
            ) { index, playlist ->
                Box {
                    PlaylistCard(
                        playlist = playlist,
                        onClick = { onOpenPlaylist(playlist.id) },
                        onLongClick = { contextPlaylist = playlist },
                        modifier = Modifier.fillMaxWidth()
                    )
                    DropdownMenu(
                        expanded = contextPlaylist?.id == playlist.id,
                        onDismissRequest = { contextPlaylist = null }
                    ) {
                        if (index > 0) {
                            DropdownMenuItem(
                                text = { Text("Move up") },
                                onClick = {
                                    val ids = state.playlists.toMutableList()
                                    ids.removeAt(index)
                                    ids.add(index - 1, playlist)
                                    viewModel.reorderPlaylists(ids.map { it.id })
                                    contextPlaylist = null
                                },
                                leadingIcon = { Icon(Icons.Outlined.ArrowUpward, contentDescription = null) }
                            )
                        }
                        if (index < state.playlists.lastIndex) {
                            DropdownMenuItem(
                                text = { Text("Move down") },
                                onClick = {
                                    val ids = state.playlists.toMutableList()
                                    ids.removeAt(index)
                                    ids.add(index + 1, playlist)
                                    viewModel.reorderPlaylists(ids.map { it.id })
                                    contextPlaylist = null
                                },
                                leadingIcon = { Icon(Icons.Outlined.ArrowDownward, contentDescription = null) }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text(strings.renamePlaylist) },
                            onClick = {
                                renameTarget = playlist
                                contextPlaylist = null
                            },
                            leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text(strings.mergeFromPlaylist) },
                            onClick = {
                                mergeTarget = playlist
                                contextPlaylist = null
                            },
                            leadingIcon = { Icon(Icons.Outlined.Merge, contentDescription = null) }
                        )
                        if (playlist.id != com.musicplayer.app.data.repository.PlaylistRepositoryImpl.LIKED_PLAYLIST_ID) {
                            DropdownMenuItem(
                                text = { Text(strings.deletePlaylist) },
                                onClick = {
                                    deleteTarget = playlist
                                    contextPlaylist = null
                                },
                                leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null) }
                            )
                        }
                    }
                }
            }
        }
    }

    openMix?.let { rec ->
        val mixSongs = rec.songIds.mapNotNull { songsById[it] }
        val mixTitle = recommendedTitle(rec)
        ModalBottomSheet(onDismissRequest = { openMix = null }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, bottom = 24.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = mixTitle,
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Text(
                            text = extra.songsCountFormat.format(mixSongs.size),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = {
                            shareSongs(context, mixTitle, mixSongs)
                        },
                        enabled = mixSongs.isNotEmpty()
                    ) {
                        Icon(Icons.Outlined.Share, contentDescription = strings.share)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        mixSongs.firstOrNull()?.let { onPlaySong(it, mixSongs) }
                        openMix = null
                    },
                    enabled = mixSongs.isNotEmpty()
                ) {
                    Icon(Icons.Outlined.PlayArrow, contentDescription = null)
                    Text(extra.playAll)
                }
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn(modifier = Modifier.heightIn(max = 440.dp)) {
                    itemsIndexed(items = mixSongs, key = { _, song -> song.id }) { _, song ->
                        SongListItem(
                            song = song,
                            onClick = {
                                onPlaySong(song, mixSongs)
                                openMix = null
                            }
                        )
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreatePlaylistDialog(
            onCreate = { name ->
                viewModel.createPlaylist(name)
                showCreateDialog = false
            },
            onDismiss = { showCreateDialog = false }
        )
    }
    if (showAiDialog) {
        AiPlaylistDialog(
            generating = aiPlaylistState == AiPlaylistUiState.Generating,
            onGenerate = { prompt -> viewModel.generateAiPlaylist(prompt) },
            onDismiss = {
                showAiDialog = false
                viewModel.clearAiPlaylistState()
            }
        )
    }
    when (val result = aiPlaylistState) {
        is AiPlaylistUiState.Created -> {
            LaunchedEffect(result.playlistId) {
                showAiDialog = false
                viewModel.clearAiPlaylistState()
                onOpenPlaylist(result.playlistId)
            }
        }
        AiPlaylistUiState.ApiKeyRequired,
        AiPlaylistUiState.Disabled,
        AiPlaylistUiState.EmptyLibrary,
        AiPlaylistUiState.Failed,
        AiPlaylistUiState.NoSelection -> {
            AlertDialog(
                onDismissRequest = viewModel::clearAiPlaylistState,
                title = { Text(strings.aiCreatePlaylist) },
                text = {
                    Text(
                        when (result) {
                            AiPlaylistUiState.ApiKeyRequired,
                            AiPlaylistUiState.Disabled -> strings.aiPlaylistUnavailable
                            AiPlaylistUiState.EmptyLibrary -> strings.noResultsFound
                            AiPlaylistUiState.NoSelection -> strings.aiPlaylistNoSelection
                            AiPlaylistUiState.Failed -> strings.aiPlaylistFailed
                            else -> strings.aiPlaylistFailed
                        }
                    )
                },
                confirmButton = {
                    TextButton(onClick = viewModel::clearAiPlaylistState) {
                        Text(strings.confirm)
                    }
                }
            )
        }
        AiPlaylistUiState.Generating,
        AiPlaylistUiState.Idle -> Unit
    }
    renameTarget?.let { playlist ->
        RenamePlaylistDialog(
            currentName = playlist.name,
            onRename = { name ->
                viewModel.renamePlaylist(playlist.id, name)
                renameTarget = null
            },
            onDismiss = { renameTarget = null }
        )
    }
    deleteTarget?.let { playlist ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text(strings.deletePlaylist) },
            text = { Text(strings.deletePlaylistConfirm) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deletePlaylist(playlist.id)
                    deleteTarget = null
                }) { Text(strings.deletePlaylist) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text(strings.cancel) }
            }
        )
    }
    mergeTarget?.let { target ->
        MergePlaylistDialog(
            allPlaylists = state.playlists.filter { it.id != target.id },
            onMerge = { sourceId ->
                viewModel.addSongsFromPlaylist(target.id, sourceId)
                mergeTarget = null
            },
            onDismiss = { mergeTarget = null }
        )
    }
}

@Composable
private fun recommendedTitle(rec: RecommendedPlaylist): String {
    val extra = LocalExtraStrings.current
    return when (rec.kind) {
        RecommendedPlaylistKind.FOR_YOU -> extra.recommendedForYou
        RecommendedPlaylistKind.RECENT -> extra.recentlyPlayedMix
        RecommendedPlaylistKind.TAG -> extra.tagMixFormat.format(rec.tag.orEmpty())
        RecommendedPlaylistKind.NEW -> extra.newArrivalsMix
    }
}

@Composable
private fun recommendedIcon(kind: RecommendedPlaylistKind): ImageVector = when (kind) {
    RecommendedPlaylistKind.FOR_YOU -> Icons.Outlined.AutoAwesome
    RecommendedPlaylistKind.RECENT -> Icons.Outlined.History
    RecommendedPlaylistKind.TAG -> Icons.Outlined.Label
    RecommendedPlaylistKind.NEW -> Icons.Outlined.NewReleases
}

@Composable
private fun RecommendedMixCard(
    recommended: RecommendedPlaylist,
    songCount: Int,
    onClick: () -> Unit
) {
    val extra = LocalExtraStrings.current
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(
                    imageVector = recommendedIcon(recommended.kind),
                    contentDescription = null,
                    modifier = Modifier.padding(10.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = recommendedTitle(recommended),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = extra.songsCountFormat.format(songCount),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun AiPlaylistDialog(
    generating: Boolean,
    onGenerate: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalStrings.current
    var prompt by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = { if (!generating) onDismiss() },
        title = { Text(strings.aiCreatePlaylist) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = prompt,
                    onValueChange = { prompt = it },
                    label = { Text(strings.aiPlaylistPrompt) },
                    enabled = !generating,
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                if (generating) {
                    Text(
                        text = strings.aiPlaylistGenerating,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onGenerate(prompt) },
                enabled = prompt.isNotBlank() && !generating
            ) {
                Text(strings.aiCreatePlaylist)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !generating) {
                Text(strings.cancel)
            }
        }
    )
}

@Composable
private fun CreatePlaylistDialog(
    onCreate: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalStrings.current
    var name by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.newPlaylist) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(strings.playlistName) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onCreate(name.ifBlank { strings.newPlaylist }) },
                enabled = true
            ) { Text(strings.newPlaylist) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.cancel) }
        }
    )
}

@Composable
private fun RenamePlaylistDialog(
    currentName: String,
    onRename: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalStrings.current
    var name by rememberSaveable { mutableStateOf(currentName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.renamePlaylist) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(strings.playlistName) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onRename(name.ifBlank { currentName }) },
                enabled = true
            ) { Text(strings.renamePlaylist) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.cancel) }
        }
    )
}

@Composable
private fun MergePlaylistDialog(
    allPlaylists: List<Playlist>,
    onMerge: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalStrings.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.mergeFromPlaylist) },
        text = {
            if (allPlaylists.isEmpty()) {
                Text(strings.mergeFromPlaylistDesc)
            } else {
                Column {
                    Text(
                        text = strings.mergeFromPlaylistDesc,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    allPlaylists.forEach { playlist ->
                        TextButton(
                            onClick = { onMerge(playlist.id) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "${playlist.name} (${playlist.songCount})",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.close) }
        }
    )
}
