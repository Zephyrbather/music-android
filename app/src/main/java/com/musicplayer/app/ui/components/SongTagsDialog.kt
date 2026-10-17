package com.musicplayer.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Label
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.musicplayer.app.domain.model.Song
import com.musicplayer.app.domain.usecase.SongTagGenerationResult
import com.musicplayer.app.domain.usecase.mergeTags
import com.musicplayer.app.ui.i18n.LocalExtraStrings
import com.musicplayer.app.ui.i18n.LocalStrings
import kotlinx.coroutines.launch

fun collectSongTags(songs: List<Song>): List<String> {
    val seen = LinkedHashMap<String, String>()
    songs.forEach { song ->
        song.semanticTags.forEach { tag ->
            val trimmed = tag.trim()
            if (trimmed.isNotBlank()) seen.putIfAbsent(trimmed.lowercase(), trimmed)
        }
    }
    return seen.values.sortedBy { it.lowercase() }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SongTagsDialog(
    song: Song,
    allTags: List<String>,
    onSave: (List<String>) -> Unit,
    onGenerateWithAi: suspend (Song) -> SongTagGenerationResult,
    onDismiss: () -> Unit
) {
    val strings = LocalStrings.current
    val extra = LocalExtraStrings.current
    val scope = rememberCoroutineScope()
    var tags by remember(song.id) { mutableStateOf(song.semanticTags) }
    var input by remember(song.id) { mutableStateOf("") }
    var generating by remember(song.id) { mutableStateOf(false) }
    var status by remember(song.id) { mutableStateOf<String?>(null) }

    fun addTag(raw: String) {
        tags = mergeTags(tags, listOf(raw))
        input = ""
    }

    Dialog(onDismissRequest = { if (!generating) onDismiss() }) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 640.dp),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Label,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = extra.tags,
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "${song.title} · ${song.artistName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (tags.isEmpty()) {
                    Text(
                        text = extra.noTags,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        tags.forEach { tag ->
                            InputChip(
                                selected = true,
                                onClick = { tags = tags - tag },
                                label = { Text("#$tag") },
                                trailingIcon = {
                                    Icon(
                                        imageVector = Icons.Outlined.Close,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = input,
                        onValueChange = { input = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        label = { Text(extra.addTag) },
                        placeholder = { Text(extra.tagInputHint) }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = { if (input.isNotBlank()) addTag(input) },
                        enabled = input.isNotBlank()
                    ) {
                        Icon(Icons.Outlined.Add, contentDescription = extra.addTag)
                    }
                }

                val suggestions = remember(tags, allTags) {
                    allTags.filter { candidate ->
                        tags.none { it.equals(candidate, ignoreCase = true) }
                    }.take(12)
                }
                if (suggestions.isNotEmpty()) {
                    Text(
                        text = extra.allTags,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        suggestions.forEach { tag ->
                            AssistChip(
                                onClick = { addTag(tag) },
                                label = { Text(tag) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Outlined.Add,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            )
                        }
                    }
                }

                OutlinedButton(
                    onClick = {
                        scope.launch {
                            generating = true
                            status = null
                            when (val result = onGenerateWithAi(song)) {
                                is SongTagGenerationResult.Generated -> {
                                    tags = result.tags
                                }
                                SongTagGenerationResult.Unavailable -> status = extra.tagsUnavailable
                                SongTagGenerationResult.Failed -> status = extra.tagsFailed
                            }
                            generating = false
                        }
                    },
                    enabled = !generating,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (generating) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(extra.tagsGenerating)
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(extra.generateTagsWithAi)
                    }
                }

                status?.let { message ->
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss, enabled = !generating) {
                        Text(strings.cancel)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(
                        onClick = { onSave(mergeTags(tags, emptyList())) },
                        enabled = !generating
                    ) {
                        Text(extra.save)
                    }
                }
            }
        }
    }
}
