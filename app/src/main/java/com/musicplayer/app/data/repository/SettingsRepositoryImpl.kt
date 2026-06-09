package com.musicplayer.app.data.repository

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.musicplayer.app.domain.model.PlaybackMode
import com.musicplayer.app.domain.model.SongCategoryTags
import com.musicplayer.app.domain.repository.AppLanguage
import com.musicplayer.app.domain.repository.AiProvider
import com.musicplayer.app.domain.repository.FontFamilyOption
import com.musicplayer.app.domain.repository.SettingsRepository
import com.musicplayer.app.domain.repository.ThemeMode
import com.musicplayer.app.domain.repository.ThemeStyle
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : SettingsRepository {
    override val themeMode: Flow<ThemeMode> = context.settingsDataStore.data.map { preferences ->
        preferences[THEME_MODE]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
            ?: ThemeMode.SYSTEM
    }

    override val themeStyle: Flow<ThemeStyle> = context.settingsDataStore.data.map { preferences ->
        preferences[THEME_STYLE]?.let { runCatching { ThemeStyle.valueOf(it) }.getOrNull() }
            ?: ThemeStyle.CLASSIC
    }

    override val fontFamilyOption: Flow<FontFamilyOption> = context.settingsDataStore.data.map { preferences ->
        preferences[FONT_FAMILY]?.let { runCatching { FontFamilyOption.valueOf(it) }.getOrNull() }
            ?: FontFamilyOption.DEFAULT
    }

    override val appLanguage: Flow<AppLanguage> = context.settingsDataStore.data.map { preferences ->
        preferences[APP_LANGUAGE]?.let { runCatching { AppLanguage.valueOf(it) }.getOrNull() }
            ?: AppLanguage.CHINESE
    }

    override val dynamicColorEnabled: Flow<Boolean> = context.settingsDataStore.data.map { preferences ->
        preferences[DYNAMIC_COLOR] ?: false
    }

    override val editorialLayoutEnabled: Flow<Boolean> = context.settingsDataStore.data.map { preferences ->
        preferences[EDITORIAL_LAYOUT] ?: true
    }

    override val autoScanLocalLibraryEnabled: Flow<Boolean> = context.settingsDataStore.data.map { preferences ->
        preferences[AUTO_SCAN_LOCAL] ?: false
    }

    override val autoScrapeEnabled: Flow<Boolean> = context.settingsDataStore.data.map { preferences ->
        preferences[AUTO_SCRAPE] ?: true
    }

    override val backgroundAiScrapeEnabled: Flow<Boolean> = context.settingsDataStore.data.map { preferences ->
        preferences[BACKGROUND_AI_SCRAPE] ?: false
    }

    override val aiCategoryScrapeEnabled: Flow<Boolean> = context.settingsDataStore.data.map { preferences ->
        preferences[AI_CATEGORY_SCRAPE] ?: false
    }

    override val playbackMode: Flow<PlaybackMode> = context.settingsDataStore.data.map { preferences ->
        preferences[PLAYBACK_MODE]?.let { runCatching { PlaybackMode.valueOf(it) }.getOrNull() }
            ?: PlaybackMode.SEQUENTIAL
    }

    override val desktopLyricsEnabled: Flow<Boolean> = context.settingsDataStore.data.map { preferences ->
        preferences[DESKTOP_LYRICS_ENABLED] ?: false
    }

    override val aiProvider: Flow<AiProvider> = context.settingsDataStore.data.map { preferences ->
        preferences[AI_PROVIDER]?.let { runCatching { AiProvider.valueOf(it) }.getOrNull() }
            ?: AiProvider.DEEPSEEK
    }

    override val aiApiKey: Flow<String> = context.settingsDataStore.data.map { preferences ->
        decryptSecret(preferences[AI_API_KEY].orEmpty())
    }

    override val aiModel: Flow<String> = context.settingsDataStore.data.map { preferences ->
        preferences[AI_MODEL]?.takeIf { it.isNotBlank() } ?: DEFAULT_DEEPSEEK_MODEL
    }

    override val adaptiveEqualizerEnabled: Flow<Boolean> = context.settingsDataStore.data.map { preferences ->
        preferences[ADAPTIVE_EQUALIZER] ?: false
    }

    override val equalizerCategoryPresets: Flow<Map<String, String>> = context.settingsDataStore.data.map { preferences ->
        val stored = preferences[EQ_CATEGORY_PRESETS]
            ?.mapNotNull(::decodeCategoryPreset)
            ?.toMap()
            .orEmpty()
        SongCategoryTags.defaultPresetByCategory + stored
    }

    override val localScanFolderUris: Flow<List<String>> = context.settingsDataStore.data.map { preferences ->
        preferences[LOCAL_SCAN_FOLDERS]?.toList().orEmpty().sorted()
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        context.settingsDataStore.edit { preferences -> preferences[THEME_MODE] = mode.name }
    }

    override suspend fun setThemeStyle(style: ThemeStyle) {
        context.settingsDataStore.edit { preferences -> preferences[THEME_STYLE] = style.name }
    }

    override suspend fun setFontFamilyOption(option: FontFamilyOption) {
        context.settingsDataStore.edit { preferences -> preferences[FONT_FAMILY] = option.name }
    }

    override suspend fun setAppLanguage(language: AppLanguage) {
        context.settingsDataStore.edit { preferences -> preferences[APP_LANGUAGE] = language.name }
    }

    override suspend fun setDynamicColorEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { preferences -> preferences[DYNAMIC_COLOR] = enabled }
    }

    override suspend fun setEditorialLayoutEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { preferences -> preferences[EDITORIAL_LAYOUT] = enabled }
    }

    override suspend fun setAutoScanLocalLibraryEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { preferences -> preferences[AUTO_SCAN_LOCAL] = enabled }
    }

    override suspend fun setAutoScrapeEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { preferences -> preferences[AUTO_SCRAPE] = enabled }
    }

    override suspend fun setBackgroundAiScrapeEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { preferences -> preferences[BACKGROUND_AI_SCRAPE] = enabled }
    }

    override suspend fun setAiCategoryScrapeEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { preferences -> preferences[AI_CATEGORY_SCRAPE] = enabled }
    }

    override suspend fun setPlaybackMode(mode: PlaybackMode) {
        context.settingsDataStore.edit { preferences -> preferences[PLAYBACK_MODE] = mode.name }
    }

    override suspend fun setDesktopLyricsEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { preferences -> preferences[DESKTOP_LYRICS_ENABLED] = enabled }
    }

    override suspend fun setAiProvider(provider: AiProvider) {
        context.settingsDataStore.edit { preferences -> preferences[AI_PROVIDER] = provider.name }
    }

    override suspend fun setAiApiKey(apiKey: String) {
        context.settingsDataStore.edit { preferences ->
            val trimmed = apiKey.trim()
            if (trimmed.isBlank()) {
                preferences.remove(AI_API_KEY)
            } else {
                preferences[AI_API_KEY] = encryptSecret(trimmed)
            }
        }
    }

    override suspend fun setAiModel(model: String) {
        context.settingsDataStore.edit { preferences ->
            preferences[AI_MODEL] = model.trim().ifBlank { DEFAULT_DEEPSEEK_MODEL }
        }
    }

    override suspend fun setAdaptiveEqualizerEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { preferences -> preferences[ADAPTIVE_EQUALIZER] = enabled }
    }

    override suspend fun setEqualizerCategoryPreset(category: String, presetId: String) {
        val normalizedCategory = SongCategoryTags.normalize(category) ?: return
        val normalizedPreset = presetId.trim().takeIf { it.isNotBlank() } ?: return
        updateStringSet(EQ_CATEGORY_PRESETS) { mappings ->
            mappings.filterNot { it.substringBefore('|') == normalizedCategory }.toSet() +
                encodeCategoryPreset(normalizedCategory, normalizedPreset)
        }
    }

    override val scrapeResumeCandidateIds: Flow<List<String>> = context.settingsDataStore.data.map { prefs ->
        prefs[SCRAPE_CANDIDATES]?.toList().orEmpty()
    }

    override val scrapeResumeCompleted: Flow<Int> = context.settingsDataStore.data.map { prefs ->
        prefs[SCRAPE_COMPLETED] ?: 0
    }

    override val scrapeResumeTotal: Flow<Int> = context.settingsDataStore.data.map { prefs ->
        prefs[SCRAPE_TOTAL] ?: 0
    }

    override suspend fun setScrapeResume(candidateIds: List<String>, completed: Int, total: Int) {
        context.settingsDataStore.edit { prefs ->
            prefs[SCRAPE_CANDIDATES] = candidateIds.toSet()
            prefs[SCRAPE_COMPLETED] = completed
            prefs[SCRAPE_TOTAL] = total
        }
    }

    override suspend fun clearScrapeResume() {
        context.settingsDataStore.edit { prefs ->
            prefs.remove(SCRAPE_CANDIDATES)
            prefs.remove(SCRAPE_COMPLETED)
            prefs.remove(SCRAPE_TOTAL)
        }
    }

    override suspend fun addLocalScanFolder(uri: String) {
        updateStringSet(LOCAL_SCAN_FOLDERS) { folders -> folders + uri }
    }

    override suspend fun removeLocalScanFolder(uri: String) {
        updateStringSet(LOCAL_SCAN_FOLDERS) { folders -> folders - uri }
    }

    override val webDavFolderUris: Flow<List<String>> = context.settingsDataStore.data.map { preferences ->
        preferences[WEBDAV_FOLDERS]?.toList().orEmpty().sorted()
    }

    override suspend fun addWebDavFolder(serverId: String, path: String) {
        updateStringSet(WEBDAV_FOLDERS) { folders -> folders + "$serverId|$path" }
    }

    override suspend fun removeWebDavFolder(serverId: String, path: String) {
        updateStringSet(WEBDAV_FOLDERS) { folders -> folders - "$serverId|$path" }
    }

    override suspend fun exportSettings(): String {
        val prefs = context.settingsDataStore.data.first()
        return JSONObject().apply {
            put("theme_mode", prefs[THEME_MODE] ?: ThemeMode.SYSTEM.name)
            put("theme_style", prefs[THEME_STYLE] ?: ThemeStyle.CLASSIC.name)
            put("font_family", prefs[FONT_FAMILY] ?: FontFamilyOption.DEFAULT.name)
            put("app_language", prefs[APP_LANGUAGE] ?: AppLanguage.CHINESE.name)
            put("dynamic_color", prefs[DYNAMIC_COLOR] ?: false)
            put("editorial_layout", prefs[EDITORIAL_LAYOUT] ?: true)
            put("auto_scan_local", prefs[AUTO_SCAN_LOCAL] ?: false)
            put("auto_scrape", prefs[AUTO_SCRAPE] ?: true)
            put("background_ai_scrape", prefs[BACKGROUND_AI_SCRAPE] ?: false)
            put("ai_category_scrape", prefs[AI_CATEGORY_SCRAPE] ?: false)
            put("playback_mode", prefs[PLAYBACK_MODE] ?: PlaybackMode.SEQUENTIAL.name)
            put("desktop_lyrics_enabled", prefs[DESKTOP_LYRICS_ENABLED] ?: false)
            put("ai_provider", prefs[AI_PROVIDER] ?: AiProvider.DEEPSEEK.name)
            put("ai_model", prefs[AI_MODEL] ?: DEFAULT_DEEPSEEK_MODEL)
            put("adaptive_equalizer", prefs[ADAPTIVE_EQUALIZER] ?: false)
            put("eq_category_presets", JSONArray(
                prefs[EQ_CATEGORY_PRESETS]?.toList().orEmpty()
            ))
            put("local_scan_folders", org.json.JSONArray(
                prefs[LOCAL_SCAN_FOLDERS]?.toList().orEmpty()
            ))
            put("scrape_candidates", org.json.JSONArray(
                prefs[SCRAPE_CANDIDATES]?.toList().orEmpty()
            ))
            put("scrape_completed", prefs[SCRAPE_COMPLETED] ?: 0)
            put("scrape_total", prefs[SCRAPE_TOTAL] ?: 0)
            put("webdav_folders", org.json.JSONArray(
                prefs[WEBDAV_FOLDERS]?.toList().orEmpty()
            ))
        }.toString()
    }

    override suspend fun importSettings(json: String) {
        val j = JSONObject(json)
        context.settingsDataStore.edit { prefs ->
            j.optString("theme_mode").takeIf { it.isNotBlank() }?.let { prefs[THEME_MODE] = it }
            j.optString("theme_style").takeIf { it.isNotBlank() }?.let { prefs[THEME_STYLE] = it }
            j.optString("font_family").takeIf { it.isNotBlank() }?.let { prefs[FONT_FAMILY] = it }
            j.optString("app_language").takeIf { it.isNotBlank() }?.let { prefs[APP_LANGUAGE] = it }
            if (j.has("dynamic_color")) prefs[DYNAMIC_COLOR] = j.getBoolean("dynamic_color")
            if (j.has("editorial_layout")) prefs[EDITORIAL_LAYOUT] = j.getBoolean("editorial_layout")
            if (j.has("auto_scan_local")) prefs[AUTO_SCAN_LOCAL] = j.getBoolean("auto_scan_local")
            if (j.has("auto_scrape")) prefs[AUTO_SCRAPE] = j.getBoolean("auto_scrape")
            if (j.has("background_ai_scrape")) {
                prefs[BACKGROUND_AI_SCRAPE] = j.getBoolean("background_ai_scrape") && prefs[AI_API_KEY] != null
            }
            if (j.has("ai_category_scrape")) {
                prefs[AI_CATEGORY_SCRAPE] = j.getBoolean("ai_category_scrape") && prefs[AI_API_KEY] != null
            }
            j.optString("playback_mode").takeIf { it.isNotBlank() }?.let { prefs[PLAYBACK_MODE] = it }
            if (j.has("desktop_lyrics_enabled")) {
                prefs[DESKTOP_LYRICS_ENABLED] = j.getBoolean("desktop_lyrics_enabled")
            }
            j.optString("ai_provider").takeIf { it.isNotBlank() }?.let { prefs[AI_PROVIDER] = it }
            j.optString("ai_model").takeIf { it.isNotBlank() }?.let { prefs[AI_MODEL] = it }
            if (j.has("adaptive_equalizer")) prefs[ADAPTIVE_EQUALIZER] = j.getBoolean("adaptive_equalizer")
            j.optJSONArray("eq_category_presets")?.let { arr ->
                prefs[EQ_CATEGORY_PRESETS] = (0 until arr.length()).map { arr.getString(it) }.toSet()
            }
            j.optJSONArray("local_scan_folders")?.let { arr ->
                prefs[LOCAL_SCAN_FOLDERS] = (0 until arr.length()).map { arr.getString(it) }.toSet()
            }
            j.optJSONArray("scrape_candidates")?.let { arr ->
                prefs[SCRAPE_CANDIDATES] = (0 until arr.length()).map { arr.getString(it) }.toSet()
            }
            if (j.has("scrape_completed")) prefs[SCRAPE_COMPLETED] = j.getInt("scrape_completed")
            if (j.has("scrape_total")) prefs[SCRAPE_TOTAL] = j.getInt("scrape_total")
            j.optJSONArray("webdav_folders")?.let { arr ->
                prefs[WEBDAV_FOLDERS] = (0 until arr.length()).map { arr.getString(it) }.toSet()
            }
        }
    }

    private fun encodeCategoryPreset(category: String, presetId: String): String {
        return "$category|$presetId"
    }

    private fun decodeCategoryPreset(value: String): Pair<String, String>? {
        val category = value.substringBefore('|').trim()
        val presetId = value.substringAfter('|', "").trim()
        val normalizedCategory = SongCategoryTags.normalize(category) ?: return null
        if (presetId.isBlank()) return null
        return normalizedCategory to presetId
    }

    private suspend fun updateStringSet(
        key: Preferences.Key<Set<String>>,
        update: (Set<String>) -> Set<String>
    ) {
        context.settingsDataStore.edit { preferences ->
            val current = preferences[key].orEmpty()
            preferences[key] = update(current)
        }
    }

    private fun encryptSecret(value: String): String {
        return runCatching {
            val cipher = Cipher.getInstance(KEY_TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())
            val encrypted = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
            val stored = byteArrayOf(cipher.iv.size.toByte()) + cipher.iv + encrypted
            Base64.encodeToString(stored, Base64.NO_WRAP)
        }.getOrDefault("")
    }

    private fun decryptSecret(value: String): String {
        if (value.isBlank()) return ""
        return runCatching {
            val stored = Base64.decode(value, Base64.NO_WRAP)
            val ivSize = stored.first().toInt() and 0xFF
            require(ivSize > 0 && stored.size > ivSize + 1)
            val iv = stored.copyOfRange(1, ivSize + 1)
            val encrypted = stored.copyOfRange(ivSize + 1, stored.size)
            val cipher = Cipher.getInstance(KEY_TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateSecretKey(), GCMParameterSpec(128, iv))
            String(cipher.doFinal(encrypted), Charsets.UTF_8)
        }.getOrDefault("")
    }

    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (keyStore.getKey(AI_KEY_ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").run {
            init(
                KeyGenParameterSpec.Builder(
                    AI_KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .build()
            )
            generateKey()
        }
    }

    private companion object {
        const val DEFAULT_DEEPSEEK_MODEL = "deepseek-v4-flash"
        const val AI_KEY_ALIAS = "music_player_ai_api_key"
        const val KEY_TRANSFORMATION = "AES/GCM/NoPadding"
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val THEME_STYLE = stringPreferencesKey("theme_style")
        val FONT_FAMILY = stringPreferencesKey("font_family")
        val APP_LANGUAGE = stringPreferencesKey("app_language")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val EDITORIAL_LAYOUT = booleanPreferencesKey("editorial_layout")
        val AUTO_SCAN_LOCAL = booleanPreferencesKey("auto_scan_local")
        val AUTO_SCRAPE = booleanPreferencesKey("auto_scrape")
        val BACKGROUND_AI_SCRAPE = booleanPreferencesKey("background_ai_scrape")
        val AI_CATEGORY_SCRAPE = booleanPreferencesKey("ai_category_scrape")
        val PLAYBACK_MODE = stringPreferencesKey("playback_mode")
        val DESKTOP_LYRICS_ENABLED = booleanPreferencesKey("desktop_lyrics_enabled")
        val AI_PROVIDER = stringPreferencesKey("ai_provider")
        val AI_API_KEY = stringPreferencesKey("ai_api_key")
        val AI_MODEL = stringPreferencesKey("ai_model")
        val ADAPTIVE_EQUALIZER = booleanPreferencesKey("adaptive_equalizer")
        val EQ_CATEGORY_PRESETS = stringSetPreferencesKey("eq_category_presets")
        val LOCAL_SCAN_FOLDERS = stringSetPreferencesKey("local_scan_folders")
        val SCRAPE_CANDIDATES = stringSetPreferencesKey("scrape_candidates")
        val SCRAPE_COMPLETED = intPreferencesKey("scrape_completed")
        val SCRAPE_TOTAL = intPreferencesKey("scrape_total")
        val WEBDAV_FOLDERS = stringSetPreferencesKey("webdav_folders")
    }
}
