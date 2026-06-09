# CPU Performance Optimization Design

## Overview

针对天玑9500芯片（1×X925超大核 + 3×X4大核 + 4×A720中核）的线程优先级优化方案，确保音频播放流畅性。

## Goals

- 音频解码和均衡器处理绑定到超大核/大核
- 后台任务（扫描、抓取）自动调度到中核
- 最小化代码改动，使用系统原生API

## Architecture

### New Component: CpuPerformanceManager

```kotlin
@Singleton
class CpuPerformanceManager @Inject constructor() {
    fun boostPlaybackThread()  // THREAD_PRIORITY_URGENT_AUDIO (-19)
    fun setBackgroundPriority() // THREAD_PRIORITY_BACKGROUND (10)
    fun setDefaultPriority()    // THREAD_PRIORITY_DEFAULT (0)
}
```

### Priority Mapping

| Task Type | Priority | Expected Core |
|-----------|----------|---------------|
| Audio Decode | URGENT_AUDIO (-19) | X925/X4 |
| Equalizer | URGENT_AUDIO (-19) | X925/X4 |
| Playback State | DEFAULT (0) | X4 |
| UI Render | DEFAULT (0) | X4 |
| File Scan | BACKGROUND (10) | A720 |
| Metadata Scrape | BACKGROUND (10) | A720 |

## Implementation Details

### 1. MusicPlaybackService

- ExoPlayer解码线程提升优先级
- 均衡器处理线程提升优先级
- 通知更新保持默认优先级

### 2. PlayMusicUseCase

- 位置更新定时器线程提升优先级
- 播放状态管理线程提升优先级

### 3. LibraryWorkCoordinator

- 扫描/抓取协程设置为后台优先级
- 使用 `withContext` 切换到低优先级Dispatcher

### 4. LocalFileScanner

- 文件IO操作设置为后台优先级

## Testing

- 验证播放时无卡顿
- 验证后台任务不影响播放流畅性
- 监控CPU核心使用情况
