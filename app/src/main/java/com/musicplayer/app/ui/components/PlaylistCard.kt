package com.musicplayer.app.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PlaylistPlay
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.musicplayer.app.domain.model.Playlist

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PlaylistCard(
    playlist: Playlist,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val content: @Composable () -> Unit = {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.PlaylistPlay,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = playlist.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = playlist.description ?: "${playlist.songCount} songs",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
    if (onLongClick != null) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .combinedClickable(onClick = onClick, onLongClick = onLongClick),
            shape = MaterialTheme.shapes.medium,
        ) { content() }
    } else {
        Card(
            onClick = onClick,
            modifier = modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
        ) { content() }
    }
}
