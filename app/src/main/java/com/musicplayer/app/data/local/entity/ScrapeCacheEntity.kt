package com.musicplayer.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.musicplayer.app.domain.model.ScrapeCache

@Entity(tableName = "scrape_cache")
data class ScrapeCacheEntity(
    @PrimaryKey val cacheKey: String,
    val provider: String,
    val payload: String,
    val updatedAtEpochMs: Long
)

fun ScrapeCacheEntity.toDomain(): ScrapeCache = ScrapeCache(
    cacheKey = cacheKey,
    provider = provider,
    payload = payload,
    updatedAtEpochMs = updatedAtEpochMs
)

fun ScrapeCache.toEntity(): ScrapeCacheEntity = ScrapeCacheEntity(
    cacheKey = cacheKey,
    provider = provider,
    payload = payload,
    updatedAtEpochMs = updatedAtEpochMs
)
