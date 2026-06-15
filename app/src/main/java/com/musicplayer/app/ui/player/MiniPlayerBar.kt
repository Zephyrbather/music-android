package com.musicplayer.app.ui.player

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Album
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.QueueMusic
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.musicplayer.app.domain.model.PlaybackState
import com.musicplayer.app.domain.model.Song
import com.musicplayer.app.ui.i18n.LocalStrings

@Composable
fun MiniPlayerBar(
    playbackState: PlaybackState,
    onClick: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onQueueClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    val song = playbackState.currentSong ?: return
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
            .compositeOver(MaterialTheme.colorScheme.surface),
        tonalElevation = 1.dp
    ) {
        Column {
            LinearProgressIndicator(
                progress = {
                    if (playbackState.durationMs <= 0L) 0f
                    else playbackState.positionMs.toFloat() / playbackState.durationMs.toFloat()
                },
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                modifier = Modifier.padding(start = 8.dp, top = 6.dp, bottom = 6.dp, end = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MiniPlayerArtwork(
                    song = song,
                    modifier = Modifier.size(44.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = playbackState.errorMessage ?: song.artistName,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (playbackState.errorMessage == null) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.error
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(onClick = onTogglePlayPause, modifier = Modifier.size(40.dp)) {
                    Icon(
                        imageVector = if (playbackState.isPlaying) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                        contentDescription = if (playbackState.isPlaying) strings.pause else strings.play
                    )
                }
                IconButton(onClick = onSkipNext, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Outlined.SkipNext, contentDescription = strings.next)
                }
                IconButton(onClick = onQueueClick, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Outlined.QueueMusic, contentDescription = strings.queueMusic)
                }
            }
        }
    }
}

@Composable
private fun MiniPlayerArtwork(
    song: Song,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.clip(RoundedCornerShape(8.dp)),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        tonalElevation = 2.dp
    ) {
        if (!song.artworkUri.isNullOrBlank()) {
            val context = LocalContext.current
            val imageRequest = remember(context, song.artworkUri) {
                ImageRequest.Builder(context)
                    .data(song.artworkUri)
                    .crossfade(false)
                    .build()
            }
            AsyncImage(
                model = imageRequest,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Outlined.Album,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}
