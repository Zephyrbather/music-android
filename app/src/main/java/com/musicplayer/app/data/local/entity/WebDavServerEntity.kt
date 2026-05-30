package com.musicplayer.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.musicplayer.app.domain.model.WebDavServer

@Entity(tableName = "webdav_servers")
data class WebDavServerEntity(
    @PrimaryKey val id: String,
    val name: String,
    val baseUrl: String,
    val username: String,
    val password: String,
    val enabled: Boolean,
    val lastSyncEpochMs: Long?
)

fun WebDavServerEntity.toDomain(): WebDavServer = WebDavServer(
    id = id,
    name = name,
    baseUrl = baseUrl,
    username = username,
    password = password,
    enabled = enabled,
    lastSyncEpochMs = lastSyncEpochMs
)

fun WebDavServer.toEntity(): WebDavServerEntity = WebDavServerEntity(
    id = id,
    name = name,
    baseUrl = baseUrl,
    username = username,
    password = password,
    enabled = enabled,
    lastSyncEpochMs = lastSyncEpochMs
)
