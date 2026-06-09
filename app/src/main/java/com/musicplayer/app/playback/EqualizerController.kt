package com.musicplayer.app.playback

import android.media.audiofx.Equalizer
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EqualizerController @Inject constructor() {
    private var equalizer: Equalizer? = null
    private var lastLevels: List<Short> = emptyList()

    fun attach(audioSessionId: Int) {
        release()
        equalizer = runCatching {
            Equalizer(0, audioSessionId).apply { enabled = true }
        }.getOrNull()
        if (lastLevels.isNotEmpty()) {
            setBandLevels(lastLevels)
        }
    }

    val bandFrequencies: List<Int>
        get() {
            val eq = equalizer ?: return listOf(30, 60, 120, 250, 500, 1000, 2000, 4000, 8000, 16000)
            return (0 until eq.numberOfBands).map { eq.getCenterFreq(it.toShort()) / 1_000_000 }
        }

    val numberOfBands: Int
        get() = equalizer?.numberOfBands?.toInt() ?: 10

    fun setBandLevels(levels: List<Short>) {
        lastLevels = levels
        val eq = equalizer ?: return
        levels.forEachIndexed { index, level ->
            if (index < eq.numberOfBands) {
                eq.setBandLevel(index.toShort(), level)
            }
        }
    }

    fun setBandLevel(band: Short, level: Short) {
        lastLevels = lastLevels.toMutableList().also { levels ->
            val index = band.toInt()
            while (levels.size <= index) levels.add(0.toShort())
            levels[index] = level
        }
        equalizer?.setBandLevel(band, level)
    }

    fun release() {
        equalizer?.release()
        equalizer = null
    }
}
