package com.musicplayer.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.musicplayer.app.domain.model.WebDavServer
import com.musicplayer.app.ui.i18n.LocalStrings
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebDavScreen(
    onBack: () -> Unit,
    onBrowseServer: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: WebDavViewModel = hiltViewModel()
) {
    val strings = LocalStrings.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val showAddDialog by viewModel.showAddDialog.collectAsStateWithLifecycle()
    val editingServer by viewModel.editingServer.collectAsStateWithLifecycle()
    val syncingServerId by viewModel.syncingServerId.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(strings.webdav) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = strings.close)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = viewModel::openAddDialog) {
                Icon(Icons.Outlined.Add, contentDescription = strings.addServer)
            }
        }
    ) { innerPadding ->
        if (state.servers.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Cloud,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = strings.webdavDesc,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(state.servers, key = { it.id }) { server ->
                    ServerCard(
                        server = server,
                        syncing = syncingServerId == server.id,
                        onToggleEnabled = { viewModel.toggleEnabled(server) },
                        onSync = { viewModel.syncServer(server.id) },
                        onEdit = { viewModel.editServer(server) },
                        onDelete = { viewModel.deleteServer(server.id) },
                        onBrowse = { onBrowseServer(server.id) },
                        strings = strings
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        ServerDialog(
            server = editingServer,
            onDismiss = viewModel::dismissAddDialog,
            onSave = viewModel::upsertServer,
            strings = strings
        )
    }
}

@Composable
private fun ServerCard(
    server: WebDavServer,
    syncing: Boolean,
    onToggleEnabled: () -> Unit,
    onSync: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onBrowse: () -> Unit,
    strings: com.musicplayer.app.ui.i18n.AppStrings
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = server.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = server.baseUrl,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Switch(
                    checked = server.enabled,
                    onCheckedChange = { onToggleEnabled() }
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${strings.lastSync}: ${
                        server.lastSyncEpochMs?.let {
                            SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(it))
                        } ?: strings.neverSynced
                    }",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = onSync, enabled = !syncing, modifier = Modifier.size(36.dp)) {
                    if (syncing) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Outlined.Sync, contentDescription = strings.sync)
                    }
                }
                IconButton(onClick = onBrowse, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Outlined.FolderOpen, contentDescription = strings.browseFiles)
                }
                IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Outlined.Edit, contentDescription = strings.savePreset)
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = strings.deleteServer,
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun ServerDialog(
    server: WebDavServer?,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String) -> Unit,
    strings: com.musicplayer.app.ui.i18n.AppStrings
) {
    var name by remember(server) { mutableStateOf(server?.name.orEmpty()) }
    var baseUrl by remember(server) { mutableStateOf(server?.baseUrl.orEmpty()) }
    var username by remember(server) { mutableStateOf(server?.username.orEmpty()) }
    var password by remember(server) { mutableStateOf(server?.password.orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (server != null) strings.manageWebDav else strings.addServer) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(strings.serverName) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = baseUrl,
                    onValueChange = { baseUrl = it },
                    label = { Text(strings.serverUrl) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text(strings.username) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(strings.password) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name, baseUrl, username, password) },
                enabled = name.isNotBlank() && baseUrl.isNotBlank()
            ) {
                Text(strings.saveComment)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancel)
            }
        }
    )
}
