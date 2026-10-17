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
    // Neon palettes only read correctly on a dark base, regardless of the system setting.
    val resolvedDark = darkTheme ||
        themeStyle == ThemeStyle.CYBERPUNK ||
        themeStyle == ThemeStyle.AURORA ||
        themeStyle == ThemeStyle.SUNSET ||
        themeStyle == ThemeStyle.LAVA
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && resolvedDark -> dynamicDarkColorScheme(context)
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> dynamicLightColorScheme(context)
        themeStyle == ThemeStyle.IMMERSIVE -> {
            val (light, dark) = remember(currentArtworkUri) {
                ImmersiveColorExtractor.extract(currentArtworkUri, context)
            }
            if (resolvedDark) dark else light
        }
        resolvedDark -> darkColors(themeStyle)
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

    ThemeStyle.TWILIGHT_PURPLE -> lightColorScheme(
        primary = TwilightPurple,
        onPrimary = Paper,
        primaryContainer = ColorTokens.twilightContainer,
        onPrimaryContainer = Ink,
        secondary = TwilightAmber,
        onSecondary = Paper,
        secondaryContainer = ColorTokens.twilightAmberContainer,
        onSecondaryContainer = Ink,
        tertiary = TwilightBlue,
        surface = ColorTokens.twilightSurface,
        onSurface = Ink,
        surfaceVariant = ColorTokens.twilightSurfaceVariant,
        onSurfaceVariant = ColorTokens.mutedInk,
        background = ColorTokens.twilightSurface
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

    ThemeStyle.PANDA -> lightColorScheme(
        primary = androidx.compose.ui.graphics.Color(0xFF1D1D1B),
        onPrimary = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
        primaryContainer = androidx.compose.ui.graphics.Color(0xFFE4E4DF),
        onPrimaryContainer = androidx.compose.ui.graphics.Color(0xFF171715),
        secondary = androidx.compose.ui.graphics.Color(0xFF6C7A6E),
        onSecondary = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
        secondaryContainer = androidx.compose.ui.graphics.Color(0xFFDDE4DB),
        onSecondaryContainer = androidx.compose.ui.graphics.Color(0xFF1B1F1A),
        tertiary = androidx.compose.ui.graphics.Color(0xFF728B58),
        surface = androidx.compose.ui.graphics.Color(0xFFFBFAF6),
        onSurface = androidx.compose.ui.graphics.Color(0xFF1B1B19),
        surfaceVariant = androidx.compose.ui.graphics.Color(0xFFE9E9E3),
        onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFF5E605A),
        background = androidx.compose.ui.graphics.Color(0xFFFBFAF6)
    )

    ThemeStyle.CYBERPUNK -> lightColorScheme(
        primary = androidx.compose.ui.graphics.Color(0xFFC4008E),
        onPrimary = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
        primaryContainer = androidx.compose.ui.graphics.Color(0xFFFFD6F3),
        onPrimaryContainer = androidx.compose.ui.graphics.Color(0xFF2A0021),
        secondary = androidx.compose.ui.graphics.Color(0xFF007C7C),
        onSecondary = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
        secondaryContainer = androidx.compose.ui.graphics.Color(0xFFC8FFFF),
        onSecondaryContainer = androidx.compose.ui.graphics.Color(0xFF002020),
        tertiary = androidx.compose.ui.graphics.Color(0xFF6A00E0),
        surface = androidx.compose.ui.graphics.Color(0xFFF8F2FB),
        onSurface = androidx.compose.ui.graphics.Color(0xFF1A0B20),
        surfaceVariant = androidx.compose.ui.graphics.Color(0xFFECE0F2),
        onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFF5E4A68),
        background = androidx.compose.ui.graphics.Color(0xFFF8F2FB)
    )

    ThemeStyle.SAKURA -> lightColorScheme(
        primary = androidx.compose.ui.graphics.Color(0xFFC85C7C),
        onPrimary = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
        primaryContainer = androidx.compose.ui.graphics.Color(0xFFFFDDE7),
        onPrimaryContainer = androidx.compose.ui.graphics.Color(0xFF3A0E1C),
        secondary = androidx.compose.ui.graphics.Color(0xFF8A6A8F),
        onSecondary = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
        secondaryContainer = androidx.compose.ui.graphics.Color(0xFFEFE0F0),
        onSecondaryContainer = androidx.compose.ui.graphics.Color(0xFF2C1532),
        tertiary = androidx.compose.ui.graphics.Color(0xFFC49A5E),
        surface = androidx.compose.ui.graphics.Color(0xFFFFF8F9),
        onSurface = androidx.compose.ui.graphics.Color(0xFF2B1A1F),
        surfaceVariant = androidx.compose.ui.graphics.Color(0xFFF5E5E9),
        onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFF6B555B),
        background = androidx.compose.ui.graphics.Color(0xFFFFF8F9)
    )

    ThemeStyle.AURORA -> lightColorScheme(
        primary = androidx.compose.ui.graphics.Color(0xFF0E8F7E),
        onPrimary = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
        primaryContainer = androidx.compose.ui.graphics.Color(0xFFC2F5EA),
        onPrimaryContainer = androidx.compose.ui.graphics.Color(0xFF003730),
        secondary = androidx.compose.ui.graphics.Color(0xFF3B6FD4),
        onSecondary = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
        secondaryContainer = androidx.compose.ui.graphics.Color(0xFFD8E3FF),
        onSecondaryContainer = androidx.compose.ui.graphics.Color(0xFF0A1F4A),
        tertiary = androidx.compose.ui.graphics.Color(0xFF8B5BD6),
        surface = androidx.compose.ui.graphics.Color(0xFFF4FBF9),
        onSurface = androidx.compose.ui.graphics.Color(0xFF10201D),
        surfaceVariant = androidx.compose.ui.graphics.Color(0xFFDCEAE6),
        onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFF4E5F5B),
        background = androidx.compose.ui.graphics.Color(0xFFF4FBF9)
    )

    ThemeStyle.SUNSET -> lightColorScheme(
        primary = androidx.compose.ui.graphics.Color(0xFFD9603A),
        onPrimary = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
        primaryContainer = androidx.compose.ui.graphics.Color(0xFFFFDCCB),
        onPrimaryContainer = androidx.compose.ui.graphics.Color(0xFF3D1204),
        secondary = androidx.compose.ui.graphics.Color(0xFFC23E6B),
        onSecondary = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
        secondaryContainer = androidx.compose.ui.graphics.Color(0xFFFFD9E3),
        onSecondaryContainer = androidx.compose.ui.graphics.Color(0xFF42091F),
        tertiary = androidx.compose.ui.graphics.Color(0xFF9A7B1E),
        surface = androidx.compose.ui.graphics.Color(0xFFFFF7F2),
        onSurface = androidx.compose.ui.graphics.Color(0xFF2A1710),
        surfaceVariant = androidx.compose.ui.graphics.Color(0xFFF6E2D5),
        onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFF6B5245),
        background = androidx.compose.ui.graphics.Color(0xFFFFF7F2)
    )

    ThemeStyle.LAVA -> lightColorScheme(
        primary = androidx.compose.ui.graphics.Color(0xFFC43A1A),
        onPrimary = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
        primaryContainer = androidx.compose.ui.graphics.Color(0xFFFFD8C7),
        onPrimaryContainer = androidx.compose.ui.graphics.Color(0xFF3B0B00),
        secondary = androidx.compose.ui.graphics.Color(0xFFC08A12),
        onSecondary = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
        secondaryContainer = androidx.compose.ui.graphics.Color(0xFFFFE7B8),
        onSecondaryContainer = androidx.compose.ui.graphics.Color(0xFF3A2A00),
        tertiary = androidx.compose.ui.graphics.Color(0xFFB32450),
        surface = androidx.compose.ui.graphics.Color(0xFFFFF6F1),
        onSurface = androidx.compose.ui.graphics.Color(0xFF29150F),
        surfaceVariant = androidx.compose.ui.graphics.Color(0xFFF5DFD4),
        onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFF6A5046),
        background = androidx.compose.ui.graphics.Color(0xFFFFF6F1)
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

    ThemeStyle.TWILIGHT_PURPLE -> darkColorScheme(
        primary = ColorTokens.twilightDark,
        onPrimary = Ink,
        primaryContainer = TwilightPurple,
        onPrimaryContainer = Paper,
        secondary = ColorTokens.twilightAmberDark,
        onSecondary = Ink,
        secondaryContainer = TwilightAmber,
        onSecondaryContainer = Paper,
        tertiary = ColorTokens.twilightBlueDark,
        surface = ColorTokens.twilightDarkSurface,
        onSurface = ColorTokens.lightInk,
        surfaceVariant = ColorTokens.twilightDarkSurfaceVariant,
        onSurfaceVariant = ColorTokens.darkMutedInk,
        background = ColorTokens.twilightDarkSurface
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

    ThemeStyle.PANDA -> darkColorScheme(
        primary = androidx.compose.ui.graphics.Color(0xFFF0F0EA),
        onPrimary = androidx.compose.ui.graphics.Color(0xFF151513),
        primaryContainer = androidx.compose.ui.graphics.Color(0xFFD7D7D0),
        onPrimaryContainer = androidx.compose.ui.graphics.Color(0xFF0E0E0C),
        secondary = androidx.compose.ui.graphics.Color(0xFFB8C2B6),
        onSecondary = androidx.compose.ui.graphics.Color(0xFF1A1A18),
        secondaryContainer = androidx.compose.ui.graphics.Color(0xFF3A4239),
        onSecondaryContainer = androidx.compose.ui.graphics.Color(0xFFE6EAE3),
        tertiary = androidx.compose.ui.graphics.Color(0xFFA9C296),
        surface = androidx.compose.ui.graphics.Color(0xFF121210),
        onSurface = androidx.compose.ui.graphics.Color(0xFFF1F1EB),
        surfaceVariant = androidx.compose.ui.graphics.Color(0xFF242422),
        onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFFC6C7C0),
        background = androidx.compose.ui.graphics.Color(0xFF0E0E0C)
    )

    ThemeStyle.CYBERPUNK -> darkColorScheme(
        primary = androidx.compose.ui.graphics.Color(0xFFFF3DDB),
        onPrimary = androidx.compose.ui.graphics.Color(0xFF16001A),
        primaryContainer = androidx.compose.ui.graphics.Color(0xFF6A0057),
        onPrimaryContainer = androidx.compose.ui.graphics.Color(0xFFFFD9F6),
        secondary = androidx.compose.ui.graphics.Color(0xFF00F0E6),
        onSecondary = androidx.compose.ui.graphics.Color(0xFF00201E),
        secondaryContainer = androidx.compose.ui.graphics.Color(0xFF004A47),
        onSecondaryContainer = androidx.compose.ui.graphics.Color(0xFFC8FFFC),
        tertiary = androidx.compose.ui.graphics.Color(0xFFA96BFF),
        surface = androidx.compose.ui.graphics.Color(0xFF0A0712),
        onSurface = androidx.compose.ui.graphics.Color(0xFFF4E9FF),
        surfaceVariant = androidx.compose.ui.graphics.Color(0xFF1D1330),
        onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFFC9B4E4),
        background = androidx.compose.ui.graphics.Color(0xFF07040C)
    )

    ThemeStyle.SAKURA -> darkColorScheme(
        primary = androidx.compose.ui.graphics.Color(0xFFFFB3C6),
        onPrimary = androidx.compose.ui.graphics.Color(0xFF3A0E1C),
        primaryContainer = androidx.compose.ui.graphics.Color(0xFF7A2F45),
        onPrimaryContainer = androidx.compose.ui.graphics.Color(0xFFFFDDE7),
        secondary = androidx.compose.ui.graphics.Color(0xFFD6B8E0),
        onSecondary = androidx.compose.ui.graphics.Color(0xFF2C1532),
        secondaryContainer = androidx.compose.ui.graphics.Color(0xFF4C3654),
        onSecondaryContainer = androidx.compose.ui.graphics.Color(0xFFF1E0F5),
        tertiary = androidx.compose.ui.graphics.Color(0xFFE8C48D),
        surface = androidx.compose.ui.graphics.Color(0xFF1A1114),
        onSurface = androidx.compose.ui.graphics.Color(0xFFF7E9ED),
        surfaceVariant = androidx.compose.ui.graphics.Color(0xFF2C1F24),
        onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFFD3BCC3),
        background = androidx.compose.ui.graphics.Color(0xFF130C0F)
    )

    ThemeStyle.AURORA -> darkColorScheme(
        primary = androidx.compose.ui.graphics.Color(0xFF6FF7D6),
        onPrimary = androidx.compose.ui.graphics.Color(0xFF00201A),
        primaryContainer = androidx.compose.ui.graphics.Color(0xFF00584A),
        onPrimaryContainer = androidx.compose.ui.graphics.Color(0xFFC2F5EA),
        secondary = androidx.compose.ui.graphics.Color(0xFF9CBEFF),
        onSecondary = androidx.compose.ui.graphics.Color(0xFF06183A),
        secondaryContainer = androidx.compose.ui.graphics.Color(0xFF24417A),
        onSecondaryContainer = androidx.compose.ui.graphics.Color(0xFFD8E3FF),
        tertiary = androidx.compose.ui.graphics.Color(0xFFD3B4FF),
        surface = androidx.compose.ui.graphics.Color(0xFF08131A),
        onSurface = androidx.compose.ui.graphics.Color(0xFFE6F5F1),
        surfaceVariant = androidx.compose.ui.graphics.Color(0xFF172A2E),
        onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFFB4CBC6),
        background = androidx.compose.ui.graphics.Color(0xFF050D12)
    )

    ThemeStyle.SUNSET -> darkColorScheme(
        primary = androidx.compose.ui.graphics.Color(0xFFFFA37A),
        onPrimary = androidx.compose.ui.graphics.Color(0xFF3A1204),
        primaryContainer = androidx.compose.ui.graphics.Color(0xFF7A2A10),
        onPrimaryContainer = androidx.compose.ui.graphics.Color(0xFFFFDCCB),
        secondary = androidx.compose.ui.graphics.Color(0xFFFF8FAE),
        onSecondary = androidx.compose.ui.graphics.Color(0xFF43101F),
        secondaryContainer = androidx.compose.ui.graphics.Color(0xFF6E2740),
        onSecondaryContainer = androidx.compose.ui.graphics.Color(0xFFFFD9E3),
        tertiary = androidx.compose.ui.graphics.Color(0xFFFFD37A),
        surface = androidx.compose.ui.graphics.Color(0xFF1B0F0A),
        onSurface = androidx.compose.ui.graphics.Color(0xFFFCEBDF),
        surfaceVariant = androidx.compose.ui.graphics.Color(0xFF35211A),
        onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFFD8BFAE),
        background = androidx.compose.ui.graphics.Color(0xFF120906)
    )

    ThemeStyle.LAVA -> darkColorScheme(
        primary = androidx.compose.ui.graphics.Color(0xFFFF7A4D),
        onPrimary = androidx.compose.ui.graphics.Color(0xFF3D0A00),
        primaryContainer = androidx.compose.ui.graphics.Color(0xFF7E1D00),
        onPrimaryContainer = androidx.compose.ui.graphics.Color(0xFFFFD8C7),
        secondary = androidx.compose.ui.graphics.Color(0xFFFFC94D),
        onSecondary = androidx.compose.ui.graphics.Color(0xFF3A2A00),
        secondaryContainer = androidx.compose.ui.graphics.Color(0xFF6E4E00),
        onSecondaryContainer = androidx.compose.ui.graphics.Color(0xFFFFE7B8),
        tertiary = androidx.compose.ui.graphics.Color(0xFFFF6B93),
        surface = androidx.compose.ui.graphics.Color(0xFF190D08),
        onSurface = androidx.compose.ui.graphics.Color(0xFFFCE9DE),
        surfaceVariant = androidx.compose.ui.graphics.Color(0xFF331F16),
        onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFFD6BBA9),
        background = androidx.compose.ui.graphics.Color(0xFF100705)
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
    val twilightContainer = androidx.compose.ui.graphics.Color(0xFFE8DEFF)
    val twilightSurface = androidx.compose.ui.graphics.Color(0xFFFCF8FF)
    val twilightSurfaceVariant = androidx.compose.ui.graphics.Color(0xFFE7DFF0)
    val twilightDark = androidx.compose.ui.graphics.Color(0xFFD0BCFF)
    val twilightDarkSurface = androidx.compose.ui.graphics.Color(0xFF17121C)
    val twilightDarkSurfaceVariant = androidx.compose.ui.graphics.Color(0xFF30273A)
    val twilightAmberContainer = androidx.compose.ui.graphics.Color(0xFFF2DFB8)
    val twilightAmberDark = androidx.compose.ui.graphics.Color(0xFFE7C781)
    val twilightBlueDark = androidx.compose.ui.graphics.Color(0xFFA7D0E3)
    val monoContainer = androidx.compose.ui.graphics.Color(0xFFD9DDE2)
    val monoSurface = androidx.compose.ui.graphics.Color(0xFFF8F9FA)
    val monoSurfaceVariant = androidx.compose.ui.graphics.Color(0xFFE2E4E8)
    val monoDark = androidx.compose.ui.graphics.Color(0xFFC7CDD4)
    val monoSecondaryDark = androidx.compose.ui.graphics.Color(0xFFB6C1C8)
    val monoDarkSurface = androidx.compose.ui.graphics.Color(0xFF121416)
    val monoDarkSurfaceVariant = androidx.compose.ui.graphics.Color(0xFF25292D)
}
