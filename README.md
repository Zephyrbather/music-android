# Music Player

原生 Android 音乐播放器，采用现代 Android 开发技术栈构建，支持本地音乐播放、WebDAV 网络存储、在线元数据抓取和 AI 智能推荐等功能。

## 技术栈

| 类别 | 技术 |
|------|------|
| 语言 | Kotlin |
| UI 框架 | Jetpack Compose |
| 设计系统 | Material Design 3 |
| 依赖注入 | Hilt |
| 数据库 | Room |
| 媒体播放 | Media3 (ExoPlayer) |
| 音频解码 | FFmpeg |
| 网络请求 | OkHttp |
| 图片加载 | Coil |
| 异步处理 | Kotlin Coroutines |
| 数据持久化 | DataStore |
| 音频标签 | JAudioTagger |
| AI 推理 | DeepSeek |

## 架构设计

项目采用 Clean Architecture 分层架构：

```
app/src/main/java/com/musicplayer/app/
├── domain/          # 领域层 - 业务逻辑和用例
│   ├── model/       # 数据模型
│   ├── repository/  # 仓库接口
│   ├── usecase/     # 用例
│   └── ai/          # AI 相关接口
├── data/            # 数据层 - 数据源和仓库实现
│   ├── local/       # 本地数据库 (Room)
│   ├── scanner/     # 媒体库扫描器
│   ├── scraper/     # 元数据抓取器
│   ├── webdav/      # WebDAV 客户端
│   ├── ai/          # AI 实现
│   ├── cache/       # 缓存管理
│   └── tag/         # 音频标签处理
├── playback/        # 播放层 - 媒体播放服务
│   ├── MusicPlaybackService.kt  # 前台播放服务
│   ├── EqualizerController.kt   # 均衡器控制
│   ├── DesktopLyricsOverlayController.kt  # 桌面歌词
│   └── DsdDecoder.kt            # DSD 解码器
├── ui/              # 表现层 - Compose UI 组件
│   ├── home/        # 首页
│   ├── library/     # 音乐库
│   ├── playlists/   # 播放列表
│   ├── player/      # 播放器界面
│   ├── settings/    # 设置页面
│   ├── components/  # 共享组件
│   ├── navigation/  # 导航配置
│   ├── theme/       # 主题系统
│   └── i18n/        # 国际化
└── di/              # 依赖注入 - Hilt 模块
```

## 功能模块

### 数据层
- **Room 数据库**：歌曲、专辑、艺术家、播放列表、WebDAV 服务器、抓取缓存
- **本地 MediaStore 扫描器**：自动扫描设备音乐文件，支持演示库回退
- **WebDAV 客户端**：支持 WebDAV 服务器连接和音乐文件浏览播放
- **音频标签处理**：使用 JAudioTagger 读取和写入音频文件元数据

### 元数据抓取器
支持多个在线数据源获取音乐元数据：

| 数据源 | 功能 |
|--------|------|
| MusicBrainz | 音乐数据库，获取歌曲、专辑、艺术家信息 |
| Cover Art Archive | 封面艺术图片 |
| LRCLIB | 歌词同步 |
| NetEase | 网易云音乐，获取中文歌曲信息 |
| QQ Music | QQ 音乐 |
| KuGou | 酷狗音乐 |
| Kuwo | 酷我音乐 |
| Migu | 咪咕音乐 |
| JioSaavn | 印度音乐平台 |
| Deezer | 欧洲音乐平台 |
| Genius | 歌词和注释 |
| ITunes | Apple 音乐商店 |
| LyricsOvh | 歌词服务 |
| Megalobiz | 歌词服务 |

### AI 功能
- **DeepSeek 元数据顾问**：使用 AI 智能推荐和补全音乐元数据
- **AI 分类**：自动为歌曲生成分类标签

### 播放层
- **Media3 前台播放服务**：支持后台播放和通知控制
- **FFmpeg 解码器**：支持多种音频格式解码
- **DSD 解码器**：支持高解析度 DSD 音频格式
- **均衡器**：支持音频均衡器调节和预设
- **桌面歌词**：支持桌面悬浮歌词显示

### UI 层
- **首页**：显示推荐内容和快速访问
- **音乐库**：按歌曲、专辑、艺术家分类浏览
- **播放列表**：创建和管理播放列表
- **正在播放**：全屏播放控制界面
- **迷你播放器**：底部迷你播放控制栏
- **设置**：
  - 外观设置（主题、字体、动态颜色）
  - 本地库设置
  - 抓取设置
  - 缓存管理
  - 数据管理
  - WebDAV 设置

### 国际化
支持多语言界面，包括中文和英文等。

### 主题系统
- 支持浅色、深色和沉浸式主题
- 动态颜色支持（Android 12+）
- 自定义字体选项
- 基于专辑封面的颜色提取

## 环境要求

- Android Studio Hedgehog (2023.1.1) 或更高版本
- JDK 17
- Android SDK 34
- 最低支持 Android 8.0 (API 26)

## 构建

```bash
# 调试版本
./gradlew :app:assembleDebug

# 发布版本
./gradlew :app:assembleRelease

# 离线构建
./gradlew :app:assembleDebug --offline
```

## 项目配置

### 应用信息
- 包名：`com.musicplayer.app`
- 版本：`0.1.0`
- 最低 SDK：26
- 目标 SDK：34
- 编译 SDK：34

### Gradle 配置
- Kotlin JVM 目标：17
- Compose 编译器版本：通过 `libs.versions.toml` 管理
- Room Schema 导出：`app/schemas/`

## 依赖管理

项目使用 Gradle Version Catalog 管理依赖版本，配置文件位于 `gradle/libs.versions.toml`。

## 权限说明

应用需要以下权限：
- `INTERNET`：网络访问，用于在线抓取和 WebDAV
- `ACCESS_NETWORK_STATE`：检查网络状态
- `FOREGROUND_SERVICE`：后台播放服务
- `FOREGROUND_SERVICE_MEDIA_PLAYBACK`：媒体播放前台服务
- `POST_NOTIFICATIONS`：显示通知
- `SYSTEM_ALERT_WINDOW`：桌面歌词悬浮窗
- `READ_MEDIA_AUDIO`：读取音频文件（Android 13+）
- `READ_EXTERNAL_STORAGE`：读取外部存储（Android 12 及以下）

## 许可证

[待定]
