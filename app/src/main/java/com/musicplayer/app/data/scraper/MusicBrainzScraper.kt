package com.musicplayer.app.data.scraper

import com.musicplayer.app.domain.model.Song
import okhttp3.OkHttpClient
import org.json.JSONObject
import javax.inject.Inject

class MusicBrainzScraper @Inject constructor(
    private val okHttpClient: OkHttpClient
) {
    suspend fun normalize(song: Song): Song {
        return song.copy(
            title = song.title.trim(),
            artistName = song.artistName.trim(),
            albumTitle = song.albumTitle.trim()
        )
    }

    suspend fun findReleaseGroupId(song: Song): String? {
        return findReleaseGroupInfo(song)?.id
    }

    suspend fun findReleaseGroupInfo(song: Song): MusicBrainzReleaseGroupInfo? {
        val info = song.toSearchInfo()
        val artist = info.artist ?: return null
        val albumOrTitle = info.album ?: info.title
        val url = httpUrl(
            base = "https://musicbrainz.org/ws/2/release-group/",
            query = mapOf(
                "query" to "releasegroup:${albumOrTitle} AND artist:${artist}",
                "fmt" to "json",
                "limit" to "6"
            )
        ) ?: return null

        val body = okHttpClient.getString(url) ?: return null
        val groups = runCatching { JSONObject(body).optJSONArray("release-groups") }.getOrNull() ?: return null
        return groups.objects()
            .mapNotNull { json ->
                val title = json.optString("title").takeIf { it.isNotBlank() }
                val artistName = json.optJSONArray("artist-credit")?.optJSONObject(0)?.optString("name")
                    ?.takeIf { it.isNotBlank() }
                val releaseDate = parseReleaseDateParts(json.optString("first-release-date"))
                val score = scoreCandidate(
                    candidateTitle = title,
                    candidateArtist = artistName,
                    candidateAlbum = title,
                    info = info
                ) + json.optInt("score", 0)
                json.optString("id").takeIf { it.isNotBlank() }?.let { id ->
                    MusicBrainzReleaseGroupInfo(
                        id = id,
                        title = title,
                        artistName = artistName,
                        releaseYear = releaseDate.year,
                        releaseMonth = releaseDate.month,
                        score = score
                    )
                }
            }
            .maxByOrNull { it.score }
            ?.takeIf { it.score >= 18 }
    }
}

data class MusicBrainzReleaseGroupInfo(
    val id: String,
    val title: String?,
    val artistName: String?,
    val releaseYear: Int?,
    val releaseMonth: Int?,
    val score: Int
)
