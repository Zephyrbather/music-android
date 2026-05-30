package com.musicplayer.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Album
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.musicplayer.app.domain.model.Song
import com.musicplayer.app.ui.i18n.LocalStrings

@Composable
fun SongListItem(
    song: Song,
    onClick: () -> Unit,
    onDoubleClick: (() -> Unit)? = null,
    onFavoriteClick: (() -> Unit)? = null,
    onScrapeClick: (() -> Unit)? = null,
    onMoreClick: (() -> Unit)? = null,
    isSelected: Boolean = false,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = tween(150),
        label = "press"
    )
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surface,
        animationSpec = tween(200),
        label = "select"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .background(bgColor, MaterialTheme.shapes.medium)
            .clip(MaterialTheme.shapes.medium)
            .pointerInput(song.id) {
                detectTapGestures(
                    onTap = {
                        pressed = true
                        onClick()
                        pressed = false
                    },
                    onDoubleTap = {
                        onDoubleClick?.invoke()
                    },
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
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
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
        if (onFavoriteClick != null) {
            IconButton(onClick = onFavoriteClick) {
                Icon(
                    imageVector = if (song.isFavorite) Icons.Outlined.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = if (song.isFavorite) strings.removeFavorite else strings.addFavorite
                )
            }
        }
        if (onScrapeClick != null) {
            IconButton(onClick = onScrapeClick) {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = strings.scrapeMetadata
                )
            }
        }
        if (onMoreClick != null) {
            IconButton(onClick = onMoreClick) {
                Icon(
                    imageVector = Icons.Outlined.MoreVert,
                    contentDescription = "More"
                )
            }
        } else if (onDoubleClick == null) {
            IconButton(onClick = onClick) {
                Icon(
                    imageVector = Icons.Outlined.PlayArrow,
                    contentDescription = "${strings.play} ${song.title}"
                )
            }
        }
    }
}

@Composable
fun SongArtwork(
    song: Song,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.clip(RoundedCornerShape(8.dp)),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        if (!song.artworkUri.isNullOrBlank()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(song.artworkUri)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Outlined.Album,
                    contentDescription = null,
                    modifier = Modifier
                        .size(26.dp)
                        .padding(1.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

fun Song.libraryDetailLabel(): String {
    return "${qualityLabel} | $albumTitle | ${durationLabel()}"
}

fun Song.durationLabel(): String {
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
