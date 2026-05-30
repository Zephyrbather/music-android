package com.musicplayer.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Album
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.musicplayer.app.domain.model.Song
import com.musicplayer.app.ui.i18n.LocalStrings

@Composable
fun EditSongDialog(
    song: Song,
    onSave: (String, String, String, String?, String?) -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalStrings.current
    var title by remember { mutableStateOf(song.title) }
    var artist by remember { mutableStateOf(song.artistName) }
    var album by remember { mutableStateOf(song.albumTitle) }
    var artworkUri by remember { mutableStateOf(song.artworkUri.orEmpty()) }
    val normalizedArtworkUri = artworkUri.trim().takeIf { it.isNotBlank() }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .heightIn(max = 620.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = strings.editInfo, style = MaterialTheme.typography.titleMedium)
                    IconButton(onClick = onDismiss, modifier = Modifier.padding(0.dp)) {
                        Icon(Icons.Outlined.Close, contentDescription = strings.close)
                    }
                }
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.medium
                ) {
                    if (!normalizedArtworkUri.isNullOrBlank()) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(normalizedArtworkUri)
                                .crossfade(true)
                                .build(),
                            contentDescription = strings.artwork,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(MaterialTheme.shapes.medium)
                        )
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Album,
                                contentDescription = strings.artwork,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(strings.editTitle) }
                )
                OutlinedTextField(
                    value = artist,
                    onValueChange = { artist = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(strings.artist) }
                )
                OutlinedButton(
                    onClick = {
                        val previousTitle = title
                        title = artist
                        artist = previousTitle
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Outlined.SwapVert, contentDescription = null)
                    Text(strings.swapTitleArtist, modifier = Modifier.padding(start = 8.dp))
                }
                OutlinedTextField(
                    value = album,
                    onValueChange = { album = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(strings.album) }
                )
                OutlinedTextField(
                    value = artworkUri,
                    onValueChange = { artworkUri = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(strings.artwork) },
                    supportingText = {
                        Text(
                            text = song.artworkProvider ?: strings.missingData,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { artworkUri = "" },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(strings.remove)
                    }
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(strings.cancel)
                    }
                    Button(
                        onClick = {
                            onSave(
                                title.trim(),
                                artist.trim(),
                                album.trim(),
                                normalizedArtworkUri,
                                normalizedArtworkUri?.let { song.artworkProvider ?: "Manual" }
                            )
                        },
                        enabled = title.isNotBlank(),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(strings.savePreset)
                    }
                }
                Spacer(modifier = Modifier.size(1.dp))
            }
        }
    }
}
