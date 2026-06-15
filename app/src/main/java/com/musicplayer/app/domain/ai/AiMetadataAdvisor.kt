package com.musicplayer.app.domain.ai

import com.musicplayer.app.domain.model.Song
import com.musicplayer.app.domain.model.TrackMetadataCandidate

data class AiSongIdentity(
    val title: String,
    val artistName: String
)

data class AiPlaylistSuggestion(
    val name: String,
    val description: String?,
    val songIds: List<String>
)

interface AiMetadataAdvisor {
    suspend fun identifySong(
        song: Song,
        apiKey: String,
        model: String
    ): AiSongIdentity?

    suspend fun selectCandidate(
        identity: AiSongIdentity,
        candidates: List<TrackMetadataCandidate>,
        apiKey: String,
        model: String
    ): TrackMetadataCandidate?

    suspend fun classifySong(
        song: Song,
        apiKey: String,
        model: String,
        categories: List<String>
    ): String?

    suspend fun generatePlaylist(
        prompt: String,
        songs: List<Song>,
        apiKey: String,
        model: String,
        maxSongs: Int = 30
    ): AiPlaylistSuggestion?

    suspend fun generateSemanticTags(
        song: Song,
        apiKey: String,
        model: String
    ): List<String>
}
