package com.musicplayer.app.domain.usecase

import com.musicplayer.app.domain.model.MetadataProvider
import com.musicplayer.app.domain.model.Song
import com.musicplayer.app.domain.repository.MusicRepository
import javax.inject.Inject

class FetchMetadataUseCase @Inject constructor(
    private val musicRepository: MusicRepository
) {
    suspend operator fun invoke(
        songId: String,
        provider: MetadataProvider = MetadataProvider.AUTO,
        rebuildLibraryArtwork: Boolean = true,
        forceRefresh: Boolean = false
    ): Song? = musicRepository.refreshMetadata(songId, provider, rebuildLibraryArtwork, forceRefresh)
}
