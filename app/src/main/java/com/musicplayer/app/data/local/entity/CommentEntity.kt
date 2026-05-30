package com.musicplayer.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "comments",
    foreignKeys = [
        ForeignKey(
            entity = SongEntity::class,
            parentColumns = ["id"],
            childColumns = ["songId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("songId")]
)
data class CommentEntity(
    @PrimaryKey val id: String,
    val songId: String,
    val rating: Int,
    val text: String,
    val timestampEpochMs: Long
)
