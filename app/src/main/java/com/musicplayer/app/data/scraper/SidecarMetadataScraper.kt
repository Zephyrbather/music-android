package com.musicplayer.app.data.scraper

import android.net.Uri
import com.musicplayer.app.domain.model.Song
import java.io.File
import javax.inject.Inject

class SidecarMetadataScraper @Inject constructor() {
    suspend fun enrich(song: Song): Song {
        val uri = Uri.parse(song.uri)
        if (uri.scheme != "file") return song
        val audioFile = File(uri.path ?: return song)
        val base = audioFile.resolveSibling(audioFile.nameWithoutExtension)

        val lyricsFile = listOf("lrc", "txt")
            .map { File("${base.path}.$it") }
            .firstOrNull { it.isFile && it.canRead() }
        val artworkFile = listOf("jpg", "jpeg", "png", "webp")
            .map { File("${base.path}.$it") }
            .firstOrNull { it.isFile && it.canRead() }

        return song.copy(
            lyrics = song.lyrics ?: lyricsFile?.readText(),
            lyricsProvider = song.lyricsProvider ?: lyricsFile?.extension?.uppercase()?.let { "Sidecar $it" },
            artworkUri = song.artworkUri ?: artworkFile?.toURI()?.toString(),
            artworkProvider = song.artworkProvider ?: artworkFile?.extension?.uppercase()?.let { "Sidecar $it" }
        )
    }
}
