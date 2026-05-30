package com.musicplayer.app.data.scraper

import com.musicplayer.app.domain.model.Song
import okhttp3.OkHttpClient
import org.json.JSONArray
import javax.inject.Inject

class LrcLibScraper @Inject constructor(
    private val okHttpClient: OkHttpClient
) {
    suspend fun findLyrics(song: Song): LyricsResult? {
        return collectMetadataCandidate(song)?.lyrics
    }

    suspend fun collectMetadataCandidate(song: Song): TrackMetadataResult? {
        val info = song.toSearchInfo()
        val variants = listOf(info, info.copy(title = cleanedTitle(info.title))).distinctBy { normalized(it.title) }
        return variants.firstNotNullOfOrNull { search ->
            val url = httpUrl(
                base = "https://lrclib.net/api/search",
                query = mapOf(
                    "track_name" to search.title,
                    "artist_name" to search.artist,
                    "album_name" to search.album
                )
            ) ?: return@firstNotNullOfOrNull null
            val body = okHttpClient.getString(url) ?: return@firstNotNullOfOrNull null
            val candidates = runCatching { JSONArray(body) }.getOrNull() ?: return@firstNotNullOfOrNull null
            candidates.objects()
                .mapNotNull { json ->
                    val synced = json.optString("syncedLyrics").takeIf { it.isNotBlank() }
                    val plain = json.optString("plainLyrics").takeIf { it.isNotBlank() }
                    val lyrics = synced ?: plain ?: return@mapNotNull null
                    val score = scoreCandidate(
                        candidateTitle = json.optString("trackName"),
                        candidateArtist = json.optString("artistName"),
                        candidateAlbum = json.optString("albumName"),
                        info = info
                    ) + if (synced != null) 4 else 0
                    LyricsResult(
                        provider = "LRCLIB",
                        lyrics = lyrics,
                        synced = synced != null,
                        score = score
                    ).let { lyricsResult ->
                        TrackMetadataResult(
                            provider = "LRCLIB",
                            title = json.optString("trackName").takeIf { it.isNotBlank() },
                            artistName = json.optString("artistName").takeIf { it.isNotBlank() },
                            albumTitle = json.optString("albumName").takeIf { it.isNotBlank() },
                            lyrics = lyricsResult,
                            score = lyricsResult.score
                        )
                    }
                }
                .maxByOrNull { it.score }
                ?.takeIf { it.score.acceptableCandidateScore(info) }
        }
    }
}
