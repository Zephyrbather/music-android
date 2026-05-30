package com.musicplayer.app.data.scraper

import com.musicplayer.app.domain.model.Song
import com.musicplayer.app.domain.model.isUnresolvedAlbumTitle
import java.util.Calendar
import java.util.TimeZone

data class MetadataSearchInfo(
    val title: String,
    val artist: String?,
    val album: String?,
    val durationSeconds: Long?
)

data class LyricsResult(
    val provider: String,
    val lyrics: String,
    val synced: Boolean,
    val score: Int
)

data class ArtworkResult(
    val provider: String,
    val imageUri: String,
    val score: Int
)

data class ArtistArtworkResult(
    val provider: String,
    val imageUri: String,
    val score: Int
)

data class TrackMetadataResult(
    val provider: String,
    val title: String? = null,
    val artistName: String? = null,
    val albumTitle: String? = null,
    val releaseYear: Int? = null,
    val releaseMonth: Int? = null,
    val lyrics: LyricsResult? = null,
    val artwork: ArtworkResult? = null,
    val score: Int
) {
    val hasMetadata: Boolean
        get() = lyrics != null || artwork != null || !albumTitle.isNullOrBlank() || releaseYear != null
}

fun Song.toSearchInfo(): MetadataSearchInfo = MetadataSearchInfo(
    title = title,
    artist = artistName.takeUnless { it == "Unknown Artist" || it == "Local Folder" },
    album = albumTitle.takeUnless { it.isUnresolvedAlbumTitle() },
    durationSeconds = durationMs.takeIf { it > 0L }?.div(1000L)
)

internal data class ReleaseDateParts(
    val year: Int?,
    val month: Int?
)

internal fun parseReleaseDateParts(value: String?): ReleaseDateParts {
    val text = value.orEmpty().trim()
    if (text.isBlank() || text == "0") return ReleaseDateParts(null, null)
    val match = Regex("""(\d{4})(?:[./\-年](\d{1,2}))?""").find(text)
    val year = match?.groupValues?.getOrNull(1)?.toIntOrNull()
    val month = match?.groupValues?.getOrNull(2)?.toIntOrNull()?.takeIf { it in 1..12 }
    return ReleaseDateParts(year, month)
}

internal fun releaseDatePartsFromEpochMillis(value: Long?): ReleaseDateParts {
    val millis = value?.takeIf { it > 0L } ?: return ReleaseDateParts(null, null)
    val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        timeInMillis = millis
    }
    return ReleaseDateParts(
        year = calendar.get(Calendar.YEAR),
        month = calendar.get(Calendar.MONTH) + 1
    )
}
