package com.musicplayer.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun FastScrollIndex(
    letters: List<String>,
    selectedLetter: String?,
    onLetterSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        letters.forEach { letter ->
            val selected = letter == selectedLetter
            Surface(
                modifier = Modifier.clickable { onLetterSelected(letter) },
                shape = MaterialTheme.shapes.small,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
            ) {
                Text(
                    text = letter,
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}
