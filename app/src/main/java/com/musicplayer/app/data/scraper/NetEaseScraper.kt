package com.musicplayer.app.data.scraper

import com.musicplayer.app.domain.model.Song
import okhttp3.OkHttpClient
import org.json.JSONObject
import javax.inject.Inject

class NetEaseScraper @Inject constructor(
    private val okHttpClient: OkHttpClient
) {
    suspend fun findLyrics(song: Song): LyricsResult? {
        val match = searchBestSong(song) ?: return null
        val lyricsUrl = httpUrl(
            base = "https://music.163.com/api/song/lyric",
            query = mapOf("id" to match.id, "lv" to "1", "kv" to "1", "tv" to "-1")
        ) ?: return null
        val body = okHttpClient.getString(lyricsUrl) ?: return null
        val json = runCatching { JSONObject(body) }.getOrNull() ?: return null
        val lyrics = json.optJSONObject("lrc")?.optString("lyric").orEmpty().trim()
        if (lyrics.isBlank()) return null
        return LyricsResult(
            provider = PROVIDER,
            lyrics = lyrics,
            synced = lyrics.contains(Regex("\\[\\d{1,2}:\\d{2}")),
            score = match.score + 2
        )
    }

    suspend fun findFallbackArtwork(song: Song): ArtworkResult? {
        val match = searchBestSong(song) ?: return null
        val image = match.artworkUri ?: return null
        return ArtworkResult(provider = PROVIDER, imageUri = image, score = match.score)
    }

    suspend fun collectCandidates(song: Song): Pair<LyricsResult?, ArtworkResult?> {
        val candidate = collectMetadataCandidate(song)
        return candidate?.lyrics to candidate?.artwork
    }

    suspend fun collectMetadataCandidate(song: Song): TrackMetadataResult? {
        return collectMetadataCandidates(song, limit = 1).firstOrNull()
    }

    suspend fun collectMetadataCandidates(song: Song, limit: Int = 4): List<TrackMetadataResult> {
        return searchSongs(song, limit).map { match ->
            val lyrics = fetchLyrics(match)
            val artwork = match.artworkUri?.let {
                ArtworkResult(PROVIDER, it, match.score)
            }
            TrackMetadataResult(
                provider = PROVIDER,
                title = match.title,
                artistName = match.artistName,
                albumTitle = match.albumTitle,
                releaseYear = match.releaseYear,
                releaseMonth = match.releaseMonth,
                lyrics = lyrics,
                artwork = artwork,
                score = maxOf(match.score, lyrics?.score ?: 0, artwork?.score ?: 0)
            )
        }
    }

    suspend fun findArtistArtwork(artistName: String): ArtistArtworkResult? {
        val normalizedName = artistName.trim()
        if (normalizedName.isBlank() || normalizedName == "Unknown Artist" || normalizedName == "Local Folder") return null
        val url = httpUrl(
            base = "https://music.163.com/api/search/get/web",
            query = mapOf("s" to normalizedName, "type" to "100", "limit" to "5")
        ) ?: return null
        val body = okHttpClient.getString(url) ?: return null
        val artists = runCatching {
            JSONObject(body).optJSONObject("result")?.optJSONArray("artists")
        }.getOrNull() ?: return null
        return artists.objects()
            .mapNotNull { json ->
                val image = json.optString("picUrl").takeIf { it.startsWith("http") } ?: return@mapNotNull null
                val score = if (normalized(json.optString("name")) == normalized(normalizedName)) 24 else 10
                ArtistArtworkResult(PROVIDER, image, score)
            }
            .maxByOrNull { it.score }
            ?.takeIf { it.score >= 10 }
    }

    private suspend fun searchBestSong(song: Song): NetEaseSongMatch? {
        return searchSongs(song, limit = 1).firstOrNull()
    }

    private suspend fun searchSongs(song: Song, limit: Int): List<NetEaseSongMatch> {
        val info = song.toSearchInfo()
        val matches = mutableListOf<NetEaseSongMatch>()
        for (term in searchTerms(info)) {
            val url = httpUrl(
                base = "https://music.163.com/api/cloudsearch/pc",
                query = mapOf("s" to term, "type" to "1", "limit" to "12", "offset" to "0")
            ) ?: continue
            val body = okHttpClient.getString(url, NETEASE_HEADERS) ?: continue
            val songs = runCatching {
                JSONObject(body).optJSONObject("result")?.optJSONArray("songs")
            }.getOrNull() ?: continue

            songs.objects()
                .mapNotNull { json ->
                    val artistsJson = json.optJSONArray("ar") ?: json.optJSONArray("artists")
                    val artists = artistsJson?.objects()
                        ?.mapNotNull { it.optString("name").takeIf(String::isNotBlank) }
                        ?.joinToString(", ")
                    val album = json.optJSONObject("al") ?: json.optJSONObject("album")
                    val releaseDate = releaseDatePartsFromEpochMillis(
                        album?.optLong("publishTime")?.takeIf { it > 0L }
                    )
                    val score = scoreCandidate(
                        candidateTitle = json.optString("name"),
                        candidateArtist = artists,
                        candidateAlbum = album?.optString("name"),
                        info = info
                    )
                    val id = json.optLong("id").takeIf { it > 0L }?.toString() ?: return@mapNotNull null
                    NetEaseSongMatch(
                        id = id,
                        title = json.optString("name").takeIf { it.isNotBlank() },
                        artistName = artists,
                        albumTitle = album?.optString("name")?.takeIf { it.isNotBlank() },
                        releaseYear = releaseDate.year,
                        releaseMonth = releaseDate.month,
                        score = score,
                        artworkUri = album?.optString("picUrl")?.takeIf { it.isNotBlank() }
                    )
                }
                .filter { it.score.acceptableCandidateScore(info) }
                .let { matches += it }
        }
        return matches
            .distinctBy { it.id }
            .sortedByDescending { it.score }
            .take(limit)
    }

    private suspend fun fetchLyrics(match: NetEaseSongMatch): LyricsResult? {
        return runCatching {
            val lyricsUrl = httpUrl(
                base = "https://music.163.com/api/song/lyric",
                query = mapOf("id" to match.id, "lv" to "1", "kv" to "1", "tv" to "-1")
            ) ?: return@runCatching null
            val body = okHttpClient.getString(lyricsUrl, NETEASE_HEADERS) ?: return@runCatching null
            val json = JSONObject(body)
            val text = json.optJSONObject("lrc")?.optString("lyric").orEmpty().trim()
            if (text.isBlank()) null else LyricsResult(
                provider = PROVIDER,
                lyrics = text,
                synced = text.contains(Regex("\\[\\d{1,2}:\\d{2}")),
                score = match.score + 2
            )
        }.getOrNull()
    }
    private companion object {
        const val PROVIDER = "NetEase Cloud Music"
        val NETEASE_HEADERS = mapOf(
            "Referer" to "https://music.163.com/",
            "User-Agent" to "Mozilla/5.0"
        )
    }
}

private data class NetEaseSongMatch(
    val id: String,
    val title: String?,
    val artistName: String?,
    val albumTitle: String?,
    val releaseYear: Int?,
    val releaseMonth: Int?,
    val score: Int,
    val artworkUri: String?
)
