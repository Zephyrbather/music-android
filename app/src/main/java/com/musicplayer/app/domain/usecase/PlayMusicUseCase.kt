package com.musicplayer.app.domain.usecase

import com.musicplayer.app.data.local.dao.PlayRecordDao
import com.musicplayer.app.domain.model.PlaybackMode
import com.musicplayer.app.domain.model.PlaybackState
import com.musicplayer.app.domain.model.Song
import com.musicplayer.app.domain.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlayMusicUseCase @Inject constructor(
    private val playRecordDao: PlayRecordDao,
    private val settingsRepository: SettingsRepository
) {
    private val playbackState = MutableStateFlow(PlaybackState())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    @Volatile
    private var topListenedSongIds: Set<String> = emptySet()

    private val recommendationHistory = ArrayDeque<Song>()
    private val recommendationHistoryLock = Any()

    init {
        scope.launch {
            settingsRepository.playbackMode.distinctUntilChanged().collect { mode ->
                playbackState.update { state ->
                    if (state.playbackMode == mode) state else state.copy(playbackMode = mode)
                }
            }
        }
        scope.launch {
            while (true) {
                refreshTopListenedSongIds()
                delay(TOP_LISTENED_REFRESH_MS)
            }
        }
    }

    fun observePlayback(): StateFlow<PlaybackState> = playbackState

    fun play(song: Song, queue: List<Song>) {
        clearRecommendationHistory()
        playbackState.update {
            it.copy(
                currentSong = song,
                queue = queue.ifEmpty { listOf(song) },
                isPlaying = true,
                positionMs = 0L,
                durationMs = song.durationMs,
                errorMessage = null
            )
        }
    }

    fun playFromQueue(song: Song) {
        clearRecommendationHistory()
        playbackState.update { state ->
            val queue = state.queue.ifEmpty { listOf(song) }
            state.copy(
                currentSong = song,
                queue = queue,
                isPlaying = true,
                positionMs = 0L,
                durationMs = song.durationMs,
                errorMessage = null
            )
        }
    }

    fun previewCurrentSong(song: Song) {
        playbackState.update { state ->
            val queue = state.queue.map { queued ->
                if (queued.id == song.id) song else queued
            }.ifEmpty { listOf(song) }
            state.copy(
                currentSong = song,
                queue = queue,
                durationMs = song.durationMs,
                errorMessage = null
            )
        }
    }

    fun togglePlayPause() {
        playbackState.update { state ->
            if (state.currentSong == null) state else state.copy(isPlaying = !state.isPlaying)
        }
    }

    fun setPlaying(playing: Boolean) {
        playbackState.update { state ->
            if (state.currentSong == null) state else state.copy(isPlaying = playing)
        }
    }

    fun skipNext(): Song? {
        var selected: Song? = null
        var recommendationMode = false
        playbackState.update { state ->
            val currentIndex = state.queue.indexOfFirst { it.id == state.currentSong?.id }
            val currentSong = state.currentSong
            val sourceScopedQueue = if (currentSong == null) {
                state.queue
            } else {
                state.queue.filter { it.hasSamePlaybackSourceAs(currentSong) }
            }
            recommendationMode = state.playbackMode == PlaybackMode.RECOMMENDATION
            val next = when (state.playbackMode) {
                PlaybackMode.SEQUENTIAL -> state.queue.getOrNull(currentIndex + 1) ?: state.queue.firstOrNull()
                PlaybackMode.SHUFFLE -> sourceScopedQueue.filterNot { it.id == currentSong?.id }.randomOrNull()
                    ?: sourceScopedQueue.firstOrNull()
                    ?: state.queue.firstOrNull()

                PlaybackMode.RECOMMENDATION -> recommendedNext(state)
            }
            selected = next
            state.copy(
                currentSong = next,
                isPlaying = next != null,
                positionMs = 0L,
                durationMs = next?.durationMs ?: 0L,
                errorMessage = null
            )
        }
        if (recommendationMode) {
            selected?.let { rememberRecommendation(it, playbackState.value.queue.size) }
        }
        return selected
    }

    fun skipPrevious(): Song? {
        var selected: Song? = null
        playbackState.update { state ->
            val currentIndex = state.queue.indexOfFirst { it.id == state.currentSong?.id }
            val previous = state.queue.getOrNull(currentIndex - 1) ?: state.queue.lastOrNull()
            selected = previous
            state.copy(
                currentSong = previous,
                isPlaying = previous != null,
                positionMs = 0L,
                durationMs = previous?.durationMs ?: 0L,
                errorMessage = null
            )
        }
        return selected
    }

    fun setPlaybackMode(mode: PlaybackMode) {
        if (playbackState.value.playbackMode != mode) clearRecommendationHistory()
        playbackState.update { it.copy(playbackMode = mode) }
        scope.launch {
            settingsRepository.setPlaybackMode(mode)
        }
    }

    fun seekTo(positionMs: Long) {
        playbackState.update { state ->
            val duration = state.durationMs.takeIf { it > 0L } ?: state.currentSong?.durationMs ?: 0L
            state.copy(positionMs = positionMs.coerceIn(0L, duration.takeIf { it > 0L } ?: positionMs))
        }
    }

    fun updatePlaybackPosition(positionMs: Long, durationMs: Long) {
        playbackState.update { state ->
            val duration = durationMs
                .takeIf { it > 0L }
                ?: state.currentSong?.durationMs
                ?: state.durationMs
            val position = positionMs
                .coerceAtLeast(0L)
                .let { if (duration > 0L) it.coerceAtMost(duration) else it }
            state.copy(
                positionMs = position,
                durationMs = duration.takeIf { it > 0L } ?: 0L
            )
        }
    }

    fun playNext(song: Song) {
        playbackState.update { state ->
            val currentIndex = state.queue.indexOfFirst { it.id == state.currentSong?.id }
            val insertIndex = if (currentIndex >= 0) currentIndex + 1 else 0
            val queueWithoutSong = state.queue.filterNot { it.id == song.id }.toMutableList()
            queueWithoutSong.add(insertIndex.coerceIn(0, queueWithoutSong.size), song)
            state.copy(queue = queueWithoutSong)
        }
    }

    fun setPlaybackError(message: String?) {
        playbackState.update { state ->
            state.copy(errorMessage = message, isPlaying = if (message == null) state.isPlaying else false)
        }
    }

    private fun recommendedNext(state: PlaybackState): Song? {
        val current = state.currentSong ?: return state.queue.firstOrNull()
        val candidates = state.queue.filter { it.id != current.id }
        if (candidates.isEmpty()) return null

        val recentPicks = synchronized(recommendationHistoryLock) { recommendationHistory.toList() }
        val recentIds = recentPicks.mapTo(HashSet()) { it.id }
        val freshCandidates = candidates.filterNot { it.id in recentIds }
        val pool = freshCandidates.ifEmpty { candidates }
        val recentArtistIds = recentPicks.takeLast(RECENT_ARTIST_WINDOW)
            .mapNotNull { it.artistId }
            .toSet()
        val recentAlbumIds = recentPicks.takeLast(RECENT_ALBUM_WINDOW)
            .mapNotNull { it.albumId }
            .toSet()

        val listenedIds = topListenedSongIds
        val scored = pool.map { song ->
            Scored(
                song,
                recommendationScore(current, song, listenedIds, recentArtistIds, recentAlbumIds)
            )
        }

        val maxScore = scored.maxOfOrNull { it.score } ?: 0
        val topTier = scored.filter { it.score == maxScore }
        return topTier.randomOrNull()?.song
            ?: state.queue.getOrNull(state.queue.indexOfFirst { it.id == current.id } + 1)
            ?: state.queue.firstOrNull()
    }

    private fun recommendationScore(
        current: Song,
        candidate: Song,
        listenedIds: Set<String>,
        recentArtistIds: Set<String>,
        recentAlbumIds: Set<String>
    ): Int {
        val currentTags = current.semanticTags.normalizedTagSet()
        val candidateTags = candidate.semanticTags.normalizedTagSet()
        val sharedTags = currentTags intersect candidateTags
        val tagUnionSize = (currentTags + candidateTags).size.coerceAtLeast(1)

        var score = 0
        if (sharedTags.isNotEmpty()) {
            score += sharedTags.size * 8
            score += sharedTags.size * 4 / tagUnionSize
        }
        if (current.categoryTag.normalizedCategory() == candidate.categoryTag.normalizedCategory()) score += 5
        if (candidate.hasSamePlaybackSourceAs(current)) score += 2
        if (candidate.artistId != null && candidate.artistId == current.artistId) score += 3
        if (candidate.albumId != null && candidate.albumId == current.albumId) score += 2
        if (candidate.isFavorite) score += 2
        if (candidate.id in listenedIds) score += 2
        if (!candidate.artworkUri.isNullOrBlank()) score += 1
        val yearDistance = candidate.releaseYear?.let { candidateYear ->
            current.releaseYear?.let { currentYear -> kotlin.math.abs(candidateYear - currentYear) }
        }
        if (yearDistance != null && yearDistance <= 3) score += 1
        if (candidate.artistId != null && candidate.artistId in recentArtistIds) {
            score -= RECENT_ARTIST_PENALTY
        }
        if (candidate.albumId != null && candidate.albumId in recentAlbumIds) {
            score -= RECENT_ALBUM_PENALTY
        }
        return score
    }

    private fun rememberRecommendation(song: Song, queueSize: Int) {
        val window = (queueSize / 4).coerceIn(MIN_RECOMMENDATION_WINDOW, MAX_RECOMMENDATION_WINDOW)
        synchronized(recommendationHistoryLock) {
            recommendationHistory.removeAll { it.id == song.id }
            recommendationHistory.addLast(song)
            while (recommendationHistory.size > window) {
                recommendationHistory.removeFirst()
            }
        }
    }

    private fun clearRecommendationHistory() {
        synchronized(recommendationHistoryLock) {
            recommendationHistory.clear()
        }
    }

    private fun Song.hasSamePlaybackSourceAs(other: Song): Boolean {
        if (source != other.source) return false
        return webDavServerId == other.webDavServerId
    }

    private fun List<String>.normalizedTagSet(): Set<String> {
        return map { it.trim().lowercase() }
            .filter { it.isNotBlank() }
            .toSet()
    }

    private fun String?.normalizedCategory(): String? {
        return this?.trim()?.lowercase()?.takeIf { it.isNotBlank() }
    }

    private suspend fun refreshTopListenedSongIds() {
        val sinceMs = System.currentTimeMillis() - TOP_LISTENED_WINDOW_MS
        topListenedSongIds = runCatching {
            playRecordDao.topSongsSince(sinceMs, limit = TOP_LISTENED_LIMIT)
                .map { it.songId }
                .toSet()
        }.getOrDefault(topListenedSongIds)
    }

    private companion object {
        private const val TOP_LISTENED_LIMIT = 500
        private const val TOP_LISTENED_REFRESH_MS = 5 * 60 * 1000L
        private const val TOP_LISTENED_WINDOW_MS = 30 * 24 * 3600_000L
        private const val MIN_RECOMMENDATION_WINDOW = 8
        private const val MAX_RECOMMENDATION_WINDOW = 24
        private const val RECENT_ARTIST_WINDOW = 3
        private const val RECENT_ALBUM_WINDOW = 2
        private const val RECENT_ARTIST_PENALTY = 6
        private const val RECENT_ALBUM_PENALTY = 4
    }

    private data class Scored(val song: Song, val score: Int)
}
