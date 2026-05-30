package com.musicplayer.app

import android.app.Application
import com.musicplayer.app.data.repository.PlaylistRepositoryImpl
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class MusicPlayerApp : Application() {
    @Inject lateinit var playlistRepository: PlaylistRepositoryImpl

    override fun onCreate() {
        super.onCreate()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            playlistRepository.getOrCreateLikedPlaylist()
        }
    }
}
