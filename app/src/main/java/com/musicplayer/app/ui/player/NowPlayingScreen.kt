package com.musicplayer.app.ui.player

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Album
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.QueueMusic
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.SkipPrevious
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.musicplayer.app.data.local.entity.CommentEntity
import com.musicplayer.app.domain.model.ArtworkCandidate
import com.musicplayer.app.domain.model.LyricsCandidate
import com.musicplayer.app.domain.model.MetadataProvider
import com.musicplayer.app.domain.model.ScrapeCandidates
import com.musicplayer.app.domain.model.PlaybackMode
import com.musicplayer.app.domain.model.PlaybackState
import com.musicplayer.app.domain.model.Song
import com.musicplayer.app.ui.i18n.LocalStrings

@Composable
fun NowPlayingScreen(
    playbackState: PlaybackState,
    controlsState: PlayerControlsState,
    onSkipPrevious: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onQueueSongSelected: (Song) -> Unit,
    onPlaybackModeChange: (PlaybackMode) -> Unit,
    onPresetSelected: (EqualizerPreset) -> Unit,
    onDraftNameChange: (String) -> Unit,
    onBandLevelChange: (Int, Float) -> Unit,
    onCreatePreset: () -> Unit,
    onSavePreset: () -> Unit,
    onDeletePreset: () -> Unit,
    onCloseEqualizer: () -> Unit,
    onOpenScrapeDetail: () -> Unit = {},
    showCommentDialog: Boolean = false,
    comments: List<CommentEntity> = emptyList(),
    onOpenComments: () -> Unit = {},
    onSaveComment: (Int, String) -> Unit = { _, _ -> },
    onDismissComments: () -> Unit = {},
    showCandidateDialog: Boolean = false,
    candidates: ScrapeCandidates? = null,
    onOpenCandidatePicker: () -> Unit = {},
    onApplyCandidateResult: (LyricsCandidate?, ArtworkCandidate?) -> Unit = { _, _ -> },
    onDismissCandidatePicker: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    val song = playbackState.currentSong
    var activePanel by rememberSaveable { mutableStateOf(PlayerPanel.NONE) }
    var showQueue by rememberSaveable { mutableStateOf(false) }
    var lyricsMode by rememberSaveable(song?.id) { mutableStateOf(false) }

    if (lyricsMode) {
        LyricsMode(
            song = song,
            positionMs = playbackState.positionMs,
            onSeekTo = onSeekTo,
            onExit = { lyricsMode = false },
            modifier = modifier
        )
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(0.15f))
        AlbumArtwork(
            song = song,
            onClick = { if (song != null) lyricsMode = true }
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = song?.title ?: strings.nothingPlaying,
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = song?.let { "${it.artistName} - ${it.albumTitle}" } ?: strings.chooseSong,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            song?.let { s ->
                val parts = buildList {
                    s.qualityLabel.takeIf { it.isNotBlank() && it != "Unknown" }?.let { add(it) }
                    s.releaseYear?.takeIf { it > 0 }?.let { year ->
                        val dateStr = if (s.releaseMonth != null && s.releaseMonth > 0) {
                            "${year}-${s.releaseMonth.toString().padStart(2, '0')}"
                        } else {
                            "$year"
                        }
                        add(dateStr)
                    }
                }
                if (parts.isNotEmpty()) {
                    Text(
                        text = parts.joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        Spacer(modifier = Modifier.weight(0.1f))
        LyricsPreview(
            lyrics = song?.lyrics,
            positionMs = playbackState.positionMs,
            modifier = Modifier.padding(vertical = 12.dp)
        )
        Spacer(modifier = Modifier.weight(0.1f))
        PlayerPanelSurface(activePanel) {
            when (activePanel) {
                PlayerPanel.EQUALIZER -> EqualizerPanel(
                    state = controlsState,
                    onPresetSelected = onPresetSelected,
                    onDraftNameChange = onDraftNameChange,
                    onBandLevelChange = onBandLevelChange,
                    onCreatePreset = onCreatePreset,
                    onSavePreset = onSavePreset,
                    onDeletePreset = onDeletePreset,
                    onClose = {
                        onCloseEqualizer()
                        activePanel = PlayerPanel.NONE
                    }
                )

                PlayerPanel.NONE -> Unit
            }
        }
        PanelButtonRow(
            enabled = song != null,
            hasQueue = playbackState.queue.isNotEmpty(),
            activePanel = activePanel,
            playbackMode = playbackState.playbackMode,
            onScrapeClick = onOpenScrapeDetail,
            onModeClick = {
                val nextMode = when (playbackState.playbackMode) {
                    PlaybackMode.SEQUENTIAL -> PlaybackMode.SHUFFLE
                    PlaybackMode.SHUFFLE -> PlaybackMode.RECOMMENDATION
                    PlaybackMode.RECOMMENDATION -> PlaybackMode.SEQUENTIAL
                }
                onPlaybackModeChange(nextMode)
            },
            onEqualizerClick = {
                activePanel = if (activePanel == PlayerPanel.EQUALIZER) PlayerPanel.NONE else PlayerPanel.EQUALIZER
            },
            onQueueClick = { showQueue = true },
            onCommentsClick = onOpenComments
        )
        PlaybackProgress(
            positionMs = playbackState.positionMs,
            durationMs = playbackState.durationMs,
            enabled = song != null,
            onSeekTo = onSeekTo,
            modifier = Modifier.padding(top = 8.dp)
        )
        PlayerTransport(
            enabled = song != null,
            isPlaying = playbackState.isPlaying,
            onSkipPrevious = onSkipPrevious,
            onTogglePlayPause = onTogglePlayPause,
            onSkipNext = onSkipNext
        )
    }

    if (showQueue) {
        QueueDialog(
            queue = playbackState.queue.ifEmpty { song?.let { listOf(it) }.orEmpty() },
            currentSongId = song?.id,
            playbackMode = playbackState.playbackMode,
            onSongSelected = onQueueSongSelected,
            onPlaybackModeChange = onPlaybackModeChange,
            onDismiss = { showQueue = false }
        )
    }

    if (showCommentDialog) {
        CommentDialog(
            comments = comments,
            onSave = onSaveComment,
            onDismiss = onDismissComments
        )
    }

    if (showCandidateDialog) {
        CandidateDialog(
            candidates = candidates,
            onApply = onApplyCandidateResult,
            onDismiss = onDismissCandidatePicker
        )
    }
}

@Composable
private fun AlbumArtwork(
    song: Song?,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth(0.82f)
            .aspectRatio(1f)
            .widthIn(max = 360.dp)
            .clickable(enabled = song != null, onClick = onClick),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        if (!song?.artworkUri.isNullOrBlank()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(song?.artworkUri)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Outlined.Album,
                    contentDescription = null,
                    modifier = Modifier.size(96.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
private fun PlaybackProgress(
    positionMs: Long,
    durationMs: Long,
    enabled: Boolean,
    onSeekTo: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var dragging by remember { mutableStateOf(false) }
    var sliderPosition by remember { mutableFloatStateOf(0f) }
    val duration = durationMs.coerceAtLeast(0L)
    val rangeEnd = duration.takeIf { it > 0L }?.toFloat() ?: 1f

    LaunchedEffect(positionMs, durationMs, dragging) {
        if (!dragging) sliderPosition = positionMs.coerceIn(0L, duration.takeIf { it > 0L } ?: positionMs).toFloat()
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Slider(
            value = sliderPosition.coerceIn(0f, rangeEnd),
            onValueChange = {
                dragging = true
                sliderPosition = it
            },
            onValueChangeFinished = {
                dragging = false
                onSeekTo(sliderPosition.toLong())
            },
            enabled = enabled && duration > 0L,
            valueRange = 0f..rangeEnd
        )
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = formatDuration(sliderPosition.toLong()),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = formatDuration(duration),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PanelButtonRow(
    enabled: Boolean,
    hasQueue: Boolean,
    activePanel: PlayerPanel,
    playbackMode: PlaybackMode,
    onScrapeClick: () -> Unit,
    onModeClick: () -> Unit,
    onEqualizerClick: () -> Unit,
    onQueueClick: () -> Unit,
    onCommentsClick: () -> Unit = {}
) {
    val strings = LocalStrings.current
    val modeTint = MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        PanelButton(
            selected = false,
            enabled = enabled,
            onClick = onScrapeClick
        ) {
            Icon(Icons.Outlined.Search, contentDescription = strings.currentTrackScrape)
        }
        IconButton(
            onClick = onModeClick,
            modifier = Modifier.size(44.dp)
        ) {
            when (playbackMode) {
                PlaybackMode.SEQUENTIAL -> SequentialIcon(modeTint)
                PlaybackMode.SHUFFLE -> Icon(Icons.Outlined.Shuffle, contentDescription = strings.shuffle, tint = modeTint)
                PlaybackMode.RECOMMENDATION -> RecommendationIcon(modeTint)
            }
        }
        PanelButton(
            selected = activePanel == PlayerPanel.EQUALIZER,
            onClick = onEqualizerClick
        ) {
            Icon(Icons.Outlined.GraphicEq, contentDescription = strings.equalizer)
        }
        IconButton(
            onClick = onCommentsClick,
            enabled = enabled,
            modifier = Modifier.size(44.dp)
        ) {
            Icon(Icons.Outlined.StarBorder, contentDescription = strings.comments)
        }
        IconButton(
            onClick = onQueueClick,
            enabled = hasQueue,
            modifier = Modifier.size(44.dp)
        ) {
            Icon(Icons.Outlined.QueueMusic, contentDescription = strings.playlist)
        }
    }
}

@Composable
private fun PlayerTransport(
    enabled: Boolean,
    isPlaying: Boolean,
    onSkipPrevious: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSkipNext: () -> Unit
) {
    val strings = LocalStrings.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onSkipPrevious,
            enabled = enabled,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(Icons.Outlined.SkipPrevious, contentDescription = strings.previous)
        }
        FilledIconButton(
            onClick = onTogglePlayPause,
            enabled = enabled,
            modifier = Modifier.size(60.dp)
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                contentDescription = if (isPlaying) strings.pause else strings.play
            )
        }
        IconButton(
            onClick = onSkipNext,
            enabled = enabled,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(Icons.Outlined.SkipNext, contentDescription = strings.next)
        }
    }
}

@Composable
private fun PanelButton(
    selected: Boolean,
    onClick: () -> Unit,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    val tint = if (selected) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.onSurfaceVariant
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(44.dp)
    ) {
        CompositionLocalProvider(
            LocalContentColor provides tint,
            content = content
        )
    }
}

@Composable
private fun PlayerPanelSurface(
    activePanel: PlayerPanel,
    content: @Composable () -> Unit
) {
    if (activePanel == PlayerPanel.NONE) return
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 380.dp),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Box(modifier = Modifier.padding(16.dp)) {
            content()
        }
    }
}

@Composable
private fun QueueDialog(
    queue: List<Song>,
    currentSongId: String?,
    playbackMode: PlaybackMode,
    onSongSelected: (Song) -> Unit,
    onPlaybackModeChange: (PlaybackMode) -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalStrings.current
    val currentIndex = queue.indexOfFirst { it.id == currentSongId }.coerceAtLeast(0)
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = currentIndex)

    LaunchedEffect(currentIndex, queue.size) {
        if (queue.isNotEmpty()) listState.scrollToItem(currentIndex)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                Column(modifier = Modifier.padding(vertical = 12.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Outlined.Close, contentDescription = strings.close)
                        }
                        Text(
                            text = strings.playlist,
                            style = MaterialTheme.typography.titleMedium
                        )
                        IconButton(
                            onClick = {
                                val nextMode = when (playbackMode) {
                                    PlaybackMode.SEQUENTIAL -> PlaybackMode.SHUFFLE
                                    PlaybackMode.SHUFFLE -> PlaybackMode.RECOMMENDATION
                                    PlaybackMode.RECOMMENDATION -> PlaybackMode.SEQUENTIAL
                                }
                                onPlaybackModeChange(nextMode)
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            val tint = MaterialTheme.colorScheme.onSurface
                            when (playbackMode) {
                                PlaybackMode.SEQUENTIAL -> SequentialIcon(tint)
                                PlaybackMode.SHUFFLE -> Icon(
                                    Icons.Outlined.Shuffle,
                                    contentDescription = strings.shuffle,
                                    tint = tint
                                )
                                PlaybackMode.RECOMMENDATION -> RecommendationIcon(tint)
                            }
                        }
                    }
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        itemsIndexed(queue, key = { _, song -> song.id }) { index, song ->
                            val selected = song.id == currentSongId
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onSongSelected(song)
                                        onDismiss()
                                    },
                                shape = MaterialTheme.shapes.medium,
                                color = if (selected) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surface
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        text = (index + 1).toString(),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.width(26.dp),
                                        textAlign = TextAlign.Center
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = song.title,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${song.artistName} - ${song.albumTitle}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Text(
                                        text = formatDuration(song.durationMs),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
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

@Composable
private fun CommentDialog(
    comments: List<CommentEntity>,
    onSave: (Int, String) -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalStrings.current
    var rating by remember { mutableStateOf(0) }
    var text by remember { mutableStateOf("") }
    val avgRating = if (comments.isNotEmpty()) comments.map { it.rating }.average() else 0.0

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = strings.comments, style = MaterialTheme.typography.titleMedium)
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Outlined.Close, contentDescription = strings.close)
                    }
                }
                if (comments.isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        repeat(5) { i ->
                            Icon(
                                imageVector = if (i < avgRating.toInt()) Icons.Outlined.Star
                                else Icons.Outlined.StarBorder,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Text(
                            text = "%.1f".format(avgRating),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "(${comments.size})",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (comments.isNotEmpty()) {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 200.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(comments, key = { it.id }) { comment ->
                            Surface(
                                shape = MaterialTheme.shapes.medium,
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        repeat(5) { i ->
                                            Icon(
                                                imageVector = if (i < comment.rating) Icons.Outlined.Star
                                                else Icons.Outlined.StarBorder,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                    if (comment.text.isNotBlank()) {
                                        Text(
                                            text = comment.text,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                    Text(
                                        text = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                                            .format(java.util.Date(comment.timestampEpochMs)),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Text(
                        text = strings.noComments,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = strings.addComment,
                    style = MaterialTheme.typography.titleMedium
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    repeat(5) { i ->
                        IconButton(
                            onClick = { rating = i + 1 },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = if (i < rating) Icons.Outlined.Star
                                else Icons.Outlined.StarBorder,
                                contentDescription = "${i + 1}",
                                tint = if (i < rating) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(strings.commentText) }
                )
                Button(
                    onClick = {
                        if (rating > 0) {
                            onSave(rating, text)
                            text = ""
                            rating = 0
                        }
                    },
                    enabled = rating > 0,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(strings.saveComment)
                }
            }
        }
    }
}

@Composable
private fun CandidateDialog(
    candidates: ScrapeCandidates?,
    onApply: (LyricsCandidate?, ArtworkCandidate?) -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalStrings.current
    var selectedLyrics by remember { mutableStateOf<LyricsCandidate?>(null) }
    var selectedArtwork by remember { mutableStateOf<ArtworkCandidate?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = strings.currentTrackScrape, style = MaterialTheme.typography.titleMedium)
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Outlined.Close, contentDescription = strings.close)
                    }
                }

                if (candidates == null) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Text(
                        text = strings.scrapeRunning,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    if (candidates.lyrics.isNotEmpty()) {
                        Text(
                            text = "${strings.commentText}:",
                            style = MaterialTheme.typography.titleSmall
                        )
                        LazyColumn(
                            modifier = Modifier.heightIn(max = 160.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(candidates.lyrics, key = { it.provider }) { result ->
                                val selected = selectedLyrics?.provider == result.provider
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedLyrics = if (selected) null else result
                                        },
                                    shape = MaterialTheme.shapes.medium,
                                    color = if (selected) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = result.provider,
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "Score: ${result.score}",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Text(
                                            text = result.lyrics.take(100).replace("\n", " "),
                                            style = MaterialTheme.typography.bodySmall,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (candidates.artworks.isNotEmpty()) {
                        Text(
                            text = "Artwork:",
                            style = MaterialTheme.typography.titleSmall
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(candidates.artworks, key = { it.provider }) { result ->
                                val selected = selectedArtwork?.provider == result.provider
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .clickable {
                                            selectedArtwork = if (selected) null else result
                                        }
                                        .padding(4.dp)
                                ) {
                                    Surface(
                                        modifier = Modifier.size(72.dp),
                                        shape = MaterialTheme.shapes.medium,
                                        color = if (selected) MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    ) {
                                        AsyncImage(
                                            model = ImageRequest.Builder(LocalContext.current)
                                                .data(result.imageUri)
                                                .crossfade(true)
                                                .build(),
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                    Text(
                                        text = result.provider,
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    if (candidates.lyrics.isEmpty() && candidates.artworks.isEmpty()) {
                        Text(
                            text = "No results found",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Button(
                        onClick = { onApply(selectedLyrics, selectedArtwork) },
                        enabled = selectedLyrics != null || selectedArtwork != null,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(strings.saveComment)
                    }
                }
            }
        }
    }
}

@Composable
private fun LyricsMode(
    song: Song?,
    positionMs: Long,
    onSeekTo: (Long) -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    val timedLines = remember(song?.lyrics) { parseTimedLyrics(song?.lyrics) }

    val view = LocalView.current
    val windowInsetsController = remember(view) {
        view.getActivityWindow()?.let { WindowCompat.getInsetsController(it, view) }
    }

    LaunchedEffect(Unit) {
        windowInsetsController?.hide(WindowInsetsCompat.Type.statusBars())
        windowInsetsController?.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    DisposableEffect(Unit) {
        onDispose {
            windowInsetsController?.show(WindowInsetsCompat.Type.statusBars())
        }
    }

    val currentIndex = remember(timedLines, positionMs) {
        timedLines.indexOfLast { it.timeMs <= positionMs }.coerceAtLeast(0)
    }
    val listState = rememberLazyListState()

    LaunchedEffect(currentIndex, timedLines.size) {
        if (timedLines.isNotEmpty()) {
            listState.animateScrollToItem(currentIndex.coerceIn(0, timedLines.lastIndex))
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .clickable(onClick = onExit)
            .padding(horizontal = 28.dp, vertical = 48.dp)
    ) {
        if (timedLines.isEmpty()) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = song?.title ?: strings.nothingPlaying,
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = song?.lyricsProvider ?: strings.chooseSong,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 96.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                itemsIndexed(timedLines, key = { index, _ -> index }) { index, line ->
                    val isCurrent = index == currentIndex
                    Text(
                        text = line.text,
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center,
                        color = if (isCurrent) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.pointerInput(line.timeMs) {
                            detectTapGestures(
                                onDoubleTap = { onSeekTo(line.timeMs) }
                            )
                        }
                    )
                }
            }
        }
    }
}

private fun android.view.View.getActivityWindow(): android.view.Window? {
    return context.getActivity()?.window
}

private fun android.content.Context.getActivity(): android.app.Activity? {
    var ctx = this
    while (ctx is android.content.ContextWrapper) {
        if (ctx is android.app.Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

@Composable
private fun SequentialIcon(tint: androidx.compose.ui.graphics.Color) {
    Canvas(modifier = Modifier.size(24.dp)) {
        val w = size.width
        val h = size.height
        val arrowH = h * 0.35f
        val arrowW = w * 0.48f
        val gap = w * 0.10f
        val arrowHead = w * 0.18f
        val strokeW = 2.5f
        val arrowColor = tint
        for (i in 0..1) {
            val y = h * 0.15f + i * (arrowH + gap)
            val left = w * 0.05f
            val right = left + arrowW
            // Arrow shaft
            drawLine(arrowColor, androidx.compose.ui.geometry.Offset(left, y + arrowH / 2), androidx.compose.ui.geometry.Offset(right, y + arrowH / 2), strokeW)
            // Arrowhead
            drawLine(arrowColor, androidx.compose.ui.geometry.Offset(right, y + arrowH / 2), androidx.compose.ui.geometry.Offset(right - arrowHead, y), strokeW)
            drawLine(arrowColor, androidx.compose.ui.geometry.Offset(right, y + arrowH / 2), androidx.compose.ui.geometry.Offset(right - arrowHead, y + arrowH), strokeW)
        }
    }
}

@Composable
private fun RecommendationIcon(tint: androidx.compose.ui.graphics.Color) {
    Canvas(modifier = Modifier.size(24.dp)) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val heartPath = androidx.compose.ui.graphics.Path().apply {
            moveTo(cx, h * 0.25f)
            lineTo(cx, h * 0.8f)
            lineTo(w * 0.12f, h * 0.5f)
            cubicTo(w * 0.02f, h * 0.4f, w * 0.05f, h * 0.15f, cx * 0.55f, h * 0.15f)
            cubicTo(cx * 0.75f, h * 0.15f, cx * 0.9f, h * 0.28f, cx, h * 0.42f)
            cubicTo(cx * 1.1f, h * 0.28f, cx * 1.25f, h * 0.15f, cx * 1.45f, h * 0.15f)
            cubicTo(cx * 1.95f, h * 0.15f, cx * 1.98f, h * 0.4f, cx * 1.88f, h * 0.5f)
            lineTo(cx, h * 0.8f)
            close()
        }
        drawPath(heartPath, tint)
        val ecgY = h * 0.32f
        val ecgPath = androidx.compose.ui.graphics.Path().apply {
            moveTo(w * 0.3f, ecgY)
            lineTo(w * 0.4f, ecgY)
            lineTo(w * 0.45f, ecgY - h * 0.08f)
            lineTo(w * 0.5f, ecgY + h * 0.1f)
            lineTo(w * 0.55f, ecgY - h * 0.12f)
            lineTo(w * 0.6f, ecgY + h * 0.04f)
            lineTo(w * 0.65f, ecgY)
            lineTo(w * 0.72f, ecgY)
        }
        drawPath(
            ecgPath,
            color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.7f),
            style = Stroke(width = 2f)
        )
    }
}

@Composable
private fun EqualizerPanel(
    state: PlayerControlsState,
    onPresetSelected: (EqualizerPreset) -> Unit,
    onDraftNameChange: (String) -> Unit,
    onBandLevelChange: (Int, Float) -> Unit,
    onCreatePreset: () -> Unit,
    onSavePreset: () -> Unit,
    onDeletePreset: () -> Unit,
    onClose: () -> Unit
) {
    val strings = LocalStrings.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = strings.equalizer, style = MaterialTheme.typography.titleMedium)
            IconButton(onClick = onClose, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Outlined.Close, contentDescription = strings.close)
            }
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.presets, key = { it.id }) { preset ->
                FilterChip(
                    selected = state.selectedPresetId == preset.id,
                    onClick = { onPresetSelected(preset) },
                    label = { Text(preset.displayName(strings), maxLines = 1, overflow = TextOverflow.Ellipsis) }
                )
            }
        }
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 140.dp)
        ) {
            itemsIndexed(state.bandLevels) { index, level ->
                val freq = state.bandFrequencies.getOrElse(index) { 0 }
                val freqLabel = if (freq >= 1000) "${freq / 1000}.${(freq % 1000) / 100}kHz" else "${freq}Hz"
                EqualizerBandSlider(
                    freqLabel = freqLabel,
                    value = level,
                    onValueChange = { onBandLevelChange(index, it) }
                )
            }
        }
        OutlinedTextField(
            value = state.draftName,
            onValueChange = onDraftNameChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text(strings.presetName) }
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onCreatePreset, modifier = Modifier.weight(1f)) {
                Text(strings.newPreset, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Button(
                onClick = onSavePreset,
                enabled = state.canDelete,
                modifier = Modifier.weight(1f)
            ) {
                Text(strings.savePreset, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Button(
                onClick = onDeletePreset,
                enabled = state.canDelete,
                modifier = Modifier.weight(1f)
            ) {
                Text(strings.deletePreset, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun EqualizerBandSlider(
    freqLabel: String,
    value: Float,
    onValueChange: (Float) -> Unit
) {
    val trackHeight = 130.dp
    val trackWidth = 6.dp
    val thumbRadius = 10.dp
    val valueRange = -6f..6f
    val inactiveColor = MaterialTheme.colorScheme.surfaceVariant
    val activeColor = MaterialTheme.colorScheme.primary
    val currentValue by rememberUpdatedState(value)
    val currentOnValueChange by rememberUpdatedState(onValueChange)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(0.dp),
        modifier = Modifier.widthIn(min = 36.dp, max = 48.dp)
    ) {
        Text(
            text = "${value.toInt()}dB",
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center
        )
        Box(
            modifier = Modifier
                .height(trackHeight + thumbRadius * 2)
                .width(36.dp)
                .pointerInput(Unit) {
                    detectVerticalDragGestures { _, dragAmount ->
                        val fraction = -dragAmount / (trackHeight.value * density)
                        val newValue = (currentValue + fraction * (valueRange.endInclusive - valueRange.start))
                            .coerceIn(valueRange.start, valueRange.endInclusive)
                        currentOnValueChange(newValue)
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val fraction = 1f - (offset.y / size.height).coerceIn(0f, 1f)
                        val newValue = valueRange.start + fraction * (valueRange.endInclusive - valueRange.start)
                        currentOnValueChange(newValue)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val trackLeft = (size.width - trackWidth.toPx()) / 2
                val trackTop = thumbRadius.toPx()
                val trackBottom = size.height - thumbRadius.toPx()
                val fraction = (value - valueRange.start) / (valueRange.endInclusive - valueRange.start)
                val thumbY = trackBottom - fraction * (trackBottom - trackTop)

                // Inactive track
                drawRoundRect(
                    color = inactiveColor,
                    topLeft = Offset(trackLeft, trackTop),
                    size = Size(trackWidth.toPx(), trackBottom - trackTop),
                    cornerRadius = CornerRadius(trackWidth.toPx() / 2)
                )
                // Active track
                drawRoundRect(
                    color = activeColor,
                    topLeft = Offset(trackLeft, thumbY),
                    size = Size(trackWidth.toPx(), trackBottom - thumbY),
                    cornerRadius = CornerRadius(trackWidth.toPx() / 2)
                )
                // Thumb
                drawCircle(
                    color = activeColor,
                    radius = thumbRadius.toPx(),
                    center = Offset(size.width / 2, thumbY)
                )
            }
        }
        Text(
            text = freqLabel,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun PlaybackMode.title(strings: com.musicplayer.app.ui.i18n.AppStrings): String {
    return when (this) {
        PlaybackMode.SEQUENTIAL -> strings.sequential
        PlaybackMode.SHUFFLE -> strings.shuffle
        PlaybackMode.RECOMMENDATION -> strings.recommendation
    }
}

private fun EqualizerPreset.displayName(strings: com.musicplayer.app.ui.i18n.AppStrings): String {
    return when (id) {
        "flat" -> strings.eqFlat
        "vocal" -> strings.eqVocal
        "bass" -> strings.eqBassBoost
        "pop" -> strings.eqPop
        "rock" -> strings.eqRock
        "classical" -> strings.eqClassical
        else -> name
    }
}

private enum class PlayerPanel {
    NONE,
    EQUALIZER
}

private fun MetadataProvider.title(strings: com.musicplayer.app.ui.i18n.AppStrings): String {
    return when (this) {
        MetadataProvider.AUTO -> strings.autoSource
        MetadataProvider.LRCLIB -> "LRCLIB"
        MetadataProvider.NETEASE -> "NetEase Cloud Music"
        MetadataProvider.MEGALOBIZ -> "Megalobiz"
        MetadataProvider.LYRICS_OVH -> "lyrics.ovh"
        MetadataProvider.MUSICBRAINZ_COVER_ART -> "MusicBrainz / CAA"
        MetadataProvider.ITUNES -> "Apple Music"
        MetadataProvider.DEEZER -> "Deezer"
        MetadataProvider.QQ_MUSIC -> "QQ Music"
        MetadataProvider.KUGOU -> "KuGou"
        MetadataProvider.KUWO -> "Kuwo"
    }
}

private fun formatDuration(durationMs: Long): String {
    if (durationMs <= 0L) return "--:--"
    val totalSeconds = durationMs / 1000L
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0L) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }
}

private data class LyricLine(val timeMs: Long, val text: String)

private fun parseTimedLyrics(lyrics: String?): List<LyricLine> {
    val timestampRegex = Regex("\\[(\\d{1,2}):(\\d{2})(?:\\.(\\d{1,3}))?]")
    return lyrics.orEmpty()
        .lineSequence()
        .mapNotNull { line ->
            val match = timestampRegex.find(line) ?: return@mapNotNull null
            val minutes = match.groupValues[1].toLong()
            val seconds = match.groupValues[2].toLong()
            val millis = match.groupValues[3].takeIf { it.isNotEmpty() }?.let {
                it.padEnd(3, '0').take(3).toLong()
            } ?: 0L
            val timeMs = minutes * 60_000L + seconds * 1_000L + millis
            val text = line.replace(timestampRegex, "").trim()
            if (text.isNotBlank()) LyricLine(timeMs, text) else null
        }
        .sortedBy { it.timeMs }
        .toList()
}

@Composable
private fun LyricsPreview(
    lyrics: String?,
    positionMs: Long,
    modifier: Modifier = Modifier
) {
    val timedLines = remember(lyrics) { parseTimedLyrics(lyrics) }
    if (timedLines.isEmpty()) return

    val currentIndex = remember(timedLines, positionMs) {
        timedLines.indexOfLast { it.timeMs <= positionMs }.coerceAtLeast(0)
    }
    val currentLine = timedLines.getOrNull(currentIndex)
    val nextLine = timedLines.getOrNull(currentIndex + 1)

    Column(
        modifier = modifier.fillMaxWidth().heightIn(min = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        currentLine?.let { line ->
            Text(
                text = line.text,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        nextLine?.let { line ->
            Text(
                text = line.text,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun lyricLines(lyrics: String?): List<String> {
    return lyrics.orEmpty()
        .lineSequence()
        .map { line ->
            line.replace(Regex("\\[\\d{1,2}:\\d{2}(?:\\.\\d{1,3})?]"), "").trim()
        }
        .filter { it.isNotBlank() }
        .toList()
}
