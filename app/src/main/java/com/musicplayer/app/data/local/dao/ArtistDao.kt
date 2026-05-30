package com.musicplayer.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.musicplayer.app.data.local.entity.ArtistEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ArtistDao {
    @Query("SELECT * FROM artists ORDER BY name ASC")
    fun observeArtists(): Flow<List<ArtistEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(artists: List<ArtistEntity>)

    @Query("SELECT * FROM artists")
    suspend fun getAll(): List<ArtistEntity>
}
