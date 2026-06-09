package com.musicplayer.app.ui.player

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Album
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.musicplayer.app.domain.model.PlaybackState
import com.musicplayer.app.domain.model.Song
import com.musicplayer.app.domain.model.SongCategoryTags
import com.musicplayer.app.ui.components.SongArtwork
import com.musicplayer.app.ui.i18n.AppStrings
import com.musicplayer.app.ui.i18n.LocalStrings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqualizerScreen(
    playbackState: PlaybackState,
    controlsState: PlayerControlsState,
    onBack: () -> Unit,
    onPresetSelected: (EqualizerPreset) -> Unit,
    onDraftNameChange: (String) -> Unit,
    onBandLevelChange: (Int, Float) -> Unit,
    onAdaptiveEqualizerChange: (Boolean) -> Unit,
    onCategoryPresetChange: (String, String) -> Unit,
    onCreatePreset: () -> Unit,
    onSavePreset: () -> Unit,
    onDeletePreset: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    val song = playbackState.currentSong

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.equalizer) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = strings.close)
                    }
                }
            )
        },
        modifier = modifier
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            EqualizerTrackHeader(song = song)
            EqualizerPresetSection(
                state = controlsState,
                strings = strings,
                onPresetSelected = onPresetSelected,
                onDraftNameChange = onDraftNameChange,
                onCreatePreset = onCreatePreset,
                onSavePreset = onSavePreset,
                onDeletePreset = onDeletePreset
            )
            EqualizerBandSection(
                state = controlsState,
                onBandLevelChange = onBandLevelChange
            )
            AdaptiveEqualizerSection(
                state = controlsState,
                currentCategory = song?.categoryTag,
                strings = strings,
                onAdaptiveEqualizerChange = onAdaptiveEqualizerChange,
                onCategoryPresetChange = onCategoryPresetChange
            )
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

@Composable
private fun EqualizerTrackHeader(
    song: Song?,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (song != null) {
                SongArtwork(song = song, modifier = Modifier.size(58.dp), imageSize = 58.dp)
            } else {
                Surface(
                    modifier = Modifier.size(58.dp),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Outlined.Album,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = song?.title ?: strings.nothingPlaying,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = song?.let { "${it.artistName} - ${it.albumTitle}" } ?: strings.chooseSong,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${strings.songCategory}: ${song?.categoryTag?.takeIf { it.isNotBlank() } ?: strings.noCategory}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = Icons.Outlined.GraphicEq,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun EqualizerPresetSection(
    state: PlayerControlsState,
    strings: AppStrings,
    onPresetSelected: (EqualizerPreset) -> Unit,
    onDraftNameChange: (String) -> Unit,
    onCreatePreset: () -> Unit,
    onSavePreset: () -> Unit,
    onDeletePreset: () -> Unit
) {
    EqualizerSectionSurface {
        Text(text = strings.presetName, style = MaterialTheme.typography.titleMedium)
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(end = 2.dp)
        ) {
            items(state.presets, key = { it.id }) { preset ->
                FilterChip(
                    selected = state.selectedPresetId == preset.id,
                    onClick = { onPresetSelected(preset) },
                    label = {
                        Text(
                            preset.displayName(strings),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
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
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = onCreatePreset,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(strings.newPreset)
            }
            Button(
                onClick = onSavePreset,
                enabled = state.canDelete,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Outlined.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(strings.savePreset)
            }
            OutlinedButton(
                onClick = onDeletePreset,
                enabled = state.canDelete,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Outlined.DeleteOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(strings.deletePreset)
            }
        }
    }
}

@Composable
private fun EqualizerBandSection(
    state: PlayerControlsState,
    onBandLevelChange: (Int, Float) -> Unit
) {
    EqualizerSectionSurface {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            contentPadding = PaddingValues(horizontal = 2.dp),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 178.dp)
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
    }
}

@Composable
private fun AdaptiveEqualizerSection(
    state: PlayerControlsState,
    currentCategory: String?,
    strings: AppStrings,
    onAdaptiveEqualizerChange: (Boolean) -> Unit,
    onCategoryPresetChange: (String, String) -> Unit
) {
    EqualizerSectionSurface {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = strings.adaptiveEqualizer, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = strings.adaptiveEqualizerDesc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${strings.songCategory}: ${currentCategory?.takeIf { it.isNotBlank() } ?: strings.noCategory}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = state.adaptiveEqualizerEnabled,
                onCheckedChange = onAdaptiveEqualizerChange
            )
        }
        if (state.adaptiveEqualizerEnabled) {
            Text(text = strings.categoryEqualizerConfig, style = MaterialTheme.typography.titleSmall)
            SongCategoryTags.supported.forEach { category ->
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(text = category, style = MaterialTheme.typography.bodyMedium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(state.presets, key = { "${category}-${it.id}" }) { preset ->
                            FilterChip(
                                selected = state.categoryPresetMap[category] == preset.id,
                                onClick = { onCategoryPresetChange(category, preset.id) },
                                label = {
                                    Text(
                                        preset.displayName(strings),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EqualizerSectionSurface(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content
        )
    }
}

@Composable
private fun EqualizerBandSlider(
    freqLabel: String,
    value: Float,
    onValueChange: (Float) -> Unit
) {
    val trackHeight = 136.dp
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
        modifier = Modifier.widthIn(min = 38.dp, max = 48.dp)
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

                drawRoundRect(
                    color = inactiveColor,
                    topLeft = Offset(trackLeft, trackTop),
                    size = Size(trackWidth.toPx(), trackBottom - trackTop),
                    cornerRadius = CornerRadius(trackWidth.toPx() / 2)
                )
                drawRoundRect(
                    color = activeColor,
                    topLeft = Offset(trackLeft, thumbY),
                    size = Size(trackWidth.toPx(), trackBottom - thumbY),
                    cornerRadius = CornerRadius(trackWidth.toPx() / 2)
                )
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

private fun EqualizerPreset.displayName(strings: AppStrings): String {
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
