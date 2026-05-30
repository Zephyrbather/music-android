package com.musicplayer.app.ui.settings

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.RemoveCircleOutline
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.musicplayer.app.domain.repository.AppLanguage
import com.musicplayer.app.domain.repository.AiProvider
import com.musicplayer.app.domain.repository.FontFamilyOption
import com.musicplayer.app.domain.repository.ThemeMode
import com.musicplayer.app.domain.repository.ThemeStyle
import com.musicplayer.app.domain.usecase.LibraryWorkState
import com.musicplayer.app.domain.usecase.LibraryWorkType
import com.musicplayer.app.ui.i18n.LocalStrings
import com.musicplayer.app.ui.i18n.ThemeModeTitle
import com.musicplayer.app.ui.i18n.ThemeStyleTitle

@Composable
fun SettingsScreen(
    onNavigateToWebDav: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val workState by viewModel.workState.collectAsStateWithLifecycle()
    val hasResumableScrape by viewModel.hasResumableScrape.collectAsStateWithLifecycle()
    val strings = LocalStrings.current
    val context = LocalContext.current
    val folderPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        runCatching { context.contentResolver.takePersistableUriPermission(uri, flags) }
        viewModel.addLocalScanFolder(uri.toString())
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) viewModel.exportData(uri)
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) viewModel.importData(uri)
    }

    val result by viewModel.exportImportResult.collectAsStateWithLifecycle()
    LaunchedEffect(result) {
        result?.let { key ->
            val msg = when (key) {
                "export_success" -> strings.exportSuccess
                "import_success" -> strings.importSuccess
                else -> "Failed"
            }
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.clearExportImportResult()
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        item {
            Text(
                text = strings.settings,
                style = MaterialTheme.typography.headlineSmall
            )
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(text = strings.language, style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppLanguage.values().forEach { language ->
                        FilterChip(
                            selected = state.appLanguage == language,
                            onClick = { viewModel.setAppLanguage(language) },
                            label = {
                                Text(
                                    when (language) {
                                        AppLanguage.CHINESE -> strings.chinese
                                        AppLanguage.ENGLISH -> strings.english
                                    }
                                )
                            }
                        )
                    }
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(text = strings.theme, style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemeMode.values().forEach { mode ->
                        FilterChip(
                            selected = state.themeMode == mode,
                            onClick = { viewModel.setThemeMode(mode) },
                            label = { Text(ThemeModeTitle(mode, strings)) }
                        )
                    }
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(ThemeStyle.values()) { style ->
                        FilterChip(
                            selected = state.themeStyle == style,
                            onClick = { viewModel.setThemeStyle(style) },
                            label = { Text(ThemeStyleTitle(style, strings)) }
                        )
                    }
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(text = strings.fontSetting, style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FontFamilyOption.values().forEach { font ->
                        FilterChip(
                            selected = state.fontFamilyOption == font,
                            onClick = { viewModel.setFontFamilyOption(font) },
                            label = {
                                Text(
                                    when (font) {
                                        FontFamilyOption.DEFAULT -> strings.fontDefault
                                        FontFamilyOption.SERIF -> strings.fontSerif
                                        FontFamilyOption.MONOSPACE -> strings.fontMonospace
                                    }
                                )
                            }
                        )
                    }
                }
            }
        }
        item {
            SettingSwitchRow(
                title = strings.dynamicColor,
                description = strings.dynamicColorDesc,
                checked = state.dynamicColor,
                onCheckedChange = viewModel::setDynamicColor
            )
        }
        item {
            SettingSwitchRow(
                title = strings.editorialLayout,
                description = strings.editorialLayoutDesc,
                checked = state.editorialLayout,
                onCheckedChange = viewModel::setEditorialLayout
            )
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(text = strings.localLibrary, style = MaterialTheme.typography.titleMedium)
                SettingSwitchRow(
                    title = strings.autoScan,
                    description = strings.autoScanDesc,
                    checked = state.autoScanLocalLibrary,
                    onCheckedChange = viewModel::setAutoScanLocalLibrary
                )
                Button(
                    onClick = viewModel::scanNow,
                    enabled = !workState.running,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(strings.scan)
                }
                if (workState.type == LibraryWorkType.SCAN && (workState.running || workState.lastResult != null)) {
                    WorkProgressPanel(
                        workState = workState,
                        runningText = strings.scanRunning,
                        finishedText = "${strings.scanFinishedPrefix}${workState.discovered}${strings.scanFinishedSuffix}"
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = strings.scanFolders,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Button(onClick = { folderPicker.launch(null) }) {
                        Icon(imageVector = Icons.Outlined.FolderOpen, contentDescription = null)
                        Text(text = strings.addFolder)
                    }
                }
                if (state.localScanFolderUris.isEmpty()) {
                    Text(
                        text = strings.noFolders,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    state.localScanFolderUris.forEach { folderUri ->
                        FolderRow(
                            uri = folderUri,
                            removeLabel = strings.remove,
                            onRemove = {
                                runCatching {
                                    context.contentResolver.releasePersistableUriPermission(
                                        Uri.parse(folderUri),
                                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                                    )
                                }
                                viewModel.removeLocalScanFolder(folderUri)
                            }
                        )
                    }
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = strings.scraping, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = strings.scrapingDesc,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = strings.scrapeStorageHint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                AssistChip(onClick = {}, label = { Text(strings.lyricsSources) })
                AssistChip(onClick = {}, label = { Text(strings.artworkSources) })
                Text(text = strings.aiScraping, style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AiProvider.values().forEach { provider ->
                        FilterChip(
                            selected = state.aiProvider == provider,
                            onClick = {},
                            label = {
                                Text(
                                    when (provider) {
                                        AiProvider.DEEPSEEK -> "DeepSeek"
                                    }
                                )
                            }
                        )
                    }
                }
                var apiKey by remember(state.aiApiKey) { mutableStateOf(state.aiApiKey) }
                var aiModel by remember(state.aiModel) { mutableStateOf(state.aiModel) }
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    label = { Text(strings.aiApiKey) }
                )
                OutlinedTextField(
                    value = aiModel,
                    onValueChange = { aiModel = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(strings.aiModel) }
                )
                OutlinedButton(
                    onClick = { viewModel.saveAiSettings(AiProvider.DEEPSEEK, apiKey, aiModel) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(strings.saveAiSettings)
                }
                SettingSwitchRow(
                    title = strings.autoScrape,
                    description = strings.autoScrapeDesc,
                    checked = state.autoScrape,
                    onCheckedChange = viewModel::setAutoScrape
                )
                SettingSwitchRow(
                    title = strings.backgroundAiScrape,
                    description = strings.backgroundAiScrapeDesc,
                    checked = state.backgroundAiScrape,
                    onCheckedChange = viewModel::setBackgroundAiScrape,
                    enabled = state.aiApiKey.isNotBlank() || state.backgroundAiScrape
                )
                Button(
                    onClick = viewModel::scrapeMissingMetadata,
                    enabled = !workState.running,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(strings.scrapeAllMissing)
                }
                if (hasResumableScrape && !workState.running) {
                    Button(
                        onClick = viewModel::resumeScrape,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(strings.scrapeResume)
                    }
                }
                if (workState.type == LibraryWorkType.SCRAPE && (workState.running || workState.lastResult != null)) {
                    val finishedSkipped = (workState.total - workState.updated - workState.failed).coerceAtLeast(0)
                    val runningSkipped = (workState.completed - workState.updated - workState.failed).coerceAtLeast(0)
                    val finishedText = "${strings.scrapeFinishedPrefix}${workState.updated}" +
                        "${strings.scrapeFinishedSkippedMiddle}${finishedSkipped}" +
                        "${strings.scrapeFinishedMiddle}${workState.failed}${strings.scrapeFinishedSuffix}"
                    WorkProgressPanel(
                        workState = workState,
                        runningText = strings.scrapeRunning,
                        finishedText = finishedText,
                        supportingText = if (workState.running) {
                            "${strings.scrapeBackgroundHint}\n${strings.scrapeFinishedPrefix}${workState.updated}" +
                                "${strings.scrapeFinishedSkippedMiddle}${runningSkipped}" +
                                "${strings.scrapeFinishedMiddle}${workState.failed}${strings.scrapeFinishedSuffix}"
                        } else {
                            null
                        }
                    )
                }
            }
        }
        item {
            val imageSize by viewModel.imageCacheSize.collectAsStateWithLifecycle()
            val artworkSize by viewModel.artworkCacheSize.collectAsStateWithLifecycle()
            val scrapeCount by viewModel.scrapeCacheEntries.collectAsStateWithLifecycle()
            val clearing by viewModel.clearingCache.collectAsStateWithLifecycle()
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(text = strings.cache, style = MaterialTheme.typography.titleMedium)
                CacheRow(
                    label = strings.imageCache,
                    value = imageSize ?: "...",
                    onClear = viewModel::clearImageCache,
                    enabled = imageSize != "0 B" && imageSize != null && imageSize != "..."
                )
                CacheRow(
                    label = strings.artworkCache,
                    value = artworkSize ?: "...",
                    onClear = viewModel::clearArtworkCache,
                    enabled = artworkSize != "0 B" && artworkSize != null && artworkSize != "..."
                )
                CacheRow(
                    label = strings.scrapeCache,
                    value = scrapeCount?.let { "$it${strings.entries}" } ?: "...",
                    onClear = viewModel::clearScrapeCache,
                    enabled = (scrapeCount ?: 0) > 0
                )
                Button(
                    onClick = viewModel::clearAllCache,
                    enabled = !clearing,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (clearing) strings.clearingCache else strings.clearCache)
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(text = strings.dataManagement, style = MaterialTheme.typography.titleMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { exportLauncher.launch("musicplayer_backup.json") },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.FileUpload,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(strings.exportData)
                    }
                    OutlinedButton(
                        onClick = { importLauncher.launch(arrayOf("application/json")) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.FileDownload,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(strings.importData)
                    }
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = strings.webdav, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = strings.webdavDesc,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    onClick = onNavigateToWebDav,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(strings.manageWebDav)
                }
            }
        }
    }
}

@Composable
private fun CacheRow(
    label: String,
    value: String,
    onClear: () -> Unit,
    enabled: Boolean
) {
    val strings = LocalStrings.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, style = MaterialTheme.typography.titleMedium)
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Button(onClick = onClear, enabled = enabled) {
            Text(strings.clearCategory)
        }
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled
        )
    }
}

@Composable
private fun FolderRow(
    uri: String,
    removeLabel: String,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = Icons.Outlined.FolderOpen,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = folderLabel(uri),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        IconButton(onClick = onRemove) {
            Icon(
                imageVector = Icons.Outlined.RemoveCircleOutline,
                contentDescription = removeLabel
            )
        }
    }
}

private fun folderLabel(uri: String): String {
    val segment = Uri.parse(uri).lastPathSegment.orEmpty()
    return Uri.decode(segment.substringAfterLast(':').substringAfterLast('/')).ifBlank { uri }
}

@Composable
private fun WorkProgressPanel(
    workState: LibraryWorkState,
    runningText: String,
    finishedText: String,
    supportingText: String? = null
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (workState.running) {
                if (workState.progress == null) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                } else {
                    LinearProgressIndicator(
                        progress = { workState.progress ?: 0f },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Text(
                    text = if (workState.total > 0) {
                        "$runningText ${workState.completed}/${workState.total}"
                    } else {
                        runningText
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
                supportingText?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Text(
                    text = finishedText,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
