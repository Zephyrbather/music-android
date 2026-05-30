package com.musicplayer.app.domain.usecase

import com.musicplayer.app.data.scraper.CompositeMetadataScraper
import com.musicplayer.app.data.scraper.LyricsResult
import com.musicplayer.app.data.scraper.ArtworkResult
import com.musicplayer.app.data.scraper.TrackMetadataResult
import com.musicplayer.app.domain.model.ArtworkCandidate
import com.musicplayer.app.domain.model.LyricsCandidate
import com.musicplayer.app.domain.model.ScrapeCandidates
import com.musicplayer.app.domain.model.ScrapeSearchMode
import com.musicplayer.app.domain.model.Song
import com.musicplayer.app.domain.model.TrackMetadataCandidate
import javax.inject.Inject

class CollectScrapeCandidatesUseCase @Inject constructor(
    private val compositeMetadataScraper: CompositeMetadataScraper
) {
    suspend operator fun invoke(
        song: Song,
        mode: ScrapeSearchMode = ScrapeSearchMode.ALL
    ): ScrapeCandidates {
        val all = compositeMetadataScraper.collectAllCandidates(song)
        val tracks = all.tracks.mapNotNull { it.toCandidate(mode) }
        return ScrapeCandidates(
            lyrics = tracks.mapNotNull { it.lyrics }.sortedByDescending { it.score },
            artworks = tracks.mapNotNull { it.artwork }.sortedByDescending { it.score },
            tracks = tracks.sortedByDescending { it.score }
        )
    }
}

private fun LyricsResult.toCandidate() = LyricsCandidate(
    provider = provider,
    lyrics = lyrics,
    synced = synced,
    score = score
)

private fun ArtworkResult.toCandidate() = ArtworkCandidate(
    provider = provider,
    imageUri = imageUri,
    score = score
)

private fun TrackMetadataResult.toCandidate(mode: ScrapeSearchMode): TrackMetadataCandidate? {
    val lyricsCandidate = lyrics?.toCandidate()
    val artworkCandidate = artwork?.toCandidate()
    return when (mode) {
        ScrapeSearchMode.ALL -> TrackMetadataCandidate(
            provider = provider,
            title = title,
            artistName = artistName,
            albumTitle = albumTitle,
            releaseYear = releaseYear,
            releaseMonth = releaseMonth,
            lyrics = lyricsCandidate,
            artwork = artworkCandidate,
            score = score
        )
        ScrapeSearchMode.LYRICS -> lyricsCandidate?.let {
            TrackMetadataCandidate(
                provider = provider,
                title = title,
                artistName = artistName,
                albumTitle = albumTitle,
                releaseYear = releaseYear,
                releaseMonth = releaseMonth,
                lyrics = it,
                score = it.score
            )
        }
        ScrapeSearchMode.ARTWORK -> artworkCandidate?.let {
            TrackMetadataCandidate(
                provider = provider,
                title = title,
                artistName = artistName,
                albumTitle = albumTitle,
                releaseYear = releaseYear,
                releaseMonth = releaseMonth,
                artwork = it,
                score = it.score
            )
        }
    }?.takeIf { it.hasMetadata }
}
