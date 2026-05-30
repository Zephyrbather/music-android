package com.musicplayer.app.domain.repository

import com.musicplayer.app.domain.model.PlaybackMode
import kotlinx.coroutines.flow.Flow

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

enum class ThemeStyle {
    CLASSIC,
    OCEAN,
    FOREST,
    ROSE,
    MONO,
    IMMERSIVE
}

enum class FontFamilyOption {
    DEFAULT,
    SERIF,
    MONOSPACE
}

enum class AppLanguage {
    CHINESE,
    ENGLISH
}

enum class AiProvider {
    DEEPSEEK
}

interface SettingsRepository {
    val themeMode: Flow<ThemeMode>
    val themeStyle: Flow<ThemeStyle>
    val appLanguage: Flow<AppLanguage>
    val dynamicColorEnabled: Flow<Boolean>
    val editorialLayoutEnabled: Flow<Boolean>
    val autoScanLocalLibraryEnabled: Flow<Boolean>
    val autoScrapeEnabled: Flow<Boolean>
    val backgroundAiScrapeEnabled: Flow<Boolean>
    val playbackMode: Flow<PlaybackMode>
    val aiProvider: Flow<AiProvider>
    val aiApiKey: Flow<String>
    val aiModel: Flow<String>
    val localScanFolderUris: Flow<List<String>>
    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setThemeStyle(style: ThemeStyle)
    suspend fun setAppLanguage(language: AppLanguage)
    suspend fun setDynamicColorEnabled(enabled: Boolean)
    suspend fun setEditorialLayoutEnabled(enabled: Boolean)
    suspend fun setAutoScanLocalLibraryEnabled(enabled: Boolean)
    suspend fun setAutoScrapeEnabled(enabled: Boolean)
    suspend fun setBackgroundAiScrapeEnabled(enabled: Boolean)
    suspend fun setPlaybackMode(mode: PlaybackMode)
    suspend fun setAiProvider(provider: AiProvider)
    suspend fun setAiApiKey(apiKey: String)
    suspend fun setAiModel(model: String)
    val scrapeResumeCandidateIds: Flow<List<String>>
    val scrapeResumeCompleted: Flow<Int>
    val scrapeResumeTotal: Flow<Int>
    suspend fun setScrapeResume(candidateIds: List<String>, completed: Int, total: Int)
    suspend fun clearScrapeResume()
    suspend fun addLocalScanFolder(uri: String)
    suspend fun removeLocalScanFolder(uri: String)
    val fontFamilyOption: Flow<FontFamilyOption>
    suspend fun setFontFamilyOption(option: FontFamilyOption)
    val webDavFolderUris: Flow<List<String>>
    suspend fun addWebDavFolder(serverId: String, path: String)
    suspend fun removeWebDavFolder(serverId: String, path: String)
    suspend fun exportSettings(): String
    suspend fun importSettings(json: String)
}
