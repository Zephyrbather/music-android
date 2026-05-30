package com.musicplayer.app.domain.usecase

import com.musicplayer.app.domain.model.Song
import com.musicplayer.app.domain.repository.MusicRepository
import com.musicplayer.app.domain.repository.WebDavRepository
import javax.inject.Inject

class SyncWebDavUseCase @Inject constructor(
    private val webDavRepository: WebDavRepository,
    private val musicRepository: MusicRepository
) {
    suspend operator fun invoke(serverId: String): List<Song> {
        val songs = webDavRepository.syncServer(serverId)
        musicRepository.upsertSongs(songs)
        return songs
    }
}
