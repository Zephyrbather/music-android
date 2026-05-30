package com.musicplayer.app.data.repository

import com.musicplayer.app.data.local.dao.PlaylistDao
import com.musicplayer.app.data.local.dao.SongDao
import com.musicplayer.app.data.local.entity.PlaylistSongEntity
import com.musicplayer.app.data.local.entity.toDomain
import com.musicplayer.app.data.local.entity.toEntity
import com.musicplayer.app.domain.model.Playlist
import com.musicplayer.app.domain.model.Song
import com.musicplayer.app.domain.repository.PlaylistRepository
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

@Singleton
class PlaylistRepositoryImpl @Inject constructor(
    private val playlistDao: PlaylistDao,
    private val songDao: SongDao
) : PlaylistRepository {
    companion object {
        const val LIKED_PLAYLIST_ID = "liked"
    }

    override suspend fun getOrCreateLikedPlaylist(): Playlist {
        val existing = playlistDao.getPlaylist(LIKED_PLAYLIST_ID)
        if (existing != null) return existing.toDomain()
        val now = System.currentTimeMillis()
        val playlist = Playlist(
            id = LIKED_PLAYLIST_ID,
            name = "喜欢",
            sortOrder = 0,
            createdAtEpochMs = now,
            updatedAtEpochMs = now
        )
        playlistDao.upsertPlaylist(playlist.toEntity())
        return playlist
    }
    override fun observePlaylists(): Flow<List<Playlist>> {
        return playlistDao.observePlaylists().map { entities -> entities.map { it.toDomain() } }
    }

    override fun observePlaylistSongs(playlistId: String): Flow<List<Song>> {
        return combine(
            playlistDao.observePlaylistEntries(playlistId),
            songDao.observeSongs()
        ) { entries, songs ->
            val songById = songs.associateBy { it.id }
            entries.mapNotNull { entry -> songById[entry.songId]?.toDomain() }
        }
    }

    override suspend fun createPlaylist(name: String, description: String?): Playlist {
        val now = System.currentTimeMillis()
        val playlist = Playlist(
            id = UUID.randomUUID().toString(),
            name = name,
            description = description,
            sortOrder = playlistDao.nextPlaylistSortOrder(),
            createdAtEpochMs = now,
            updatedAtEpochMs = now
        )
        playlistDao.upsertPlaylist(playlist.toEntity())
        return playlist
    }

    override suspend fun addSong(playlistId: String, songId: String) {
        if (playlistDao.isSongInPlaylist(playlistId, songId) > 0) return
        playlistDao.upsertPlaylistSong(
            PlaylistSongEntity(
                playlistId = playlistId,
                songId = songId,
                position = playlistDao.nextPosition(playlistId),
                addedAtEpochMs = System.currentTimeMillis()
            )
        )
        playlistDao.refreshPlaylistCount(playlistId, System.currentTimeMillis())
    }

    override suspend fun addSongsFromPlaylist(targetId: String, sourceId: String): Int {
        val existingIds = playlistDao.getPlaylistSongIds(targetId).toSet()
        val sourceIds = playlistDao.getPlaylistSongIds(sourceId)
        val newIds = sourceIds.filter { it !in existingIds }
        var nextPos = playlistDao.nextPosition(targetId)
        newIds.forEach { songId ->
            playlistDao.upsertPlaylistSong(
                PlaylistSongEntity(
                    playlistId = targetId,
                    songId = songId,
                    position = nextPos++,
                    addedAtEpochMs = System.currentTimeMillis()
                )
            )
        }
        if (newIds.isNotEmpty()) {
            playlistDao.refreshPlaylistCount(targetId, System.currentTimeMillis())
        }
        return newIds.size
    }

    override suspend fun removeSong(playlistId: String, songId: String) {
        playlistDao.removePlaylistSong(playlistId, songId)
        playlistDao.refreshPlaylistCount(playlistId, System.currentTimeMillis())
    }

    override suspend fun removeSongFromAllPlaylists(songId: String) {
        val playlistIds = playlistDao.getPlaylistIdsForSong(songId)
        playlistDao.removeSongFromAllPlaylists(songId)
        val now = System.currentTimeMillis()
        playlistIds.forEach { playlistDao.refreshPlaylistCount(it, now) }
    }

    override suspend fun deletePlaylist(playlistId: String) {
        playlistDao.deletePlaylist(playlistId)
    }

    override suspend fun renamePlaylist(playlistId: String, name: String) {
        playlistDao.renamePlaylist(playlistId, name, System.currentTimeMillis())
    }

    override suspend fun reorderPlaylists(ids: List<String>) {
        ids.forEachIndexed { index, id ->
            playlistDao.updateSortOrder(id, index)
        }
    }

    override suspend fun moveSong(playlistId: String, songId: String, toPosition: Int) {
        playlistDao.updateSongPosition(playlistId, songId, toPosition)
    }
}
