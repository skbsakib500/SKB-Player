# Decisions — SKB-Player

## D-001 · Kotlin + Jetpack Compose

**Context:** Modern Android video player, one developer.

**Decision:** Kotlin 1.9.24 + Jetpack Compose (BOM 2024.06).

**Why:**
- Compose = declarative UI, fast iteration
- Kotlin null-safety, coroutines
- Material3 built-in
- No XML layouts, no findViewById

**Rejected:**
- Java + XML — verbose, slow
- Flutter — separate toolchain
- WebView — wrong for media surface

**Status:** Accepted · 2026-09-15

---

## D-002 · Media3 ExoPlayer (not VLC, not MediaPlayer)

**Context:** Need advanced playback (subtitle profiles, tracks, PiP).

**Decision:** AndroidX Media3 ExoPlayer 1.4.1.

**Why:**
- Official AndroidX, actively maintained
- Full track/subtitle control
- MediaSession integration
- PiP + foreground service ready

**Rejected:**
- VLC/libVLC — huge binary, license complexity
- MediaPlayer — no modern APIs
- IJKPlayer — abandoned

**Status:** Accepted · 2026-09-15

---

## D-003 · Privacy-first, offline-only

**Context:** Video players often leak metadata.

**Decision:** No internet. No tracking. No ads.

**Why:**
- User trust
- Local files only
- No permissions beyond storage/media

**Rejected:**
- Cloud library
- Online sources
- Analytics SDK

**Status:** Accepted · 2026-09-15

---

## D-004 · Foreground service for playback

**Context:** Playback must survive app background.

**Decision:** PlaybackService + MediaSession.

**Why:**
- Android standard
- Notification controls
- System lock-screen integration
- Required for PiP

**Rejected:**
- In-activity playback only
- WorkManager (wrong tool)

**Status:** Accepted · 2026-09-15

---

## D-005 · Managers as library layer

**Context:** Data must be accessible across screens.

**Decision:** 9 `library/` managers, each owns a concern.

**Why:**
- Single responsibility
- Testable individually
- Screens don't touch storage directly

**Rejected:**
- God repository
- ViewModel does everything

**Status:** Accepted · 2026-09-15

---

## D-006 · Reflection for Media3 bugs

**Context:** `setUseTextureView` broken in Media3 1.4.1.

**Decision:** Use reflection to call it (commit e9d1519).

**Why:**
- Unblocks correct rendering
- Reversible once upstream fixed

**Rejected:**
- Pin old Media3 — missing features
- Fork Media3 — maintenance burden

**Status:** Temporary · 2026-09-15

---

## D-007 · SKB Project Memory (this release)

**Context:** 20+ commits, version file stale, no docs.

**Decision:** Same memory pattern as other SKB projects.

**Status:** Accepted · 2026-09-16
