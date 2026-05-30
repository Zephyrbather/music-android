package com.musicplayer.app.domain.model

data class Album(
    val id: String,
    val title: String,
    val artistName: String = "Unknown Artist",
    val artworkUri: String? = null,
    val songCount: Int = 0,
    val releaseYear: Int? = null,
    val releaseMonth: Int? = null,
    val source: Source = Source.LOCAL
)
