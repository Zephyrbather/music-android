package com.musicplayer.app.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.annotation.ExperimentalCoilApi
import coil.imageLoader
import android.net.Uri
import com.musicplayer.app.data.cache.CacheManager
import com.musicplayer.app.domain.usecase.ExportImportUseCase
import com.musicplayer.app.domain.usecase.AiGenerateSemanticTagsUseCase
import com.musicplayer.app.domain.usecase.AiSemanticTagResult
import com.musicplayer.app.domain.usecase.LibraryWorkCoordinator
import com.musicplayer.app.domain.usecase.LibraryWorkState
import com.musicplayer.app.domain.repository.AppLanguage
import com.musicplayer.app.domain.repository.AiProvider
import com.musicplayer.app.domain.repository.FontFamilyOption
import com.musicplayer.app.domain.repository.SettingsRepository
import com.musicplayer.app.domain.repository.ThemeMode
import com.musicplayer.app.domain.repository.ThemeStyle
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val themeStyle: ThemeStyle = ThemeStyle.CLASSIC,
    val appLanguage: AppLanguage = AppLanguage.CHINESE,
    val dynamicColor: Boolean = false,
    val editorialLayout: Boolean = true,
    val autoScanLocalLibrary: Boolean = false,
    val autoScrape: Boolean = true,
    val backgroundAiScrape: Boolean = false,
    val aiCategoryScrape: Boolean = false,
    val aiFeaturesEnabled: Boolean = false,
    val aiPlaylistEnabled: Boolean = false,
    val aiSemanticTagsEnabled: Boolean = false,
    val aiProvider: AiProvider = AiProvider.DEEPSEEK,
    val aiApiKey: String = "",
    val aiModel: String = "deepseek-v4-flash",
    val localScanFolderUris: List<String> = emptyList(),
    val fontFamilyOption: FontFamilyOption = FontFamilyOption.DEFAULT
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val libraryWorkCoordinator: LibraryWorkCoordinator,
    private val cacheManager: CacheManager,
    private val exportImportUseCase: ExportImportUseCase,
    private val aiGenerateSemanticTagsUseCase: AiGenerateSemanticTagsUseCase,
    @ApplicationContext private val appContext: Context
) : ViewModel() {
    private val appearanceState = combine(
        combine(
            settingsRepository.themeMode,
            settingsRepository.themeStyle,
            settingsRepository.appLanguage,
            settingsRepository.dynamicColorEnabled,
            settingsRepository.editorialLayoutEnabled
        ) { themeMode, themeStyle, appLanguage, dynamicColor, editorialLayout ->
            arrayOf(themeMode, themeStyle, appLanguage, dynamicColor, editorialLayout)
        },
        settingsRepository.fontFamilyOption
    ) { arr, fontFamily ->
        @Suppress("UNCHECKED_CAST")
        AppearanceSettings(
            arr[0] as ThemeMode,
            arr[1] as ThemeStyle,
            arr[2] as AppLanguage,
            arr[3] as Boolean,
            arr[4] as Boolean,
            fontFamily
        )
    }

    private val libraryState = combine(
        settingsRepository.autoScanLocalLibraryEnabled,
        settingsRepository.localScanFolderUris
    ) { autoScan, folders ->
        LibrarySettings(autoScan, folders)
    }

    private val scrapingState = combine(
        combine(
            settingsRepository.autoScrapeEnabled,
            settingsRepository.backgroundAiScrapeEnabled,
            settingsRepository.aiCategoryScrapeEnabled,
            settingsRepository.aiFeaturesEnabled,
            settingsRepository.aiPlaylistEnabled,
            settingsRepository.aiSemanticTagsEnabled,
            settingsRepository.aiProvider,
            settingsRepository.aiApiKey
        ) { values ->
            val autoScrape = values[0] as Boolean
            val backgroundAiScrape = values[1] as Boolean
            val aiCategoryScrape = values[2] as Boolean
            val aiFeaturesEnabled = values[3] as Boolean
            val aiPlaylistEnabled = values[4] as Boolean
            val aiSemanticTagsEnabled = values[5] as Boolean
            val aiProvider = values[6] as AiProvider
            val aiApiKey = values[7] as String
            ScrapingSettings(
                autoScrape = autoScrape,
                backgroundAiScrape = backgroundAiScrape,
                aiCategoryScrape = aiCategoryScrape,
                aiFeaturesEnabled = aiFeaturesEnabled,
                aiPlaylistEnabled = aiPlaylistEnabled,
                aiSemanticTagsEnabled = aiSemanticTagsEnabled,
                aiProvider = aiProvider,
                aiApiKey = aiApiKey
            )
        },
        settingsRepository.aiModel
    ) { scraping, aiModel ->
        scraping.copy(aiModel = aiModel)
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ScrapingSettings())

    val uiState: StateFlow<SettingsUiState> = combine(
        appearanceState,
        libraryState,
        scrapingState
    ) { appearance, library, scraping ->
        SettingsUiState(
            themeMode = appearance.themeMode,
            themeStyle = appearance.themeStyle,
            appLanguage = appearance.appLanguage,
            dynamicColor = appearance.dynamicColor,
            editorialLayout = appearance.editorialLayout,
            autoScanLocalLibrary = library.autoScan,
            autoScrape = scraping.autoScrape,
            backgroundAiScrape = scraping.backgroundAiScrape,
            aiCategoryScrape = scraping.aiCategoryScrape,
            aiFeaturesEnabled = scraping.aiFeaturesEnabled,
            aiPlaylistEnabled = scraping.aiPlaylistEnabled,
            aiSemanticTagsEnabled = scraping.aiSemanticTagsEnabled,
            aiProvider = scraping.aiProvider,
            aiApiKey = scraping.aiApiKey,
            aiModel = scraping.aiModel,
            localScanFolderUris = library.folders,
            fontFamilyOption = appearance.fontFamily
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    val workState: StateFlow<LibraryWorkState> = libraryWorkCoordinator.observeState()

    private val _imageCacheSize = MutableStateFlow<String?>(null)
    val imageCacheSize: StateFlow<String?> = _imageCacheSize.asStateFlow()

    private val _artworkCacheSize = MutableStateFlow<String?>(null)
    val artworkCacheSize: StateFlow<String?> = _artworkCacheSize.asStateFlow()

    private val _scrapeCacheEntries = MutableStateFlow<Int?>(null)
    val scrapeCacheEntries: StateFlow<Int?> = _scrapeCacheEntries.asStateFlow()

    private val _cacheSizeFormatted = MutableStateFlow<String?>(null)
    val cacheSizeFormatted: StateFlow<String?> = _cacheSizeFormatted.asStateFlow()

    private val _clearingCache = MutableStateFlow(false)
    val clearingCache: StateFlow<Boolean> = _clearingCache.asStateFlow()

    private val _exportImportResult = MutableStateFlow<String?>(null)
    val exportImportResult: StateFlow<String?> = _exportImportResult.asStateFlow()

    private val _aiSemanticTagResult = MutableStateFlow<AiSemanticTagResult?>(null)
    val aiSemanticTagResult: StateFlow<AiSemanticTagResult?> = _aiSemanticTagResult.asStateFlow()

    private val _generatingSemanticTags = MutableStateFlow(false)
    val generatingSemanticTags: StateFlow<Boolean> = _generatingSemanticTags.asStateFlow()

    init {
        loadCacheSizes()
    }

    fun loadCacheSizes() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                _imageCacheSize.value = computeDirSize("image_cache_disk").let(::formatBytes)
                _artworkCacheSize.value = computeDirSize("embedded-artwork").let(::formatBytes)
                _scrapeCacheEntries.value = cacheManager.count()
                val total = computeDirSize("image_cache_disk") + computeDirSize("embedded-artwork")
                _cacheSizeFormatted.value = formatBytes(total)
            }
        }
    }

    @OptIn(ExperimentalCoilApi::class)
    fun clearImageCache() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                appContext.imageLoader.diskCache?.clear()
            }
            loadCacheSizes()
        }
    }

    fun clearArtworkCache() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                File(appContext.cacheDir, "embedded-artwork").deleteRecursively()
            }
            loadCacheSizes()
        }
    }

    fun clearScrapeCache() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                cacheManager.clearAll()
            }
            loadCacheSizes()
        }
    }

    @OptIn(ExperimentalCoilApi::class)
    fun clearAllCache() {
        viewModelScope.launch {
            _clearingCache.value = true
            withContext(Dispatchers.IO) {
                appContext.imageLoader.diskCache?.clear()
                File(appContext.cacheDir, "embedded-artwork").deleteRecursively()
                cacheManager.clearAll()
            }
            _clearingCache.value = false
            loadCacheSizes()
        }
    }

    private fun computeDirSize(name: String): Long {
        return File(appContext.cacheDir, name).let { dir ->
            if (dir.exists()) dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
            else 0L
        }
    }

    private fun formatBytes(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            else -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            settingsRepository.setThemeMode(mode)
        }
    }

    fun setThemeStyle(style: ThemeStyle) {
        viewModelScope.launch {
            settingsRepository.setThemeStyle(style)
        }
    }

    fun setAppLanguage(language: AppLanguage) {
        viewModelScope.launch {
            settingsRepository.setAppLanguage(language)
        }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setDynamicColorEnabled(enabled)
        }
    }

    fun setEditorialLayout(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setEditorialLayoutEnabled(enabled)
        }
    }

    fun setFontFamilyOption(option: FontFamilyOption) {
        viewModelScope.launch {
            settingsRepository.setFontFamilyOption(option)
        }
    }

    fun setAutoScanLocalLibrary(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setAutoScanLocalLibraryEnabled(enabled)
        }
    }

    fun setAutoScrape(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setAutoScrapeEnabled(enabled)
        }
    }

    fun setBackgroundAiScrape(enabled: Boolean) {
        viewModelScope.launch {
            if (!enabled || (settingsRepository.aiFeaturesEnabled.first() && settingsRepository.aiApiKey.first().isNotBlank())) {
                settingsRepository.setBackgroundAiScrapeEnabled(enabled)
            }
        }
    }

    fun setAiCategoryScrape(enabled: Boolean) {
        viewModelScope.launch {
            if (!enabled || (settingsRepository.aiFeaturesEnabled.first() && settingsRepository.aiApiKey.first().isNotBlank())) {
                settingsRepository.setAiCategoryScrapeEnabled(enabled)
            }
        }
    }

    fun setAiFeaturesEnabled(enabled: Boolean) {
        viewModelScope.launch {
            if (!enabled || settingsRepository.aiApiKey.first().isNotBlank()) {
                settingsRepository.setAiFeaturesEnabled(enabled)
            }
        }
    }

    fun setAiPlaylistEnabled(enabled: Boolean) {
        viewModelScope.launch {
            if (!enabled || (settingsRepository.aiFeaturesEnabled.first() && settingsRepository.aiApiKey.first().isNotBlank())) {
                settingsRepository.setAiPlaylistEnabled(enabled)
            }
        }
    }

    fun setAiSemanticTagsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            if (!enabled || (settingsRepository.aiFeaturesEnabled.first() && settingsRepository.aiApiKey.first().isNotBlank())) {
                settingsRepository.setAiSemanticTagsEnabled(enabled)
            }
        }
    }

    fun generateMissingSemanticTags() {
        viewModelScope.launch {
            if (_generatingSemanticTags.value) return@launch
            _generatingSemanticTags.value = true
            _aiSemanticTagResult.value = withContext(Dispatchers.IO) {
                aiGenerateSemanticTagsUseCase()
            }
            _generatingSemanticTags.value = false
        }
    }

    fun saveAiSettings(provider: AiProvider, apiKey: String, model: String) {
        viewModelScope.launch {
            settingsRepository.setAiProvider(provider)
            settingsRepository.setAiApiKey(apiKey)
            settingsRepository.setAiModel(model)
            if (apiKey.isBlank()) {
                settingsRepository.setAiFeaturesEnabled(false)
                settingsRepository.setBackgroundAiScrapeEnabled(false)
                settingsRepository.setAiCategoryScrapeEnabled(false)
                settingsRepository.setAiPlaylistEnabled(false)
                settingsRepository.setAiSemanticTagsEnabled(false)
            }
        }
    }

    fun addLocalScanFolder(uri: String) {
        viewModelScope.launch {
            settingsRepository.addLocalScanFolder(uri)
        }
    }

    fun removeLocalScanFolder(uri: String) {
        viewModelScope.launch {
            settingsRepository.removeLocalScanFolder(uri)
        }
    }

    fun scanNow() {
        libraryWorkCoordinator.startScan()
    }

    val hasResumableScrape: StateFlow<Boolean> = libraryWorkCoordinator.hasResumableScrape

    fun scrapeMissingMetadata() {
        libraryWorkCoordinator.startScrapeMissing()
    }

    fun resumeScrape() {
        libraryWorkCoordinator.resumeScrapeMissing()
    }

    fun exportData(uri: Uri) {
        viewModelScope.launch {
            val success = exportImportUseCase.exportTo(uri)
            _exportImportResult.value = if (success) "export_success" else "export_failed"
        }
    }

    fun importData(uri: Uri) {
        viewModelScope.launch {
            val success = exportImportUseCase.importFrom(uri)
            _exportImportResult.value = if (success) "import_success" else "import_failed"
        }
    }

    fun clearExportImportResult() {
        _exportImportResult.value = null
    }
}

private data class AppearanceSettings(
    val themeMode: ThemeMode,
    val themeStyle: ThemeStyle,
    val appLanguage: AppLanguage,
    val dynamicColor: Boolean,
    val editorialLayout: Boolean,
    val fontFamily: FontFamilyOption
)

private data class LibrarySettings(
    val autoScan: Boolean,
    val folders: List<String>
)

private data class ScrapingSettings(
    val autoScrape: Boolean = true,
    val backgroundAiScrape: Boolean = false,
    val aiCategoryScrape: Boolean = false,
    val aiFeaturesEnabled: Boolean = false,
    val aiPlaylistEnabled: Boolean = false,
    val aiSemanticTagsEnabled: Boolean = false,
    val aiProvider: AiProvider = AiProvider.DEEPSEEK,
    val aiApiKey: String = "",
    val aiModel: String = "deepseek-v4-flash"
)
