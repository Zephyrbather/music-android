package com.musicplayer.app.ui.player

import android.media.MediaPlayer
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.musicplayer.app.data.karaoke.KaraokeRecording
import com.musicplayer.app.data.karaoke.karaokeDirectory
import com.musicplayer.app.ui.i18n.LocalKaraokeStrings
import com.musicplayer.app.ui.i18n.LocalStrings
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.ceil

@Composable
internal fun KaraokeRecordingsDialog(
    recordings: List<KaraokeRecording>,
    onDelete: (KaraokeRecording) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val strings = LocalStrings.current
    val karaokeStrings = LocalKaraokeStrings.current
    val playerState = remember { mutableStateOf<MediaPlayer?>(null) }
    var playingFileName by remember { mutableStateOf<String?>(null) }
    var pendingDelete by remember { mutableStateOf<KaraokeRecording?>(null) }

    fun stopPlayback() {
        playerState.value?.let { player ->
            runCatching { player.stop() }
            runCatching { player.release() }
        }
        playerState.value = null
        playingFileName = null
    }

    DisposableEffect(Unit) {
        onDispose {
            playerState.value?.release()
            playerState.value = null
        }
    }

    fun togglePlayback(recording: KaraokeRecording) {
        if (playingFileName == recording.fileName) {
            runCatching { playerState.value?.pause() }
            playingFileName = null
            return
        }
        stopPlayback()
        val file = recording.file(context)
        if (!file.exists()) return
        val player = MediaPlayer()
        val started = runCatching {
            player.setDataSource(file.absolutePath)
            player.setOnCompletionListener {
                playingFileName = null
                playerState.value = null
                runCatching { player.release() }
            }
            player.prepare()
            player.start()
        }.isSuccess
        if (!started) {
            runCatching { player.release() }
            return
        }
        playerState.value = player
        playingFileName = recording.fileName
    }

    Dialog(
        onDismissRequest = {
            stopPlayback()
            onDismiss()
        }
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 6.dp,
            modifier = Modifier
                .widthIn(max = 640.dp)
                .fillMaxWidth(0.92f)
        ) {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Mic,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.size(10.dp))
                    Text(
                        text = karaokeStrings.recordings,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = {
                        stopPlayback()
                        onDismiss()
                    }) {
                        Icon(Icons.Outlined.Close, contentDescription = strings.close)
                    }
                }
                Spacer(Modifier.height(8.dp))
                if (recordings.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = karaokeStrings.noRecordings,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 460.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        items(recordings, key = { it.fileName }) { recording ->
                            val isPlaying = playingFileName == recording.fileName
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(MaterialTheme.shapes.medium)
                                    .clickable { togglePlayback(recording) }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = { togglePlayback(recording) }) {
                                    Icon(
                                        imageVector = if (isPlaying) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                                        contentDescription = if (isPlaying) karaokeStrings.pause else karaokeStrings.play,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = recording.title.ifBlank { recording.fileName },
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = listOfNotNull(
                                            recording.artist.takeIf { it.isNotBlank() },
                                            formatKaraokeDate(recording.createdAt),
                                            formatKaraokeTime(recording.durationMs)
                                        ).joinToString(" · "),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                recording.score?.let { score ->
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.secondaryContainer
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.Star,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(Modifier.size(4.dp))
                                            Text(
                                                text = score.toString(),
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer
                                            )
                                        }
                                    }
                                    Spacer(Modifier.size(6.dp))
                                }
                                IconButton(onClick = { pendingDelete = recording }) {
                                    Icon(
                                        imageVector = Icons.Outlined.Delete,
                                        contentDescription = karaokeStrings.delete,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    pendingDelete?.let { recording ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            text = { Text(karaokeStrings.deleteConfirm) },
            confirmButton = {
                TextButton(onClick = {
                    if (playingFileName == recording.fileName) stopPlayback()
                    onDelete(recording)
                    pendingDelete = null
                }) {
                    Text(karaokeStrings.delete)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text(strings.cancel)
                }
            }
        )
    }
}

@Composable
internal fun KaraokeScoreResultDialog(score: Int, onDismiss: () -> Unit) {
    val karaokeStrings = LocalKaraokeStrings.current
    val filledStars = ceil(score / 20f).toInt().coerceIn(0, 5)
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            tonalElevation = 8.dp,
            modifier = Modifier.widthIn(max = 400.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 28.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = karaokeStrings.scoreResult,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = score.toString(),
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = karaokeStrings.score,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    repeat(5) { index ->
                        Icon(
                            imageVector = Icons.Outlined.Star,
                            contentDescription = null,
                            tint = if (index < filledStars) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(karaokeStrings.done)
                }
            }
        }
    }
}

private fun formatKaraokeDate(createdAt: Long): String {
    return SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(createdAt))
}

private fun formatKaraokeTime(durationMs: Long): String {
    val totalSeconds = (durationMs / 1000L).coerceAtLeast(0L)
    return "%02d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}
