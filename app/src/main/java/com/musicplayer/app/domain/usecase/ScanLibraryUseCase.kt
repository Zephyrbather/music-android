package com.musicplayer.app.domain.usecase

import com.musicplayer.app.domain.model.Song
import com.musicplayer.app.domain.repository.MusicRepository
import javax.inject.Inject

class ScanLibraryUseCase @Inject constructor(
    private val musicRepository: MusicRepository
) {
    suspend operator fun invoke(onBatch: suspend (List<Song>) -> Unit = {}): List<Song> {
        val songs = musicRepository.scanLocalLibrary { batch ->
            musicRepository.upsertSongs(batch, rebuildLibraryArtwork = false)
            onBatch(batch)
        }
        musicRepository.rebuildLibraryArtwork(fetchArtistArtwork = false)
        return songs
    }
}
