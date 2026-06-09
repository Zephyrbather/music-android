package com.musicplayer.app.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musicplayer.app.domain.model.Album
import com.musicplayer.app.domain.model.Artist
import com.musicplayer.app.domain.model.Playlist
import com.musicplayer.app.domain.model.Song
import com.musicplayer.app.domain.model.Source
import com.musicplayer.app.domain.model.albumGroupingId
import com.musicplayer.app.domain.model.artistGroupingId
import com.musicplayer.app.domain.repository.MusicRepository
import com.musicplayer.app.domain.repository.PlaylistRepository
import com.musicplayer.app.domain.repository.SettingsRepository
import com.musicplayer.app.domain.repository.WebDavRepository
import com.musicplayer.app.domain.usecase.FetchMetadataUseCase
import com.musicplayer.app.domain.usecase.ManagePlaylistUseCase
import com.musicplayer.app.domain.usecase.PlayMusicUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class LibraryTab {
    SONGS,
    ALBUMS,
    ARTISTS
}

enum class AlbumSortMode {
    RELEASE_DATE,
    ALBUM_TITLE,
    ARTIST_NAME
}

enum class AlbumSortDirection {
    ASCENDING,
    DESCENDING
}

data class AlbumSortState(
    val mode: AlbumSortMode = AlbumSortMode.ALBUM_TITLE,
    val direction: AlbumSortDirection = AlbumSortDirection.ASCENDING
)

data class LibraryUiState(
    val songs: List<Song> = emptyList(),
    val albums: List<Album> = emptyList(),
    val artists: List<Artist> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
    val selectedTab: LibraryTab = LibraryTab.SONGS,
    val albumSortMode: AlbumSortMode = AlbumSortMode.ALBUM_TITLE,
    val albumSortDirection: AlbumSortDirection = AlbumSortDirection.ASCENDING,
    val showOnlineLibrary: Boolean = false,
    val songIndexLetters: List<String> = emptyList(),
    val firstSongIndexByLetter: Map<String, Int> = emptyMap(),
    val artistIndexLetters: List<String> = emptyList(),
    val firstArtistIndexByLetter: Map<String, Int> = emptyMap()
)

data class DownloadProgress(
    val total: Int = 0,
    val completed: Int = 0,
    val downloadingIds: Set<String> = emptySet()
)

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val musicRepository: MusicRepository,
    private val playlistRepository: PlaylistRepository,
    private val managePlaylistUseCase: ManagePlaylistUseCase,
    private val playMusicUseCase: PlayMusicUseCase,
    private val fetchMetadataUseCase: FetchMetadataUseCase,
    private val webDavRepository: WebDavRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    private val selectedTab = MutableStateFlow(LibraryTab.SONGS)
    private val albumSortState = MutableStateFlow(AlbumSortState())
    private val showOnlineLibrary = MutableStateFlow(false)
    private val _downloadProgress = MutableStateFlow(DownloadProgress())
    val downloadProgress: StateFlow<DownloadProgress> = _downloadProgress.asStateFlow()

    private val libraryContent: StateFlow<LibraryContent> = combine(
        musicRepository.observeSongs(),
        musicRepository.observeArtists(),
        showOnlineLibrary,
        albumSortState
    ) { songs, storedArtists, online, sortState ->
        buildLibraryContent(
            songs = songs,
            storedArtists = storedArtists,
            online = online,
            sortState = sortState
        )
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryContent())

    val uiState: StateFlow<LibraryUiState> = combine(
        libraryContent,
        playlistRepository.observePlaylists(),
        selectedTab
    ) { content, playlists, tab ->
        LibraryUiState(
            songs = content.songs,
            albums = content.albums,
            artists = content.artists,
            playlists = playlists,
            selectedTab = tab,
            albumSortMode = content.albumSortMode,
            albumSortDirection = content.albumSortDirection,
            showOnlineLibrary = content.showOnlineLibrary,
            songIndexLetters = content.songIndexLetters,
            firstSongIndexByLetter = content.firstSongIndexByLetter,
            artistIndexLetters = content.artistIndexLetters,
            firstArtistIndexByLetter = content.firstArtistIndexByLetter
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryUiState())

    fun selectTab(tab: LibraryTab) {
        selectedTab.value = tab
    }

    fun setAlbumSortMode(mode: AlbumSortMode) {
        val current = albumSortState.value
        albumSortState.value = if (current.mode == mode) {
            current.copy(direction = current.direction.toggle())
        } else {
            AlbumSortState(mode = mode, direction = mode.defaultDirection())
        }
    }

    fun toggleLibraryMode() {
        showOnlineLibrary.value = !showOnlineLibrary.value
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

    fun downloadSong(song: Song, outputDir: File) {
        val progress = _downloadProgress.value
        if (song.id in progress.downloadingIds) return
        viewModelScope.launch {
            val allSongs = musicRepository.observeSongs().first()
            val fileName = song.uri.substringAfterLast('/')
            val dup = allSongs.firstOrNull { it.source == Source.LOCAL && it.uri.substringAfterLast('/') == fileName }
            if (dup != null) {
                _downloadProgress.value = DownloadProgress(total = 1, completed = 1, downloadingIds = emptySet())
                return@launch
            }
            _downloadProgress.value = DownloadProgress(total = 1, completed = 0, downloadingIds = setOf(song.id))
            val downloaded = withContext(Dispatchers.IO) {
                webDavRepository.downloadSong(song, outputDir)
            }
            if (downloaded) {
                musicRepository.rebuildLibraryArtwork(fetchArtistArtwork = false)
            }
            _downloadProgress.value = DownloadProgress(total = 1, completed = 1, downloadingIds = emptySet())
        }
    }

    fun downloadAllSongs(outputDir: File) {
        viewModelScope.launch {
            val allSongs = musicRepository.observeSongs().first()
            val localFileNames = allSongs.filter { it.source == Source.LOCAL }
                .map { it.uri.substringAfterLast('/') }.toSet()
            val toDownload = allSongs.filter { it.source == Source.WEBDAV }
                .filter { it.uri.substringAfterLast('/') !in localFileNames }
            if (toDownload.isEmpty()) return@launch
            var completed = 0
            val allIds = toDownload.map { it.id }.toSet()
            _downloadProgress.value = DownloadProgress(total = toDownload.size, completed = 0, downloadingIds = allIds)
            for (song in toDownload) {
                val downloaded = withContext(Dispatchers.IO) {
                    webDavRepository.downloadSong(song, outputDir)
                }
                if (downloaded) {
                    musicRepository.rebuildLibraryArtwork(fetchArtistArtwork = false)
                }
                completed++
                _downloadProgress.value = DownloadProgress(
                    total = toDownload.size,
                    completed = completed,
                    downloadingIds = if (completed >= toDownload.size) emptySet() else allIds
                )
            }
        }
    }
}

private data class LibraryContent(
    val songs: List<Song> = emptyList(),
    val albums: List<Album> = emptyList(),
    val artists: List<Artist> = emptyList(),
    val albumSortMode: AlbumSortMode = AlbumSortMode.ALBUM_TITLE,
    val albumSortDirection: AlbumSortDirection = AlbumSortDirection.ASCENDING,
    val showOnlineLibrary: Boolean = false,
    val songIndexLetters: List<String> = emptyList(),
    val firstSongIndexByLetter: Map<String, Int> = emptyMap(),
    val artistIndexLetters: List<String> = emptyList(),
    val firstArtistIndexByLetter: Map<String, Int> = emptyMap()
)

private fun buildLibraryContent(
    songs: List<Song>,
    storedArtists: List<Artist>,
    online: Boolean,
    sortState: AlbumSortState
): LibraryContent {
    val filteredSongs = if (online) {
        songs.filter { it.source == Source.WEBDAV }
    } else {
        songs.filter { it.source != Source.WEBDAV }
    }
    val sortedSongs = filteredSongs.sortedWith(compareByLibrarySongTitle())
    val derivedAlbums = filteredSongs.groupBy { it.albumGroupingId() }
        .map { (id, albumSongs) ->
            val first = albumSongs.first()
            val releaseSong = albumSongs
                .filter { (it.releaseYear ?: 0) > 0 }
                .minWithOrNull(
                    compareBy<Song> { it.releaseYear ?: Int.MAX_VALUE }
                        .thenBy { it.releaseMonth ?: Int.MAX_VALUE }
                )
            Album(
                id = id,
                title = first.albumTitle,
                artistName = first.artistName,
                artworkUri = albumSongs.firstNotNullOfOrNull { it.artworkUri },
                songCount = albumSongs.size,
                releaseYear = releaseSong?.releaseYear,
                releaseMonth = releaseSong?.releaseMonth,
                source = first.source
            )
        }.sortedWith(sortState.comparator())
    val storedById = storedArtists.associateBy { it.id }
    val storedByName = storedArtists.associateBy { it.name.normalizedNameKey() }
    val derivedArtists = filteredSongs.groupBy { it.artistGroupingId() }
        .map { (id, artistSongs) ->
            val first = artistSongs.first()
            Artist(
                id = id,
                name = first.artistName,
                albumCount = artistSongs.map { it.albumGroupingId() }.distinct().size,
                songCount = artistSongs.size,
                artworkUri = storedById[id]?.artworkUri
                    ?: storedByName[first.artistName.normalizedNameKey()]?.artworkUri
                    ?: artistSongs.firstNotNullOfOrNull { it.artworkUri }
            )
        }.sortedWith(compareByLibraryArtistName())
    val songIndexLetters = sortedSongs.map { it.title.libraryIndexLetter() }
    val artistIndexLetters = derivedArtists.map { it.name.libraryIndexLetter() }
    return LibraryContent(
        songs = sortedSongs,
        albums = derivedAlbums,
        artists = derivedArtists,
        albumSortMode = sortState.mode,
        albumSortDirection = sortState.direction,
        showOnlineLibrary = online,
        songIndexLetters = songIndexLetters,
        firstSongIndexByLetter = firstIndexByLetter(songIndexLetters),
        artistIndexLetters = artistIndexLetters,
        firstArtistIndexByLetter = firstIndexByLetter(artistIndexLetters)
    )
}

private fun firstIndexByLetter(letters: List<String>): Map<String, Int> {
    val result = LinkedHashMap<String, Int>()
    letters.forEachIndexed { index, letter ->
        result.putIfAbsent(letter, index)
    }
    return result
}

private fun AlbumSortState.comparator(): Comparator<Album> {
    return when (mode) {
        AlbumSortMode.RELEASE_DATE -> compareByReleaseDate(direction)
        AlbumSortMode.ALBUM_TITLE -> compareByPinyinAlbumTitle().withDirection(direction)
        AlbumSortMode.ARTIST_NAME -> compareByPinyinArtistName().thenByPinyinTitle().withDirection(direction)
    }
}

private fun AlbumSortMode.defaultDirection(): AlbumSortDirection {
    return when (this) {
        AlbumSortMode.RELEASE_DATE -> AlbumSortDirection.DESCENDING
        AlbumSortMode.ALBUM_TITLE,
        AlbumSortMode.ARTIST_NAME -> AlbumSortDirection.ASCENDING
    }
}

private fun AlbumSortDirection.toggle(): AlbumSortDirection {
    return when (this) {
        AlbumSortDirection.ASCENDING -> AlbumSortDirection.DESCENDING
        AlbumSortDirection.DESCENDING -> AlbumSortDirection.ASCENDING
    }
}

private fun compareByReleaseDate(direction: AlbumSortDirection): Comparator<Album> = Comparator { a, b ->
    val aKey = a.releaseDateKey()
    val bKey = b.releaseDateKey()
    when {
        aKey == null && bKey == null -> compareByPinyinAlbumTitle().compare(a, b)
        aKey == null -> 1
        bKey == null -> -1
        aKey != bKey -> if (direction == AlbumSortDirection.ASCENDING) {
            aKey.compareTo(bKey)
        } else {
            bKey.compareTo(aKey)
        }
        else -> compareByPinyinAlbumTitle().compare(a, b)
    }
}

private fun compareByPinyinAlbumTitle(): Comparator<Album> = Comparator { a, b ->
    compareLibraryText(a.title, b.title)
}

private fun compareByPinyinArtistName(): Comparator<Album> = Comparator { a, b ->
    compareLibraryText(a.artistName, b.artistName)
}

private fun Comparator<Album>.thenByPinyinTitle(): Comparator<Album> = then(compareByPinyinAlbumTitle())

private fun compareByLibrarySongTitle(): Comparator<Song> = Comparator { a, b ->
    compareLibraryText(a.title, b.title)
}

private fun compareByLibraryArtistName(): Comparator<Artist> = Comparator { a, b ->
    compareLibraryText(a.name, b.name)
}

private fun compareLibraryText(a: String, b: String): Int {
    val aDigit = a.trim().firstOrNull()?.isDigit() == true
    val bDigit = b.trim().firstOrNull()?.isDigit() == true
    when {
        aDigit && !bDigit -> return -1
        !aDigit && bDigit -> return 1
    }

    val initialCmp = libraryInitialRank(a).compareTo(libraryInitialRank(b))
    if (initialCmp != 0) return initialCmp

    val pinyinCmp = PinyinHelper.comparePinyin(a, b)
    return if (pinyinCmp != 0) pinyinCmp else a.compareTo(b)
}

private fun String.libraryIndexLetter(): String {
    val first = trim().firstOrNull() ?: return "#"
    if (first in '0'..'9') return "#"
    if (first in 'A'..'Z') return first.toString()
    if (first in 'a'..'z') return first.uppercaseChar().toString()
    return PinyinHelper.toPinyinInitial(first) ?: "#"
}

private fun libraryInitialRank(value: String): Int {
    val first = value.trim().firstOrNull() ?: return 26
    val initial = PinyinHelper.toPinyinInitial(first)?.firstOrNull()
    return if (initial != null && initial in 'A'..'Z') initial - 'A' else 26
}

private fun <T> Comparator<T>.withDirection(direction: AlbumSortDirection): Comparator<T> {
    return if (direction == AlbumSortDirection.ASCENDING) this else Comparator { a, b -> compare(b, a) }
}

private fun Album.releaseDateKey(): Int? {
    val year = releaseYear?.takeIf { it > 0 } ?: return null
    val month = releaseMonth?.takeIf { it in 1..12 } ?: 0
    return year * 100 + month
}

private fun String.normalizedNameKey(): String {
    return lowercase().replace(Regex("\\s+"), " ").trim()
}
