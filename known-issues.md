# File Claw - Known Issues & Tech Debt
Last Updated: October 15, 2026
Document Version: 1.1
Next Review Date: January 15, 2027

## 1. Issue Classification System
- **Severity**: `CRITICAL` (Crashes/Data Loss), `HIGH` (Core Feature Broken), `MEDIUM` (Degraded Experience), `LOW` (Cosmetic).
- **Status**: `OPEN`, `IN_PROGRESS`, `CLOSED`, `BY_DESIGN`.
- **Area**: `UI`, `FILE_IO`, `PDF_ENGINE`, `DATABASE`, `PERFORMANCE`.

## 2. Active Issues (CRITICAL and HIGH)

| ID | Title | Severity | Status | Area | Description & Workaround |
|----|-------|----------|--------|------|--------------------------|
| BUG-001 | Large Text File UI Freeze | HIGH | OPEN | `FILE_IO` | Files > 10MB cause ANRs due to full read into memory on main thread string assignment. *Workaround*: App limits rendering blocks, but true streaming is missing. |
| BUG-002 | Search Indexing Latency | HIGH| OPEN | `PERFORMANCE`| Searching entire external storage triggers massive GC churn. |

## 3. Active Issues (MEDIUM and LOW)

| ID | Title | Severity | Status | Area | Description & Workaround |
|----|-------|----------|--------|------|--------------------------|
| BUG-003 | PDF Zoom Reset on Rotation | MEDIUM| OPEN| `UI`| Activity recreation loses `pdfScale` bounds. |
| BUG-004 | CSV Table Horizontal Scroll | LOW| OPEN| `UI`| Extreme widths cause minor clipping on strict bounds. |
| BUG-005 | Password Field Obscuring | LOW| OPEN| `UI`| Keyboard overlaps the unlock dialog on small screens. |

### Loop 8 watchlist
- **PDF crisp-zoom memory**: the per-page 9M-pixel budget (~36MB ARGB) bounds memory, but very long sessions at 5× zoom on low-RAM devices may still pressure the heap; `releaseRecord` frees offscreen pages (monitor via `dumpsys meminfo`).
- **PDF smart-dark classification threshold**: the 4% saturated-pixel cutoff is a heuristic; pages that mix large photos WITH body text invert the photos only if the text dominates. Tunable in `classifyPage()` (`assets/pdfjs/search.html`).
- **DOCX pinch vs text selection**: 2-finger gestures are consumed at the Initial pass; long-press selection inside the rendered DOCX is unaffected, but exotic WebView widgets may need per-case validation.
- **Search ranking is name-only**: global search still matches file NAMES, not contents; content search is planned (see `docs/prompts/NEXT_LOOP_PROMPT.md`).

## 4. Recently Fixed Issues
- **Light-mode status bar icons invisible (user-reported, Loop 7)**: `enableEdgeToEdge()` set icon appearance from the system dark-mode flag once, while the canvas followed the in-app theme. *Fix*: `FilesClawTheme` now re-asserts `isAppearanceLightStatusBars/NavigationBars` on the host window whenever the in-app theme resolves (`ui/theme/Theme.kt`).
- **Preview top bar duplicated the bottom tray (user-reported, Loop 7)**: Search/Outline/Focus/Copy icons were stacked in the top bar. *Fix*: top bar reduced to back + file name + 3-dot menu.
- **Document Outline / Viewer Appearance menu items dead (user-reported, Loop 7)**: flags were set but the bottom sheets were never rendered. *Fix*: both sheets are now wired (outline jump + theme/font/zoom appearance).
- **In-document search appeared broken in MD/TXT (user-reported, Loop 7)**: pipeline was complete; the search field never received focus so users could not type. *Fix*: auto-focus + keyboard raise on open.
- **TextFileSearchEngine overlap double-count**: cross-chunk candidates starting inside a previously counted match were double-counted. *Fix*: byte-range guard + regression coverage.
- **Test suite did not compile**: pre-existing type error in `TextFileSearchEngineTest` (String vs ByteArray). *Fix*: corrected; suite is 134/134 green as of Loop 7.
- **PDF Password Crash**: `PdfRenderer` previously crashed wildly on encrypted files. *Fix*: Implemented `PDFBox-Android` as a robust fallback to decrypt safely into cache. Verified via local tests.
- **PDF zoom blurry at high scale (user-reported, Loop 8)**: pages were rasterized once at ≤2 device pixels per CSS pixel and pinch merely magnified the bitmap. *Fix*: `onScaleChanged` debounce feeds the settled scale to `setPdfRenderScale()` which re-rasterizes visible pages under a ~9M-pixel/page budget (`assets/pdfjs/search.html`, `FilePreviewScreen.kt`).
- **PDF dark mode turned photos ghost-like (user-reported, Loop 8)**: whole-canvas `invert(.88)` inverted faces into negatives. *Fix*: per-page classification (48px downsample, >4% saturated pixels ⇒ `photo` class) keeps photo pages natural with a slight dim; text pages get a full luminance inversion.
- **Focus-mode transition snapped/flashy (user-reported, Loop 8)**: content padding flipped binary at frame 0, status-bar inset flipped instantly, and PDF pages re-rendered (blank flash) per resize frame. *Fix*: scaffold insets always applied (animate frame-by-frame), unconditional `statusBarsPadding()`, matched 220–260ms tween specs, debounced JS resize + skip-if-unchanged geometry.
- **Global search listed deleted files (Loop 8)**: results are existence-filtered after relevance ranking (`FileSearchRanker`).
- **Search-term history corrupted by `|*|` in queries (Loop 8)**: storage migrated to a JSON array with legacy-format fallback read.

## 5. Technical Debt Register
- **Architecture Debt**: `MainViewModel` is a God Class. It manages Home, Preview, Editor, and Navigation. MUST be split into scoped viewmodels. *(Loop 7 note: inline edit state was added minimally; a full split is planned behind the MD/TXT loops.)*
- **Testing Debt**: Automated test coverage was 0% historically; Loop 7 restored a green 134-test suite and added the first UI-flow helper tests. Keep expanding (target: 80% core coverage via `jacocoCoreVerify`).
- **I/O Debt**: `FileManager` utilizes older `java.io.File` APIs synchronously in many places instead of non-blocking `NIO` or strict Coroutine stream channels.
- **String Debt**: Hardcoded UI strings exist across all Compose screens.
- **Build Debt**: `docs/TODO.md` notes `gradlew` is tracked without the executable bit in some checkouts; run `chmod +x gradlew` after cloning (Loop 7 verified builds need this).

## 6. Platform Limitations
- **Android Scoped Storage**: Strict file limits prevent raw `java.io.File` modifications in standard `Documents` or `Downloads` directories on API 30+ without `MANAGE_EXTERNAL_STORAGE`.
- **Compose Lazy Lists**: Rapidly scrolling thousands of heavy items with complex thumbnail logic can drop frames on lower-end devices.

## 7. Third-Party Limitations
- **PDFBox-Android**: Extremely heavy dependency (~5MB). Updates are incredibly slow/orphan state, but required because native Android `PdfRenderer` explicitly lacks password support.

## 8. Won't Fix / By Design
- **DOCX Formatting Loss**: Custom parser strictly isolates text. Retaining word-perfect structural formatting (images, fonts, alignments) is `BY_DESIGN` ignored to keep the app lightweight and avoid bloated Apache POI imports.
- **No Cloud Sync**: App is marketed and designed as purely offline file manager. Network APIs are excluded intentionally.

## 9. Issue Triage Rules
- Users/QA submit items mapped to the BUG format.
- Maintainer assigns Severity.
- `CRITICAL` forces a hotfix branch. `HIGH` enters the next sprint.

## 10. Metrics and Trends
- Currently tracking high error rates on nested ZIP files (Unimplemented).
- Memory optimization is the highest trending issue area based on internal benchmarks.

---
**Update Log**
- 2026-06-15: v1.0 - Initial document creation.
