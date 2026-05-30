package com.musicplayer.app.data.scraper

import com.musicplayer.app.domain.model.Song
import okhttp3.OkHttpClient
import org.json.JSONObject
import javax.inject.Inject

class LyricsOvhScraper @Inject constructor(
    private val okHttpClient: OkHttpClient
) {
    suspend fun findLyrics(song: Song): LyricsResult? {
        return collectMetadataCandidate(song)?.lyrics
    }

    suspend fun collectMetadataCandidate(song: Song): TrackMetadataResult? {
        val info = song.toSearchInfo()
        val artist = info.artist ?: return null
        val titles = listOf(info.title, cleanedTitle(info.title)).distinctBy(::normalized)
        return titles.firstNotNullOfOrNull { title ->
            val url = pathUrl("https://api.lyrics.ovh/v1", artist, title) ?: return@firstNotNullOfOrNull null
            val body = okHttpClient.getString(url) ?: return@firstNotNullOfOrNull null
            val lyrics = runCatching { JSONObject(body).optString("lyrics") }.getOrNull().orEmpty().trim()
            if (lyrics.isBlank()) return@firstNotNullOfOrNull null
            val lyricsResult = LyricsResult(
                provider = "lyrics.ovh",
                lyrics = lyrics,
                synced = lyrics.contains(Regex("\\[\\d{1,2}:\\d{2}")),
                score = scoreCandidate(title, artist, info.album, info)
            ).takeIf { it.score >= 8 }
            lyricsResult?.let {
                TrackMetadataResult(
                    provider = "lyrics.ovh",
                    title = title,
                    artistName = artist,
                    lyrics = it,
                    score = it.score
                )
            }
        }
    }
}
