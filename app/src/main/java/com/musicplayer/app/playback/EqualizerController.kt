package com.musicplayer.app.playback

import android.media.audiofx.Equalizer
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.log10

@Singleton
class EqualizerController @Inject constructor() {
    companion object {
        val uiFrequencies: List<Int> = listOf(30, 60, 120, 250, 500, 1000, 2000, 4000, 8000, 16000)
        private const val RAMP_STEP_MS = 16L
        // Positive boost is capped so stacked bands keep ~3 dB of headroom.
        private const val MAX_BOOST_MB = 300
    }

    private val handler = Handler(Looper.getMainLooper())
    private var equalizer: Equalizer? = null
    private var lastLevels: List<Short> = List(uiFrequencies.size) { 0 }
    private var appliedLevels: List<Short> = emptyList()
    private var rampRunnable: Runnable? = null

    fun attach(audioSessionId: Int) {
        release()
        equalizer = runCatching {
            Equalizer(0, audioSessionId).apply { enabled = true }
        }.getOrNull()
        val eq = equalizer ?: return
        applyDeviceLevels(mapToDeviceBands(lastLevels, eq))
    }

    val bandFrequencies: List<Int>
        get() = uiFrequencies

    val numberOfBands: Int
        get() = uiFrequencies.size

    fun setBandLevels(levels: List<Short>, rampMs: Long = 0L) {
        lastLevels = List(uiFrequencies.size) { index -> levels.getOrElse(index) { 0 } }
        val eq = equalizer ?: return
        val target = mapToDeviceBands(lastLevels, eq)
        if (rampMs > 0L) {
            rampTo(target, rampMs)
        } else {
            cancelRamp()
            applyDeviceLevels(target)
        }
    }

    fun setBandLevel(band: Short, level: Short) {
        cancelRamp()
        val index = band.toInt()
        lastLevels = lastLevels.toMutableList().also { levels ->
            while (levels.size <= index) levels.add(0)
            levels[index] = level
        }
        equalizer?.let { applyDeviceLevels(mapToDeviceBands(lastLevels, it)) }
    }

    fun release() {
        cancelRamp()
        equalizer?.release()
        equalizer = null
        appliedLevels = emptyList()
    }

    private fun applyDeviceLevels(levels: List<Short>) {
        val eq = equalizer ?: return
        levels.forEachIndexed { index, level ->
            eq.setBandLevel(index.toShort(), level)
        }
        appliedLevels = levels
    }

    private fun rampTo(target: List<Short>, durationMs: Long) {
        cancelRamp()
        val eq = equalizer ?: return
        val start = if (appliedLevels.size == target.size) appliedLevels.toList() else target
        if (start == target) {
            applyDeviceLevels(target)
            return
        }
        val startedAt = SystemClock.uptimeMillis()
        val runnable = object : Runnable {
            override fun run() {
                val activeEq = equalizer ?: return
                val progress = ((SystemClock.uptimeMillis() - startedAt).toFloat() / durationMs)
                    .coerceIn(0f, 1f)
                val eased = 1f - (1f - progress) * (1f - progress)
                val step = List(target.size) { index ->
                    (start[index] + (target[index] - start[index]) * eased).toInt().toShort()
                }
                step.forEachIndexed { index, level ->
                    activeEq.setBandLevel(index.toShort(), level)
                }
                appliedLevels = step
                if (progress < 1f) {
                    handler.postDelayed(this, RAMP_STEP_MS)
                } else {
                    rampRunnable = null
                }
            }
        }
        rampRunnable = runnable
        handler.post(runnable)
    }

    private fun cancelRamp() {
        rampRunnable?.let { handler.removeCallbacks(it) }
        rampRunnable = null
    }

    private fun mapToDeviceBands(levels: List<Short>, eq: Equalizer): List<Short> {
        val count = eq.numberOfBands.toInt()
        val frequencies = (0 until count).map { eq.getCenterFreq(it.toShort()) / 1_000_000 }
        val mapped = if (frequencies == uiFrequencies) {
            levels
        } else {
            frequencies.map { frequency -> interpolateLevel(levels, frequency) }
        }
        return applyHeadroom(mapped)
    }

    private fun interpolateLevel(levels: List<Short>, frequency: Int): Short {
        if (frequency <= uiFrequencies.first()) return levels.first()
        if (frequency >= uiFrequencies.last()) return levels.last()
        var upper = uiFrequencies.indexOfFirst { it >= frequency }
        if (upper <= 0) upper = 1
        val lowFrequency = uiFrequencies[upper - 1]
        val highFrequency = uiFrequencies[upper]
        val ratio = ((log10(frequency.toDouble()) - log10(lowFrequency.toDouble())) /
            (log10(highFrequency.toDouble()) - log10(lowFrequency.toDouble()))).toFloat()
        val lowLevel = levels[upper - 1]
        val highLevel = levels[upper]
        return (lowLevel + (highLevel - lowLevel) * ratio).toInt().toShort()
    }

    private fun applyHeadroom(levels: List<Short>): List<Short> {
        val peak = levels.maxOrNull() ?: return levels
        if (peak <= MAX_BOOST_MB) return levels
        val trim = (peak - MAX_BOOST_MB).toShort()
        return levels.map { (it - trim).toShort() }
    }
}
