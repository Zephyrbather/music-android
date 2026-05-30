package com.musicplayer.app.data.repository

import android.net.Uri
import android.util.Log
import com.musicplayer.app.data.local.dao.SongDao
import com.musicplayer.app.data.local.dao.WebDavServerDao
import com.musicplayer.app.data.local.entity.toDomain
import com.musicplayer.app.data.local.entity.toEntity
import com.musicplayer.app.data.scanner.EmbeddedMetadata
import com.musicplayer.app.data.scanner.LocalFileScanner
import com.musicplayer.app.data.tag.AudioTagWriter
import com.musicplayer.app.data.webdav.WebDavClient
import com.musicplayer.app.data.webdav.WebDavItem
import com.musicplayer.app.data.webdav.WebDavTrack
import com.musicplayer.app.domain.model.Song
import com.musicplayer.app.domain.model.ScrapeSearchMode
import com.musicplayer.app.domain.model.Source
import com.musicplayer.app.domain.model.WebDavServer
import com.musicplayer.app.domain.model.isUnresolvedAlbumTitle
import com.musicplayer.app.domain.repository.WebDavRepository
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import okhttp3.Credentials

@Singleton
class WebDavRepositoryImpl @Inject constructor(
    private val webDavServerDao: WebDavServerDao,
    private val webDavClient: WebDavClient,
    private val songDao: SongDao,
    private val localFileScanner: LocalFileScanner,
    private val audioTagWriter: AudioTagWriter
) : WebDavRepository {
    override fun observeServers(): Flow<List<WebDavServer>> {
        return webDavServerDao.observeServers().map { entities -> entities.map { it.toDomain() } }
    }

    override suspend fun upsertServer(server: WebDavServer) {
        webDavServerDao.upsert(server.toEntity())
    }

    override suspend fun deleteServer(serverId: String) {
        webDavServerDao.delete(serverId)
    }

    override suspend fun syncServer(serverId: String): List<Song> {
        val server = webDavServerDao.getServer(serverId)?.toDomain() ?: return emptyList()
        val tracks = webDavClient.listAudio(server)
        val syncedAt = System.currentTimeMillis()
        webDavServerDao.upsert(server.copy(lastSyncEpochMs = syncedAt).toEntity())
        val songs = tracks.toSyncedSongs(server, syncedAt)
        if (songs.isNotEmpty()) songDao.upsertAll(songs.map { it.toEntity() })
        return songs
    }

    override suspend fun syncFolder(server: WebDavServer, folderPath: String): List<Song> {
        val syncedAt = System.currentTimeMillis()
        Log.d("WebDAV", "syncFolder: server=${server.baseUrl}, folderPath=$folderPath")
        val tracks = webDavClient.listAudioInFolder(server, folderPath)
        Log.d("WebDAV", "syncFolder: got ${tracks.size} tracks from listAudioInFolder")
        val songs = tracks.toSyncedSongs(server, syncedAt)
        Log.d("WebDAV", "syncFolder: mapped to ${songs.size} songs, ids=${songs.take(3).map { it.id }}")
        if (songs.isNotEmpty()) {
            try {
                songDao.upsertAll(songs.map { it.toEntity() })
                Log.d("WebDAV", "syncFolder: upsertAll succeeded for ${songs.size} songs")
            } catch (e: Exception) {
                Log.e("WebDAV", "syncFolder: upsertAll failed", e)
                throw e
            }
        }
        return songs
    }

    override suspend fun downloadSong(song: Song, outputDir: File): Boolean {
        val serverId = song.webDavServerId ?: return false
        val server = webDavServerDao.getServer(serverId)?.toDomain() ?: return false
        val fileName = song.uri.substringAfterLast('/').let {
            if (it.isBlank()) "${song.title}.mp3" else it
        }
        val outputFile = File(outputDir, fileName)
        val success = webDavClient.downloadFile(song.uri, server, outputFile)
        if (success) {
            val localSong = song.copy(
                uri = Uri.fromFile(outputFile).toString(),
                source = Source.LOCAL,
                webDavServerId = null
            )
            val savedSong = audioTagWriter.writeIfSupported(
                song = localSong,
                mode = ScrapeSearchMode.ALL,
                renameLocalFile = false
            )
            songDao.upsertAll(listOf(savedSong.toEntity()))
        }
        return success
    }

    override suspend fun listDirectory(server: WebDavServer, path: String): List<WebDavItem> {
        return webDavClient.listDirectory(server, path)
    }

    private suspend fun List<WebDavTrack>.toSyncedSongs(
        server: WebDavServer,
        syncedAt: Long
    ): List<Song> {
        return chunked(REMOTE_METADATA_PARALLELISM).flatMap { tracks ->
            coroutineScope {
                tracks.map { track ->
                    async(Dispatchers.IO) { track.toSyncedSong(server, syncedAt) }
                }.awaitAll()
            }
        }
    }

    private suspend fun WebDavTrack.toSyncedSong(
        server: WebDavServer,
        syncedAt: Long
    ): Song {
        val baseSong = toSong(server, syncedAt)
        val existing = songDao.getSong(baseSong.id)?.toDomain()
        val prefixBytes = webDavClient.readPrefix(uri, server, REMOTE_EMBEDDED_PREFIX_BYTES)
            ?: ByteArray(0)
        val embedded = runCatching {
            localFileScanner.readRemoteEmbeddedMetadata(
                remoteUri = uri,
                requestHeaders = mapOf(
                    "Authorization" to Credentials.basic(server.username, server.password)
                ),
                prefixBytes = prefixBytes
            )
        }.getOrNull()
        return baseSong.mergeWithExistingAndEmbedded(existing, embedded)
    }

    private fun WebDavTrack.toSong(server: WebDavServer, syncedAt: Long) = run {
        val fileName = uri.substringAfterLast('/').let {
            java.net.URLDecoder.decode(it, "UTF-8")
        }
        val (parsedArtist, parsedTitle) = parseFileName(fileName)
        Song(
            id = "webdav-${server.id}-${uri.hashCode()}",
            title = parsedTitle.ifBlank { name.ifBlank { fileName } },
            artistName = parsedArtist.ifBlank { server.name },
            albumTitle = UNRESOLVED_ALBUM_TITLE,
            durationMs = 0L,
            uri = uri,
            source = Source.WEBDAV,
            webDavServerId = server.id,
            mimeType = mimeType,
            dateAddedEpochMs = syncedAt
        )
    }

    private fun Song.mergeWithExistingAndEmbedded(
        existing: Song?,
        embedded: EmbeddedMetadata?
    ): Song {
        return copy(
            title = embedded?.title ?: existing?.title ?: title,
            artistId = existing?.artistId ?: artistId,
            artistName = embedded?.artist ?: existing?.artistName ?: artistName,
            albumId = existing?.albumId
                .takeUnless { existing?.albumTitle?.isUnresolvedAlbumTitle() == true }
                ?: albumId,
            albumTitle = embedded?.album ?: existing?.albumTitle?.takeUnless { it.isUnresolvedAlbumTitle() } ?: albumTitle,
            durationMs = embedded?.durationMs
                ?: existing?.durationMs?.takeIf { it > 0L }
                ?: durationMs,
            trackNumber = existing?.trackNumber ?: trackNumber,
            discNumber = existing?.discNumber ?: discNumber,
            artworkUri = embedded?.artworkUri ?: existing?.artworkUri,
            artworkProvider = when {
                embedded?.artworkUri != null -> EMBEDDED_PROVIDER
                else -> existing?.artworkProvider
            },
            lyrics = embedded?.lyrics ?: existing?.lyrics,
            lyricsProvider = when {
                embedded?.lyrics != null -> EMBEDDED_PROVIDER
                else -> existing?.lyricsProvider
            },
            mimeType = embedded?.mimeType ?: existing?.mimeType ?: mimeType,
            bitrateKbps = embedded?.bitrateKbps ?: existing?.bitrateKbps,
            sampleRateHz = embedded?.sampleRateHz ?: existing?.sampleRateHz,
            releaseYear = embedded?.releaseYear ?: existing?.releaseYear,
            releaseMonth = embedded?.releaseMonth ?: existing?.releaseMonth,
            dateAddedEpochMs = existing?.dateAddedEpochMs ?: dateAddedEpochMs,
            isFavorite = existing?.isFavorite ?: isFavorite
        )
    }

    private fun parseFileName(filename: String): Pair<String, String> {
        val nameWithoutExt = filename.substringBeforeLast('.')
        if (nameWithoutExt.isBlank()) return "" to filename

        // Pattern: "Artist - Title"
        for (sep in listOf(" - ", "-", " – ", "–", " — ", "—")) {
            val idx = nameWithoutExt.indexOf(sep)
            if (idx > 0) {
                val artist = nameWithoutExt.substring(0, idx).trim()
                val title = nameWithoutExt.substring(idx + sep.length).trim()
                if (artist.isNotBlank() && title.isNotBlank()) return artist to title
            }
        }

        // Pattern: "TrackNumber. Title" or "TrackNumber Title"
        val trackPattern = Regex("""^(\d+)[\.\s、]+(.+)$""")
        trackPattern.find(nameWithoutExt)?.let { match ->
            return "Unknown Artist" to match.groupValues[2].trim()
        }

        return "" to nameWithoutExt.trim()
    }

    private companion object {
        const val REMOTE_METADATA_PARALLELISM = 4
        const val REMOTE_EMBEDDED_PREFIX_BYTES = 8 * 1024 * 1024
        const val EMBEDDED_PROVIDER = "Embedded"
        const val UNRESOLVED_ALBUM_TITLE = "待刮削"
    }
}

private fun String.endsWithAudioExtension(): Boolean {
    val lower = lowercase()
    return listOf(".mp3", ".flac", ".m4a", ".aac", ".ogg", ".opus", ".wav", ".dsf", ".dff")
        .any(lower::endsWith)
}
