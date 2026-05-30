package com.musicplayer.app.data.scraper

import com.musicplayer.app.domain.model.Song
import javax.inject.Inject
import okhttp3.OkHttpClient
import org.json.JSONObject

class MiguScraper @Inject constructor(
    private val okHttpClient: OkHttpClient
) {
    suspend fun findLyrics(song: Song): LyricsResult? {
        return collectMetadataCandidate(song)?.lyrics
    }

    suspend fun findArtwork(song: Song): ArtworkResult? {
        return collectMetadataCandidate(song)?.artwork
    }

    suspend fun collectMetadataCandidate(song: Song): TrackMetadataResult? {
        val match = searchBestSong(song) ?: return null
        val lyrics = fetchLyrics(match)
        val artwork = match.artworkUri?.let { ArtworkResult(PROVIDER, it, match.score) }
        return TrackMetadataResult(
            provider = PROVIDER,
            title = match.title,
            artistName = match.artistName,
            albumTitle = match.albumTitle,
            lyrics = lyrics,
            artwork = artwork,
            score = maxOf(match.score, lyrics?.score ?: 0, artwork?.score ?: 0)
        )
    }

    private suspend fun searchBestSong(song: Song): MiguSongMatch? {
        val info = song.toSearchInfo()
        val matches = mutableListOf<MiguSongMatch>()
        for (term in searchTerms(info)) {
            val url = httpUrl(
                base = "https://m.music.migu.cn/migu/remoting/scr_search_tag",
                query = mapOf(
                    "keyword" to term,
                    "type" to "2",
                    "rows" to "12",
                    "pgc" to "1"
                )
            ) ?: continue
            val body = okHttpClient.getString(url) ?: continue
            val rows = runCatching { JSONObject(body).optJSONArray("musics") }.getOrNull()
                ?: continue
            rows.objects()
                .mapNotNull { json ->
                    val title = json.optString("songName").takeIf { it.isNotBlank() }
                    val artist = json.optString("singerName").takeIf { it.isNotBlank() }
                    val album = json.optString("albumName").takeIf { it.isNotBlank() }
                    val copyrightId = json.optString("copyrightId").takeIf { it.isNotBlank() }
                        ?: json.optString("id").takeIf { it.isNotBlank() }
                    val score = scoreCandidate(
                        candidateTitle = title,
                        candidateArtist = artist,
                        candidateAlbum = album,
                        info = info
                    )
                    MiguSongMatch(
                        id = copyrightId,
                        title = title,
                        artistName = artist,
                        albumTitle = album,
                        artworkUri = json.optString("cover")
                            .ifBlank { json.optString("albumPicL") }
                            .ifBlank { json.optString("albumPicM") }
                            .normalizeMiguImage()
                            ?.takeIf { it.startsWith("http") },
                        score = score
                    )
                }
                .maxByOrNull { it.score }
                ?.let { matches += it }
        }
        return matches.maxByOrNull { it.score }
            ?.takeIf { it.score.acceptableCandidateScore(info) }
    }

    private suspend fun fetchLyrics(match: MiguSongMatch): LyricsResult? {
        val id = match.id ?: return null
        val urls = listOfNotNull(
            httpUrl(
                base = "https://music.migu.cn/v3/api/music/audioPlayer/getLyric",
                query = mapOf("copyrightId" to id)
            ),
            httpUrl(
                base = "https://m.music.migu.cn/migu/remoting/cms_detail_tag",
                query = mapOf("cpid" to id)
            )
        )
        return urls.firstNotNullOfOrNull { url ->
            val body = okHttpClient.getString(url) ?: return@firstNotNullOfOrNull null
            val json = runCatching { JSONObject(body) }.getOrNull() ?: return@firstNotNullOfOrNull null
            val lyrics = listOf("lyric", "lyrics", "lrc")
                .firstNotNullOfOrNull { key -> json.optString(key).takeIf { it.isNotBlank() } }
                ?: json.optJSONObject("data")?.let { data ->
                    listOf("lyric", "lyrics", "lrc")
                        .firstNotNullOfOrNull { key -> data.optString(key).takeIf { it.isNotBlank() } }
                }
                ?: return@firstNotNullOfOrNull null
            LyricsResult(
                provider = PROVIDER,
                lyrics = lyrics.trim(),
                synced = lyrics.contains(Regex("\\[\\d{1,2}:\\d{2}")),
                score = match.score
            )
        }
    }

    private companion object {
        const val PROVIDER = "Migu Music"
    }
}

private data class MiguSongMatch(
    val id: String?,
    val title: String?,
    val artistName: String?,
    val albumTitle: String?,
    val artworkUri: String?,
    val score: Int
)

private fun String.normalizeMiguImage(): String? {
    val value = takeIf { it.isNotBlank() } ?: return null
    val full = if (value.startsWith("//")) "https:$value" else value
    return full.replace("http://", "https://")
}
