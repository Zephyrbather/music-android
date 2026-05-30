package com.musicplayer.app.data.repository

import android.content.Context
import android.net.Uri
import com.musicplayer.app.data.local.dao.AlbumDao
import com.musicplayer.app.data.local.dao.ArtistDao
import com.musicplayer.app.data.local.dao.SongDao
import com.musicplayer.app.data.local.entity.toDomain
import com.musicplayer.app.data.local.entity.toEntity
import com.musicplayer.app.data.scanner.LocalFileScanner
import com.musicplayer.app.data.scraper.MetadataScraper
import com.musicplayer.app.data.tag.AudioTagWriter
import com.musicplayer.app.domain.model.Album
import com.musicplayer.app.domain.model.Artist
import com.musicplayer.app.domain.model.MetadataProvider
import com.musicplayer.app.domain.model.ScrapeSearchMode
import com.musicplayer.app.domain.model.Song
import com.musicplayer.app.domain.model.albumGroupingId
import com.musicplayer.app.domain.model.artistGroupingId
import com.musicplayer.app.domain.model.hasUnresolvedAlbumTitle
import com.musicplayer.app.domain.repository.MusicRepository
import com.musicplayer.app.domain.repository.PlaylistRepository
import com.musicplayer.app.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MusicRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val songDao: SongDao,
    private val albumDao: AlbumDao,
    private val artistDao: ArtistDao,
    private val localFileScanner: LocalFileScanner,
    private val metadataScraper: MetadataScraper,
    private val audioTagWriter: AudioTagWriter,
    private val settingsRepository: SettingsRepository,
    private val playlistRepository: PlaylistRepository
) : MusicRepository {
    override fun observeSongs(): Flow<List<Song>> {
        return songDao.observeSongs().map { entities -> entities.map { it.toDomain() } }
    }

    override fun observeAlbums(): Flow<List<Album>> {
        return songDao.observeSongs().map { entities -> entities.map { it.toDomain() }.toAlbums() }
    }

    override fun observeArtists(): Flow<List<Artist>> {
        return artistDao.observeArtists().map { entities -> entities.map { it.toDomain() } }
    }

    override fun observeSong(songId: String): Flow<Song?> {
        return songDao.observeSong(songId).map { it?.toDomain() }
    }

    override suspend fun upsertSongs(songs: List<Song>) {
        if (songs.isEmpty()) return
        songDao.upsertAll(songs.map { it.toEntity() })
        rebuildLibraryArtwork(fetchArtistArtwork = false)
    }

    override suspend fun setFavorite(songId: String, favorite: Boolean) {
        songDao.setFavorite(songId, favorite)
        val liked = playlistRepository.getOrCreateLikedPlaylist()
        if (favorite) {
            playlistRepository.addSong(liked.id, songId)
        } else {
            playlistRepository.removeSong(liked.id, songId)
        }
    }

    override suspend fun removeFromLibrary(songId: String) {
        songDao.deleteByIds(listOf(songId))
        playlistRepository.removeSongFromAllPlaylists(songId)
        rebuildLibraryArtwork(fetchArtistArtwork = false)
    }

    override suspend fun deleteLocalFile(songId: String): Boolean = withContext(Dispatchers.IO) {
        val song = songDao.getSong(songId)?.toDomain() ?: return@withContext false
        if (song.source != com.musicplayer.app.domain.model.Source.LOCAL) return@withContext false
        val uri = Uri.parse(song.uri)
        val deleted = runCatching {
            when (uri.scheme) {
                "file" -> uri.path?.let(::File)?.delete() == true
                "content" -> context.contentResolver.delete(uri, null, null) > 0
                else -> false
            }
        }.getOrDefault(false)
        if (deleted) removeFromLibrary(songId)
        deleted
    }

    override suspend fun saveScrapedMetadata(
        song: Song,
        mode: ScrapeSearchMode,
        renameLocalFile: Boolean
    ): Song {
        val savedSong = audioTagWriter.writeIfSupported(
            song = song,
            mode = mode,
            renameLocalFile = renameLocalFile
        )
        songDao.upsertAll(listOf(savedSong.toEntity()))
        rebuildLibraryArtwork(fetchArtistArtwork = false)
        return savedSong
    }

    override suspend fun scanLocalLibrary(onBatch: suspend (List<Song>) -> Unit): List<Song> {
        val songs = localFileScanner.scan(
            folderUris = settingsRepository.localScanFolderUris.first(),
            onBatch = onBatch
        )
        val scannedUris = songs.map { it.uri }.toSet()
        val scannedKeys = songs.map { songKey(it) }.toSet()
        val allExisting = songDao.getSongs()
        val existingLocal = allExisting.filter { it.source == com.musicplayer.app.domain.model.Source.LOCAL }
        // Delete LOCAL duplicates that match scanned songs but have a different URI (stale SAF copies)
        val staleLocalIds = existingLocal
            .filter { songKey(it) in scannedKeys && it.uri !in scannedUris }
            .map { it.id }
        if (staleLocalIds.isNotEmpty()) songDao.deleteByIds(staleLocalIds)
        // Delete WEBDAV entries that have been downloaded locally (same metadata as a LOCAL song)
        val staleWebDavIds = allExisting
            .filter { it.source == com.musicplayer.app.domain.model.Source.WEBDAV && songKey(it) in scannedKeys }
            .map { it.id }
        if (staleWebDavIds.isNotEmpty()) songDao.deleteByIds(staleWebDavIds)
        // Preserve existing LOCAL entries whose file paths match scanned metadata (e.g. downloaded songs)
        val matchedUris = existingLocal
            .filter { songKey(it) in scannedKeys }
            .map { it.uri }
        // Also preserve file:// entries that still exist on disk
        val fileUris = existingLocal
            .filter { it.uri.startsWith("file://") && java.io.File(java.net.URI(it.uri)).exists() }
            .map { it.uri }
        val currentUris = (scannedUris + matchedUris + fileUris).distinct()
        if (currentUris.isNotEmpty()) {
            val deleted = songDao.removeLocalSongsNotIn(currentUris)
            if (deleted > 0) rebuildLibraryArtwork(fetchArtistArtwork = false)
        }
        return songs
    }

    override suspend fun refreshMetadata(songId: String, provider: MetadataProvider): Song? {
        val current = songDao.getSong(songId)?.toDomain() ?: return null
        val mode = current.missingScrapeMode() ?: return current
        val enriched = metadataScraper.enrich(current, provider)
        return saveScrapedMetadata(enriched, mode = mode, renameLocalFile = false)
    }

    override suspend fun refreshArtistArtwork(artistName: String): String? {
        val artwork = metadataScraper.findArtistArtwork(artistName) ?: return null
        val allSongs = songDao.getSongs().map { it.toDomain() }
        val artistSongs = allSongs.filter { it.artistName == artistName }
        if (artistSongs.isNotEmpty()) {
            val first = artistSongs.first()
            artistDao.upsertAll(
                listOf(
                    Artist(
                        id = first.artistGroupingId(),
                        name = artistName,
                        albumCount = artistSongs.map { it.albumGroupingId() }.distinct().size,
                        songCount = artistSongs.size,
                        artworkUri = artwork.imageUri
                    ).toEntity()
                )
            )
        }
        return artwork.imageUri
    }

    override suspend fun updateSongMetadata(
        songId: String,
        title: String,
        artistName: String,
        albumTitle: String,
        artworkUri: String?,
        artworkProvider: String?
    ) {
        songDao.updateMetadata(songId, title, artistName, albumTitle, artworkUri, artworkProvider)
        songDao.getSong(songId)?.toDomain()?.let {
            audioTagWriter.writeIfSupported(it, mode = ScrapeSearchMode.ALL, renameLocalFile = false)
        }
        rebuildLibraryArtwork(fetchArtistArtwork = false)
    }

    override suspend fun rebuildLibraryArtwork(fetchArtistArtwork: Boolean) {
        val allSongs = songDao.getSongs().map { it.toDomain() }
        albumDao.clearAll()
        val albums = allSongs.toAlbums()
        if (albums.isNotEmpty()) {
            albumDao.upsertAll(albums.map { it.toEntity() })
        }
        artistDao.upsertAll(allSongs.toArtists(metadataScraper, fetchArtistArtwork).map { it.toEntity() })
    }
}

private fun songKey(song: com.musicplayer.app.data.local.entity.SongEntity) =
    "${song.artistName}|${song.albumTitle}|${song.title}|${song.durationMs}"

private fun songKey(song: Song) =
    "${song.artistName}|${song.albumTitle}|${song.title}|${song.durationMs}"

private fun Song.missingScrapeMode(): ScrapeSearchMode? {
    val missingLyrics = lyrics.isNullOrBlank()
    val missingArtwork = artworkUri.isNullOrBlank()
    val missingReleaseDate = releaseYear == null
    val missingAlbum = hasUnresolvedAlbumTitle()
    return when {
        missingLyrics && missingArtwork -> ScrapeSearchMode.ALL
        missingAlbum -> ScrapeSearchMode.ALL
        missingReleaseDate -> ScrapeSearchMode.ALL
        missingLyrics -> ScrapeSearchMode.LYRICS
        missingArtwork -> ScrapeSearchMode.ARTWORK
        else -> null
    }
}

private fun List<Song>.toAlbums(): List<Album> {
    return groupBy { it.albumGroupingId() }
        .map { (id, albumSongs) ->
            val first = albumSongs.first()
            val releaseSong = albumSongs
                .filter { (it.releaseYear ?: 0) > 0 }
                .minWithOrNull(compareBy<Song> { it.releaseYear ?: Int.MAX_VALUE }
                    .thenBy { it.releaseMonth ?: Int.MAX_VALUE })
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
        }
}

private suspend fun List<Song>.toArtists(
    metadataScraper: MetadataScraper,
    fetchArtistArtwork: Boolean
): List<Artist> {
    return groupBy { it.artistGroupingId() }
        .map { (id, artistSongs) ->
            val first = artistSongs.first()
            Artist(
                id = id,
                name = first.artistName,
                albumCount = artistSongs.map { it.albumGroupingId() }.distinct().size,
                songCount = artistSongs.size,
                artworkUri = if (fetchArtistArtwork) {
                    metadataScraper.findArtistArtwork(first.artistName)?.imageUri
                } else {
                    null
                } ?: artistSongs.firstNotNullOfOrNull { it.artworkUri }
            )
        }
}
