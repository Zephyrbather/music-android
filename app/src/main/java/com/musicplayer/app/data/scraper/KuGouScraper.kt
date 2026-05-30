package com.musicplayer.app.data.scraper

import com.musicplayer.app.domain.model.Song
import javax.inject.Inject
import okhttp3.OkHttpClient
import org.json.JSONObject

class KuGouScraper @Inject constructor(
    private val okHttpClient: OkHttpClient
) {
    suspend fun findLyrics(song: Song): LyricsResult? {
        val match = searchBestSong(song) ?: return null
        val url = httpUrl(
            base = "https://lyrics.kugou.com/search",
            query = mapOf(
                "ver" to "1",
                "man" to "yes",
                "client" to "pc",
                "keyword" to listOfNotNull(song.artistName, cleanedTitle(song.title)).joinToString(" "),
                "duration" to song.durationMs.takeIf { it > 0L }?.div(1000L)?.toString(),
                "hash" to match.hash
            )
        ) ?: return null
        val body = okHttpClient.getString(url) ?: return null
        val candidates = runCatching { JSONObject(body).optJSONArray("candidates") }.getOrNull() ?: return null
        val candidate = candidates.objects().firstOrNull() ?: return null
        val downloadUrl = httpUrl(
            base = "https://lyrics.kugou.com/download",
            query = mapOf(
                "ver" to "1",
                "client" to "pc",
                "id" to candidate.optString("id"),
                "accesskey" to candidate.optString("accesskey"),
                "fmt" to "lrc",
                "charset" to "utf8"
            )
        ) ?: return null
        val downloadBody = okHttpClient.getString(downloadUrl) ?: return null
        val content = runCatching { JSONObject(downloadBody).optString("content") }.getOrNull().orEmpty()
        val lyrics = runCatching {
            android.util.Base64.decode(content, android.util.Base64.DEFAULT).toString(Charsets.UTF_8)
        }.getOrDefault(content).trim()
        if (lyrics.isBlank()) return null
        return LyricsResult(
            provider = "KuGou",
            lyrics = lyrics,
            synced = lyrics.contains(Regex("\\[\\d{1,2}:\\d{2}")),
            score = match.score
        )
    }

    suspend fun findArtwork(song: Song): ArtworkResult? {
        val match = searchBestSong(song) ?: return null
        return fetchArtwork(match)
    }

    suspend fun collectCandidates(song: Song): Pair<LyricsResult?, ArtworkResult?> {
        val candidate = collectMetadataCandidate(song)
        return candidate?.lyrics to candidate?.artwork
    }

    suspend fun collectMetadataCandidate(song: Song): TrackMetadataResult? {
        val match = searchBestSong(song) ?: return null
        val lyrics = runCatching {
            val url = httpUrl(
                base = "https://lyrics.kugou.com/search",
                query = mapOf(
                    "ver" to "1", "man" to "yes", "client" to "pc",
                    "keyword" to listOfNotNull(song.artistName, cleanedTitle(song.title)).joinToString(" "),
                    "duration" to song.durationMs.takeIf { it > 0L }?.div(1000L)?.toString(),
                    "hash" to match.hash
                )
            ) ?: return@runCatching null
            val body = okHttpClient.getString(url) ?: return@runCatching null
            val candidates = JSONObject(body).optJSONArray("candidates") ?: return@runCatching null
            val candidate = candidates.objects().firstOrNull() ?: return@runCatching null
            val downloadUrl = httpUrl(
                base = "https://lyrics.kugou.com/download",
                query = mapOf("ver" to "1", "client" to "pc", "id" to candidate.optString("id"),
                    "accesskey" to candidate.optString("accesskey"), "fmt" to "lrc", "charset" to "utf8")
            ) ?: return@runCatching null
            val downloadBody = okHttpClient.getString(downloadUrl) ?: return@runCatching null
            val content = JSONObject(downloadBody).optString("content").orEmpty()
            val text = runCatching {
                android.util.Base64.decode(content, android.util.Base64.DEFAULT).toString(Charsets.UTF_8)
            }.getOrDefault(content).trim()
            if (text.isBlank()) null else LyricsResult(
                provider = "KuGou", lyrics = text,
                synced = text.contains(Regex("\\[\\d{1,2}:\\d{2}")), score = match.score
            )
        }.getOrNull()

        val albumData = fetchAlbumData(match)
        val artwork = albumData?.artwork

        return TrackMetadataResult(
            provider = "KuGou",
            title = match.title,
            artistName = match.artistName,
            albumTitle = albumData?.albumTitle ?: match.albumTitle,
            releaseYear = albumData?.releaseYear ?: match.releaseYear,
            releaseMonth = albumData?.releaseMonth ?: match.releaseMonth,
            lyrics = lyrics,
            artwork = artwork,
            score = maxOf(match.score, lyrics?.score ?: 0, artwork?.score ?: 0)
        )
    }

    private suspend fun fetchArtwork(match: KuGouSongMatch): ArtworkResult? {
        return fetchAlbumData(match)?.artwork
    }

    private suspend fun fetchAlbumData(match: KuGouSongMatch): KuGouAlbumData? {
        if (match.albumId == null) return null
        val url = httpUrl(
            base = "https://wwwapi.kugou.com/yy/index.php",
            query = mapOf("r" to "play/getdata", "hash" to match.hash, "album_id" to match.albumId)
        ) ?: return null
        val body = okHttpClient.getString(url) ?: return null
        val data = runCatching { JSONObject(body).optJSONObject("data") }.getOrNull() ?: return null
        val image = data.optString("album_img")
            .replace("{size}", "800")
            .takeIf { it.startsWith("http") }
        val releaseDate = parseReleaseDateParts(
            listOf("publish_date", "publishtime", "publish_time", "release_date")
                .firstNotNullOfOrNull { key -> data.optString(key).takeIf(String::isNotBlank) }
        )
        return KuGouAlbumData(
            artwork = image?.let { ArtworkResult("KuGou", it, match.score) },
            albumTitle = data.optString("album_name").takeIf { it.isNotBlank() },
            releaseYear = releaseDate.year,
            releaseMonth = releaseDate.month
        )
    }

    private suspend fun searchBestSong(song: Song): KuGouSongMatch? {
        val info = song.toSearchInfo()
        val matches = mutableListOf<KuGouSongMatch>()
        for (term in searchTerms(info)) {
            val url = httpUrl(
                base = "https://songsearch.kugou.com/song_search_v2",
                query = mapOf(
                    "keyword" to term,
                    "page" to "1",
                    "pagesize" to "12"
                )
            ) ?: continue
            val body = okHttpClient.getString(url) ?: continue
            val songs = runCatching {
                JSONObject(body).optJSONObject("data")?.optJSONArray("lists")
            }.getOrNull() ?: continue
            songs.objects()
                .mapNotNull { json ->
                    val hash = json.optString("FileHash").ifBlank { json.optString("FileHash".lowercase()) }
                        .takeIf { it.isNotBlank() } ?: return@mapNotNull null
                    val score = scoreCandidate(
                        candidateTitle = json.optString("SongName"),
                        candidateArtist = json.optString("SingerName"),
                        candidateAlbum = json.optString("AlbumName"),
                        info = info
                    )
                    val releaseDate = parseReleaseDateParts(
                        listOf("PublishDate", "PublishTime", "Publish", "publish_date")
                            .firstNotNullOfOrNull { key -> json.optString(key).takeIf(String::isNotBlank) }
                    )
                    KuGouSongMatch(
                        hash = hash,
                        albumId = json.optString("AlbumID").takeIf { it.isNotBlank() },
                        title = json.optString("SongName").takeIf { it.isNotBlank() },
                        artistName = json.optString("SingerName").takeIf { it.isNotBlank() },
                        albumTitle = json.optString("AlbumName").takeIf { it.isNotBlank() },
                        releaseYear = releaseDate.year,
                        releaseMonth = releaseDate.month,
                        score = score
                    )
                }
                .maxByOrNull { it.score }
                ?.let { matches += it }
        }
        return matches.maxByOrNull { it.score }
            ?.takeIf { it.score.acceptableCandidateScore(info) }
    }
}

private data class KuGouSongMatch(
    val hash: String,
    val albumId: String?,
    val title: String?,
    val artistName: String?,
    val albumTitle: String?,
    val releaseYear: Int?,
    val releaseMonth: Int?,
    val score: Int
)

private data class KuGouAlbumData(
    val artwork: ArtworkResult?,
    val albumTitle: String?,
    val releaseYear: Int?,
    val releaseMonth: Int?
)
