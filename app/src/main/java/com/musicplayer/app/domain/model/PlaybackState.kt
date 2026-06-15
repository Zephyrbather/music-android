package com.musicplayer.app.domain.model

data class PlaybackState(
    val currentSong: Song? = null,
    val queue: List<Song> = emptyList(),
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = currentSong?.durationMs ?: 0L,
    val errorMessage: String? = null,
    val playbackMode: PlaybackMode = PlaybackMode.SEQUENTIAL,
    val repeatMode: RepeatMode = RepeatMode.OFF
)

enum class PlaybackMode {
    SEQUENTIAL,
    SHUFFLE,
    RECOMMENDATION
}

enum class RepeatMode {
    OFF,
    ONE,
    ALL
}
