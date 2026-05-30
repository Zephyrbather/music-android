package com.musicplayer.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.musicplayer.app.data.local.entity.AlbumEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AlbumDao {
    @Query("SELECT * FROM albums ORDER BY title ASC")
    fun observeAlbums(): Flow<List<AlbumEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(albums: List<AlbumEntity>)

    @Query("DELETE FROM albums")
    suspend fun clearAll()

    @Query("SELECT * FROM albums")
    suspend fun getAll(): List<AlbumEntity>
}
