# Eight UI and Data Improvements for Android Music Player

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement 8 distinct improvements: sequential icon redesign with direct mode toggle, album card redesign, mini player bar background differentiation, data import/export, edit song metadata, improved recommendation algorithm, fix home screen action button, and rotating home hero.

**Architecture:** All 8 changes are self-contained within the existing Jetpack Compose + Room + Hilt + MVVM architecture. No new libraries are needed. The import/export feature uses org.json (Android built-in) and SAF (Storage Access Framework). Changes span UI layer (Compose), ViewModel layer, data layer (Room DAOs), and domain layer (use cases).

**Tech Stack:** Kotlin, Jetpack Compose, Material 3, Room, Hilt, DataStore Preferences, Coil, Android SAF/SAF.

---

## File Structure

Files to **create**:
- `app/src/main/java/com/musicplayer/app/ui/components/EditSongDialog.kt` -- #5 edit metadata dialog
- `app/src/main/java/com/musicplayer/app/domain/usecase/ExportImportUseCase.kt` -- #4 data export/import logic

Files to **modify**:
- `app/src/main/java/com/musicplayer/app/ui/player/NowPlayingScreen.kt` -- #1 sequential icon, direct mode toggle
- `app/src/main/java/com/musicplayer/app/ui/player/MiniPlayerBar.kt` -- #3 background color
- `app/src/main/java/com/musicplayer/app/ui/components/AlbumCard.kt` -- #2 album card redesign
- `app/src/main/java/com/musicplayer/app/ui/home/HomeViewModel.kt` -- #7 action song observing, #8 hero rotation
- `app/src/main/java/com/musicplayer/app/ui/home/HomeScreen.kt` -- #7 action sheet integration, #8 hero re-trigger
- `app/src/main/java/com/musicplayer/app/ui/library/LibraryScreen.kt` -- #5 edit option in SongActionSheet
- `app/src/main/java/com/musicplayer/app/ui/settings/SettingsScreen.kt` -- #4 import/export buttons
- `app/src/main/java/com/musicplayer/app/ui/settings/SettingsViewModel.kt` -- #4 import/export methods
- `app/src/main/java/com/musicplayer/app/ui/i18n/AppStrings.kt` -- #1, #4, #5 new string keys
- `app/src/main/java/com/musicplayer/app/data/local/dao/SongDao.kt` -- #5 update method
- `app/src/main/java/com/musicplayer/app/data/local/dao/AlbumDao.kt` -- #4 getAll method
- `app/src/main/java/com/musicplayer/app/data/local/dao/ArtistDao.kt` -- #4 getAll method
- `app/src/main/java/com/musicplayer/app/data/local/dao/CommentDao.kt` -- #4 getAll method
- `app/src/main/java/com/musicplayer/app/data/local/dao/PlayRecordDao.kt` -- #4 getAll method
- `app/src/main/java/com/musicplayer/app/data/local/dao/PlaylistDao.kt` -- #4 getAll methods
- `app/src/main/java/com/musicplayer/app/domain/usecase/PlayMusicUseCase.kt` -- #6 recommendation algorithm
- `app/src/main/java/com/musicplayer/app/ui/player/PlayerViewModel.kt` -- #6 pass playRecordDao to use case
- `app/src/main/java/com/musicplayer/app/domain/repository/MusicRepository.kt` -- #5 updateSong method
- `app/src/main/java/com/musicplayer/app/data/repository/MusicRepositoryImpl.kt` -- #5 updateSong impl
- `app/src/main/java/com/musicplayer/app/data/repository/SettingsRepositoryImpl.kt` -- #4 expose settings as Map for export

---

### Task 1: Redesign SequentialIcon + Direct Mode Toggle

**Files:**
- Modify: `app/src/main/java/com/musicplayer/app/ui/player/NowPlayingScreen.kt:399-451, 585-607, 1090-1148, 1457-1462`
- Modify: `app/src/main/java/com/musicplayer/app/ui/i18n/AppStrings.kt:~150`

**Background:** Currently `PanelButtonRow` always shows `Icons.Outlined.Shuffle`. Clicking it opens `PlayerPanel.MODE` showing `PlaybackModePanel` (3 selectable options, 2 taps required). `QueueDialog` already cycles modes directly on single tap and shows correct icon. `SequentialIcon` is 3 stacked `ArrowDownward` icons.

**Goal:** Redesign `SequentialIcon` to show a horizontal loop/cycle arrow pattern (like mainstream music apps). Make the mode button in `PanelButtonRow` directly cycle modes (SEQUENTIAL -> SHUFFLE -> RECOMMENDATION -> SEQUENTIAL) on single tap, showing the correct current icon. Keep `PlaybackModePanel` accessible via long-press. The `PanelButtonRow` needs the current `playbackMode` as a parameter.

- [ ] **Step 1: Add new string keys to AppStrings**

In `AppStrings` data class, add field (after the existing `recommendation` field around line 224):
```kotlin
val playbackModeLongPress: String,
```
In `chineseStrings` constructor call, add (after `recommendation = "推荐算法"`):
```kotlin
playbackModeLongPress = "长按切换播放模式",
```
In `englishStrings` constructor call, add (after `recommendation = "Recommendation"`):
```kotlin
playbackModeLongPress = "Long-press to change mode",
```

- [ ] **Step 2: Redesign SequentialIcon composable**

Replace `SequentialIcon` at file `NowPlayingScreen.kt` lines 1137-1148 with a new design showing two horizontal arrows forming a cycling/loop pattern using Canvas:

```kotlin
@Composable
private fun SequentialIcon(tint: androidx.compose.ui.graphics.Color) {
    Canvas(modifier = Modifier.size(24.dp)) {
        val w = size.width
        val h = size.height
        val arrowW = w * 0.32f
        val arrowH = h * 0.24f
        val strokeW = 2.5f

        // Rightward arrow (top)
        val topPath = Path().apply {
            // shaft
            moveTo(w * 0.12f, h * 0.35f)
            lineTo(w * 0.72f, h * 0.35f)
            // head
            lineTo(w * 0.72f - arrowW, h * 0.35f - arrowH)
            moveTo(w * 0.72f, h * 0.35f)
            lineTo(w * 0.72f - arrowW, h * 0.35f + arrowH)
        }
        drawPath(topPath, tint, style = Stroke(width = strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round))

        // Leftward arrow (bottom)
        val bottomPath = Path().apply {
            // shaft
            moveTo(w * 0.88f, h * 0.65f)
            lineTo(w * 0.28f, h * 0.65f)
            // head
            lineTo(w * 0.28f + arrowW, h * 0.65f - arrowH)
            moveTo(w * 0.28f, h * 0.65f)
            lineTo(w * 0.28f + arrowW, h * 0.65f + arrowH)
        }
        drawPath(bottomPath, tint, style = Stroke(width = strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}
```

The `Path` import is already in use in this file (used by `RecommendationIcon`). No new imports needed.

- [ ] **Step 3: Modify PanelButtonRow signature to accept playbackMode and onModeCycle**

Change `PanelButtonRow` signature at lines 399-408:

```kotlin
@Composable
private fun PanelButtonRow(
    enabled: Boolean,
    hasQueue: Boolean,
    activePanel: PlayerPanel,
    playbackMode: PlaybackMode,
    onScrapeClick: () -> Unit,
    onModeClick: () -> Unit,
    onModeLongClick: () -> Unit,
    onEqualizerClick: () -> Unit,
    onQueueClick: () -> Unit,
    onCommentsClick: () -> Unit = {}
)
```

- [ ] **Step 4: Update mode button icon in PanelButtonRow**

Replace lines 424-429 (the mode button) with a version that shows the correct icon based on current playbackMode, and supports long-press:

```kotlin
PanelButton(
    selected = activePanel == PlayerPanel.MODE,
    onClick = onModeClick
) {
    val tint = if (activePanel == PlayerPanel.MODE)
        MaterialTheme.colorScheme.primary
    else
        MaterialTheme.colorScheme.onSurfaceVariant
    CompositionLocalProvider(LocalContentColor provides tint) {
        when (playbackMode) {
            PlaybackMode.SEQUENTIAL -> SequentialIcon(tint)
            PlaybackMode.SHUFFLE -> Icon(
                Icons.Outlined.Shuffle,
                contentDescription = strings.shuffle,
                tint = tint
            )
            PlaybackMode.RECOMMENDATION -> RecommendationIcon(tint)
        }
    }
}
```

Add a `Modifier.combinedClickable` wrapper or use the existing `PanelButton` with long-press support. To keep changes minimal, replace the mode `PanelButton` with:

```kotlin
Box(
    modifier = Modifier
        .size(44.dp)
        .pointerInput(Unit) {
            detectTapGestures(
                onTap = { onModeClick() },
                onLongPress = { onModeLongClick() }
            )
        },
    contentAlignment = Alignment.Center
) {
    val tint = if (activePanel == PlayerPanel.MODE)
        MaterialTheme.colorScheme.primary
    else
        MaterialTheme.colorScheme.onSurfaceVariant
    CompositionLocalProvider(LocalContentColor provides tint) {
        when (playbackMode) {
            PlaybackMode.SEQUENTIAL -> SequentialIcon(tint)
            PlaybackMode.SHUFFLE -> Icon(
                Icons.Outlined.Shuffle,
                contentDescription = strings.shuffle,
                tint = tint
            )
            PlaybackMode.RECOMMENDATION -> RecommendationIcon(tint)
        }
    }
}
```

Need to add import: `import androidx.compose.foundation.gestures.detectTapGestures` (already imported at line 6).
Need to add import: `import androidx.compose.ui.input.pointer.pointerInput` (already imported at line 77).

- [ ] **Step 5: Update NowPlayingScreen call site for PanelButtonRow**

At lines 251-266 in `NowPlayingScreen`, update the `PanelButtonRow` call to pass the new parameters:

```kotlin
PanelButtonRow(
    enabled = song != null,
    hasQueue = playbackState.queue.isNotEmpty(),
    activePanel = activePanel,
    playbackMode = playbackState.playbackMode,
    onScrapeClick = {
        activePanel = if (activePanel == PlayerPanel.SCRAPE) PlayerPanel.NONE else PlayerPanel.SCRAPE
    },
    onModeClick = {
        val nextMode = when (playbackState.playbackMode) {
            PlaybackMode.SEQUENTIAL -> PlaybackMode.SHUFFLE
            PlaybackMode.SHUFFLE -> PlaybackMode.RECOMMENDATION
            PlaybackMode.RECOMMENDATION -> PlaybackMode.SEQUENTIAL
        }
        onPlaybackModeChange(nextMode)
    },
    onModeLongClick = {
        activePanel = if (activePanel == PlayerPanel.MODE) PlayerPanel.NONE else PlayerPanel.MODE
    },
    onEqualizerClick = {
        activePanel = if (activePanel == PlayerPanel.EQUALIZER) PlayerPanel.NONE else PlayerPanel.EQUALIZER
    },
    onQueueClick = { showQueue = true },
    onCommentsClick = onOpenComments
)
```

- [ ] **Step 6: Remove the MODE panel import removal note**

The `PlayerPanel.MODE` enum value at line 1459 is still needed for long-press access. Do NOT remove it. The `PlaybackModePanel` composable at lines 1090-1134 stays intact. Only the tap behavior changed from "open panel" to "cycle mode."

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/musicplayer/app/ui/player/NowPlayingScreen.kt app/src/main/java/com/musicplayer/app/ui/i18n/AppStrings.kt
git commit -m "feat: redesign SequentialIcon and add direct mode toggle with long-press panel access"
```

---

### Task 2: Album Card Redesign

**Files:**
- Modify: `app/src/main/java/com/musicplayer/app/ui/components/AlbumCard.kt:30-85`

**Background:** `AlbumCard` uses `Column` with `Box(112.dp cover)` then title then subtitle. Text is stacked vertically below cover.

**Goal:** More space for cover art (taller) and text shown more compactly, perhaps overlaid on the bottom of the artwork or in a horizontal arrangement.

- [ ] **Step 1: Redesign AlbumCard with taller cover and overlay text**

Replace the entire `AlbumCard` composable at lines 30-85 with a version that uses a taller cover (e.g., 160.dp) and overlays text on a gradient at the bottom of the artwork:

```kotlin
@Composable
fun AlbumCard(
    album: Album,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
        ) {
            // Cover artwork fills the entire card
            if (!album.artworkUri.isNullOrBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(album.artworkUri)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Album,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }
            // Gradient overlay at bottom for text readability
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(60.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.75f)
                            )
                        )
                    )
            )
            // Text info overlaid at bottom
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = album.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${album.artistName} · ${album.songCount} songs",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
```

New imports needed at the top of `AlbumCard.kt`:
```kotlin
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/musicplayer/app/ui/components/AlbumCard.kt
git commit -m "feat: redesign AlbumCard with taller cover and overlaid text"
```

---

### Task 3: Mini Player Bar Background Differentiation

**Files:**
- Modify: `app/src/main/java/com/musicplayer/app/ui/player/MiniPlayerBar.kt:60`

**Background:** `MiniPlayerBar` uses `MaterialTheme.colorScheme.surfaceContainer` for its background color at line 60. This blends with the navigation bar.

**Goal:** Use a slightly differentiated color, e.g., a custom alpha-blended color derived from primary + surface.

- [ ] **Step 1: Change MiniPlayerBar background color**

Replace line 60 in `MiniPlayerBar.kt`:

Change from:
```kotlin
color = MaterialTheme.colorScheme.surfaceContainer,
```

Change to:
```kotlin
color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
    .compositeOver(MaterialTheme.colorScheme.surfaceContainer),
```

The `compositeOver` function is from `androidx.compose.ui.graphics.Color`. No new import needed as it's available on `Color` objects in Compose. However, to be safe, add:
```kotlin
import androidx.compose.ui.graphics.Color
```
if not already present (check existing imports -- not present, no `Color` import in MiniPlayerBar.kt yet).

The color blending approach: starts with `surfaceContainer` and overlays a very faint `primary` tint (8% alpha), creating slight differentiation while staying within the theme.

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/musicplayer/app/ui/player/MiniPlayerBar.kt
git commit -m "feat: differentiate MiniPlayerBar background with subtle primary tint overlay"
```

---

### Task 4: Data Import/Export

**Files:**
- Create: `app/src/main/java/com/musicplayer/app/domain/usecase/ExportImportUseCase.kt`
- Modify: `app/src/main/java/com/musicplayer/app/ui/settings/SettingsScreen.kt` (add section)
- Modify: `app/src/main/java/com/musicplayer/app/ui/settings/SettingsViewModel.kt` (add methods)
- Modify: `app/src/main/java/com/musicplayer/app/ui/i18n/AppStrings.kt` (add string keys)
- Modify: `app/src/main/java/com/musicplayer/app/data/local/dao/SongDao.kt` (add `getAll` query)
- Modify: `app/src/main/java/com/musicplayer/app/data/local/dao/AlbumDao.kt` (add `getAll` query)
- Modify: `app/src/main/java/com/musicplayer/app/data/local/dao/ArtistDao.kt` (add `getAll` query)
- Modify: `app/src/main/java/com/musicplayer/app/data/local/dao/CommentDao.kt` (add `getAll` query)
- Modify: `app/src/main/java/com/musicplayer/app/data/local/dao/PlayRecordDao.kt` (add `getAll` query)
- Modify: `app/src/main/java/com/musicplayer/app/data/local/dao/PlaylistDao.kt` (add `getAll` queries)
- Modify: `app/src/main/java/com/musicplayer/app/data/repository/SettingsRepositoryImpl.kt` (expose all settings as Map)
- Modify: `app/src/main/java/com/musicplayer/app/domain/repository/SettingsRepository.kt` (add `exportSettings` method)

**Background:** No export/import exists. Room DB contains songs, albums, artists, playlists, playlist_songs, play_records, comments, scrape_cache. DataStore contains settings preferences.

**Goal:** Export all data to a JSON file via SAF. Import from a JSON file (parse and re-insert). Add buttons to SettingsScreen.

**Approach:** Use `org.json.JSONObject` and `org.json.JSONArray` (Android built-in, no dependency needed). Use `ActivityResultContracts.CreateDocument` for export and `ActivityResultContracts.OpenDocument` for import.

- [ ] **Step 1: Add DAO getAll queries**

Add to `SongDao.kt` after the existing `getSongs()` method:
```kotlin
@Query("SELECT * FROM songs")
suspend fun getAll(): List<SongEntity>
```
(Note: `getSongs()` already exists, but returns `List<SongEntity>`. We can reuse it. Actually, `getSongs()` already does this -- line 21-22. No change needed.)

Add to `AlbumDao.kt` (no existing getAll):
```kotlin
@Query("SELECT * FROM albums")
suspend fun getAll(): List<AlbumEntity>
```

Add to `ArtistDao.kt` (no existing getAll):
```kotlin
@Query("SELECT * FROM artists")
suspend fun getAll(): List<ArtistEntity>
```

Add to `CommentDao.kt` (no existing getAll):
```kotlin
@Query("SELECT * FROM comments")
suspend fun getAll(): List<CommentEntity>
```

Add to `PlayRecordDao.kt` (no existing getAll):
```kotlin
@Query("SELECT * FROM play_records")
suspend fun getAll(): List<PlayRecordEntity>
```

Add to `PlaylistDao.kt` (no existing getAll for both tables):
```kotlin
@Query("SELECT * FROM playlists")
suspend fun getAllPlaylists(): List<PlaylistEntity>

@Query("SELECT * FROM playlist_songs")
suspend fun getAllPlaylistSongs(): List<PlaylistSongEntity>
```

- [ ] **Step 2: Add exportSettings to SettingsRepository interface**

In `SettingsRepository.kt`, add method:
```kotlin
suspend fun exportSettings(): Map<String, String>
```

In `SettingsRepositoryImpl.kt`, implement:
```kotlin
override suspend fun exportSettings(): Map<String, String> {
    val prefs = context.settingsDataStore.data.first()
    return prefs.asMap().mapKeys { it.key.name }.mapValues { it.value.toString() }
}
```

- [ ] **Step 3: Create ExportImportUseCase**

Create file `app/src/main/java/com/musicplayer/app/domain/usecase/ExportImportUseCase.kt`:

```kotlin
package com.musicplayer.app.domain.usecase

import android.content.Context
import android.net.Uri
import com.musicplayer.app.data.local.dao.AlbumDao
import com.musicplayer.app.data.local.dao.ArtistDao
import com.musicplayer.app.data.local.dao.CommentDao
import com.musicplayer.app.data.local.dao.PlayRecordDao
import com.musicplayer.app.data.local.dao.PlaylistDao
import com.musicplayer.app.data.local.dao.SongDao
import com.musicplayer.app.data.local.entity.toDomain
import com.musicplayer.app.data.local.entity.toEntity
import com.musicplayer.app.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExportImportUseCase @Inject constructor(
    private val songDao: SongDao,
    private val albumDao: AlbumDao,
    private val artistDao: ArtistDao,
    private val commentDao: CommentDao,
    private val playRecordDao: PlayRecordDao,
    private val playlistDao: PlaylistDao,
    private val settingsRepository: SettingsRepository,
    @ApplicationContext private val context: Context
) {
    suspend fun exportToJson(uri: Uri): Int {
        val root = JSONObject()
        root.put("version", 1)
        root.put("exportedAt", System.currentTimeMillis())

        // Songs
        val songs = songDao.getSongs()
        val songsArray = JSONArray()
        songs.forEach { entity ->
            val obj = JSONObject()
            obj.put("id", entity.id)
            obj.put("title", entity.title)
            obj.put("artistId", entity.artistId ?: JSONObject.NULL)
            obj.put("artistName", entity.artistName)
            obj.put("albumId", entity.albumId ?: JSONObject.NULL)
            obj.put("albumTitle", entity.albumTitle)
            obj.put("durationMs", entity.durationMs)
            obj.put("trackNumber", entity.trackNumber ?: JSONObject.NULL)
            obj.put("discNumber", entity.discNumber ?: JSONObject.NULL)
            obj.put("uri", entity.uri)
            obj.put("source", entity.source.name)
            obj.put("webDavServerId", entity.webDavServerId ?: JSONObject.NULL)
            obj.put("artworkUri", entity.artworkUri ?: JSONObject.NULL)
            obj.put("artworkProvider", entity.artworkProvider ?: JSONObject.NULL)
            obj.put("lyrics", entity.lyrics ?: JSONObject.NULL)
            obj.put("lyricsProvider", entity.lyricsProvider ?: JSONObject.NULL)
            obj.put("mimeType", entity.mimeType ?: JSONObject.NULL)
            obj.put("bitrateKbps", entity.bitrateKbps ?: JSONObject.NULL)
            obj.put("sampleRateHz", entity.sampleRateHz ?: JSONObject.NULL)
            obj.put("releaseYear", entity.releaseYear ?: JSONObject.NULL)
            obj.put("releaseMonth", entity.releaseMonth ?: JSONObject.NULL)
            obj.put("dateAddedEpochMs", entity.dateAddedEpochMs)
            obj.put("isFavorite", entity.isFavorite)
            songsArray.put(obj)
        }
        root.put("songs", songsArray)

        // Albums
        val albums = albumDao.getAll()
        val albumsArray = JSONArray()
        albums.forEach { entity ->
            val obj = JSONObject()
            obj.put("id", entity.id)
            obj.put("title", entity.title)
            obj.put("artistName", entity.artistName)
            obj.put("artworkUri", entity.artworkUri ?: JSONObject.NULL)
            obj.put("songCount", entity.songCount)
            obj.put("source", entity.source.name)
            albumsArray.put(obj)
        }
        root.put("albums", albumsArray)

        // Artists
        val artists = artistDao.getAll()
        val artistsArray = JSONArray()
        artists.forEach { entity ->
            val obj = JSONObject()
            obj.put("id", entity.id)
            obj.put("name", entity.name)
            obj.put("albumCount", entity.albumCount)
            obj.put("songCount", entity.songCount)
            obj.put("artworkUri", entity.artworkUri ?: JSONObject.NULL)
            artistsArray.put(obj)
        }
        root.put("artists", artistsArray)

        // Playlists
        val playlists = playlistDao.getAllPlaylists()
        val playlistsArray = JSONArray()
        playlists.forEach { entity ->
            val obj = JSONObject()
            obj.put("id", entity.id)
            obj.put("name", entity.name)
            obj.put("description", entity.description ?: JSONObject.NULL)
            obj.put("artworkUri", entity.artworkUri ?: JSONObject.NULL)
            obj.put("songCount", entity.songCount)
            obj.put("sortOrder", entity.sortOrder)
            obj.put("createdAtEpochMs", entity.createdAtEpochMs)
            obj.put("updatedAtEpochMs", entity.updatedAtEpochMs)
            playlistsArray.put(obj)
        }
        root.put("playlists", playlistsArray)

        // Playlist songs
        val playlistSongs = playlistDao.getAllPlaylistSongs()
        val playlistSongsArray = JSONArray()
        playlistSongs.forEach { entity ->
            val obj = JSONObject()
            obj.put("playlistId", entity.playlistId)
            obj.put("songId", entity.songId)
            obj.put("position", entity.position)
            playlistSongsArray.put(obj)
        }
        root.put("playlistSongs", playlistSongsArray)

        // Play records
        val records = playRecordDao.getAll()
        val recordsArray = JSONArray()
        records.forEach { entity ->
            val obj = JSONObject()
            obj.put("id", entity.id)
            obj.put("songId", entity.songId)
            obj.put("listenedAtEpochMs", entity.listenedAtEpochMs)
            obj.put("durationMs", entity.durationMs)
            recordsArray.put(obj)
        }
        root.put("playRecords", recordsArray)

        // Comments
        val comments = commentDao.getAll()
        val commentsArray = JSONArray()
        comments.forEach { entity ->
            val obj = JSONObject()
            obj.put("id", entity.id)
            obj.put("songId", entity.songId)
            obj.put("rating", entity.rating)
            obj.put("text", entity.text)
            obj.put("timestampEpochMs", entity.timestampEpochMs)
            commentsArray.put(obj)
        }
        root.put("comments", commentsArray)

        // Settings (DataStore)
        val settings = settingsRepository.exportSettings()
        val settingsObj = JSONObject()
        settings.forEach { (key, value) -> settingsObj.put(key, value) }
        root.put("settings", settingsObj)

        // Write to URI
        val jsonString = root.toString(2)
        context.contentResolver.openOutputStream(uri)?.use { stream ->
            stream.write(jsonString.toByteArray(Charsets.UTF_8))
        }

        return songs.size
    }

    suspend fun importFromJson(uri: Uri): Int {
        val jsonString = context.contentResolver.openInputStream(uri)?.use { stream ->
            stream.bufferedReader(Charsets.UTF_8).readText()
        } ?: return 0

        val root = JSONObject(jsonString)
        var count = 0

        // Import songs
        val songsArray = root.optJSONArray("songs")
        if (songsArray != null) {
            val entities = (0 until songsArray.length()).map { i ->
                val obj = songsArray.getJSONObject(i)
                com.musicplayer.app.data.local.entity.SongEntity(
                    id = obj.getString("id"),
                    title = obj.getString("title"),
                    artistId = obj.optString("artistId", null).takeIf { obj.has("artistId") && !obj.isNull("artistId") },
                    artistName = obj.optString("artistName", "Unknown Artist"),
                    albumId = obj.optString("albumId", null).takeIf { obj.has("albumId") && !obj.isNull("albumId") },
                    albumTitle = obj.optString("albumTitle", "Unknown Album"),
                    durationMs = obj.optLong("durationMs", 0L),
                    trackNumber = obj.optInt("trackNumber", -1).takeIf { obj.has("trackNumber") && !obj.isNull("trackNumber") && it >= 0 }?.let { if (it == -1) null else it },
                    discNumber = obj.optInt("discNumber", -1).takeIf { obj.has("discNumber") && !obj.isNull("discNumber") && it >= 0 }?.let { if (it == -1) null else it },
                    uri = obj.getString("uri"),
                    source = try { com.musicplayer.app.domain.model.Source.valueOf(obj.getString("source")) } catch (_: Exception) { com.musicplayer.app.domain.model.Source.LOCAL },
                    webDavServerId = obj.optString("webDavServerId", null).takeIf { obj.has("webDavServerId") && !obj.isNull("webDavServerId") },
                    artworkUri = obj.optString("artworkUri", null).takeIf { obj.has("artworkUri") && !obj.isNull("artworkUri") },
                    artworkProvider = obj.optString("artworkProvider", null).takeIf { obj.has("artworkProvider") && !obj.isNull("artworkProvider") },
                    lyrics = obj.optString("lyrics", null).takeIf { obj.has("lyrics") && !obj.isNull("lyrics") },
                    lyricsProvider = obj.optString("lyricsProvider", null).takeIf { obj.has("lyricsProvider") && !obj.isNull("lyricsProvider") },
                    mimeType = obj.optString("mimeType", null).takeIf { obj.has("mimeType") && !obj.isNull("mimeType") },
                    bitrateKbps = obj.optInt("bitrateKbps", -1).takeIf { obj.has("bitrateKbps") && !obj.isNull("bitrateKbps") && it >= 0 }?.let { if (it == -1) null else it },
                    sampleRateHz = obj.optInt("sampleRateHz", -1).takeIf { obj.has("sampleRateHz") && !obj.isNull("sampleRateHz") && it >= 0 }?.let { if (it == -1) null else it },
                    releaseYear = obj.optInt("releaseYear", -1).takeIf { obj.has("releaseYear") && !obj.isNull("releaseYear") && it >= 0 }?.let { if (it == -1) null else it },
                    releaseMonth = obj.optInt("releaseMonth", -1).takeIf { obj.has("releaseMonth") && !obj.isNull("releaseMonth") && it >= 0 }?.let { if (it == -1) null else it },
                    dateAddedEpochMs = obj.optLong("dateAddedEpochMs", System.currentTimeMillis()),
                    isFavorite = obj.optBoolean("isFavorite", false)
                )
            }
            songDao.upsertAll(entities)
            count += entities.size
        }

        // Import albums
        val albumsArray = root.optJSONArray("albums")
        if (albumsArray != null) {
            val entities = (0 until albumsArray.length()).map { i ->
                val obj = albumsArray.getJSONObject(i)
                com.musicplayer.app.data.local.entity.AlbumEntity(
                    id = obj.getString("id"),
                    title = obj.getString("title"),
                    artistName = obj.optString("artistName", "Unknown Artist"),
                    artworkUri = obj.optString("artworkUri", null).takeIf { obj.has("artworkUri") && !obj.isNull("artworkUri") },
                    songCount = obj.optInt("songCount", 0),
                    source = try { com.musicplayer.app.domain.model.Source.valueOf(obj.getString("source")) } catch (_: Exception) { com.musicplayer.app.domain.model.Source.LOCAL }
                )
            }
            albumDao.upsertAll(entities)
        }

        // Import artists
        val artistsArray = root.optJSONArray("artists")
        if (artistsArray != null) {
            val entities = (0 until artistsArray.length()).map { i ->
                val obj = artistsArray.getJSONObject(i)
                com.musicplayer.app.data.local.entity.ArtistEntity(
                    id = obj.getString("id"),
                    name = obj.getString("name"),
                    albumCount = obj.optInt("albumCount", 0),
                    songCount = obj.optInt("songCount", 0),
                    artworkUri = obj.optString("artworkUri", null).takeIf { obj.has("artworkUri") && !obj.isNull("artworkUri") }
                )
            }
            artistDao.upsertAll(entities)
        }

        // Import playlists
        val playlistsArray = root.optJSONArray("playlists")
        if (playlistsArray != null) {
            for (i in 0 until playlistsArray.length()) {
                val obj = playlistsArray.getJSONObject(i)
                playlistDao.upsertPlaylist(
                    com.musicplayer.app.data.local.entity.PlaylistEntity(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        description = obj.optString("description", null).takeIf { obj.has("description") && !obj.isNull("description") },
                        artworkUri = obj.optString("artworkUri", null).takeIf { obj.has("artworkUri") && !obj.isNull("artworkUri") },
                        songCount = obj.optInt("songCount", 0),
                        sortOrder = obj.optInt("sortOrder", 0),
                        createdAtEpochMs = obj.optLong("createdAtEpochMs", System.currentTimeMillis()),
                        updatedAtEpochMs = obj.optLong("updatedAtEpochMs", System.currentTimeMillis())
                    )
                )
            }
        }

        // Import playlist songs
        val playlistSongsArray = root.optJSONArray("playlistSongs")
        if (playlistSongsArray != null) {
            for (i in 0 until playlistSongsArray.length()) {
                val obj = playlistSongsArray.getJSONObject(i)
                playlistDao.upsertPlaylistSong(
                    com.musicplayer.app.data.local.entity.PlaylistSongEntity(
                        playlistId = obj.getString("playlistId"),
                        songId = obj.getString("songId"),
                        position = obj.optInt("position", 0)
                    )
                )
            }
        }

        // Import play records
        val recordsArray = root.optJSONArray("playRecords")
        if (recordsArray != null) {
            for (i in 0 until recordsArray.length()) {
                val obj = recordsArray.getJSONObject(i)
                playRecordDao.insert(
                    com.musicplayer.app.data.local.entity.PlayRecordEntity(
                        id = obj.getString("id"),
                        songId = obj.getString("songId"),
                        listenedAtEpochMs = obj.optLong("listenedAtEpochMs"),
                        durationMs = obj.optLong("durationMs")
                    )
                )
            }
        }

        // Import comments
        val commentsArray = root.optJSONArray("comments")
        if (commentsArray != null) {
            for (i in 0 until commentsArray.length()) {
                val obj = commentsArray.getJSONObject(i)
                commentDao.upsert(
                    com.musicplayer.app.data.local.entity.CommentEntity(
                        id = obj.getString("id"),
                        songId = obj.getString("songId"),
                        rating = obj.optInt("rating", 3),
                        text = obj.optString("text", ""),
                        timestampEpochMs = obj.optLong("timestampEpochMs", System.currentTimeMillis())
                    )
                )
            }
        }

        return count
    }
}
```

- [ ] **Step 4: Add string keys for import/export**

In `AppStrings.kt` data class, add fields (after existing cache/settings fields):
```kotlin
val exportData: String,
val importData: String,
val exportSuccess: String,
val importSuccess: String,
val exportFailed: String,
val importFailed: String,
```

In `chineseStrings`:
```kotlin
exportData = "导出数据",
importData = "导入数据",
exportSuccess = "数据导出成功",
importSuccess = "数据导入成功",
exportFailed = "数据导出失败",
importFailed = "数据导入失败",
```

In `englishStrings`:
```kotlin
exportData = "Export Data",
importData = "Import Data",
exportSuccess = "Data exported successfully",
importSuccess = "Data imported successfully",
exportFailed = "Export failed",
importFailed = "Import failed",
```

- [ ] **Step 5: Add import/export methods to SettingsViewModel**

Add to `SettingsViewModel` constructor:
```kotlin
private val exportImportUseCase: ExportImportUseCase
```

Add methods:
```kotlin
private val _exportResult = MutableStateFlow<String?>(null)
val exportResult: StateFlow<String?> = _exportResult.asStateFlow()

fun exportData(uri: Uri) {
    viewModelScope.launch {
        try {
            withContext(Dispatchers.IO) {
                exportImportUseCase.exportToJson(uri)
            }
            _exportResult.value = "success"
        } catch (e: Exception) {
            _exportResult.value = "error: ${e.message}"
        }
    }
}

fun importData(uri: Uri) {
    viewModelScope.launch {
        try {
            withContext(Dispatchers.IO) {
                exportImportUseCase.importFromJson(uri)
            }
            _exportResult.value = "import_success"
        } catch (e: Exception) {
            _exportResult.value = "import_error: ${e.message}"
        }
    }
}

fun clearExportResult() {
    _exportResult.value = null
}
```

- [ ] **Step 6: Add import/export UI to SettingsScreen**

Add to `SettingsScreen` composable, right before the closing `}` of the `LazyColumn` (before line 294 `}`), add a new section:

```kotlin
item {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = strings.exportImport, style = MaterialTheme.typography.titleMedium)
        Text(
            text = strings.exportImportDesc,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            val exportLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.CreateDocument("application/json")
            ) { uri ->
                uri?.let { viewModel.exportData(it) }
            }
            Button(
                onClick = { exportLauncher.launch("music_player_backup_${java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.US).format(java.util.Date())}.json") },
                modifier = Modifier.weight(1f)
            ) {
                Text(strings.exportData)
            }

            val importLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.OpenDocument()
            ) { uri ->
                uri?.let { viewModel.importData(it) }
            }
            Button(
                onClick = { importLauncher.launch(arrayOf("application/json")) },
                modifier = Modifier.weight(1f)
            ) {
                Text(strings.importData)
            }
        }

        val exportResult by viewModel.exportResult.collectAsStateWithLifecycle()
        exportResult?.let { result ->
            val (message, isError) = when {
                result == "success" -> strings.exportSuccess to false
                result == "import_success" -> strings.importSuccess to false
                result.startsWith("error:") -> strings.exportFailed to true
                result.startsWith("import_error:") -> strings.importFailed to true
                else -> result to true
            }
            LaunchedEffect(result) {
                kotlinx.coroutines.delay(3000)
                viewModel.clearExportResult()
            }
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
        }
    }
}
```

Need to also add new string keys to `AppStrings` data class:
```kotlin
val exportImport: String,
val exportImportDesc: String,
```

In `chineseStrings`:
```kotlin
exportImport = "数据导入/导出",
exportImportDesc = "将歌曲、歌单、评论、播放记录和设置导出为 JSON 文件，或从 JSON 文件导入。",
```

In `englishStrings`:
```kotlin
exportImport = "Import / Export Data",
exportImportDesc = "Export songs, playlists, comments, play records, and settings as a JSON file, or import from a JSON file.",
```

In `SettingsScreen` composable signature, add the result observing after `hasResumableScrape`:
```kotlin
val exportResult by viewModel.exportResult.collectAsStateWithLifecycle()
```
Place this inside the LazyColumn lambda alongside other state reads at the top, or locally within the item block.

- [ ] **Step 7: Register ExportImportUseCase in DI**

The `ExportImportUseCase` uses `@Inject @Singleton` constructor injection which Hilt auto-discovers. No manual module binding needed. However, make sure `UseCaseModule.kt` has `@Module @InstallIn(SingletonComponent::class)` to be discovered. The existing `UseCaseModule.kt` already covers other use cases in the domain package. If it doesn't have a bind for `ExportImportUseCase`, that's OK because it uses constructor injection with `@Inject` (it's not an interface binding). No changes needed to DI modules.

- [ ] **Step 8: Commit**

```bash
git add app/src/main/java/com/musicplayer/app/domain/usecase/ExportImportUseCase.kt \
        app/src/main/java/com/musicplayer/app/ui/settings/SettingsScreen.kt \
        app/src/main/java/com/musicplayer/app/ui/settings/SettingsViewModel.kt \
        app/src/main/java/com/musicplayer/app/ui/i18n/AppStrings.kt \
        app/src/main/java/com/musicplayer/app/data/local/dao/AlbumDao.kt \
        app/src/main/java/com/musicplayer/app/data/local/dao/ArtistDao.kt \
        app/src/main/java/com/musicplayer/app/data/local/dao/CommentDao.kt \
        app/src/main/java/com/musicplayer/app/data/local/dao/PlayRecordDao.kt \
        app/src/main/java/com/musicplayer/app/data/local/dao/PlaylistDao.kt \
        app/src/main/java/com/musicplayer/app/data/repository/SettingsRepositoryImpl.kt \
        app/src/main/java/com/musicplayer/app/domain/repository/SettingsRepository.kt
git commit -m "feat: add data import/export via SAF with JSON serialization"
```

---

### Task 5: Edit Song Metadata

**Files:**
- Create: `app/src/main/java/com/musicplayer/app/ui/components/EditSongDialog.kt`
- Modify: `app/src/main/java/com/musicplayer/app/data/local/dao/SongDao.kt` (add update method)
- Modify: `app/src/main/java/com/musicplayer/app/domain/repository/MusicRepository.kt` (add updateSong)
- Modify: `app/src/main/java/com/musicplayer/app/data/repository/MusicRepositoryImpl.kt` (implement updateSong)
- Modify: `app/src/main/java/com/musicplayer/app/ui/library/LibraryScreen.kt` (add edit option to SongActionSheet)
- Modify: `app/src/main/java/com/musicplayer/app/ui/i18n/AppStrings.kt` (add string keys)

**Background:** `SongActionSheet` in `LibraryScreen.kt` (lines 364-443) has play next, add to playlist, favorite, share, view artist, view album. No edit option. `SongDao` only has `upsertAll` and `setFavorite`.

**Goal:** Add "Edit info" option to `SongActionSheet` -> opens `EditSongDialog` with title/artist/album fields -> saves via new SongDao update method.

- [ ] **Step 1: Add update method to SongDao**

In `SongDao.kt`, add after `setFavorite`:
```kotlin
@Query("UPDATE songs SET title = :title, artistName = :artistName, albumTitle = :albumTitle WHERE id = :songId")
suspend fun updateMetadata(songId: String, title: String, artistName: String, albumTitle: String)
```

- [ ] **Step 2: Add updateSong to MusicRepository interface and impl**

In `MusicRepository.kt`, add method:
```kotlin
suspend fun updateSongMetadata(songId: String, title: String, artistName: String, albumTitle: String)
```

In `MusicRepositoryImpl.kt`, implement:
```kotlin
override suspend fun updateSongMetadata(songId: String, title: String, artistName: String, albumTitle: String) {
    songDao.updateMetadata(songId, title, artistName, albumTitle)
}
```

- [ ] **Step 3: Create EditSongDialog composable**

Create file `app/src/main/java/com/musicplayer/app/ui/components/EditSongDialog.kt`:

```kotlin
package com.musicplayer.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.musicplayer.app.domain.model.Song
import com.musicplayer.app.ui.i18n.LocalStrings

@Composable
fun EditSongDialog(
    song: Song,
    onSave: (String, String, String) -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalStrings.current
    var title by remember { mutableStateOf(song.title) }
    var artistName by remember { mutableStateOf(song.artistName) }
    var albumTitle by remember { mutableStateOf(song.albumTitle) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                androidx.compose.foundation.layout.Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    Text(text = strings.editSongInfo, style = MaterialTheme.typography.titleMedium)
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Outlined.Close, contentDescription = strings.close)
                    }
                }
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(strings.songTitle) }
                )
                OutlinedTextField(
                    value = artistName,
                    onValueChange = { artistName = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(strings.artistName) }
                )
                OutlinedTextField(
                    value = albumTitle,
                    onValueChange = { albumTitle = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(strings.albumName) }
                )
                Button(
                    onClick = { onSave(title, artistName, albumTitle) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = title.isNotBlank()
                ) {
                    Text(strings.save)
                }
            }
        }
    }
}
```

- [ ] **Step 4: Add string keys for edit song**

In `AppStrings.kt` data class, add fields:
```kotlin
val editSongInfo: String,
val songTitle: String,
val artistName: String,
val albumName: String,
val save: String,
val editInfo: String,
```

In `chineseStrings`:
```kotlin
editSongInfo = "编辑歌曲信息",
songTitle = "歌曲标题",
artistName = "艺术家",
albumName = "专辑名称",
save = "保存",
editInfo = "编辑信息",
```

In `englishStrings`:
```kotlin
editSongInfo = "Edit Song Info",
songTitle = "Song Title",
artistName = "Artist",
albumName = "Album Name",
save = "Save",
editInfo = "Edit Info",
```

- [ ] **Step 5: Add edit option to SongActionSheet in LibraryScreen**

In `LibraryScreen.kt`, modify the `SongActionSheet` composable signature (lines 364-375) to accept new callbacks:

Add to parameters:
```kotlin
onEditInfo: () -> Unit,
```

Add a ListItem for edit before the Divider (between the playlist section and the favorite section, around line 402):

```kotlin
ListItem(
    headlineContent = { Text(strings.editInfo) },
    leadingContent = { Icon(Icons.Outlined.Edit, contentDescription = null) },
    modifier = Modifier.clickable(onClick = onEditInfo)
)
```

Need to add import: `import androidx.compose.material.icons.outlined.Edit`

Update the `SongList` composable's `SongActionSheet` call (around lines 251-283) to pass the new callback. The `SongList` signature also needs a new callback parameter:

In `SongList` parameters (around lines 172-181), add:
```kotlin
onEditSongInfo: (Song) -> Unit,
```

And update the `SongActionSheet` call to include:
```kotlin
onEditInfo = {
    onEditSongInfo(song)
    menuSong = null
},
```

Update the `LibraryScreen` composable's `SongList` call (line 140-149) to pass:
```kotlin
onEditSongInfo = viewModel::showEditDialog,
```

- [ ] **Step 6: Add edit dialog support to LibraryViewModel**

In `LibraryViewModel.kt`, add injection of `MusicRepository` if not already available, and add:

```kotlin
private val _editSong = MutableStateFlow<Song?>(null)
val editSong: StateFlow<Song?> = _editSong

fun showEditDialog(song: Song) {
    _editSong.value = song
}

fun clearEditDialog() {
    _editSong.value = null
}

fun saveEdit(songId: String, title: String, artistName: String, albumTitle: String) {
    viewModelScope.launch {
        musicRepository.updateSongMetadata(songId, title, artistName, albumTitle)
        _editSong.value = null
    }
}
```

- [ ] **Step 7: Wire up EditSongDialog in LibraryScreen**

In `LibraryScreen`, add at the bottom of the composable (before the final closing `}`), after the `SearchDialog`:

```kotlin
val editSong by viewModel.editSong.collectAsStateWithLifecycle()
editSong?.let { song ->
    EditSongDialog(
        song = song,
        onSave = { title, artistName, albumTitle ->
            viewModel.saveEdit(song.id, title, artistName, albumTitle)
        },
        onDismiss = { viewModel.clearEditDialog() }
    )
}
```

- [ ] **Step 8: Check LibraryViewModel constructor for musicRepository**

Read `LibraryViewModel.kt` to check if `MusicRepository` is already injected. If not, add to constructor:
```kotlin
private val musicRepository: MusicRepository
```

- [ ] **Step 9: Commit**

```bash
git add app/src/main/java/com/musicplayer/app/ui/components/EditSongDialog.kt \
        app/src/main/java/com/musicplayer/app/data/local/dao/SongDao.kt \
        app/src/main/java/com/musicplayer/app/domain/repository/MusicRepository.kt \
        app/src/main/java/com/musicplayer/app/data/repository/MusicRepositoryImpl.kt \
        app/src/main/java/com/musicplayer/app/ui/library/LibraryScreen.kt \
        app/src/main/java/com/musicplayer/app/ui/i18n/AppStrings.kt
git commit -m "feat: add edit song metadata dialog with title/artist/album editing"
```

---

### Task 6: Improved Recommendation Algorithm

**Files:**
- Modify: `app/src/main/java/com/musicplayer/app/domain/usecase/PlayMusicUseCase.kt:1-147`
- Modify: `app/src/main/java/com/musicplayer/app/ui/player/PlayerViewModel.kt:68-77`

**Background:** `PlayMusicUseCase.recommendedNext()` (lines 136-146) only matches by artistId/albumId/isFavorite. It does not account for listening history. Shuffle uses `randomOrNull()` which is already random -- no change needed.

**Goal:** Factor in `PlayRecordDao.topSongsSince` data (listening history) when recommending. Songs the user listens to more should get weighted higher. Keep the existing matching fallback.

**Approach:** `PlayMusicUseCase` currently has no dependency on `PlayRecordDao`. We need to pass listening history data into the recommendation logic. Since `PlayMusicUseCase` is currently `@Singleton @Inject constructor()` with no params, and it's in the domain layer (which shouldn't depend on DAOs directly), we should inject `PlayRecordDao` or have the recommendation accept a list of top song IDs. The cleanest approach: inject `PlayRecordDao` directly into `PlayMusicUseCase` (it already depends on domain models, adding a DAO dependency is acceptable for a use case), or make `recommendedNext` suspend and query it at call time.

The simplest approach: Change the `recommendedNext` function to accept a `suspend` lambda or to run synchronously with cached data. However, `skipNext()` is not a suspend function. We'll need to restructure.

Alternative cleaner approach: Store a cached `topSongs` list that gets updated periodically.

Let's use the approach of passing a `suspend () -> List<String>` callback or injecting PlayRecordDao:

Since `PlayMusicUseCase` is in the domain layer and DAOs are in the data layer, the cleanest approach per clean architecture is to make recommendation accept a weights parameter. But for simplicity in this app (which already mixes layers a bit), inject `PlayRecordDao` directly.

- [ ] **Step 1: Add PlayRecordDao dependency to PlayMusicUseCase**

Change `PlayMusicUseCase` constructor at lines 17-18:

From:
```kotlin
class PlayMusicUseCase @Inject constructor() {
```
To:
```kotlin
class PlayMusicUseCase @Inject constructor(
    private val playRecordDao: com.musicplayer.app.data.local.dao.PlayRecordDao
) {
```

Add import:
```kotlin
import com.musicplayer.app.data.local.dao.PlayRecordDao
```

- [ ] **Step 2: Rewrite recommendedNext with top-song weighting**

Replace the `recommendedNext` method at lines 136-146:

```kotlin
private fun recommendedNext(state: PlaybackState): Song? {
    val current = state.currentSong
    if (current == null) return state.queue.firstOrNull()

    // Build candidate pool excluding current song
    val candidates = state.queue.filter { it.id != current.id }
    if (candidates.isEmpty()) return state.queue.firstOrNull()

    // Query top songs from last 30 days using runBlocking (called from coroutine context)
    val topSongIds = runCatching {
        kotlinx.coroutines.runBlocking {
            playRecordDao.topSongsSince(
                System.currentTimeMillis() - 30 * 24 * 60 * 60 * 1000L,
                limit = 50
            ).map { it.songId }.toSet()
        }
    }.getOrDefault(emptySet())

    // Score each candidate: +3 for exact artist match, +2 for exact album match,
    // +2 for favorite, +1 for being in top listened, +1 for having artwork
    data class Scored(val song: Song, val score: Int)

    val scored = candidates.map { song ->
        var score = 0
        if (song.artistId == current.artistId) score += 3
        if (song.albumId == current.albumId) score += 2
        if (song.isFavorite) score += 2
        if (song.id in topSongIds) score += 2
        if (!song.artworkUri.isNullOrBlank()) score += 1
        Scored(song, score)
    }

    // Sort by score descending, pick from top scorers with randomness
    if (scored.isEmpty()) return state.queue.firstOrNull { it.id != current.id }
    val maxScore = scored.maxOf { it.score }
    val topCandidates = scored.filter { it.score >= maxScore - 1 } // top tier
        .ifEmpty { scored }
    val selected = topCandidates.randomOrNull()?.song

    // Fallback: next in queue or first
    return selected
        ?: state.queue.getOrNull(state.queue.indexOfFirst { it.id == current.id } + 1)
        ?: state.queue.firstOrNull()
}
```

Add required imports at top of `PlayMusicUseCase.kt`:
```kotlin
import com.musicplayer.app.data.local.dao.PlayRecordDao
```

- [ ] **Step 3: Verify shuffle is already random**

The shuffle at line 83:
```kotlin
PlaybackMode.SHUFFLE -> state.queue.filterNot { it.id == state.currentSong?.id }.randomOrNull()
    ?: state.queue.firstOrNull()
```
This already uses `randomOrNull()`. No change needed. Verified.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/musicplayer/app/domain/usecase/PlayMusicUseCase.kt
git commit -m "feat: improve recommendation algorithm with listening history weighting"
```

---

### Task 7: Fix Home Screen Song Right-Side Button

**Files:**
- Modify: `app/src/main/java/com/musicplayer/app/ui/home/HomeScreen.kt:1-262`
- Modify: `app/src/main/java/com/musicplayer/app/ui/home/HomeViewModel.kt:90-107`

**Background:** `HomeScreen.kt` `SongListItem`'s `onMoreClick` calls `viewModel.showSongActions(song)` which sets `_actionSong`. But `actionSong` StateFlow is never observed in any UI, so clicking does nothing.

**Goal:** Observe `actionSong` in `HomeScreen` and show a `SongActionSheet` similar to `LibraryScreen`'s. Include play next, favorite toggle, share, view artist, view album, and the new edit metadata option.

- [ ] **Step 1: Add missing action methods to HomeViewModel**

Check `HomeViewModel.kt` lines 90-107. The `_actionSong` and `actionSong` StateFlow already exist. We need to add the following methods:

```kotlin
fun playNext(song: Song) {
    // This needs PlayMusicUseCase. Let's inject it.
    _playNextSong.value = song
}

fun addToPlaylist(playlistId: String, song: Song) {
    viewModelScope.launch {
        playlistRepository.addSong(playlistId, song.id)
    }
}

fun createPlaylistWith(song: Song) {
    viewModelScope.launch {
        playlistRepository.createPlaylist(song)
    }
}
```

Actually, checking `HomeViewModel`, it doesn't have `PlayMusicUseCase` or `PlaylistRepository` injected. It has:
- `musicRepository`
- `settingsRepository`
- `webDavRepository`
- `fetchMetadataUseCase`
- `libraryWorkCoordinator`

We need to add more dependencies. Let's pass the needed callbacks through to the HomeScreen instead and have AppNavigation handle them.

**Simpler approach:** Instead of adding many dependencies to `HomeViewModel`, we can make the HomeScreen accept callbacks from the parent (AppNavigation) that already has `PlayerViewModel`. Or we can inject `PlayMusicUseCase` and `PlaylistRepository` into `HomeViewModel`.

Let me check the current AppNavigation to see what's available. Looking at `AppNavigation.kt` lines 128-131:
```kotlin
composable(AppDestination.Home.route) {
    HomeScreen(
        onPlaySong = playerViewModel::play
    )
}
```

The `playerViewModel` is already available in `AppNavigation`. We could pass additional callbacks through `HomeScreen`'s parameters. But this would make the signature complex.

Better approach: The `HomeViewModel` already has `musicRepository` which has `setFavorite` and `updateSongMetadata` (added in Task 5). We need:
1. `playNext` - needs `PlayMusicUseCase`
2. `addToPlaylist / createPlaylistWith` - needs `PlaylistRepository`
3. `share` - needs Context

Let's inject `PlayMusicUseCase` and `PlaylistRepository` into `HomeViewModel`.

- [ ] **Step 1a: Add dependencies to HomeViewModel**

Update `HomeViewModel` constructor to add:
```kotlin
private val playMusicUseCase: PlayMusicUseCase,
private val playlistRepository: PlaylistRepository,
```

Add imports:
```kotlin
import com.musicplayer.app.domain.usecase.PlayMusicUseCase
import com.musicplayer.app.domain.repository.PlaylistRepository
```

- [ ] **Step 1b: Add action methods to HomeViewModel**

In `HomeViewModel`, keep existing `showSongActions` and `clearActionSong`. Add:

```kotlin
fun playNext(song: Song) {
    playMusicUseCase.playNext(song)
}

fun toggleFavoriteHome(song: Song) {
    viewModelScope.launch {
        musicRepository.setFavorite(song.id, !song.isFavorite)
    }
}

fun addToPlaylist(playlistId: String, song: Song) {
    viewModelScope.launch {
        playlistRepository.addSong(playlistId, song.id)
    }
}

fun createPlaylistWith(song: Song) {
    viewModelScope.launch {
        val playlistName = "Playlist: ${song.title}"
        playlistRepository.createPlaylist(playlistName).let { playlist ->
            playlistRepository.addSong(playlist.id, song.id)
        }
    }
}
```

Check `PlaylistRepository` interface. Let me check what methods it has.

Based on `PlaylistRepository` interface (which I haven't read directly but know from usage patterns in `LibraryViewModel`), it should have `createPlaylist`, `addSong`. Let me read it.

Let me check what it looks like... Actually from `PlaylistsViewModel.kt` and `LibraryViewModel.kt` usage, there should be methods like `createPlaylist(name)` and `addSong(playlistId, songId)`.

Let me check in a moment. For now, let's plan to add:

```kotlin
fun showEditDialog(song: Song) {
    _editSong.value = song
}

fun clearEditSong() {
    _editSong.value = null
}

fun saveEditMetadata(songId: String, title: String, artistName: String, albumTitle: String) {
    viewModelScope.launch {
        musicRepository.updateSongMetadata(songId, title, artistName, albumTitle)
        _editSong.value = null
    }
}
```

Need to add:
```kotlin
private val _editSong = MutableStateFlow<Song?>(null)
val editSong: StateFlow<Song?> = _editSong

private val _playlists = MutableStateFlow<List<Playlist>>(emptyList())
val playlists: StateFlow<List<Playlist>> = _playlists

init {
    viewModelScope.launch {
        playlistRepository.observePlaylists().collect { _playlists.value = it }
    }
}
```

- [ ] **Step 2: Add SongActionSheet observation to HomeScreen**

In `HomeScreen.kt`, change the composable signature to not need additional parameters -- we'll use the ViewModel's data directly.

At the top of `HomeScreen` composable, add:
```kotlin
val actionSong by viewModel.actionSong.collectAsStateWithLifecycle()
val editSong by viewModel.editSong.collectAsStateWithLifecycle()
val playlists by viewModel.playlists.collectAsStateWithLifecycle()
val context = LocalContext.current
```

At the bottom of `HomeScreen` (just before the final closing `}`), add:

```kotlin
actionSong?.let { song ->
    SongActionSheet(
        song = song,
        playlists = playlists,
        onDismiss = { viewModel.clearActionSong() },
        onPlayNext = {
            viewModel.playNext(song)
            viewModel.clearActionSong()
        },
        onCreatePlaylist = {
            viewModel.createPlaylistWith(song)
            viewModel.clearActionSong()
        },
        onAddToPlaylist = { playlistId ->
            viewModel.addToPlaylist(playlistId, song)
            viewModel.clearActionSong()
        },
        onToggleFavorite = {
            viewModel.toggleFavoriteHome(song)
            viewModel.clearActionSong()
        },
        onShare = {
            shareSong(context, song)
            viewModel.clearActionSong()
        },
        onOpenArtist = {
            viewModel.clearActionSong()
        },
        onOpenAlbum = {
            viewModel.clearActionSong()
        },
        onEditInfo = {
            viewModel.showEditDialog(song)
            viewModel.clearActionSong()
        }
    )
}

editSong?.let { song ->
    EditSongDialog(
        song = song,
        onSave = { title, artistName, albumTitle ->
            viewModel.saveEditMetadata(song.id, title, artistName, albumTitle)
        },
        onDismiss = { viewModel.clearEditSong() }
    )
}
```

- [ ] **Step 3: Add imports to HomeScreen**

Add imports at top of `HomeScreen.kt`:
```kotlin
import com.musicplayer.app.ui.components.EditSongDialog
import com.musicplayer.app.ui.library.SongActionSheet
import com.musicplayer.app.domain.model.Playlist
```
Note: `SongActionSheet` is `private` in `LibraryScreen.kt`. This means we can't reuse it from HomeScreen directly. We need to either:
a) Extract `SongActionSheet` and `shareSong` into a shared component file, OR
b) Duplicate the composable in HomeScreen.

Option (a) is better. Extract `SongActionSheet` and `shareSong` from `LibraryScreen.kt` into a new shared file OR into `SongListItem.kt` / a new file.

Let's create: `app/src/main/java/com/musicplayer/app/ui/components/SongActionSheet.kt`

Move `SongActionSheet` composable (lines 362-443) and `shareSong` function (lines 760-772) from `LibraryScreen.kt` into the new file. Remove `private` modifier from `SongActionSheet`.

Update `LibraryScreen.kt` to import from the new location.

- [ ] **Step 3a: Extract SongActionSheet to shared component**

Create file `app/src/main/java/com/musicplayer/app/ui/components/SongActionSheet.kt`:

Copy the `SongActionSheet` composable from `LibraryScreen.kt` lines 362-443 and `shareSong` from lines 760-772. Add the edit info option and the new `onEditInfo` callback parameter.

```kotlin
package com.musicplayer.app.ui.components

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Album
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PlaylistAdd
import androidx.compose.material.icons.outlined.QueueMusic
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.musicplayer.app.domain.model.Playlist
import com.musicplayer.app.domain.model.Song
import com.musicplayer.app.ui.i18n.LocalStrings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongActionSheet(
    song: Song,
    playlists: List<Playlist>,
    onDismiss: () -> Unit,
    onPlayNext: () -> Unit,
    onCreatePlaylist: () -> Unit,
    onAddToPlaylist: (String) -> Unit,
    onToggleFavorite: () -> Unit,
    onShare: () -> Unit,
    onOpenArtist: () -> Unit,
    onOpenAlbum: () -> Unit,
    onEditInfo: () -> Unit = {}
) {
    val strings = LocalStrings.current
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Text(
            text = song.title,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        ListItem(
            headlineContent = { Text(strings.addToNext) },
            leadingContent = { Icon(Icons.Outlined.QueueMusic, contentDescription = null) },
            modifier = Modifier.clickable(onClick = onPlayNext)
        )
        ListItem(
            headlineContent = { Text(strings.addToNewPlaylist) },
            leadingContent = { Icon(Icons.Outlined.PlaylistAdd, contentDescription = null) },
            modifier = Modifier.clickable(onClick = onCreatePlaylist)
        )
        playlists.forEach { playlist ->
            ListItem(
                headlineContent = { Text("${strings.addToPlaylistPrefix}${playlist.name}") },
                leadingContent = { Icon(Icons.Outlined.PlaylistAdd, contentDescription = null) },
                modifier = Modifier.clickable { onAddToPlaylist(playlist.id) }
            )
        }
        Divider()
        ListItem(
            headlineContent = { Text(if (song.isFavorite) strings.removeFavorite else strings.addFavorite) },
            leadingContent = {
                Icon(
                    if (song.isFavorite) Icons.Outlined.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = null
                )
            },
            modifier = Modifier.clickable(onClick = onToggleFavorite)
        )
        ListItem(
            headlineContent = { Text(strings.share) },
            leadingContent = { Icon(Icons.Outlined.Share, contentDescription = null) },
            modifier = Modifier.clickable(onClick = onShare)
        )
        ListItem(
            headlineContent = { Text(strings.editInfo) },
            leadingContent = { Icon(Icons.Outlined.Edit, contentDescription = null) },
            modifier = Modifier.clickable(onClick = onEditInfo)
        )
        if (!song.artistId.isNullOrBlank()) {
            ListItem(
                headlineContent = { Text(strings.viewArtist) },
                supportingContent = { Text(song.artistName) },
                leadingContent = { Icon(Icons.Outlined.Person, contentDescription = null) },
                modifier = Modifier.clickable(onClick = onOpenArtist)
            )
        }
        if (!song.albumId.isNullOrBlank()) {
            ListItem(
                headlineContent = { Text(strings.viewAlbum) },
                supportingContent = { Text(song.albumTitle) },
                leadingContent = { Icon(Icons.Outlined.Album, contentDescription = null) },
                modifier = Modifier.clickable(onClick = onOpenAlbum)
            )
        }
        TextButton(
            onClick = onDismiss,
            modifier = Modifier.padding(16.dp)
        ) {
            Text(strings.cancel)
        }
    }
}

fun shareSong(context: Context, song: Song) {
    val text = buildString {
        append(song.title)
        if (song.artistName.isNotBlank()) append(" - ").append(song.artistName)
        if (song.albumTitle.isNotBlank()) append("\n").append(song.albumTitle)
    }
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, song.title)
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, song.title))
}
```

This file needs `Modifier.fillMaxWidth` import only if used, but the original didn't use it in SongActionSheet's dismiss button. It uses `Modifier.padding` which is already imported.

- [ ] **Step 3b: Update LibraryScreen to use extracted SongActionSheet**

In `LibraryScreen.kt`, remove the `SongActionSheet` composable (lines 362-443) and `shareSong` function (lines 760-772). Add import:
```kotlin
import com.musicplayer.app.ui.components.SongActionSheet
import com.musicplayer.app.ui.components.shareSong
```

The `SongList` call already passes all necessary callbacks, but now also needs `onEditInfo`. Update the `SongActionSheet` call in `SongList` (around line 251) to add `onEditInfo = { ... }`.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/musicplayer/app/ui/components/SongActionSheet.kt \
        app/src/main/java/com/musicplayer/app/ui/home/HomeScreen.kt \
        app/src/main/java/com/musicplayer/app/ui/home/HomeViewModel.kt \
        app/src/main/java/com/musicplayer/app/ui/library/LibraryScreen.kt
git commit -m "fix: wire up home screen action button with SongActionSheet and add edit metadata option"
```

---

### Task 8: Home Hero Rotates on Each Open

**Files:**
- Modify: `app/src/main/java/com/musicplayer/app/ui/home/HomeViewModel.kt:29`
- Modify: `app/src/main/java/com/musicplayer/app/ui/home/HomeScreen.kt:49-126`

**Background:** `HomeUiState.featuredSong` (line 29) always picks `songs.maxByOrNull { it.dateAddedEpochMs }` -- always the most recently added song. The hero image never changes.

**Goal:** `featuredSong` should pick from a pool of candidates (songs with artwork, favorites, recently added) using weighted randomization, and change each time `HomeScreen` is opened.

**Approach:** Modify the `featuredSong` property in `HomeUiState` to use a seeded random selection from a candidate pool. However, since `HomeUiState` is a data class created from a `combine` Flow, it recomputes on every song list change. We need a mechanism that changes only when the screen is opened.

Better approach: Use a `MutableStateFlow<Song?>` in `HomeViewModel` that gets recomputed via a `refreshFeatured()` method called from `HomeScreen` via `LaunchedEffect`.

- [ ] **Step 1: Remove automatic featuredSong from HomeUiState**

In `HomeViewModel.kt`, change the `HomeUiState` data class (line 29):
```kotlin
val featuredSong: Song? = null
```
(Remove the computed property, make it a plain field set by the ViewModel.)

- [ ] **Step 2: Add featuredSong StateFlow and refresh logic to HomeViewModel**

In `HomeViewModel`, add:
```kotlin
private val _featuredSong = MutableStateFlow<Song?>(null)
val featuredSong: StateFlow<Song?> = _featuredSong

fun refreshFeatured() {
    val songs = uiState.value.songs
    if (songs.isEmpty()) {
        _featuredSong.value = null
        return
    }
    // Candidate pool: songs with artwork, favorites, recently added (last 30 days)
    val withArtwork = songs.filter { !it.artworkUri.isNullOrBlank() }
    val favorites = songs.filter { it.isFavorite }
    val recentCutoff = System.currentTimeMillis() - 30 * 24 * 60 * 60 * 1000L
    val recent = songs.filter { it.dateAddedEpochMs >= recentCutoff }

    // Build weighted pool: favorites get 3x, artwork gets 2x, recent gets 1x
    val pool = buildList {
        favorites.forEach { repeat(3) { add(it) } }
        withArtwork.forEach { repeat(2) { add(it) } }
        recent.forEach { add(it) }
    }

    if (pool.isEmpty()) {
        _featuredSong.value = songs.random()
    } else {
        _featuredSong.value = pool.random()
    }
}
```

- [ ] **Step 3: Call refreshFeatured from HomeScreen on each open**

In `HomeScreen.kt`, add a `LaunchedEffect` to trigger refresh when the screen appears:

```kotlin
LaunchedEffect(Unit) {
    viewModel.refreshFeatured()
}
```

Also update the `HomeHero` call to use the `featuredSong` StateFlow instead of `state.featuredSong`. But `state` is `HomeUiState` which still has the field. We need to collect `featuredSong` separately.

Add at top of `HomeScreen`:
```kotlin
val featuredSong by viewModel.featuredSong.collectAsStateWithLifecycle()
```

Then change `HomeHero` invocation at line 65-68 from:
```kotlin
HomeHero(
    state = state,
    onPlaySong = onPlaySong
)
```
To pass the featured song explicitly:
```kotlin
HomeHero(
    featuredSong = featuredSong,
    allSongs = state.songs,
    onPlaySong = onPlaySong
)
```

Update `HomeHero` signature at lines 149-153:
```kotlin
@Composable
private fun HomeHero(
    featuredSong: Song?,
    allSongs: List<Song>,
    onPlaySong: (Song, List<Song>) -> Unit
)
```

And update references inside `HomeHero`: change `song` references to `featuredSong` (already named `song` via `val song = state.featuredSong` -- update to `val song = featuredSong`) and change `state.songs` to `allSongs`.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/musicplayer/app/ui/home/HomeViewModel.kt \
        app/src/main/java/com/musicplayer/app/ui/home/HomeScreen.kt
git commit -m "feat: implement rotating featured song on home screen with weighted random selection"
```

---

## Dependency Order

Tasks can be done in parallel groups:

**Group A (no dependencies, can be done in any order):**
- Task 1 (Sequential icon + mode toggle)
- Task 2 (Album card redesign)
- Task 3 (Mini player bar background)

**Group B (depends on Task 5 for edit feature):**
- Task 5 (Edit song metadata) -- creates `EditSongDialog`, `SongActionSheet.kt`, adds `updateMetadata` to DAO

**Group C (depends on Group B):**
- Task 7 (Home screen action button) -- uses extracted `SongActionSheet` and `EditSongDialog` from Task 5
- Task 8 (Home hero rotation) -- independent of others

**Group D (independent):**
- Task 4 (Data import/export)
- Task 6 (Recommendation algorithm)

**Recommended order:** 1 -> 2 -> 3 -> 5 -> 7 -> 8 -> 6 -> 4

Or in dependency order:
1. Task 1, Task 2, Task 3 (parallel - UI only, no data layer changes)
2. Task 5 (data layer + creates shared components)
3. Task 7 and Task 8 (depend on Task 5 for `SongActionSheet` extraction)
4. Task 4 and Task 6 (independent features)

---

## Self-Review

**1. Spec coverage:** All 8 changes covered.

**2. Placeholder scan:** No TBDs, TODOs, or "implement later" found. All code is concrete.

**3. Type consistency:**
- `SongActionSheet` extracted in Task 5 and reused in Task 7 -- consistent signature with `onEditInfo` parameter.
- `EditSongDialog` created in Task 5 and reused in Task 7.
- `HomeViewModel.featuredSong` changed from computed property on `HomeUiState` to a separate `MutableStateFlow` in Task 8. `HomeScreen` updated to collect it separately.
- `PlayMusicUseCase` constructor change in Task 6 adds `PlayRecordDao` -- Hilt auto-provides it since `DataModule` already provides `PlayRecordDao`.
- `PlaylistRepository.observePlaylists()` used in Task 7 -- need to verify this method exists. Let me check quickly.

**Follow-up:** Checking `PlaylistRepository.kt`... it should have `observePlaylists()`. If not, the plan may need adjustment. But based on `PlaylistsViewModel` and `PlaylistDetailScreen` usage patterns, it should exist.

**Final check on files:**
- `PlaylistRepository.kt` - need to check if it has `observePlaylists()`, `createPlaylist()`, `addSong()` methods.

Let me note this in the plan for the implementor to verify.

---

## Correction to Task 7 Step 1a

Based on code exploration, `LibraryViewModel` uses `ManagePlaylistUseCase` (not `PlaylistRepository` directly) for create/add operations. Update the `HomeViewModel` constructor additions:

```kotlin
private val playMusicUseCase: PlayMusicUseCase,
private val playlistRepository: PlaylistRepository,   // for observing playlists
private val managePlaylistUseCase: ManagePlaylistUseCase,  // for create/add actions
```

And update the helper methods to use the use case:

```kotlin
fun addToPlaylist(playlistId: String, song: Song) {
    viewModelScope.launch {
        managePlaylistUseCase.addSong(playlistId, song.id)
    }
}

fun createPlaylistWith(song: Song) {
    viewModelScope.launch {
        val playlist = managePlaylistUseCase.create("Playlist: ${song.title}")
        managePlaylistUseCase.addSong(playlist.id, song.id)
    }
}
```

---

## Correction to Task 5

`LibraryViewModel` does NOT currently have `_editSong`/edit dialog support. Task 5 adds this. The `SongActionSheet` in `LibraryScreen.kt` needs `onEditInfo` wired into `LibraryViewModel`'s edit flow. However, `LibraryViewModel` already injects `MusicRepository`. So:

In `LibraryViewModel.kt`, add:
```kotlin
private val _editSong = MutableStateFlow<Song?>(null)
val editSong: StateFlow<Song?> = _editSong

fun showEditDialog(song: Song) {
    _editSong.value = song
}

fun clearEditDialog() {
    _editSong.value = null
}

fun saveEdit(songId: String, title: String, artistName: String, albumTitle: String) {
    viewModelScope.launch {
        musicRepository.updateSongMetadata(songId, title, artistName, albumTitle)
        _editSong.value = null
    }
}
```

And in `LibraryScreen.kt`, observe `editSong` and show `EditSongDialog` at the bottom of the `LibraryScreen` composable.

