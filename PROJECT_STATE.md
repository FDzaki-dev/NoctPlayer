# PROJECT_STATE.md — NoctPlayer

## Current State
Version: 0.3.0 (Phase 1 — Core Architecture, UI/UX polish pass)
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

## v0.3.0 — UI/UX Polish Pass (Atomic Change)
Cross-cutting visual/interaction refresh across theme + library + player modules.
Treated as one atomic batch (not split by module) because a partial apply would
leave visually inconsistent screens (e.g. new type scale in Library but not
Player). Files touched: 7 (Color.kt, Type.kt, Theme.kt, LibraryScreen.kt,
LibraryViewModel.kt, PlayerScreen.kt, PlayerViewModel.kt) + 1 new
(ui/components/Shimmer.kt). Confidence: 90% (static-analysis only, no
device/emulator — see Scope of Guarantee).
- **Theme**: expanded color roles (secondary, outline, surfaceContainer tiers,
  progress-track, scrim colors) and full Material3 type scale (headline/title/
  body/label at all sizes). `NoctPlayerTheme` now forces light (white) system
  status-bar icons via `WindowInsetsController`, matching the AMOLED-black-only
  design (previously relied on default/light-icon heuristics).
- **Library screen**: animated search field (expand/collapse instead of hard
  toggle), item-count subtitle in top bar, sort-menu selection dot, redesigned
  empty state (icon + two-line copy + tonal CTA button), skeleton-grid loading
  state (`LibraryGridSkeleton`, new shared `Shimmer.kt`) replacing the bare
  center spinner, grid cells now show a **continue-watching progress bar**
  (bottom of thumbnail, teal accent) sourced from `WatchProgressEntity`, plus
  a bottom gradient scrim (was flat black chip) and a press-scale animation.
- **Player screen**: control overlay now cross-fades in/out (`AnimatedVisibility`
  + fade) instead of an instant show/hide cut; top/bottom bars use gradient
  scrims instead of flat 50%-alpha black bands; gesture hint bubble now shows
  an icon (rewind/forward/brightness/volume) alongside the value instead of
  text only; favorite icon has a spring-scale pop on toggle and tints teal
  when active; slider and speed label recolored to the accent when non-default;
  speed picker dialog restyled to the dark theme (was default light-ish
  `AlertDialog` colors) with a highlighted current-speed row.
- **Bug fix found during polish**: `PlayerViewModel.load()` never initialized
  `isFavorite` from `repository.isFavorite(id)`, so the favorite icon always
  opened as unfilled even for already-favorited videos. Fixed by reading the
  flow once (`.first()`) on load.
- **New AI assumption**: continue-watching progress bar is hidden once a video
  is marked finished (`WatchProgressEntity.isFinished`) or has no duration yet
  (SAF items before first playback) — same "unknown" cases `formatDuration`
  already treats as `--:--`. Revisit if a "recently finished" indicator is
  wanted later.

## Roadmap (not yet implemented)
- [ ] Subtitle rendering: SRT/ASS/SSA via Media3 subtitle support, delay + style controls
      — `SubtitlePrefsRepository` (DataStore) and `TrackSelectionHelper` (audio/subtitle
      track listing + selection + external-subtitle attach via SAF) are already written
      and functional but **not yet wired into PlayerScreen** — next batch, not a stub.
- [ ] Audio track selector + subtitle track selector UI (backing functions exist in
      `TrackSelectionHelper.kt`; needs a bottom-sheet UI to call them)
- [ ] LibVLC fallback engine + codec-failure auto-switch from ExoPlayer
- [ ] Video info panel: codec, bitrate, fps, resolution, file size (MediaExtractor/MediaMetadataRetriever)
- [ ] File operations: rename/delete/share from library grid (long-press menu)
- [ ] Favorites screen, Recently Played screen, Playlist screen (schema exists)
- [ ] Settings screen (theme, default speed, gesture sensitivity toggles)
- [ ] Thumbnail generation fallback for formats Coil/MediaStore doesn't thumbnail natively
- [ ] Real app icon (two-tone per house style used in AdShield, or new NoctPlayer mark)
- [ ] Handle codec-unsupported / corrupt-file playback errors gracefully in PlayerScreen (partial: generic error state exists, not codec-specific)

## Crash Log History
- 2026-08-06 — CI run 84287811225: `app:compileReleaseKotlin` failed, 8x
  "Unresolved reference: dp" in `PlayerScreen.kt` (lines 69,71,212,279,285,302).
  Cause: missing `import androidx.compose.ui.unit.dp`. Fixed in v0.1.1; project-
  wide swept for the same gap, none found elsewhere.

## Known Risks / Unverified Items
- **MediaStore.Files column availability across OEMs (v0.2.0 fix, unverified on
  real hardware)**: extension-based query + non-throwing column lookups are a
  defensive design, but the actual behavior on Infinix XOS / other OEM skins
  hasn't been confirmed on-device yet. If videos still don't appear after this
  update, the SAF "Add folder" fallback is the guaranteed path — ask the user
  to add the folder manually and treat that as a strong diagnostic signal
  (MediaStore path failing on their device specifically).
- **SafFolderScanner tree walk**: recursive `DocumentFile.listFiles()` makes
  one IPC round-trip per directory level; large folder trees (thousands of
  files / deep nesting) may be slow. No pagination/depth-limit implemented
  yet — acceptable for a user-added handful of folders, worth revisiting if
  reports of slow "Add folder" scans come in.
- **SAF duration/resolution unknown until first playback** (`durationMs = 0`,
  shown as "--:--" in the grid) since DocumentFile doesn't expose media
  metadata — only MediaStore does. A real fix would need MediaMetadataRetriever
  per file at scan time, which is expensive for large folders; deferred.
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
- `TrackSelectionHelper.kt`'s `attachExternalSubtitle` and `SubtitlePrefsRepository`
  are written but unused by any screen yet (Phase 2 in progress) — dead code
  from the compiler's perspective until wired in, not a functional risk.

## Scope of Guarantee
This delivery is static-analysis-only (import/symbol consistency, structural
correctness, manifest/gradle sanity). No Android SDK/emulator/runtime available
in this environment — first real verification must happen via CI or Android
Studio.
