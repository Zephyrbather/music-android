package com.musicplayer.app.performance

import kotlinx.coroutines.Dispatchers
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppDispatchers @Inject constructor() {
    val playback = Dispatchers.Main.immediate
    val io = Dispatchers.IO
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val scraper = Dispatchers.IO.limitedParallelism(4)
    val database = Dispatchers.IO
}