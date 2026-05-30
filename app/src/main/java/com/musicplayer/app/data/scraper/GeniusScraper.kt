package com.musicplayer.app.data.scraper

import com.musicplayer.app.domain.model.Song
import javax.inject.Inject
import okhttp3.OkHttpClient
import org.json.JSONObject

class GeniusScraper @Inject constructor(
    private val okHttpClient: OkHttpClient
) {
    suspend fun collectMetadataCandidate(song: Song): TrackMetadataResult? {
        val info = song.toSearchInfo()
        val term = listOfNotNull(info.artist, cleanedTitle(info.title)).joinToString(" ").ifBlank { info.title }
        val url = httpUrl(
            base = "https://genius.com/api/search/multi",
            query = mapOf("q" to term)
        ) ?: return null
        val body = okHttpClient.getString(url) ?: return null
        val sections = runCatching {
            JSONObject(body).optJSONObject("response")?.optJSONArray("sections")
        }.getOrNull() ?: return null

        return sections.objects()
            .flatMap { section ->
                section.optJSONArray("hits")?.objects().orEmpty()
            }
            .mapNotNull { hit ->
                val result = hit.optJSONObject("result") ?: return@mapNotNull null
                val artwork = result.optString("song_art_image_url")
                    .ifBlank { result.optString("header_image_url") }
                    .takeIf { it.startsWith("http") } ?: return@mapNotNull null
                val artistName = result.optJSONObject("primary_artist")?.optString("name")
                    ?.takeIf { it.isNotBlank() }
                val title = result.optString("title").takeIf { it.isNotBlank() }
                val score = scoreCandidate(
                    candidateTitle = title,
                    candidateArtist = artistName,
                    candidateAlbum = null,
                    info = info
                )
                val releaseDate = parseReleaseDateParts(result.optString("release_date_for_display"))
                TrackMetadataResult(
                    provider = "Genius",
                    title = title,
                    artistName = artistName,
                    releaseYear = releaseDate.year,
                    releaseMonth = releaseDate.month,
                    artwork = ArtworkResult("Genius", artwork, score),
                    score = score
                )
            }
            .maxByOrNull { it.score }
            ?.takeIf { it.score.acceptableCandidateScore(info) }
    }
}
