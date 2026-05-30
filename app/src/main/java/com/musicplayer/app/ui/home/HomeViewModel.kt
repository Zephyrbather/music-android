package com.musicplayer.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musicplayer.app.data.local.dao.CommentDao
import com.musicplayer.app.data.local.dao.PlayRecordDao
import com.musicplayer.app.domain.model.Album
import com.musicplayer.app.domain.model.Playlist
import com.musicplayer.app.domain.model.Song
import com.musicplayer.app.domain.model.WebDavServer
import com.musicplayer.app.domain.model.hasUnresolvedAlbumTitle
import com.musicplayer.app.domain.repository.MusicRepository
import com.musicplayer.app.domain.repository.PlaylistRepository
import com.musicplayer.app.domain.repository.SettingsRepository
import com.musicplayer.app.domain.repository.WebDavRepository
import com.musicplayer.app.domain.usecase.FetchMetadataUseCase
import com.musicplayer.app.domain.usecase.LibraryWorkCoordinator
import com.musicplayer.app.domain.usecase.LibraryWorkState
import com.musicplayer.app.domain.usecase.ManagePlaylistUseCase
import com.musicplayer.app.domain.usecase.PlayMusicUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.random.Random

data class HomeUiState(
    val songs: List<Song> = emptyList(),
    val albums: List<Album> = emptyList(),
    val servers: List<WebDavServer> = emptyList(),
    val isScanning: Boolean = false,
    val recommendedSongs: List<Song> = emptyList()
) {
    val recentSongs: List<Song> = songs.sortedByDescending { it.dateAddedEpochMs }.take(6)
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val musicRepository: MusicRepository,
    webDavRepository: WebDavRepository,
    private val settingsRepository: SettingsRepository,
    private val fetchMetadataUseCase: FetchMetadataUseCase,
    private val libraryWorkCoordinator: LibraryWorkCoordinator,
    private val playMusicUseCase: PlayMusicUseCase,
    private val playlistRepository: PlaylistRepository,
    private val managePlaylistUseCase: ManagePlaylistUseCase,
    private val playRecordDao: PlayRecordDao,
    private val commentDao: CommentDao
) : ViewModel() {
    val uiState: StateFlow<HomeUiState> = combine(
        musicRepository.observeSongs(),
        musicRepository.observeAlbums(),
        webDavRepository.observeServers()
    ) { songs, albums, servers ->
        HomeUiState(
            songs = songs,
            albums = albums,
            servers = servers,
            recommendedSongs = buildRecommendedSongs(songs)
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    val playlists: StateFlow<List<Playlist>> = playlistRepository.observePlaylists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val workState: StateFlow<LibraryWorkState> = libraryWorkCoordinator.observeState()

    private val _featuredSong = MutableStateFlow<Song?>(null)
    val featuredSong: StateFlow<Song?> = _featuredSong

    init {
        viewModelScope.launch {
            if (settingsRepository.autoScanLocalLibraryEnabled.first()) {
                libraryWorkCoordinator.startScan()
            }
            if (settingsRepository.autoScrapeEnabled.first()) {
                val songs = musicRepository.observeSongs().first()
                val needsScrape = songs.any {
                    it.lyrics.isNullOrBlank() ||
                        it.artworkUri.isNullOrBlank() ||
                        it.releaseYear == null ||
                        it.hasUnresolvedAlbumTitle()
                }
                if (needsScrape) {
                    libraryWorkCoordinator.startScrapeMissing()
                }
            }
            refreshFeatured()
        }
    }

    fun refreshFeatured() {
        viewModelScope.launch {
            val songs = musicRepository.observeSongs().first()
            if (songs.isEmpty()) {
                _featuredSong.value = null
                return@launch
            }
            val now = System.currentTimeMillis()
            val sevenDaysMs = 7 * 24 * 60 * 60 * 1000L
            val thirtyDaysMs = 30 * 24 * 60 * 60 * 1000L

            val lastPlayedMap = playRecordDao.lastPlayedPerSong().associate { it.songId to it.lastPlayedMs }
            val mostRecentTimestamp = playRecordDao.mostRecentTimestamp()
            val ratingSummaries = commentDao.averageRatingsAll()
            val ratingMap = ratingSummaries.associate { it.songId to it.avgRating }
            val songsWithRatings = ratingSummaries.size
            val sparseRatings = songsWithRatings < 5

            val recentArtist = if (mostRecentTimestamp != null) {
                val recentSongId = lastPlayedMap.maxByOrNull { it.value }?.key
                songs.find { it.id == recentSongId }?.artistName
            } else null

            val weighted = songs.flatMap { song ->
                val lastPlayed = lastPlayedMap[song.id]
                val daysSincePlayed = if (lastPlayed != null) (now - lastPlayed) / (24 * 60 * 60 * 1000) else null
                val isNew = now - song.dateAddedEpochMs <= thirtyDaysMs
                val avgRating = ratingMap[song.id]

                val weight = when {
                    lastPlayed == null -> 10
                    daysSincePlayed != null && daysSincePlayed >= 7 -> 7
                    daysSincePlayed != null && daysSincePlayed >= 30 -> 5
                    song.artistName == recentArtist && recentArtist != null -> 6
                    isNew -> 4
                    avgRating != null && avgRating >= 3.5 -> 3
                    avgRating != null -> 1
                    else -> 3
                }
                val adjustedWeight = if (sparseRatings) {
                    (weight * 0.4 + 3 * 0.6).toInt().coerceAtLeast(1)
                } else weight
                List(adjustedWeight) { song }
            }
            _featuredSong.value = weighted[Random.nextInt(weighted.size)]
        }
    }

    fun scan() {
        libraryWorkCoordinator.startScan()
    }

    fun toggleFavorite(song: Song) {
        viewModelScope.launch {
            musicRepository.setFavorite(song.id, !song.isFavorite)
        }
    }

    fun removeFromLibrary(song: Song) {
        viewModelScope.launch {
            musicRepository.removeFromLibrary(song.id)
        }
    }

    fun deleteLocalFile(song: Song) {
        viewModelScope.launch {
            musicRepository.deleteLocalFile(song.id)
        }
    }

    fun scrape(song: Song) {
        viewModelScope.launch {
            fetchMetadataUseCase(song.id)
        }
    }

    fun selectSong(songId: String) {
        _selectedSongId.value = songId
    }

    fun showSongActions(song: Song) {
        _actionSong.value = song
    }

    fun clearActionSong() {
        _actionSong.value = null
    }

    fun playNext(song: Song) {
        playMusicUseCase.playNext(song)
    }

    fun addToPlaylist(playlistId: String, song: Song) {
        viewModelScope.launch {
            managePlaylistUseCase.addSong(playlistId, song.id)
        }
    }

    fun createPlaylistWith(song: Song) {
        viewModelScope.launch {
            val playlist = managePlaylistUseCase.create("New Mix")
            managePlaylistUseCase.addSong(playlist.id, song.id)
        }
    }

    fun updateSongMetadata(
        songId: String,
        title: String,
        artistName: String,
        albumTitle: String,
        artworkUri: String?,
        artworkProvider: String?
    ) {
        viewModelScope.launch {
            musicRepository.updateSongMetadata(songId, title, artistName, albumTitle, artworkUri, artworkProvider)
        }
    }

    fun showEditDialog(song: Song) {
        _editSong.value = song
        _actionSong.value = null
    }

    fun dismissEditDialog() {
        _editSong.value = null
    }

    private val _selectedSongId = MutableStateFlow<String?>(null)
    val selectedSongId: StateFlow<String?> = _selectedSongId

    private val _actionSong = MutableStateFlow<Song?>(null)
    val actionSong: StateFlow<Song?> = _actionSong

    private val _editSong = MutableStateFlow<Song?>(null)
    val editSong: StateFlow<Song?> = _editSong
}

private fun buildRecommendedSongs(songs: List<Song>): List<Song> {
    return songs
        .sortedWith(
            compareByDescending<Song> { it.isFavorite }
                .thenByDescending { !it.artworkUri.isNullOrBlank() }
                .thenByDescending { it.dateAddedEpochMs }
        )
        .take(RECOMMENDATION_POOL_SIZE)
        .shuffled()
        .take(RECOMMENDATION_DISPLAY_COUNT)
}

private const val RECOMMENDATION_POOL_SIZE = 50
private const val RECOMMENDATION_DISPLAY_COUNT = 10
