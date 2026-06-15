package com.musicplayer.app.domain.repository

import com.musicplayer.app.domain.model.Album
import com.musicplayer.app.domain.model.Artist
import com.musicplayer.app.domain.model.MetadataProvider
import com.musicplayer.app.domain.model.ScrapeSearchMode
import com.musicplayer.app.domain.model.Song
import kotlinx.coroutines.flow.Flow

interface MusicRepository {
    fun observeSongs(): Flow<List<Song>>
    fun observeAlbums(): Flow<List<Album>>
    fun observeArtists(): Flow<List<Artist>>
    fun observeSong(songId: String): Flow<Song?>
    suspend fun upsertSongs(songs: List<Song>, rebuildLibraryArtwork: Boolean = true)
    suspend fun saveScrapedMetadata(
        song: Song,
        mode: ScrapeSearchMode = ScrapeSearchMode.ALL,
        renameLocalFile: Boolean = false,
        rebuildLibraryArtwork: Boolean = true
    ): Song
    suspend fun rebuildLibraryArtwork(fetchArtistArtwork: Boolean = false)
    suspend fun setFavorite(songId: String, favorite: Boolean)
    suspend fun removeFromLibrary(songId: String)
    suspend fun deleteLocalFile(songId: String): Boolean
    suspend fun scanLocalLibrary(onBatch: suspend (List<Song>) -> Unit = {}): List<Song>
    suspend fun refreshMetadata(
        songId: String,
        provider: MetadataProvider = MetadataProvider.AUTO,
        rebuildLibraryArtwork: Boolean = true
    ): Song?
    suspend fun refreshArtistArtwork(artistName: String): String?
    suspend fun updateSongMetadata(
        songId: String,
        title: String,
        artistName: String,
        albumTitle: String,
        artworkUri: String?,
        artworkProvider: String?
    )
    suspend fun updateSongCategory(songId: String, categoryTag: String?): Song?
    suspend fun updateSongSemanticTags(songId: String, tags: List<String>): Song?
}
