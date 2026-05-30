package com.musicplayer.app.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.widget.Toast
import com.musicplayer.app.data.webdav.WebDavItem
import com.musicplayer.app.domain.model.WebDavServer
import com.musicplayer.app.ui.i18n.LocalStrings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebDavBrowserScreen(
    serverId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WebDavViewModel = hiltViewModel()
) {
    val strings = LocalStrings.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val server = state.servers.firstOrNull { it.id == serverId }

    var currentPath by remember { mutableStateOf("") }
    val items = remember { mutableStateListOf<WebDavItem>() }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val selectedFolders by viewModel.webDavFolderUris.collectAsStateWithLifecycle()
    val syncingServerId by viewModel.syncingServerId.collectAsStateWithLifecycle()
    val syncProgress by viewModel.syncProgress.collectAsStateWithLifecycle()
    val serverFolders = remember(selectedFolders, serverId) {
        selectedFolders.filter { it.startsWith("$serverId|") }.map { it.removePrefix("$serverId|") }.toSet()
    }
    val isSyncing = syncingServerId == serverId

    LaunchedEffect(syncProgress.finished) {
        if (syncProgress.finished) {
            val msg = if (syncProgress.songsFound > 0) {
                "同步完成！找到 ${syncProgress.songsFound} 首歌曲"
            } else if (syncProgress.errors.isNotEmpty()) {
                "同步失败：${syncProgress.errors.first()}"
            } else {
                "未找到音频文件"
            }
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(server, currentPath) {
        if (server == null) return@LaunchedEffect
        loading = true
        error = null
        try {
            items.clear()
            items.addAll(viewModel.listDirectory(server, currentPath))
        } catch (e: Exception) {
            error = e.message
        }
        loading = false
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = server?.name ?: strings.webdav,
                            style = MaterialTheme.typography.titleMedium
                        )
                        if (currentPath.isNotEmpty()) {
                            Text(
                                text = currentPath,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (currentPath.isEmpty()) onBack()
                        else {
                            currentPath = currentPath.substringBeforeLast('/', "").ifBlank { "" }
                        }
                    }) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = strings.close)
                    }
                }
            )
        }
    ) { innerPadding ->
        if (server == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(strings.serverNotFound, color = MaterialTheme.colorScheme.error)
            }
            return@Scaffold
        }

        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            if (serverFolders.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Outlined.FolderOpen,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "${strings.selectedFolders}: ${serverFolders.size}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(
                        onClick = {
                            server?.let { srv ->
                                Toast.makeText(context, "开始同步 ${serverFolders.size} 个文件夹…", Toast.LENGTH_SHORT).show()
                                viewModel.syncFolders(srv, serverFolders.toList())
                            }
                        },
                        enabled = !isSyncing,
                        modifier = Modifier.size(32.dp)
                    ) {
                        if (isSyncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                Icons.Outlined.Sync,
                                contentDescription = strings.syncFolders,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            if (isSyncing && syncProgress.totalFolders > 0) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${strings.syncing} ${syncProgress.currentIndex + 1}/${syncProgress.totalFolders}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "${strings.songs}: ${syncProgress.songsFound}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = syncProgress.currentFolder,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = {
                            if (syncProgress.totalFolders > 0)
                                syncProgress.currentIndex.toFloat() / syncProgress.totalFolders
                            else 0f
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            if (!isSyncing && syncProgress.finished && syncProgress.songsFound > 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Outlined.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "同步完成，找到 ${syncProgress.songsFound} 首歌曲",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            syncProgress.errors.forEach { err ->
                Text(
                    text = err,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (loading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (error != null) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(error!!, color = MaterialTheme.colorScheme.error)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(8.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(items, key = { it.uri }) { item ->
                        val folderKey = item.uri.removePrefix(server.baseUrl.trimEnd('/')).trimStart('/').trimEnd('/').let {
                            it.ifEmpty { "/" }
                        }
                        val isSelected = serverFolders.contains(folderKey)
                        ListItem(
                            headlineContent = {
                                Text(
                                    text = item.name,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            leadingContent = {
                                Icon(
                                    imageVector = when {
                                        item.isDirectory && isSelected -> Icons.Outlined.FolderOpen
                                        item.isDirectory -> Icons.Outlined.Folder
                                        item.name.endsWithAudioExtension() -> Icons.Outlined.MusicNote
                                        else -> Icons.Outlined.InsertDriveFile
                                    },
                                    contentDescription = null,
                                    tint = when {
                                        isSelected -> MaterialTheme.colorScheme.primary
                                        item.isDirectory -> MaterialTheme.colorScheme.primary
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            },
                            supportingContent = {
                                if (!item.isDirectory && item.sizeBytes != null) {
                                    Text(
                                        text = formatFileSize(item.sizeBytes),
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            },
                            trailingContent = if (item.isDirectory) {
                                {
                                    IconButton(
                                        onClick = {
                                            if (isSelected) viewModel.removeFolder(server.id, folderKey)
                                            else viewModel.addFolder(server.id, folderKey)
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isSelected) Icons.Outlined.Check
                                            else Icons.Outlined.Add,
                                            contentDescription = if (isSelected) strings.remove else strings.selectFolder,
                                            tint = if (isSelected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            } else null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (item.isDirectory) {
                                        val subPath = item.uri.removePrefix(server.baseUrl.trimEnd('/'))
                                            .trimStart('/').trimEnd('/')
                                        currentPath = subPath
                                    }
                                }
                        )
                    }
                }
            }
        }
    }
}

private fun String.endsWithAudioExtension(): Boolean {
    val lower = lowercase()
    return listOf(".mp3", ".flac", ".m4a", ".aac", ".ogg", ".opus", ".wav", ".dsf", ".dff")
        .any(lower::endsWith)
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        else -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
    }
}
