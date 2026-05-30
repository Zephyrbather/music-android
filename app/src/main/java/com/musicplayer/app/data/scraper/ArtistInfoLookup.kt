package com.musicplayer.app.data.scraper

import com.musicplayer.app.data.cache.CacheManager
import com.musicplayer.app.domain.model.ArtistOnlineInfo
import com.musicplayer.app.domain.model.ScrapeCache
import javax.inject.Inject
import org.json.JSONObject
import okhttp3.OkHttpClient

class ArtistInfoLookup @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val cacheManager: CacheManager
) {
    suspend fun findArtistInfo(artistName: String): ArtistOnlineInfo? {
        val normalizedName = artistName.trim()
        if (normalizedName.isBlank() || normalizedName == "Unknown Artist" || normalizedName == "Local Folder") {
            return null
        }

        val cacheKey = "artist-info:${normalized(normalizedName)}"
        cacheManager.get(cacheKey)?.let { cached ->
            parsePayload(cached.payload)?.let { return it }
        }

        val info = listOf(
            "https://zh.wikipedia.org/api/rest_v1/page/summary",
            "https://en.wikipedia.org/api/rest_v1/page/summary"
        ).firstNotNullOfOrNull { baseUrl ->
            findWikipediaSummary(baseUrl, normalizedName)
        } ?: return null

        cacheManager.put(
            ScrapeCache(
                cacheKey = cacheKey,
                provider = info.provider,
                payload = info.toPayload()
            )
        )
        return info
    }

    private suspend fun findWikipediaSummary(baseUrl: String, artistName: String): ArtistOnlineInfo? {
        val url = pathUrl(baseUrl, artistName) ?: return null
        val body = okHttpClient.getString(url) ?: return null
        val json = runCatching { JSONObject(body) }.getOrNull() ?: return null
        val extract = json.optString("extract").trim()
        if (extract.isBlank()) return null
        return ArtistOnlineInfo(
            name = json.optString("title").takeIf { it.isNotBlank() } ?: artistName,
            bio = extract,
            avatarUri = json.optJSONObject("thumbnail")
                ?.optString("source")
                ?.takeIf { it.isNotBlank() },
            provider = "Wikipedia"
        )
    }

    private fun parsePayload(payload: String): ArtistOnlineInfo? {
        val json = runCatching { JSONObject(payload) }.getOrNull() ?: return null
        val bio = json.optString("bio").trim()
        if (bio.isBlank()) return null
        return ArtistOnlineInfo(
            name = json.optString("name"),
            bio = bio,
            avatarUri = json.optString("avatarUri").takeIf { it.isNotBlank() },
            provider = json.optString("provider").ifBlank { "Wikipedia" }
        )
    }

    private fun ArtistOnlineInfo.toPayload(): String {
        return JSONObject()
            .put("name", name)
            .put("bio", bio)
            .put("avatarUri", avatarUri)
            .put("provider", provider)
            .toString()
    }
}
