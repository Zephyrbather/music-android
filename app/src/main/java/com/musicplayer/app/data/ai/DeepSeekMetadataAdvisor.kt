package com.musicplayer.app.data.ai

import com.musicplayer.app.domain.ai.AiMetadataAdvisor
import com.musicplayer.app.domain.ai.AiPlaylistSuggestion
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

    override suspend fun generatePlaylist(
        prompt: String,
        songs: List<Song>,
        apiKey: String,
        model: String,
        maxSongs: Int
    ): AiPlaylistSuggestion? {
        val trimmedPrompt = prompt.trim()
        if (trimmedPrompt.isBlank() || songs.isEmpty()) return null
        val compactSongs = JSONArray().apply {
            songs.take(MAX_PLAYLIST_INPUT_SONGS).forEach { song ->
                put(
                    JSONObject().apply {
                        put("id", song.id)
                        put("title", song.title)
                        put("artist", song.artistName)
                        put("album", song.albumTitle)
                        put("category", song.categoryTag.orEmpty())
                        put("favorite", song.isFavorite)
                        put("year", song.releaseYear ?: JSONObject.NULL)
                    }
                )
            }
        }
        val input = JSONObject().apply {
            put("request", trimmedPrompt)
            put("maxSongs", maxSongs.coerceIn(1, MAX_PLAYLIST_SIZE))
            put("library", compactSongs)
        }
        val result = completeJson(
            apiKey = apiKey,
            model = model,
            systemPrompt = "Create a music playlist from the provided local library summary. " +
                "Only choose songs by id from the library. Return JSON only with name, description, and songIds array. " +
                "Do not invent song ids. Match the user's requested mood, scene, language, artist, era, or style.",
            userPrompt = input.toString()
        ) ?: return null
        val songIds = result.optJSONArray("songIds")
            ?.let { arr -> (0 until arr.length()).mapNotNull { arr.optString(it).takeIf(String::isNotBlank) } }
            .orEmpty()
            .distinct()
            .take(maxSongs.coerceIn(1, MAX_PLAYLIST_SIZE))
        if (songIds.isEmpty()) return null
        val libraryIds = songs.map { it.id }.toSet()
        val validIds = songIds.filter { it in libraryIds }
        if (validIds.isEmpty()) return null
        return AiPlaylistSuggestion(
            name = result.optString("name").trim().takeIf { it.isNotBlank() } ?: "AI Mix",
            description = result.optString("description").trim().takeIf { it.isNotBlank() },
            songIds = validIds
        )
    }

    override suspend fun generateSemanticTags(
        song: Song,
        apiKey: String,
        model: String
    ): List<String> {
        val input = JSONObject().apply {
            put("title", song.title)
            put("artist", song.artistName)
            put("album", song.albumTitle)
            put("category", song.categoryTag.orEmpty())
            put("year", song.releaseYear ?: JSONObject.NULL)
            put("lyricsExcerpt", song.lyrics.orEmpty().take(MAX_LYRICS_EXCERPT_CHARS))
        }
        val result = completeJson(
            apiKey = apiKey,
            model = model,
            systemPrompt = "Generate concise semantic music tags for local library organization. " +
                "Return JSON only with tags array. Include mood, scene, language, style, and energy when inferable. " +
                "Use short lowercase English tags, no more than 8 tags. Do not include artist/title names.",
            userPrompt = input.toString()
        ) ?: return emptyList()
        return result.optJSONArray("tags")
            ?.let { arr -> (0 until arr.length()).mapNotNull { arr.optString(it).normalizeSemanticTag() } }
            .orEmpty()
            .distinct()
            .take(MAX_SEMANTIC_TAGS)
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
        const val MAX_PLAYLIST_INPUT_SONGS = 200
        const val MAX_PLAYLIST_SIZE = 50
        const val MAX_LYRICS_EXCERPT_CHARS = 1200
        const val MAX_SEMANTIC_TAGS = 8
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}

private fun String.normalizeSemanticTag(): String? {
    val normalized = trim()
        .lowercase()
        .replace(Regex("""\s+"""), "-")
        .filter { it.isLetterOrDigit() || it == '-' }
        .trim('-')
    return normalized.takeIf { it.length in 2..32 }
}
