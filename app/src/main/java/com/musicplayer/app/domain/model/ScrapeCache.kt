package com.musicplayer.app.domain.model

data class ScrapeCache(
    val cacheKey: String,
    val provider: String,
    val payload: String,
    val updatedAtEpochMs: Long = System.currentTimeMillis()
)
