package com.musicplayer.app.domain.model

data class Artist(
    val id: String,
    val name: String,
    val albumCount: Int = 0,
    val songCount: Int = 0,
    val artworkUri: String? = null
)
