# File Claw — NEXT LOOP PROMPT (Loop 9)

> **Scope:** v1.3.0 device-testing round — search polish, PDF reader (crisp zoom + smart dark + focus transition), DOCX zoom. User will test the v1.3.0 patch on a real device and report bugs; fix those first, then push the improvement backlog below.
> **Golden rule:** 100% offline. No new network code, no cloud calls, no heavy new dependencies without measuring APK impact.

---

## PART 0 — Working protocol (senior agents team)

1. Clone/refresh the repo, and read in order: `worklog.md` (if present), `changelogs.md`, `known-issues.md`, `docs/TODO.md`, `rules.md`, `tech-specs.md`.
2. Run the audit team FIRST (research-only sub-agents, one per area):
   - Agent A: search sweep — global `SearchScreen.kt` + `MainViewModel.searchLocalFiles` + in-viewer overlay + WebView pipelines (`DocumentPreviewSupport.kt`, `assets/pdfjs/search.html`, `assets/editor/docx_editor.html`).
   - Agent B: PDF pipeline — `assets/pdfjs/search.html` (zoom budget, `classifyPage`, resize debounce), `FilePreviewScreen.kt::PdfPreview` (`onScaleChanged`, night mode), memory behavior.
   - Agent C: DOCX + focus transitions — `DocxPreviewWebView` (pinch interceptor, double-tap, persisted zoom), focus-mode AnimatedVisibility specs, fallback path.
   Log findings to `worklog.md` (append-only, Task ID + template).
3. Implement, run the quality gate (PART 4), deliver a **patch zip with only updated files** (repo-relative paths, upload-ready to GitHub). APK compile happens via GitHub Actions — do NOT ship an APK.

## PART 1 — User-reported bug fixes (fill after device testing of v1.3.0)

- [ ] BUG-USER-1: ______ (report verbatim → root-cause → fix → regression note)
- [ ] BUG-USER-2: ______
- [ ] BUG-USER-3: ______

Known watch items from Loop 8 (verify on device, fix if confirmed):
- PDF crisp zoom memory on low-RAM devices at 5× (per-page ~36MB budget; watch `dumpsys meminfo`).
- PDF smart-dark 4% saturated-pixel threshold: pages mixing large photos AND text; scanned photo pages that should stay natural but invert.
- DOCX pinch: any gesture conflict with text selection / horizontal scroll >100%.
- Focus transition on very large PDFs (debounced resize still re-renders once — should not flash).
- Global search ranking feels wrong for any real-world query (tune `FileSearchRanker` buckets).

## PART 2 — Feature upgrades (priority order)

### 2.1 Search round 2
- Match-case (Aa) + whole-word toggles in the in-viewer search overlay; thread into `searchWord()` JS (`DocumentPreviewSupport.kt`), pdf.js regex flags (`search.html`), and `TextFileSearchEngine` flags.
- Wrap-around cue: when next/prev wraps modulo, flash the counter badge (no silent jump).
- Match list strip / bottom sheet for PDF (up to 2000 stored matches exist in JS; surface page numbers + jump).
- Deep-link: global search result → open file with `initialSearchQuery` prefilled in the in-viewer overlay.
- Global search content mode (P2, large): reuse `TextFileSearchEngine` for "search inside text files" with snippets; strictly behind a toggle.

### 2.2 PDF round 2
- Zoom presets chip row (Fit / 100% / 200%) wired to the same `setPdfRenderScale` pipeline.
- Battery/perf: cap re-render at 3× zoom on devices with <4GB RAM (read `ActivityManager.memoryClass`).
- Night-mode toggle should NOT re-render pages (verify); add a subtle crossfade when filter applies.

### 2.3 DOCX round 2
- Hide zoom controls when `pureKotlinFallback` is active (chip currently moves but does nothing visible).
- Double-tap toggle animation (animate `docxZoomFactor` with a 180ms tween instead of instant jump).
- Verify print output ignores CSS zoom (`@media print` reset) after all zoom changes.

### 2.4 Cross-cutting polish
- Focus mode: verify search overlay stays reachable in focus mode on phones (currently gated on `isBarsVisible` — decide intended UX and document).
- Highlight all 4 search pipelines consistently (MD/TXT/PDF/DOCX) with the terracotta palette; WebViewPreview (`findAllAsync`) fallback still amber — restyle or document.

## PART 3 — Repo hygiene (loop 8 carried over)

- The GitHub repo root still contains 40+ old one-off scripts (`fix_*.js`, `patch_*.js`, `debug_braces*.js`, `check_*.js`, `find_*.js`, `remove_fling*.js`, `track_braces*.js`, `restore_previews.js`, `revert.js`, `update_docx.js`, `update_pdf.py`, `fix.sh`, `build.log`, `backup/`). They are NOT in the app build. Delete them in the GitHub UI (or a cleanup commit) — see the chat deletion list from Loop 8.
- `gradlew` needs the executable bit after any fresh clone (`chmod +x gradlew`) — documented in `docs/TODO.md`.

## PART 4 — Quality gate (MUST pass before delivery)

```bash
export ANDROID_HOME=<sdk-path>
export JAVA_HOME=<jdk-17-with-jlink>   # JRE is NOT enough: javac/jlink required
./gradlew :app:compileDebugKotlin --offline --daemon
./gradlew :app:testDebugUnitTest  --offline --daemon   # 146 tests, 0 failures (v1.3.0 baseline)
```
- Unit tests for every new pure helper (`FileSearchRanker`, `DocxZoomPolicy` pattern).
- JS changes: syntax-check every edited `<script>` with `node --check` (Loop 8 pattern).
- Manual smoke checklist: search screen (ranked results + highlight + counts + clear-all) → in-viewer search (badge, arrows, Enter) → PDF (pinch → crisp re-render, dark mode on photo page vs text page, focus transition) → DOCX (opens 40%, pinch, double-tap toggle, slider has 40% stop, night mode, print) → all other formats unchanged.
- Update `changelogs.md` (v1.4.0 block per template), `known-issues.md`, and replace THIS file with the next loop's prompt (keep only one).

## PART 5 — Explicit non-goals this loop

- APK building/signing (GitHub Actions handles it).
- Cloud anything, telemetry — never (app is offline by design).
- Full `MainViewModel` god-class split — extract only pieces touched.
