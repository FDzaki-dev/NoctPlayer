# NoctPlayer

Offline-only Android video player. Minimalist, IINA-inspired UI, zero network/cloud/telemetry.

## Highlights
- 100% local playback via Media3 ExoPlayer — no INTERNET permission
- MediaStore-based library scan (internal + SD card)
- Gestures: swipe brightness/volume, horizontal seek, double-tap skip ±10s, pinch zoom
- Resume position, favorites, recently played
- AMOLED dark theme, auto-hide controls

## Status
Phase 1 (core architecture) — see `PROJECT_STATE.md` for the full roadmap and what's
implemented vs. planned. Static-analysis-only guarantee: this has not been compiled
in a real Android SDK/emulator environment; verify in Android Studio before release.

## Build
```
./gradlew assembleDebug
```

## Tech stack
Kotlin, Jetpack Compose (Material 3), Clean Architecture + MVVM, Room, MediaStore,
Coroutines/Flow, Media3 ExoPlayer, Coil (thumbnails).
