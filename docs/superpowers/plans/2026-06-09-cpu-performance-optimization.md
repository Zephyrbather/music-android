# CPU Performance Optimization Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 针对天玑9500芯片优化线程优先级，确保音频播放流畅性

**Architecture:** 创建CpuPerformanceManager工具类，使用android.os.Process.setThreadPriority()管理播放相关线程优先级

**Tech Stack:** Kotlin, Android Process API, Coroutines, Hilt

---

## File Structure

### New Files
- `app/src/main/java/com/musicplayer/app/performance/CpuPerformanceManager.kt` - CPU性能管理器

### Modified Files
- `app/src/main/java/com/musicplayer/app/playback/MusicPlaybackService.kt:59` - 提升播放线程优先级
- `app/src/main/java/com/musicplayer/app/domain/usecase/PlayMusicUseCase.kt:27` - 提升播放状态线程优先级
- `app/src/main/java/com/musicplayer/app/domain/usecase/LibraryWorkCoordinator.kt:53` - 降低后台任务优先级
- `app/src/main/java/com/musicplayer/app/data/scanner/LocalFileScanner.kt:28` - 降低文件扫描优先级

---

### Task 1: Create CpuPerformanceManager

**Files:**
- Create: `app/src/main/java/com/musicplayer/app/performance/CpuPerformanceManager.kt`

- [ ] **Step 1: Create CpuPerformanceManager class**

```kotlin
package com.musicplayer.app.performance

import android.os.Process
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CpuPerformanceManager @Inject constructor() {
    
    fun boostPlaybackThread() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO)
    }
    
    fun setBackgroundPriority() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)
    }
    
    fun setDefaultPriority() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_DEFAULT)
    }
}
```

- [ ] **Step 2: Verify file compiles**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/musicplayer/app/performance/CpuPerformanceManager.kt
git commit -m "feat: add CpuPerformanceManager for thread priority control"
```

---

### Task 2: Integrate into MusicPlaybackService

**Files:**
- Modify: `app/src/main/java/com/musicplayer/app/playback/MusicPlaybackService.kt`

- [ ] **Step 1: Add CpuPerformanceManager injection**

Add import:
```kotlin
import com.musicplayer.app.performance.CpuPerformanceManager
```

Add field after line 57:
```kotlin
@Inject lateinit var cpuPerformanceManager: CpuPerformanceManager
```

- [ ] **Step 2: Boost playback thread after ExoPlayer creation**

After line 80 (after player creation), add:
```kotlin
cpuPerformanceManager.boostPlaybackThread()
```

- [ ] **Step 3: Verify compilation**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/musicplayer/app/playback/MusicPlaybackService.kt
git commit -m "feat: boost playback thread priority in MusicPlaybackService"
```

---

### Task 3: Integrate into PlayMusicUseCase

**Files:**
- Modify: `app/src/main/java/com/musicplayer/app/domain/usecase/PlayMusicUseCase.kt`

- [ ] **Step 1: Add CpuPerformanceManager injection**

Add import:
```kotlin
import com.musicplayer.app.performance.CpuPerformanceManager
```

Modify constructor (line 22-25):
```kotlin
@Singleton
class PlayMusicUseCase @Inject constructor(
    private val playRecordDao: PlayRecordDao,
    private val settingsRepository: SettingsRepository,
    private val cpuPerformanceManager: CpuPerformanceManager
) {
```

- [ ] **Step 2: Boost thread priority in init block**

In the init block, add thread priority boost at the start (after line 29):
```kotlin
init {
    cpuPerformanceManager.boostPlaybackThread()
    scope.launch {
        // ... existing code
    }
}
```

- [ ] **Step 3: Verify compilation**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/musicplayer/app/domain/usecase/PlayMusicUseCase.kt
git commit -m "feat: boost playback state thread priority in PlayMusicUseCase"
```

---

### Task 4: Integrate into LibraryWorkCoordinator

**Files:**
- Modify: `app/src/main/java/com/musicplayer/app/domain/usecase/LibraryWorkCoordinator.kt`

- [ ] **Step 1: Add CpuPerformanceManager injection**

Add import:
```kotlin
import com.musicplayer.app.performance.CpuPerformanceManager
```

Modify constructor (line 45-52):
```kotlin
@Singleton
class LibraryWorkCoordinator @Inject constructor(
    private val musicRepository: MusicRepository,
    private val scanLibraryUseCase: ScanLibraryUseCase,
    private val fetchMetadataUseCase: FetchMetadataUseCase,
    private val aiScrapeMetadataUseCase: AiScrapeMetadataUseCase,
    private val aiGenerateSongCategoryUseCase: AiGenerateSongCategoryUseCase,
    private val settingsRepository: SettingsRepository,
    private val cpuPerformanceManager: CpuPerformanceManager
) {
```

- [ ] **Step 2: Set background priority in startScan**

In startScan function, after line 64 (inside scope.launch), add:
```kotlin
cpuPerformanceManager.setBackgroundPriority()
```

- [ ] **Step 3: Set background priority in runScrape**

In runScrape function, after line 109 (at the start of the function), add:
```kotlin
cpuPerformanceManager.setBackgroundPriority()
```

- [ ] **Step 4: Verify compilation**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/musicplayer/app/domain/usecase/LibraryWorkCoordinator.kt
git commit -m "feat: set background priority for library work tasks"
```

---

### Task 5: Integrate into LocalFileScanner

**Files:**
- Modify: `app/src/main/java/com/musicplayer/app/data/scanner/LocalFileScanner.kt`

- [ ] **Step 1: Add CpuPerformanceManager injection**

Add import:
```kotlin
import com.musicplayer.app.performance.CpuPerformanceManager
```

Modify constructor (line 22-24):
```kotlin
class LocalFileScanner @Inject constructor(
    @ApplicationContext private val context: Context,
    private val cpuPerformanceManager: CpuPerformanceManager
) {
```

- [ ] **Step 2: Set background priority in scan function**

In scan function, after line 28 (inside withContext), add:
```kotlin
cpuPerformanceManager.setBackgroundPriority()
```

- [ ] **Step 3: Verify compilation**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/musicplayer/app/data/scanner/LocalFileScanner.kt
git commit -m "feat: set background priority for file scanning operations"
```

---

### Task 6: Final Verification

- [ ] **Step 1: Run full build**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 2: Run lint check**

Run: `./gradlew lintDebug`
Expected: No critical issues

- [ ] **Step 3: Final commit**

```bash
git add .
git commit -m "feat: complete CPU performance optimization for Dimensity 9500"
```
