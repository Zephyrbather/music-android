package com.musicplayer.app.domain.model

data class WebDavServer(
    val id: String,
    val name: String,
    val baseUrl: String,
    val username: String,
    val password: String,
    val enabled: Boolean = true,
    val lastSyncEpochMs: Long? = null
)
