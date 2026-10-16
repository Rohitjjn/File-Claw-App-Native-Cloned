# File Claw — Loop 12 Patch Notes (v1.3.2)

**Base:** `main` @ `67fd7c2` (tag `v10.0.4` — the Loop 10 patch merged via PR #26)
**Recommended branch:** `v1.3.2` (create from `main`), upload this ZIP's contents, then run **Build and Release Android App** with `version_name = 1.3.2`.
**Scope:** Bug A (DOCX zoom), Bug B (search arrows stop early), Bug C (heavy PDFs / large pages), Bug D (dark-mode photo negatives), Bug E (evidence-based performance). DOCX viewer chrome untouched. 100% offline — no new dependencies.

---

## 1. Root causes and fixes

### Bug B — "7 results, arrows reach only 2–3" (PROVEN, deterministic)
- **Observed:** counter shows N results; next arrow never moves past 2; previous wraps to last and back to 2.
- **Root cause (code-path proof):** the DOCX and TXT/MD WebView effects issued ONE combined script per state change: `searchWord(q); nextSearchMatch();`. `searchWord()` rebuilds the highlight list and resets the renderer's active index to 0 (calling `highlightMatch(0)`), so every arrow press restarted from 0 and advanced exactly one step → index 1 → display "2/N" forever. The match list itself was never truncated — the commands were wrong.
- **Fix:** new pure `InViewerSearchCommandPolicy` (query searched exactly once; arrow presses issue only `nextSearchMatch()`/`prevSearchMatch()` deltas; trigger-counter resets absorbed). Wired into both `DocxPreviewWebView` and `SearchableDocumentWebView` (TXT/MD + DOCX fallback). PDF path audited: Kotlin-owned index + wrap-around + `showPdfSearchMatch` modulo were already correct.
- **Proof:** 7 unit tests incl. a simulation of 7 next presses asserting zero re-searches and exactly one step each — all 7 matches reachable both directions.

### Bug A — DOCX zoom unreliable on large documents
- **Observed:** pinch chip moves but the page lags seconds behind, then jumps; worst on big documents.
- **Root cause (code-path + web.dev/MDN evidence):** every gated pinch delta (≈60/s) ran `evaluateJavascript("setZoom()")`, each applying CSS `zoom` — a **layout** property — forcing Chromium's style→layout→paint over the entire rendered document per frame; evals queued faster than the renderer could lay out. (docx-preview has no zoom support — upstream wontfix docxjs#109 — so the host must own it.)
- **Fix (the pdf.js-viewer pattern):** two-phase zoom. During the pinch Android sends `setZoomDisplay()` → compositor-only `transform: scale()` preview (no layout), throttled to ~30 dispatches/s with a new `pinchActive` state + `onPinchSettled` callback from the gesture loop. On settle, `setZoom()` commits CSS `zoom` once, resyncs the horizontal scroll area and restores the relative scroll offset (260ms in-page backstop if settle is missed). Toolbar ±, percent dialog and page load commit immediately.
- Kept from Loop 10: `PinchGestureGate`, `DocxZoomPolicy` clamps, replay-after-render.

### Bug C — heavy PDFs and very large pages
- **Observed:** 100+ page PDFs slow to open, memory balloons, some oversized pages blank.
- **Root causes (code-read; mechanisms confirmed against the official pdf.js viewer sources):**
  1. `prepareDocument()` awaited one sequential `getPage()` per page before the document was reported loaded — O(pages) at open — and retained every `PDFPageProxy` forever.
  2. The per-page pixel budget ignored that 3–5 canvases are resident inside the 900px prefetch window (per-page 12–16M px ⇒ 144–256MB of live RGBA across residents).
  3. `desiredPixelRatio()` had a floor of 1.0 and no per-dimension cap: a page whose 1× area already exceeded the budget allocated an oversized canvas — beyond Chromium's canvas limits the browser **silently ignores every draw command** → permanently blank page (documented pdf.js #11195-class platform behavior; Android Chrome area limits measured 11k–16k px per side in the canvas-size test matrix).
  4. Search indexing kept every page's full text + piece geometry for the viewer's lifetime (duplicate of the document).
  5. A failed render left a blank page until the user scrolled far away and back; renderer-process death was unhandled.
- **Fixes (official-viewer mechanisms, re-implemented for this reader):** lazy open (page 1 only + sized placeholders — same tradeoff the official viewer makes), `ensurePage()` on approach with a render race guard, `releaseRecord()` now calls `page.cleanup()` and drops the proxy, resident-window LRU (max 3 canvases, non-visible LRU evicted), `setPdfQualityBudget` reinterpreted as **TOTAL RGBA budget** (`PdfRenderBudgetPolicy.totalCanvasBytes` = per-page tier × 3 × 4; Loop 10 tiers still hold per page), new `PdfCanvasDimensionPolicy` (area + 8192px dimension cap, 0.05 floor → oversized pages downsample, never blank; 9 tests incl. a sweep), 128-page text-index LRU, per-page "could not be rendered — Retry" affordance, renderer-death recovery (PDF: error state + **Try Again** rebuild; DOCX: text fallback), preview WebViews destroyed on composition release (`onRelease`).

### Bug D — dark mode makes human photos look like negatives
- **Observed:** portraits — especially grayscale/low-saturation — became film negatives in dark mode.
- **Root cause:** whole-canvas `invert(1) hue-rotate(180deg)` on every page, guarded only by a >4% saturation-pixel `.photo` heuristic. A CSS filter on the single page canvas cannot selectively invert text while preserving an embedded image, and saturation says nothing about grayscale photos (same limitation discussed in pdf.js #11075/#8253; separating text/vector from raster layers is not safely possible in this architecture without per-page operator-list parsing).
- **Fix (safety over aggressive inversion):** pages are classified after every render; **only confidently near-binary text pages invert** (≤1.5% colorful pixels, ≤12% continuous-tone pixels, ≥55% light pixels — such a page cannot contain a photograph by construction). Everything else — color photos, grayscale/low-saturation portraits, mixed pages, charts, scans — keeps original colors, slightly dimmed (`brightness(.9)`). Thresholds live in pure `PdfDarkModePolicy` (14 tests) mirrored in the reader. Unclassified pages default to natural.
- **Tradeoff (documented in known-issues.md):** mixed text+photo pages stay light instead of inverting. The failure direction is now "a page stays brighter than ideal", never "a portrait gets ghosted".

### Bug E — evidence-backed performance fixes
| Change | Evidence |
|---|---|
| DOCX pinch eval flood → throttle + two-phase commit | per-event eval path (Bug A above) |
| Search re-highlight per arrow press → deltas only | full TreeWalker + DOM rewrite ran on every press (Bug B above) |
| DOCX base64 bridge (~5 file copies at peak: Kotlin String + bridge String + JS UTF-16 string + atob binary string + Uint8Array) → streamed ArrayBuffer via WebViewAssetLoader `/docx-src/document.docx` (bridge path kept as in-page fallback) | code-path arithmetic for a 20MB file ≈ 130MB+ transient |
| Eager all-pages `getPage()` → lazy placeholders | O(pages) open loop |
| Page proxies + text never freed → cleanup/drop + LRU caps | retention paths listed in Bug C |
| Stale WebView instances → `onRelease { destroy() }` | prompt Bug E item; keyed recreation replaced views without destroying them |

Not done (would be speculative): TXT/MD `setViewerZoom` relayout cost (rare user action), PPTX WebView `onRelease` (no keyed recreation), BUG-001/BUG-002 (not implicated by this loop's evidence).

---

## 2. Research notes (GitHub sources, verified this loop)

- **pdf.js official viewer** (`web/pdf_viewer.js`, `pdf_rendering_queue.js`, `pdf_page_view.js`, `app_options.js`): LRU `PDFPageViewBuffer` sized `max(10, 2×visible+1)`; renders visible pages + 1 ahead; evicted views destroyed with `pdfPage.cleanup()`; document-level `cleanup()` 30s idle; mobile `maxCanvasPixels` = 5MP with a 32767 per-dimension cap and a CSS-zoom + detail-canvas fallback. File Claw adopts the buffer/cleanup/placeholder pattern sized for its own budgets.
- **Canvas limits** (canvas-size test matrix, MDN, SO): exceeding browser canvas dimensions silently blanks the canvas — the direct cause of the "very large pages" bug; our 8192px cap is deliberately conservative.
- **AndroidDocViewer** (seapeak233, **Apache-2.0**): DOCX zoom is just WebView native pinch (`setSupportZoom(true)`, no JS pipeline) — nothing to port; File Claw's pipeline is more capable and stays.
- **docxjs / docx-preview** (VolodymyrBaydalka, **Apache-2.0**, latest 0.4.1): no zoom support, maintainer wontfix #109 ("resize/scale … is out of scope of this library"). Bundled version kept this loop — a renderer/versions migration was not provable offline here.
- All PDF.js/docxjs/JSZip assets remain the pinned offline bundles; **no new dependency, no license file changes needed** (patterns only, no code copied).

---

## 3. Changed files (14 — this is exactly what the ZIP contains)

| # | File | Status | Why |
|---|------|--------|-----|
| 1 | `app/src/main/assets/pdfjs/search.html` | modified | lazy pages, residency, dimension caps, retry, dark classifier, text LRU |
| 2 | `app/src/main/assets/editor/docx_editor.html` | modified | two-phase zoom, streamed bytes, print guard |
| 3 | `app/src/main/java/com/example/ui/screens/FilePreviewScreen.kt` | modified | search policy wiring, pinchActive/settle, PDF budget bytes, crash recovery, onRelease |
| 4 | `app/src/main/java/com/example/ui/screens/DocumentPreviewSupport.kt` | modified | `InViewerSearchCommandPolicy` (new object) |
| 5 | `app/src/main/java/com/example/core/format/PdfRenderBudgetPolicy.kt` | modified | `totalCanvasBytes`, `RESIDENT_PAGE_WINDOW`, `BYTES_PER_PIXEL` |
| 6 | `app/src/main/java/com/example/core/format/PdfCanvasDimensionPolicy.kt` | **new** | area + dimension caps |
| 7 | `app/src/main/java/com/example/core/format/PdfDarkModePolicy.kt` | **new** | tone classification policy |
| 8 | `app/src/test/java/com/example/ui/screens/InViewerSearchCommandPolicyTest.kt` | **new** | 7 tests |
| 9 | `app/src/test/java/com/example/core/format/PdfCanvasDimensionPolicyTest.kt` | **new** | 9 tests |
| 10 | `app/src/test/java/com/example/core/format/PdfDarkModePolicyTest.kt` | **new** | 14 tests |
| 11 | `app/src/test/java/com/example/core/format/PdfRenderBudgetPolicyTest.kt` | modified | +2 residency tests |
| 12 | `changelogs.md` | modified | unreleased v1.3.2 section; v1.3.1 marked shipped (PR #26 / v10.0.4); sections renumbered |
| 13 | `known-issues.md` | modified | BUG-006/007/008, Loop 12 watchlist, fixed list, by-design tradeoffs |
| 14 | `docs/TODO.md` | modified | Loop 12 section |
| 15 | `docs/prompts/NEXT_LOOP_PROMPT.md` | rewritten | Loop 13 prompt |
| 16 | `worklog.md` | modified | `LOOP12-DOCX-SEARCH-PDF-DARK-PERF` entry |

## 4. Quality gate — honest results

| Check | Command | Result |
|---|---|---|
| JS syntax (search.html classic script) | `node --check` on extracted script | **PASS** |
| JS syntax (search.html module script) | `node --check` on extracted `.mjs` | **PASS** |
| JS syntax (docx_editor.html script) | `node --check` on extracted script | **PASS** |
| HTML structure (script/style/html balance) | static scan | **PASS** |
| Kotlin brace/paren balance (9 touched/new files) | static scan vs pristine baseline | **PASS** (FilePreviewScreen +3/+4 pre-exists in the pristine file — unchanged) |
| `:app:compileDebugKotlin` | Gradle | **NOT RUN** — no Android SDK/JDK in this environment; delegated to GitHub Actions |
| `:app:testDebugUnitTest` (197 expected: 167 baseline + 30 new) | Gradle | **NOT RUN** — same |
| `:app:assembleDebug` | Gradle | **NOT RUN** — same |
| Device behaviors | — | **NOT RUN** — your device test is the gate |

## 5. Device smoke checklist (after installing the CI APK)

1. **Search**: 7-match DOCX → next walks 2,3,…,7 and wraps to 1; previous walks backwards; repeat in TXT, MD, PDF. Type a new query mid-navigation → exactly one re-search.
2. **Large PDF**: open a 200+ page file (should open fast), scroll to ~200 and back, zoom 2–3×, search; `adb shell dumpsys meminfo <pkg>` before/after should show bounded growth; no blank pages; revisit a far page twice.
3. **Huge page**: any poster/blueprint PDF must render (downsampled), never blank.
4. **Dark mode**: color portrait, grayscale portrait, mixed page, chart, B/W scan, plain text page → portraits natural, text pages dark.
5. **DOCX**: pinch a large DOCX (smooth, settles crisp, chip live), toolbar ±/percent instant, double-tap toggle, >100% horizontal scroll, open a DOCX from inside a zip.
6. **Recovery**: if the PDF viewer ever errors → Try Again rebuilds it.

## 6. How to ship

1. `main` → create branch **`v1.3.2`**.
2. Upload the ZIP contents (**Add files via upload**) — paths are already repo-relative.
3. Run **Build and Release Android App** with `version_name = 1.3.2`.
4. Test the APK with the checklist above; report results for Loop 13 (`docs/prompts/NEXT_LOOP_PROMPT.md` is already written).

## 7. Ranked next-loop ideas (only after this build verifies on device)

1. Bounded tile/detail rendering at extreme PDF zoom (pdf.js `enableDetailCanvas` reference design).
2. Match-count parity policy across the four search pipelines (honest truncation labels).
3. Content search in global search (rank files by match count, bounded memory).
4. HTML preview search parity (BUG-008).
5. DOCX outline sheet via the existing page observer.
