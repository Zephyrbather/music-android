package com.musicplayer.app.data.scraper

import com.musicplayer.app.domain.model.Song
import javax.inject.Inject
import okhttp3.OkHttpClient
import org.json.JSONObject

class JioSaavnScraper @Inject constructor(
    private val okHttpClient: OkHttpClient
) {
    suspend fun collectMetadataCandidate(song: Song): TrackMetadataResult? {
        val info = song.toSearchInfo()
        val term = listOfNotNull(info.artist, cleanedTitle(info.title)).joinToString(" ").ifBlank { info.title }
        val url = httpUrl(
            base = "https://www.jiosaavn.com/api.php",
            query = mapOf(
                "__call" to "autocomplete.get",
                "_format" to "json",
                "_marker" to "0",
                "query" to term
            )
        ) ?: return null
        val body = okHttpClient.getString(url) ?: return null
        val songs = runCatching { JSONObject(body).optJSONObject("songs")?.optJSONArray("data") }.getOrNull()
            ?: return null

        return songs.objects()
            .mapNotNull { json ->
                val artwork = json.optString("image")
                    .replace("50x50", "500x500")
                    .replace("150x150", "500x500")
                    .takeIf { it.startsWith("http") } ?: return@mapNotNull null
                val title = json.optString("title").takeIf { it.isNotBlank() }
                val album = json.optString("album").takeIf { it.isNotBlank() }
                val artist = json.optString("more_info")
                    .takeIf { it.isNotBlank() }
                    ?.let { moreInfo -> runCatching { JSONObject(moreInfo).optString("singers") }.getOrNull() }
                    ?.takeIf { it.isNotBlank() }
                val score = scoreCandidate(
                    candidateTitle = title,
                    candidateArtist = artist,
                    candidateAlbum = album,
                    info = info
                )
                TrackMetadataResult(
                    provider = "JioSaavn",
                    title = title,
                    artistName = artist,
                    albumTitle = album,
                    artwork = ArtworkResult("JioSaavn", artwork, score),
                    score = score
                )
            }
            .maxByOrNull { it.score }
            ?.takeIf { it.score.acceptableCandidateScore(info) }
    }
}
