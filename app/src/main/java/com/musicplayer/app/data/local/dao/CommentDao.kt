package com.musicplayer.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.musicplayer.app.data.local.entity.CommentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CommentDao {
    @Query("SELECT * FROM comments WHERE songId = :songId ORDER BY timestampEpochMs DESC")
    fun observeBySongId(songId: String): Flow<List<CommentEntity>>

    @Query("SELECT AVG(CAST(rating AS REAL)) FROM comments WHERE songId = :songId")
    suspend fun averageRating(songId: String): Double?

    @Query("SELECT songId, AVG(CAST(rating AS REAL)) as avgRating, COUNT(*) as count FROM comments GROUP BY songId")
    suspend fun averageRatingsAll(): List<SongRatingSummary>

    data class SongRatingSummary(
        val songId: String,
        val avgRating: Double,
        val count: Int
    )

    @Query("SELECT COUNT(*) FROM comments WHERE songId = :songId")
    suspend fun countBySongId(songId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(comment: CommentEntity)

    @Query("DELETE FROM comments WHERE id = :commentId")
    suspend fun delete(commentId: String)

    @Query("SELECT * FROM comments")
    suspend fun getAll(): List<CommentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(comments: List<CommentEntity>)
}
