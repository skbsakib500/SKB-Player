# Changelog — SKB-Player

Format: [Keep a Changelog](https://keepachangelog.com/)
Versioning: [SemVer](https://semver.org/)

## [Unreleased]

### Added
- SKB project memory system (docs + .skb/)
- README, CHANGELOG (this file)

### Fixed
- (pending) versionName sync
- (pending) .gitignore .github/ inconsistency

## [2.4.0] — (per commit history)

### Added
- Splash animation
- History thumbnails
- Pinch-zoom (video surface)
- Rotate layout

### Fixed
- setUseTextureView via reflection (Media3 1.4.1 compat)

## [2.3.0]

### Added
- Long-press multi-select + batch delete in Videos tab
- Clean HomeScreen imports
- Wire selectedFolderId through MainActivity

## [2.2.0]

### Added
- Video item ⋮ menu on all lists
- Hoist folder state for proper back nav

## [2.1.0]

### Added
- Register as video player (intent filter)
- Handle external open

### Fixed
- audioSessionId via reflection
- LaunchedEffect order after openSingleVideo

## [2.0.0]

### Added
- Favorites, watch later, playlists
- Equalizer, AB repeat
- Theme switch, thumbnails
- Multi-select

### Fixed
- FileOps revert to Result API

## [1.4.0]

### Added
- Pinch zoom, auto-rotate
- Folders tab
- File rename/delete/info

## [1.3.0]

### Added
- Subtitle delay/position/profiles
- Settings tab
- Sort modes

### Fixed
- Spacer/key imports
- PixelCopy fallback

## [1.2.0]

### Added
- Search screen
- Custom app icon
- AMOLED splash theme

## [1.1.0]

### Added
- Playlist queue
- Watch time tracking
- Full history screen

## [1.0.0]

### Added
- Study mode: bookmarks, notes, screenshots

## [0.9.0]

### Added
- Resume dialog
- Audio track selector
- Sleep timer
- Picture-in-Picture

## [0.8.0]

### Added
- Foreground service
- MediaSession for background playback

## [0.7.0]

### Added
- In-app navigation
- Bottom nav
- AMOLED theme
- Double-back to exit
