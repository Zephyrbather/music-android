package com.musicplayer.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Album
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.HealthAndSafety
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.musicplayer.app.domain.model.MetadataIssue
import com.musicplayer.app.domain.model.MetadataIssueType
import com.musicplayer.app.ui.adaptive.LocalPadLayout
import com.musicplayer.app.ui.i18n.ExtraStrings
import com.musicplayer.app.ui.i18n.LocalExtraStrings
import com.musicplayer.app.ui.i18n.LocalStrings

private sealed interface DuplicateCleanup {
    data class Single(val issue: MetadataIssue) : DuplicateCleanup
    data class All(val count: Int) : DuplicateCleanup
}

@Composable
fun MetadataHealthScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MetadataHealthViewModel = hiltViewModel()
) {
    val strings = LocalStrings.current
    val extra = LocalExtraStrings.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var pendingDuplicateAction by remember { mutableStateOf<DuplicateCleanup?>(null) }

    LaunchedEffect(Unit) {
        if (!state.hasChecked) {
            viewModel.check(
                titleFallback = extra.metadataFallbackTitle,
                artistFallback = extra.metadataFallbackArtist,
                albumFallback = extra.metadataFallbackAlbum
            )
        }
    }
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            val message = when (event) {
                is MetadataHealthEvent.Fixed -> listOfNotNull(
                    event.repaired.takeIf { it > 0 }
                        ?.let { extra.metadataFixedFormat.format(it) },
                    event.removed.takeIf { it > 0 }
                        ?.let { extra.metadataRemovedFormat.format(it) }
                ).joinToString("，")
            }
            if (message.isNotEmpty()) snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = if (LocalPadLayout.current) {
                    Modifier
                        .fillMaxHeight()
                        .widthIn(max = 860.dp)
                } else {
                    Modifier.fillMaxSize()
                },
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.Outlined.ArrowBack,
                                contentDescription = null
                            )
                        }
                        Text(
                            text = extra.metadataHealth,
                            style = MaterialTheme.typography.headlineSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                item {
                    Surface(
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.HealthAndSafety,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(30.dp)
                                )
                                Text(
                                    text = extra.metadataHealthHint,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Button(
                                    onClick = {
                                        viewModel.check(
                                            titleFallback = extra.metadataFallbackTitle,
                                            artistFallback = extra.metadataFallbackArtist,
                                            albumFallback = extra.metadataFallbackAlbum
                                        )
                                    },
                                    enabled = !state.checking && !state.repairing
                                ) {
                                    Text(
                                        if (state.hasChecked) extra.metadataCheckAgain
                                        else extra.runMetadataCheck
                                    )
                                }
                                if (state.checking || state.repairing) {
                                    CircularProgressIndicator(modifier = Modifier.size(22.dp))
                                    Text(
                                        text = if (state.repairing) extra.metadataFixing
                                        else extra.metadataChecking,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }
                }
                if (state.hasChecked && state.issues.isEmpty() && !state.checking) {
                    item {
                        Surface(
                            shape = MaterialTheme.shapes.large,
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = extra.metadataNoIssues,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                    }
                }
                if (state.issues.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = extra.metadataIssueSummaryFormat.format(state.issues.size),
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedButton(
                                onClick = {
                                    val duplicateCount = state.issues
                                        .filter { it.type == MetadataIssueType.DUPLICATE }
                                        .sumOf { it.duplicateIds.size }
                                    if (duplicateCount > 0) {
                                        pendingDuplicateAction = DuplicateCleanup.All(duplicateCount)
                                    } else {
                                        viewModel.fixAll()
                                    }
                                },
                                enabled = !state.repairing && !state.checking
                            ) {
                                Text(extra.metadataFixAll)
                            }
                        }
                    }
                    val grouped = state.issues.groupBy { it.type }
                    MetadataIssueType.entries.forEach { type ->
                        val typeIssues = grouped[type].orEmpty()
                        if (typeIssues.isNotEmpty()) {
                            item(key = "header-$type") {
                                MetadataSectionHeader(
                                    title = type.title(extra),
                                    icon = type.icon(),
                                    count = typeIssues.size
                                )
                            }
                            items(typeIssues, key = { "${it.type}-${it.songId}-${it.duplicateIds.joinToString(",")}" }) { issue ->
                                MetadataIssueRow(
                                    issue = issue,
                                    detail = issue.detail(extra),
                                    repairEnabled = !state.repairing && !state.checking,
                                    onFix = {
                                        if (issue.type == MetadataIssueType.DUPLICATE) {
                                            pendingDuplicateAction = DuplicateCleanup.Single(issue)
                                        } else {
                                            viewModel.fix(issue)
                                        }
                                    },
                                    fixLabel = extra.metadataFix
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    pendingDuplicateAction?.let { action ->
        val count = when (action) {
            is DuplicateCleanup.Single -> action.issue.duplicateIds.size
            is DuplicateCleanup.All -> action.count
        }
        AlertDialog(
            onDismissRequest = { pendingDuplicateAction = null },
            title = { Text(extra.metadataDuplicateConfirmTitle) },
            text = { Text(extra.metadataDuplicateConfirmFormat.format(count)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        when (action) {
                            is DuplicateCleanup.Single -> viewModel.fix(action.issue)
                            is DuplicateCleanup.All -> viewModel.fixAll()
                        }
                        pendingDuplicateAction = null
                    }
                ) {
                    Text(extra.metadataCleanAction)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDuplicateAction = null }) {
                    Text(strings.cancel)
                }
            }
        )
    }
}

@Composable
private fun MetadataSectionHeader(
    title: String,
    icon: ImageVector,
    count: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun MetadataIssueRow(
    issue: MetadataIssue,
    detail: String,
    repairEnabled: Boolean,
    onFix: () -> Unit,
    fixLabel: String
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = issue.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${issue.artistName} · ${issue.albumTitle}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = detail,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            TextButton(onClick = onFix, enabled = repairEnabled) {
                Text(fixLabel)
            }
        }
    }
}

private fun MetadataIssueType.title(extra: ExtraStrings): String {
    return when (this) {
        MetadataIssueType.SWAPPED_NAMES -> extra.metadataSwappedNames
        MetadataIssueType.UUID_ALBUM -> extra.metadataUuidAlbums
        MetadataIssueType.INVALID_TEXT -> extra.metadataInvalidText
        MetadataIssueType.DUPLICATE -> extra.metadataDuplicates
    }
}

private fun MetadataIssue.detail(extra: ExtraStrings): String {
    return when (type) {
        MetadataIssueType.SWAPPED_NAMES -> extra.metadataSwappedDetail
        MetadataIssueType.UUID_ALBUM -> extra.metadataUuidDetailFormat.format(proposedAlbum)
        MetadataIssueType.INVALID_TEXT -> extra.metadataInvalidDetail
        MetadataIssueType.DUPLICATE -> extra.metadataDuplicateDetailFormat.format(duplicateCount)
    }
}

private fun MetadataIssueType.icon(): ImageVector {
    return when (this) {
        MetadataIssueType.SWAPPED_NAMES -> Icons.Outlined.SwapHoriz
        MetadataIssueType.UUID_ALBUM -> Icons.Outlined.Album
        MetadataIssueType.INVALID_TEXT -> Icons.Outlined.TextFields
        MetadataIssueType.DUPLICATE -> Icons.Outlined.ContentCopy
    }
}
