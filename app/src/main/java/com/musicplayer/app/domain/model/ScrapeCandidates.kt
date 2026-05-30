package com.musicplayer.app.domain.model

data class ScrapeCandidates(
    val lyrics: List<LyricsCandidate>,
    val artworks: List<ArtworkCandidate>,
    val tracks: List<TrackMetadataCandidate> = emptyList()
)

enum class ScrapeSearchMode {
    ALL,
    LYRICS,
    ARTWORK
}

data class LyricsCandidate(
    val provider: String,
    val lyrics: String,
    val synced: Boolean,
    val score: Int
)

data class ArtworkCandidate(
    val provider: String,
    val imageUri: String,
    val score: Int
)

data class TrackMetadataCandidate(
    val provider: String,
    val title: String? = null,
    val artistName: String? = null,
    val albumTitle: String? = null,
    val releaseYear: Int? = null,
    val releaseMonth: Int? = null,
    val lyrics: LyricsCandidate? = null,
    val artwork: ArtworkCandidate? = null,
    val score: Int
) {
    val hasMetadata: Boolean
        get() = lyrics != null || artwork != null || !albumTitle.isNullOrBlank() || releaseYear != null
}
