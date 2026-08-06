# PROJECT_STATE.md — NoctPlayer

## Current State
Version: 0.1.0 (Phase 1 — Core Architecture)
Package: com.noctplayer.app
Min/Target/Compile SDK: 26 / 34 / 34

## Architecture Decisions
- **Media engine**: Media3 ExoPlayer chosen as primary per spec. LibVLC fallback
  deferred to Phase 2 — adding it now would require an AAR/NDK dependency decision
  (libVLC-android vs libvlc-all) that should be scoped as its own atomic change,
  not bundled into the initial scaffold.
- **Persistence split**: Room holds derived/queryable state (watch progress,
  favorites, playlists). Raw media metadata is re-synced from MediaStore on each
  library refresh rather than treated as source of truth, so a file moved/deleted
  outside the app is reconciled automatically (`deleteMissing` in MediaDao).
- **Crash logger**: Implemented as a custom `Thread.UncaughtExceptionHandler`
  writing via MediaStore Documents/NoctPlayer/logs (API 29+) with legacy
  fallback path for API 26-28, per project-wide standing instruction. Retention
  is FIFO at 50 files, enforced after every write.
- **Gestures**: Implemented as raw `pointerInput` detectors (tap/double-tap,
  vertical drag split by screen half, horizontal drag, transform/pinch) rather
  than a gesture library, to keep the player screen dependency-free and match
  the "no unnecessary deps" spirit of a privacy-focused offline app.
- **Navigation**: Two routes only (library, player) for Phase 1. Favorites/
  Playlists/Settings screens are stubbed as DB schema + repository methods but
  have no screen yet — see roadmap.

## AI Assumptions (flag if incorrect)
1. Assumed a single-Activity Compose app (no legacy XML fragments) is desired.
2. Assumed landscape-forced fullscreen player is acceptable even for portrait
   source videos (matches most modern player UX; can be revisited).
3. Assumed GitHub Release APK should be **unsigned-if-no-secrets, signed-if-
   secrets-present** — build does not hard-fail when keystore secrets are absent,
   so first CI run succeeds before secrets are configured. Confirm this is
   the desired behavior once you're ready to cut a real signed release.
4. App icon: implemented as a vector adaptive icon (`mipmap-anydpi-v26`,
   background + foreground drawables) rather than raster PNGs, since minSdk
   26 supports adaptive icons natively. Placeholder mark (teal play-triangle
   on black) — swap the foreground vector for a real brand mark whenever
   ready; no build-breaking gap remains.

## Roadmap (not yet implemented)
- [ ] Subtitle rendering: SRT/ASS/SSA via Media3 subtitle support, delay + style controls
- [ ] Audio track selector + subtitle track selector UI (TrackSelectionOverride)
- [ ] LibVLC fallback engine + codec-failure auto-switch from ExoPlayer
- [ ] Video info panel: codec, bitrate, fps, resolution, file size (MediaExtractor/MediaMetadataRetriever)
- [ ] File operations: rename/delete/share from library grid (long-press menu)
- [ ] Favorites screen, Recently Played screen, Playlist screen (schema exists)
- [ ] Settings screen (theme, default speed, gesture sensitivity toggles)
- [ ] Thumbnail generation fallback for formats Coil/MediaStore doesn't thumbnail natively
- [ ] Real app icon (two-tone per house style used in AdShield, or new NoctPlayer mark)
- [ ] Handle codec-unsupported / corrupt-file playback errors gracefully in PlayerScreen (partial: generic error state exists, not codec-specific)

## Crash Log History
None yet — no builds run outside this environment (static generation only).

## Known Risks / Unverified Items
- `Modifier.graphicsLayer(scaleX=, scaleY=)` import path used
  (`androidx.compose.ui.graphics.graphicsLayer`) — verify resolves correctly
  against Compose BOM 2024.06.00 when opened in Android Studio; alternate
  import is `androidx.compose.ui.draw.graphicsLayer` (lambda form) if it doesn't.
- No `gradle-wrapper.jar`/`gradle-wrapper.properties` included — CI generates
  the wrapper on first run (`gradle wrapper --gradle-version 8.9`); for local
  Termux builds, run `gradle wrapper` once after first `git push` if you build
  locally instead of relying on CI.
- Room `exportSchema = true` but no `schemas/` directory checked in yet —
  first local build will generate it; consider committing it in a later batch.

## Scope of Guarantee
This delivery is static-analysis-only (import/symbol consistency, structural
correctness, manifest/gradle sanity). No Android SDK/emulator/runtime available
in this environment — first real verification must happen via CI or Android
Studio.
