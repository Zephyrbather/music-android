package com.musicplayer.app.data.scraper

import com.musicplayer.app.domain.model.Song
import okhttp3.OkHttpClient
import org.json.JSONObject
import javax.inject.Inject

class ITunesScraper @Inject constructor(
    private val okHttpClient: OkHttpClient
) {
    suspend fun findArtwork(song: Song): ArtworkResult? {
        return collectMetadataCandidate(song)?.artwork
    }

    suspend fun collectMetadataCandidate(song: Song): TrackMetadataResult? {
        return collectMetadataCandidates(song, limit = 1).firstOrNull()
    }

    suspend fun collectMetadataCandidates(song: Song, limit: Int = 4): List<TrackMetadataResult> {
        val info = song.toSearchInfo()
        val term = searchTerms(info).first()
        val url = httpUrl(
            base = "https://itunes.apple.com/search",
            query = mapOf("term" to term, "entity" to "song", "limit" to "12", "country" to "cn")
        ) ?: return emptyList()
        val body = okHttpClient.getString(url) ?: return emptyList()
        val results = runCatching { JSONObject(body).optJSONArray("results") }.getOrNull() ?: return emptyList()
        return results.objects()
            .mapNotNull { json ->
                val artwork = json.optString("artworkUrl100")
                    .replace("100x100bb", "600x600bb")
                    .takeIf { it.startsWith("http") } ?: return@mapNotNull null
                val score = scoreCandidate(
                    candidateTitle = json.optString("trackName"),
                    candidateArtist = json.optString("artistName"),
                    candidateAlbum = json.optString("collectionName"),
                    info = info
                )
                val releaseDate = parseReleaseDateParts(json.optString("releaseDate"))
                TrackMetadataResult(
                    provider = "Apple Music",
                    title = json.optString("trackName").takeIf { it.isNotBlank() },
                    artistName = json.optString("artistName").takeIf { it.isNotBlank() },
                    albumTitle = json.optString("collectionName").takeIf { it.isNotBlank() },
                    releaseYear = releaseDate.year,
                    releaseMonth = releaseDate.month,
                    artwork = ArtworkResult("Apple Music", artwork, score),
                    score = score
                )
            }
            .filter { it.score.acceptableCandidateScore(info) }
            .distinctBy { listOf(it.title, it.artistName, it.albumTitle).joinToString("|") }
            .sortedByDescending { it.score }
            .take(limit)
            .toList()
    }
}
