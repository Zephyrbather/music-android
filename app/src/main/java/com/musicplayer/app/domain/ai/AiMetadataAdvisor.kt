package com.musicplayer.app.domain.ai

import com.musicplayer.app.domain.model.Song
import com.musicplayer.app.domain.model.TrackMetadataCandidate

data class AiSongIdentity(
    val title: String,
    val artistName: String
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
}
