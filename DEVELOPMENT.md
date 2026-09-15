# Development Guide — SKB-Player

## Prerequisites

- JDK 17+
- Android SDK 34
- Gradle 8.7 (wrapper recommended)
- A device or emulator (API 24+)

## Build

    # Debug APK
    ./gradlew :app:assembleDebug
    # → app/build/outputs/apk/debug/app-debug.apk

    # Release (unsigned)
    ./gradlew :app:assembleRelease

    # Clean
    ./gradlew clean

## Install

    adb install -r app/build/outputs/apk/debug/app-debug.apk

## Run tests (none yet)

    # Future:
    ./gradlew test

## CI

GitHub Actions `.github/workflows/build.yml`:
- Runs on push to `main` and `workflow_dispatch`
- JDK 17 (temurin)
- Builds debug APK
- Artifact: see Actions tab

## Project Layout

    app/src/main/java/com/skb/player/
    ├── MainActivity.kt          root nav host
    ├── PlaybackService.kt       MediaSession service
    ├── ui/                      12 Compose screens
    └── library/                 9 managers

## Adding a Screen

1. Create `ui/YourScreen.kt` as `@Composable`
2. Register route in `MainActivity.kt`
3. Add to bottom nav if top-level
4. Use manager from `library/` for data

## Adding a Manager

1. Create `library/YourManager.kt`
2. Own one concern (storage, scan, etc.)
3. Expose suspend functions or StateFlow
4. Consume from screens

## Commit Convention

    <type>: <summary>

Types: feat, fix, docs, chore, style, refactor, release

Historical style: `v2.4: <description>`

## Release Checklist

- [ ] Bump `versionName` + `versionCode` in `app/build.gradle.kts`
- [ ] Update CHANGELOG.md
- [ ] Update ROADMAP.md
- [ ] Update `.skb/state.json`
- [ ] `git commit -m "release: vX.Y.Z"`
- [ ] `git tag vX.Y.Z && git push --tags`
- [ ] GitHub Actions builds APK
- [ ] Archive to `~/Mimi-Forever/backups/`

## Git Safety

Never (without confirmation):
- force push
- reset --hard on pushed branches
- delete remote branches
- rewrite published history

## Known Pain Points

1. No tests (KI-P05)
2. Reflection for Media3 setUseTextureView (KI-P06)
3. 3 dialogs files (KI-P04)
4. Version drift (KI-P01)

## Reference

- Repo: https://github.com/skbsakib500/SKB-Player
- Sibling: `~/SKB-Music`
- Ecosystem: `~/Agent-Mimi`, `~/Mimi-Android`
