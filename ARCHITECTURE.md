# Architecture — SKB-Player

## Layer Diagram

    ┌────────────────────────────────────────────────────────┐
    │  UI LAYER (Jetpack Compose)                            │
    │  ───────────────────────────────                       │
    │  SplashScreen → MainActivity → Bottom Nav              │
    │  ├── HomeScreen          Videos grid                   │
    │  ├── FolderScreen        Folder tree                   │
    │  ├── HistoryScreen       Watch history                 │
    │  ├── SearchScreen        Search                        │
    │  ├── SettingsScreen      Preferences                   │
    │  ├── VideoPlayerScreen   ExoPlayer host                │
    │  ├── BookmarkDialogs     Add/edit bookmark             │
    │  ├── VideoDialogs        Rename/delete/info            │
    │  └── NewDialogs          Playlists, EQ, etc.           │
    └──────────────────────┬─────────────────────────────────┘
                           │
    ┌──────────────────────▼─────────────────────────────────┐
    │  LIBRARY LAYER (managers)                              │
    │  ────────────────────────                              │
    │  MediaScanner       scan local video files             │
    │  HistoryManager     watch history (SQLite?)            │
    │  BookmarkManager    per-video bookmarks                │
    │  PlaylistManager    user playlists                     │
    │  FavoritesManager   favorites + watch later            │
    │  SettingsManager    prefs (SharedPreferences)          │
    │  FileOps            rename / delete / move             │
    │  ThumbnailLoader    video frame extraction             │
    │  EqualizerManager   audio EQ presets                   │
    └──────────────────────┬─────────────────────────────────┘
                           │
    ┌──────────────────────▼─────────────────────────────────┐
    │  PLAYBACK LAYER                                        │
    │  ──────────────                                        │
    │  PlaybackService    foreground service + MediaSession  │
    │  Media3 ExoPlayer   actual playback engine             │
    └────────────────────────────────────────────────────────┘

## Directory Map

    SKB-Player/
    ├── .github/workflows/build.yml   CI: build debug APK
    ├── app/
    │   ├── build.gradle.kts          deps, SDK config
    │   └── src/main/
    │       ├── AndroidManifest.xml
    │       ├── java/com/skb/player/
    │       │   ├── MainActivity.kt           root + nav
    │       │   ├── PlaybackService.kt        MediaSession service
    │       │   ├── ui/                       12 Compose screens
    │       │   │   ├── SplashScreen.kt
    │       │   │   ├── HomeScreen.kt
    │       │   │   ├── FolderScreen.kt
    │       │   │   ├── HistoryScreen.kt
    │       │   │   ├── SearchScreen.kt
    │       │   │   ├── SettingsScreen.kt
    │       │   │   ├── VideoPlayerScreen.kt
    │       │   │   ├── BookmarkDialogs.kt
    │       │   │   ├── VideoDialogs.kt
    │       │   │   ├── NewDialogs.kt
    │       │   │   └── Theme.kt
    │       │   └── library/                  9 managers
    │       │       ├── MediaScanner.kt
    │       │       ├── HistoryManager.kt
    │       │       ├── BookmarkManager.kt
    │       │       ├── PlaylistManager.kt
    │       │       ├── FavoritesManager.kt
    │       │       ├── SettingsManager.kt
    │       │       ├── FileOps.kt
    │       │       ├── ThumbnailLoader.kt
    │       │       └── EqualizerManager.kt
    │       └── res/                          icons, themes
    ├── build.gradle.kts
    ├── settings.gradle.kts
    └── gradle.properties

## Data Flow — Play a Video

    User taps video in HomeScreen
        │
        ▼
    MainActivity opens VideoPlayerScreen
        │
        ▼
    VideoPlayerScreen requests PlaybackService
        │
        ▼
    PlaybackService starts ExoPlayer with MediaItem
        │
        ▼
    Progress saved to HistoryManager (on pause/stop)
        │
        ▼
    MediaSession exposes controls to system (PiP, notification)

## Data Flow — Scan Library

    App launch → MediaScanner.scan()
        │
        ▼
    Query MediaStore (video/*)
        │
        ▼
    For each: extract thumbnail via ThumbnailLoader
        │
        ▼
    Cache to HomeScreen state
        │
        ▼
    Optional: save to SharedPreferences / Room

## External Intent — Open With

    Another app shares video
        │
        ▼
    Android intent: ACTION_VIEW video/*
        │
        ▼
    SKB-Player activity filter matches
        │
        ▼
    MainActivity receives URI
        │
        ▼
    Direct open VideoPlayerScreen with that URI

## Storage

| Data | Where |
|------|-------|
| Settings | SharedPreferences |
| Watch history | (see HistoryManager — SQLite or prefs) |
| Bookmarks | (see BookmarkManager) |
| Playlists | (see PlaylistManager) |
| Favorites | (see FavoritesManager) |
| Thumbnails | filesystem cache |

## Known Architectural Debt

    1. Version drift — build.gradle.kts = 0.1.0, commits say v2.4
    2. 3 "dialogs" files: BookmarkDialogs, VideoDialogs, NewDialogs
       → candidate for consolidation
    3. `.gitignore` ignores `.github/` but workflow IS tracked
       (inconsistent — workflow only lives because added before ignore)
    4. No tests (see KNOWN-ISSUES)
    5. TextUtils-based reflection for setUseTextureView
       (workaround for Media3 1.4.1 bug — see commit e9d1519)
    6. No Room / proper persistence documented — managers ad hoc
