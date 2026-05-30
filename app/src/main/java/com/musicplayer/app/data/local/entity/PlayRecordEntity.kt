package com.musicplayer.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "play_records")
data class PlayRecordEntity(
    @PrimaryKey val id: String,
    val songId: String,
    val listenedAtEpochMs: Long,
    val durationMs: Long
)
