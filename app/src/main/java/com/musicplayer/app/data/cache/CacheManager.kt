package com.musicplayer.app.data.cache

import com.musicplayer.app.data.local.dao.ScrapeCacheDao
import com.musicplayer.app.data.local.entity.toDomain
import com.musicplayer.app.data.local.entity.toEntity
import com.musicplayer.app.domain.model.ScrapeCache
import javax.inject.Inject

class CacheManager @Inject constructor(
    private val scrapeCacheDao: ScrapeCacheDao
) {
    suspend fun get(cacheKey: String): ScrapeCache? = scrapeCacheDao.get(cacheKey)?.toDomain()

    suspend fun put(cache: ScrapeCache) {
        scrapeCacheDao.upsert(cache.toEntity())
    }

    suspend fun prune(maxAgeMs: Long) {
        scrapeCacheDao.prune(System.currentTimeMillis() - maxAgeMs)
    }

    suspend fun count(): Int = scrapeCacheDao.count()

    suspend fun clearAll() {
        scrapeCacheDao.clearAll()
    }
}
