# SKB-Player

> **SKB Dev Ecosystem · Project Identity Card**
> _Build Once. Track Everything. Update Safely. Remember Forever._

---

## Identity

| Field | Value |
|-------|-------|
| Project ID | `skb-player` |
| Display Name | SKB Player |
| Package | `com.skb.player` |
| Type | Android App (Kotlin + Jetpack Compose) |
| Domain | Media (video player) |
| versionName | `0.1.0` ⚠️ (stale — commits say v2.4) |
| versionCode | `1` ⚠️ (stale) |
| Status | **Active** |
| Author | SKB Dev |
| GitHub | [skbsakib500/SKB-Player](https://github.com/skbsakib500/SKB-Player) |
| Branch | `main` |
| Kotlin files | 22 |

---

## What Is This

SKB-Player is a **privacy-first Android video player**.

- Local file playback via Media3/ExoPlayer
- No internet, no tracking, no ads
- AMOLED theme, gesture-driven
- Bookmarks, playlists, watch history
- Equalizer, AB repeat, subtitle profiles
- PiP + foreground service for background playback

---

## Position In Ecosystem

    SKB-Player (this project)  ← media layer, independent
    SKB-Music                  ← sibling (music focus)

**Not part of Mimi Life OS.** Media is separate concern.

Sibling ecosystem:
- `Mimi-Android`   — Life OS delivery (WebView + Python)
- `Agent-Mimi`     — Life OS brain (Python)
- `SKB-Music`      — music player
- `Mimi-Forever`   — backup vault

---

## Tech Stack

| Layer | Tech |
|-------|------|
| Language | Kotlin 1.9.24 |
| UI | Jetpack Compose (BOM 2024.06) |
| Media | Media3 ExoPlayer 1.4.1 |
| Build | AGP 8.5.2, Gradle 8.7, JDK 17 |
| Min SDK | 24 |
| Target SDK | 34 |
| CI | GitHub Actions (`build.yml`) |

---

## Feature Set (from commit history)

### Core (v0.7–v1.0)
- In-app navigation, bottom nav, AMOLED theme
- Foreground service + MediaSession
- Resume dialog, audio track selector, sleep timer, PiP
- Study mode: bookmarks, notes, screenshots

### Enhanced (v1.1–v1.4)
- Playlist queue, watch time, full history
- Search, custom icon, AMOLED splash
- Subtitle delay/position/profiles, sort modes
- Pinch zoom, auto-rotate, folders tab, rename/delete/info

### Pro (v2.0–v2.4)
- Favorites, watch later, playlists
- Equalizer, AB repeat, theme switch, thumbnails
- Multi-select, batch delete
- Intent filters (register as external video player)
- TextureView reflection for Media3 compat

---

## Memory Files

PROJECT.md · ARCHITECTURE.md · ROADMAP.md · CHANGELOG.md ·
DECISIONS.md · KNOWN-ISSUES.md · TODO.md · DEVELOPMENT.md · `.skb/`

_Last updated: 2026-09-15_
