package com.musicplayer.app.domain.usecase

import com.musicplayer.app.domain.model.Playlist
import com.musicplayer.app.domain.repository.PlaylistRepository
import javax.inject.Inject

class ManagePlaylistUseCase @Inject constructor(
    private val playlistRepository: PlaylistRepository
) {
    suspend fun create(name: String, description: String? = null): Playlist {
        return playlistRepository.createPlaylist(name, description)
    }

    suspend fun addSong(playlistId: String, songId: String) {
        playlistRepository.addSong(playlistId, songId)
    }

    suspend fun removeSong(playlistId: String, songId: String) {
        playlistRepository.removeSong(playlistId, songId)
    }
}
