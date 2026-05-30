package com.musicplayer.app.data.scraper

import com.musicplayer.app.domain.model.Song
import okhttp3.OkHttpClient
import javax.inject.Inject

class MegalobizScraper @Inject constructor(
    private val okHttpClient: OkHttpClient
) {
    suspend fun findLyrics(song: Song): LyricsResult? {
        return collectMetadataCandidate(song)?.lyrics
    }

    suspend fun collectMetadataCandidate(song: Song): TrackMetadataResult? {
        val info = song.toSearchInfo()
        val term = listOfNotNull(info.artist, cleanedTitle(info.title)).joinToString(" ").ifBlank { info.title }
        val searchUrl = "https://www.megalobiz.com/search/all?q=${encoded(term)}"
        val html = okHttpClient.getString(searchUrl) ?: return null
        val resultUrl = Regex("""https://www\.megalobiz\.com/lrc/maker/[^\s"'<>]+?\.html|/lrc/maker/[^\s"'<>]+?\.html""")
            .findAll(html)
            .map { it.value }
            .map { if (it.startsWith("http")) it else "https://www.megalobiz.com$it" }
            .distinct()
            .firstOrNull() ?: return null
        val lyricsHtml = okHttpClient.getString(resultUrl) ?: return null
        val lyrics = extractLyrics(lyricsHtml) ?: return null
        val lyricsResult = LyricsResult(
            provider = "Megalobiz",
            lyrics = lyrics,
            synced = lyrics.contains(Regex("\\[\\d{1,2}:\\d{2}")),
            score = 9
        )
        return TrackMetadataResult(
            provider = "Megalobiz",
            title = cleanedTitle(info.title).takeIf { it.isNotBlank() },
            artistName = info.artist,
            lyrics = lyricsResult,
            score = lyricsResult.score
        )
    }

    private fun extractLyrics(html: String): String? {
        val patterns = listOf(
            Regex("""entity_decode\('((?:\\.|[^'])*)'\)""", RegexOption.DOT_MATCHES_ALL),
            Regex("""entity_decode\("((?:\\.|[^"])*)"\)""", RegexOption.DOT_MATCHES_ALL),
            Regex("""<textarea[^>]*>(.*?)</textarea>""", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
        )
        return patterns.firstNotNullOfOrNull { pattern ->
            val raw = pattern.find(html)?.groupValues?.getOrNull(1) ?: return@firstNotNullOfOrNull null
            raw.replace("\\n", "\n")
                .replace("\\r", "\n")
                .replace("&amp;", "&")
                .replace("&quot;", "\"")
                .replace("&#039;", "'")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .trim()
                .takeIf { it.isNotBlank() }
        }
    }
}
