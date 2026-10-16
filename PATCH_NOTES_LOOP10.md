# File Claw — Loop 10 Patch Notes
## Search in the app header · keyboard-aware edit tray · reliable DOCX zoom · crisp PDF zoom

**Base:** `main` @ `1a637d1` (v1.3.0) · **Delivered:** patch ZIP, changed files only, repository-relative paths
**Install path (per request):** create branch `v1.3.1` from `main`, upload these files into it ("Add files via upload"), then run the existing **Build and Release Android App** workflow (workflow_dispatch) — it compiles and signs the APK. Gradle `versionName` intentionally stays `1.3.0`; set the workflow's `version_name` input to `1.3.1` so the test APK is distinguishable. No inconsistent version numbers are baked into the repo.

---

## 1. Root cause → fix summary

### Bug 1 — Find bar now lives in the header row
- **Root cause:** in-document search rendered as a *second*, floating `AnimatedVisibility` surface below the header; `goToNextMatch` / `goToPrevMatch` were scoped inside that overlay, so the header could not host them, and closing the overlay never hid the keyboard.
- **Fix:** while find mode is active, the app bar row itself is replaced by a new `ViewerFindBarHeader` (close control, focused single-line query field, live `current / total` badge — including `0 / 0` —, previous/next arrows, clear-query control). The filename and overflow menu stay hidden until search closes. The 149-line floating overlay was deleted. Navigation lambdas were hoisted to the screen scope so the header arrows and the IME Search action share exactly one implementation. Opening find from focus mode reveals the bars first; the field is focused once (no refocus per keystroke); closing hides the keyboard, restores the normal app bar and clears highlights/counts through each engine's existing empty-query path (PDF `searchPdf("")`, DOCX `searchWord("")`, TXT/MD `searchWord("")`, large-file engine reset). System **back now closes find mode first** (deliberate, Chrome-style). Prev/next are disabled while there are no navigable matches; truncated totals keep the `x/y of N` presentation.

### Bug 2 — Edit tray sits above the keyboard
- **Root cause:** the activity is edge-to-edge (`enableEdgeToEdge()`, `Scaffold(contentWindowInsets = 0)`) and the manifest declared **no** `windowSoftInputMode`, while the bottom tray wrapper applied only `navigationBarsPadding()`. IME insets were never applied to the tray, and the system could pan the window instead of dispatching insets at all.
- **Fix:** `MainActivity` now declares `android:windowSoftInputMode="adjustResize"` (the single inset-dispatch owner; no `adjustPan`). While inline editing, the tray pads with the **union** of navigation-bar and IME insets — `WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom)` — so it animates to a position directly above the keyboard without double-counting the gesture-nav area, and returns to its usual floating position when the keyboard hides. Reading mode and non-text viewers keep the exact previous bottom anchoring. Unsaved edits, caret and selection are untouched by keyboard toggles.

### Bug 3A — DOCX zoom genuinely changes the rendered document
- **Root cause (primary):** the pinch interceptor fed **every** pointer event into `calculateZoom()`, including the *transition* event where the second finger had just landed while the first still carried single-finger history. That event produces a wildly wrong span ratio, so `DocxZoomPolicy.applyPinch` slammed the zoom into the 25%/500% clamp on first contact — the percentage chip changed while the document jumped to an extreme size or barely moved.
- **Root cause (secondary):** `onPageFinished` re-applied a factory-time (stale) zoom/theme capture after load, and the post-render reapply skipped `currentZoom == 1.0`, leaving a small race where a re-render could visually reset the size. The pure-Kotlin fallback renderer never runs any zoom pipeline, yet dead zoom controls were still shown.
- **Fix:** new pure `PinchGestureGate` discards transition-event deltas (any pointer-count change: finger lands, lifts, 2→3) and forwards only deltas from a *confirmed, stable* two-pointer gesture; it disarms on single-pointer events and at gesture end. The zoom/theme values applied at page finish are read fresh via `rememberUpdatedState`, and `docx_editor.html` now reapplies `setZoom(currentZoom)` unconditionally after every render (idempotent, closes the 100%-flash race). Zoom-out / percentage chip / zoom-in are hidden while the fallback renderer is active (its text preview and search keep working). One authoritative zoom model is preserved: Compose-owned pinch → `docxZoomFactor` → JS `setZoom()`; native WebView zoom stays off; single-finger scroll, long-press selection, horizontal panning and the `@media print` zoom reset are untouched.

### Bug 3B — PDF text re-renders sharply after pinch
- **Root cause:** the HiDPI architecture was already correct (backing store = viewport × devicePixelRatio × absolute pinch scale, CSS size fixed), but (a) the fixed 9M-pixel budget + ratio cap 6 **undersampled high-density screens beyond ~2×** (dpr≈3 phone at 3× needs ~12.9M backing pixels, got ~5.7M) — visible blur exactly in the practical 2–3× range; (b) `setPdfRenderScale` re-rendered into the *same* canvas after resizing it, destroying the previously visible bitmap before the async render completed — the blank flash after every pinch; (c) one fixed budget ignored device capability; (d) pages whose render was in flight during a scale change kept stale sharpness.
- **Fix:** the per-page budget is now device-tiered from heap signals only (`ActivityManager.isLowRamDevice` → 6M; `memoryClass` <128 → 9M, 128–255 → 12M, ≥256 → 16M pixels, hard-clamped [4M, 24M]; `memoryClass` is used strictly as an app-heap cap, **never** as physical RAM) and pushed to the reader via a new `setPdfQualityBudget()` bridge right after the JS context is ready. `maxRenderRatio` rose 6 → 8. Re-renders draw into a fresh **offscreen canvas that is swapped in on completion** — the old bitmap stays visible until the sharper one is ready, eliminating the blank flash. Redundant re-renders at an already-achieved ratio are skipped, and an in-flight render performs one bounded follow-up if the achievable ratio grew mid-render. Failed/cancelled offscreen buffers are released immediately; offscreen pages are still released as before. Search highlight geometry stays percentage-based, so highlights remain zoom-aligned and the current match navigable.

---

## 2. File-by-file changed list (all paths inside the ZIP)

| # | Repository-relative path | Status | Purpose |
|---|--------------------------|--------|---------|
| 1 | `app/src/main/AndroidManifest.xml` | modified | `windowSoftInputMode="adjustResize"` on `MainActivity` (Bug 2) |
| 2 | `app/src/main/java/com/example/core/format/PinchGestureGate.kt` | **new** | Pure pinch-transition gate (Bug 3A) |
| 3 | `app/src/main/java/com/example/core/format/PdfRenderBudgetPolicy.kt` | **new** | Device-tiered PDF canvas budget (Bug 3B) |
| 4 | `app/src/main/java/com/example/ui/screens/DocumentPreviewSupport.kt` | modified | Pure `InViewerSearchNavigation` next/prev math (Bug 1) |
| 5 | `app/src/main/java/com/example/ui/screens/FilePreviewScreen.kt` | modified | Header find bar; overlay removed; IME-aware tray; fallback control hiding; pinch gate wiring; PDF budget push; fresh post-load zoom/theme |
| 6 | `app/src/main/assets/editor/docx_editor.html` | modified | Unconditional post-render `setZoom(currentZoom)` (Bug 3A) |
| 7 | `app/src/main/assets/pdfjs/search.html` | modified | `setPdfQualityBudget`; ratio cap 8; offscreen swap render; redundant-skip; bounded follow-up (Bug 3B) |
| 8 | `app/src/test/java/com/example/core/format/PinchGestureGateTest.kt` | **new** | 7 tests for the gesture gate |
| 9 | `app/src/test/java/com/example/core/format/PdfRenderBudgetPolicyTest.kt` | **new** | 8 tests for the budget policy |
| 10 | `app/src/test/java/com/example/ui/screens/InViewerSearchNavigationTest.kt` | **new** | 6 tests for search navigation math |
| 11 | `worklog.md` | **new** | Structured Loop 10 entry `LOOP10-SEARCH-IME-ZOOM` |
| 12 | `changelogs.md` | modified | Unreleased Loop 10 section in the mandatory format |
| 13 | `known-issues.md` | modified | Loop 10 watchlist |
| 14 | `docs/TODO.md` | modified | Loop 10 section (done vs pending) |
| 15 | `docs/prompts/NEXT_LOOP_PROMPT.md` | modified | Replacement Loop 11 prompt |
| 16 | `PATCH_NOTES_LOOP10.md` | **new** | This report |

No APK, no build output, no temporary scripts are included.

## 3. Test / build / CI report (honest labels)

| Check | Command / method | Outcome |
|-------|------------------|---------|
| Inline JS syntax (search.html, classic + module scripts) | `node --check` on extracted blocks | **PASS** |
| Inline JS syntax (docx_editor.html) | `node --check` on extracted block | **PASS** |
| Manifest well-formedness | XML parse | **PASS** |
| Kotlin structural balance (7 touched/new .kt files) | brace/paren balance scan | **PASS** |
| Kotlin compilation | `./gradlew :app:compileDebugKotlin` | **NOT RUN here** — no Android SDK/JDK in the patch environment; runs in GitHub Actions |
| Unit tests | `./gradlew :app:testDebugUnitTest` | **NOT RUN here** — expected baseline 188 static `@Test` methods (167 counted in this checkout before the loop + 21 added); never compare against the stale 134/146 figures |
| APK assembly | GitHub Actions `build-and-release.yml` | **PENDING** — trigger after uploading |
| Device smoke | see checklist below | **PENDING** — user device pass |

## 4. Device smoke checklist (to run after installing the Actions-built APK)

1. **Header find bar (PDF, DOCX, TXT, MD):** tap Find → the header row is replaced by the find bar (no floating bar below it) → type a common word → count badge updates → prev/next navigate real highlights → Enter (IME Search) advances → query with no matches shows `0 / 0` with arrows disabled → clear → close restores the filename + menu and removes highlights → system back closes find first → switching files resets search.
2. **Large TXT/MD path:** open a >10 MB text file, search a common word → "Searching full file…" appears, count matches truncation notice, next/previous open bounded excerpts at the right place.
3. **Edit tray above IME (TXT + MD):** tap Edit → focus the text → keyboard opens → the entire tray (undo/redo/indent/bold/italic/history/save) sits above the keyboard and is tappable; open/close repeatedly → typing at start/middle/end, scroll, save all work; keyboard hidden → tray returns to its normal floating position; reading-mode trays (PDF/DOCX/MD) stay bottom-anchored; test gesture nav and three-button nav if available.
4. **DOCX zoom:** open short + multi-page DOCX → opens at the persisted zoom (no 100% flash) → pinch in/out several times: the *document* visibly scales, not just the chip → toolbar ±/percentage dialog/double-tap 40%⇄100% → scroll/pan/long-press selection still work → print/export keeps original page size → if the fallback text view renders instead, zoom controls are absent.
5. **PDF sharpness:** text-heavy + photo-heavy PDFs; pinch to ~2× and ~3×, wait for the settle → glyphs sharpen without a blank flash → pinch back out and repeat → scroll pages in/out, search a match, toggle night mode, enter/exit focus mode, rotate → no overlay drift, no blank pages, no zoom reset → on a low-RAM device watch for renderer kills at high zoom.
6. **Regression sweep:** CSV, ZIP, image, audio/video, PPTX, HTML viewers still open and navigate; print/export and share still work from both the menu and the floating tray.

## 5. Known limitations (stated honestly)

- Sharpness at extreme zoom (>3×) remains bounded by the per-page budget by design; the bounded next step (viewport/tile rendering) is queued as the top Loop 11 backlog item rather than removing caps and risking OOM.
- The pinch gate skips exactly one event per finger-count change; this should be imperceptible but is listed on the Loop 10 watchlist for hardware confirmation.
- All Android compile/test/device results in this document are labeled **pending** until the Actions build and your device pass complete; nothing above claims device-verified behavior.
