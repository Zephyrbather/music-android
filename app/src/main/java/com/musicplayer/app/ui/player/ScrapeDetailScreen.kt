package com.musicplayer.app.ui.player

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Album
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.musicplayer.app.domain.model.ArtworkCandidate
import com.musicplayer.app.domain.model.LyricsCandidate
import com.musicplayer.app.domain.model.PlaybackState
import com.musicplayer.app.domain.model.ScrapeCandidates
import com.musicplayer.app.domain.model.ScrapeSearchMode
import com.musicplayer.app.domain.model.Song
import com.musicplayer.app.domain.model.TrackMetadataCandidate
import com.musicplayer.app.ui.i18n.LocalStrings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScrapeDetailScreen(
    playbackState: PlaybackState,
    controlsState: PlayerControlsState,
    scrapeCandidates: ScrapeCandidates?,
    selectedScrapeCandidate: TrackMetadataCandidate?,
    onBack: () -> Unit,
    onSearchKeywordChange: (String) -> Unit,
    onSearchModeChange: (ScrapeSearchMode) -> Unit,
    onPrepareScrape: () -> Unit,
    onScrapeCurrent: () -> Unit,
    onAiScrapeCurrent: () -> Unit,
    onAiGenerateCategory: () -> Unit,
    onSelectScrapeCandidate: (TrackMetadataCandidate?) -> Unit,
    onApplyScrapeSelection: () -> Unit
) {
    val strings = LocalStrings.current
    val song = playbackState.currentSong
    val hasResult = scrapeCandidates != null
    val canApply = selectedScrapeCandidate != null
    val searchMode = controlsState.scrapeSearchMode
    val searchButtonText = when (searchMode) {
        ScrapeSearchMode.ALL -> strings.scrapeMetadata
        ScrapeSearchMode.LYRICS -> strings.searchLyricsOnly
        ScrapeSearchMode.ARTWORK -> strings.searchArtworkOnly
    }
    val saveButtonText = when (searchMode) {
        ScrapeSearchMode.ALL -> strings.saveAllMetadata
        ScrapeSearchMode.LYRICS -> strings.saveLyricsOnly
        ScrapeSearchMode.ARTWORK -> strings.saveArtworkOnly
    }
    val sourceCandidates = remember(scrapeCandidates, searchMode) {
        scrapeCandidates?.toTrackCandidates(searchMode).orEmpty()
    }
    val previewCandidate = selectedScrapeCandidate
    val screenListState = rememberLazyListState()
    val showResults = hasResult && !controlsState.scraping && !controlsState.aiScraping
    val resultItemIndex = 5 +
        (if (controlsState.aiScrapeStatus != null) 1 else 0) +
        (if (controlsState.categoryStatus != null) 1 else 0) +
        (if (controlsState.scraping || controlsState.aiScraping) 1 else 0)

    LaunchedEffect(song?.id) {
        onPrepareScrape()
    }

    LaunchedEffect(scrapeCandidates, showResults, sourceCandidates.size, searchMode) {
        if (showResults) {
            screenListState.animateScrollToItem(resultItemIndex)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.currentTrackScrape) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = strings.close)
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            state = screenListState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                OutlinedTextField(
                    value = controlsState.searchKeyword,
                    onValueChange = onSearchKeywordChange,
                    label = { Text(strings.scrapeMetadata) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = MaterialTheme.typography.bodyMedium
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ScrapeModeChip(
                        text = strings.searchAll,
                        selected = searchMode == ScrapeSearchMode.ALL,
                        onClick = { onSearchModeChange(ScrapeSearchMode.ALL) },
                        modifier = Modifier.weight(1f)
                    )
                    ScrapeModeChip(
                        text = strings.searchLyricsOnly,
                        selected = searchMode == ScrapeSearchMode.LYRICS,
                        onClick = { onSearchModeChange(ScrapeSearchMode.LYRICS) },
                        modifier = Modifier.weight(1f)
                    )
                    ScrapeModeChip(
                        text = strings.searchArtworkOnly,
                        selected = searchMode == ScrapeSearchMode.ARTWORK,
                        onClick = { onSearchModeChange(ScrapeSearchMode.ARTWORK) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Text(
                    text = "${strings.songCategory}: ${song?.categoryTag?.takeIf { it.isNotBlank() } ?: strings.noCategory}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item {
                Button(
                    onClick = onScrapeCurrent,
                    enabled = !controlsState.scraping && !controlsState.aiScraping && song != null,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (controlsState.scraping) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Icon(imageVector = Icons.Outlined.Search, contentDescription = null)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (controlsState.scraping) strings.scrapeRunning else searchButtonText)
                }
            }

            item {
                Button(
                    onClick = onAiScrapeCurrent,
                    enabled = !controlsState.scraping && !controlsState.aiScraping && song != null,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (controlsState.aiScraping) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Icon(imageVector = Icons.Outlined.Search, contentDescription = null)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (controlsState.aiScraping) strings.aiScrapeRunning else strings.aiAutoScrape)
                }
            }

            item {
                Button(
                    onClick = onAiGenerateCategory,
                    enabled = !controlsState.categoryGenerating && !controlsState.scraping &&
                        !controlsState.aiScraping && song != null,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (controlsState.categoryGenerating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Icon(imageVector = Icons.Outlined.Search, contentDescription = null)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (controlsState.categoryGenerating) strings.categoryGenerating else strings.generateSongCategory)
                }
            }

            controlsState.categoryStatus?.let { status ->
                item {
                    Text(
                        text = when (status) {
                            AiCategoryStatus.API_KEY_REQUIRED -> strings.aiKeyRequired
                            AiCategoryStatus.FAILED -> strings.categoryFailed
                            AiCategoryStatus.SAVED -> strings.categoryGenerated
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (status == AiCategoryStatus.SAVED) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.error
                        }
                    )
                }
            }

            controlsState.aiScrapeStatus?.let { status ->
                item {
                    Text(
                        text = when (status) {
                            AiScrapeStatus.API_KEY_REQUIRED -> strings.aiKeyRequired
                            AiScrapeStatus.NO_SELECTION -> strings.aiNoSelection
                            AiScrapeStatus.FAILED -> strings.aiScrapeFailed
                            AiScrapeStatus.SAVED -> strings.aiScrapeSaved
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (status == AiScrapeStatus.SAVED) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.error
                        }
                    )
                }
            }

            if (controlsState.scraping || controlsState.aiScraping || controlsState.categoryGenerating) {
                item {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }

            if (showResults) {
                if (sourceCandidates.isEmpty()) {
                    item {
                        Text(
                            text = strings.noResultsFound,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    }
                } else {
                    item {
                        Text(
                            text = strings.candidateSources,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    itemsIndexed(
                        items = sourceCandidates,
                        key = { index, candidate ->
                            "${index}-${candidate.provider}-${candidate.score}-${candidate.title.orEmpty()}-${candidate.albumTitle.orEmpty()}"
                        }
                    ) { _, candidate ->
                        val selected = selectedScrapeCandidate == candidate
                        SourceCandidateCard(
                            candidate = candidate,
                            selected = selected,
                            onTry = {
                                onSelectScrapeCandidate(if (selected) null else candidate)
                            }
                        )
                    }
                }
            }

            previewCandidate?.let { candidate ->
                item {
                    PlaybackCandidatePreview(
                        song = song,
                        candidate = candidate,
                        searchMode = searchMode,
                        positionMs = playbackState.positionMs
                    )
                }
            }

            item {
                Button(
                    onClick = {
                        onApplyScrapeSelection()
                        onBack()
                    },
                    enabled = canApply,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Outlined.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(saveButtonText)
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun ScrapeModeChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                text = text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        modifier = modifier
    )
}

@Composable
private fun SourceCandidateCard(
    candidate: TrackMetadataCandidate,
    selected: Boolean,
    onTry: () -> Unit
) {
    val strings = LocalStrings.current
    val artwork = candidate.artwork
    val albumText = candidate.albumTitle?.takeIf { it.isNotBlank() } ?: strings.missingData
    val dateText = formatReleaseDate(candidate.releaseYear, candidate.releaseMonth) ?: strings.missingData
    val lyricsLabel = when {
        candidate.lyrics?.synced == true -> strings.syncedLyrics
        candidate.lyrics != null -> strings.plainLyrics
        else -> strings.missingData
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (selected) {
                    Modifier.border(2.dp, MaterialTheme.colorScheme.primary, MaterialTheme.shapes.medium)
                } else {
                    Modifier
                }
            )
            .clickable(onClick = onTry),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ArtworkThumb(
                imageUri = artwork?.imageUri,
                modifier = Modifier.size(72.dp)
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = candidate.provider,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "↑${candidate.score}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = candidate.title ?: strings.missingData,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                InfoLine(label = strings.album, value = albumText)
                InfoLine(label = strings.albumDate, value = dateText)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    MetadataPill(text = lyricsLabel, emphasized = candidate.lyrics?.synced == true)
                    MetadataPill(text = if (artwork != null) strings.artwork else strings.missingData)
                }
            }

            TextButton(onClick = onTry) {
                Icon(
                    imageVector = if (selected) Icons.Outlined.Check else Icons.Outlined.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (selected) strings.usingCandidate else strings.tryCandidate)
            }
        }
    }
}

@Composable
private fun PlaybackCandidatePreview(
    song: Song?,
    candidate: TrackMetadataCandidate,
    searchMode: ScrapeSearchMode,
    positionMs: Long
) {
    val strings = LocalStrings.current
    val useLyrics = searchMode != ScrapeSearchMode.ARTWORK
    val useArtwork = searchMode != ScrapeSearchMode.LYRICS
    val useSongIdentity = searchMode == ScrapeSearchMode.ALL
    val useAlbumInfo = searchMode == ScrapeSearchMode.ALL
    val artworkUri = if (useArtwork) candidate.artwork?.imageUri ?: song?.artworkUri else song?.artworkUri
    val title = if (useSongIdentity) candidate.title ?: song?.title ?: strings.chooseSong else song?.title ?: strings.chooseSong
    val artist = if (useSongIdentity) candidate.artistName ?: song?.artistName ?: strings.missingData else song?.artistName ?: strings.missingData
    val album = if (useAlbumInfo) candidate.albumTitle ?: song?.albumTitle ?: strings.missingData else song?.albumTitle ?: strings.missingData
    val releaseDate = formatReleaseDate(
        year = if (useAlbumInfo) candidate.releaseYear ?: song?.releaseYear else song?.releaseYear,
        month = if (useAlbumInfo) candidate.releaseMonth ?: song?.releaseMonth else song?.releaseMonth
    ) ?: strings.missingData
    val lyrics = if (useLyrics) candidate.lyrics?.lyrics ?: song?.lyrics else song?.lyrics
    val lrcLines = remember(lyrics) { lyrics?.let { parseLrc(it) } }
    val currentLrcIndex = remember(lrcLines, positionMs) {
        lrcLines?.let { findCurrentLrcIndex(it, positionMs) } ?: -1
    }
    val lyricsListState = rememberLazyListState()
    LaunchedEffect(currentLrcIndex) {
        if (currentLrcIndex >= 0) {
            lyricsListState.animateScrollToItem((currentLrcIndex - 2).coerceAtLeast(0))
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = strings.playingPreview,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = MaterialTheme.shapes.medium
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ArtworkThumb(
                    imageUri = artworkUri,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = artist,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                InfoLine(label = strings.album, value = album)
                InfoLine(label = strings.albumDate, value = releaseDate)

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 180.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shape = MaterialTheme.shapes.small
                ) {
                    when {
                        lrcLines != null && lrcLines.isNotEmpty() -> {
                            LazyColumn(
                                state = lyricsListState,
                                modifier = Modifier.padding(12.dp)
                            ) {
                                itemsIndexed(lrcLines) { index, line ->
                                    val isCurrent = index == currentLrcIndex
                                    Text(
                                        text = line.text,
                                        style = if (isCurrent) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodySmall,
                                        color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
                                        modifier = Modifier.padding(vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        !lyrics.isNullOrBlank() -> {
                            Column(
                                modifier = Modifier
                                    .verticalScroll(rememberScrollState())
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = lyrics,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        else -> {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = strings.missingData,
                                    style = MaterialTheme.typography.bodySmall,
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

@Composable
private fun ArtworkThumb(
    imageUri: String?,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface
    ) {
        if (!imageUri.isNullOrBlank()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(imageUri)
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
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun MetadataPill(
    text: String,
    emphasized: Boolean = false
) {
    val color = if (emphasized) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        color = if (emphasized) Color(0xFF2E7D32).copy(alpha = 0.14f) else MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.extraSmall
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun InfoLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 12.dp)
        )
    }
}

private fun ScrapeCandidates.toTrackCandidates(mode: ScrapeSearchMode): List<TrackMetadataCandidate> {
    return when (mode) {
        ScrapeSearchMode.ALL -> tracks.ifEmpty { mergeLegacyCandidates() }
        ScrapeSearchMode.LYRICS -> tracks.filter { it.lyrics != null }.ifEmpty {
            lyrics.map {
                TrackMetadataCandidate(
                    provider = it.provider,
                    lyrics = it,
                    score = it.score
                )
            }
        }
        ScrapeSearchMode.ARTWORK -> tracks.filter { it.artwork != null }.ifEmpty {
            artworks.map {
                TrackMetadataCandidate(
                    provider = it.provider,
                    artwork = it,
                    score = it.score
                )
            }
        }
    }.sortedByDescending { it.score }
}

private fun ScrapeCandidates.mergeLegacyCandidates(): List<TrackMetadataCandidate> {
    return (lyrics.map {
        TrackMetadataCandidate(provider = it.provider, lyrics = it, score = it.score)
    } + artworks.map {
        TrackMetadataCandidate(provider = it.provider, artwork = it, score = it.score)
    }).groupBy { it.provider }
        .map { (provider, candidates) ->
            val lyrics = candidates.firstNotNullOfOrNull { it.lyrics }
            val artwork = candidates.firstNotNullOfOrNull { it.artwork }
            TrackMetadataCandidate(
                provider = provider,
                lyrics = lyrics,
                artwork = artwork,
                score = maxOf(lyrics?.score ?: 0, artwork?.score ?: 0)
            )
        }
}

private fun formatReleaseDate(year: Int?, month: Int?): String? {
    val validYear = year?.takeIf { it > 0 } ?: return null
    val validMonth = month?.takeIf { it in 1..12 }
    return if (validMonth != null) {
        "$validYear-${validMonth.toString().padStart(2, '0')}"
    } else {
        validYear.toString()
    }
}
