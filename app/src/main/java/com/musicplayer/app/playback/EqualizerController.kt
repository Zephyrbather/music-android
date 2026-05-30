package com.musicplayer.app.playback

import android.media.audiofx.Equalizer
import javax.inject.Inject

class EqualizerController @Inject constructor() {
    private var equalizer: Equalizer? = null

    fun attach(audioSessionId: Int) {
        release()
        equalizer = runCatching {
            Equalizer(0, audioSessionId).apply { enabled = true }
        }.getOrNull()
    }

    val bandFrequencies: List<Int>
        get() {
            val eq = equalizer ?: return listOf(30, 60, 120, 250, 500, 1000, 2000, 4000, 8000, 16000)
            return (0 until eq.numberOfBands).map { eq.getCenterFreq(it.toShort()) / 1_000_000 }
        }

    val numberOfBands: Int
        get() = equalizer?.numberOfBands?.toInt() ?: 10

    fun setBandLevels(levels: List<Short>) {
        val eq = equalizer ?: return
        levels.forEachIndexed { index, level ->
            if (index < eq.numberOfBands) {
                eq.setBandLevel(index.toShort(), level)
            }
        }
    }

    fun setBandLevel(band: Short, level: Short) {
        equalizer?.setBandLevel(band, level)
    }

    fun release() {
        equalizer?.release()
        equalizer = null
    }
}
