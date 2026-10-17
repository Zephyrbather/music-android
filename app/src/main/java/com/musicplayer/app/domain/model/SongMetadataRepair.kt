package com.musicplayer.app.domain.model

data class SongMetadataRepair(
    val songId: String,
    val title: String,
    val artistName: String,
    val albumTitle: String
) {
    fun artistId(): String = stableMusicId("artist", artistName)

    fun albumId(): String? {
        if (albumTitle.isUnresolvedAlbumTitle()) return null
        return stableMusicId("album", "$artistName-$albumTitle")
    }
}
