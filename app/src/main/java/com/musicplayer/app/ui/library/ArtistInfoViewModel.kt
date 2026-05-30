package com.musicplayer.app.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musicplayer.app.data.scraper.ArtistInfoLookup
import com.musicplayer.app.domain.model.ArtistOnlineInfo
import com.musicplayer.app.domain.repository.MusicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ArtistInfoUiState(
    val artistName: String? = null,
    val loading: Boolean = false,
    val info: ArtistOnlineInfo? = null
)

@HiltViewModel
class ArtistInfoViewModel @Inject constructor(
    private val artistInfoLookup: ArtistInfoLookup,
    private val musicRepository: MusicRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(ArtistInfoUiState())
    val uiState: StateFlow<ArtistInfoUiState> = _uiState.asStateFlow()

    fun load(artistName: String) {
        if (_uiState.value.artistName == artistName && (_uiState.value.loading || _uiState.value.info != null)) {
            return
        }
        _uiState.update { ArtistInfoUiState(artistName = artistName, loading = true) }
        viewModelScope.launch {
            val info = artistInfoLookup.findArtistInfo(artistName)
            _uiState.update {
                it.copy(loading = false, info = info)
            }
        }
    }

    fun scrapeAvatar(artistName: String) {
        _uiState.update { it.copy(artistName = artistName, loading = true) }
        viewModelScope.launch {
            val avatarUri = musicRepository.refreshArtistArtwork(artistName)
            _uiState.update { state ->
                val current = state.info
                state.copy(
                    loading = false,
                    info = if (!avatarUri.isNullOrBlank()) {
                        current?.copy(avatarUri = avatarUri, provider = "Artist artwork")
                            ?: ArtistOnlineInfo(
                                name = artistName,
                                bio = current?.bio.orEmpty(),
                                avatarUri = avatarUri,
                                provider = "Artist artwork"
                            )
                    } else {
                        current
                    }
                )
            }
        }
    }
}
