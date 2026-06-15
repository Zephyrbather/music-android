package com.musicplayer.app.domain.usecase

import com.musicplayer.app.domain.ai.AiMetadataAdvisor
import com.musicplayer.app.domain.model.Playlist
import com.musicplayer.app.domain.repository.MusicRepository
import com.musicplayer.app.domain.repository.PlaylistRepository
import com.musicplayer.app.domain.repository.SettingsRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.first

sealed interface AiGeneratePlaylistResult {
    data class Created(val playlist: Playlist, val songCount: Int) : AiGeneratePlaylistResult
    data object Disabled : AiGeneratePlaylistResult
    data object ApiKeyRequired : AiGeneratePlaylistResult
    data object EmptyLibrary : AiGeneratePlaylistResult
    data object NoSelection : AiGeneratePlaylistResult
    data object Failed : AiGeneratePlaylistResult
}

class AiGeneratePlaylistUseCase @Inject constructor(
    private val musicRepository: MusicRepository,
    private val playlistRepository: PlaylistRepository,
    private val settingsRepository: SettingsRepository,
    private val aiMetadataAdvisor: AiMetadataAdvisor
) {
    suspend operator fun invoke(prompt: String): AiGeneratePlaylistResult {
        if (!settingsRepository.aiFeaturesEnabled.first() || !settingsRepository.aiPlaylistEnabled.first()) {
            return AiGeneratePlaylistResult.Disabled
        }
        val apiKey = settingsRepository.aiApiKey.first()
        if (apiKey.isBlank()) return AiGeneratePlaylistResult.ApiKeyRequired
        val songs = musicRepository.observeSongs().first()
            .filter { !it.uri.startsWith("demo://") }
        if (songs.isEmpty()) return AiGeneratePlaylistResult.EmptyLibrary
        val model = settingsRepository.aiModel.first()
        val suggestion = runCatching {
            aiMetadataAdvisor.generatePlaylist(
                prompt = prompt,
                songs = songs,
                apiKey = apiKey,
                model = model
            )
        }.getOrNull() ?: return AiGeneratePlaylistResult.NoSelection
        val playlist = playlistRepository.createPlaylist(
            name = suggestion.name,
            description = suggestion.description
        )
        suggestion.songIds.forEach { songId ->
            playlistRepository.addSong(playlist.id, songId)
        }
        return AiGeneratePlaylistResult.Created(
            playlist = playlist,
            songCount = suggestion.songIds.size
        )
    }
}
