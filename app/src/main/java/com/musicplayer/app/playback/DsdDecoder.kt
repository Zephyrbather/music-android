package com.musicplayer.app.playback

import javax.inject.Inject

class DsdDecoder @Inject constructor() {
    fun supports(uri: String): Boolean {
        val lower = uri.lowercase()
        return lower.endsWith(".dsf") || lower.endsWith(".dff")
    }

    fun describe(): String {
        return "DSD is routed through the bundled Media3 FFmpeg decoder when available."
    }
}
