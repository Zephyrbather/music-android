package com.musicplayer.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.musicplayer.app.data.local.entity.ScrapeCacheEntity

@Dao
interface ScrapeCacheDao {
    @Query("SELECT * FROM scrape_cache WHERE cacheKey = :cacheKey")
    suspend fun get(cacheKey: String): ScrapeCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(cache: ScrapeCacheEntity)

    @Query("DELETE FROM scrape_cache WHERE updatedAtEpochMs < :olderThanEpochMs")
    suspend fun prune(olderThanEpochMs: Long)

    @Query("SELECT COUNT(*) FROM scrape_cache")
    suspend fun count(): Int

    @Query("DELETE FROM scrape_cache")
    suspend fun clearAll()
}
