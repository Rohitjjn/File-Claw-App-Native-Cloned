# Backlog and known issues

Priorities: **P0** data loss / crash / security · **P1** high-impact feature or correctness · **P2** quality · **P3** polish.

## KNOWN ISSUES (read first)
1. **Loop 7 (Markdown/TXT) is compiled and unit-tested in CI, but not run on a device.** The authoring sandbox
   still has no Java/Android SDK, so verification moved to GitHub Actions: `.github/workflows/android-ci.yml`
   runs on every push to `arena/**` and on PRs to `main` (jsdom tests for `reader.js`, `testDebugUnitTest`, a
   Kotlin→jsdom Markdown/TXT save round trip, `assembleDebug` and `assembleRelease`). Results for this loop are
   in `docs/CHANGELOG.md`. CI cannot exercise typing/IME inside the WebView editor, keyboard insets, CSS zoom
   with selection handles or printing: run `docs/SANITY_CHECKLIST.md` → "Markdown / TXT reader and editor (Loop 7)".
   Loop 6 Android code compiled as part of the same build; its device behaviour is still unverified.
2. Gradle wiring for JaCoCo (`jacocoCoreReport` / `jacocoCoreVerify`), `enableUnitTestCoverage`, detekt, and Room schema export is
   unverified. detekt is `ignoreFailures = true` until its first green run.
3. Edits to files opened from another app are saved to the in-app copy only (the user is told). Write-back is P1 below.
4. WebView renderers (DOCX, PPTX, grid) still receive whole files as base64 (now capped at 25 MB, then falls back to the archive tree).
5. Hex viewer shows only the first 4 KB. DOCX preview derives pages from `docx-preview` sections, but page-count accuracy still needs device verification; legacy `.doc` remains text-extraction-only.
6. 3D viewer draws a wireframe sample (≤ 25k triangles); no shading, no GLB/PLY/DWG.
7. Legacy `.doc` / `.xls` / `.ppt` and `.rar` are identified but not openable.
8. Bundled JS libraries (docx-preview, pptx-preview, x-spreadsheet) have no license text in the shipped files; verify upstream licenses (JSZip is MIT/GPL dual, confirmed from its header).
9. `applicationId` is still the AI-Studio-generated `com.aistudio.filesclaw.apxqzy`; JitPack repository is unscoped (supply-chain exposure).

## P0
- [x] Earlier project baseline Quality Gate was recorded green (`compile_applet` / `testDebugUnitTest`); **Loop 6 was not rerun** because Java/Android SDK are unavailable here (see KNOWN ISSUES #1).
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

## Loop 6 — PDF, DOCX, TXT, Markdown reader improvements (implementation pending Android verification)
- [x] PDF.js local reader with whole-document text search/highlights, page navigation, password handling, dark reader canvas, and native PrintManager PDF export; PDF.js 4.10.38 assets and notices are bundled offline. Removed the obsolete base64 `readBytes()` PDF path from generic `WebViewPreview`.
- [x] DOCX whole-rendered-document search/highlights, dark mode, 25%–500% zoom with 100% default, slider and exact integer-percent entry/reset dialog, rendered-WebView print with extraction fallback.
- [x] TXT/Markdown full-content search for files within the existing preview cap; for truncated files, streaming full-file search with all-hit counts, first 10,000 stored match offsets, and bounded excerpts on next/previous. Preview/print/copy avoid unbounded file reads; print/copy cap is 2,000,000 characters.
- [x] TXT and Markdown fenced-code copy buttons; parser supports matching backtick and tilde fence types. Added parser, HTML-builder, and streaming-search unit tests, but they were not run in this environment.
- [x] Focus mode keeps reader canvas under system bars and uses the effective reader theme for system-bar icon contrast; search overlay hides while controls are hidden.
- [ ] Android compilation, unit tests, device/WebView behavior, actual PrintManager preview/export, DOCX zoom, and immersive-mode restoration: run and record in a Java/Android SDK-enabled environment.

## Loop 7 — Markdown & TXT (done in CI; device verification pending)
- [x] Status/navigation bar icons follow the app theme (Light app on a dark-mode phone no longer gets white icons on white).
- [x] Markdown/TXT top bar: back, file name, 3-dot only; Focus mode in the 3-dot menu.
- [x] Document outline, Viewer appearance and Go to line actually work for Markdown/TXT; search bar is visible (drawn above the WebView), auto-focused, Enter = next.
- [x] In-place editing on the preview screen (rich Markdown with source-preserving save, plain TXT), edit tray (Undo/Redo/Tab/Bold/Italic/Save/History), X discard confirmation, save confirmation with change summary, edit tools menu, edit history sheet, draft survives recreation.
- [x] Viewer appearance: zoom 25–500 %, page themes, typeface, size, spacing, margins, width, justify, wrapping; live and persisted.
- [ ] **Loop 8:** fix what the user reports after testing Loop 7, then the brainstormed improvements in `docs/prompts/LOOP_08_PROMPT.md`.
- [ ] Device checks in `docs/SANITY_CHECKLIST.md` (IME/Gboard composition, execCommand output, keyboard insets on API 26–29, zoom + selection, print of the rendered theme).
- [ ] Unsaved edits are lost on **process death** (draft is in memory only); persist drafts to disk with a recovery banner (Loop 8 prompt).
- [ ] Saving does not detect that the file changed on disk since it was opened.
- [ ] `saveTextFile` still writes a hidden `.name.bak` next to the user's file; move backups to app storage.
- [ ] Undo history resets when switching rich ↔ Markdown source mode.
- [ ] Relative images (`![](img.png)`) and remote images don't load in the reader (no base URL / no INTERNET); decide on a guarded asset loader.
- [ ] Bottom-tray icons are 36 dp (below the 48 dp touch-target guideline).
- [ ] Document outline / Viewer appearance menu items still do nothing for PDF/DOCX and other text types (out of Loop 7 scope).

## P1
- [ ] **Write-back to source URI** for files opened from other apps (store `sourceUri` on `RecentFileEntity`, Room `Migration(3,4)`, `openOutputStream(uri, "wt")`).
- [ ] Don't copy 500 MB media imports: stream `content://` directly into ExoPlayer/Coil/PDF viewer.
- [ ] **Loop 7: continuous paged TXT/Markdown viewer** on `TextWindowReader` for files > 250k characters. Loop 6 adds streaming full-file search and bounded excerpts at selected hits, but not continuous scrolling or Markdown block-context preservation across windows.
- [ ] Editor screen (still used for JSON/XML/code and other text types; Markdown/TXT now edit in place): line-number gutter shares scroll state with the text field; undo stack stores full text per keystroke; "Smart indent" strips indentation (destructive for Python/YAML/Makefile).
- [ ] Legacy Office: BIFF8 `.xls` reader first, then `.doc` text extraction.
- [ ] Root-level `GlassBackdrop` + NavHost transition policy; glass sheets/dialogs/search.
- [ ] Split `FilePreviewScreen.kt` (2,980 lines) into per-viewer files; type-safe navigation instead of shared singleton state.
- [ ] `ZipPreview`: show the `unsafe` warning icon; show archive-bomb refusals distinctly.
- [ ] Instrumented tests (Compose UI) for open → edit → save → reopen on each editable kind.
- [ ] Loop 7 device/WebView tests for PDF.js module asset MIME/worker loading, PDF search geometry/password/print, DOCX cross-run search/zoom/dark/print, and focus-mode window restoration.
- [ ] Extend TXT/Markdown large-file paging beyond bounded search excerpts; test Markdown syntax/block context across page boundaries and navigation beyond the 10,000 stored search offsets.

## P2
- [ ] RAR via a pure-Java reader (previous decision was to drop junrar; revisit).
- [ ] GLB/PLY/DXF viewers; shaded STL; DWG identify-only → metadata.
- [ ] CJK charset detection (GBK, Shift-JIS, Big5); currently non-UTF-8 falls back to windows-1252.
- [ ] Deduplicate `formatFileSize` / `formatElapsedTime` (4 copies) and migrate off deprecated `Icons.Default.ArrowBack/Undo`.
- [ ] Replace JitPack unscoped repo with scoped content filter.
- [ ] Benchmark on device: cold start, scroll jank (Macrobenchmark), 500 MB open.

## P3
- [ ] HEIC/AVIF fallbacks below API 31; animated WebP.
- [ ] Sample-file generator removed in 1.1.0; add an optional "demo files" screen if wanted.
- [ ] Localisation (all strings are hard-coded English).
