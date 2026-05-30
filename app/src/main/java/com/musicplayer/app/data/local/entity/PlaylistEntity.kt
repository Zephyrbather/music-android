package com.musicplayer.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.musicplayer.app.domain.model.Playlist

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String?,
    val artworkUri: String?,
    val songCount: Int,
    val sortOrder: Int = 0,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long
)

fun PlaylistEntity.toDomain(): Playlist = Playlist(
    id = id,
    name = name,
    description = description,
    artworkUri = artworkUri,
    songCount = songCount,
    sortOrder = sortOrder,
    createdAtEpochMs = createdAtEpochMs,
    updatedAtEpochMs = updatedAtEpochMs
)

fun Playlist.toEntity(): PlaylistEntity = PlaylistEntity(
    id = id,
    name = name,
    description = description,
    artworkUri = artworkUri,
    songCount = songCount,
    sortOrder = sortOrder,
    createdAtEpochMs = createdAtEpochMs,
    updatedAtEpochMs = updatedAtEpochMs
)
