package com.musicplayer.app.ui.player

data class LrcLine(val timeMs: Long, val text: String)

fun parseLrc(lrcText: String): List<LrcLine> {
    val regex = Regex("""\[(\d{1,3}):(\d{2})(?:[.:](\d{1,3}))?\](.*)""")
    return lrcText.lines().mapNotNull { line ->
        regex.matchEntire(line.trim())?.let { match ->
            val min = match.groupValues[1].toIntOrNull() ?: return@mapNotNull null
            val sec = match.groupValues[2].toIntOrNull() ?: return@mapNotNull null
            val ms = match.groupValues[3].padEnd(3, '0').take(3).toIntOrNull() ?: 0
            val text = match.groupValues[4].trim()
            if (text.isNotEmpty()) LrcLine(min * 60_000L + sec * 1000L + ms, text) else null
        }
    }
}

fun findCurrentLrcIndex(lines: List<LrcLine>, positionMs: Long): Int {
    if (lines.isEmpty()) return -1
    var idx = lines.indexOfLast { it.timeMs <= positionMs }
    if (idx < 0 && lines.isNotEmpty()) idx = 0
    return idx
}
