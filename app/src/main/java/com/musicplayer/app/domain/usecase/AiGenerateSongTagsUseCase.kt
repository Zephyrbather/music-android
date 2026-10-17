package com.musicplayer.app.domain.usecase

import com.musicplayer.app.domain.ai.AiMetadataAdvisor
import com.musicplayer.app.domain.model.Song
import com.musicplayer.app.domain.repository.MusicRepository
import com.musicplayer.app.domain.repository.SettingsRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.first

sealed interface SongTagGenerationResult {
    data class Generated(val tags: List<String>) : SongTagGenerationResult
    data object Unavailable : SongTagGenerationResult
    data object Failed : SongTagGenerationResult
}

class AiGenerateSongTagsUseCase @Inject constructor(
    private val musicRepository: MusicRepository,
    private val settingsRepository: SettingsRepository,
    private val aiMetadataAdvisor: AiMetadataAdvisor
) {
    suspend operator fun invoke(song: Song): SongTagGenerationResult {
        if (!settingsRepository.aiFeaturesEnabled.first() ||
            !settingsRepository.aiSemanticTagsEnabled.first()
        ) {
            return SongTagGenerationResult.Unavailable
        }
        val apiKey = settingsRepository.aiApiKey.first()
        if (apiKey.isBlank()) return SongTagGenerationResult.Unavailable
        val model = settingsRepository.aiModel.first()
        val generated = runCatching {
            aiMetadataAdvisor.generateSemanticTags(song, apiKey, model)
        }.getOrNull()
        if (generated.isNullOrEmpty()) return SongTagGenerationResult.Failed
        val merged = mergeTags(song.semanticTags, generated)
        musicRepository.updateSongSemanticTags(song.id, merged)
        return SongTagGenerationResult.Generated(merged)
    }
}

fun mergeTags(existing: List<String>, added: List<String>): List<String> {
    val seen = LinkedHashMap<String, String>()
    (existing + added).forEach { raw ->
        val tag = raw.trim().removePrefix("#").trim()
        if (tag.isBlank() || '|' in tag) return@forEach
        seen.putIfAbsent(tag.lowercase(), tag)
    }
    return seen.values.take(12)
}
