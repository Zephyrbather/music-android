package com.musicplayer.app.ui.theme

import com.musicplayer.app.domain.repository.ThemeMode
import com.musicplayer.app.domain.repository.ThemeStyle

data class ThemePreferences(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val themeStyle: ThemeStyle = ThemeStyle.CLASSIC,
    val dynamicColor: Boolean = true,
    val editorialLayout: Boolean = true
)
