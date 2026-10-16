# File Claw — NEXT LOOP PROMPT (Loop 11)

> **Scope:** v1.3.1 device-testing round — verify and polish the Loop 10 viewer fixes (header find bar, IME-aware edit tray, DOCX pinch gate, PDF sharp re-render) across PDF, DOCX, TXT and Markdown. User tests the APK built by GitHub Actions from branch `v1.3.1` and reports real-device bugs; fix those FIRST, then pull from the improvement backlog below.
> **Golden rule:** 100% offline. No new network code, no cloud calls, no heavy new dependencies without measuring APK impact.

---

## PART 0 — Working protocol (senior agents team)

1. Clone/refresh the repo, read in order: `worklog.md` (Loop 10 entry `LOOP10-SEARCH-IME-ZOOM`), `PATCH_NOTES_LOOP10.md`, `changelogs.md` (unreleased section), `known-issues.md` (Loop 10 watchlist), `docs/TODO.md`, `rules.md`, `tech-specs.md`, `BUILD_GUIDE.md`.
2. Establish the REAL test baseline from the current checkout/CI before touching anything (static count at Loop 10 start was 167 `@Test` methods + 21 added by Loop 10 = 188; never trust the stale 134/146 figures).
3. Run the audit team FIRST (research-only, sequential if parallel sub-agents are unavailable; label reports individually):
   - Agent A: viewer search sweep — header find bar across all four formats + WebView pipelines (`DocumentPreviewSupport.kt`, `assets/pdfjs/search.html`, `assets/editor/docx_editor.html`), stale-highlight/count edge cases after Loop 10.
   - Agent B: PDF pipeline — `setPdfQualityBudget` behavior on real devices (`dumpsys meminfo`, logcat renderer kills), `renderedRatio` swap path, focus-mode/rotation resize debounce.
   - Agent C: DOCX + IME — `PinchGestureGate` feel on hardware, fallback control hiding, `safeDrawing.bottom` tray behavior with gesture vs three-button nav.
4. Implement, run the quality gate (PART 4), deliver a **patch zip with only updated files** (repo-relative paths, upload-ready to GitHub). APK compile happens via GitHub Actions — do NOT ship an APK.

## PART 1 — User-reported bug fixes (fill after device testing of the v1.3.1 branch)

- [ ] BUG-USER-1: ______ (report verbatim → root-cause → fix → regression note)
- [ ] BUG-USER-2: ______
- [ ] BUG-USER-3: ______

Known watch items from Loop 10 (verify on device, fix if confirmed):
- PDF sharpness on a dpr≈3 device at 2×/3× after the budget raise (16M tier); if a device class still blurs or the renderer is killed at 5×, investigate **viewport/tile rendering** (bounded strategy), do NOT simply remove caps.
- DOCX pinch start latency: the gate skips exactly one transition event per finger-count change; confirm it is imperceptible and that adding/removing a finger mid-pinch does not jump the zoom.
- Edit tray with three-button navigation in landscape and large-font mode (union inset must not double-pad).
- Back-closes-find-first interaction: confirm no flow expected back to leave the document while find is open.
- DOCX fallback renderer: zoom controls are hidden there by design; if users want zoom in fallback mode, implement it in the fallback text view instead of re-enabling dead controls.

## PART 2 — Improvement backlog (proposal ranking by the Loop 10 lead; keep scope on PDF/DOCX/TXT/MD unless the user expands it)

1. **PDF tile rendering at extreme zoom** (impact: high, risk: medium, testable: unit-testable tile scheduler + device memory probes). Render only the visible viewport region at full backing density above a zoom threshold (e.g. >3×), instead of whole pages, so sharpness is no longer bounded by the per-page budget. Grounded in: `desiredPixelRatio()` clamp is the only thing between long documents and OOM at 5×.
2. **Search-in-file contents for global search** (impact: high, risk: medium, testable: extend `TextFileSearchEngine` + `FileSearchRanker`). Rank files by match count with the existing bounded-memory streaming engine; reuse the Loop 10 header find bar for opening the first hit.
3. **Match-count parity audit across the four pipelines** (impact: medium, risk: low, testable: pure logic). DOCX navigable cap is 5,000, TXT/MD 1,000, PDF stores 2,000 page targets — extract these caps into one tested policy object so the displayed total and the navigable count can never drift apart.
4. **Focus-mode IME regression guard** (impact: low, risk: low, testable: instrumentation-lite). Add a debug overlay/flag that logs inset values when the edit tray moves, to make the Loop 10 IME fix verifiable without a full UI-test dependency.

## PART 3 — Non-goals

- No new document-rendering dependencies; keep the bundled PDF.js / docx-preview / JSZip assets offline and pinned.
- No redesign of the Claude-inspired visual language; no changes to formats outside PDF/DOCX/TXT/MD without explicit user approval.
- Do not merge or auto-close Draft PR #22 from automation; PR decisions belong to the user after APK testing.

## PART 4 — Quality gate

```bash
chmod +x gradlew   # if needed after clone; keep the executable bit
./gradlew :app:compileDebugKotlin --no-daemon
./gradlew :app:testDebugUnitTest --no-daemon
./gradlew :app:assembleDebug --no-daemon
```

- Compare test counts with the CURRENT baseline (188 static `@Test` methods after Loop 10), not with 134/146.
- Syntax-check every edited inline `<script>` with `node --check` (extract to a temp file; do not commit it).
- Deliver: patch ZIP with only changed files at repository-relative paths, worklog entry, changelog entries in the mandatory format, and an honest pass/fail/not-run check report. Never fabricate green output.
