# File Claw — NEXT LOOP PROMPT (Loop 8)

> **Scope:** MD/TXT preview + inline edit — round 2. User will test the Loop 7 build on a real device and report bugs; fix those first, then push the MD/TXT experience to a truly premium level.
> **Golden rule:** 100% offline. No new network code, no cloud calls, no heavy new dependencies without measuring APK impact.

---

## PART 0 — Working protocol (senior agents team)

1. Download/refresh the uploaded repo zip, extract, and read in order: `worklog.md` (if present), `changelogs.md`, `known-issues.md`, `docs/TODO.md`, `rules.md`, `tech-specs.md`.
2. Run the audit team FIRST (research-only sub-agents, one per area, separate tasks):
   - Agent A: status bar / theme / system bars regression sweep (`Theme.kt`, `MainActivity.kt`, `FilePreviewScreen.kt` system-bar effects).
   - Agent B: `FilePreviewScreen.kt` preview path (top bar, 3-dot menu, bottom tray, `SearchableDocumentWebView`, `DocumentPreviewSupport.kt` JS).
   - Agent C: inline edit path (`InlineTextEditorSupport.kt`, `EditorScreen.kt`, `MainViewModel.isEditMode`).
   Log each agent's findings to `worklog.md` (append-only, Task ID + template).
3. Implement, then run the full quality gate (see PART 4) before packaging. Deliver: source zip + debug APK + updated docs.

## PART 1 — User-reported bug fixes (fill after device testing)

- [ ] BUG-USER-1: ______ (report verbatim, root-cause, fix, regression note)
- [ ] BUG-USER-2: ______
- [ ] BUG-USER-3: ______

Anything not reproducible from the report alone: reproduce first with a minimal file in `/tmp` fixtures, then fix. Never "fix" blind.

## PART 2 — TXT/MD feature upgrades (priority order)

### 2.1 Find & Replace (inline edit)
- Replace bar inside inline edit mode (find next/prev, replace, replace-all).
- Live diff preview before replace-all; single atomic write via existing `AtomicFileWriter` pipeline; undo stack captures pre-replace state.

### 2.2 Search UX (preview)
- Case-sensitive + whole-word toggles in the in-document search bar.
- Match list strip (jump to any match), match count already exists — extend it.
- Auto-clear highlights when the overlay closes (already partially handled — verify).

### 2.3 Viewer Appearance round 2
- Line-height control (1.3 / 1.55 / 1.8) and reading column width (600px / 720px / full).
- Font family choice: Sans / Serif / Mono for body text.
- Fold `document.body.style.zoom` fallback: if `CSS.supports('zoom')` is false, fall back to `font-size` scaling — verify on old WebViews.
- Sync pinch zoom ↔ slider state where feasible; persist per file extension (md vs txt) in `ViewerAppearancePrefs`.

### 2.4 Outline round 2
- Nested tree rendering (children under parents) in `DocumentOutlineBottomSheet`.
- Track and highlight the heading currently in view (scroll listener → JS bridge → marker in sheet).
- "Go to Line" accuracy for MD: consider emitting invisible line-anchor spans during HTML generation (`<span data-line="N">`) so jumps become exact instead of approximate.

### 2.5 Markdown rendering extras (offline only)
- Task lists (`- [ ]` / `- [x]`) with tappable checkboxes (read-only tap → toast, or edit-mode sync).
- Footnotes + strikethrough (`~~text~~`) if the GFM flavour already parses them — style them.
- Offline fenced-code syntax highlighting: bundle a tiny highlighter (e.g. prism core ≤ 20KB, selected languages) into assets; lazy-load only when code blocks exist. Measure APK delta first.
- Image zoom on tap (reuse the image viewer's pinch-zoom composable) for embedded images.

### 2.6 Inline edit upgrades
- Markdown helper second row (H1/H2, list, quote, link, code) — keep the primary tray untouched per spec.
- Crash-safe auto-draft: every 30s while dirty, write draft to cache; on next open of the same file offer "Recover draft?" dialog; clear on save/discard.
- Selection toolbar (ActionMode) custom callbacks wired to bold/italic wrap helpers.
- Word/char counter chip in the tray (only when space allows on screen ≥ 400dp).

### 2.7 Large-file path
- >2MB text files: editing currently blocked by `editable=false` (truncated). Implement windowed editing: load ±50k chars around cursor with `TextWindowReader`, save merges windows back atomically. Guard with clear banner. This is the P1 item.

## PART 3 — Known issues to verify/keep an eye on (from Loop 7)

1. `ViewerAppearancePrefs.getFontSize` default changed 14 → 16; ensure no visual regression for users who never opened the appearance sheet.
2. Outline sheet is text-files-only now (pdf/docx outline was never wired — either wire via their engines or keep hidden; do not show a dead menu item).
3. `TextFileSearchEngine` overlap dedupe was fixed byte-wise; add a regression test for `bufferSize=2` + `"aaaaa"`/`"aa"` and CRLF files.
4. Status-bar icon appearance is asserted from `FilesClawTheme` SideEffect + preview's per-file canvas; test theme toggle inside Settings while a preview is in back stack.
5. `zoomPercent` slider steps by 5; presets include 500% — verify horizontal scrolling + code blocks still usable at 500%.

## PART 4 — Quality gate (MUST pass before delivery)

```bash
export ANDROID_HOME=<sdk-path>
export JAVA_HOME=<jdk-17-with-jlink>   # JRE is NOT enough: jlink is required by AGP
./gradlew :app:compileDebugKotlin --no-daemon --max-workers=1
./gradlew :app:testDebugUnitTest --no-daemon --max-workers=1     # 134+ tests, 0 failures
./gradlew :app:assembleDebug     --no-daemon --max-workers=1     # full APK
```
- Unit tests for every new pure helper (mirror-test critical logic in Node if JVM is unavailable).
- Detekt should get its first green run (`ignoreFailures` stays until then — but aim to flip it).
- Manual smoke checklist (on device or emulator): open md → outline jumps → appearance theme+zoom → search with keyboard → edit inline → bold/italic/tab/undo/redo/history → save confirm → discard confirm → export copy → rotation mid-edit → back gesture mid-edit.
- Update `changelogs.md` (new version block per template), `known-issues.md`, `docs/TODO.md`, and replace THIS file with the next loop's prompt (delete old prompt files; keep only one).

## PART 5 — Explicit non-goals this loop

- PDF/DOCX/PPTX/CSV changes (separate loops).
- Cloud sync, online fetchers, telemetry — never (app is offline by design).
- Splitting `MainViewModel` god-class fully — only extract the pieces touched (avoid mega-refactor risk mid-iteration).
