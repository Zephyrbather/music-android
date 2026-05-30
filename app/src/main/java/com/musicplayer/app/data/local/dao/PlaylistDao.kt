package com.musicplayer.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.musicplayer.app.data.local.entity.PlaylistEntity
import com.musicplayer.app.data.local.entity.PlaylistSongEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlists ORDER BY sortOrder ASC, createdAtEpochMs ASC")
    fun observePlaylists(): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlist_songs WHERE playlistId = :playlistId ORDER BY position ASC")
    fun observePlaylistEntries(playlistId: String): Flow<List<PlaylistSongEntity>>

    @Query("SELECT songId FROM playlist_songs WHERE playlistId = :playlistId")
    suspend fun getPlaylistSongIds(playlistId: String): List<String>

    @Query("SELECT * FROM playlists WHERE id = :playlistId")
    suspend fun getPlaylist(playlistId: String): PlaylistEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPlaylist(playlist: PlaylistEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPlaylistSong(entry: PlaylistSongEntity)

    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId AND songId = :songId")
    suspend fun removePlaylistSong(playlistId: String, songId: String)

    @Query("SELECT DISTINCT playlistId FROM playlist_songs WHERE songId = :songId")
    suspend fun getPlaylistIdsForSong(songId: String): List<String>

    @Query("DELETE FROM playlist_songs WHERE songId = :songId")
    suspend fun removeSongFromAllPlaylists(songId: String)

    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId")
    suspend fun removeAllPlaylistSongs(playlistId: String)

    @Query("DELETE FROM playlists WHERE id = :playlistId")
    suspend fun deletePlaylist(playlistId: String)

    @Query("UPDATE playlists SET name = :name, updatedAtEpochMs = :updatedAt WHERE id = :playlistId")
    suspend fun renamePlaylist(playlistId: String, name: String, updatedAt: Long)

    @Query("UPDATE playlists SET sortOrder = :sortOrder WHERE id = :playlistId")
    suspend fun updateSortOrder(playlistId: String, sortOrder: Int)

    @Query("UPDATE playlist_songs SET position = :position WHERE playlistId = :playlistId AND songId = :songId")
    suspend fun updateSongPosition(playlistId: String, songId: String, position: Int)

    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM playlist_songs WHERE playlistId = :playlistId")
    suspend fun nextPosition(playlistId: String): Int

    @Query("SELECT COALESCE(MAX(sortOrder), -1) + 1 FROM playlists")
    suspend fun nextPlaylistSortOrder(): Int

    @Query("UPDATE playlists SET songCount = (SELECT COUNT(*) FROM playlist_songs WHERE playlistId = :playlistId), updatedAtEpochMs = :updatedAtEpochMs WHERE id = :playlistId")
    suspend fun refreshPlaylistCount(playlistId: String, updatedAtEpochMs: Long)

    @Query("SELECT * FROM playlists")
    suspend fun getAllPlaylists(): List<PlaylistEntity>

    @Query("SELECT * FROM playlist_songs")
    suspend fun getAllPlaylistSongs(): List<PlaylistSongEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAllPlaylists(playlists: List<PlaylistEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAllPlaylistSongs(entries: List<PlaylistSongEntity>)

    @Query("SELECT COUNT(*) FROM playlist_songs WHERE playlistId = :playlistId AND songId = :songId")
    suspend fun isSongInPlaylist(playlistId: String, songId: String): Int
}
