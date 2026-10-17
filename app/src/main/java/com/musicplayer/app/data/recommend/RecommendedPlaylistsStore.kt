package com.musicplayer.app.data.recommend

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

enum class RecommendedPlaylistKind { FOR_YOU, RECENT, TAG, NEW }

data class RecommendedPlaylist(
    val kind: RecommendedPlaylistKind,
    val tag: String? = null,
    val songIds: List<String> = emptyList(),
    val updatedAtMs: Long = 0L
)

object RecommendedPlaylistsStore {
    private const val PREFS_NAME = "recommended_playlists"
    private const val KEY_ITEMS = "items"

    fun load(context: Context): List<RecommendedPlaylist> {
        val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_ITEMS, null) ?: return emptyList()
        val array = runCatching { JSONArray(raw) }.getOrNull() ?: return emptyList()
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val kind = runCatching {
                    RecommendedPlaylistKind.valueOf(item.optString("kind"))
                }.getOrNull() ?: continue
                val ids = item.optJSONArray("songIds") ?: JSONArray()
                val songIds = buildList {
                    for (idIndex in 0 until ids.length()) {
                        val id = ids.optString(idIndex)
                        if (id.isNotBlank()) add(id)
                    }
                }
                if (songIds.isEmpty()) continue
                add(
                    RecommendedPlaylist(
                        kind = kind,
                        tag = item.optString("tag").takeIf { it.isNotBlank() },
                        songIds = songIds,
                        updatedAtMs = item.optLong("updatedAtMs")
                    )
                )
            }
        }
    }

    fun save(context: Context, playlists: List<RecommendedPlaylist>) {
        val array = JSONArray()
        playlists.forEach { playlist ->
            array.put(
                JSONObject()
                    .put("kind", playlist.kind.name)
                    .put("tag", playlist.tag ?: JSONObject.NULL)
                    .put("songIds", JSONArray(playlist.songIds))
                    .put("updatedAtMs", playlist.updatedAtMs)
            )
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_ITEMS, array.toString())
            .apply()
    }
}
