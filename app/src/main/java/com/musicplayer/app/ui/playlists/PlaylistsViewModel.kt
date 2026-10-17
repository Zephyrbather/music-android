package com.musicplayer.app.ui.playlists

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musicplayer.app.data.local.dao.PlayRecordDao
import com.musicplayer.app.data.local.dao.SongDao
import com.musicplayer.app.data.local.entity.toDomain
import com.musicplayer.app.data.recommend.RecommendedPlaylist
import com.musicplayer.app.data.recommend.RecommendedPlaylistKind
import com.musicplayer.app.data.recommend.RecommendedPlaylistsStore
import com.musicplayer.app.domain.model.Playlist
import com.musicplayer.app.domain.model.Song
import com.musicplayer.app.domain.repository.PlaylistRepository
import com.musicplayer.app.domain.repository.SettingsRepository
import com.musicplayer.app.domain.usecase.AiGeneratePlaylistResult
import com.musicplayer.app.domain.usecase.AiGeneratePlaylistUseCase
import com.musicplayer.app.domain.usecase.ManagePlaylistUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class PlaylistsUiState(
    val playlists: List<Playlist> = emptyList(),
    val aiPlaylistAvailable: Boolean = false
)

@HiltViewModel
class PlaylistsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val playlistRepository: PlaylistRepository,
    private val settingsRepository: SettingsRepository,
    private val managePlaylistUseCase: ManagePlaylistUseCase,
    private val aiGeneratePlaylistUseCase: AiGeneratePlaylistUseCase,
    private val playRecordDao: PlayRecordDao,
    private val songDao: SongDao
) : ViewModel() {
    val uiState: StateFlow<PlaylistsUiState> = kotlinx.coroutines.flow.combine(
        playlistRepository.observePlaylists(),
        settingsRepository.aiFeaturesEnabled,
        settingsRepository.aiPlaylistEnabled,
        settingsRepository.aiApiKey
    ) { playlists, aiEnabled, aiPlaylistEnabled, apiKey ->
        PlaylistsUiState(
            playlists = playlists,
            aiPlaylistAvailable = aiEnabled && aiPlaylistEnabled && apiKey.isNotBlank()
        )
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlaylistsUiState())

    private val _aiPlaylistState = MutableStateFlow<AiPlaylistUiState>(AiPlaylistUiState.Idle)
    val aiPlaylistState: StateFlow<AiPlaylistUiState> = _aiPlaylistState.asStateFlow()

    private val _recommendedPlaylists = MutableStateFlow<List<RecommendedPlaylist>>(emptyList())
    val recommendedPlaylists: StateFlow<List<RecommendedPlaylist>> = _recommendedPlaylists.asStateFlow()

    private val _recommendationsRefreshing = MutableStateFlow(false)
    val recommendationsRefreshing: StateFlow<Boolean> = _recommendationsRefreshing.asStateFlow()

    private val _librarySongs = MutableStateFlow<List<Song>>(emptyList())
    val librarySongs: StateFlow<List<Song>> = _librarySongs.asStateFlow()

    fun observePlaylistSongs(playlistId: String): Flow<List<Song>> {
        return playlistRepository.observePlaylistSongs(playlistId)
    }

    fun createPlaylist(name: String) {
        viewModelScope.launch {
            managePlaylistUseCase.create(name = name)
        }
    }

    fun deletePlaylist(playlistId: String) {
        if (playlistId == com.musicplayer.app.data.repository.PlaylistRepositoryImpl.LIKED_PLAYLIST_ID) return
        viewModelScope.launch {
            playlistRepository.deletePlaylist(playlistId)
        }
    }

    fun renamePlaylist(playlistId: String, name: String) {
        viewModelScope.launch {
            playlistRepository.renamePlaylist(playlistId, name)
        }
    }

    fun addSongsFromPlaylist(targetId: String, sourceId: String) {
        viewModelScope.launch {
            playlistRepository.addSongsFromPlaylist(targetId, sourceId)
        }
    }

    fun removeSong(playlistId: String, songId: String) {
        viewModelScope.launch {
            playlistRepository.removeSong(playlistId, songId)
        }
    }

    fun reorderPlaylists(ids: List<String>) {
        viewModelScope.launch {
            playlistRepository.reorderPlaylists(ids)
        }
    }

    fun moveSong(playlistId: String, songId: String, toPosition: Int) {
        viewModelScope.launch {
            playlistRepository.moveSong(playlistId, songId, toPosition)
        }
    }

    fun generateAiPlaylist(prompt: String) {
        if (prompt.isBlank() || _aiPlaylistState.value == AiPlaylistUiState.Generating) return
        viewModelScope.launch {
            _aiPlaylistState.value = AiPlaylistUiState.Generating
            _aiPlaylistState.value = when (val result = aiGeneratePlaylistUseCase(prompt)) {
                is AiGeneratePlaylistResult.Created -> AiPlaylistUiState.Created(result.playlist.id, result.songCount)
                AiGeneratePlaylistResult.ApiKeyRequired -> AiPlaylistUiState.ApiKeyRequired
                AiGeneratePlaylistResult.Disabled -> AiPlaylistUiState.Disabled
                AiGeneratePlaylistResult.EmptyLibrary -> AiPlaylistUiState.EmptyLibrary
                AiGeneratePlaylistResult.Failed -> AiPlaylistUiState.Failed
                AiGeneratePlaylistResult.NoSelection -> AiPlaylistUiState.NoSelection
            }
        }
    }

    fun clearAiPlaylistState() {
        _aiPlaylistState.value = AiPlaylistUiState.Idle
    }

    fun refreshRecommendations(force: Boolean = true) {
        if (_recommendationsRefreshing.value) return
        viewModelScope.launch {
            _recommendationsRefreshing.value = true
            try {
                val now = System.currentTimeMillis()
                val songs = withContext(Dispatchers.IO) {
                    songDao.getSongs().map { it.toDomain() }
                }.filter { !it.uri.startsWith("demo://") }
                _librarySongs.value = songs
                val stored = RecommendedPlaylistsStore.load(context)
                val freshEnough = stored.isNotEmpty() &&
                    now - stored.maxOf { it.updatedAtMs } < AUTO_REFRESH_MS
                if (!force && freshEnough) {
                    _recommendedPlaylists.value = stored
                    return@launch
                }
                if (songs.isEmpty()) {
                    _recommendedPlaylists.value = emptyList()
                    RecommendedPlaylistsStore.save(context, emptyList())
                    return@launch
                }
                val recent = withContext(Dispatchers.IO) {
                    playRecordDao.topSongsSince(now - RECENT_WINDOW_MS, limit = 200)
                }
                val generated = buildRecommendations(songs, recent, now)
                RecommendedPlaylistsStore.save(context, generated)
                _recommendedPlaylists.value = generated
            } finally {
                _recommendationsRefreshing.value = false
            }
        }
    }

    private fun buildRecommendations(
        songs: List<Song>,
        recent: List<PlayRecordDao.SongPlayCount>,
        now: Long
    ): List<RecommendedPlaylist> {
        val byId = songs.associateBy { it.id }
        val recentSongs = recent.mapNotNull { byId[it.songId] }
        val recentIds = recentSongs.map { it.id }.distinct()
        val tagWeights = mutableMapOf<String, Int>()
        fun addTagWeights(source: List<Song>) {
            source.forEach { song ->
                (song.semanticTags + listOfNotNull(song.categoryTag)).forEach { tag ->
                    val key = tag.trim().lowercase()
                    if (key.isNotBlank()) tagWeights.merge(key, 1, Int::plus)
                }
            }
        }
        addTagWeights(recentSongs)
        if (tagWeights.isEmpty()) addTagWeights(songs)
        val topTag = tagWeights.maxByOrNull { it.value }?.key
        val random = java.util.Random(now)

        val forYouSongs = songs.shuffled(random).sortedByDescending { song ->
            val affinity = (song.semanticTags + listOfNotNull(song.categoryTag))
                .map { it.trim().lowercase() }
                .sumOf { tagWeights[it] ?: 0 }
            affinity * 3 +
                (if (song.isFavorite) 6 else 0) +
                (if (song.id in recentIds) 4 else 0)
        }
        val recentMixIds = (recentIds + songs.shuffled(random).map { it.id })
            .distinct()
            .take(MIX_SIZE)
        val tagSongs = topTag?.let { tag ->
            songs.filter { song ->
                (song.semanticTags + listOfNotNull(song.categoryTag))
                    .any { it.trim().lowercase() == tag }
            }.shuffled(random)
        }.orEmpty()
        val newSongs = songs.shuffled(random).sortedByDescending { it.dateAddedEpochMs }

        return buildList {
            add(
                RecommendedPlaylist(
                    kind = RecommendedPlaylistKind.FOR_YOU,
                    songIds = forYouSongs.take(MIX_SIZE).map { it.id },
                    updatedAtMs = now
                )
            )
            add(
                RecommendedPlaylist(
                    kind = RecommendedPlaylistKind.RECENT,
                    songIds = recentMixIds,
                    updatedAtMs = now
                )
            )
            if (topTag != null && tagSongs.isNotEmpty()) {
                add(
                    RecommendedPlaylist(
                        kind = RecommendedPlaylistKind.TAG,
                        tag = topTag,
                        songIds = tagSongs.take(MIX_SIZE).map { it.id },
                        updatedAtMs = now
                    )
                )
            }
            add(
                RecommendedPlaylist(
                    kind = RecommendedPlaylistKind.NEW,
                    songIds = newSongs.take(MIX_SIZE).map { it.id },
                    updatedAtMs = now
                )
            )
        }
    }

    init {
        refreshRecommendations(force = false)
    }

    private companion object {
        const val MIX_SIZE = 20
        const val RECENT_WINDOW_MS = 30L * 24 * 60 * 60 * 1000
        const val AUTO_REFRESH_MS = 24L * 60 * 60 * 1000
    }
}

sealed interface AiPlaylistUiState {
    data object Idle : AiPlaylistUiState
    data object Generating : AiPlaylistUiState
    data class Created(val playlistId: String, val songCount: Int) : AiPlaylistUiState
    data object Disabled : AiPlaylistUiState
    data object ApiKeyRequired : AiPlaylistUiState
    data object EmptyLibrary : AiPlaylistUiState
    data object NoSelection : AiPlaylistUiState
    data object Failed : AiPlaylistUiState
}
