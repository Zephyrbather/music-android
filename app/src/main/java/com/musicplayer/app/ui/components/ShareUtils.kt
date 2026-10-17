package com.musicplayer.app.ui.components

import android.content.Context
import android.content.Intent
import com.musicplayer.app.domain.model.Song

fun shareSongs(context: Context, title: String, songs: List<Song>) {
    val text = buildString {
        append(title)
        append("\n\n")
        songs.forEachIndexed { index, song ->
            append(index + 1).append(". ").append(song.title)
            if (song.artistName.isNotBlank()) append(" - ").append(song.artistName)
            append("\n")
        }
    }
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, title)
        putExtra(Intent.EXTRA_TEXT, text.trimEnd())
    }
    context.startActivity(Intent.createChooser(intent, title))
}
