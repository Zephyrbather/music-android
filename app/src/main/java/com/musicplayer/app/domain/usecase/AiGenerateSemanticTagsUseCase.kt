package com.musicplayer.app.domain.usecase

import com.musicplayer.app.domain.ai.AiMetadataAdvisor
import com.musicplayer.app.domain.repository.MusicRepository
import com.musicplayer.app.domain.repository.SettingsRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.first

data class AiSemanticTagResult(
    val updated: Int = 0,
    val failed: Int = 0,
    val skipped: Int = 0
)

class AiGenerateSemanticTagsUseCase @Inject constructor(
    private val musicRepository: MusicRepository,
    private val settingsRepository: SettingsRepository,
    private val aiMetadataAdvisor: AiMetadataAdvisor
) {
    suspend operator fun invoke(limit: Int = 50): AiSemanticTagResult {
        if (!settingsRepository.aiFeaturesEnabled.first() || !settingsRepository.aiSemanticTagsEnabled.first()) {
            return AiSemanticTagResult(skipped = 1)
        }
        val apiKey = settingsRepository.aiApiKey.first()
        if (apiKey.isBlank()) return AiSemanticTagResult(skipped = 1)
        val model = settingsRepository.aiModel.first()
        val candidates = musicRepository.observeSongs().first()
            .filter { it.semanticTags.isEmpty() && !it.uri.startsWith("demo://") }
            .take(limit.coerceAtLeast(1))
        var updated = 0
        var failed = 0
        candidates.forEach { song ->
            val tags = runCatching {
                aiMetadataAdvisor.generateSemanticTags(song, apiKey, model)
            }.getOrDefault(emptyList())
            if (tags.isEmpty()) {
                failed++
            } else {
                musicRepository.updateSongSemanticTags(song.id, tags)
                updated++
            }
        }
        return AiSemanticTagResult(
            updated = updated,
            failed = failed,
            skipped = 0
        )
    }
}
