package com.musicplayer.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.musicplayer.app.domain.repository.FontFamilyOption
import com.musicplayer.app.domain.repository.ThemeMode
import com.musicplayer.app.domain.repository.ThemeStyle

@Composable
fun MusicPlayerTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    themeStyle: ThemeStyle = ThemeStyle.CLASSIC,
    dynamicColor: Boolean = false,
    currentArtworkUri: String? = null,
    fontFamilyOption: FontFamilyOption = FontFamilyOption.DEFAULT,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && darkTheme -> dynamicDarkColorScheme(context)
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> dynamicLightColorScheme(context)
        themeStyle == ThemeStyle.IMMERSIVE -> {
            val (light, dark) = remember(currentArtworkUri) {
                ImmersiveColorExtractor.extract(currentArtworkUri, context)
            }
            if (darkTheme) dark else light
        }
        darkTheme -> darkColors(themeStyle)
        else -> lightColors(themeStyle)
    }
    val typography = when (fontFamilyOption) {
        FontFamilyOption.DEFAULT -> DefaultTypography
        FontFamilyOption.SERIF -> SerifTypography
        FontFamilyOption.MONOSPACE -> MonospaceTypography
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
        content = content
    )
}

private fun lightColors(style: ThemeStyle) = when (style) {
    ThemeStyle.IMMERSIVE, ThemeStyle.CLASSIC -> lightColorScheme(
        primary = Vermilion,
        onPrimary = Paper,
        primaryContainer = ColorTokens.vermilionContainer,
        onPrimaryContainer = Ink,
        secondary = Moss,
        onSecondary = Paper,
        secondaryContainer = ColorTokens.mossContainer,
        onSecondaryContainer = Ink,
        tertiary = Sky,
        surface = Paper,
        onSurface = Ink,
        surfaceVariant = SoftGray,
        onSurfaceVariant = ColorTokens.mutedInk,
        background = Paper
    )

    ThemeStyle.OCEAN -> lightColorScheme(
        primary = OceanBlue,
        onPrimary = Paper,
        primaryContainer = ColorTokens.oceanContainer,
        onPrimaryContainer = Ink,
        secondary = Coral,
        onSecondary = Paper,
        secondaryContainer = ColorTokens.coralContainer,
        onSecondaryContainer = Ink,
        tertiary = Moss,
        surface = ColorTokens.oceanSurface,
        onSurface = Ink,
        surfaceVariant = ColorTokens.oceanSurfaceVariant,
        onSurfaceVariant = ColorTokens.mutedInk,
        background = ColorTokens.oceanSurface
    )

    ThemeStyle.FOREST -> lightColorScheme(
        primary = Forest,
        onPrimary = Paper,
        primaryContainer = ColorTokens.forestContainer,
        onPrimaryContainer = Ink,
        secondary = Brass,
        onSecondary = Ink,
        secondaryContainer = ColorTokens.brassContainer,
        onSecondaryContainer = Ink,
        tertiary = Sky,
        surface = ColorTokens.forestSurface,
        onSurface = Ink,
        surfaceVariant = ColorTokens.forestSurfaceVariant,
        onSurfaceVariant = ColorTokens.mutedInk,
        background = ColorTokens.forestSurface
    )

    ThemeStyle.ROSE -> lightColorScheme(
        primary = Rose,
        onPrimary = Paper,
        primaryContainer = ColorTokens.roseContainer,
        onPrimaryContainer = Ink,
        secondary = Plum,
        onSecondary = Paper,
        secondaryContainer = ColorTokens.plumContainer,
        onSecondaryContainer = Ink,
        tertiary = Brass,
        surface = ColorTokens.roseSurface,
        onSurface = Ink,
        surfaceVariant = ColorTokens.roseSurfaceVariant,
        onSurfaceVariant = ColorTokens.mutedInk,
        background = ColorTokens.roseSurface
    )

    ThemeStyle.MONO -> lightColorScheme(
        primary = MonoPrimary,
        onPrimary = Paper,
        primaryContainer = ColorTokens.monoContainer,
        onPrimaryContainer = Ink,
        secondary = MonoSecondary,
        onSecondary = Paper,
        secondaryContainer = ColorTokens.monoSurfaceVariant,
        onSecondaryContainer = Ink,
        tertiary = Brass,
        surface = ColorTokens.monoSurface,
        onSurface = Ink,
        surfaceVariant = ColorTokens.monoSurfaceVariant,
        onSurfaceVariant = ColorTokens.mutedInk,
        background = ColorTokens.monoSurface
    )
}

private fun darkColors(style: ThemeStyle) = when (style) {
    ThemeStyle.IMMERSIVE, ThemeStyle.CLASSIC -> darkColorScheme(
        primary = ColorTokens.vermilionDark,
        onPrimary = Ink,
        primaryContainer = Vermilion,
        onPrimaryContainer = Paper,
        secondary = ColorTokens.mossDark,
        onSecondary = Ink,
        tertiary = ColorTokens.skyDark,
        surface = PaperDark,
        onSurface = ColorTokens.lightInk,
        surfaceVariant = ColorTokens.darkSurfaceVariant,
        onSurfaceVariant = ColorTokens.darkMutedInk,
        background = PaperDark
    )

    ThemeStyle.OCEAN -> darkColorScheme(
        primary = ColorTokens.oceanDark,
        onPrimary = Ink,
        primaryContainer = OceanBlue,
        onPrimaryContainer = Paper,
        secondary = ColorTokens.coralDark,
        onSecondary = Ink,
        secondaryContainer = Coral,
        onSecondaryContainer = Paper,
        tertiary = ColorTokens.mossDark,
        surface = ColorTokens.oceanDarkSurface,
        onSurface = ColorTokens.lightInk,
        surfaceVariant = ColorTokens.oceanDarkSurfaceVariant,
        onSurfaceVariant = ColorTokens.darkMutedInk,
        background = ColorTokens.oceanDarkSurface
    )

    ThemeStyle.FOREST -> darkColorScheme(
        primary = ColorTokens.forestDark,
        onPrimary = Ink,
        primaryContainer = Forest,
        onPrimaryContainer = Paper,
        secondary = ColorTokens.brassDark,
        onSecondary = Ink,
        secondaryContainer = Brass,
        onSecondaryContainer = Ink,
        tertiary = ColorTokens.skyDark,
        surface = ColorTokens.forestDarkSurface,
        onSurface = ColorTokens.lightInk,
        surfaceVariant = ColorTokens.forestDarkSurfaceVariant,
        onSurfaceVariant = ColorTokens.darkMutedInk,
        background = ColorTokens.forestDarkSurface
    )

    ThemeStyle.ROSE -> darkColorScheme(
        primary = ColorTokens.roseDark,
        onPrimary = Ink,
        primaryContainer = Rose,
        onPrimaryContainer = Paper,
        secondary = ColorTokens.plumDark,
        onSecondary = Ink,
        secondaryContainer = Plum,
        onSecondaryContainer = Paper,
        tertiary = ColorTokens.brassDark,
        surface = ColorTokens.roseDarkSurface,
        onSurface = ColorTokens.lightInk,
        surfaceVariant = ColorTokens.roseDarkSurfaceVariant,
        onSurfaceVariant = ColorTokens.darkMutedInk,
        background = ColorTokens.roseDarkSurface
    )

    ThemeStyle.MONO -> darkColorScheme(
        primary = ColorTokens.monoDark,
        onPrimary = Ink,
        primaryContainer = MonoPrimary,
        onPrimaryContainer = Paper,
        secondary = ColorTokens.monoSecondaryDark,
        onSecondary = Ink,
        secondaryContainer = MonoSecondary,
        onSecondaryContainer = Paper,
        tertiary = ColorTokens.brassDark,
        surface = ColorTokens.monoDarkSurface,
        onSurface = ColorTokens.lightInk,
        surfaceVariant = ColorTokens.monoDarkSurfaceVariant,
        onSurfaceVariant = ColorTokens.darkMutedInk,
        background = ColorTokens.monoDarkSurface
    )
}

private object ColorTokens {
    val vermilionContainer = androidx.compose.ui.graphics.Color(0xFFF0CFC5)
    val mossContainer = androidx.compose.ui.graphics.Color(0xFFD5E2C6)
    val mutedInk = androidx.compose.ui.graphics.Color(0xFF6D635D)
    val vermilionDark = androidx.compose.ui.graphics.Color(0xFFFFB4A1)
    val mossDark = androidx.compose.ui.graphics.Color(0xFFBDD0B1)
    val skyDark = androidx.compose.ui.graphics.Color(0xFFAEC8E5)
    val lightInk = androidx.compose.ui.graphics.Color(0xFFF2EDE4)
    val darkSurfaceVariant = androidx.compose.ui.graphics.Color(0xFF2A2622)
    val darkMutedInk = androidx.compose.ui.graphics.Color(0xFFCFC5B9)
    val oceanContainer = androidx.compose.ui.graphics.Color(0xFFC8E6F0)
    val oceanSurface = androidx.compose.ui.graphics.Color(0xFFF4FAFC)
    val oceanSurfaceVariant = androidx.compose.ui.graphics.Color(0xFFDCEAF0)
    val oceanDark = androidx.compose.ui.graphics.Color(0xFFA9D8EA)
    val oceanDarkSurface = androidx.compose.ui.graphics.Color(0xFF10191D)
    val oceanDarkSurfaceVariant = androidx.compose.ui.graphics.Color(0xFF203038)
    val coralContainer = androidx.compose.ui.graphics.Color(0xFFFFD4C7)
    val coralDark = androidx.compose.ui.graphics.Color(0xFFFFB49E)
    val forestContainer = androidx.compose.ui.graphics.Color(0xFFCFE4CD)
    val forestSurface = androidx.compose.ui.graphics.Color(0xFFF5FAF2)
    val forestSurfaceVariant = androidx.compose.ui.graphics.Color(0xFFDDE8D7)
    val forestDark = androidx.compose.ui.graphics.Color(0xFFB4D7AD)
    val forestDarkSurface = androidx.compose.ui.graphics.Color(0xFF121A13)
    val forestDarkSurfaceVariant = androidx.compose.ui.graphics.Color(0xFF223023)
    val brassContainer = androidx.compose.ui.graphics.Color(0xFFEADDB9)
    val brassDark = androidx.compose.ui.graphics.Color(0xFFE0C77C)
    val roseContainer = androidx.compose.ui.graphics.Color(0xFFFFD6DE)
    val roseSurface = androidx.compose.ui.graphics.Color(0xFFFFF7F8)
    val roseSurfaceVariant = androidx.compose.ui.graphics.Color(0xFFF4DDE2)
    val roseDark = androidx.compose.ui.graphics.Color(0xFFFFB2C3)
    val roseDarkSurface = androidx.compose.ui.graphics.Color(0xFF1D1316)
    val roseDarkSurfaceVariant = androidx.compose.ui.graphics.Color(0xFF352329)
    val plumContainer = androidx.compose.ui.graphics.Color(0xFFE5D3F2)
    val plumDark = androidx.compose.ui.graphics.Color(0xFFD2B5EA)
    val monoContainer = androidx.compose.ui.graphics.Color(0xFFD9DDE2)
    val monoSurface = androidx.compose.ui.graphics.Color(0xFFF8F9FA)
    val monoSurfaceVariant = androidx.compose.ui.graphics.Color(0xFFE2E4E8)
    val monoDark = androidx.compose.ui.graphics.Color(0xFFC7CDD4)
    val monoSecondaryDark = androidx.compose.ui.graphics.Color(0xFFB6C1C8)
    val monoDarkSurface = androidx.compose.ui.graphics.Color(0xFF121416)
    val monoDarkSurfaceVariant = androidx.compose.ui.graphics.Color(0xFF25292D)
}
