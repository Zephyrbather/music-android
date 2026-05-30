# Music Player App — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a native Android music player (Kotlin + Jetpack Compose) with local + multi-WebDAV playback, magazine-editorial UI, and manual metadata scraping.

**Architecture:** Single-module Clean Architecture with 3 layers — presentation (Compose screens + ViewModels), domain (models, repository interfaces, use cases), data (Room, WebDAV client, file scanner, metadata scrapers). Single `MusicPlaybackService` foreground service owns ExoPlayer. Hilt DI throughout.

**Tech Stack:** Kotlin, Jetpack Compose (BOM 2024.12.01), Material 3, Hilt, Room, Media3 (ExoPlayer + FFmpeg extension), OkHttp, Coil, JAudiotagger, DataStore

---

## File Structure

```
app/
├── build.gradle.kts
├── src/main/
│   ├── AndroidManifest.xml
│   ├── java/com/musicplayer/app/
│   │   ├── MusicPlayerApp.kt
│   │   ├── MainActivity.kt
│   │   ├── domain/
│   │   │   ├── model/
│   │   │   │   ├── Song.kt, Album.kt, Artist.kt, Playlist.kt
│   │   │   │   ├── WebDavServer.kt, ScrapeCache.kt, Source.kt
│   │   │   │   └── PlaybackState.kt
│   │   │   ├── repository/
│   │   │   │   ├── MusicRepository.kt
│   │   │   │   ├── PlaylistRepository.kt
│   │   │   │   ├── WebDavRepository.kt
│   │   │   │   └── SettingsRepository.kt
│   │   │   └── usecase/
│   │   │       ├── PlayMusicUseCase.kt
│   │   │       ├── ScanLibraryUseCase.kt
│   │   │       ├── FetchMetadataUseCase.kt
│   │   │       ├── ManagePlaylistUseCase.kt
│   │   │       └── SyncWebDavUseCase.kt
│   │   ├── data/
│   │   │   ├── local/
│   │   │   │   ├── AppDatabase.kt
│   │   │   │   ├── dao/ (SongDao, AlbumDao, ArtistDao, PlaylistDao, WebDavServerDao, ScrapeCacheDao)
│   │   │   │   ├── entity/ (SongEntity, AlbumEntity, ArtistEntity, PlaylistEntity, PlaylistSongEntity, WebDavServerEntity, ScrapeCacheEntity)
│   │   │   │   └── converter/ (Converters.kt)
│   │   │   ├── repository/
│   │   │   │   ├── MusicRepositoryImpl.kt
│   │   │   │   ├── PlaylistRepositoryImpl.kt
│   │   │   │   ├── WebDavRepositoryImpl.kt
│   │   │   │   └── SettingsRepositoryImpl.kt
│   │   │   ├── webdav/ (WebDavClient.kt)
│   │   │   ├── scanner/ (LocalFileScanner.kt)
│   │   │   ├── scraper/ (MetadataScraper.kt, MusicBrainzScraper.kt, CoverArtArchiveScraper.kt, LrcLibScraper.kt, NetEaseScraper.kt)
│   │   │   └── cache/ (CacheManager.kt)
│   │   ├── playback/
│   │   │   ├── MusicPlaybackService.kt
│   │   │   ├── DsdDecoder.kt
│   │   │   └── EqualizerController.kt
│   │   ├── ui/
│   │   │   ├── theme/ (Theme.kt, Color.kt, Typography.kt, ThemeManager.kt)
│   │   │   ├── navigation/ (AppNavigation.kt)
│   │   │   ├── home/ (HomeScreen.kt, HomeViewModel.kt)
│   │   │   ├── library/ (LibraryScreen.kt, LibraryViewModel.kt)
│   │   │   ├── playlists/ (PlaylistsScreen.kt, PlaylistDetailScreen.kt, PlaylistsViewModel.kt)
│   │   │   ├── settings/ (SettingsScreen.kt, SettingsViewModel.kt)
│   │   │   ├── player/ (NowPlayingScreen.kt, MiniPlayerBar.kt, PlayerViewModel.kt)
│   │   │   └── components/ (SongListItem.kt, AlbumCard.kt, PlaylistCard.kt, SourceBadge.kt, FastScrollIndex.kt)
│   │   └── di/ (AppModule.kt, DataModule.kt, UseCaseModule.kt)
│   └── res/
│       ├── values/strings.xml
│       └── font/ (noto-sans-sc, noto-serif-sc, etc.)
├── build.gradle.kts (project-level)
├── settings.gradle.kts
├── gradle.properties
└── gradle/libs.versions.toml
```

---
