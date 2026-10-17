package com.musicplayer.app.data.karaoke

import android.content.Context
import android.os.Environment
import java.io.File
import org.json.JSONArray
import org.json.JSONObject

data class KaraokeRecording(
    val fileName: String,
    val title: String,
    val artist: String,
    val createdAt: Long,
    val durationMs: Long,
    val score: Int?
) {
    fun file(context: Context): File = File(karaokeDirectory(context), fileName)
}

fun karaokeDirectory(context: Context): File {
    val base = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC) ?: context.filesDir
    return File(base, "Karaoke")
}

object KaraokeRecordingStore {
    private const val PREFS_NAME = "karaoke_recordings"
    private const val KEY_ITEMS = "items"

    fun load(context: Context): List<KaraokeRecording> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_ITEMS, null) ?: return emptyList()
        val array = runCatching { JSONArray(raw) }.getOrNull() ?: return emptyList()
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val fileName = item.optString("fileName")
                if (fileName.isBlank()) continue
                if (!File(karaokeDirectory(context), fileName).exists()) continue
                add(
                    KaraokeRecording(
                        fileName = fileName,
                        title = item.optString("title"),
                        artist = item.optString("artist"),
                        createdAt = item.optLong("createdAt"),
                        durationMs = item.optLong("durationMs"),
                        score = if (item.isNull("score")) null else item.optInt("score")
                    )
                )
            }
        }.sortedByDescending { it.createdAt }
    }

    fun add(context: Context, recording: KaraokeRecording) {
        val updated = (listOf(recording) + load(context).filterNot { it.fileName == recording.fileName })
            .sortedByDescending { it.createdAt }
        persist(context, updated)
    }

    fun remove(context: Context, fileName: String) {
        File(karaokeDirectory(context), fileName).delete()
        persist(context, load(context).filterNot { it.fileName == fileName })
    }

    private fun persist(context: Context, recordings: List<KaraokeRecording>) {
        val array = JSONArray()
        recordings.forEach { recording ->
            array.put(
                JSONObject()
                    .put("fileName", recording.fileName)
                    .put("title", recording.title)
                    .put("artist", recording.artist)
                    .put("createdAt", recording.createdAt)
                    .put("durationMs", recording.durationMs)
                    .put("score", recording.score ?: JSONObject.NULL)
            )
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_ITEMS, array.toString())
            .apply()
    }
}
