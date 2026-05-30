package com.musicplayer.app.data.scraper

import com.musicplayer.app.domain.model.Song
import javax.inject.Inject
import okhttp3.OkHttpClient
import org.json.JSONObject

class KuwoScraper @Inject constructor(
    private val okHttpClient: OkHttpClient
) {
    suspend fun findLyrics(song: Song): LyricsResult? {
        val match = searchBestSong(song) ?: return null
        val url = httpUrl(
            base = "https://m.kuwo.cn/newh5/singles/songinfoandlrc",
            query = mapOf("musicId" to match.id)
        ) ?: return null
        val body = okHttpClient.getString(url) ?: return null
        val lrcRows = runCatching {
            JSONObject(body).optJSONObject("data")?.optJSONArray("lrclist")
        }.getOrNull() ?: return null
        val lyrics = lrcRows.objects()
            .mapNotNull { row ->
                val line = row.optString("lineLyric").trim()
                if (line.isBlank()) null else {
                    val time = row.optString("time").takeIf { it.isNotBlank() }?.toLrcTimeTag().orEmpty()
                    "$time$line"
                }
            }
            .joinToString("\n")
            .trim()
        if (lyrics.isBlank()) return null
        return LyricsResult(
            provider = "Kuwo",
            lyrics = lyrics,
            synced = lyrics.contains(Regex("\\[\\d{1,2}:\\d{2}")),
            score = match.score
        )
    }

    suspend fun findArtwork(song: Song): ArtworkResult? {
        val match = searchBestSong(song) ?: return null
        return match.artworkUri?.let { ArtworkResult("Kuwo", it, match.score) }
    }

    suspend fun collectCandidates(song: Song): Pair<LyricsResult?, ArtworkResult?> {
        val candidate = collectMetadataCandidate(song)
        return candidate?.lyrics to candidate?.artwork
    }

    suspend fun collectMetadataCandidate(song: Song): TrackMetadataResult? {
        val match = searchBestSong(song) ?: return null
        val lyrics = runCatching {
            val url = httpUrl(
                base = "https://m.kuwo.cn/newh5/singles/songinfoandlrc",
                query = mapOf("musicId" to match.id)
            ) ?: return@runCatching null
            val body = okHttpClient.getString(url) ?: return@runCatching null
            val lrcRows = JSONObject(body).optJSONObject("data")?.optJSONArray("lrclist") ?: return@runCatching null
            val text = lrcRows.objects()
                .mapNotNull { row ->
                    val line = row.optString("lineLyric").trim()
                    if (line.isBlank()) null else {
                        val time = row.optString("time").takeIf { it.isNotBlank() }?.toLrcTimeTag().orEmpty()
                        "$time$line"
                    }
                }
                .joinToString("\n").trim()
            if (text.isBlank()) null else LyricsResult(
                provider = "Kuwo", lyrics = text,
                synced = text.contains(Regex("\\[\\d{1,2}:\\d{2}")), score = match.score
            )
        }.getOrNull()

        val artwork = match.artworkUri?.let { ArtworkResult("Kuwo", it, match.score) }

        return TrackMetadataResult(
            provider = "Kuwo",
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

    private suspend fun searchBestSong(song: Song): KuwoSongMatch? {
        val info = song.toSearchInfo()
        val matches = mutableListOf<KuwoSongMatch>()
        for (term in searchTerms(info)) {
            val url = httpUrl(
                base = "https://search.kuwo.cn/r.s",
                query = mapOf(
                    "all" to term,
                    "ft" to "music",
                    "itemset" to "web_2013",
                    "client" to "kt",
                    "pn" to "0",
                    "rn" to "12",
                    "rformat" to "json",
                    "encoding" to "utf8"
                )
            ) ?: continue
            val body = okHttpClient.getString(url) ?: continue
            val list = runCatching { JSONObject(body).optJSONArray("abslist") }.getOrNull()
                ?: continue
            list.objects()
                .mapNotNull { json ->
                    val id = json.optString("MUSICRID").substringAfter("MUSIC_")
                        .ifBlank { json.optString("DC_TARGETID") }
                        .takeIf { it.isNotBlank() } ?: return@mapNotNull null
                    val score = scoreCandidate(
                        candidateTitle = json.optString("SONGNAME"),
                        candidateArtist = json.optString("ARTIST"),
                        candidateAlbum = json.optString("ALBUM"),
                        info = info
                    )
                    val releaseDate = parseReleaseDateParts(
                        listOf("RELEASEDATE", "RELEASETIME", "PUBLISHDATE", "PUBTIME")
                            .firstNotNullOfOrNull { key -> json.optString(key).takeIf(String::isNotBlank) }
                    )
                    KuwoSongMatch(
                        id = id,
                        title = json.optString("SONGNAME").takeIf { it.isNotBlank() },
                        artistName = json.optString("ARTIST").takeIf { it.isNotBlank() },
                        albumTitle = json.optString("ALBUM").takeIf { it.isNotBlank() },
                        releaseYear = releaseDate.year,
                        releaseMonth = releaseDate.month,
                        artworkUri = json.optString("web_albumpic_short")
                            .takeIf { it.isNotBlank() }
                            ?.let { if (it.startsWith("http")) it else "https://img4.kuwo.cn/star/albumcover/$it" },
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

private data class KuwoSongMatch(
    val id: String,
    val title: String?,
    val artistName: String?,
    val albumTitle: String?,
    val releaseYear: Int?,
    val releaseMonth: Int?,
    val artworkUri: String?,
    val score: Int
)

private fun String.toLrcTimeTag(): String {
    val seconds = toFloatOrNull() ?: return ""
    val minutes = seconds.toInt() / 60
    val remainSeconds = seconds - minutes * 60
    return "[%02d:%05.2f]".format(minutes, remainSeconds)
}
