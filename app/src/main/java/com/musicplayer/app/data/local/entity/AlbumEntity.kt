package com.musicplayer.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.musicplayer.app.domain.model.Album
import com.musicplayer.app.domain.model.Source

@Entity(tableName = "albums")
data class AlbumEntity(
    @PrimaryKey val id: String,
    val title: String,
    val artistName: String,
    val artworkUri: String?,
    val songCount: Int,
    val releaseYear: Int?,
    val releaseMonth: Int?,
    val source: Source
)

fun AlbumEntity.toDomain(): Album = Album(
    id = id,
    title = title,
    artistName = artistName,
    artworkUri = artworkUri,
    songCount = songCount,
    releaseYear = releaseYear,
    releaseMonth = releaseMonth,
    source = source
)

fun Album.toEntity(): AlbumEntity = AlbumEntity(
    id = id,
    title = title,
    artistName = artistName,
    artworkUri = artworkUri,
    songCount = songCount,
    releaseYear = releaseYear,
    releaseMonth = releaseMonth,
    source = source
)
