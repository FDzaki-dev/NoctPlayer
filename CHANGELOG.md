# Changelog

## [0.1.0] - 2026-08-06 — Phase 1: Core Architecture
### Added
- Project scaffold: Gradle Kotlin DSL, Compose, Room, Media3 ExoPlayer, Coil
- MediaStore video scanner (`MediaStoreScanner`) — internal storage + SD card, no legacy permission on API 29+
- Room database: `media_items`, `watch_progress`, `favorites`, `playlists`/`playlist_items`
- `MediaRepository` unifying scan + DB + resume/favorite logic
- Library screen: adaptive grid, thumbnail via Coil, search, sort (date/name/duration/size)
- Player screen: ExoPlayer playback, resume position (saved every 5s + on exit),
  gesture controls (vertical swipe = brightness/volume by screen half, horizontal
  drag = seek, double-tap = ±10s skip, pinch = zoom), auto-hide controls, speed picker
  (0.25x–2x), favorite toggle, landscape-forced immersive fullscreen
- Fail-safe MediaStore-based crash logger with FIFO 50-file retention (`CrashLogger`)
- Adaptive app icon (vector, teal play-mark on black; API 26+ native adaptive icon,
  no legacy PNG fallback needed since minSdk = 26)
- GitHub Actions CI: release build, apksigner verification + SHA-256 in step summary,
  auto-publish signed APK to GitHub Releases

### Known gaps (see PROJECT_STATE.md roadmap)
- Subtitle rendering (SRT/ASS/SSA) not yet implemented
- LibVLC fallback engine not yet implemented (ExoPlayer only)
- Audio/subtitle track selector UI not yet implemented
- Video info panel (codec/bitrate/fps) not yet implemented
- File rename/delete/share from library not yet implemented
- Playlist UI not yet implemented (DB schema present)
