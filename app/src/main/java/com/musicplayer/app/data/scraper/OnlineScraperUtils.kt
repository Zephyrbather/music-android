package com.musicplayer.app.data.scraper

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

internal suspend fun OkHttpClient.getString(
    url: String,
    headers: Map<String, String> = emptyMap(),
    timeoutMs: Long = SCRAPER_CALL_TIMEOUT_MS
): String? = withContext(Dispatchers.IO) {
    runCatching {
        val client = if (timeoutMs > 0L) {
            newBuilder()
                .callTimeout(timeoutMs, TimeUnit.MILLISECONDS)
                .build()
        } else {
            this@getString
        }
        val builder = Request.Builder()
            .url(url)
            .header("User-Agent", headers["User-Agent"] ?: "MusicPlayer Android/0.1")
        headers.forEach { (name, value) ->
            if (!name.equals("User-Agent", ignoreCase = true)) {
                builder.header(name, value)
            }
        }
        val request = builder.build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return@withContext null
            response.body?.string()
        }
    }.getOrNull()
}

private const val SCRAPER_CALL_TIMEOUT_MS = 8_000L

internal fun httpUrl(
    base: String,
    query: Map<String, String?>
): String? {
    val builder = base.toHttpUrlOrNull()?.newBuilder() ?: return null
    query.forEach { (key, value) ->
        if (!value.isNullOrBlank()) builder.addQueryParameter(key, value)
    }
    return builder.build().toString()
}

internal fun pathUrl(
    base: String,
    vararg segments: String
): String? {
    val builder = base.toHttpUrlOrNull()?.newBuilder() ?: return null
    segments.forEach { builder.addPathSegment(it) }
    return builder.build().toString()
}

internal fun encoded(value: String): String = URLEncoder.encode(value, "UTF-8")

internal fun JSONArray.objects(): Sequence<JSONObject> = sequence {
    for (index in 0 until length()) {
        optJSONObject(index)?.let { yield(it) }
    }
}

internal fun normalized(value: String?): String {
    return value.orEmpty()
        .lowercase()
        .replace(Regex("[^\\p{L}\\p{N}]+"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()
}

internal fun cleanedTitle(title: String): String {
    var value = title.trim()
    val patterns = listOf(
        Regex("(?i)\\s+(feat\\.?|ft\\.?|with)\\s+.+$"),
        Regex("(?i)\\s*[-–—]\\s*(live|remaster(?:ed)?|karaoke|instrumental|伴奏|version|ver\\.?)\\s*$"),
        Regex("\\s*[\\(\\[（【][^\\)\\]）】]+[\\)\\]）】]\\s*$")
    )
    patterns.forEach { pattern ->
        val candidate = value.replace(pattern, "").trim()
        if (candidate.isNotBlank()) value = candidate
    }
    return value
}

internal fun scoreCandidate(
    candidateTitle: String?,
    candidateArtist: String?,
    candidateAlbum: String?,
    info: MetadataSearchInfo
): Int {
    var score = 0
    val title = normalized(info.title)
    val artist = normalized(info.artist)
    val album = normalized(info.album)
    val candidateTitleNormalized = normalized(candidateTitle)
    val candidateArtistNormalized = normalized(candidateArtist)
    val candidateAlbumNormalized = normalized(candidateAlbum)

    if (title.isNotBlank() && candidateTitleNormalized.isNotBlank()) {
        if (candidateTitleNormalized == title) score += 18
        else if (candidateTitleNormalized.contains(title) || title.contains(candidateTitleNormalized)) score += 8
    }

    if (artist.isNotBlank() && candidateArtistNormalized.isNotBlank()) {
        if (candidateArtistNormalized == artist) score += 18
        else if (candidateArtistNormalized.contains(artist) || artist.contains(candidateArtistNormalized)) score += 12
    } else if (artist.isBlank() && title.isNotBlank() && candidateArtistNormalized.isNotBlank()) {
        if (candidateArtistNormalized == title) score += 18
        else if (candidateArtistNormalized.contains(title) || title.contains(candidateArtistNormalized)) score += 10
    }

    if (album.isNotBlank() && candidateAlbumNormalized.isNotBlank()) {
        if (candidateAlbumNormalized == album) score += 8
        else if (candidateAlbumNormalized.contains(album) || album.contains(candidateAlbumNormalized)) score += 3
    }

    return score
}

internal fun searchTerms(info: MetadataSearchInfo): List<String> {
    val title = cleanedTitle(info.title).ifBlank { info.title }
    return listOf(
        listOfNotNull(title, info.artist).filter { it.isNotBlank() }.joinToString("－"),
        title,
        info.title,
        listOfNotNull(title, info.artist).filter { it.isNotBlank() }.joinToString(" "),
        listOfNotNull(info.artist, title).filter { it.isNotBlank() }.joinToString(" ")
    )
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinctBy(::normalized)
}

internal fun Int.acceptableCandidateScore(info: MetadataSearchInfo): Boolean {
    return this >= if (info.artist.isNullOrBlank()) 8 else 24
}
