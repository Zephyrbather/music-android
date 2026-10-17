package com.musicplayer.app.data.karaoke

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class KaraokeRecorder(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null

    val isRecording: Boolean
        get() = recorder != null

    fun start(title: String, artistName: String): File? {
        stop()
        val directory = karaokeDirectory(context)
        if (!directory.exists() && !directory.mkdirs()) return null
        val file = File(directory, buildFileName(title, artistName))
        val created = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }
        return try {
            created.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128_000)
                setAudioSamplingRate(44_100)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
            recorder = created
            outputFile = file
            file
        } catch (error: Exception) {
            runCatching { created.release() }
            file.delete()
            null
        }
    }

    fun amplitude(): Float {
        val value = runCatching { recorder?.maxAmplitude ?: 0 }.getOrDefault(0)
        return (value / MAX_AMPLITUDE).coerceIn(0f, 1f)
    }

    fun stop(): File? {
        val current = recorder ?: return null
        recorder = null
        runCatching { current.stop() }
        runCatching { current.release() }
        val file = outputFile?.takeIf { it.length() > 0L }
        outputFile = null
        return file
    }

    private fun buildFileName(title: String, artistName: String): String {
        val label = listOf(title, artistName)
            .joinToString("_")
            .replace(Regex("[\\\\/:*?\"<>|\\s]+"), "_")
            .trim('_')
            .take(48)
            .ifBlank { "karaoke" }
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        return "${label}_$stamp.m4a"
    }

    private companion object {
        const val MAX_AMPLITUDE = 32767f
    }
}
