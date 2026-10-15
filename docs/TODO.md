## Loop 9 — Four-format preview reliability follow-up (in progress)

- [x] Harden TXT/Markdown print-job lifecycle by keeping the temporary WebView alive through Android PrintDocumentAdapter completion and disposing it from the adapter finish callback.
- [x] Escape both document content and title before inserting them into the print HTML; report unavailable print service and print-start errors.
- [x] Add regression tests for repeated Unicode search matches/UTF-16 offsets, result limits, CRLF code fences, embedded backticks, and closing-fence length rules.
- [x] Add the evidence-led Loop 10 prompt at `docs/loops/LOOP_10_NEXT_LOOP_PROMPT.md`.
- [ ] Inspect latest GitHub Actions run for Loop 9 commit; unit tests, coverage, lint, Detekt and debug APK must pass.
- [ ] Real-device print lifecycle, Unicode search and fenced-code copy smoke tests remain pending.

Status: source edits are committed, but do not mark Loop 9 verified until the newest SHA's CI completes. No real-device test has been performed by this implementation pass.

## Loop 8 — Four-format document preview polish (in progress)

- [x] Added shared case-insensitive text-match offsets and fenced-code extraction utility, with unit tests for case-insensitive occurrences, indentation-preserving copy payloads, tilde fences, multiple blocks and unclosed fences.
- [x] Restored the default native reader selection for TXT/MD (rendered view remains available via the existing toggle).
- [x] Improved focus-mode edge-to-edge handling and restoration of system-bar/window state on exit/disposal.
- [x] Replaced PDF private-field color-filter reflection with the renderer's public night-mode API; image-heavy/scanned PDFs may remain renderer-limited.
- [x] Exposed DOCX in the search and immersive-focus entry points.
- [~] TXT/code-reader search now moves the viewport when Previous/Next changes the selected match; PDF page navigation exists, but raw-stream search is approximate and visual highlight behavior still needs device verification.
- [~] Added per-fenced-block copy controls in the native TXT/code reader and retained Markdown WebView code-block copy controls; verify clipboard behavior on-device.
- [~] Print routing now distinguishes PDF, DOCX, TXT/MD and images. PDF pages are rasterized one at a time into Android PrintManager output; DOCX prints the rendered WebView. Temporary WebView cleanup and print-service failure handling still need review.
- [~] DOCX dark theme now styles document text/tables while leaving images untouched; fit-page behavior and actual pinch/zoom range still require device verification.
- [~] DOCX search now counts extracted paragraph matches and uses WebView find navigation. Exact mapping between extracted paragraph offsets and rendered text must be verified against complex DOCX files.
- [ ] Run Android unit tests, coverage, lint, Detekt and debug APK CI; perform real-device smoke checks for all four formats and navigation modes.

Status note: `[~]` means implemented but not yet fully verified. GitHub Actions is running on the latest branch head; inspect the exact head SHA and final run conclusion before treating the code as build-verified. Real-device smoke tests remain required. Keep PR in draft and do not merge until the user tests the APK.

# Backlog and known issues

Priorities: **P0** data loss / crash / security · **P1** high-impact feature or correctness · **P2** quality · **P3** polish.

## KNOWN ISSUES (read first)
1. **Loop 6/7 Android verification is pending.** This editing environment could not establish a local checkout, so Gradle tasks were not run here. The dedicated .github/workflows/android-tests.yml workflow now runs on PRs to main and pushes to the Loop 6 branch. Check the actual workflow result, JUnit XML and reports before merging; no test/build pass is claimed until the run is green. Run `./gradlew :app:testDebugUnitTest`, `./gradlew compile_applet` if supplied by the build harness, and `./gradlew assembleDebug` before merge. Do not interpret the feature checklist below as a passing build report.
2. Gradle wiring for JaCoCo (`jacocoCoreReport` / `jacocoCoreVerify`), `enableUnitTestCoverage`, detekt, and Room schema export is
   unverified. detekt is `ignoreFailures = true` until its first green run.
3. Edits to files opened from another app are saved to the in-app copy only (the user is told). Write-back is P1 below.
4. WebView renderers (DOCX, PPTX, grid) still receive whole files as base64 (now capped at 25 MB, then falls back to the archive tree).
5. Hex viewer shows only the first 4 KB. Hard-coded `onDocumentLoaded(1)` makes DOCX page counts meaningless.
6. 3D viewer draws a wireframe sample (≤ 25k triangles); no shading, no GLB/PLY/DWG.
7. Legacy `.doc` / `.xls` / `.ppt` and `.rar` are identified but not openable.
8. Bundled JS libraries (docx-preview, pptx-preview, x-spreadsheet) have no license text in the shipped files; verify upstream licenses (JSZip is MIT/GPL dual, confirmed from its header).
9. `applicationId` is still the AI-Studio-generated `com.aistudio.filesclaw.apxqzy`; JitPack repository is unscoped (supply-chain exposure).

## P0
- [ ] Run the Loop 6 Quality Gate on an Android-capable runner and fix compile fallout (pending in this branch; see KNOWN ISSUES #1).
- [x] Archive-password dialog masks input (`PasswordVisualTransformation`), PDF password also masked.
- [x] `autoSaveDrafts` renamed to "Auto-save on Exit" with clear explanation; notification updated to "Changes saved to file."
- [x] Surface corrupt-PDF errors cleanly with error display and back action instead of endless spinner.
- [x] **Batch Selection Mode** in File Browser (`FileTreeComponent` and `AllFilesScreen`): multi-select files/directories with simultaneous batch Move, Copy, and Delete, backed by pure-Kotlin `BatchFileOperations` and unit tests.
- [x] **Text / Markdown / JSON Viewer Overhaul**: Default to premium in-app browser preview; enable zoom out below 1.0x (minimum-scale=0.25); one-tap copy button in top app bar; remove redundant "Show Syntax Code" menu item.
- [x] **Image Viewer Enhancements**: Theme-adaptive background (light in light theme, dark in dark theme); deep 15x zoom support; removed duplicate sub-header overlay.
- [x] **PDF Viewer Deep Zoom & Smart Night Mode**: Increased maximum zoom to 10.0f; replaced negative inversion with hue-preserving document dark mode that eliminates "negative ghost" distortion on photos and faces.
- [x] **Compact Single-Line Search**: Sleek, compact 40.dp rounded pill search bar across `SearchScreen`, `HomeScreen`, and `AllFilesScreen`.
- [x] **Unified Floating Action Pill (Loop 4)**: Lightweight glassmorphic floating control bar for PDF (page jump, search, night mode, share), Image (photo indicator, share), and Text (find, copy, edit, share) with auto-fade and tap toggle.
- [x] **Grid vs List View Toggle & Rich Cards (Loop 4)**: Added grid/list toggle in `HomeScreen` and `AllFilesScreen` with 2-column grid cards, colored `FileExtensionBadge`, size, and timestamp.
- [x] **Favorites / Starred Files & Filter Tabs (Loop 4)**: Interactive Star icon on every file card, persistent favorite storage, and `All Files` vs `★ Favorites` filter tabs.
- [x] **In-Viewer Text Search (Loop 4)**: Floating in-document search overlay with occurrences count, previous/next match navigation, and pure-Kotlin `PdfTextSearcher` engine with unit tests.
- [x] **DOCX Engine Fix & Offline OOXML Extractor (Loop 5)**: Fixed WebView DOCX preview script loading and base64 bridge; added pure-Kotlin offline OOXML extractor (`DocxExtractor.kt`) and JUnit test suite.
- [x] **Glassmorphic File Properties Bottom Sheet (Loop 5)**: Modern Jetpack Compose `ModalBottomSheet` with drag handle, size, MIME type, permissions, path copy, and async MD5/SHA-256 checksum computation.
- [x] **Staggered Spring & Fade-In Reveal Animation (Loop 5)**: Added `StaggeredAnimatedItem` across grid and list file cards.
- [x] **PDF Last-Read Page Bookmark & Auto-Resume (Loop 5)**: Automatically saves and restores page bookmarks in `SharedPreferences` with confirmation notification.
- [x] **Native Print & PDF Export (Loop 5)**: Integrated Android's native `PrintManager` and `PrintDocumentAdapter` in `FilePrintHelper.kt` for Text, Markdown, Image, and Code files.
- [x] **Quick "Go to Line" in Text & Code Viewer (Loop 5)**: Direct line navigation dialog with smooth scroll and temporary line highlight flash.
- [x] **Loop 6 — Immersive Focus Mode**: Hide Android system bars alongside reader chrome and provide floating Back/exit-focus affordances.
- [x] **Loop 6 — Appearance Picker**: Persist Claude Glass, Nord Dark, Warm Parchment and 11–22 sp font-size preference for Markdown/code/text.
- [x] **Loop 6 — Outline / TOC**: Parse Markdown headings outside fences, extract styled DOCX headings, and offer PDF page-outline fallback.
- [x] **Loop 6 — Atomic Export Copy**: Use the existing atomic write/index refresh path and select a new filename on collisions.
- [x] **Loop 6 — DOCX zoom out**: Allow preview zoom down to 0.25×.
- [x] **Loop 7 — PDF Search Hardening**: Case/whitespace normalization, locale-stable matching, and guards for non-positive result limits; regression tests added.
- [x] **Loop 7 — Save As Safety**: Centralized safe filename selection and tests for source collisions, existing copies, path traversal, hidden files, and extensionless names.
- [x] **Loop 7 — Atomic Write Tests**: Test successful replacement, empty payload, missing parent, and write-exception cleanup/data preservation.
- [x] **Loop 7 — Outline Navigation**: Preserve existing Markdown ids and resolve DOCX heading jumps by normalized text before ordinal fallback.
- [x] **Loop 7 — Dedicated CI**: Added Android tests/build workflow with unit tests, coverage, lint, detekt, optional compile_applet, debug APK and test artifacts.
- [ ] **Loop 6/7 Quality Gate**: Await the dedicated GitHub Actions result; Android Gradle commands were not executed in the editing environment.

## P1
- [ ] **Write-back to source URI** for files opened from other apps (store `sourceUri` on `RecentFileEntity`, Room `Migration(3,4)`, `openOutputStream(uri, "wt")`).
- [ ] Don't copy 500 MB media imports: stream `content://` directly into ExoPlayer/Coil/PDF viewer.
- [ ] Paged text viewer on top of `TextWindowReader` (UI for files > 250k chars).
- [ ] Editor: line-number gutter shares scroll state with the text field; undo stack stores full text per keystroke; "Smart indent" strips indentation (destructive for Python/YAML/Makefile).
- [ ] Legacy Office: BIFF8 `.xls` reader first, then `.doc` text extraction.
- [ ] Root-level `GlassBackdrop` + NavHost transition policy; glass sheets/dialogs/search.
- [ ] Split `FilePreviewScreen.kt` (2,980 lines) into per-viewer files; type-safe navigation instead of shared singleton state.
- [ ] `ZipPreview`: show the `unsafe` warning icon; show archive-bomb refusals distinctly.
- [ ] Instrumented tests (Compose UI) for open → edit → save → reopen on each editable kind.

## P2
- [ ] RAR via a pure-Java reader (previous decision was to drop junrar; revisit).
- [ ] GLB/PLY/DXF viewers; shaded STL; DWG identify-only → metadata.
- [ ] CJK charset detection (GBK, Shift-JIS, Big5); currently non-UTF-8 falls back to windows-1252.
- [ ] Remove the dead pdf.js branch inside `WebViewPreview` and `canBeRenderedInWeb("pdf")`.
- [ ] Deduplicate `formatFileSize` / `formatElapsedTime` (4 copies) and migrate off deprecated `Icons.Default.ArrowBack/Undo`.
- [ ] Replace JitPack unscoped repo with scoped content filter.
- [ ] Benchmark on device: cold start, scroll jank (Macrobenchmark), 500 MB open.

## P3
- [ ] HEIC/AVIF fallbacks below API 31; animated WebP.
- [ ] Sample-file generator removed in 1.1.0; add an optional "demo files" screen if wanted.
- [ ] Localisation (all strings are hard-coded English).
