package com.musicplayer.app.ui.theme

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.palette.graphics.Palette

object ImmersiveColorExtractor {
    private var lastArtworkUri: String? = null
    private var cachedLightScheme: ColorScheme? = null
    private var cachedDarkScheme: ColorScheme? = null

    fun extract(artworkUri: String?, context: Context): Pair<ColorScheme, ColorScheme> {
        if (artworkUri == lastArtworkUri) {
            val light = cachedLightScheme
            val dark = cachedDarkScheme
            if (light != null && dark != null) return light to dark
        }
        lastArtworkUri = artworkUri

        val bitmap = loadBitmap(artworkUri, context)
        val palette = if (bitmap != null) Palette.from(bitmap).generate() else null

        cachedLightScheme = buildScheme(palette, darkTheme = false)
        cachedDarkScheme = buildScheme(palette, darkTheme = true)
        return cachedLightScheme!! to cachedDarkScheme!!
    }

    private fun buildScheme(palette: Palette?, darkTheme: Boolean): ColorScheme {
        val vibrant = palette?.getVibrantColor(0)?.toColor()
        val dominant = palette?.getDominantColor(0)?.toColor()
        val muted = palette?.getMutedColor(0)?.toColor()
        val darkVibrant = palette?.getDarkVibrantColor(0)?.toColor()
        val darkMuted = palette?.getDarkMutedColor(0)?.toColor()
        val lightVibrant = palette?.getLightVibrantColor(0)?.toColor()

        val primary = vibrant ?: dominant ?: Vermilion
        val secondary = muted ?: lightVibrant ?: Coral
        val tertiary = darkVibrant ?: darkMuted ?: Brass

        val surface = if (darkTheme) Color(0xFF121110) else Color(0xFFFBFAF8)
        val onSurface = if (darkTheme) Color(0xFFF0EBE4) else Color(0xFF1A1714)

        val onPrimary = contentColorFor(primary)
        val onSecondary = contentColorFor(secondary)
        val onTertiary = contentColorFor(tertiary)

        val primaryContainer = blendOver(primary.copy(alpha = 0.12f), surface)
        val onPrimaryContainer = primary.mix(onSurface, 0.6f)
        val secondaryContainer = blendOver(secondary.copy(alpha = 0.12f), surface)
        val onSecondaryContainer = secondary.mix(onSurface, 0.6f)
        val tertiaryContainer = blendOver(tertiary.copy(alpha = 0.12f), surface)
        val onTertiaryContainer = tertiary.mix(onSurface, 0.6f)

        val surfaceVariant = primary.copy(alpha = if (darkTheme) 0.08f else 0.06f).let {
            blendOver(it, surface)
        }
        val onSurfaceVariant = onSurface.copy(alpha = 0.72f)
        val outline = onSurface.copy(alpha = if (darkTheme) 0.22f else 0.18f)
        val outlineVariant = onSurface.copy(alpha = if (darkTheme) 0.12f else 0.08f)
        val surfaceTint = primary.copy(alpha = 0.06f)

        return if (darkTheme) {
            darkColorScheme(
                primary = primary,
                onPrimary = onPrimary,
                primaryContainer = primaryContainer,
                onPrimaryContainer = onPrimaryContainer,
                secondary = secondary,
                onSecondary = onSecondary,
                secondaryContainer = secondaryContainer,
                onSecondaryContainer = onSecondaryContainer,
                tertiary = tertiary,
                onTertiary = onTertiary,
                tertiaryContainer = tertiaryContainer,
                onTertiaryContainer = onTertiaryContainer,
                surface = surface,
                onSurface = onSurface,
                surfaceVariant = surfaceVariant,
                onSurfaceVariant = onSurfaceVariant,
                background = surface,
                onBackground = onSurface,
                outline = outline,
                outlineVariant = outlineVariant,
                surfaceTint = surfaceTint
            )
        } else {
            lightColorScheme(
                primary = primary,
                onPrimary = onPrimary,
                primaryContainer = primaryContainer,
                onPrimaryContainer = onPrimaryContainer,
                secondary = secondary,
                onSecondary = onSecondary,
                secondaryContainer = secondaryContainer,
                onSecondaryContainer = onSecondaryContainer,
                tertiary = tertiary,
                onTertiary = onTertiary,
                tertiaryContainer = tertiaryContainer,
                onTertiaryContainer = onTertiaryContainer,
                surface = surface,
                onSurface = onSurface,
                surfaceVariant = surfaceVariant,
                onSurfaceVariant = onSurfaceVariant,
                background = surface,
                onBackground = onSurface,
                outline = outline,
                outlineVariant = outlineVariant,
                surfaceTint = surfaceTint
            )
        }
    }

    private fun contentColorFor(bg: Color): Color {
        val luminance = 0.299 * bg.red + 0.587 * bg.green + 0.114 * bg.blue
        return if (luminance > 0.55) Color(0xFF1A1714) else Color(0xFFFBF8F2)
    }

    /** Blend [overlay] on top of [base] using source-over compositing. */
    private fun blendOver(overlay: Color, base: Color): Color {
        val a = overlay.alpha
        val invA = 1f - a
        return Color(
            red = overlay.red * a + base.red * invA,
            green = overlay.green * a + base.green * invA,
            blue = overlay.blue * a + base.blue * invA,
            alpha = 1f
        )
    }

    /** Linear interpolation between two colors by [ratio] (0 = this, 1 = other). */
    private fun Color.mix(other: Color, ratio: Float): Color {
        val r = 1f - ratio
        return Color(
            red = red * r + other.red * ratio,
            green = green * r + other.green * ratio,
            blue = blue * r + other.blue * ratio,
            alpha = alpha * r + other.alpha * ratio
        )
    }

    private fun Int.toColor(): Color = Color(this)

    private fun loadBitmap(uri: String?, context: Context): android.graphics.Bitmap? {
        if (uri.isNullOrBlank()) return null
        return try {
            if (uri.startsWith("file://") || uri.startsWith("/")) {
                BitmapFactory.decodeFile(uri.removePrefix("file://"))
            } else if (uri.startsWith("content://")) {
                context.contentResolver.openInputStream(android.net.Uri.parse(uri))?.use {
                    BitmapFactory.decodeStream(it)
                }
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }
}
