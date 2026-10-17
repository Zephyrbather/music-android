package com.musicplayer.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.musicplayer.app.data.local.entity.PlayRecordEntity

@Dao
interface PlayRecordDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: PlayRecordEntity)

    @Query("""
        SELECT songId, SUM(durationMs) as totalMs FROM play_records
        WHERE listenedAtEpochMs >= :sinceMs
        GROUP BY songId ORDER BY totalMs DESC LIMIT :limit
    """)
    suspend fun topSongsSince(sinceMs: Long, limit: Int = 500): List<SongPlayCount>

    @Query("SELECT MAX(listenedAtEpochMs) FROM play_records")
    suspend fun mostRecentTimestamp(): Long?

    @Query("""
        SELECT songId, MAX(listenedAtEpochMs) as lastPlayedMs FROM play_records
        GROUP BY songId
    """)
    suspend fun lastPlayedPerSong(): List<SongLastPlayed>

    data class SongLastPlayed(
        val songId: String,
        val lastPlayedMs: Long
    )

    @Query("SELECT COUNT(*) FROM play_records")
    suspend fun count(): Int

    @Query("""
        SELECT * FROM play_records
        WHERE listenedAtEpochMs >= :startMs AND listenedAtEpochMs < :endMs
        ORDER BY listenedAtEpochMs ASC
    """)
    suspend fun recordsBetween(startMs: Long, endMs: Long): List<PlayRecordEntity>

    @Query("SELECT * FROM play_records")
    suspend fun getAll(): List<PlayRecordEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(records: List<PlayRecordEntity>)

    data class SongPlayCount(
        val songId: String,
        val totalMs: Long
    )
}
