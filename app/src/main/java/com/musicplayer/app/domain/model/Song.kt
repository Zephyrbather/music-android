package com.musicplayer.app.domain.model

data class Song(
    val id: String,
    val title: String,
    val artistId: String? = null,
    val artistName: String = "Unknown Artist",
    val albumId: String? = null,
    val albumTitle: String = "Unknown Album",
    val durationMs: Long = 0L,
    val trackNumber: Int? = null,
    val discNumber: Int? = null,
    val uri: String,
    val source: Source = Source.LOCAL,
    val webDavServerId: String? = null,
    val artworkUri: String? = null,
    val artworkProvider: String? = null,
    val lyrics: String? = null,
    val lyricsProvider: String? = null,
    val mimeType: String? = null,
    val bitrateKbps: Int? = null,
    val sampleRateHz: Int? = null,
    val releaseYear: Int? = null,
    val releaseMonth: Int? = null,
    val dateAddedEpochMs: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false
) {
    val qualityLabel: String
        get() {
            sampleRateHz?.let { rate ->
                return if (rate >= 1000) "%.1fkHz".format(rate / 1000f) else "${rate}Hz"
            }
            return bitrateKbps?.let { "${it}kbps" }
                ?: mimeType?.substringAfter('/')?.uppercase()
                ?: "Unknown"
        }
}
