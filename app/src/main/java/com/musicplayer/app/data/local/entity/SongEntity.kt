package com.musicplayer.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.musicplayer.app.domain.model.Song
import com.musicplayer.app.domain.model.Source

@Entity(
    tableName = "songs",
    indices = [
        Index("albumId"),
        Index("artistId"),
        Index("source"),
        Index(value = ["uri"], unique = true),
        Index(value = ["albumTitle", "discNumber", "trackNumber", "title"])
    ]
)
data class SongEntity(
    @PrimaryKey val id: String,
    val title: String,
    val artistId: String?,
    val artistName: String,
    val albumId: String?,
    val albumTitle: String,
    val durationMs: Long,
    val trackNumber: Int?,
    val discNumber: Int?,
    val uri: String,
    val source: Source,
    val webDavServerId: String?,
    val artworkUri: String?,
    val artworkProvider: String?,
    val lyrics: String?,
    val lyricsProvider: String?,
    val mimeType: String?,
    val bitrateKbps: Int?,
    val sampleRateHz: Int?,
    val releaseYear: Int?,
    val releaseMonth: Int?,
    val categoryTag: String?,
    val dateAddedEpochMs: Long,
    val isFavorite: Boolean
)

fun SongEntity.toDomain(): Song = Song(
    id = id,
    title = title,
    artistId = artistId,
    artistName = artistName,
    albumId = albumId,
    albumTitle = albumTitle,
    durationMs = durationMs,
    trackNumber = trackNumber,
    discNumber = discNumber,
    uri = uri,
    source = source,
    webDavServerId = webDavServerId,
    artworkUri = artworkUri,
    artworkProvider = artworkProvider,
    lyrics = lyrics,
    lyricsProvider = lyricsProvider,
    mimeType = mimeType,
    bitrateKbps = bitrateKbps,
    sampleRateHz = sampleRateHz,
    releaseYear = releaseYear,
    releaseMonth = releaseMonth,
    categoryTag = categoryTag,
    dateAddedEpochMs = dateAddedEpochMs,
    isFavorite = isFavorite
)

fun Song.toEntity(): SongEntity = SongEntity(
    id = id,
    title = title,
    artistId = artistId,
    artistName = artistName,
    albumId = albumId,
    albumTitle = albumTitle,
    durationMs = durationMs,
    trackNumber = trackNumber,
    discNumber = discNumber,
    uri = uri,
    source = source,
    webDavServerId = webDavServerId,
    artworkUri = artworkUri,
    artworkProvider = artworkProvider,
    lyrics = lyrics,
    lyricsProvider = lyricsProvider,
    mimeType = mimeType,
    bitrateKbps = bitrateKbps,
    sampleRateHz = sampleRateHz,
    releaseYear = releaseYear,
    releaseMonth = releaseMonth,
    categoryTag = categoryTag,
    dateAddedEpochMs = dateAddedEpochMs,
    isFavorite = isFavorite
)
