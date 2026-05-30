package com.musicplayer.app.data.scraper

import com.musicplayer.app.domain.model.Song
import okhttp3.OkHttpClient
import javax.inject.Inject

class CoverArtArchiveScraper @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val musicBrainzScraper: MusicBrainzScraper
) {
    suspend fun attachCover(song: Song): Song {
        return findArtwork(song)?.let { result ->
            song.copy(artworkUri = result.imageUri, artworkProvider = result.provider)
        } ?: song
    }

    suspend fun findArtwork(song: Song): ArtworkResult? {
        return collectMetadataCandidate(song)?.artwork
    }

    suspend fun collectMetadataCandidate(song: Song): TrackMetadataResult? {
        val releaseGroup = musicBrainzScraper.findReleaseGroupInfo(song) ?: return null
        val frontImage = "https://coverartarchive.org/release-group/${releaseGroup.id}/front-500"
        val artwork = ArtworkResult(
            provider = "MusicBrainz / Cover Art Archive",
            imageUri = frontImage,
            score = releaseGroup.score
        )
        return TrackMetadataResult(
            provider = "MusicBrainz / Cover Art Archive",
            title = song.title.takeIf { it.isNotBlank() },
            artistName = releaseGroup.artistName,
            albumTitle = releaseGroup.title,
            releaseYear = releaseGroup.releaseYear,
            releaseMonth = releaseGroup.releaseMonth,
            artwork = artwork,
            score = releaseGroup.score
        )
    }
}
