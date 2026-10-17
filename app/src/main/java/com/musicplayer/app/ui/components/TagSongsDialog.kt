package com.musicplayer.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Label
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.window.Dialog
import com.musicplayer.app.domain.model.Song
import com.musicplayer.app.ui.i18n.LocalExtraStrings
import com.musicplayer.app.ui.i18n.LocalStrings

@Composable
fun TagSongsDialog(
    title: String,
    fixedTagName: String?,
    songs: List<Song>,
    preselectedIds: Set<String> = emptySet(),
    onConfirm: (tagName: String, songIds: Set<String>) -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalStrings.current
    val extra = LocalExtraStrings.current
    var tagName by remember { mutableStateOf(fixedTagName.orEmpty()) }
    var query by remember { mutableStateOf("") }
    var selectedIds by remember { mutableStateOf(preselectedIds) }
    val filteredSongs = remember(songs, query) {
        val keyword = query.trim()
        if (keyword.isBlank()) songs
        else songs.filter {
            it.title.contains(keyword, ignoreCase = true) ||
                it.artistName.contains(keyword, ignoreCase = true) ||
                it.albumTitle.contains(keyword, ignoreCase = true)
        }
    }
    val canConfirm = tagName.isNotBlank() && selectedIds.isNotEmpty()

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 680.dp),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Label,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = title, style = MaterialTheme.typography.titleLarge)
                        Text(
                            text = extra.selectedCountFormat.format(selectedIds.size),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (fixedTagName == null) {
                    OutlinedTextField(
                        value = tagName.removePrefix("#"),
                        onValueChange = { tagName = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text(extra.tagNameLabel) },
                        placeholder = { Text(extra.tagInputHint) }
                    )
                }

                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.Outlined.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                    },
                    placeholder = { Text(extra.searchSongs) }
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 380.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(items = filteredSongs, key = { it.id }) { song ->
                        val checked = song.id in selectedIds
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedIds = if (checked) selectedIds - song.id
                                    else selectedIds + song.id
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = { isChecked ->
                                    selectedIds = if (isChecked) selectedIds + song.id
                                    else selectedIds - song.id
                                }
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = song.title,
                                    style = MaterialTheme.typography.bodyLarge,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = song.artistName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                    if (filteredSongs.isEmpty()) {
                        item {
                            Text(
                                text = extra.noMatchingSongs,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 16.dp)
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(strings.cancel)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(
                        onClick = {
                            onConfirm(
                                tagName.trim().removePrefix("#").trim(),
                                selectedIds
                            )
                        },
                        enabled = canConfirm
                    ) {
                        Text(extra.save)
                    }
                }
            }
        }
    }
}
