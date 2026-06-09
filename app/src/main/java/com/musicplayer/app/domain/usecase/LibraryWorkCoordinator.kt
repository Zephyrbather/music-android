package com.musicplayer.app.domain.usecase

import com.musicplayer.app.domain.model.Song
import com.musicplayer.app.domain.model.hasUnresolvedAlbumTitle
import com.musicplayer.app.domain.repository.MusicRepository
import com.musicplayer.app.domain.repository.SettingsRepository
import com.musicplayer.app.performance.CpuPerformanceManager
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class LibraryWorkType {
    NONE,
    SCAN,
    SCRAPE
}

data class LibraryWorkState(
    val type: LibraryWorkType = LibraryWorkType.NONE,
    val running: Boolean = false,
    val completed: Int = 0,
    val total: Int = 0,
    val discovered: Int = 0,
    val updated: Int = 0,
    val failed: Int = 0,
    val lastResult: String? = null
) {
    val progress: Float?
        get() = total.takeIf { it > 0 }?.let { completed.toFloat() / it.toFloat() }
}

@Singleton
class LibraryWorkCoordinator @Inject constructor(
    private val musicRepository: MusicRepository,
    private val scanLibraryUseCase: ScanLibraryUseCase,
    private val fetchMetadataUseCase: FetchMetadataUseCase,
    private val aiScrapeMetadataUseCase: AiScrapeMetadataUseCase,
    private val aiGenerateSongCategoryUseCase: AiGenerateSongCategoryUseCase,
    private val settingsRepository: SettingsRepository,
    private val cpuPerformanceManager: CpuPerformanceManager
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val state = MutableStateFlow(LibraryWorkState())

    fun observeState(): StateFlow<LibraryWorkState> = state

    val hasResumableScrape: StateFlow<Boolean> =
        settingsRepository.scrapeResumeTotal.map { it > 0 }
            .stateIn(scope, SharingStarted.Eagerly, false)

    fun startScan() {
        if (state.value.running) return
        scope.launch {
            cpuPerformanceManager.setBackgroundPriority()
            state.value = LibraryWorkState(type = LibraryWorkType.SCAN, running = true)
            var discovered = 0
            runCatching {
                scanLibraryUseCase { batch ->
                    discovered += batch.size
                    state.update { it.copy(discovered = discovered) }
                }
            }
                .onSuccess { songs ->
                    state.value = LibraryWorkState(
                        type = LibraryWorkType.SCAN,
                        running = false,
                        completed = songs.size,
                        total = songs.size,
                        discovered = discovered,
                        lastResult = "scan:${songs.size}"
                    )
                }
                .onFailure {
                    state.value = LibraryWorkState(
                        type = LibraryWorkType.SCAN,
                        running = false,
                        failed = 1,
                        lastResult = "scan_failed"
                    )
                }
        }
    }

    fun startScrapeMissing() {
        if (state.value.running) return
        scope.launch {
            settingsRepository.clearScrapeResume()
            runScrape(freshStart = true)
        }
    }

    fun resumeScrapeMissing() {
        if (state.value.running) return
        scope.launch {
            runScrape(freshStart = false)
        }
    }

    private suspend fun runScrape(freshStart: Boolean) {
        cpuPerformanceManager.setBackgroundPriority()
        val allSongs = musicRepository.observeSongs().first()
        val requestedAiMetadataScrape = settingsRepository.backgroundAiScrapeEnabled.first()
        val requestedAiCategoryScrape = settingsRepository.aiCategoryScrapeEnabled.first()
        val requiresAi = requestedAiMetadataScrape || requestedAiCategoryScrape
        val apiKey = if (requiresAi) settingsRepository.aiApiKey.first() else ""
        val model = if (requiresAi) settingsRepository.aiModel.first() else ""
        if (requiresAi && apiKey.isBlank()) {
            if (requestedAiMetadataScrape) settingsRepository.setBackgroundAiScrapeEnabled(false)
            if (requestedAiCategoryScrape) settingsRepository.setAiCategoryScrapeEnabled(false)
        }
        val useAiMetadataScrape = requestedAiMetadataScrape && apiKey.isNotBlank()
        val useAiCategoryScrape = requestedAiCategoryScrape && apiKey.isNotBlank()
        val allCandidates = allSongs.filter {
            it.needsMetadataScrape() || (useAiCategoryScrape && it.needsCategoryScrape())
        }

        val candidates = if (freshStart) {
            allCandidates
        } else {
            val savedCompleted = settingsRepository.scrapeResumeCompleted.first()
            allCandidates.drop(savedCompleted)
        }

        if (candidates.isEmpty()) {
            settingsRepository.clearScrapeResume()
            return
        }

        val startCompleted = if (freshStart) 0 else settingsRepository.scrapeResumeCompleted.first()
        val candidateIds = allCandidates.map { it.id }
        settingsRepository.setScrapeResume(candidateIds, startCompleted, allCandidates.size)

        state.value = LibraryWorkState(
            type = LibraryWorkType.SCRAPE,
            running = true,
            total = allCandidates.size,
            completed = startCompleted
        )

        var updated = 0
        var failed = 0
        coroutineScope {
            val parallelism = if (useAiMetadataScrape || useAiCategoryScrape) AI_SCRAPE_PARALLELISM else SCRAPE_PARALLELISM
            candidates.chunked(parallelism).forEach { chunk ->
                val results = chunk.map { song ->
                    async {
                        song to runCatching {
                            var resultSong: Song? = null
                            if (song.needsMetadataScrape()) {
                                val normal = fetchMetadataUseCase(
                                    songId = song.id,
                                    rebuildLibraryArtwork = false
                                )
                                resultSong = if (useAiMetadataScrape && normal?.hasMoreMetadataThan(song) != true) {
                                    when (val result = aiScrapeMetadataUseCase(
                                        song = song,
                                        apiKey = apiKey,
                                        model = model,
                                        renameLocalFile = false,
                                        rebuildLibraryArtwork = false
                                    )) {
                                        is AiMetadataScrapeResult.Saved -> result.song
                                        is AiMetadataScrapeResult.NoSelection -> null
                                        AiMetadataScrapeResult.Failed -> null
                                    }
                                } else {
                                    normal
                                }
                            }
                            val categoryBase = resultSong ?: song
                            if (useAiCategoryScrape && categoryBase.needsCategoryScrape()) {
                                when (val result = aiGenerateSongCategoryUseCase(
                                    song = categoryBase,
                                    apiKey = apiKey,
                                    model = model
                                )) {
                                    is AiGenerateSongCategoryResult.Saved -> result.song
                                    AiGenerateSongCategoryResult.Failed -> resultSong
                                }
                            } else {
                                resultSong
                            }
                        }.getOrNull()
                    }
                }.awaitAll()
                for ((song, result) in results) {
                    if (result != null && result.hasMoreMetadataThan(song)) {
                        updated += 1
                    } else if (result == null) {
                        failed += 1
                    }
                }
                val newCompleted = state.value.completed + chunk.size
                state.update {
                    it.copy(
                        completed = newCompleted,
                        updated = updated,
                        failed = failed
                    )
                }
                if (results.any { (song, result) -> result != null && result.hasMoreMetadataThan(song) }) {
                    musicRepository.rebuildLibraryArtwork(fetchArtistArtwork = false)
                }
                settingsRepository.setScrapeResume(candidateIds, newCompleted, allCandidates.size)
            }
        }

        state.update {
            it.copy(
                running = false,
                lastResult = "scrape:$updated:${allCandidates.size - updated - failed}:$failed"
            )
        }
        settingsRepository.clearScrapeResume()
        scope.launch {
            musicRepository.rebuildLibraryArtwork(fetchArtistArtwork = true)
        }
    }

    private companion object {
        const val SCRAPE_PARALLELISM = 8
        const val AI_SCRAPE_PARALLELISM = 1
    }
}

private fun Song.needsMetadataScrape(): Boolean {
    return artworkUri.isNullOrBlank() || lyrics.isNullOrBlank() || releaseYear == null || hasUnresolvedAlbumTitle()
}

private fun Song.needsCategoryScrape(): Boolean {
    return categoryTag.isNullOrBlank()
}

private fun Song.hasMoreMetadataThan(before: Song): Boolean {
    return (before.artworkUri.isNullOrBlank() && !artworkUri.isNullOrBlank()) ||
        (before.lyrics.isNullOrBlank() && !lyrics.isNullOrBlank()) ||
        title != before.title ||
        artistName != before.artistName ||
        albumTitle != before.albumTitle ||
        releaseYear != before.releaseYear ||
        releaseMonth != before.releaseMonth ||
        categoryTag != before.categoryTag
}
