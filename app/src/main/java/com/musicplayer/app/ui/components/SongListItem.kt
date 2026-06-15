package com.musicplayer.app.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.musicplayer.app.domain.model.Song
import com.musicplayer.app.ui.i18n.LocalStrings

@OptIn(ExperimentalFoundationApi::class)
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
    val bgColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
    val detailLabel = remember(
        song.id,
        song.qualityLabel,
        song.artistName,
        song.albumTitle,
        song.durationMs,
        song.categoryTag,
        song.semanticTags
    ) {
        song.libraryDetailLabel()
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(bgColor, MaterialTheme.shapes.medium)
            .clip(MaterialTheme.shapes.medium)
            .combinedClickable(
                onClick = onClick,
                onDoubleClick = onDoubleClick
            )
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
                text = detailLabel,
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
    modifier: Modifier = Modifier,
    imageSize: Dp = 56.dp
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        if (!song.artworkUri.isNullOrBlank()) {
            val context = LocalContext.current
            val imageSizePx = with(LocalDensity.current) { imageSize.roundToPx().coerceAtLeast(1) }
            val imageRequest = remember(context, song.artworkUri, imageSizePx) {
                ImageRequest.Builder(context)
                    .data(song.artworkUri)
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
                modifier = Modifier
                    .size(26.dp)
                    .padding(1.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

fun Song.libraryDetailLabel(): String {
    val artistAlbum = listOf(artistName, albumTitle)
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .joinToString(" - ")
    return listOfNotNull(
        categoryTag?.takeIf { it.isNotBlank() },
        semanticTags.takeIf { it.isNotEmpty() }?.take(3)?.joinToString(" ") { "#$it" },
        qualityLabel,
        artistAlbum.takeIf { it.isNotBlank() },
        durationLabel()
    ).joinToString(" | ")
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
