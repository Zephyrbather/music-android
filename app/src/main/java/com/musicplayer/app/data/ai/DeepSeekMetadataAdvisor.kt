package com.musicplayer.app.data.ai

import com.musicplayer.app.domain.ai.AiMetadataAdvisor
import com.musicplayer.app.domain.ai.AiSongIdentity
import com.musicplayer.app.domain.model.SongCategoryTags
import com.musicplayer.app.domain.model.Song
import com.musicplayer.app.domain.model.TrackMetadataCandidate
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

@Singleton
class DeepSeekMetadataAdvisor @Inject constructor(
    private val okHttpClient: OkHttpClient
) : AiMetadataAdvisor {
    override suspend fun identifySong(
        song: Song,
        apiKey: String,
        model: String
    ): AiSongIdentity? {
        val input = JSONObject().apply {
            put("storedTitle", song.title)
            put("storedArtist", song.artistName)
            put("storedAlbum", song.albumTitle)
            put("fileName", song.uri.substringAfterLast('/'))
        }
        val result = completeJson(
            apiKey = apiKey,
            model = model,
            systemPrompt = "Identify a music track from noisy local metadata. Return JSON only with " +
                "title and artist. Preserve the original language. Use an empty artist if unknown.",
            userPrompt = input.toString()
        ) ?: return null
        val title = result.optString("title").trim().takeIf { it.isNotBlank() } ?: return null
        val artist = result.optString("artist").trim()
        return AiSongIdentity(title = title, artistName = artist)
    }

    override suspend fun selectCandidate(
        identity: AiSongIdentity,
        candidates: List<TrackMetadataCandidate>,
        apiKey: String,
        model: String
    ): TrackMetadataCandidate? {
        if (candidates.isEmpty()) return null
        val compactCandidates = JSONArray().apply {
            candidates.forEachIndexed { index, candidate ->
                put(
                    JSONObject().apply {
                        put("index", index)
                        put("provider", candidate.provider)
                        put("title", candidate.title.orEmpty())
                        put("artist", candidate.artistName.orEmpty())
                        put("album", candidate.albumTitle.orEmpty())
                        put("year", candidate.releaseYear ?: JSONObject.NULL)
                        put("hasLyrics", candidate.lyrics != null)
                        put("hasArtwork", candidate.artwork != null)
                        put("sourceScore", candidate.score)
                    }
                )
            }
        }
        val input = JSONObject().apply {
            put("identifiedTitle", identity.title)
            put("identifiedArtist", identity.artistName)
            put("candidates", compactCandidates)
        }
        val result = completeJson(
            apiKey = apiKey,
            model = model,
            systemPrompt = "Choose the search result that matches the identified music track. " +
                "Return JSON only with selectedIndex as a zero-based integer, or -1 when none match. " +
                "Prefer an exact title and artist match; artwork and lyrics completeness are secondary.",
            userPrompt = input.toString()
        ) ?: return null
        return candidates.getOrNull(result.optInt("selectedIndex", -1))
    }

    override suspend fun classifySong(
        song: Song,
        apiKey: String,
        model: String,
        categories: List<String>
    ): String? {
        val input = JSONObject().apply {
            put("title", song.title)
            put("artist", song.artistName)
            put("album", song.albumTitle)
            put("fileName", song.uri.substringAfterLast('/'))
            put("categories", JSONArray(categories))
        }
        val result = completeJson(
            apiKey = apiKey,
            model = model,
            systemPrompt = "Classify the music track into exactly one category from the provided categories. " +
                "Use the best equalizer-oriented category. Return JSON only with category.",
            userPrompt = input.toString()
        ) ?: return null
        return SongCategoryTags.normalize(result.optString("category"))
    }

    private suspend fun completeJson(
        apiKey: String,
        model: String,
        systemPrompt: String,
        userPrompt: String
    ): JSONObject? = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext null
        val requestBody = JSONObject().apply {
            put("model", model.trim().ifBlank { DEFAULT_MODEL })
            put(
                "messages",
                JSONArray().apply {
                    put(JSONObject().put("role", "system").put("content", systemPrompt))
                    put(JSONObject().put("role", "user").put("content", userPrompt))
                }
            )
            put("response_format", JSONObject().put("type", "json_object"))
        }
        val request = Request.Builder()
            .url(API_URL)
            .header("Authorization", "Bearer ${apiKey.trim()}")
            .header("Content-Type", "application/json")
            .post(requestBody.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()
        runCatching {
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use null
                val content = JSONObject(response.body?.string().orEmpty())
                    .optJSONArray("choices")
                    ?.optJSONObject(0)
                    ?.optJSONObject("message")
                    ?.optString("content")
                    ?.trim()
                    .orEmpty()
                parseJsonResponse(content)
            }
        }.getOrNull()
    }

    private fun parseJsonResponse(content: String): JSONObject? {
        val json = content.removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        return runCatching { JSONObject(json) }.getOrNull()
    }

    private companion object {
        const val API_URL = "https://api.deepseek.com/chat/completions"
        const val DEFAULT_MODEL = "deepseek-v4-flash"
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}
