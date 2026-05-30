package com.musicplayer.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.musicplayer.app.domain.model.Source

@Composable
fun SourceBadge(
    source: Source,
    modifier: Modifier = Modifier
) {
    AssistChip(
        onClick = {},
        modifier = modifier,
        label = { Text(source.label) },
        leadingIcon = {
            Icon(
                imageVector = if (source == Source.WEBDAV) Icons.Outlined.Cloud else Icons.Outlined.Storage,
                contentDescription = null,
                modifier = Modifier
            )
        }
    )
}
