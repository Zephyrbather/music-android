package com.musicplayer.app.ui.player

import android.content.Context
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musicplayer.app.data.local.dao.CommentDao
import com.musicplayer.app.data.local.dao.PlayRecordDao
import com.musicplayer.app.data.local.dao.SongDao
import com.musicplayer.app.data.local.entity.CommentEntity
import com.musicplayer.app.data.local.entity.PlayRecordEntity
import com.musicplayer.app.data.local.entity.toDomain
import com.musicplayer.app.data.local.entity.toEntity
import com.musicplayer.app.domain.model.ArtworkCandidate
import com.musicplayer.app.domain.model.LyricsCandidate
import com.musicplayer.app.domain.model.MetadataProvider
import com.musicplayer.app.domain.model.PlaybackMode
import com.musicplayer.app.domain.model.PlaybackState
import com.musicplayer.app.domain.model.Playlist
import com.musicplayer.app.domain.model.ScrapeCandidates
import com.musicplayer.app.domain.model.ScrapeSearchMode
import com.musicplayer.app.domain.model.Song
import com.musicplayer.app.domain.model.SongCategoryTags
import com.musicplayer.app.domain.model.TrackMetadataCandidate
import com.musicplayer.app.domain.repository.MusicRepository
import com.musicplayer.app.domain.repository.PlaylistRepository
import com.musicplayer.app.domain.repository.SettingsRepository
import com.musicplayer.app.domain.usecase.CollectScrapeCandidatesUseCase
import com.musicplayer.app.domain.usecase.AiGenerateSongCategoryResult
import com.musicplayer.app.domain.usecase.AiGenerateSongCategoryUseCase
import com.musicplayer.app.domain.usecase.AiMetadataScrapeResult
import com.musicplayer.app.domain.usecase.AiScrapeMetadataUseCase
import com.musicplayer.app.domain.usecase.FetchMetadataUseCase
import com.musicplayer.app.domain.usecase.ManagePlaylistUseCase
import com.musicplayer.app.domain.usecase.PlayMusicUseCase
import com.musicplayer.app.playback.EqualizerController
import com.musicplayer.app.playback.MusicPlaybackService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EqualizerPreset(
    val id: String,
    val name: String,
    val bandLevels: List<Float>,
    val editable: Boolean = false
)

data class PlayerControlsState(
    val presets: List<EqualizerPreset> = defaultEqualizerPresets(),
    val selectedPresetId: String = "flat",
    val draftName: String = "",
    val bandLevels: List<Float> = List(10) { 0f },
    val bandFrequencies: List<Int> = listOf(30, 60, 120, 250, 500, 1000, 2000, 4000, 8000, 16000),
    val adaptiveEqualizerEnabled: Boolean = false,
    val categoryPresetMap: Map<String, String> = SongCategoryTags.defaultPresetByCategory,
    val selectedMetadataProvider: MetadataProvider = MetadataProvider.AUTO,
    val scraping: Boolean = false,
    val eqPanelOpen: Boolean = false,
    val searchKeyword: String = "",
    val scrapeSearchMode: ScrapeSearchMode = ScrapeSearchMode.ALL,
    val aiScraping: Boolean = false,
    val aiScrapeStatus: AiScrapeStatus? = null,
    val categoryGenerating: Boolean = false,
    val categoryStatus: AiCategoryStatus? = null
) {
    val selectedPreset: EqualizerPreset?
        get() = presets.firstOrNull { it.id == selectedPresetId }

    val canDelete: Boolean
        get() = selectedPreset?.editable == true
}

enum class AiScrapeStatus {
    API_KEY_REQUIRED,
    NO_SELECTION,
    FAILED,
    SAVED
}

enum class AiCategoryStatus {
    API_KEY_REQUIRED,
    FAILED,
    SAVED
}

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val playMusicUseCase: PlayMusicUseCase,
    private val fetchMetadataUseCase: FetchMetadataUseCase,
    private val equalizerController: EqualizerController,
    private val commentDao: CommentDao,
    private val songDao: SongDao,
    private val playRecordDao: PlayRecordDao,
    private val collectCandidatesUseCase: CollectScrapeCandidatesUseCase,
    private val musicRepository: MusicRepository,
    private val settingsRepository: SettingsRepository,
    private val aiScrapeMetadataUseCase: AiScrapeMetadataUseCase,
    private val aiGenerateSongCategoryUseCase: AiGenerateSongCategoryUseCase,
    private val playlistRepository: PlaylistRepository,
    private val managePlaylistUseCase: ManagePlaylistUseCase,
    @ApplicationContext private val context: Context
) : ViewModel() {
    val playbackState: StateFlow<PlaybackState> = playMusicUseCase.observePlayback()
    val desktopLyricsEnabled: StateFlow<Boolean> = settingsRepository.desktopLyricsEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)
    val playlists: StateFlow<List<Playlist>> = playlistRepository.observePlaylists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val lyricOffsetPrefs =
        context.getSharedPreferences("lyric_offsets", Context.MODE_PRIVATE)
    private val _lyricOffsetMs = MutableStateFlow(0L)
    val lyricOffsetMs: StateFlow<Long> = _lyricOffsetMs.asStateFlow()

    private val lyricFontPrefs =
        context.getSharedPreferences("lyric_font", Context.MODE_PRIVATE)
    private val _lyricFontScale = MutableStateFlow(
        lyricFontPrefs.getFloat(KEY_LYRIC_FONT_SCALE, 1f).coerceIn(LYRIC_FONT_MIN, LYRIC_FONT_MAX)
    )
    val lyricFontScale: StateFlow<Float> = _lyricFontScale.asStateFlow()

    private var lastPlayStartMs: Long = 0L
    private var lastPlaySongId: String? = null

    init {
        viewModelScope.launch {
            playbackState.map { it.currentSong?.id }.distinctUntilChanged().collect { songId ->
                recordPlayEnd()
                lastPlaySongId = songId
                lastPlayStartMs = if (songId != null) System.currentTimeMillis() else 0L
                _lyricOffsetMs.value = songId?.let { lyricOffsetPrefs.getLong(it, 0L) } ?: 0L
            }
        }
    }

    fun adjustLyricOffset(deltaMs: Long) {
        val songId = playbackState.value.currentSong?.id ?: return
        val next = (_lyricOffsetMs.value + deltaMs).coerceIn(-10_000L, 10_000L)
        _lyricOffsetMs.value = next
        lyricOffsetPrefs.edit().putLong(songId, next).apply()
    }

    fun resetLyricOffset() {
        val songId = playbackState.value.currentSong?.id ?: return
        _lyricOffsetMs.value = 0L
        lyricOffsetPrefs.edit().remove(songId).apply()
    }

    fun adjustLyricFontScale(delta: Float) {
        val next = (_lyricFontScale.value + delta)
            .coerceIn(LYRIC_FONT_MIN, LYRIC_FONT_MAX)
            .let { (it * 100).toInt() / 100f }
        _lyricFontScale.value = next
        lyricFontPrefs.edit().putFloat(KEY_LYRIC_FONT_SCALE, next).apply()
    }

    fun resetLyricFontScale() {
        _lyricFontScale.value = 1f
        lyricFontPrefs.edit().remove(KEY_LYRIC_FONT_SCALE).apply()
    }

    private fun recordPlayEnd() {
        val songId = lastPlaySongId ?: return
        val startMs = lastPlayStartMs
        val elapsed = System.currentTimeMillis() - startMs
        lastPlaySongId = null
        lastPlayStartMs = 0L
        if (elapsed > 1000L) {
            viewModelScope.launch {
                playRecordDao.insert(
                    PlayRecordEntity(
                        id = UUID.randomUUID().toString(),
                        songId = songId,
                        listenedAtEpochMs = startMs,
                        durationMs = elapsed
                    )
                )
            }
        }
    }

    private val controls = MutableStateFlow(
        PlayerControlsState(
            bandFrequencies = equalizerController.bandFrequencies,
            bandLevels = List(equalizerController.numberOfBands) { 0f }
        )
    )
    val controlsState: StateFlow<PlayerControlsState> = controls
    private var latestSongCategoryById: Map<String, String?> = emptyMap()
    private var lastAdaptivePresetId: String? = null

    init {
        viewModelScope.launch {
            settingsRepository.adaptiveEqualizerEnabled.collect { enabled ->
                controls.update { it.copy(adaptiveEqualizerEnabled = enabled) }
                lastAdaptivePresetId = null
                if (enabled) applyAdaptiveEqualizer()
            }
        }
        viewModelScope.launch {
            settingsRepository.equalizerCategoryPresets.collect { mappings ->
                controls.update { it.copy(categoryPresetMap = mappings) }
                if (controls.value.adaptiveEqualizerEnabled) {
                    lastAdaptivePresetId = null
                    applyAdaptiveEqualizer()
                }
            }
        }
        viewModelScope.launch {
            musicRepository.observeSongs()
                .map { songs -> songs.associate { it.id to it.categoryTag } }
                .distinctUntilChanged()
                .collect { categoryById ->
                    latestSongCategoryById = categoryById
                    applyAdaptiveEqualizer()
                }
        }
        viewModelScope.launch {
            playbackState.map { it.currentSong?.id }
                .distinctUntilChanged()
                .collect {
                    controls.update { state -> state.copy(categoryStatus = null) }
                    lastAdaptivePresetId = null
                    applyAdaptiveEqualizer()
                }
        }
    }

    fun play(song: Song, queue: List<Song>) {
        playMusicUseCase.play(song, queue)
        startPlayback(song)
    }

    fun playQueueSong(song: Song) {
        playMusicUseCase.playFromQueue(song)
        startPlayback(song)
    }

    fun togglePlayPause() {
        val wasPlaying = playbackState.value.isPlaying
        playMusicUseCase.togglePlayPause()
        val currentSong = playbackState.value.currentSong ?: return
        if (wasPlaying) {
            recordPlayEnd()
            context.startService(MusicPlaybackService.pauseIntent(context))
        } else {
            startPlayback(currentSong)
        }
    }

    fun skipNext() {
        playMusicUseCase.skipNext()?.let(::startPlayback)
    }

    fun skipPrevious() {
        playMusicUseCase.skipPrevious()?.let(::startPlayback)
    }

    fun setPlaybackMode(mode: PlaybackMode) {
        playMusicUseCase.setPlaybackMode(mode)
    }

    fun toggleDesktopLyrics() {
        context.startService(MusicPlaybackService.toggleDesktopLyricsIntent(context))
    }

    fun addCurrentSongToPlaylist(playlistId: String) {
        val songId = playbackState.value.currentSong?.id ?: return
        viewModelScope.launch {
            managePlaylistUseCase.addSong(playlistId, songId)
        }
    }

    fun createPlaylistForCurrentSong() {
        val songId = playbackState.value.currentSong?.id ?: return
        viewModelScope.launch {
            val playlist = managePlaylistUseCase.create("New Mix")
            managePlaylistUseCase.addSong(playlist.id, songId)
        }
    }

    fun seekTo(positionMs: Long) {
        playMusicUseCase.seekTo(positionMs)
        context.startService(MusicPlaybackService.seekIntent(context, positionMs))
    }

    fun openEqualizer() {
        controls.update { it.copy(eqPanelOpen = true) }
    }

    fun closeEqualizer() {
        controls.update { it.copy(eqPanelOpen = false) }
    }

    fun selectPreset(preset: EqualizerPreset) {
        applyPreset(preset)
    }

    fun setDraftName(name: String) {
        controls.update { it.copy(draftName = name) }
    }

    fun setBandLevel(index: Int, value: Float) {
        equalizerController.setBandLevel(index.toShort(), value.toEqualizerBandLevel())
        controls.update { state ->
            state.copy(bandLevels = state.bandLevels.toMutableList().also { it[index] = value })
        }
    }

    fun setAdaptiveEqualizerEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setAdaptiveEqualizerEnabled(enabled)
            if (enabled) applyAdaptiveEqualizer()
        }
    }

    fun setCategoryPreset(category: String, presetId: String) {
        viewModelScope.launch {
            settingsRepository.setEqualizerCategoryPreset(category, presetId)
        }
    }

    fun setMetadataProvider(provider: MetadataProvider) {
        controls.update { it.copy(selectedMetadataProvider = provider) }
    }

    fun createPreset() {
        controls.update { state ->
            val id = "custom-${System.currentTimeMillis()}"
            val name = state.draftName.ifBlank { "${state.presets.count { it.editable } + 1}" }
            val preset = EqualizerPreset(id, name, state.bandLevels.toList(), editable = true)
            state.copy(
                presets = state.presets + preset,
                selectedPresetId = id,
                draftName = name
            )
        }
    }

    private fun applyAdaptiveEqualizer(categoryTag: String? = currentSongCategoryTag()) {
        val state = controls.value
        if (!state.adaptiveEqualizerEnabled) return
        val category = SongCategoryTags.normalize(categoryTag)
        val presetId = category?.let {
            state.categoryPresetMap[it] ?: SongCategoryTags.defaultPresetByCategory[it]
        } ?: "flat"
        val preset = state.presets.firstOrNull { it.id == presetId }
            ?: state.presets.firstOrNull { it.id == "flat" }
            ?: return
        if (preset.id == lastAdaptivePresetId) return
        lastAdaptivePresetId = preset.id
        applyPreset(preset, rampMs = 240L)
    }

    private fun currentSongCategoryTag(): String? {
        val song = playbackState.value.currentSong ?: return null
        return latestSongCategoryById[song.id] ?: song.categoryTag
    }

    private fun applyPreset(preset: EqualizerPreset, rampMs: Long = 0L) {
        val bandCount = controls.value.bandLevels.size
        val levels = preset.bandLevels.let { presetLevels ->
            if (presetLevels.size == bandCount) presetLevels
            else List(bandCount) { i -> presetLevels.getOrElse(i) { 0f } }
        }
        equalizerController.setBandLevels(levels.map { it.toEqualizerBandLevel() }, rampMs)
        controls.update { state ->
            state.copy(
                selectedPresetId = preset.id,
                draftName = preset.name,
                bandLevels = levels
            )
        }
    }

    fun savePreset() {
        controls.update { state ->
            val selected = state.selectedPreset ?: return@update state
            if (!selected.editable) return@update state
            state.copy(
                presets = state.presets.map {
                    if (it.id == selected.id) {
                        it.copy(
                            name = state.draftName.ifBlank { it.name },
                            bandLevels = state.bandLevels.toList()
                        )
                    } else {
                        it
                    }
                }
            )
        }
    }

    fun deletePreset() {
        controls.update { state ->
            val selected = state.selectedPreset ?: return@update state
            if (!selected.editable) return@update state
            val fallback = state.presets.first()
            state.copy(
                presets = state.presets.filterNot { it.id == selected.id },
                selectedPresetId = fallback.id,
                draftName = fallback.name,
                bandLevels = fallback.bandLevels.toList()
            )
        }
    }

    private val _scrapeCandidates = MutableStateFlow<ScrapeCandidates?>(null)
    val scrapeCandidates: StateFlow<ScrapeCandidates?> = _scrapeCandidates.asStateFlow()

    private val _selectedScrapeLyrics = MutableStateFlow<LyricsCandidate?>(null)
    val selectedScrapeLyrics: StateFlow<LyricsCandidate?> = _selectedScrapeLyrics.asStateFlow()

    private val _selectedScrapeArtwork = MutableStateFlow<ArtworkCandidate?>(null)
    val selectedScrapeArtwork: StateFlow<ArtworkCandidate?> = _selectedScrapeArtwork.asStateFlow()

    private val _selectedScrapeCandidate = MutableStateFlow<TrackMetadataCandidate?>(null)
    val selectedScrapeCandidate: StateFlow<TrackMetadataCandidate?> = _selectedScrapeCandidate.asStateFlow()
    private var scrapeBaseSong: Song? = null
    private var scrapeRequestVersion = 0L

    fun setSearchKeyword(keyword: String) {
        if (controls.value.searchKeyword == keyword) return
        scrapeRequestVersion += 1
        controls.update { it.copy(searchKeyword = keyword, scraping = false, aiScraping = false, aiScrapeStatus = null) }
        clearScrapeCandidates()
    }

    fun prepareScrapeDetail() {
        val song = playbackState.value.currentSong ?: return
        if (scrapeBaseSong?.id != song.id) {
            scrapeBaseSong = song
            scrapeRequestVersion += 1
            controls.update {
                it.copy(
                    searchKeyword = song.toDefaultScrapeKeyword(),
                    scraping = false,
                    aiScraping = false,
                    aiScrapeStatus = null
                )
            }
            clearScrapeCandidates()
        } else if (controls.value.searchKeyword.isBlank()) {
            controls.update {
                it.copy(searchKeyword = scrapeBaseSong.toDefaultScrapeKeyword())
            }
        }
    }

    fun setScrapeSearchMode(mode: ScrapeSearchMode) {
        if (controls.value.scrapeSearchMode == mode) return
        scrapeRequestVersion += 1
        controls.update { it.copy(scrapeSearchMode = mode, scraping = false, aiScraping = false, aiScrapeStatus = null) }
        clearScrapeCandidates()
    }

    fun scrapeCurrent() {
        val song = scrapeBaseSong ?: playbackState.value.currentSong ?: return
        scrapeBaseSong = song
        val ctrl = controls.value
        val requestVersion = scrapeRequestVersion + 1
        scrapeRequestVersion = requestVersion
        viewModelScope.launch {
            controls.update { it.copy(scraping = true) }
            _scrapeCandidates.value = null
            _selectedScrapeLyrics.value = null
            _selectedScrapeArtwork.value = null
            _selectedScrapeCandidate.value = null
            val searchSong = if (ctrl.searchKeyword.isNotBlank()) {
                if (ctrl.searchKeyword.trim() == song.toDefaultScrapeKeyword()) {
                    song.withDefaultScrapeKeyword(ctrl.searchKeyword)
                } else {
                    song.withSearchKeyword(ctrl.searchKeyword)
                }
            } else {
                song.withoutScrapeSourceMetadata()
            }
            val candidates = runCatching {
                collectCandidatesUseCase(searchSong, ctrl.scrapeSearchMode, forceRefresh = true)
            }.getOrElse {
                ScrapeCandidates(lyrics = emptyList(), artworks = emptyList())
            }
            val stillCurrent = scrapeRequestVersion == requestVersion &&
                controls.value.searchKeyword == ctrl.searchKeyword &&
                controls.value.scrapeSearchMode == ctrl.scrapeSearchMode
            if (stillCurrent) {
                _scrapeCandidates.value = candidates
                controls.update { it.copy(scraping = false) }
            }
        }
    }

    fun aiScrapeCurrent() {
        val song = scrapeBaseSong ?: playbackState.value.currentSong ?: return
        scrapeBaseSong = song
        val requestVersion = scrapeRequestVersion + 1
        scrapeRequestVersion = requestVersion
        viewModelScope.launch {
            if (!settingsRepository.aiFeaturesEnabled.first()) {
                controls.update { it.copy(aiScrapeStatus = AiScrapeStatus.API_KEY_REQUIRED) }
                return@launch
            }
            val apiKey = settingsRepository.aiApiKey.first()
            val model = settingsRepository.aiModel.first()
            if (apiKey.isBlank()) {
                controls.update { it.copy(aiScrapeStatus = AiScrapeStatus.API_KEY_REQUIRED) }
                return@launch
            }
            controls.update {
                it.copy(aiScraping = true, aiScrapeStatus = null, scrapeSearchMode = ScrapeSearchMode.ALL)
            }
            _scrapeCandidates.value = null
            _selectedScrapeLyrics.value = null
            _selectedScrapeArtwork.value = null
            _selectedScrapeCandidate.value = null
            val status = runCatching {
                when (val result = aiScrapeMetadataUseCase(song, apiKey, model)) {
                    is AiMetadataScrapeResult.Saved -> {
                        if (scrapeRequestVersion != requestVersion) return@runCatching AiScrapeStatus.SAVED
                        _scrapeCandidates.value = result.candidates
                        _selectedScrapeCandidate.value = result.selected
                        _selectedScrapeLyrics.value = result.selected.lyrics
                        _selectedScrapeArtwork.value = result.selected.artwork
                        scrapeBaseSong = result.song
                        playMusicUseCase.previewCurrentSong(result.song)
                        AiScrapeStatus.SAVED
                    }
                    is AiMetadataScrapeResult.NoSelection -> {
                        if (scrapeRequestVersion != requestVersion) return@runCatching AiScrapeStatus.NO_SELECTION
                        _scrapeCandidates.value = result.candidates
                        AiScrapeStatus.NO_SELECTION
                    }
                    AiMetadataScrapeResult.Failed -> AiScrapeStatus.FAILED
                }
            }.getOrElse { AiScrapeStatus.FAILED }
            if (scrapeRequestVersion == requestVersion) {
                controls.update { it.copy(aiScraping = false, aiScrapeStatus = status) }
            }
        }
    }

    fun aiGenerateCategoryForCurrentSong() {
        val song = scrapeBaseSong ?: playbackState.value.currentSong ?: return
        scrapeBaseSong = song
        viewModelScope.launch {
            if (!settingsRepository.aiFeaturesEnabled.first()) {
                controls.update { it.copy(categoryStatus = AiCategoryStatus.API_KEY_REQUIRED) }
                return@launch
            }
            val apiKey = settingsRepository.aiApiKey.first()
            val model = settingsRepository.aiModel.first()
            if (apiKey.isBlank()) {
                controls.update { it.copy(categoryStatus = AiCategoryStatus.API_KEY_REQUIRED) }
                return@launch
            }
            controls.update { it.copy(categoryGenerating = true, categoryStatus = null) }
            val status = runCatching {
                when (val result = aiGenerateSongCategoryUseCase(song, apiKey, model)) {
                    is AiGenerateSongCategoryResult.Saved -> {
                        scrapeBaseSong = result.song
                        playMusicUseCase.previewCurrentSong(result.song)
                        applyAdaptiveEqualizer(result.song.categoryTag)
                        AiCategoryStatus.SAVED
                    }
                    AiGenerateSongCategoryResult.Failed -> AiCategoryStatus.FAILED
                }
            }.getOrElse { AiCategoryStatus.FAILED }
            controls.update { it.copy(categoryGenerating = false, categoryStatus = status) }
        }
    }

    fun selectScrapeLyrics(candidate: LyricsCandidate?) {
        _selectedScrapeLyrics.value = candidate
        _selectedScrapeCandidate.value = null
    }

    fun selectScrapeArtwork(candidate: ArtworkCandidate?) {
        _selectedScrapeArtwork.value = candidate
        _selectedScrapeCandidate.value = null
    }

    fun selectScrapeCandidate(candidate: TrackMetadataCandidate?) {
        _selectedScrapeCandidate.value = candidate
        _selectedScrapeLyrics.value = candidate?.lyrics
        _selectedScrapeArtwork.value = candidate?.artwork
        previewScrapeCandidate(candidate)
    }

    fun applyScrapeSelection() {
        val song = scrapeBaseSong ?: playbackState.value.currentSong ?: return
        val selectedCandidate = _selectedScrapeCandidate.value
        val mode = controls.value.scrapeSearchMode
        val lyrics = if (mode != ScrapeSearchMode.ARTWORK) {
            _selectedScrapeLyrics.value ?: selectedCandidate?.lyrics
        } else {
            null
        }
        val artwork = if (mode != ScrapeSearchMode.LYRICS) {
            _selectedScrapeArtwork.value ?: selectedCandidate?.artwork
        } else {
            null
        }
        viewModelScope.launch {
            val saved = saveScrapeSelection(song, selectedCandidate, lyrics, artwork, mode)
            scrapeBaseSong = saved
            playMusicUseCase.previewCurrentSong(saved)
            _scrapeCandidates.value = null
            _selectedScrapeLyrics.value = null
            _selectedScrapeArtwork.value = null
            _selectedScrapeCandidate.value = null
        }
    }

    private suspend fun saveScrapeSelection(
        song: Song,
        selectedCandidate: TrackMetadataCandidate?,
        lyrics: LyricsCandidate?,
        artwork: ArtworkCandidate?,
        mode: ScrapeSearchMode
    ): Song {
        var updated = song
        if (lyrics != null && mode != ScrapeSearchMode.ARTWORK) {
            updated = updated.copy(lyrics = lyrics.lyrics, lyricsProvider = lyrics.provider)
        }
        if (artwork != null && mode != ScrapeSearchMode.LYRICS) {
            updated = updated.copy(artworkUri = artwork.imageUri, artworkProvider = artwork.provider)
        }
        if (selectedCandidate != null && mode == ScrapeSearchMode.ALL) {
            updated = updated.copy(
                title = selectedCandidate.title?.takeIf { it.isNotBlank() } ?: updated.title,
                artistName = selectedCandidate.artistName?.takeIf { it.isNotBlank() } ?: updated.artistName,
                albumTitle = selectedCandidate.albumTitle?.takeIf { it.isNotBlank() } ?: updated.albumTitle,
                releaseYear = selectedCandidate.releaseYear ?: updated.releaseYear,
                releaseMonth = selectedCandidate.releaseMonth ?: updated.releaseMonth
            )
        }
        return musicRepository.saveScrapedMetadata(
            song = updated,
            mode = mode,
            renameLocalFile = mode == ScrapeSearchMode.ALL
        )
    }

    fun clearScrapeCandidates() {
        _scrapeCandidates.value = null
        _selectedScrapeLyrics.value = null
        _selectedScrapeArtwork.value = null
        _selectedScrapeCandidate.value = null
    }

    fun clearScrapeResult() {
        clearScrapeCandidates()
    }

    private fun previewScrapeCandidate(candidate: TrackMetadataCandidate?) {
        val song = scrapeBaseSong ?: playbackState.value.currentSong ?: return
        if (candidate == null) return
        val mode = controls.value.scrapeSearchMode
        val useLyrics = mode != ScrapeSearchMode.ARTWORK
        val useArtwork = mode != ScrapeSearchMode.LYRICS
        val useSongIdentity = mode == ScrapeSearchMode.ALL
        val useAlbumInfo = mode == ScrapeSearchMode.ALL
        val previewSong = song.copy(
            title = if (useSongIdentity) candidate.title?.takeIf { it.isNotBlank() } ?: song.title else song.title,
            artistName = if (useSongIdentity) candidate.artistName?.takeIf { it.isNotBlank() } ?: song.artistName else song.artistName,
            albumTitle = if (useAlbumInfo) candidate.albumTitle?.takeIf { it.isNotBlank() } ?: song.albumTitle else song.albumTitle,
            artworkUri = if (useArtwork) candidate.artwork?.imageUri ?: song.artworkUri else song.artworkUri,
            artworkProvider = if (useArtwork) candidate.artwork?.provider ?: song.artworkProvider else song.artworkProvider,
            lyrics = if (useLyrics) candidate.lyrics?.lyrics ?: song.lyrics else song.lyrics,
            lyricsProvider = if (useLyrics) candidate.lyrics?.provider ?: song.lyricsProvider else song.lyricsProvider,
            releaseYear = if (useAlbumInfo) candidate.releaseYear ?: song.releaseYear else song.releaseYear,
            releaseMonth = if (useAlbumInfo) candidate.releaseMonth ?: song.releaseMonth else song.releaseMonth
        )
        val wasPlaying = playbackState.value.isPlaying
        val positionMs = playbackState.value.positionMs
        playMusicUseCase.previewCurrentSong(previewSong)
        ContextCompat.startForegroundService(context, MusicPlaybackService.playIntent(context, previewSong))
        if (positionMs > 0L) {
            playMusicUseCase.seekTo(positionMs)
            context.startService(MusicPlaybackService.seekIntent(context, positionMs))
        }
        if (!wasPlaying) {
            playMusicUseCase.togglePlayPause()
            context.startService(MusicPlaybackService.pauseIntent(context))
        }
    }

    private val _showCommentDialog = MutableStateFlow(false)
    val showCommentDialog: StateFlow<Boolean> = _showCommentDialog

    private val currentSongId = playbackState.map { it.currentSong?.id.orEmpty() }

    @OptIn(ExperimentalCoroutinesApi::class)
    val comments: StateFlow<List<CommentEntity>> = currentSongId
        .flatMapLatest { songId ->
            if (songId.isNotEmpty()) commentDao.observeBySongId(songId)
            else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun openComments() { _showCommentDialog.value = true }
    fun dismissComments() { _showCommentDialog.value = false }

    fun saveComment(rating: Int, text: String) {
        val song = playbackState.value.currentSong ?: return
        viewModelScope.launch {
            commentDao.upsert(
                CommentEntity(
                    id = UUID.randomUUID().toString(),
                    songId = song.id,
                    rating = rating.coerceIn(1, 5),
                    text = text.trim(),
                    timestampEpochMs = System.currentTimeMillis()
                )
            )
            if (rating == 5 && !song.isFavorite) {
                musicRepository.setFavorite(song.id, true)
            }
        }
    }

    private val _candidates = MutableStateFlow<ScrapeCandidates?>(null)
    val candidates: StateFlow<ScrapeCandidates?> = _candidates

    private val _showCandidateDialog = MutableStateFlow(false)
    val showCandidateDialog: StateFlow<Boolean> = _showCandidateDialog

    fun openCandidatePicker() {
        val song = playbackState.value.currentSong ?: return
        viewModelScope.launch {
            _candidates.value = null
            _showCandidateDialog.value = true
            _candidates.value = collectCandidatesUseCase(song, forceRefresh = true)
        }
    }

    fun dismissCandidatePicker() {
        _showCandidateDialog.value = false
        _candidates.value = null
    }

    fun applyCandidateResult(lyrics: LyricsCandidate?, artwork: ArtworkCandidate?) {
        val song = playbackState.value.currentSong ?: return
        viewModelScope.launch {
            var updated = song
            if (lyrics != null) {
                updated = updated.copy(
                    lyrics = lyrics.lyrics,
                    lyricsProvider = lyrics.provider
                )
            }
            if (artwork != null) {
                updated = updated.copy(
                    artworkUri = artwork.imageUri,
                    artworkProvider = artwork.provider
                )
            }
            musicRepository.saveScrapedMetadata(updated, mode = ScrapeSearchMode.ALL, renameLocalFile = false)
            dismissCandidatePicker()
        }
    }

    override fun onCleared() {
        super.onCleared()
        recordPlayEnd()
    }

    private fun startPlayback(song: Song) {
        ContextCompat.startForegroundService(context, MusicPlaybackService.playIntent(context, song))
    }
}

private fun defaultEqualizerPresets(): List<EqualizerPreset> = listOf(
    EqualizerPreset("flat", "Flat", listOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f)),
    EqualizerPreset("vocal", "Vocal", listOf(-2f, -1f, 0f, 1f, 3f, 3f, 2f, 1f, 0f, -1f)),
    EqualizerPreset("bass", "Bass Boost", listOf(6f, 5f, 4f, 2f, 1f, 0f, 0f, -1f, -2f, -2f)),
    EqualizerPreset("pop", "Pop", listOf(0f, 1f, 2f, 2f, 1f, 2f, 3f, 2f, 1f, 0f)),
    EqualizerPreset("rock", "Rock", listOf(4f, 4f, 2f, 1f, 0f, 1f, 3f, 4f, 4f, 3f)),
    EqualizerPreset("classical", "Classical", listOf(-2f, -1f, 0f, 0f, 1f, 2f, 3f, 3f, 2f, 1f))
)

private fun Float.toEqualizerBandLevel(): Short {
    return (coerceIn(-6f, 6f) * 100f).toInt().toShort()
}

private fun Song?.toDefaultScrapeKeyword(): String {
    val song = this ?: return ""
    return song.title.toSongTitleKeyword(song.artistName)
}

private fun Song.withSearchKeyword(keyword: String): Song {
    val parts = keyword.trim()
        .split(Regex("""\s+[-–—·|/]\s+"""), limit = 2)
        .map { it.trim() }
    val searchSong = withoutScrapeSourceMetadata()
    return if (parts.size == 2 && parts[0].isNotBlank() && parts[1].isNotBlank()) {
        searchSong.copy(artistName = parts[0], title = parts[1], albumTitle = "")
    } else {
        searchSong.copy(title = keyword.trim(), artistName = "", albumTitle = "")
    }
}

private fun Song.withDefaultScrapeKeyword(keyword: String): Song {
    val titleSplit = title.trim().splitTitleArtistCandidate()
    val artistHint = artistName.takeUnless { it.isBlank() || it == "Unknown Artist" || it == "Local Folder" }
        ?: titleSplit?.let { split ->
            if (split.third) split.first else split.second
        }
    return withoutScrapeSourceMetadata().copy(
        title = keyword.trim(),
        artistName = artistHint.orEmpty(),
        albumTitle = ""
    )
}

private fun String.toSongTitleKeyword(artistName: String?): String {
    val title = trim()
    if (title.isBlank()) return ""
    val artist = artistName.orEmpty().trim()
    val split = title.splitTitleArtistCandidate() ?: return title
    val normalizedArtist = normalizedScrapeText(artist)
    val left = split.first
    val right = split.second
    return when {
        normalizedArtist.isNotBlank() && normalizedScrapeText(left) == normalizedArtist -> right
        normalizedArtist.isNotBlank() && normalizedScrapeText(right) == normalizedArtist -> left
        split.third -> right
        else -> left
    }
}

private fun String.splitTitleArtistCandidate(): Triple<String, String, Boolean>? {
    val spaced = Regex("""\s+[-–—·|/]\s+""").find(this)
    if (spaced != null) {
        val left = substring(0, spaced.range.first).trim()
        val right = substring(spaced.range.last + 1).trim()
        if (left.isNotBlank() && right.isNotBlank()) return Triple(left, right, true)
    }
    val compact = Regex("""[-–—－]""").find(this) ?: return null
    val left = substring(0, compact.range.first).trim()
    val right = substring(compact.range.last + 1).trim()
    return if (left.isNotBlank() && right.isNotBlank()) Triple(left, right, false) else null
}

private fun normalizedScrapeText(value: String): String {
    return value.lowercase()
        .replace(Regex("[^\\p{L}\\p{N}]+"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()
}

private fun Song.withoutScrapeSourceMetadata(): Song {
    return copy(
        lyrics = null,
        lyricsProvider = null,
        artworkUri = null,
        artworkProvider = null,
        releaseYear = null,
        releaseMonth = null
    )
}

private const val KEY_LYRIC_FONT_SCALE = "font_scale"
private const val LYRIC_FONT_MIN = 0.75f
private const val LYRIC_FONT_MAX = 1.5f
