package com.musicplayer.app.domain.repository

import com.musicplayer.app.domain.model.Playlist
import com.musicplayer.app.domain.model.Song
import kotlinx.coroutines.flow.Flow

interface PlaylistRepository {
    fun observePlaylists(): Flow<List<Playlist>>
    fun observePlaylistSongs(playlistId: String): Flow<List<Song>>
    suspend fun createPlaylist(name: String, description: String? = null): Playlist
    suspend fun addSong(playlistId: String, songId: String)
    suspend fun addSongsFromPlaylist(targetId: String, sourceId: String): Int
    suspend fun removeSong(playlistId: String, songId: String)
    suspend fun removeSongFromAllPlaylists(songId: String)
    suspend fun getOrCreateLikedPlaylist(): Playlist
    suspend fun deletePlaylist(playlistId: String)
    suspend fun renamePlaylist(playlistId: String, name: String)
    suspend fun reorderPlaylists(ids: List<String>)
    suspend fun moveSong(playlistId: String, songId: String, toPosition: Int)
}
