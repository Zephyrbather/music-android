package com.musicplayer.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val baseTextStyles = mapOf(
    "displaySmall" to { font: FontFamily -> TextStyle(fontFamily = font, fontWeight = FontWeight.Bold, fontSize = 34.sp, lineHeight = 40.sp) },
    "headlineSmall" to { font: FontFamily -> TextStyle(fontFamily = font, fontWeight = FontWeight.Bold, fontSize = 26.sp, lineHeight = 32.sp) },
    "titleLarge" to { font: FontFamily -> TextStyle(fontFamily = font, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp) },
    "titleMedium" to { font: FontFamily -> TextStyle(fontFamily = font, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp) },
    "bodyMedium" to { font: FontFamily -> TextStyle(fontFamily = font, fontSize = 14.sp, lineHeight = 20.sp) },
    "labelMedium" to { font: FontFamily -> TextStyle(fontFamily = font, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp) }
)

private fun buildTypography(font: FontFamily) = Typography(
    displaySmall = baseTextStyles["displaySmall"]!!(font),
    headlineSmall = baseTextStyles["headlineSmall"]!!(font),
    titleLarge = baseTextStyles["titleLarge"]!!(font),
    titleMedium = baseTextStyles["titleMedium"]!!(font),
    bodyMedium = baseTextStyles["bodyMedium"]!!(font),
    labelMedium = baseTextStyles["labelMedium"]!!(font)
)

val DefaultTypography = buildTypography(FontFamily.Default)
val SerifTypography = buildTypography(FontFamily.Serif)
val MonospaceTypography = buildTypography(FontFamily.Monospace)
