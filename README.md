# Music Player

原生 Android 音乐播放器，采用现代 Android 开发技术栈构建。

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

## 架构设计

项目采用 Clean Architecture 分层架构：

```
app/src/main/java/com/musicplayer/app/
├── domain/          # 领域层 - 业务逻辑和用例
├── data/            # 数据层 - 数据源和仓库实现
├── playback/        # 播放层 - 媒体播放服务
├── ui/              # 表现层 - Compose UI 组件
└── di/              # 依赖注入 - Hilt 模块
```

## 功能模块

### 数据层
- Room 数据库：歌曲、专辑、艺术家、播放列表、WebDAV 服务器、抓取缓存
- 本地 MediaStore 扫描器，支持演示库回退
- WebDAV PROPFIND 客户端脚手架和同步用例

### 元数据抓取器
- MusicBrainz 音乐数据库
- Cover Art Archive 封面艺术
- LRCLIB 歌词
- NetEase 网易云音乐

### 播放层
- Media3 前台播放服务
- FFmpeg 解码器支持
- 通知控制

### UI 层
- 首页
- 音乐库
- 播放列表
- 设置
- 正在播放
- 迷你播放器

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

## 许可证

[待定]
