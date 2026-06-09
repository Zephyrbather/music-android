package com.musicplayer.app.playback

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.provider.Settings
import android.text.TextUtils
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import com.musicplayer.app.domain.model.PlaybackState
import com.musicplayer.app.domain.model.Song
import com.musicplayer.app.domain.usecase.PlayMusicUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

internal class DesktopLyricsOverlayController(
    private val context: Context,
    private val playMusicUseCase: PlayMusicUseCase,
    private val scope: CoroutineScope
) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var rootView: LinearLayout? = null
    private var currentLineView: TextView? = null
    private var nextLineView: TextView? = null
    private var layoutParams: WindowManager.LayoutParams? = null
    private var stateJob: Job? = null
    private var cachedSongId: String? = null
    private var cachedTimedLines: List<DesktopLyricLine> = emptyList()
    private var cachedPlainLines: List<String> = emptyList()

    fun show() {
        if (rootView != null || !canDrawOverlays(context)) return
        val view = createView()
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = dp(96)
        }
        runCatching {
            windowManager.addView(view, params)
            rootView = view
            layoutParams = params
            stateJob = scope.launch {
                playMusicUseCase.observePlayback().collect { state ->
                    update(state)
                }
            }
        }
    }

    fun hide() {
        stateJob?.cancel()
        stateJob = null
        rootView?.let { view ->
            runCatching { windowManager.removeView(view) }
        }
        rootView = null
        currentLineView = null
        nextLineView = null
        layoutParams = null
    }

    fun release() {
        hide()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun createView(): LinearLayout {
        val current = lyricTextView(isPrimary = true)
        val next = lyricTextView(isPrimary = false)
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            minimumWidth = dp(188)
            setPadding(dp(18), dp(12), dp(18), dp(12))
            background = GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(
                    Color.argb(226, 20, 24, 34),
                    Color.argb(226, 72, 52, 102)
                )
            ).apply {
                cornerRadius = dp(18).toFloat()
                setStroke(dp(1), Color.argb(70, 255, 255, 255))
            }
            elevation = dp(8).toFloat()
            addView(current)
            addView(next)
        }
        currentLineView = current
        nextLineView = next
        attachDragHandler(container)
        return container
    }

    private fun lyricTextView(isPrimary: Boolean): TextView {
        return TextView(context).apply {
            setTextColor(if (isPrimary) Color.WHITE else Color.argb(190, 255, 255, 255))
            textSize = if (isPrimary) 17f else 13f
            typeface = if (isPrimary) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            includeFontPadding = false
            gravity = Gravity.CENTER
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
            maxWidth = dp(360)
            setShadowLayer(3f, 0f, 1f, Color.argb(140, 0, 0, 0))
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun attachDragHandler(view: View) {
        var downRawX = 0f
        var downRawY = 0f
        var startX = 0
        var startY = 0
        view.setOnTouchListener { _, event ->
            val params = layoutParams ?: return@setOnTouchListener false
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downRawX = event.rawX
                    downRawY = event.rawY
                    startX = params.x
                    startY = params.y
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = startX + (event.rawX - downRawX).roundToInt()
                    params.y = (startY + (event.rawY - downRawY).roundToInt()).coerceAtLeast(0)
                    rootView?.let { windowManager.updateViewLayout(it, params) }
                    true
                }
                MotionEvent.ACTION_UP,
                MotionEvent.ACTION_CANCEL -> {
                    view.performClick()
                    true
                }
                else -> false
            }
        }
    }

    private fun update(state: PlaybackState) {
        val song = state.currentSong
        if (song?.id != cachedSongId) {
            cachedSongId = song?.id
            cachedTimedLines = parseTimedLyrics(song?.lyrics)
            cachedPlainLines = plainLyricLines(song?.lyrics)
        }

        val fallback = song?.notificationLine().orEmpty().ifBlank { "Zephyr music" }
        val index = cachedTimedLines.indexOfLast { it.timeMs <= state.positionMs }.let { if (it < 0) 0 else it }
        val current = cachedTimedLines.getOrNull(index)?.text
            ?: cachedPlainLines.firstOrNull()
            ?: fallback
        val next = cachedTimedLines.getOrNull(index + 1)?.text
            ?: cachedPlainLines.drop(1).firstOrNull()

        currentLineView?.text = current
        nextLineView?.apply {
            text = next.orEmpty()
            visibility = if (next.isNullOrBlank()) View.GONE else View.VISIBLE
        }
    }

    private fun Song.notificationLine(): String {
        val cleanTitle = title.trim()
        val cleanArtist = artistName.trim()
        return when {
            cleanTitle.isNotBlank() && cleanArtist.isNotBlank() -> "$cleanTitle-$cleanArtist"
            cleanTitle.isNotBlank() -> cleanTitle
            cleanArtist.isNotBlank() -> cleanArtist
            else -> ""
        }
    }

    private fun dp(value: Int): Int {
        return (value * context.resources.displayMetrics.density).roundToInt()
    }

    private data class DesktopLyricLine(val timeMs: Long, val text: String)

    private companion object {
        private val TIMESTAMP_REGEX = Regex("\\[(\\d{1,3}):(\\d{2})(?:[.:](\\d{1,3}))?]")

        fun canDrawOverlays(context: Context): Boolean {
            return Settings.canDrawOverlays(context)
        }

        fun parseTimedLyrics(lyrics: String?): List<DesktopLyricLine> {
            return lyrics.orEmpty()
                .lineSequence()
                .mapNotNull { line ->
                    val match = TIMESTAMP_REGEX.find(line) ?: return@mapNotNull null
                    val minutes = match.groupValues[1].toLongOrNull() ?: return@mapNotNull null
                    val seconds = match.groupValues[2].toLongOrNull() ?: return@mapNotNull null
                    val millis = match.groupValues[3].takeIf { it.isNotEmpty() }?.let {
                        it.padEnd(3, '0').take(3).toLongOrNull()
                    } ?: 0L
                    val text = line.replace(TIMESTAMP_REGEX, "").trim()
                    if (text.isBlank()) return@mapNotNull null
                    DesktopLyricLine(minutes * 60_000L + seconds * 1_000L + millis, text)
                }
                .sortedBy { it.timeMs }
                .toList()
        }

        fun plainLyricLines(lyrics: String?): List<String> {
            return lyrics.orEmpty()
                .lineSequence()
                .map { it.replace(TIMESTAMP_REGEX, "").trim() }
                .filter { it.isNotBlank() }
                .toList()
        }
    }
}
