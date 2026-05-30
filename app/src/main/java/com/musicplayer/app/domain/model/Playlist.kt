package com.musicplayer.app.domain.model

data class Playlist(
    val id: String,
    val name: String,
    val description: String? = null,
    val artworkUri: String? = null,
    val songCount: Int = 0,
    val sortOrder: Int = 0,
    val createdAtEpochMs: Long = System.currentTimeMillis(),
    val updatedAtEpochMs: Long = System.currentTimeMillis()
)
