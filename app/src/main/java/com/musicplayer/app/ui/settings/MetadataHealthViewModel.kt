package com.musicplayer.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musicplayer.app.domain.model.MetadataIssue
import com.musicplayer.app.domain.model.MetadataIssueType
import com.musicplayer.app.domain.model.SongMetadataRepair
import com.musicplayer.app.domain.model.detectMetadataIssues
import com.musicplayer.app.domain.repository.MusicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class MetadataHealthUiState(
    val checking: Boolean = false,
    val repairing: Boolean = false,
    val hasChecked: Boolean = false,
    val issues: List<MetadataIssue> = emptyList()
)

sealed interface MetadataHealthEvent {
    data class Fixed(val repaired: Int, val removed: Int) : MetadataHealthEvent
}

@HiltViewModel
class MetadataHealthViewModel @Inject constructor(
    private val musicRepository: MusicRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(MetadataHealthUiState())
    val uiState = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<MetadataHealthEvent>(extraBufferCapacity = 4)
    val events = _events.asSharedFlow()

    private var fallbackTitle: String = "Unknown Song"
    private var fallbackArtist: String = "Unknown Artist"
    private var fallbackAlbum: String = "Unknown Album"

    fun check(titleFallback: String, artistFallback: String, albumFallback: String) {
        if (_uiState.value.checking) return
        fallbackTitle = titleFallback
        fallbackArtist = artistFallback
        fallbackAlbum = albumFallback
        viewModelScope.launch {
            _uiState.update { it.copy(checking = true) }
            refreshIssues()
        }
    }

    fun fix(issue: MetadataIssue) {
        if (_uiState.value.repairing) return
        viewModelScope.launch {
            _uiState.update { it.copy(repairing = true) }
            val (repaired, removed) = if (issue.type == MetadataIssueType.DUPLICATE) {
                issue.duplicateIds.forEach { musicRepository.removeFromLibrary(it) }
                0 to issue.duplicateIds.size
            } else {
                musicRepository.applyMetadataRepairs(listOf(issue.toRepair()))
                1 to 0
            }
            refreshIssues()
            _uiState.update { it.copy(repairing = false) }
            _events.emit(MetadataHealthEvent.Fixed(repaired, removed))
        }
    }

    fun fixAll() {
        val issues = _uiState.value.issues
        if (issues.isEmpty() || _uiState.value.repairing) return
        viewModelScope.launch {
            _uiState.update { it.copy(repairing = true) }
            val repairs = issues
                .filter { it.type != MetadataIssueType.DUPLICATE }
                .distinctBy { it.songId }
                .map { it.toRepair() }
            val duplicateIds = issues
                .filter { it.type == MetadataIssueType.DUPLICATE }
                .flatMap { issue -> issue.duplicateIds }
                .distinct()
            musicRepository.applyMetadataRepairs(repairs)
            duplicateIds.forEach { musicRepository.removeFromLibrary(it) }
            refreshIssues()
            _uiState.update { it.copy(repairing = false) }
            _events.emit(MetadataHealthEvent.Fixed(repairs.size, duplicateIds.size))
        }
    }

    private suspend fun refreshIssues() {
        val songs = musicRepository.observeSongs().first()
        val issues = withContext(Dispatchers.Default) {
            detectMetadataIssues(songs, fallbackTitle, fallbackArtist, fallbackAlbum)
        }
        _uiState.update {
            it.copy(checking = false, hasChecked = true, issues = issues)
        }
    }
}

private fun MetadataIssue.toRepair() = SongMetadataRepair(
    songId = songId,
    title = proposedTitle,
    artistName = proposedArtist,
    albumTitle = proposedAlbum
)
