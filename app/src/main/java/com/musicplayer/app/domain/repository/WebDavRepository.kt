package com.musicplayer.app.domain.repository

import com.musicplayer.app.data.webdav.WebDavItem
import com.musicplayer.app.domain.model.Song
import com.musicplayer.app.domain.model.WebDavServer
import java.io.File
import kotlinx.coroutines.flow.Flow

interface WebDavRepository {
    fun observeServers(): Flow<List<WebDavServer>>
    suspend fun upsertServer(server: WebDavServer)
    suspend fun deleteServer(serverId: String)
    suspend fun syncServer(serverId: String): List<Song>
    suspend fun syncFolder(server: WebDavServer, folderPath: String): List<Song>
    suspend fun listDirectory(server: WebDavServer, path: String): List<WebDavItem>
    suspend fun downloadSong(song: Song, outputDir: File): Boolean
}
