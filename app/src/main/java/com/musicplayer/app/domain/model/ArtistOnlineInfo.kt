package com.musicplayer.app.domain.model

data class ArtistOnlineInfo(
    val name: String,
    val bio: String,
    val avatarUri: String?,
    val provider: String
)
