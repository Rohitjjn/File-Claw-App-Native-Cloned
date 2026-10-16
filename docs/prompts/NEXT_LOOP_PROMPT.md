# File Claw — NEXT LOOP PROMPT (Loop 15)

> **Scope:** v1.3.3 device-testing round — verify the Loop 14 fixes on real hardware FIRST (DOCX real rendering, toolbar rotate/search order, scrubber, rotation lifecycle, always-light PDF, 8x zoom, configChanges sweep), then pull from the backlog. User tests the APK built by GitHub Actions from branch `v1.3.3` and reports real-device bugs; fix those FIRST.
> **Golden rule:** 100% offline. No new network code, no cloud calls, no heavy new dependencies without measuring APK impact.

---

## PART 0 — Working protocol (senior agents team)

1. Clone/refresh the repo, record remote URL, default branch, current branch, HEAD SHA, open PRs with head/base SHAs (PRs #21/#22 were still open at Loop 14 start — leave alone). Verify the CURRENT state of `main`, `v1.3.1`–`v1.3.3` branches instead of assuming; Loop 14 landed on `feat/loop-14-docx-pdf-viewer-reliability` from main @ `dd4681e` (tag v10.0.5) and ships via branch `v1.3.3` + workflow version input.
2. Read in order: `worklog.md` (Loop 14 entry `LOOP14-DOCX-RESTORE-PDF-SCRUBBER-ROTATE-LIGHT`), `PATCH_NOTES_LOOP14.md`, `changelogs.md` (unreleased v1.3.3 section), `known-issues.md` (Loop 14 watchlist + closed BUG-003), `docs/TODO.md`, `rules.md`, `tech-specs.md`, `BUILD_GUIDE.md`.
3. Establish the REAL test baseline from the current checkout/CI before touching anything (static count at Loop 14 start was 199 `@Test`; Loop 14: −14 `PdfDarkModePolicyTest` + 16 `PdfRotationPolicyTest` + 4 `DocxTextRecoveryTest` = **205 expected in CI**; never trust the stale 134/146/167/197 figures).
4. Run the audit team FIRST (research-only; label reports individually if run sequentially as virtual audits):
   - Agent A: DOCX — confirm the `/assets/` registration behaves on device (storage + zip DOCX), recovery-banner reason text quality, retry path.
   - Agent B: rotation/scrubber — rotate a password PDF through all four steps with active search; scrubber drag on 500+ pages; `dumpsys meminfo` during scrub.
   - Agent C: lifecycle — configChanges regression sweep (theme flip, locale, split-screen, IME in edit mode), renderer-kill logcat scan at 8x zoom.
5. Implement, run the quality gate (PART 4), deliver a **patch zip with only updated files** (repo-relative paths, upload-ready). APK compile happens via GitHub Actions — do NOT ship an APK.

## PART 1 — Device verification checklist for the Loop 14 build (gate before any new work)

- [ ] **DOCX real rendering**: formatted pages (boundaries, margins, spacing), tables/images/lists render, 100+ page file, find bar counts + next/prev, pinch/±/percent/double-tap zoom, horizontal scroll >100%, malformed DOCX → honest recovery banner + working Retry.
- [ ] **PDF toolbar order**: Page indicator · Rotate · Search · Print · Share — exactly this.
- [ ] **Rotate**: 90/180/270/0 on a middle page — no page-1 reset, no blank flash, no password re-prompt (password PDF!), search/highlights follow, zoom preserved, scrubber stays synchronized.
- [ ] **Scrubber**: first→last→first drag on a 500+ page PDF; bubble `n / total` tracks; release jumps; memory flat during drag; visible with gesture navigation.
- [ ] **Always-light PDF**: PDF under Android dark mode stays light end-to-end (viewer, status bar canvas, pages); DOCX/TXT/MD dark modes still work.
- [ ] **8x zoom**: text sharp at max pinch, horizontal+vertical pan, `dumpsys meminfo` bounded, no blank pages on poster/blueprint pages.
- [ ] **configChanges sweep**: rotate on Home/Preview/Edit, system theme toggle, split-screen — no state loss, no dead WebView, edit tray + IME still correct.
- [ ] **Loop 10/12 regressions green**: header find bar, IME edit tray, back-closes-find, search arrows 1→N both directions (DOCX/TXT/MD/PDF), heavy-PDF bounded scroll, renderer-crash Try Again.

## PART 2 — User-reported bug fixes (fill after device testing of the v1.3.3 branch)

- [ ] BUG-USER-1: ______ (report verbatim → root-cause → fix → regression note)
- [ ] BUG-USER-2: ______
- [ ] BUG-USER-3: ______

Known watch items from Loop 14 (see `known-issues.md` Loop 14 watchlist; fix if confirmed):
- DOCX recovery banner reason strings: if they read too technical, translate to friendly copy (keep content-free).
- Scrubber vs landscape pages on narrow screens (minor overlap possible).
- Rotation anchor is relative-within-page by design; if users report drift on mixed-orientation documents, consider per-orientation anchors.
- `configChanges` breadth: any screen that behaved differently after recreation removal.

## PART 3 — Improvement backlog (ranked by the Loop 14 lead)

1. **DOCX page-count/outline sheet** (impact: medium, risk: low): outline bottom sheet from DOCX headings using the existing `DocumentOutlineBottomSheet` + the page observer already present.
2. **Match-count parity policy** (impact: medium, risk: low): DOCX 5,000 / TXT-MD 1,000 / PDF 2,000 caps into one tested policy with honest `3/7+` labels.
3. **Content search in global search** (impact: high, risk: medium): extend `TextFileSearchEngine` + ranker to file contents, bounded memory.
4. **HTML in-document search parity (BUG-008)**: command-policy pipeline for HTML previews.
5. **Bounded tile rendering at extreme PDF zoom** (impact: high, risk: medium, unchanged from Loop 12/13): only after the 8x whole-page behavior is device-proven insufficient; pdf.js `enableDetailCanvas` is the reference design; do NOT ship a partial tile implementation.

## PART 4 — Quality gate (unchanged)

```bash
chmod +x ./gradlew
./gradlew :app:compileDebugKotlin --no-daemon
./gradlew :app:testDebugUnitTest --no-daemon
./gradlew :app:assembleDebug --no-daemon
git diff --check
```

Also: `node --check` on modified inline JS (extract to temp files; module scripts to `.mjs`); XML parse for manifest changes; verify the Actions run for the exact head SHA. Mark each `PASS`/`FAIL`/`NOT RUN` with real output. No fake green. A headless-browser end-to-end pass on `docx_editor.html` and `search.html` (as done in Loop 14) is strongly recommended before delivery.

## PART 5 — Non-goals

- No renderer migration (docx-preview stays bundled; a migration must be proven offline-safe: assets, minSdk, page layout, search, print/export, dark mode, APK size).
- No proprietary Word/Drive code, no cloud viewers, no network rendering.
- Do not redesign the DOCX viewer chrome or unrelated TXT/MD UI.
- Never merge/publish a release without user approval; never modify `main` directly.
