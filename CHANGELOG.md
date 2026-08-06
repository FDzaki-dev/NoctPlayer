# Changelog

## [0.3.0] - 2026-08-07 — UI/UX polish pass
### Added
- `ui/components/Shimmer.kt`: `ShimmerBlock` + `LibraryGridSkeleton` skeleton
  loading placeholders
- Continue-watching progress bar on library grid cells, sourced from
  `WatchProgressEntity` via a new `LibraryGridItem` wrapper in
  `LibraryViewModel`
- Expanded theme: secondary/outline/surfaceContainer color roles, full
  Material3 type scale, forced light system status-bar icons

### Changed
- `LibraryScreen.kt`: animated search field, item-count subtitle, sort-menu
  selection indicator, redesigned empty state, skeleton grid replaces bare
  spinner, grid cells get bottom gradient scrim + press-scale animation
- `PlayerScreen.kt`: controls overlay cross-fades instead of hard cut,
  gradient scrims replace flat-alpha bars, gesture hint bubble now shows an
  icon, favorite icon has a spring-scale pop, slider/speed label accent-tinted
  when active, speed picker dialog restyled to dark theme
- `LibraryViewModel.kt`: `uiState.items` is now `List<LibraryGridItem>`
  (media + progress fraction) instead of `List<MediaItemEntity>` — combines
  `observeRecentlyPlayed()` via a chained 5-arg + 2-arg `combine()` to avoid
  kotlinx.coroutines' unsafe vararg `combine` overload for 6+ flows

### Fixed
- `PlayerViewModel.load()`: favorite icon always opened unfilled even for
  already-favorited videos, because `isFavorite` was never read from
  `repository.isFavorite(id)` on load. Now reads it once via `.first()`.

### Impact Report
- Files touched: 8 (7 modified: Color.kt, Type.kt, Theme.kt, LibraryScreen.kt,
  LibraryViewModel.kt, PlayerScreen.kt, PlayerViewModel.kt; 1 new: Shimmer.kt)
- Treated as one atomic batch spanning theme + library + player modules —
  justified in PROJECT_STATE.md (partial apply would be visually inconsistent)
- Confidence: 90% — internally consistent, symbol/import-checked; no
  device/emulator available to confirm gradient/animation rendering or
  `combine()` chain behavior at runtime (first real check is CI or Android
  Studio)

## [0.2.0] - 2026-08-06 — Reliable video detection (dual-source scanning)
### Added
- `SafFolderScanner`: user-added SAF folder trees scanned directly via
  DocumentsProvider, bypassing MediaStore indexing entirely — guaranteed
  fallback for videos the system hasn't scanned or misclassifies
- `SafFolderPrefs`: persists which folders the user added (with persisted
  URI permission) across app restarts
- "Add folder" button in Library top bar + empty-state prompt, using
  `ActivityResultContracts.OpenDocumentTree`
- `SupportedFormats.kt`: single source of truth for supported extensions
  (mp4, mkv, avi, mov, webm, flv, ts, m4v, 3gp, mpg) shared by both scanners

### Changed (breaking, atomic)
- `MediaStoreScanner` now queries `MediaStore.Files` filtered by filename
  extension instead of `MediaStore.Video.Media`. Root issue: some OEM
  MediaProviders (verified concern on this project's reference device,
  Infinix XOS) fail to classify `.mkv`/`.ts`/`.flv`/`.avi` as
  `MEDIA_TYPE_VIDEO`, making them invisible to a `Video.Media`-only query
  even though the file plays fine. Extension-based `Files` query catches
  those misclassified rows; column lookups are now non-throwing with safe
  defaults for older/incomplete provider schemas.
- **Schema/ID scheme**: `MediaItemEntity` primary key changed from
  `mediaStoreId: Long` to `id: String` (source-prefixed: `ms:<id>` /
  `saf:<sha1>`), so MediaStore- and SAF-sourced items can coexist without
  collision. Cascaded through `WatchProgressEntity`, `FavoriteEntity`,
  `PlaylistItemEntity`, all DAOs, `MediaRepository`, `LibraryViewModel`,
  `PlayerViewModel`, `PlayerScreen`, `NavGraph` (route arg now URL-encoded
  String, not `NavType.LongType`), `MainActivity`. Applied as one atomic
  batch — a partial apply would not compile.
- Room DB bumped to version 2 with `fallbackToDestructiveMigration()` (no
  shipped users on v1; replace with a real migration before public release)
- Added `androidx.documentfile:documentfile:1.0.1` dependency

### Impact Report
- Files touched: 19 (14 modified, 5 new: `SafFolderScanner.kt`,
  `SafFolderPrefs.kt`, `SupportedFormats.kt`, `Converters.kt`, this changelog)
- Confidence: 80% — logic is internally consistent and statically verified
  (brace/XML/YAML checks pass, `mediaStoreId` sweep clean), but the
  `MediaStore.Files` column-availability behavior across OEM skins and the
  DocumentFile tree-walk performance on large folders are both unverified
  without a real device (see PROJECT_STATE.md risks)

## [0.1.2] - 2026-08-06 — CI release asset naming
### Changed
- `.github/workflows/build.yml`: release APK asset renamed from generic
  `app-release.apk` to `NoctPlayer-v${VERSION}-release.apk` before signature
  verification and GitHub Release publish, so the Releases sidebar shows a
  self-describing filename per build/version instead of the Gradle default.

## [0.1.1] - 2026-08-06 — CI build fix
### Fixed
- `PlayerScreen.kt`: missing `import androidx.compose.ui.unit.dp` caused 8
  "Unresolved reference: dp" compile errors, confirmed from CI run
  `84287811225` (`app:compileReleaseKotlin` failure). Root cause: `dp` usages
  were added incrementally while writing the gesture/controls overlay and the
  import was never added — static brace/XML checks in Phase 1 didn't catch it
  because they don't do symbol resolution. Swept the rest of the project for
  the same class of gap (any `.dp`/`.sp` usage without its `unit` import) —
  no other files affected.

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
