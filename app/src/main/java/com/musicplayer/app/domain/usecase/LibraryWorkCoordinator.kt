package com.musicplayer.app.domain.usecase

import com.musicplayer.app.domain.model.Song
import com.musicplayer.app.domain.model.hasUnresolvedAlbumTitle
import com.musicplayer.app.domain.repository.MusicRepository
import com.musicplayer.app.domain.repository.SettingsRepository
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
    private val settingsRepository: SettingsRepository
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
        val allSongs = musicRepository.observeSongs().first()
        val allCandidates = allSongs.filter { it.needsMetadataScrape() }

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

        val useAi = settingsRepository.backgroundAiScrapeEnabled.first()
        val apiKey = if (useAi) settingsRepository.aiApiKey.first() else ""
        val model = if (useAi) settingsRepository.aiModel.first() else ""
        if (useAi && apiKey.isBlank()) {
            settingsRepository.setBackgroundAiScrapeEnabled(false)
        }

        val startCompleted = if (freshStart) 0 else settingsRepository.scrapeResumeCompleted.first()
        settingsRepository.setScrapeResume(allCandidates.map { it.id }, startCompleted, allCandidates.size)

        state.value = LibraryWorkState(
            type = LibraryWorkType.SCRAPE,
            running = true,
            total = allCandidates.size,
            completed = startCompleted
        )

        var updated = 0
        var failed = 0
        coroutineScope {
            val parallelism = if (useAi && apiKey.isNotBlank()) AI_SCRAPE_PARALLELISM else SCRAPE_PARALLELISM
            candidates.chunked(parallelism).forEach { chunk ->
                val results = chunk.map { song ->
                    async {
                        song to runCatching {
                            val normal = fetchMetadataUseCase(song.id)
                            if (useAi && apiKey.isNotBlank() && normal?.hasMoreMetadataThan(song) != true) {
                                when (val result = aiScrapeMetadataUseCase(song, apiKey, model, renameLocalFile = false)) {
                                    is AiMetadataScrapeResult.Saved -> result.song
                                    is AiMetadataScrapeResult.NoSelection -> null
                                    AiMetadataScrapeResult.Failed -> null
                                }
                            } else {
                                normal
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
            settingsRepository.setScrapeResume(allCandidates.map { it.id }, newCompleted, allCandidates.size)
            }
        }

        state.update {
            it.copy(
                running = false,
                lastResult = "scrape:$updated:${allCandidates.size - updated - failed}:$failed"
            )
        }
        settingsRepository.clearScrapeResume()
        musicRepository.rebuildLibraryArtwork(fetchArtistArtwork = true)
    }

    private companion object {
        const val SCRAPE_PARALLELISM = 8
        const val AI_SCRAPE_PARALLELISM = 1
    }
}

private fun Song.needsMetadataScrape(): Boolean {
    return artworkUri.isNullOrBlank() || lyrics.isNullOrBlank() || releaseYear == null || hasUnresolvedAlbumTitle()
}

private fun Song.hasMoreMetadataThan(before: Song): Boolean {
    return (before.artworkUri.isNullOrBlank() && !artworkUri.isNullOrBlank()) ||
        (before.lyrics.isNullOrBlank() && !lyrics.isNullOrBlank()) ||
        title != before.title ||
        artistName != before.artistName ||
        albumTitle != before.albumTitle ||
        releaseYear != before.releaseYear ||
        releaseMonth != before.releaseMonth
}
