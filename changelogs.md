# File Claw - Changelog
Last Updated: October 15, 2026
Document Version: 1.2
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

## 3. Current Version Section
**Current Version:** v1.2.0
**Release Date:** October 15, 2026
**Total Changes:** MD/TXT experience overhaul — light-mode status bar fix, clean top bar, working Document Outline & Viewer Appearance (with browser-style 25%–500% zoom), fully working in-document search, and a premium inline edit mode on the same screen. First loop with a fully compiled and tested build (134 unit tests green, debug APK assembled).

**Highlights:**
- Status bar icons now follow the in-app theme in light mode (were invisible on white canvas).
- Top bar shows only back arrow + file name + 3-dot menu; duplicate action icons removed.
- Document Outline and Viewer Appearance menu items are wired to real bottom sheets and work.
- In-document search auto-focuses with the keyboard raised; matches navigate correctly.
- Edit button morphs the preview into an editor in place (no separate screen): undo/redo/tab/bold/italic/history tray, X-to-discard confirmation, save confirmation, export modified copy.
- Viewer Appearance gains syntax themes (incl. Auto), font size control, and browser-style zoom 25%–500% persisted across sessions.
- Pre-existing unit-test compilation error fixed and a real overlap-dedup bug in `TextFileSearchEngine` fixed; full test suite now green.

**Issues In Progress:**
- See `known-issues.md`; next-loop prompt lives at `docs/prompts/NEXT_LOOP_PROMPT.md`.

## 4. Version History

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

## 5. Change Entry Template
All upcoming pulls or changes MUST map to this template:

```markdown
### [vX.Y.Z] - YYYY-MM-DD
**[TYPE]**
- **Affected:** `Module/Class`
- **Description:** What changed and why.
- **Author:** Name/ID
- **[BREAKING]** (If applicable): Migration details.
```

## 6. Update Rules
- **When**: MUST be updated before any branch merge into `main` or `release`.
- **Who**: The developer executing the change.
- **Review**: Required during PR code review.

## 7. User-Facing Changelog
- Derive user notes by ignoring `[REFACTOR]` and technical `[FIXED]` entries.
- Focus on `[ADDED]` features and user-reported `[FIXED]` items.
- Example User Note: "Added support for viewing password-protected PDFs!"

## 8. Rollback Log
*No rollbacks recorded yet.*

---
**Update Log**
- 2026-06-15: v1.0 - Initial document creation.
- 2026-06-16: v1.1 - Added changelog listings for v1.0.1 hotfix and rendering optimizations.
