package com.musicplayer.app.domain.usecase

import com.musicplayer.app.domain.ai.AiMetadataAdvisor
import com.musicplayer.app.domain.model.ScrapeCandidates
import com.musicplayer.app.domain.model.ScrapeSearchMode
import com.musicplayer.app.domain.model.Song
import com.musicplayer.app.domain.model.TrackMetadataCandidate
import com.musicplayer.app.domain.repository.MusicRepository
import javax.inject.Inject

sealed interface AiMetadataScrapeResult {
    data class Saved(
        val candidates: ScrapeCandidates,
        val selected: TrackMetadataCandidate,
        val song: Song
    ) : AiMetadataScrapeResult

    data class NoSelection(val candidates: ScrapeCandidates) : AiMetadataScrapeResult

    data object Failed : AiMetadataScrapeResult
}

class AiScrapeMetadataUseCase @Inject constructor(
    private val aiMetadataAdvisor: AiMetadataAdvisor,
    private val collectCandidatesUseCase: CollectScrapeCandidatesUseCase,
    private val musicRepository: MusicRepository
) {
    suspend operator fun invoke(
        song: Song,
        apiKey: String,
        model: String,
        renameLocalFile: Boolean = true
    ): AiMetadataScrapeResult {
        val identity = aiMetadataAdvisor.identifySong(song, apiKey, model)
            ?: return AiMetadataScrapeResult.Failed
        val candidates = collectCandidatesUseCase(
            song = song.forAiSearch(identity.title, identity.artistName),
            mode = ScrapeSearchMode.ALL
        )
        val selected = aiMetadataAdvisor.selectCandidate(identity, candidates.tracks, apiKey, model)
            ?: return AiMetadataScrapeResult.NoSelection(candidates)
        val saved = musicRepository.saveScrapedMetadata(
            song = selected.applyTo(song),
            mode = ScrapeSearchMode.ALL,
            renameLocalFile = renameLocalFile
        )
        return AiMetadataScrapeResult.Saved(candidates, selected, saved)
    }
}

private fun Song.forAiSearch(title: String, artistName: String): Song {
    return copy(
        title = title.trim(),
        artistName = artistName.trim(),
        albumTitle = "",
        lyrics = null,
        lyricsProvider = null,
        artworkUri = null,
        artworkProvider = null,
        releaseYear = null,
        releaseMonth = null
    )
}

private fun TrackMetadataCandidate.applyTo(song: Song): Song {
    return song.copy(
        title = title?.takeIf { it.isNotBlank() } ?: song.title,
        artistName = artistName?.takeIf { it.isNotBlank() } ?: song.artistName,
        albumTitle = albumTitle?.takeIf { it.isNotBlank() } ?: song.albumTitle,
        releaseYear = releaseYear ?: song.releaseYear,
        releaseMonth = releaseMonth ?: song.releaseMonth,
        lyrics = lyrics?.lyrics ?: song.lyrics,
        lyricsProvider = lyrics?.provider ?: song.lyricsProvider,
        artworkUri = artwork?.imageUri ?: song.artworkUri,
        artworkProvider = artwork?.provider ?: song.artworkProvider
    )
}
