package com.musicplayer.app.domain.usecase

import android.content.Context
import android.net.Uri
import com.musicplayer.app.data.local.dao.AlbumDao
import com.musicplayer.app.data.local.dao.ArtistDao
import com.musicplayer.app.data.local.dao.CommentDao
import com.musicplayer.app.data.local.dao.PlayRecordDao
import com.musicplayer.app.data.local.dao.PlaylistDao
import com.musicplayer.app.data.local.dao.SongDao
import com.musicplayer.app.data.local.entity.AlbumEntity
import com.musicplayer.app.data.local.entity.ArtistEntity
import com.musicplayer.app.data.local.entity.CommentEntity
import com.musicplayer.app.data.local.entity.PlayRecordEntity
import com.musicplayer.app.data.local.entity.PlaylistEntity
import com.musicplayer.app.data.local.entity.PlaylistSongEntity
import com.musicplayer.app.data.local.entity.SongEntity
import com.musicplayer.app.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class ExportImportUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val songDao: SongDao,
    private val albumDao: AlbumDao,
    private val artistDao: ArtistDao,
    private val commentDao: CommentDao,
    private val playRecordDao: PlayRecordDao,
    private val playlistDao: PlaylistDao,
    private val settingsRepository: SettingsRepository
) {
    suspend fun exportTo(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val json = JSONObject()

            json.put("songs", JSONArray().apply {
                songDao.getSongs().forEach { put(it.toJson()) }
            })
            json.put("albums", JSONArray().apply {
                albumDao.getAll().forEach { put(it.toJson()) }
            })
            json.put("artists", JSONArray().apply {
                artistDao.getAll().forEach { put(it.toJson()) }
            })
            json.put("comments", JSONArray().apply {
                commentDao.getAll().forEach { put(it.toJson()) }
            })
            json.put("playRecords", JSONArray().apply {
                playRecordDao.getAll().forEach { put(it.toJson()) }
            })
            json.put("playlists", JSONArray().apply {
                playlistDao.getAllPlaylists().forEach { put(it.toJson()) }
            })
            json.put("playlistSongs", JSONArray().apply {
                playlistDao.getAllPlaylistSongs().forEach { put(it.toJson()) }
            })
            json.put("settings", settingsRepository.exportSettings())

            context.contentResolver.openOutputStream(uri)?.use { out ->
                out.write(json.toString(2).toByteArray())
            }
            true
        }.getOrDefault(false)
    }

    suspend fun importFrom(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                ?: return@runCatching false
            val json = JSONObject(text)

            json.optJSONArray("songs")?.let { arr ->
                val songs = (0 until arr.length()).map { songFromJson(arr.getJSONObject(it)) }
                songDao.upsertAll(songs)
            }
            json.optJSONArray("albums")?.let { arr ->
                val albums = (0 until arr.length()).map { albumFromJson(arr.getJSONObject(it)) }
                albumDao.upsertAll(albums)
            }
            json.optJSONArray("artists")?.let { arr ->
                val artists = (0 until arr.length()).map { artistFromJson(arr.getJSONObject(it)) }
                artistDao.upsertAll(artists)
            }
            json.optJSONArray("comments")?.let { arr ->
                val comments = (0 until arr.length()).map { commentFromJson(arr.getJSONObject(it)) }
                commentDao.upsertAll(comments)
            }
            json.optJSONArray("playRecords")?.let { arr ->
                val records = (0 until arr.length()).map { playRecordFromJson(arr.getJSONObject(it)) }
                playRecordDao.upsertAll(records)
            }
            json.optJSONArray("playlists")?.let { arr ->
                val playlists = (0 until arr.length()).map { playlistFromJson(arr.getJSONObject(it)) }
                playlistDao.upsertAllPlaylists(playlists)
            }
            json.optJSONArray("playlistSongs")?.let { arr ->
                val entries = (0 until arr.length()).map { playlistSongFromJson(arr.getJSONObject(it)) }
                playlistDao.upsertAllPlaylistSongs(entries)
            }
            json.optString("settings")?.takeIf { it.isNotBlank() }?.let {
                settingsRepository.importSettings(it)
            }
            true
        }.getOrDefault(false)
    }
}

// JSON serialization helpers for Room entities
private fun SongEntity.toJson(): JSONObject = JSONObject().apply {
    put("id", id); put("title", title); put("artistId", artistId ?: "")
    put("artistName", artistName); put("albumId", albumId ?: ""); put("albumTitle", albumTitle)
    put("durationMs", durationMs); put("trackNumber", trackNumber ?: 0); put("discNumber", discNumber ?: 0)
    put("uri", uri); put("source", source.name); put("webDavServerId", webDavServerId ?: "")
    put("artworkUri", artworkUri ?: ""); put("artworkProvider", artworkProvider ?: "")
    put("lyrics", lyrics ?: ""); put("lyricsProvider", lyricsProvider ?: "")
    put("mimeType", mimeType ?: ""); put("bitrateKbps", bitrateKbps ?: 0)
    put("sampleRateHz", sampleRateHz ?: 0); put("releaseYear", releaseYear ?: 0)
    put("releaseMonth", releaseMonth ?: 0); put("dateAddedEpochMs", dateAddedEpochMs)
    put("categoryTag", categoryTag ?: "")
    put("semanticTags", semanticTags ?: "")
    put("isFavorite", isFavorite)
}

private fun songFromJson(j: JSONObject): SongEntity = SongEntity(
    id = j.getString("id"), title = j.getString("title"),
    artistId = j.optString("artistId").takeIf { it.isNotBlank() },
    artistName = j.getString("artistName"),
    albumId = j.optString("albumId").takeIf { it.isNotBlank() },
    albumTitle = j.getString("albumTitle"),
    durationMs = j.getLong("durationMs"),
    trackNumber = j.optInt("trackNumber").takeIf { it > 0 },
    discNumber = j.optInt("discNumber").takeIf { it > 0 },
    uri = j.getString("uri"),
    source = com.musicplayer.app.domain.model.Source.valueOf(j.getString("source")),
    webDavServerId = j.optString("webDavServerId").takeIf { it.isNotBlank() },
    artworkUri = j.optString("artworkUri").takeIf { it.isNotBlank() },
    artworkProvider = j.optString("artworkProvider").takeIf { it.isNotBlank() },
    lyrics = j.optString("lyrics").takeIf { it.isNotBlank() },
    lyricsProvider = j.optString("lyricsProvider").takeIf { it.isNotBlank() },
    mimeType = j.optString("mimeType").takeIf { it.isNotBlank() },
    bitrateKbps = j.optInt("bitrateKbps").takeIf { it > 0 },
    sampleRateHz = j.optInt("sampleRateHz").takeIf { it > 0 },
    releaseYear = j.optInt("releaseYear").takeIf { it > 0 },
    releaseMonth = j.optInt("releaseMonth").takeIf { it > 0 },
    categoryTag = j.optString("categoryTag").takeIf { it.isNotBlank() },
    semanticTags = j.optString("semanticTags").takeIf { it.isNotBlank() },
    dateAddedEpochMs = j.getLong("dateAddedEpochMs"),
    isFavorite = j.getBoolean("isFavorite")
)

private fun AlbumEntity.toJson(): JSONObject = JSONObject().apply {
    put("id", id); put("title", title); put("artistName", artistName)
    put("artworkUri", artworkUri ?: ""); put("songCount", songCount)
    put("releaseYear", releaseYear ?: 0); put("releaseMonth", releaseMonth ?: 0)
    put("source", source.name)
}

private fun albumFromJson(j: JSONObject): AlbumEntity = AlbumEntity(
    id = j.getString("id"), title = j.getString("title"),
    artistName = j.getString("artistName"),
    artworkUri = j.optString("artworkUri").takeIf { it.isNotBlank() },
    songCount = j.getInt("songCount"),
    releaseYear = j.optInt("releaseYear").takeIf { it > 0 },
    releaseMonth = j.optInt("releaseMonth").takeIf { it > 0 },
    source = com.musicplayer.app.domain.model.Source.valueOf(j.getString("source"))
)

private fun ArtistEntity.toJson(): JSONObject = JSONObject().apply {
    put("id", id); put("name", name); put("albumCount", albumCount)
    put("songCount", songCount); put("artworkUri", artworkUri ?: "")
}

private fun artistFromJson(j: JSONObject): ArtistEntity = ArtistEntity(
    id = j.getString("id"), name = j.getString("name"),
    albumCount = j.getInt("albumCount"), songCount = j.getInt("songCount"),
    artworkUri = j.optString("artworkUri").takeIf { it.isNotBlank() }
)

private fun CommentEntity.toJson(): JSONObject = JSONObject().apply {
    put("id", id); put("songId", songId); put("rating", rating)
    put("text", text); put("timestampEpochMs", timestampEpochMs)
}

private fun commentFromJson(j: JSONObject): CommentEntity = CommentEntity(
    id = j.getString("id"), songId = j.getString("songId"),
    rating = j.getInt("rating"), text = j.getString("text"),
    timestampEpochMs = j.getLong("timestampEpochMs")
)

private fun PlayRecordEntity.toJson(): JSONObject = JSONObject().apply {
    put("id", id); put("songId", songId)
    put("listenedAtEpochMs", listenedAtEpochMs); put("durationMs", durationMs)
}

private fun playRecordFromJson(j: JSONObject): PlayRecordEntity = PlayRecordEntity(
    id = j.getString("id"), songId = j.getString("songId"),
    listenedAtEpochMs = j.getLong("listenedAtEpochMs"),
    durationMs = j.getLong("durationMs")
)

private fun PlaylistEntity.toJson(): JSONObject = JSONObject().apply {
    put("id", id); put("name", name); put("description", description ?: "")
    put("artworkUri", artworkUri ?: ""); put("songCount", songCount)
    put("createdAtEpochMs", createdAtEpochMs); put("updatedAtEpochMs", updatedAtEpochMs)
    put("sortOrder", sortOrder)
}

private fun playlistFromJson(j: JSONObject): PlaylistEntity = PlaylistEntity(
    id = j.getString("id"), name = j.getString("name"),
    description = j.optString("description").takeIf { it.isNotBlank() },
    artworkUri = j.optString("artworkUri").takeIf { it.isNotBlank() },
    songCount = j.getInt("songCount"),
    createdAtEpochMs = j.getLong("createdAtEpochMs"),
    updatedAtEpochMs = j.getLong("updatedAtEpochMs"),
    sortOrder = j.optInt("sortOrder")
)

private fun PlaylistSongEntity.toJson(): JSONObject = JSONObject().apply {
    put("playlistId", playlistId); put("songId", songId)
    put("position", position); put("addedAtEpochMs", addedAtEpochMs)
}

private fun playlistSongFromJson(j: JSONObject): PlaylistSongEntity = PlaylistSongEntity(
    playlistId = j.getString("playlistId"), songId = j.getString("songId"),
    position = j.getInt("position"), addedAtEpochMs = j.getLong("addedAtEpochMs")
)
