package com.musicplayer.app.playback

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.musicplayer.app.MainActivity
import com.musicplayer.app.R
import com.musicplayer.app.domain.model.Song
import com.musicplayer.app.domain.usecase.PlayMusicUseCase
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import java.net.URL
import javax.inject.Inject
import kotlin.concurrent.thread

@AndroidEntryPoint
class MusicPlaybackService : Service() {
    private lateinit var player: ExoPlayer
    private var mediaSession: MediaSessionCompat? = null
    @Inject lateinit var playMusicUseCase: PlayMusicUseCase

    private val mainHandler = Handler(Looper.getMainLooper())
    private var currentTitle: String = ""
    private var currentArtist: String = ""
    private var currentAlbum: String = ""
    private var currentArtworkUri: String? = null
    private var currentArtworkBitmap: Bitmap? = null
    private var loadingArtworkUri: String? = null
    private var failedArtworkUri: String? = null
    private var isPlaying: Boolean = false

    override fun onCreate() {
        super.onCreate()
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()
        player = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, true)
            .build()
        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_READY -> {
                        updateMediaMetadata()
                        updatePlaybackState()
                        updateNotification()
                    }
                    Player.STATE_ENDED -> {
                        val nextSong = playMusicUseCase.skipNext()
                        if (nextSong != null) {
                            playSong(nextSong)
                        } else {
                            isPlaying = false
                            playMusicUseCase.setPlaying(false)
                            updatePlaybackState()
                            updateNotification()
                            stopForeground(STOP_FOREGROUND_DETACH)
                        }
                    }
                }
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
                playMusicUseCase.setPlaying(playing)
                updatePlaybackState()
                updateNotification()
            }
        })
        createNotificationChannel()
        initMediaSession()
        updateMediaMetadata()
        updatePlaybackState()
        startForeground(PLAYBACK_NOTIFICATION_ID, buildNotification())
    }

    private fun initMediaSession() {
        runCatching {
            mediaSession = MediaSessionCompat(this, "MusicPlayback").apply {
                setCallback(object : MediaSessionCompat.Callback() {
                    override fun onPlay() { resumePlayback() }
                    override fun onPause() { pausePlayback() }
                    override fun onSkipToNext() {
                        playMusicUseCase.skipNext()?.let { playSong(it) }
                    }
                    override fun onSkipToPrevious() {
                        if (player.currentPosition > RESTART_THRESHOLD_MS) seekPlaybackTo(0L)
                        else playMusicUseCase.skipPrevious()?.let { playSong(it) }
                    }
                    override fun onSeekTo(pos: Long) {
                        seekPlaybackTo(pos)
                    }
                    override fun onStop() {
                        pausePlayback()
                        stopForeground(STOP_FOREGROUND_DETACH)
                    }
                })
                isActive = true
            }
        }.onFailure { Log.w(TAG, "MediaSession init failed", it) }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY -> {
                val uri = intent.getStringExtra(EXTRA_URI).orEmpty()
                val title = intent.getStringExtra(EXTRA_TITLE).orEmpty()
                val artist = intent.getStringExtra(EXTRA_ARTIST).orEmpty()
                val album = intent.getStringExtra(EXTRA_ALBUM).orEmpty()
                val artworkUri = intent.getStringExtra(EXTRA_ARTWORK_URI)
                currentTitle = title.ifBlank { getString(R.string.app_name) }
                currentArtist = artist
                currentAlbum = album
                setCurrentArtwork(artworkUri)
                if (uri.isNotBlank() && !uri.startsWith("demo://")) {
                    playUri(uri)
                }
                updateMediaMetadata()
                updatePlaybackState()
                updateNotification()
            }

            ACTION_PAUSE -> {
                pausePlayback()
            }

            ACTION_SEEK -> {
                val positionMs = intent.getLongExtra(EXTRA_POSITION_MS, 0L)
                seekPlaybackTo(positionMs)
            }

            ACTION_STOP -> {
                pausePlayback()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }

            ACTION_TOGGLE_PLAY -> {
                togglePlayback()
            }

            ACTION_NEXT -> {
                playMusicUseCase.skipNext()?.let { playSong(it) }
            }

            ACTION_PREVIOUS -> {
                if (player.currentPosition > RESTART_THRESHOLD_MS) {
                    seekPlaybackTo(0L)
                } else {
                    playMusicUseCase.skipPrevious()?.let { playSong(it) }
                }
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        mediaSession?.isActive = false
        mediaSession?.release()
        mediaSession = null
        player.release()
        super.onDestroy()
    }

    private fun playUri(uri: String) {
        val currentUri = player.currentMediaItem?.localConfiguration?.uri.toString()
        if (currentUri != uri) {
            player.setMediaItem(MediaItem.fromUri(uri))
            player.prepare()
        }
        player.play()
        isPlaying = true
        playMusicUseCase.setPlaying(true)
    }

    private fun playSong(song: Song) {
        if (song.uri.startsWith("demo://")) return
        currentTitle = song.title
        currentArtist = song.artistName
        currentAlbum = song.albumTitle
        setCurrentArtwork(song.artworkUri)
        player.setMediaItem(MediaItem.fromUri(song.uri))
        player.prepare()
        player.play()
        isPlaying = true
        playMusicUseCase.setPlaying(true)
        updateMediaMetadata()
        updateNotification()
    }

    private fun pausePlayback() {
        player.pause()
        isPlaying = false
        playMusicUseCase.setPlaying(false)
        updatePlaybackState()
        updateNotification()
    }

    private fun resumePlayback() {
        if (player.currentMediaItem == null) return
        player.play()
        isPlaying = true
        playMusicUseCase.setPlaying(true)
        updatePlaybackState()
        updateNotification()
    }

    private fun togglePlayback() {
        if (player.isPlaying) pausePlayback() else resumePlayback()
    }

    private fun seekPlaybackTo(positionMs: Long) {
        val position = positionMs.coerceAtLeast(0L)
        player.seekTo(position)
        playMusicUseCase.seekTo(position)
        updatePlaybackState()
        updateNotification()
    }

    private fun setCurrentArtwork(uri: String?) {
        val cleanUri = uri?.takeIf { it.isNotBlank() }
        if (cleanUri == currentArtworkUri) return
        currentArtworkUri = cleanUri
        currentArtworkBitmap = null
        loadingArtworkUri = null
        failedArtworkUri = null
    }

    private fun updateMediaMetadata() {
        val session = mediaSession ?: return
        val metadata = MediaMetadataCompat.Builder().apply {
            putString(MediaMetadataCompat.METADATA_KEY_TITLE, currentTitle.ifBlank { "Music" })
            putString(MediaMetadataCompat.METADATA_KEY_ARTIST, currentArtist.ifBlank { "Music Player" })
            putString(MediaMetadataCompat.METADATA_KEY_ALBUM, currentAlbum.ifBlank { getString(R.string.app_name) })
            putLong(MediaMetadataCompat.METADATA_KEY_DURATION, player.duration.coerceAtLeast(0L))
            currentArtworkBitmap?.let { bitmap ->
                putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, bitmap)
                putBitmap(MediaMetadataCompat.METADATA_KEY_ART, bitmap)
            }
        }.build()
        session.setMetadata(metadata)
    }

    private fun updatePlaybackState() {
        val session = mediaSession ?: return
        val state = if (isPlaying) PlaybackStateCompat.STATE_PLAYING
        else PlaybackStateCompat.STATE_PAUSED
        val builder = PlaybackStateCompat.Builder()
            .setState(state, player.currentPosition.coerceAtLeast(0L), 1.0f)
            .setActions(
                PlaybackStateCompat.ACTION_PLAY or
                    PlaybackStateCompat.ACTION_PAUSE or
                    PlaybackStateCompat.ACTION_PLAY_PAUSE or
                    PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                    PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                    PlaybackStateCompat.ACTION_SEEK_TO or
                    PlaybackStateCompat.ACTION_STOP
            )
        session.setPlaybackState(builder.build())
    }

    private fun updateNotification() {
        val manager = getSystemService(NotificationManager::class.java) ?: return
        val notification = buildNotification()
        if (isPlaying) {
            startForeground(PLAYBACK_NOTIFICATION_ID, notification)
        } else {
            manager.notify(PLAYBACK_NOTIFICATION_ID, notification)
        }
    }

    private fun buildNotification(): Notification {
        val activityIntent = Intent(this, MainActivity::class.java)
        val contentPendingIntent = PendingIntent.getActivity(
            this, 0, activityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val artist = currentArtist.ifBlank { getString(R.string.app_name) }
        val album = currentAlbum.takeIf { it.isNotBlank() }
        val subtitle = listOfNotNull(artist, album).joinToString(" - ")
        val playPauseTitle = if (isPlaying) {
            getString(R.string.notification_pause)
        } else {
            getString(R.string.notification_play)
        }

        val builder = NotificationCompat.Builder(this, PLAYBACK_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_music)
            .setContentTitle(currentTitle.ifBlank { getString(R.string.app_name) })
            .setContentText(subtitle)
            .setSubText(album)
            .setContentIntent(contentPendingIntent)
            .setCustomContentView(
                buildCompactNotificationView(
                    title = currentTitle.ifBlank { getString(R.string.app_name) },
                    playPauseIcon = if (isPlaying) R.drawable.ic_notification_pause else R.drawable.ic_notification_play,
                    playPauseTitle = playPauseTitle
                )
            )
            .setOngoing(isPlaying)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(
                R.drawable.ic_notification_previous,
                getString(R.string.notification_previous),
                buildPendingIntent(ACTION_PREVIOUS)
            )
            .addAction(
                if (isPlaying) R.drawable.ic_notification_pause else R.drawable.ic_notification_play,
                playPauseTitle,
                buildPendingIntent(ACTION_TOGGLE_PLAY)
            )
            .addAction(
                R.drawable.ic_notification_next,
                getString(R.string.notification_next),
                buildPendingIntent(ACTION_NEXT)
            )
        builder.setStyle(NotificationCompat.DecoratedCustomViewStyle())

        currentArtworkBitmap?.let { bitmap ->
            builder.setLargeIcon(bitmap)
        } ?: currentArtworkUri
            ?.takeUnless { it == failedArtworkUri }
            ?.let(::loadArtworkAsync)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
        }

        return builder.build()
    }

    private fun buildCompactNotificationView(
        title: String,
        playPauseIcon: Int,
        playPauseTitle: String
    ): RemoteViews {
        return RemoteViews(packageName, R.layout.notification_playback_compact).apply {
            setTextViewText(R.id.notification_title, title)
            currentArtworkBitmap?.let { bitmap ->
                setImageViewBitmap(R.id.notification_artwork, bitmap)
            } ?: setImageViewResource(R.id.notification_artwork, R.drawable.ic_notification_music)

            setImageViewResource(R.id.notification_play_pause, playPauseIcon)
            setOnClickPendingIntent(R.id.notification_previous, buildPendingIntent(ACTION_PREVIOUS))
            setOnClickPendingIntent(R.id.notification_play_pause, buildPendingIntent(ACTION_TOGGLE_PLAY))
            setOnClickPendingIntent(R.id.notification_next, buildPendingIntent(ACTION_NEXT))

            setCharSequence(
                R.id.notification_previous,
                "setContentDescription",
                getString(R.string.notification_previous)
            )
            setCharSequence(R.id.notification_play_pause, "setContentDescription", playPauseTitle)
            setCharSequence(R.id.notification_next, "setContentDescription", getString(R.string.notification_next))
        }
    }

    private fun loadArtworkAsync(uri: String) {
        if (loadingArtworkUri == uri) return
        loadingArtworkUri = uri
        thread(name = "PlaybackArtworkLoader") {
            val bitmap = loadArtwork(uri)
            mainHandler.post {
                if (currentArtworkUri == uri) {
                    loadingArtworkUri = null
                    if (bitmap != null) {
                        currentArtworkBitmap = bitmap
                        failedArtworkUri = null
                        updateMediaMetadata()
                        updateNotification()
                    } else {
                        failedArtworkUri = uri
                    }
                } else if (loadingArtworkUri == uri) {
                    loadingArtworkUri = null
                }
            }
        }
    }

    private fun loadArtwork(uri: String): Bitmap? {
        return runCatching {
            val parsed = Uri.parse(uri)
            val bitmap = when (parsed.scheme?.lowercase()) {
                "http", "https" -> {
                    val url = URL(uri)
                    val connection = url.openConnection().apply {
                        connectTimeout = ARTWORK_TIMEOUT_MS
                        readTimeout = ARTWORK_TIMEOUT_MS
                    }
                    connection.getInputStream().use { BitmapFactory.decodeStream(it) }
                }
                "content", "android.resource", "file" -> {
                    contentResolver.openInputStream(parsed)?.use { BitmapFactory.decodeStream(it) }
                }
                else -> {
                    File(uri).takeIf { it.exists() }?.inputStream()?.use { BitmapFactory.decodeStream(it) }
                }
            }
            bitmap?.scaledForNotification()
        }.getOrNull()
    }

    private fun Bitmap.scaledForNotification(): Bitmap {
        val longest = maxOf(width, height)
        if (longest <= MAX_ARTWORK_SIZE_PX) return this
        val scale = MAX_ARTWORK_SIZE_PX.toFloat() / longest.toFloat()
        return Bitmap.createScaledBitmap(
            this,
            (width * scale).toInt().coerceAtLeast(1),
            (height * scale).toInt().coerceAtLeast(1),
            true
        )
    }

    private fun buildPendingIntent(action: String): PendingIntent {
        return PendingIntent.getService(
            this, action.hashCode(),
            Intent(this, MusicPlaybackService::class.java).setAction(action),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            PLAYBACK_CHANNEL_ID,
            getString(R.string.playback_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.playback_channel_description)
            setShowBadge(false)
            setSound(null, null)
        }
        manager.createNotificationChannel(channel)
    }

    companion object {
        private const val TAG = "MusicPlaybackSvc"
        private const val PLAYBACK_CHANNEL_ID = "music_playback"
        private const val PLAYBACK_NOTIFICATION_ID = 1101
        private const val ACTION_PLAY = "com.musicplayer.app.action.PLAY"
        private const val ACTION_PAUSE = "com.musicplayer.app.action.PAUSE"
        private const val ACTION_SEEK = "com.musicplayer.app.action.SEEK"
        private const val ACTION_STOP = "com.musicplayer.app.action.STOP"
        private const val ACTION_TOGGLE_PLAY = "com.musicplayer.app.action.TOGGLE_PLAY"
        private const val ACTION_NEXT = "com.musicplayer.app.action.NEXT"
        private const val ACTION_PREVIOUS = "com.musicplayer.app.action.PREVIOUS"
        private const val EXTRA_URI = "extra_uri"
        private const val EXTRA_TITLE = "extra_title"
        private const val EXTRA_ARTIST = "extra_artist"
        private const val EXTRA_ALBUM = "extra_album"
        private const val EXTRA_ARTWORK_URI = "extra_artwork_uri"
        private const val EXTRA_POSITION_MS = "extra_position_ms"
        private const val RESTART_THRESHOLD_MS = 3_000L
        private const val ARTWORK_TIMEOUT_MS = 5_000
        private const val MAX_ARTWORK_SIZE_PX = 512

        fun playIntent(context: Context, song: Song): Intent {
            return Intent(context, MusicPlaybackService::class.java)
                .setAction(ACTION_PLAY)
                .putExtra(EXTRA_URI, song.uri)
                .putExtra(EXTRA_TITLE, song.title)
                .putExtra(EXTRA_ARTIST, song.artistName)
                .putExtra(EXTRA_ALBUM, song.albumTitle)
                .putExtra(EXTRA_ARTWORK_URI, song.artworkUri)
        }

        fun pauseIntent(context: Context): Intent {
            return Intent(context, MusicPlaybackService::class.java).setAction(ACTION_PAUSE)
        }

        fun seekIntent(context: Context, positionMs: Long): Intent {
            return Intent(context, MusicPlaybackService::class.java)
                .setAction(ACTION_SEEK)
                .putExtra(EXTRA_POSITION_MS, positionMs)
        }

        fun stopIntent(context: Context): Intent {
            return Intent(context, MusicPlaybackService::class.java).setAction(ACTION_STOP)
        }
    }
}
