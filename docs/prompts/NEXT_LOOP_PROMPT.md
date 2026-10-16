# File Claw — NEXT LOOP PROMPT (Loop 13)

> **Scope:** v1.3.2 device-testing round — verify the Loop 12 fixes on real hardware first (search arrows, heavy-PDF lifecycle, dark-mode classifier, two-phase DOCX zoom, streamed DOCX bytes), then pull from the improvement backlog. User tests the APK built by GitHub Actions from branch `v1.3.2` and reports real-device bugs; fix those FIRST.
> **Golden rule:** 100% offline. No new network code, no cloud calls, no heavy new dependencies without measuring APK impact.

---

## PART 0 — Working protocol (senior agents team)

1. Clone/refresh the repo, record remote URL, default branch, current branch, HEAD SHA, open PRs with head/base SHAs. Verify the CURRENT state of `main`, `v1.3.1` and any `v1.3.2` branch instead of assuming; previous loops mentioned draft PR #22 (superseded, leave alone) and merges via PR #26 (tag v10.0.4).
2. Read in order: `worklog.md` (Loop 12 entry `LOOP12-DOCX-SEARCH-PDF-DARK-PERF`), `PATCH_NOTES_LOOP12.md`, `changelogs.md` (unreleased v1.3.2 section), `known-issues.md` (Loop 12 watchlist + BUG-006/007/008), `docs/TODO.md`, `rules.md`, `tech-specs.md`, `BUILD_GUIDE.md`.
3. Establish the REAL test baseline from the current checkout/CI before touching anything (static count at Loop 12 start was 167 `@Test` methods; Loop 12 added 30 → expect 197 in CI; never trust the stale 134/146/188 figures).
4. Run the audit team FIRST (research-only; label reports individually if run sequentially as virtual audits):
   - Agent A: search sweep — confirm `InViewerSearchCommandPolicy` wiring in both WebView effects, PDF snapshot index sync after Loop 12, HTML (`findAllAsync`) gap (BUG-008).
   - Agent B: PDF lifecycle — `dumpsys meminfo` before/after a 200-page scroll session, renderer-kill logcat scan, revisit-after-release latency, resident-window behavior.
   - Agent C: DOCX — two-phase zoom feel on a 100+ page document, settle-commit timing, streamed `/docx-src/document.docx` path plus fallback, fallback renderer regression check.
5. Implement, run the quality gate (PART 4), deliver a **patch zip with only updated files** (repo-relative paths, upload-ready to GitHub). APK compile happens via GitHub Actions — do NOT ship an APK.

## PART 1 — Device verification checklist for the Loop 12 build (gate before any new work)

- [ ] **Search arrows (the original "7 results, arrows reach only 2–3")**: a 7-match document in DOCX, TXT, MD and PDF — next walks 1→7 and wraps, previous walks 7→1 and wraps; mid-typing navigation never re-searches; closing find clears counts.
- [ ] **Heavy PDF**: open a 200+ page document (time to interactive), scroll to page ~200, scroll back, zoom 2×/3×, search a common word; `dumpsys meminfo` before/after must show bounded growth; no renderer kills in logcat.
- [ ] **Very large page**: poster/blueprint PDF renders (downsampled), never blank.
- [ ] **Dark mode**: color portrait, grayscale portrait, low-saturation portrait, mixed text+photo, chart, B/W scan, text-only page — portraits must NEVER look like negatives; text pages still invert deep.
- [ ] **DOCX zoom**: pinch on a 100+ page DOCX tracks fingers smoothly (no lag spike, no jump), settles crisply, chip updates live; toolbar +/- and percent dialog still instant; double-tap toggle works; zoom >100% keeps horizontal scrolling.
- [ ] **DOCX open**: large DOCX (10–20 MB) opens without memory spikes; DOCX from inside a zip still opens (streamed path + fallback).
- [ ] **Renderer-crash recovery** (if triggerable): PDF error state shows Try Again which rebuilds; DOCX falls back to Clean Document View.
- [ ] **Loop 10 regressions still green**: header find bar, IME edit tray, back-closes-find, fallback zoom controls hidden, PDF sharp settle re-render.

## PART 2 — User-reported bug fixes (fill after device testing of the v1.3.2 branch)

- [ ] BUG-USER-1: ______ (report verbatim → root-cause → fix → regression note)
- [ ] BUG-USER-2: ______
- [ ] BUG-USER-3: ______

Known watch items from Loop 12 (see `known-issues.md` Loop 12 watchlist; fix if confirmed):
- BUG-006: mixed-orientation PDFs get page-1-estimated placeholders — if the shift on first approach is noticeable, fetch real geometry for the first N pages of each distinct size instead of all pages.
- BUG-007: DOCX pinch preview transiently overflows horizontally mid-gesture — if it bothers users, clamp the preview transform horizontally or sync wrapper min-width during preview.
- Resident window 3 too tight on fast flips → raise `residentPageWindow` in `search.html` AND `PdfRenderBudgetPolicy.RESIDENT_PAGE_WINDOW` together (keep the mirror exact), re-measure memory.
- Dark mode dim level (`brightness(.9)` on `.pdf-page.natural`) — tune only there; never re-enable blanket inversion.

## PART 3 — Improvement backlog (ranked by the Loop 12 lead)

1. **Bounded tile rendering at extreme PDF zoom** (impact: high, risk: medium, testable: pure tile-scheduler unit tests + device memory probes). Above a zoom threshold render only the viewport region at full backing density (pdf.js `enableDetailCanvas` is the reference design). Precondition: tile coordinates, scroll anchoring, highlight overlay mapping and cleanup verified on device — do NOT ship a partial tile implementation.
2. **Match-count parity policy** (impact: medium, risk: low). DOCX navigable cap 5,000, TXT/MD 1,000, PDF stored 2,000 page targets — extract into one tested policy so displayed total vs navigable count can never drift; adds honest `3/7+` truncation labels if ever needed.
3. **Content search in global search** (impact: high, risk: medium). Extend `TextFileSearchEngine` + `FileSearchRanker` to rank files by match count with bounded memory; reuse the header find bar to open the first hit.
4. **HTML in-document search parity** (BUG-008): replace `findAllAsync`-only highlighting with the shared command-policy pipeline (next/prev + count badge) for HTML previews with scripts disabled-safe.
5. **DOCX page-count/outline sheet**: outline bottom sheet for DOCX headings using the existing `DocumentOutlineBottomSheet`, reusing the page observer already present.

## PART 4 — Quality gate (unchanged)

```bash
chmod +x ./gradlew
./gradlew :app:compileDebugKotlin --no-daemon
./gradlew :app:testDebugUnitTest --no-daemon
./gradlew :app:assembleDebug --no-daemon
```

Also: `git diff --check`; extract modified inline JS to temp files and `node --check`; XML parse for manifest changes; verify the Actions run for the exact PR commit. Mark each `PASS`/`FAIL`/`NOT RUN` with real output. No fake green.

## PART 5 — Non-goals

- No renderer migration (docx-preview stays at its bundled version unless a migration is proven offline-safe: assets, minSdk, page layout, search, print/export, dark mode, APK size).
- No AI models, server processing, external APIs or heavy libraries for dark mode or search.
- Do not touch the DOCX viewer chrome (Claude-inspired header) or unrelated TXT/MD UI.
- Never merge/publish a release without user approval; never modify `main` directly.
