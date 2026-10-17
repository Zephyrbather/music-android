package com.musicplayer.app.ui.components

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.graphics.drawable.toBitmap
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.musicplayer.app.domain.model.Song
import com.musicplayer.app.ui.i18n.ExtraStrings
import com.musicplayer.app.ui.i18n.LocalExtraStrings
import kotlin.math.max
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class LyricPosterStyle { FILM, MINIMAL, GLASS }

enum class LyricPosterPalette(val accent: Int) {
    THEME(0),
    GOLD(0xFFE8C07A.toInt()),
    OCEAN(0xFF7CC7FF.toInt()),
    SAKURA(0xFFFFA6C9.toInt())
}

private const val POSTER_WIDTH = 1080
private const val POSTER_HEIGHT = 1440

suspend fun renderLyricPoster(
    context: Context,
    song: Song,
    lines: List<String>,
    highlightIndex: Int,
    style: LyricPosterStyle,
    palette: LyricPosterPalette,
    themeAccent: Int
): Bitmap = withContext(Dispatchers.IO) {
    val accent = if (palette == LyricPosterPalette.THEME) themeAccent else palette.accent
    val bitmap = Bitmap.createBitmap(POSTER_WIDTH, POSTER_HEIGHT, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val cover = loadPosterCover(context, song)
    if (cover != null) {
        canvas.drawBitmap(
            cover,
            null,
            coverRect(cover),
            Paint().apply { isFilterBitmap = true }
        )
    } else {
        canvas.drawColor(Color.rgb(22, 20, 28))
    }
    drawScrim(canvas, accent)

    val window = lyricWindow(lines, highlightIndex)
    when (style) {
        LyricPosterStyle.FILM -> drawFilmPoster(canvas, song, window.lines, window.highlight, accent)
        LyricPosterStyle.MINIMAL -> drawMinimalPoster(canvas, song, window.lines, window.highlight, accent)
        LyricPosterStyle.GLASS -> drawGlassPoster(canvas, song, window.lines, window.highlight, accent)
    }
    bitmap
}

private suspend fun loadPosterCover(context: Context, song: Song): Bitmap? {
    val artwork = song.artworkUri?.takeIf { it.isNotBlank() } ?: return null
    return runCatching {
        val request = ImageRequest.Builder(context)
            .data(artwork)
            .size(POSTER_WIDTH, POSTER_WIDTH)
            .allowHardware(false)
            .build()
        val result = context.imageLoader.execute(request)
        (result as? SuccessResult)?.drawable?.toBitmap()
    }.getOrNull()?.let(::blurCover)
}

private fun blurCover(source: Bitmap): Bitmap {
    val smallWidth = 72
    val smallHeight = max(1, smallWidth * source.height / source.width)
    val small = Bitmap.createScaledBitmap(source, smallWidth, smallHeight, true)
    val blurred = Bitmap.createScaledBitmap(small, POSTER_WIDTH, POSTER_HEIGHT, true)
    if (small !== source && small !== blurred) small.recycle()
    return blurred
}

private fun coverRect(cover: Bitmap): RectF {
    val scale = max(
        POSTER_WIDTH.toFloat() / cover.width,
        POSTER_HEIGHT.toFloat() / cover.height
    )
    val width = cover.width * scale
    val height = cover.height * scale
    val left = (POSTER_WIDTH - width) / 2f
    val top = (POSTER_HEIGHT - height) / 2f
    return RectF(left, top, left + width, top + height)
}

private fun drawScrim(canvas: Canvas, accent: Int) {
    val darkScrim = Paint().apply {
        shader = LinearGradient(
            0f, 0f, 0f, POSTER_HEIGHT.toFloat(),
            intArrayOf(
                Color.argb(196, 10, 10, 14),
                Color.argb(172, 10, 10, 14),
                Color.argb(240, 8, 8, 12)
            ),
            floatArrayOf(0f, 0.48f, 1f),
            Shader.TileMode.CLAMP
        )
    }
    canvas.drawRect(0f, 0f, POSTER_WIDTH.toFloat(), POSTER_HEIGHT.toFloat(), darkScrim)

    val accentWash = Paint().apply {
        shader = LinearGradient(
            0f, POSTER_HEIGHT * 0.55f, 0f, POSTER_HEIGHT.toFloat(),
            Color.argb(0, Color.red(accent), Color.green(accent), Color.blue(accent)),
            Color.argb(70, Color.red(accent), Color.green(accent), Color.blue(accent)),
            Shader.TileMode.CLAMP
        )
    }
    canvas.drawRect(0f, POSTER_HEIGHT * 0.5f, POSTER_WIDTH.toFloat(), POSTER_HEIGHT.toFloat(), accentWash)
}

private data class LyricWindow(val lines: List<String>, val highlight: Int)

private fun lyricWindow(lines: List<String>, highlightIndex: Int): LyricWindow {
    val cleaned = lines.map { it.trim() }.filter { it.isNotEmpty() }
    if (cleaned.isEmpty()) return LyricWindow(emptyList(), -1)
    val safeIndex = highlightIndex.coerceIn(0, cleaned.lastIndex)
    val start = (safeIndex - 2).coerceIn(0, max(0, cleaned.size - 5))
    val window = cleaned.drop(start).take(5)
    return LyricWindow(window, safeIndex - start)
}

private data class FittedLyrics(
    val paint: Paint,
    val highlightPaint: Paint,
    val groups: List<List<String>>,
    val highlightIndex: Int,
    val lineHeight: Float
)

private fun fitLyrics(
    lines: List<String>,
    highlightIndex: Int,
    maxWidth: Float,
    maxHeight: Float,
    startSize: Float,
    basePaint: Paint,
    highlightPaint: Paint
): FittedLyrics {
    var textSize = startSize
    var lineHeight = textSize * 1.6f
    while (true) {
        val normal = Paint(basePaint).apply { this.textSize = textSize }
        val highlight = Paint(highlightPaint).apply { this.textSize = textSize }
        val groups = lines.mapIndexed { index, line ->
            wrapText(line, if (index == highlightIndex) highlight else normal, maxWidth)
        }
        val total = groups.sumOf { it.size }
        if (total * lineHeight <= maxHeight || textSize <= 34f) {
            return FittedLyrics(normal, highlight, groups, highlightIndex, lineHeight)
        }
        textSize -= 3f
        lineHeight = textSize * 1.6f
    }
}

private fun drawLyricsBlock(
    canvas: Canvas,
    fitted: FittedLyrics,
    centerX: Float,
    centerY: Float,
    accent: Int
) {
    val total = fitted.groups.sumOf { it.size }
    if (total == 0) return
    val textX = centerX
    var y = centerY - (total - 1) * fitted.lineHeight / 2f
    val pillPaint = Paint().apply {
        color = Color.argb(64, Color.red(accent), Color.green(accent), Color.blue(accent))
        isAntiAlias = true
    }
    fitted.groups.forEachIndexed { index, group ->
        val isHighlight = index == fitted.highlightIndex
        val paint = if (isHighlight) fitted.highlightPaint else fitted.paint
        group.forEach { part ->
            if (isHighlight) {
                val width = paint.measureText(part) + 52f
                val pillLeft = textX - width / 2f
                canvas.drawRoundRect(
                    RectF(pillLeft, y - fitted.lineHeight * 0.62f, pillLeft + width, y + fitted.lineHeight * 0.24f),
                    fitted.lineHeight * 0.42f,
                    fitted.lineHeight * 0.42f,
                    pillPaint
                )
            }
            canvas.drawText(part, textX, y, paint)
            y += fitted.lineHeight
        }
    }
}

private fun drawFilmPoster(
    canvas: Canvas,
    song: Song,
    lines: List<String>,
    highlightIndex: Int,
    accent: Int
) {
    val perforation = Paint().apply {
        color = Color.argb(34, 255, 255, 255)
        isAntiAlias = true
    }
    var y = 56f
    while (y < POSTER_HEIGHT - 30f) {
        canvas.drawRoundRect(RectF(18f, y, 42f, y + 26f), 7f, 7f, perforation)
        canvas.drawRoundRect(
            RectF(POSTER_WIDTH - 42f, y, POSTER_WIDTH - 18f, y + 26f),
            7f,
            7f,
            perforation
        )
        y += 78f
    }

    val accentPaint = Paint().apply {
        color = accent
        isAntiAlias = true
    }
    val labelPaint = Paint().apply {
        color = accent
        textSize = 30f
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    }
    canvas.drawText("L  Y  R  I  C  S", POSTER_WIDTH / 2f, 168f, labelPaint)
    canvas.drawRoundRect(
        RectF(POSTER_WIDTH / 2f - 46f, 192f, POSTER_WIDTH / 2f + 46f, 197f),
        3f,
        3f,
        accentPaint
    )

    val fitted = fitLyrics(
        lines = lines,
        highlightIndex = highlightIndex,
        maxWidth = POSTER_WIDTH - 280f,
        maxHeight = 720f,
        startSize = 56f,
        basePaint = Paint().apply {
            color = Color.argb(232, 255, 255, 255)
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create("serif", Typeface.NORMAL)
        },
        highlightPaint = Paint().apply {
            color = Color.WHITE
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create("serif", Typeface.BOLD)
        }
    )
    drawLyricsBlock(
        canvas = canvas,
        fitted = fitted,
        centerX = POSTER_WIDTH / 2f,
        centerY = 650f,
        accent = accent
    )

    drawCenteredSongInfo(canvas, song, accent, POSTER_HEIGHT - 220f)
}

private fun drawMinimalPoster(
    canvas: Canvas,
    song: Song,
    lines: List<String>,
    highlightIndex: Int,
    accent: Int
) {
    val shadePaint = Paint().apply {
        shader = LinearGradient(
            0f, 0f, POSTER_WIDTH.toFloat(), 0f,
            intArrayOf(Color.argb(150, 6, 6, 10), Color.argb(20, 6, 6, 10)),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP
        )
    }
    canvas.drawRect(0f, 0f, POSTER_WIDTH.toFloat(), POSTER_HEIGHT.toFloat(), shadePaint)

    val accentPaint = Paint().apply {
        color = accent
        isAntiAlias = true
    }
    canvas.drawRoundRect(RectF(106f, 250f, 113f, 1010f), 4f, 4f, accentPaint)

    val quotePaint = Paint().apply {
        color = Color.argb(80, Color.red(accent), Color.green(accent), Color.blue(accent))
        textSize = 200f
        isAntiAlias = true
        typeface = Typeface.create("serif", Typeface.BOLD)
    }
    canvas.drawText("\u201C", 84f, 306f, quotePaint)

    val fitted = fitLyrics(
        lines = lines,
        highlightIndex = highlightIndex,
        maxWidth = POSTER_WIDTH - 300f,
        maxHeight = 560f,
        startSize = 54f,
        basePaint = Paint().apply {
            color = Color.argb(235, 255, 255, 255)
            isAntiAlias = true
            textAlign = Paint.Align.LEFT
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        },
        highlightPaint = Paint().apply {
            color = Color.WHITE
            isAntiAlias = true
            textAlign = Paint.Align.LEFT
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        }
    )
    drawLyricsBlockLeft(
        canvas = canvas,
        fitted = fitted,
        left = 148f,
        centerY = 660f,
        accent = accent
    )

    val labelPaint = Paint().apply {
        color = accent
        textSize = 26f
        isAntiAlias = true
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    }
    canvas.drawText("N O W   P L A Y I N G", 106f, POSTER_HEIGHT - 236f, labelPaint)

    val titlePaint = Paint().apply {
        color = Color.WHITE
        textSize = 52f
        isAntiAlias = true
        typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
    }
    canvas.drawText(ellipsize(song.title, titlePaint, POSTER_WIDTH - 212f), 106f, POSTER_HEIGHT - 164f, titlePaint)

    val artistPaint = Paint().apply {
        color = Color.argb(205, 255, 255, 255)
        textSize = 32f
        isAntiAlias = true
        typeface = Typeface.create("sans-serif", Typeface.NORMAL)
    }
    canvas.drawText(
        ellipsize("${song.artistName} · ${song.albumTitle}", artistPaint, POSTER_WIDTH - 212f),
        106f,
        POSTER_HEIGHT - 110f,
        artistPaint
    )
    canvas.drawRoundRect(RectF(106f, POSTER_HEIGHT - 78f, 176f, POSTER_HEIGHT - 74f), 2f, 2f, accentPaint)
}

private fun drawLyricsBlockLeft(
    canvas: Canvas,
    fitted: FittedLyrics,
    left: Float,
    centerY: Float,
    accent: Int
) {
    val total = fitted.groups.sumOf { it.size }
    if (total == 0) return
    var y = centerY - (total - 1) * fitted.lineHeight / 2f
    val pillPaint = Paint().apply {
        color = Color.argb(64, Color.red(accent), Color.green(accent), Color.blue(accent))
        isAntiAlias = true
    }
    fitted.groups.forEachIndexed { index, group ->
        val isHighlight = index == fitted.highlightIndex
        val paint = if (isHighlight) fitted.highlightPaint else fitted.paint
        group.forEach { part ->
            if (isHighlight) {
                val width = paint.measureText(part) + 52f
                canvas.drawRoundRect(
                    RectF(left - 26f, y - fitted.lineHeight * 0.62f, left - 26f + width, y + fitted.lineHeight * 0.24f),
                    fitted.lineHeight * 0.42f,
                    fitted.lineHeight * 0.42f,
                    pillPaint
                )
            }
            canvas.drawText(part, left, y, paint)
            y += fitted.lineHeight
        }
    }
}

private fun drawGlassPoster(
    canvas: Canvas,
    song: Song,
    lines: List<String>,
    highlightIndex: Int,
    accent: Int
) {
    val card = RectF(92f, 300f, POSTER_WIDTH - 92f, 1100f)
    canvas.drawRoundRect(
        card,
        58f,
        58f,
        Paint().apply {
            color = Color.argb(44, 255, 255, 255)
            isAntiAlias = true
        }
    )
    canvas.drawRoundRect(
        card,
        58f,
        58f,
        Paint().apply {
            style = Paint.Style.STROKE
            strokeWidth = 2.5f
            color = Color.argb(96, 255, 255, 255)
            isAntiAlias = true
        }
    )
    val accentLine = Paint().apply {
        color = accent
        isAntiAlias = true
    }
    canvas.drawRoundRect(
        RectF(POSTER_WIDTH / 2f - 38f, 272f, POSTER_WIDTH / 2f + 38f, 277f),
        3f,
        3f,
        accentLine
    )

    val fitted = fitLyrics(
        lines = lines,
        highlightIndex = highlightIndex,
        maxWidth = card.width() - 140f,
        maxHeight = 620f,
        startSize = 54f,
        basePaint = Paint().apply {
            color = Color.argb(238, 255, 255, 255)
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        },
        highlightPaint = Paint().apply {
            color = Color.WHITE
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        }
    )
    drawLyricsBlock(
        canvas = canvas,
        fitted = fitted,
        centerX = POSTER_WIDTH / 2f,
        centerY = card.centerY(),
        accent = accent
    )

    drawCenteredSongInfo(canvas, song, accent, POSTER_HEIGHT - 246f)
}

private fun drawCenteredSongInfo(canvas: Canvas, song: Song, accent: Int, startY: Float) {
    val titlePaint = Paint().apply {
        color = Color.WHITE
        textSize = 50f
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
    }
    canvas.drawText(
        ellipsize(song.title, titlePaint, POSTER_WIDTH - 220f),
        POSTER_WIDTH / 2f,
        startY,
        titlePaint
    )
    val artistPaint = Paint().apply {
        color = Color.argb(205, 255, 255, 255)
        textSize = 31f
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif", Typeface.NORMAL)
    }
    canvas.drawText(
        ellipsize("${song.artistName} · ${song.albumTitle}", artistPaint, POSTER_WIDTH - 260f),
        POSTER_WIDTH / 2f,
        startY + 52f,
        artistPaint
    )
    canvas.drawRoundRect(
        RectF(POSTER_WIDTH / 2f - 34f, startY + 86f, POSTER_WIDTH / 2f + 34f, startY + 90f),
        2f,
        2f,
        Paint().apply {
            color = accent
            isAntiAlias = true
        }
    )
}

private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
    val value = text.ifBlank { " " }
    if (paint.measureText(value) <= maxWidth) return listOf(value)
    val result = mutableListOf<String>()
    var remaining = value
    while (remaining.isNotEmpty() && result.size < 4) {
        val count = paint.breakText(remaining, true, maxWidth, null)
        if (count <= 0) break
        var cut = count
        if (cut < remaining.length) {
            val candidate = remaining.substring(0, cut)
            val lastSpace = candidate.lastIndexOf(' ')
            if (lastSpace > 0) cut = lastSpace
        }
        result += remaining.substring(0, cut).trim()
        remaining = remaining.substring(cut).trim()
    }
    return result.ifEmpty { listOf(value) }
}

private fun ellipsize(text: String, paint: Paint, maxWidth: Float): String {
    if (paint.measureText(text) <= maxWidth) return text
    val ellipsis = "…"
    val available = maxWidth - paint.measureText(ellipsis)
    if (available <= 0f) return ellipsis
    val count = paint.breakText(text, true, available, null)
    return text.substring(0, count.coerceAtLeast(0)).trimEnd() + ellipsis
}

fun savePosterToGallery(context: Context, bitmap: Bitmap): Uri? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, "lyric-poster-${System.currentTimeMillis()}.png")
        put(MediaStore.Images.Media.MIME_TYPE, "image/png")
        put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/MusicPlayer")
        put(MediaStore.Images.Media.IS_PENDING, 1)
    }
    val resolver = context.contentResolver
    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return null
    val written = runCatching {
        resolver.openOutputStream(uri)?.use { output ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
        } ?: false
    }.getOrDefault(false)
    if (!written) {
        runCatching { resolver.delete(uri, null, null) }
        return null
    }
    values.clear()
    values.put(MediaStore.Images.Media.IS_PENDING, 0)
    resolver.update(uri, values, null, null)
    return uri
}

fun sharePoster(context: Context, uri: Uri, title: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, title))
}

@Composable
fun LyricPosterDialog(
    song: Song,
    lines: List<String>,
    highlightIndex: Int,
    onDismiss: () -> Unit
) {
    val extra = LocalExtraStrings.current
    val context = LocalContext.current
    val themeAccent = MaterialTheme.colorScheme.primary
    var style by remember { mutableStateOf(LyricPosterStyle.FILM) }
    var palette by remember { mutableStateOf(LyricPosterPalette.THEME) }
    var bitmap by remember(song.id) { mutableStateOf<Bitmap?>(null) }
    var generating by remember { mutableStateOf(true) }

    LaunchedEffect(song.id, style, palette, lines, highlightIndex) {
        generating = true
        bitmap = renderLyricPoster(
            context = context,
            song = song,
            lines = lines,
            highlightIndex = highlightIndex,
            style = style,
            palette = palette,
            themeAccent = themeAccent.toArgb()
        )
        generating = false
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .widthIn(max = 640.dp)
                .padding(vertical = 18.dp),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = extra.lyricPoster,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Outlined.Close, contentDescription = null)
                    }
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .widthIn(max = 360.dp)
                        .fillMaxWidth()
                        .aspectRatio(0.75f)
                        .clip(RoundedCornerShape(18.dp))
                        .background(ComposeColor.Black),
                    contentAlignment = Alignment.Center
                ) {
                    bitmap?.let { image ->
                        Image(
                            bitmap = image.asImageBitmap(),
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    if (generating) {
                        CircularProgressIndicator()
                    }
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(LyricPosterStyle.entries) { item ->
                        FilterChip(
                            selected = style == item,
                            onClick = { style = item },
                            label = { Text(item.label(extra)) }
                        )
                    }
                    item { Spacer(modifier = Modifier.width(8.dp)) }
                    items(LyricPosterPalette.entries) { item ->
                        FilterChip(
                            selected = palette == item,
                            onClick = { palette = item },
                            leadingIcon = {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (item == LyricPosterPalette.THEME) themeAccent
                                            else ComposeColor(item.accent)
                                        )
                                )
                            },
                            label = { Text(item.label(extra)) }
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val image = bitmap ?: return@OutlinedButton
                            val uri = savePosterToGallery(context, image)
                            Toast.makeText(
                                context,
                                if (uri != null) extra.posterSaved else extra.posterSaveFailed,
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        enabled = bitmap != null && !generating,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(extra.posterSave)
                    }
                    Button(
                        onClick = {
                            val image = bitmap ?: return@Button
                            val uri = savePosterToGallery(context, image)
                            if (uri == null) {
                                Toast.makeText(context, extra.posterShareFailed, Toast.LENGTH_SHORT).show()
                            } else {
                                sharePoster(context, uri, extra.lyricPoster)
                            }
                        },
                        enabled = bitmap != null && !generating,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(extra.posterShare)
                    }
                }
            }
        }
    }
}

private fun LyricPosterStyle.label(extra: ExtraStrings): String = when (this) {
    LyricPosterStyle.FILM -> extra.posterStyleClassic
    LyricPosterStyle.MINIMAL -> extra.posterStyleMinimal
    LyricPosterStyle.GLASS -> extra.posterStyleGlass
}

private fun LyricPosterPalette.label(extra: ExtraStrings): String = when (this) {
    LyricPosterPalette.THEME -> extra.posterPaletteTheme
    LyricPosterPalette.GOLD -> extra.posterPaletteGold
    LyricPosterPalette.OCEAN -> extra.posterPaletteOcean
    LyricPosterPalette.SAKURA -> extra.posterPaletteSakura
}
