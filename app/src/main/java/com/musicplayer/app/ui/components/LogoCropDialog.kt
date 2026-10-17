package com.musicplayer.app.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.graphics.drawable.toBitmap
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.musicplayer.app.ui.i18n.LocalExtraStrings
import com.musicplayer.app.ui.i18n.LocalStrings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private const val MIN_ZOOM = 1f
private const val MAX_ZOOM = 4f
private const val OUTPUT_SIZE = 512
private const val LOAD_SIZE = 2048

@Composable
fun LogoCropDialog(
    sourceUri: Uri,
    onDismiss: () -> Unit,
    onConfirm: (Uri) -> Unit
) {
    val context = LocalContext.current
    val strings = LocalStrings.current
    val extra = LocalExtraStrings.current
    val scope = rememberCoroutineScope()

    var bitmap by remember(sourceUri) { mutableStateOf<Bitmap?>(null) }
    var loadFailed by remember(sourceUri) { mutableStateOf(false) }
    var viewportSide by remember { mutableFloatStateOf(0f) }
    var zoom by remember(sourceUri) { mutableFloatStateOf(MIN_ZOOM) }
    var offset by remember(sourceUri) { mutableStateOf(Offset.Zero) }
    var saving by remember(sourceUri) { mutableStateOf(false) }

    LaunchedEffect(sourceUri) {
        val request = ImageRequest.Builder(context)
            .data(sourceUri)
            .size(LOAD_SIZE)
            .allowHardware(false)
            .build()
        val result = runCatching { context.imageLoader.execute(request) }.getOrNull()
        val drawable = (result as? SuccessResult)?.drawable
        if (drawable != null) {
            bitmap = withContext(Dispatchers.IO) { drawable.toBitmap() }
        } else {
            loadFailed = true
        }
    }

    val image = bitmap
    val baseScale = if (image != null && viewportSide > 0f) {
        viewportSide / min(image.width, image.height)
    } else {
        0f
    }
    val scale = baseScale * zoom

    fun clampOffset(raw: Offset): Offset {
        if (image == null || viewportSide <= 0f) return raw
        val currentScale = baseScale * zoom
        val maxX = max(0f, (image.width * currentScale - viewportSide) / 2f)
        val maxY = max(0f, (image.height * currentScale - viewportSide) / 2f)
        return Offset(raw.x.coerceIn(-maxX, maxX), raw.y.coerceIn(-maxY, maxY))
    }

    val outlineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)

    Dialog(
        onDismissRequest = { if (!saving) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier
                .widthIn(max = 380.dp)
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = extra.logoCropTitle,
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = extra.logoCropHint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .onSizeChanged {
                            viewportSide = min(it.width, it.height).toFloat()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (image != null && viewportSide > 0f) {
                        val imageBitmap = remember(image) { image.asImageBitmap() }
                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(image, viewportSide) {
                                    detectTransformGestures { centroid, pan, gestureZoom, _ ->
                                        val newZoom = (zoom * gestureZoom).coerceIn(MIN_ZOOM, MAX_ZOOM)
                                        val ratio = newZoom / zoom
                                        val center = Offset(size.width / 2f, size.height / 2f)
                                        val centered = centroid - center
                                        val next = centered - (centered - offset) * ratio + pan
                                        zoom = newZoom
                                        offset = clampOffset(next)
                                    }
                                }
                        ) {
                            val side = min(size.width, size.height)
                            scale.let { currentScale ->
                                val dstW = (imageBitmap.width * currentScale).roundToInt()
                                val dstH = (imageBitmap.height * currentScale).roundToInt()
                                val dstLeft = ((size.width - dstW) / 2f + offset.x).roundToInt()
                                val dstTop = ((size.height - dstH) / 2f + offset.y).roundToInt()
                                clipPath(Path().apply { addOval(Rect(Offset.Zero, size)) }) {
                                    drawImage(
                                        image = imageBitmap,
                                        srcOffset = IntOffset.Zero,
                                        srcSize = IntSize(imageBitmap.width, imageBitmap.height),
                                        dstOffset = IntOffset(dstLeft, dstTop),
                                        dstSize = IntSize(dstW, dstH),
                                        filterQuality = FilterQuality.High
                                    )
                                }
                            }
                            drawCircle(
                                color = outlineColor,
                                radius = side / 2f - 1f,
                                style = Stroke(width = 2.dp.toPx())
                            )
                        }
                    } else if (loadFailed) {
                        Text(
                            text = extra.logoLoadFailed,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        CircularProgressIndicator()
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = extra.logoZoom,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Slider(
                        value = zoom,
                        onValueChange = { value ->
                            zoom = value
                            offset = clampOffset(offset)
                        },
                        valueRange = MIN_ZOOM..MAX_ZOOM,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss, enabled = !saving) {
                        Text(strings.cancel)
                    }
                    Button(
                        enabled = image != null && viewportSide > 0f && !saving,
                        modifier = Modifier.padding(start = 8.dp),
                        onClick = {
                            val current = image ?: return@Button
                            saving = true
                            scope.launch {
                                val cropped = withContext(Dispatchers.IO) {
                                    saveCroppedLogo(context, current, viewportSide, scale, offset)
                                }
                                saving = false
                                if (cropped != null) {
                                    onConfirm(cropped)
                                } else {
                                    loadFailed = true
                                }
                            }
                        }
                    ) {
                        if (saving) {
                            CircularProgressIndicator(
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(18.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text(strings.confirm)
                        }
                    }
                }
            }
        }
        }
    }
}

private fun saveCroppedLogo(
    context: Context,
    bitmap: Bitmap,
    viewportSide: Float,
    scale: Float,
    offset: Offset
): Uri? = runCatching {
    val side = (viewportSide / scale).roundToInt().coerceIn(1, min(bitmap.width, bitmap.height))
    val centerX = bitmap.width / 2f - offset.x / scale
    val centerY = bitmap.height / 2f - offset.y / scale
    val left = (centerX - side / 2f).roundToInt().coerceIn(0, bitmap.width - side)
    val top = (centerY - side / 2f).roundToInt().coerceIn(0, bitmap.height - side)
    val cropped = Bitmap.createBitmap(bitmap, left, top, side, side)
    val output = Bitmap.createScaledBitmap(cropped, OUTPUT_SIZE, OUTPUT_SIZE, true)
    val file = File(context.filesDir, "custom-logo.png")
    val temp = File(context.filesDir, "custom-logo.tmp.png")
    FileOutputStream(temp).use { stream ->
        output.compress(Bitmap.CompressFormat.PNG, 100, stream)
    }
    if (!temp.renameTo(file)) {
        temp.copyTo(file, overwrite = true)
        temp.delete()
    }
    Uri.fromFile(file)
}.getOrNull()
