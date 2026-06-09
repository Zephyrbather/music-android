package com.musicplayer.app.ui.playlists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musicplayer.app.data.local.dao.PlayRecordDao
import com.musicplayer.app.data.local.dao.SongDao
import com.musicplayer.app.data.local.entity.toDomain
import com.musicplayer.app.domain.model.Playlist
import com.musicplayer.app.domain.model.Song
import com.musicplayer.app.domain.repository.PlaylistRepository
import com.musicplayer.app.domain.repository.SettingsRepository
import com.musicplayer.app.domain.usecase.ManagePlaylistUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Calendar
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class PlaylistsUiState(
    val playlists: List<Playlist> = emptyList()
)

data class RankedSong(
    val song: Song,
    val totalMs: Long
)

enum class RankingPeriod { DAILY, WEEKLY, MONTHLY, YEARLY }

@HiltViewModel
class PlaylistsViewModel @Inject constructor(
    private val playlistRepository: PlaylistRepository,
    private val settingsRepository: SettingsRepository,
    private val managePlaylistUseCase: ManagePlaylistUseCase,
    private val playRecordDao: PlayRecordDao,
    private val songDao: SongDao
) : ViewModel() {
    val uiState: StateFlow<PlaylistsUiState> = playlistRepository.observePlaylists()
        .map { playlists -> PlaylistsUiState(playlists) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlaylistsUiState())

    private val _rankingPeriod = MutableStateFlow(RankingPeriod.WEEKLY)
    val rankingPeriod: StateFlow<RankingPeriod> = _rankingPeriod.asStateFlow()

    private val _rankedSongs = MutableStateFlow<List<RankedSong>>(emptyList())
    val rankedSongs: StateFlow<List<RankedSong>> = _rankedSongs.asStateFlow()

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

    fun selectRankingPeriod(period: RankingPeriod) {
        _rankingPeriod.value = period
        loadRankings(period)
    }

    fun loadRankings(period: RankingPeriod = _rankingPeriod.value) {
        viewModelScope.launch {
            val rankings = withContext(Dispatchers.IO) {
                val cal = Calendar.getInstance()
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                when (period) {
                    RankingPeriod.DAILY -> {} // today at 00:00
                    RankingPeriod.WEEKLY -> cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
                    RankingPeriod.MONTHLY -> cal.set(Calendar.DAY_OF_MONTH, 1)
                    RankingPeriod.YEARLY -> {
                        cal.set(Calendar.MONTH, Calendar.JANUARY)
                        cal.set(Calendar.DAY_OF_MONTH, 1)
                    }
                }
                val sinceMs = cal.timeInMillis
                val counts = playRecordDao.topSongsSince(sinceMs)
                val songs = songDao.getSongs()
                val songById = songs.associate { it.id to it.toDomain() }
                counts.mapNotNull { count ->
                    songById[count.songId]?.let { RankedSong(it, count.totalMs) }
                }
            }
            _rankedSongs.value = rankings
        }
    }

    init {
        loadRankings()
    }
}
