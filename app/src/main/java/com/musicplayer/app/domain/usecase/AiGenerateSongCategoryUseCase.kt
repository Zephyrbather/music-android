package com.musicplayer.app.domain.usecase

import com.musicplayer.app.domain.ai.AiMetadataAdvisor
import com.musicplayer.app.domain.model.Song
import com.musicplayer.app.domain.model.SongCategoryTags
import com.musicplayer.app.domain.repository.MusicRepository
import javax.inject.Inject

sealed interface AiGenerateSongCategoryResult {
    data class Saved(val category: String, val song: Song) : AiGenerateSongCategoryResult
    data object Failed : AiGenerateSongCategoryResult
}

class AiGenerateSongCategoryUseCase @Inject constructor(
    private val aiMetadataAdvisor: AiMetadataAdvisor,
    private val musicRepository: MusicRepository
) {
    suspend operator fun invoke(
        song: Song,
        apiKey: String,
        model: String
    ): AiGenerateSongCategoryResult {
        val category = aiMetadataAdvisor.classifySong(
            song = song,
            apiKey = apiKey,
            model = model,
            categories = SongCategoryTags.supported
        ) ?: return AiGenerateSongCategoryResult.Failed
        val saved = musicRepository.updateSongCategory(song.id, category)
            ?: song.copy(categoryTag = category)
        return AiGenerateSongCategoryResult.Saved(category, saved)
    }
}
