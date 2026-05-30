package com.musicplayer.app.data.scraper

import com.musicplayer.app.domain.model.Song
import javax.inject.Inject
import okhttp3.OkHttpClient
import org.json.JSONObject

class QQMusicScraper @Inject constructor(
    private val okHttpClient: OkHttpClient
) {
    suspend fun findLyrics(song: Song): LyricsResult? {
        val match = searchBestSong(song) ?: return null
        val url = httpUrl(
            base = "https://c.y.qq.com/lyric/fcgi-bin/fcg_query_lyric_new.fcg",
            query = mapOf(
                "songmid" to match.mid,
                "format" to "json",
                "nobase64" to "1",
                "g_tk" to "5381"
            )
        ) ?: return null
        val body = okHttpClient.getString(url, QQ_HEADERS) ?: return null
        val jsonText = body.substringAfter("MusicJsonCallback(", body).substringBeforeLast(")", body)
        val lyrics = runCatching { JSONObject(jsonText).optString("lyric") }.getOrNull().orEmpty()
            .replace("&#10;", "\n")
            .replace("&apos;", "'")
            .replace("&quot;", "\"")
            .trim()
        if (lyrics.isBlank()) return null
        return LyricsResult(
            provider = "QQ Music",
            lyrics = lyrics,
            synced = lyrics.contains(Regex("\\[\\d{1,2}:\\d{2}")),
            score = match.score
        )
    }

    suspend fun findArtwork(song: Song): ArtworkResult? {
        val match = searchBestSong(song) ?: return null
        val image = "https://y.gtimg.cn/music/photo_new/T002R800x800M000${match.albumMid}.jpg"
        return ArtworkResult("QQ Music", image, match.score)
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
            val artwork = ArtworkResult(
                "QQ Music",
                "https://y.gtimg.cn/music/photo_new/T002R800x800M000${match.albumMid}.jpg",
                match.score
            )
            TrackMetadataResult(
                provider = "QQ Music",
                title = match.title,
                artistName = match.artistName,
                albumTitle = match.albumTitle,
                releaseYear = match.releaseYear,
                releaseMonth = match.releaseMonth,
                lyrics = lyrics,
                artwork = artwork,
                score = maxOf(match.score, lyrics?.score ?: 0, artwork.score)
            )
        }
    }

    suspend fun findArtistArtwork(artistName: String): ArtistArtworkResult? {
        val normalizedName = artistName.trim()
        if (normalizedName.isBlank() || normalizedName == "Unknown Artist" || normalizedName == "Local Folder") return null
        val url = httpUrl(
            base = "https://c.y.qq.com/soso/fcgi-bin/client_search_cp",
            query = mapOf(
                "w" to normalizedName,
                "format" to "json",
                "n" to "5",
                "p" to "1",
                "t" to "9"
            )
        ) ?: return null
        val body = okHttpClient.getString(url, QQ_HEADERS) ?: return null
        val list = runCatching {
            JSONObject(body).optJSONObject("data")
                ?.optJSONObject("singer")
                ?.optJSONArray("list")
        }.getOrNull() ?: return null
        return list.objects()
            .mapNotNull { json ->
                val mid = json.optString("singerMID").ifBlank { json.optString("singermid") }
                    .takeIf { it.isNotBlank() } ?: return@mapNotNull null
                val score = if (normalized(json.optString("singerName")) == normalized(normalizedName)) 24 else 10
                ArtistArtworkResult(
                    provider = "QQ Music",
                    imageUri = "https://y.gtimg.cn/music/photo_new/T001R800x800M000$mid.jpg",
                    score = score
                )
            }
            .maxByOrNull { it.score }
            ?.takeIf { it.score >= 10 }
    }

    private suspend fun searchBestSong(song: Song): QQSongMatch? {
        return searchSongs(song, limit = 1).firstOrNull()
    }

    private suspend fun searchSongs(song: Song, limit: Int): List<QQSongMatch> {
        val info = song.toSearchInfo()
        val matches = mutableListOf<QQSongMatch>()
        for (term in searchTerms(info)) {
            val url = httpUrl(
                base = "https://c.y.qq.com/soso/fcgi-bin/search_for_qq_cp",
                query = mapOf(
                    "w" to term,
                    "format" to "json",
                    "n" to "12",
                    "p" to "1",
                    "t" to "0",
                    "aggr" to "1",
                    "cr" to "1",
                    "lossless" to "0",
                    "flag_qc" to "0",
                    "g_tk" to "5381"
                )
            ) ?: continue
            val body = okHttpClient.getString(url, QQ_HEADERS) ?: continue
            val songs = runCatching {
                JSONObject(body).optJSONObject("data")
                    ?.optJSONObject("song")
                    ?.optJSONArray("list")
            }.getOrNull() ?: continue

            songs.objects()
                .mapNotNull { json ->
                    val mid = json.optString("songmid").takeIf { it.isNotBlank() } ?: return@mapNotNull null
                    val albumMid = json.optString("albummid").takeIf { it.isNotBlank() } ?: return@mapNotNull null
                    val artists = json.optJSONArray("singer")?.objects()
                        ?.mapNotNull { it.optString("name").takeIf(String::isNotBlank) }
                        ?.joinToString(", ")
                    val albumTitle = json.optString("albumname").takeIf { it.isNotBlank() }
                    val releaseDate = parseReleaseDateParts(
                        listOf("publictime", "public_time", "publishTime", "pubtime")
                            .firstNotNullOfOrNull { key -> json.optString(key).takeIf(String::isNotBlank) }
                    )
                    val score = scoreCandidate(
                        candidateTitle = json.optString("songname"),
                        candidateArtist = artists,
                        candidateAlbum = albumTitle,
                        info = info
                    )
                    QQSongMatch(
                        mid = mid,
                        albumMid = albumMid,
                        title = json.optString("songname").takeIf { it.isNotBlank() },
                        artistName = artists,
                        albumTitle = albumTitle,
                        releaseYear = releaseDate.year,
                        releaseMonth = releaseDate.month,
                        score = score
                    )
                }
                .filter { it.score.acceptableCandidateScore(info) }
                .let { matches += it }
        }
        return matches
            .distinctBy { it.mid }
            .sortedByDescending { it.score }
            .take(limit)
    }

    private suspend fun fetchLyrics(match: QQSongMatch): LyricsResult? {
        return runCatching {
            val url = httpUrl(
                base = "https://c.y.qq.com/lyric/fcgi-bin/fcg_query_lyric_new.fcg",
                query = mapOf("songmid" to match.mid, "format" to "json", "nobase64" to "1", "g_tk" to "5381")
            ) ?: return@runCatching null
            val body = okHttpClient.getString(url, QQ_HEADERS) ?: return@runCatching null
            val jsonText = body.substringAfter("MusicJsonCallback(", body).substringBeforeLast(")", body)
            val text = runCatching { JSONObject(jsonText).optString("lyric") }.getOrNull().orEmpty()
                .replace("&#10;", "\n")
                .replace("&apos;", "'")
                .replace("&quot;", "\"")
                .trim()
            if (text.isBlank()) null else LyricsResult(
                provider = "QQ Music",
                lyrics = text,
                synced = text.contains(Regex("\\[\\d{1,2}:\\d{2}")),
                score = match.score
            )
        }.getOrNull()
    }
}

private val QQ_HEADERS = mapOf(
    "Referer" to "https://y.qq.com/",
    "User-Agent" to "Mozilla/5.0"
)

private data class QQSongMatch(
    val mid: String,
    val albumMid: String,
    val title: String?,
    val artistName: String?,
    val albumTitle: String?,
    val releaseYear: Int?,
    val releaseMonth: Int?,
    val score: Int
)
