# File Claw - Changelog
Last Updated: October 16, 2026
Document Version: 1.3
Next Review Date: January 15, 2027

## 1. Changelog Format Rules (MANDATORY)
- **Every entry MUST include**: Version number, Date, Change Type, Affected Files/Modules, Description.
- **Change types**: `[ADDED]`, `[CHANGED]`, `[DEPRECATED]`, `[REMOVED]`, `[FIXED]`, `[SECURITY]`, `[PERFORMANCE]`, `[REFACTOR]`.
- **Breaking changes**: MUST be prefixed with `[BREAKING]` and include a migration guide.

## 2. Version Numbering System
- **MAJOR**: Architectual overhauls, massive UI structural changes, breaking data migrations.
- **MINOR**: New features (Audio player, PDF editing), significant enhancements.
- **PATCH**: Bug fixes, minor visual tweaks.
- Example: `v1.2.4` (Major 1, Minor 2, Patch 4).

## 3. Unreleased Changes (pending device verification)
**Version:** v1.3.1 (pending — branch `v1.3.1`; the Gradle `versionName` stays `1.3.0` until the APK built by the `Build and Release` workflow is device-tested; the workflow's version input carries the release identity so no inconsistent numbers ship)
**Date:** October 16, 2026
**Change Type:** `[FIXED]` / `[ADDED]` / `[CHANGED]`
**Verification status:** Kotlin compilation, unit tests and APK assembly NOT run in the patch environment (no Android SDK/JDK); delegated to GitHub Actions (`build-and-release.yml`). JavaScript syntax checks, XML validation and static structure checks were run and passed. Device behaviors below are implemented but not yet hardware-verified.

**[ADDED]**
- **Affected:** `ui/screens/FilePreviewScreen.kt`
- **Description:** Chrome-style header find bar: while in-document search is active, the app bar row itself is replaced by a compact one-line find bar (close control, focused single-line query field, live `current / total` match badge including `0 / 0`, previous/next arrows, clear-query control); the filename and overflow menu stay hidden until search closes; system back closes find mode first.
- **Author:** Loop 10 senior-agent team
- **Affected:** `core/format/PinchGestureGate.kt` (new), `src/test/java/com/example/core/format/PinchGestureGateTest.kt` (new)
- **Description:** Pure multi-touch gesture gate for the DOCX pinch pipeline: discards transition-event deltas (mixed pointer history), forwards only confirmed stable two-pointer deltas, disarms on finger lift/cancel. Includes seven dedicated unit tests.
- **Author:** Loop 10 senior-agent team
- **Affected:** `core/format/PdfRenderBudgetPolicy.kt` (new), `src/test/java/com/example/core/format/PdfRenderBudgetPolicyTest.kt` (new)
- **Description:** Device-tiered per-page PDF canvas pixel budget derived from heap signals only (`ActivityManager.isLowRamDevice`, `memoryClass` as an app-heap cap — never read as physical RAM): 6M (low-RAM), 9M (<128 heap class), 12M (128–255), 16M (≥256), hard-clamped to [4M, 24M]. Includes eight dedicated unit tests.
- **Author:** Loop 10 senior-agent team
- **Affected:** `ui/screens/DocumentPreviewSupport.kt`, `src/test/java/com/example/ui/screens/InViewerSearchNavigationTest.kt` (new)
- **Description:** Pure `InViewerSearchNavigation` next/prev wrap-around math shared by the header arrows and the IME Search action; stale indices are clamped, empty match lists return null. Includes six dedicated unit tests.
- **Author:** Loop 10 senior-agent team

**[FIXED]**
- **Affected:** `ui/screens/FilePreviewScreen.kt`
- **Description:** In-document search no longer opens a second floating surface below the header — the duplicate floating overlay (149 lines) was removed and its controls moved into the header find bar; closing search hides the keyboard, restores the normal app bar, and clears stale highlights/counts through each engine's existing empty-query path; opening search from focus mode reveals the bars first and focuses the field once (no refocus per keystroke); prev/next are disabled while there are no navigable matches and truncated totals keep the `x/y of N` presentation.
- **Author:** Loop 10 senior-agent team
- **Affected:** `ui/screens/FilePreviewScreen.kt`, `AndroidManifest.xml`
- **Description:** Inline TXT/Markdown edit tray now sits directly above the Android keyboard while editing: `windowSoftInputMode="adjustResize"` is declared (single inset-dispatch owner, no `adjustPan`) and the tray applies the UNION of navigation-bar and IME insets (`WindowInsets.safeDrawing.only(Bottom)`) only in edit mode, so it animates above the IME without double-counting the gesture-nav area, returns to its normal floating position when the keyboard hides, and reading-mode trays keep their exact previous bottom anchoring.
- **Author:** Loop 10 senior-agent team
- **Affected:** `ui/screens/FilePreviewScreen.kt`, `assets/editor/docx_editor.html`
- **Description:** DOCX pinch zoom actually scales the rendered document now: the first (transition) pointer event no longer injects a bogus zoom delta that slammed the zoom into the 25%/500% clamp on first contact; zoom commands survive the load window (fresh zoom/theme are reapplied at page finish and unconditionally after every render pass, closing the 100%-flash race); single-finger scroll, long-press selection and horizontal panning are untouched; `@media print` zoom reset is preserved.
- **Author:** Loop 10 senior-agent team
- **Affected:** `ui/screens/FilePreviewScreen.kt`
- **Description:** The pure-Kotlin DOCX fallback renderer no longer shows dead zoom controls: the zoom-out button, percentage chip and zoom-in button are hidden when the fallback renderer took over, while its text preview and search keep working.
- **Author:** Loop 10 senior-agent team
- **Affected:** `assets/pdfjs/search.html`, `ui/screens/FilePreviewScreen.kt`
- **Description:** PDF text stays sharp after pinch zoom in the practical 2–3× range: the per-page canvas budget is now device-tiered (pushed from Kotlin via `setPdfQualityBudget`), the render-ratio cap rose 6→8, re-renders draw into a fresh offscreen canvas that is swapped in on completion (the previously visible bitmap is never destroyed mid-render — no more blank flash after pinch), redundant re-renders at an already-achieved ratio are skipped, in-flight renders do a single bounded follow-up when the achievable ratio grew, and search highlight coordinates stay percentage-based (zoom-aligned).
- **Author:** Loop 10 senior-agent team

**[CHANGED]**
- **Affected:** `assets/pdfjs/search.html`
- **Description:** `pagePixelBudget` is mutable (`let`, default 9.0e6) and clamped inside `setPdfQualityBudget(pixels)` to [4.0e6, 24.0e6]; `maxRenderRatio` raised from 6 to 8 so high-density screens can reach the devicePixelRatio × pinch-scale backing they need; visible pages re-render first, offscreen canvases are zeroed on release as before.
- **Author:** Loop 10 senior-agent team
- **Affected:** `worklog.md` (new), `known-issues.md`, `docs/TODO.md`, `docs/prompts/NEXT_LOOP_PROMPT.md`, `PATCH_NOTES_LOOP10.md` (new)
- **Description:** Loop 10 documentation: structured worklog entry `LOOP10-SEARCH-IME-ZOOM` (per-persona audit findings, evidence, true check outcomes, device-pending list), Loop 10 known-issue watchlist, TODO loop section, and the replacement next-loop prompt. Patch ZIP contains only the changed files at repository-relative paths (no APK, no build output, no temporary scripts).
- **Author:** Loop 10 senior-agent team

## 4. Current Version Section
**Current Version:** v1.3.0
**Release Date:** October 15, 2026
**Total Changes:** Search experience overhaul (relevance-ranked global search, match highlighting, themed in-viewer search bar, keyboard actions, WebView re-highlight debounce) + PDF reader fixes (crisp pinch re-rasterization like Google Drive, smart dark mode that keeps photos natural, smooth focus-mode transition) + DOCX upgrades (opens at 40% zoom, pinch-to-zoom, double-tap 40%⇄100% toggle, persisted zoom). Dedicated tests are included for the new pure-Kotlin policies. This repository integration has not been build/test-verified here; Android APK assembly remains delegated to GitHub Actions.

**Highlights:**
- Global search now ranks results (exact > prefix > word-boundary > substring), highlights the matched part of each file name, shows per-section result counts and a "first 150 of 150+" notice, and no longer lists files deleted from disk.
- Search-term history moved to a robust JSON storage with legacy migration and a "Clear all" action.
- In-viewer search bar: terracotta match-count badge, animated prev/next controls, keyboard Search action jumps to the next match, 40dp touch targets, and themed terracotta highlights across MD/TXT/PDF pipelines.
- PDF zoom re-rasterizes pages at the settled pinch scale with a per-page pixel budget — text stays crisp at up to 5× instead of magnifying a blurry bitmap.
- PDF smart dark mode classifies each rendered page (photo-heavy vs text) and only inverts text pages; photos and scanned portraits keep their natural colors.
- Focus mode transition is smooth everywhere: content resizes with the animating bars, status-bar inset no longer flips, and the PDF WebView resize storm (blank flashing pages) is debounced away.
- DOCX documents open at 40% zoom by default (persisted across sessions), support pinch in/out and a double-tap 40%⇄100% toggle, with no 100% flash on open; the zoom slider now includes 40% as a stop.
- New pure-logic modules `FileSearchRanker` and `DocxZoomPolicy` with dedicated unit tests.

**Issues In Progress:**
- See `known-issues.md`; next-loop prompt lives at `docs/prompts/NEXT_LOOP_PROMPT.md`.

## 5. Version History

### [v1.3.0] - Search Polish, PDF Reader Fixes & DOCX Zoom (2026-10-15)
**[ADDED]**
- **Affected:** `core/search/FileSearchRanker.kt` (new), `src/test/java/com/example/core/search/FileSearchRankerTest.kt` (new)
- **Description:** Pure relevance-ranking + match-highlight policy for global search: exact (400) > prefix (300) > word-boundary (200) > substring (100), tie-broken by shorter then alphabetical name; exposes `rank()`, `firstMatchRange()` and `isTruncated()`. Includes six dedicated unit tests.
- **Author:** Loop 8 senior-agent team
- **Affected:** `core/format/DocxZoomPolicy.kt` (new), `src/test/java/com/example/core/format/DocxZoomPolicyTest.kt` (new)
- **Description:** DOCX zoom policy: shipped default 40%, clamp 25–500%, pinch multiply with de-noising, double-tap 40%⇄100% toggle, percent conversions. Includes six dedicated unit tests.
- **Author:** Loop 8 senior-agent team
- **Affected:** `ui/screens/FilePreviewScreen.kt`, `assets/editor/docx_editor.html`, `ui/component/AppearanceBottomSheet.kt`
- **Description:** DOCX pinch-to-zoom (Compose-level 2-finger interception on the Initial pointer pass driving the shared `setZoom()` pipeline; single-finger scroll untouched) and double-tap 40%⇄100% toggle (JS-side detection with a new `onDocumentDoubleTap` bridge). DOCX zoom now persists per session via `ViewerAppearancePrefs.get/setDocxZoomPercent`.
- **Author:** Loop 8 senior-agent team
- **Affected:** `ui/screens/FilePreviewScreen.kt` (`PdfPreview`)
- **Description:** `onScaleChanged` + debounce now feeds the settled pinch scale to the new `setPdfRenderScale()` JS hook, which re-rasterizes visible pages at the new resolution without touching CSS layout (Google-Drive-style crisp zoom).
- **Author:** Loop 8 senior-agent team

**[CHANGED]**
- **Affected:** `.github/workflows/build-and-release.yml`
- **Description:** Updated the manual release workflow's default version-name input to v1.3.0 so a default dispatch matches the app's current version; version-code auto-generation and the release workflow trigger remain unchanged.
- **Affected:** `assets/pdfjs/search.html`
- **Description:** Page rasterization is now hi-DPI aware and zoom-aware: desired ratio = devicePixelRatio × pinch scale, capped by a ~9M-pixel per-page budget (≈36MB ARGB) and ratio 6 — sharp at high zoom with bounded memory. Search highlights re-themed to the Claude terracotta palette. Night mode now uses a full luminance inversion (`invert(1) hue-rotate(180deg) brightness(.94) contrast(.92)`) on text pages.
- **Author:** Loop 8 senior-agent team
- **Affected:** `assets/pdfjs/search.html`
- **Description:** Smart dark mode: every rendered page is classified via a 48px downsample measuring saturated-pixel fraction; pages >4% colorful pixels get the `photo` class and keep natural colors (only slightly dimmed) instead of becoming ghost-like negatives. Runs once per render, filter-independent, best-effort.
- **Author:** Loop 8 senior-agent team
- **Affected:** `assets/pdfjs/search.html`
- **Description:** WebView `resize` handler debounced (180ms) and `updateRecordGeometry` skips records whose viewport is unchanged — kills the per-frame canvas zeroing/re-render storm that flashed blank pages during focus-mode transitions and rotations.
- **Author:** Loop 8 senior-agent team
- **Affected:** `ui/screens/SearchScreen.kt`, `viewmodel/MainViewModel.kt`, `MainActivity.kt`
- **Description:** Global search upgraded: relevance-ranked results (via `FileSearchRanker`), files deleted from disk are filtered before display, new `deviceSearchTruncated` state drives a "first 150 of 150+" notice, result rows carry a highlighted matched substring (terracotta bold), section headers show result counts, on-palette accent colors replace hardcoded hex values, rows use 16dp radius + hairline border + `animateItemPlacement()`, empty state gained a hint line, and the screen now raises the keyboard on open. Search-term history persisted as a JSON array with legacy `|*|` migration plus a "Clear all" action. Dead `onNavigateToPreview` parameter removed.
- **Author:** Loop 8 senior-agent team
- **Affected:** `ui/screens/FilePreviewScreen.kt`
- **Description:** DOCX documents open at 40% zoom by default (seeded from the persisted preference) and the zoom is applied BEFORE `loadDocx`, so the document never flashes at 100%. Zoom slider switched to 0.05 steps so 40% is a reachable stop, with a policy-driven coerce. Bottom-bar DOCX gate now keys on `DocxSuccess` state only (legacy `.doc` hex view no longer shows zoom buttons).
- **Author:** Loop 8 senior-agent team
- **Affected:** `ui/screens/DocumentPreviewSupport.kt`, `ui/screens/FilePreviewScreen.kt`
- **Description:** MD/TXT search highlight CSS re-themed from amber to the Claude terracotta palette (translucent fill, solid terracotta active state with warm outline).
- **Author:** Loop 8 senior-agent team

**[FIXED]**
- **Affected:** `assets/pdfjs/search.html`, `ui/screens/FilePreviewScreen.kt`
- **Description:** PDF zoom blurriness (user-reported): pages were rasterized once at ≤2 device pixels per CSS pixel and pinch only magnified that bitmap. Now the settled pinch scale re-rasterizes visible pages at the new resolution inside a pixel budget, keeping text crisp up to 5×.
- **Author:** Loop 8 senior-agent team
- **Affected:** `assets/pdfjs/search.html`
- **Description:** PDF dark-mode "ghost people" (user-reported): whole-canvas `invert(.88)` turned faces into washed-out negatives. Smart per-page classification now keeps photo-heavy pages natural while text pages get a clean full inversion.
- **Author:** Loop 8 senior-agent team
- **Affected:** `ui/screens/FilePreviewScreen.kt`
- **Description:** Focus-mode transition (user-reported, all file types): content padding flipped binary at frame 0 while bars were still animating, and the top-bar/status-bar inset flipped instantly — the jarring snap is gone because scaffold insets are now always applied and animate frame-by-frame; all bar/pill/overlay transitions share matched 220–260ms FastOutSlowInEasing specs; the top bar and search overlay no longer teleport under the status bar mid-animation.
- **Author:** Loop 8 senior-agent team
- **Affected:** `ui/screens/FilePreviewScreen.kt`
- **Description:** In-viewer search overlay: keyboard Search/Enter now jumps to the next match (shared `goToNextMatch`/`goToPrevMatch` handlers with the arrow buttons), match counter upgraded to a themed badge (error-tinted on zero matches), prev/next controls animate in/out with scale+fade and use 36dp targets. WebView re-highlight is debounced 150ms so typing in large documents no longer re-runs the DOM highlight per keystroke.
- **Author:** Loop 8 senior-agent team
- **Affected:** `viewmodel/MainViewModel.kt`
- **Description:** Global search could surface deleted files (tapping failed); results are now existence-filtered after ranking. Search-term history used a fragile `|*|` separator that corrupted on queries containing it; storage migrated to JSON with fallback read.
- **Author:** Loop 8 senior-agent team

### [v1.2.0] - MD/TXT Premium Round: Inline Edit, Outline, Appearance & Zoom (2026-10-15)
**[FIXED]**
- **Affected:** `ui/theme/Theme.kt`
- **Description:** Light-mode status bar icons were invisible: `enableEdgeToEdge()` derived icon appearance from the SYSTEM dark-mode flag once at Activity creation while the visible canvas followed the IN-APP theme. `FilesClawTheme` now re-asserts `isAppearanceLightStatusBars/NavigationBars` from the in-app theme on every relevant recomposition, so icons always contrast with the actual background (and follow live theme changes).
- **Author:** Loop 7 senior-agent team
- **Affected:** `ui/screens/FilePreviewScreen.kt`
- **Description:** Removed the duplicated action icon row (Search, Outline, Focus, Copy) from the preview top bar; it now keeps only back navigation, the file name, and the 3-dot menu. Focus mode remains reachable via tap gestures and the floating exit pill.
- **Author:** Loop 7 senior-agent team
- **Affected:** `ui/screens/DocumentPreviewSupport.kt`, `ui/screens/FilePreviewScreen.kt`
- **Description:** In-document search in MD/TXT previews now auto-focuses the field and raises the keyboard when the overlay opens (the search pipeline itself was complete; focus was the missing UX link).
- **Author:** Loop 7 senior-agent team
- **Affected:** `core/text/TextFileSearchEngine.kt`
- **Description:** Fixed cross-chunk overlap deduplication: candidates starting inside an already-counted match's byte range are no longer double-counted (regression-tested with `bufferSize=2`).
- **Author:** Loop 7 senior-agent team
- **Affected:** `src/test/java/com/example/core/text/TextFileSearchEngineTest.kt`
- **Description:** Fixed pre-existing test compilation error (String passed where ByteArray expected). Full suite: 134 tests, 0 failures.
- **Author:** Loop 7 senior-agent team

**[ADDED]**
- **Affected:** `ui/screens/FilePreviewScreen.kt`, `ui/component/DocumentOutlineBottomSheet.kt`
- **Description:** Document Outline is now functional for MD/TXT: the previously orphaned bottom sheet is rendered from the 3-dot menu and heading taps jump to the exact heading in the WebView (heading-index anchoring with line fallback via new `scrollToHeadingIndex`/`scrollToLine` JS helpers).
- **Author:** Loop 7 senior-agent team
- **Affected:** `ui/component/AppearanceBottomSheet.kt`, `ui/screens/FilePreviewScreen.kt`, `ui/screens/DocumentPreviewSupport.kt`
- **Description:** Viewer Appearance is now functional: syntax themes (Auto/Claude Glass/Nord Dark/Warm Parchment) recolor MD/TXT documents, font size (11–22sp) controls the base text size, and a browser-style DOCUMENT ZOOM section (25%–500% slider, ± buttons, presets) applies live via `setViewerZoom`. All preferences persist in `viewer_appearance_prefs`.
- **Author:** Loop 7 senior-agent team
- **Affected:** `viewmodel/MainViewModel.kt`, `ui/screens/InlineTextEditorSupport.kt` (new), `ui/screens/FilePreviewScreen.kt`, `ui/screens/EditorScreen.kt`
- **Description:** Premium inline edit mode for MD/TXT: tapping Edit morphs the preview screen itself into an editor (no separate editor route, no line numbers, same surface). Bottom tray swaps in place to Undo / Redo / Tab / Bold / Italic / History / Save; the top bar swaps back-arrow for an X that discards with confirmation; save runs through the charset-safe `.bak`-backup pipeline behind a confirmation dialog; 3-dot menu gains Save changes / Export modified copy; system back honors unsaved-changes flow. Markdown-aware bold/italic wrap-toggle helpers are pure and unit-tested (`InlineTextEditTest`, 16 tests).
- **Author:** Loop 7 senior-agent team

**[CHANGED]**
- **Affected:** `docs/prompts/`, root helper scripts, `changelogs.md`
- **Description:** Removed archived loop prompts (LOOP_02/03/07) and all one-off patch/fix/debug scripts from previous loops; only `docs/prompts/NEXT_LOOP_PROMPT.md` (Loop 8) is kept. This changelog updated to v1.2.0.
- **Author:** Loop 7 senior-agent team
- **Affected:** `docs/TODO.md`
- **Description:** Loop 6 "never compiled" known issue resolved: this loop compiled Kotlin, ran all 134 unit tests, and assembled a debug APK with Android SDK 34 + JDK 17 in CI sandbox.
- **Author:** Loop 7 senior-agent team

### [v1.0.4] - Sidebar, Auto-Encoding & Large PDF Fixes (2026-06-28)
**[ADDED]**
- **Affected:** `HomeScreen.kt`, `FileTreeComponent.kt`
- **Description:** Added a collapsible file tree navigation sidebar in the modal drawer.
- **Affected:** `FileManager.kt`, `SettingsScreen.kt`, `SettingEntity.kt`
- **Description:** Added an "Auto" encoding option, set as the default, which detects file encoding via BOM headers. Added extra encodings to the settings dropdown.

**[CHANGED]**
- **Affected:** `MainViewModel.kt`
- **Description:** Redesigned the temporary cache system to properly cache up to 5 files and skip caching for files larger than 30MB to prevent IO delays.
- **Affected:** `MainViewModel.kt`, `FilePreviewScreen.kt`
- **Description:** Refactored `PdfSuccess` to avoid reading the entire PDF into a Base64 string, preventing `OutOfMemoryError` on 100MB+ PDF files.

**[FIXED]**
- **Affected:** `AnimateItemMock.kt`
- **Description:** Removed mock to properly leverage Jetpack Compose's `animateItemPlacement` alongside Flinger library for 120fps smooth scrolling.
- **Affected:** `FileManager.kt`
- **Description:** Truncated text files to 5MB max length when rendering in the text previewer to prevent OOM errors on large text files.

### [v1.0.3] - Unified Document Viewers & Jam-less WebView Transitions (2026-06-18)
**[ADDED]**
- **Affected:** `/app/src/main/assets/mammoth/mammoth.browser.js`, `/app/src/main/assets/pdfjs/build/pdf.js`, `/app/src/main/assets/pdfjs/build/pdf.worker.js`
- **Description:** Added offline web assets to assets folders for client-side document processing directly in WebView.
- **Affected:** `FilePreviewScreen.kt`
- **Description:** Added `PdfPreviewWebView` and `DocxPreviewWebView` implementations.
**[CHANGED]**
- **Affected:** `FilePreviewScreen.kt`
- **Description:** Updated `MarkdownPreview` to support a continuous theme-matched loading progress overlay during page parsing and WebView instantiation.
- **Affected:** `MainViewModel.kt`
- **Description:** Updated `FileContentState.PdfSuccess` and `FileContentState.DocxSuccess` to carry pre-processed Base64 strings loaded in IO Dispatchers to support instantaneous document file clicks with zero main-thread blockage.

### [v1.0.2] - Markdown WebView Overhaul (2026-06-17)
**[ADDED]**
- **Affected:** `build.gradle.kts`
- **Description:** Added `org.jetbrains:markdown:0.7.3` dependency to parse markdown natively into HTML payload.
**[CHANGED]**
- **Affected:** `FilePreviewScreen.kt`
- **Description:** Shifted primary `MarkdownPreview` method from standard Compose text annotations to a full GPU-accelerated WebView implementation inheriting a Claude CSS aesthetic and JS bridges.

### [v1.0.1] - Startup Crash Hotfix & Optimizations (2026-06-16)
**[FIXED]**
- **Affected:** `AppDatabase.kt`, `RecentFileEntity.kt`
- **Description:** Fixed immediate application launch crash by bumping the Room database version to 3, allowing `fallbackToDestructiveMigration` to safely rebuild and incorporate newly defined indexing patterns without file integrity mismatches on local SQLite storage.
- **Affected:** `EditorScreen.kt`
- **Description:** Shifted undo/redo buffer from standard `mutableStateListOf` to a lean custom stack wrapper container over `ArrayDeque` with 50-item threshold capacity. Avoided continuous recompositions of buttons and text layouts during general keystrokes.
- **Affected:** `MainViewModel.kt`
- **Description:** Debounced user text searches inside local files directory by 150ms and introduced immediate in-flight query job cancellation to keep indices querying non-blocking.
- **Affected:** `HomeScreen.kt`, `SearchScreen.kt`
- **Description:** Integrated `derivedStateOf` mapping for list selections (`take(5)`) under primary lists to leverage optimized UI-thread composition speeds.

### [v1.0.0] - Initial Release (2026-06-15)
**[ADDED]**
- Core MVVM Architecture and Navigation Graph setup.
- `FileManager` service for IO operations.
- `AppDatabase` Room integration.
- PDF Viewing with dual-engine fallback structure.
- CSV parsing and tabular viewing.
- Basic Text Editing and Saving capabilities.

## 6. Change Entry Template
All upcoming pulls or changes MUST map to this template:

```markdown
### [vX.Y.Z] - YYYY-MM-DD
**[TYPE]**
- **Affected:** `Module/Class`
- **Description:** What changed and why.
- **Author:** Name/ID
- **[BREAKING]** (If applicable): Migration details.
```

## 7. Update Rules
- **When**: MUST be updated before any branch merge into `main` or `release`.
- **Who**: The developer executing the change.
- **Review**: Required during PR code review.

## 8. User-Facing Changelog
- Derive user notes by ignoring `[REFACTOR]` and technical `[FIXED]` entries.
- Focus on `[ADDED]` features and user-reported `[FIXED]` items.
- Example User Note: "Added support for viewing password-protected PDFs!"

## 9. Rollback Log
*No rollbacks recorded yet.*

---
**Update Log**
- 2026-06-15: v1.0 - Initial document creation.
- 2026-06-16: v1.1 - Added changelog listings for v1.0.1 hotfix and rendering optimizations.
