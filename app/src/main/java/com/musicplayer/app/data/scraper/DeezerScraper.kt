package com.musicplayer.app.data.scraper

import com.musicplayer.app.domain.model.Song
import okhttp3.OkHttpClient
import org.json.JSONObject
import javax.inject.Inject

class DeezerScraper @Inject constructor(
    private val okHttpClient: OkHttpClient
) {
    suspend fun findArtwork(song: Song): ArtworkResult? {
        return collectMetadataCandidate(song)?.artwork
    }

    suspend fun collectMetadataCandidate(song: Song): TrackMetadataResult? {
        val info = song.toSearchInfo()
        val query = buildString {
            info.artist?.let { append("artist:\"").append(it).append("\" ") }
            append("track:\"").append(cleanedTitle(info.title)).append("\"")
        }
        val url = httpUrl(
            base = "https://api.deezer.com/search",
            query = mapOf("q" to query, "limit" to "10")
        ) ?: return null
        val body = okHttpClient.getString(url) ?: return null
        val results = runCatching { JSONObject(body).optJSONArray("data") }.getOrNull() ?: return null
        return results.objects()
            .mapNotNull { json ->
                val album = json.optJSONObject("album")
                val artist = json.optJSONObject("artist")
                val artwork = album?.optString("cover_xl")
                    ?.takeIf { it.startsWith("http") } ?: return@mapNotNull null
                val score = scoreCandidate(
                    candidateTitle = json.optString("title"),
                    candidateArtist = artist?.optString("name"),
                    candidateAlbum = album.optString("title"),
                    info = info
                )
                val releaseDate = parseReleaseDateParts(json.optString("release_date"))
                TrackMetadataResult(
                    provider = "Deezer",
                    title = json.optString("title").takeIf { it.isNotBlank() },
                    artistName = artist?.optString("name")?.takeIf { it.isNotBlank() },
                    albumTitle = album.optString("title").takeIf { it.isNotBlank() },
                    releaseYear = releaseDate.year,
                    releaseMonth = releaseDate.month,
                    artwork = ArtworkResult("Deezer", artwork, score),
                    score = score
                )
            }
            .maxByOrNull { it.score }
            ?.takeIf { it.score.acceptableCandidateScore(info) }
    }
}
