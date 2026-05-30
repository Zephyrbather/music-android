package com.musicplayer.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.util.Log
import com.musicplayer.app.data.webdav.WebDavItem
import com.musicplayer.app.domain.model.Song
import com.musicplayer.app.domain.model.WebDavServer
import com.musicplayer.app.domain.repository.MusicRepository
import com.musicplayer.app.domain.repository.SettingsRepository
import com.musicplayer.app.domain.repository.WebDavRepository
import com.musicplayer.app.domain.usecase.LibraryWorkCoordinator
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class WebDavUiState(
    val servers: List<WebDavServer> = emptyList(),
    val syncingServerId: String? = null
)

data class SyncProgress(
    val totalFolders: Int = 0,
    val currentIndex: Int = 0,
    val currentFolder: String = "",
    val songsFound: Int = 0,
    val errors: List<String> = emptyList(),
    val finished: Boolean = false
)

@HiltViewModel
class WebDavViewModel @Inject constructor(
    private val webDavRepository: WebDavRepository,
    private val settingsRepository: SettingsRepository,
    private val musicRepository: MusicRepository,
    private val libraryWorkCoordinator: LibraryWorkCoordinator
) : ViewModel() {
    val uiState: StateFlow<WebDavUiState> = webDavRepository.observeServers()
        .map { servers -> WebDavUiState(servers = servers) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WebDavUiState())

    private val _showAddDialog = MutableStateFlow(false)
    val showAddDialog: StateFlow<Boolean> = _showAddDialog.asStateFlow()

    private val _editingServer = MutableStateFlow<WebDavServer?>(null)
    val editingServer: StateFlow<WebDavServer?> = _editingServer.asStateFlow()

    private val _syncingServerId = MutableStateFlow<String?>(null)
    val syncingServerId: StateFlow<String?> = _syncingServerId.asStateFlow()

    private val _syncProgress = MutableStateFlow(SyncProgress())
    val syncProgress: StateFlow<SyncProgress> = _syncProgress.asStateFlow()

    fun openAddDialog() {
        _showAddDialog.value = true
    }

    fun dismissAddDialog() {
        _showAddDialog.value = false
        _editingServer.value = null
    }

    fun editServer(server: WebDavServer) {
        _editingServer.value = server
        _showAddDialog.value = true
    }

    fun upsertServer(name: String, baseUrl: String, username: String, password: String) {
        val editing = _editingServer.value
        val server = if (editing != null) {
            editing.copy(name = name, baseUrl = baseUrl, username = username, password = password)
        } else {
            WebDavServer(
                id = UUID.randomUUID().toString(),
                name = name,
                baseUrl = baseUrl,
                username = username,
                password = password
            )
        }
        viewModelScope.launch {
            webDavRepository.upsertServer(server)
            dismissAddDialog()
        }
    }

    fun deleteServer(serverId: String) {
        viewModelScope.launch {
            webDavRepository.deleteServer(serverId)
        }
    }

    fun syncServer(serverId: String) {
        viewModelScope.launch {
            _syncingServerId.value = serverId
            runCatching { webDavRepository.syncServer(serverId) }
                .onSuccess { songs ->
                    persistSyncedSongs(songs)
                    startAutoScrapeForMissing(songs)
                }
            _syncingServerId.value = null
        }
    }

    fun toggleEnabled(server: WebDavServer) {
        viewModelScope.launch {
            webDavRepository.upsertServer(server.copy(enabled = !server.enabled))
        }
    }

    val webDavFolderUris: StateFlow<List<String>> = settingsRepository.webDavFolderUris
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addFolder(serverId: String, path: String) {
        viewModelScope.launch {
            settingsRepository.addWebDavFolder(serverId, path)
        }
    }

    fun removeFolder(serverId: String, path: String) {
        viewModelScope.launch {
            settingsRepository.removeWebDavFolder(serverId, path)
        }
    }

    suspend fun listDirectory(server: WebDavServer, path: String): List<WebDavItem> {
        return withContext(Dispatchers.IO) {
            webDavRepository.listDirectory(server, path)
        }
    }

    fun syncFolders(server: WebDavServer, folderPaths: List<String>) {
        viewModelScope.launch {
            _syncingServerId.value = server.id
            _syncProgress.value = SyncProgress(
                totalFolders = folderPaths.size,
                currentIndex = 0,
                currentFolder = folderPaths.firstOrNull().orEmpty(),
                songsFound = 0
            )
            var totalSongs = 0
            val syncedSongs = mutableListOf<Song>()
            val errors = mutableListOf<String>()
            folderPaths.forEachIndexed { index, path ->
                _syncProgress.value = _syncProgress.value.copy(
                    currentIndex = index,
                    currentFolder = path
                )
                val result = runCatching { webDavRepository.syncFolder(server, path) }
                result.onSuccess { songs ->
                    Log.d("WebDAV", "syncFolders: path=$path -> ${songs.size} songs")
                    persistSyncedSongs(songs)
                    syncedSongs += songs
                    totalSongs += songs.size
                    _syncProgress.value = _syncProgress.value.copy(songsFound = totalSongs)
                }.onFailure { e ->
                    Log.e("WebDAV", "syncFolders: path=$path FAILED", e)
                    errors.add("$path: ${e.message}")
                    _syncProgress.value = _syncProgress.value.copy(errors = errors.toList())
                }
            }
            startAutoScrapeForMissing(syncedSongs)
            _syncProgress.value = _syncProgress.value.copy(finished = true, errors = errors)
            _syncingServerId.value = null
        }
    }

    private suspend fun persistSyncedSongs(songs: List<Song>) {
        if (songs.isNotEmpty()) {
            musicRepository.upsertSongs(songs)
        }
    }

    private suspend fun startAutoScrapeForMissing(songs: List<Song>) {
        if (songs.any { it.artworkUri.isNullOrBlank() || it.lyrics.isNullOrBlank() } &&
            settingsRepository.autoScrapeEnabled.first()
        ) {
            libraryWorkCoordinator.startScrapeMissing()
        }
    }
}
