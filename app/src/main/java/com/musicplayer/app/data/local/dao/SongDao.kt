package com.musicplayer.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.musicplayer.app.data.local.entity.SongEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SongDao {
    @Query("SELECT * FROM songs ORDER BY albumTitle ASC, discNumber ASC, trackNumber ASC, title ASC")
    fun observeSongs(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE id = :songId")
    fun observeSong(songId: String): Flow<SongEntity?>

    @Query("SELECT * FROM songs WHERE id = :songId")
    suspend fun getSong(songId: String): SongEntity?

    @Query("SELECT * FROM songs")
    suspend fun getSongs(): List<SongEntity>

    @Query("SELECT * FROM songs WHERE id IN (:ids)")
    fun observeSongsByIds(ids: List<String>): Flow<List<SongEntity>>

    @Query("SELECT COUNT(*) FROM songs")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(songs: List<SongEntity>)

    @Query("UPDATE songs SET isFavorite = :favorite WHERE id = :songId")
    suspend fun setFavorite(songId: String, favorite: Boolean)

    @Query(
        "UPDATE songs SET title = :title, artistName = :artistName, albumTitle = :albumTitle, " +
            "artworkUri = :artworkUri, artworkProvider = :artworkProvider WHERE id = :songId"
    )
    suspend fun updateMetadata(
        songId: String,
        title: String,
        artistName: String,
        albumTitle: String,
        artworkUri: String?,
        artworkProvider: String?
    )

    @Query("UPDATE songs SET categoryTag = :categoryTag WHERE id = :songId")
    suspend fun updateCategory(songId: String, categoryTag: String?)

    @Query("DELETE FROM songs WHERE source = 'LOCAL' AND uri NOT IN (:uris)")
    suspend fun removeLocalSongsNotIn(uris: List<String>): Int

    @Query("SELECT COUNT(*) FROM songs WHERE source = 'LOCAL' AND uri LIKE '%/' || :fileName")
    suspend fun countLocalByFileName(fileName: String): Int

    @Query("UPDATE songs SET uri = :newUri, source = 'LOCAL' WHERE id = :songId")
    suspend fun updateSongUriAndSource(songId: String, newUri: String): Int

    @Query("SELECT uri FROM songs WHERE source = 'LOCAL'")
    suspend fun getLocalUris(): List<String>

    @Query("DELETE FROM songs WHERE source = 'WEBDAV' AND uri LIKE '%/' || :fileName")
    suspend fun deleteWebDavByFileName(fileName: String): Int

    @Query("DELETE FROM songs WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<String>)
}
