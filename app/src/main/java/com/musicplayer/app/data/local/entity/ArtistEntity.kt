package com.musicplayer.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.musicplayer.app.domain.model.Artist

@Entity(tableName = "artists")
data class ArtistEntity(
    @PrimaryKey val id: String,
    val name: String,
    val albumCount: Int,
    val songCount: Int,
    val artworkUri: String?
)

fun ArtistEntity.toDomain(): Artist = Artist(
    id = id,
    name = name,
    albumCount = albumCount,
    songCount = songCount,
    artworkUri = artworkUri
)

fun Artist.toEntity(): ArtistEntity = ArtistEntity(
    id = id,
    name = name,
    albumCount = albumCount,
    songCount = songCount,
    artworkUri = artworkUri
)
