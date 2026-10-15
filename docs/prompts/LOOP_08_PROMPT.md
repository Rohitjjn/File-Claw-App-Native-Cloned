# Loop 8 prompt — Markdown & TXT: fix what testing finds, then make it best-in-class

> **Provenance (read this honestly).** This prompt was written at the end of Loop 7 by one agent in one
> Arena session. The "senior team" below is a structured brainstorm: one agent reviewed the Loop 7
> work from eight senior roles in turn. No separate agents or models were run, and the model ID was not
> exposed by Arena. Do not record this prompt as evidence that a multi-agent team executed anything.
> Record only the tools and models you actually use (docs/AGENTS.md, "truthful tool use").

## 0. Mission

Loop 7 rebuilt the Markdown/TXT experience: a new reader page (`app/src/main/assets/reader/`), in-place
editing on the preview screen (rich Markdown with source-preserving save, plain text for TXT), working
Document outline / Viewer appearance / in-document search, browser-like zoom (25–500 %) and a status
bar that follows the app theme. This loop has two jobs, in this order:

1. **Fix every bug the user reports after testing Loop 7** (Phase A). These are P0; nothing in
   Phase B starts until each reported bug is reproduced, root-caused, fixed and verified.
2. **Make Markdown and TXT preview + editing feel premium** (Phase B): better UI, UX, features,
   reliability and speed, from the brainstorm below. Pick the highest-value items that fit the loop.

Scope stays **Markdown (`.md`, `.markdown`) and TXT (`.txt`) only**. Other formats come later.

## 1. Read first

1. `docs/AGENTS.md` (loop manual, environment lessons, decisions), `docs/TODO.md`, `docs/CHANGELOG.md`
   (the Loop 7 entry), this file.
2. Code map for this area:
   - `ui/screens/FilePreviewScreen.kt`: screen state, top bar, bottom tray, sheets and dialogs. Search for
     `isReaderDoc`, `isInlineEditing`, `ReaderDocumentView`.
   - `ui/reader/ReaderDocumentView.kt`: WebView host, `ReaderController` (commands into the page),
     `ReaderBridge` (`window.FCBridge` callbacks).
   - `ui/reader/ReaderHtml.kt`: page builder; Markdown is rendered per top-level block with
     `data-b/data-s/data-e` source ranges; TXT segments carry `data-s/data-e` (+ `data-cs/data-ce` for code).
   - `ui/reader/ReaderSheets.kt`: Viewer appearance sheet, edit history sheet, edit toolbar, edit menu, dialogs.
   - `ui/reader/ReaderAppearance.kt`, `ReaderOutline.kt`, `ReaderTextStats.kt`: pure Kotlin, unit tested.
   - `assets/reader/reader.js` + `reader.css`: appearance, search, navigation, editor, history, serializer.
   - `viewmodel/MainViewModel.kt`: `InlineEditSession`, draft holder, `saveTextFile(..., onResult)`.
3. Tests: `app/src/test/java/com/example/ui/reader/*`, `tools/reader-js-tests/` (jsdom).
   CI: `.github/workflows/android-ci.yml` runs on every push to `arena/**` and on PRs to `main`.
   It runs the jsdom tests, unit tests, and debug and release assembly. It never publishes a release.

## 2. Non-negotiable constraints

- **Never lose user text.** Untouched Markdown blocks must be written back byte for byte (CRLF, odd
  formatting, link definitions, front matter, HTML blocks). Add a regression test for every save bug.
- Destructive actions keep confirmations: X / Back while editing → discard confirmation, Save →
  confirmation with change summary. Do not add silent auto-save over the original file.
- Edit mode must keep looking like preview mode (same page, same styles, no line numbers).
- Don't change the **preview** bottom tray (search, line no., print, copy, edit, share) or the minimal
  top bar (back, name, 3-dot) unless the user asks.
- Truncated / converted files stay read-only.
- CI must be green before handing off. Report measured results as measured and everything else as
  "not verified on a device". Update CHANGELOG, TODO, `changelogs.md`, `docs/prompts/README.md`.

## 3. Phase A — bugs from the user's testing (do first)

Ask the user for their list if it is not already in the conversation. For each bug:
reproduce (or explain why it can't be) → root cause with file/line → smallest safe fix → automated test
(JUnit or jsdom) → manual check note. Keep a table in the CHANGELOG: *report → cause → fix → test*.

Likely trouble spots worth checking first, because jsdom can't exercise them:

| Area | What may go wrong | Where |
|---|---|---|
| IME / Gboard | Composition breaks after toolbar taps; autocorrect replacing text inside bold/links; Enter in lists | `reader.js` `restoreRange`, `onBeforeInput` |
| `execCommand` | Headings/lists/quote produce odd DOM (nested `ul` in `ul`, spans with inline styles) → odd Markdown | `RICH_COMMANDS`, `collectRuns`, `mdList` |
| Selection | Bold/Italic act on the wrong range after the keyboard closes or a sheet opens | `savedRange` handling |
| Keyboard | Tray not above the keyboard on some API levels / gesture nav; caret hidden behind tray | `FilePreviewScreen` IME padding, `SOFT_INPUT_ADJUST_RESIZE` effect |
| Scroll | Jump when entering/leaving edit mode or after save reload (TXT DOM swap) | `enter/exit`, `restoreScroll` |
| Save | CRLF/BOM/encoding round-trip; "No changes to save" when only formatting changed; dirty flag stale | `serializeRich`, `MainViewModel.saveTextFile` |
| Rotation | Draft restore after recreation; history resets (expected) but text must survive | `inlineEditDraft`, `rebaseInlineEdit` |
| Search | Overlay focus/keyboard; counts on huge files; search after reload | search `LaunchedEffect`s |
| Appearance | Zoom slider lag on long files; CSS `zoom` caret offsets on old WebView builds; sepia/black contrast | `applyAppearance`, `reader.css` |
| Outline | TXT heuristics false positives (ALL CAPS, numbered lines); MD headings inside HTML blocks | `ReaderOutline` |
| Status bar | Icons in focus mode with sepia/black page themes; Light app on dark system | `SideEffect` in `FilePreviewScreen`, `MainActivity` |

## 4. Phase B — brainstorm by senior role (pick, then justify the pick)

### Product lead (what users feel)
- Remember **reading position per file** and offer "Continue where you left off".
- **Reading stats**: words, characters, reading time in the outline sheet header and in the save dialog.
- **Quick actions on text selection** in preview: Copy, Search in document, Share quote, Highlight (later).
- "Recent edits" safety net: **local version snapshots on every save** (last N per file in app storage)
  with a simple diff view and one-tap restore.

### Senior Android / Compose engineer
- Move inline-edit state into a small `ReaderViewModel` (or a state holder) to shrink `FilePreviewScreen.kt`.
- **Disk drafts**: persist the debounced draft to `filesDir/drafts/` so process death never loses edits;
  show a "Recover unsaved edits?" banner on next open (compare file mtime/size).
- **External change detection**: before saving, compare mtime/size/hash with what was opened; if the file
  changed on disk, show Overwrite / Save as copy / Reload.
- Touch targets: tray icons are 36 dp; move to 48 dp hit areas without changing the visual size.
- Replace the hidden `.name.bak` file next to user files with an app-private backup (see Security).

### WebView / editor engineer
- **Markdown input rules** while typing: `# ` → heading, `- `/`* ` → list, `1. ` → numbered, `> ` → quote,
  ```` ``` ```` → code block, `[ ] ` → checklist, `---` → divider; `**x**` / `*x*` auto-format.
- **Smart lists**: Enter on an empty item exits the list; Backspace at item start outdents; checklist
  items continue as checklist.
- **Find & replace in edit mode** (rich and plain), with match case / whole word.
- Table editing: add/remove row and column, Tab moves between cells.
- Link popover on tap in edit mode: open, edit, remove.
- Keep undo history across rich ↔ source switches (store text snapshots in Kotlin), and group typing
  per word for finer undo.
- Code blocks: language picker in the code header while editing.

### UX / visual designer
- Editing affordance: subtle caret-colour accent, "Editing" chip, and an empty-document placeholder.
- A second, scrollable **keyboard accessory row** (headings, lists, quote, code, link, checklist) that
  appears above the main tray only while the keyboard is open. Keep the main tray order the user asked for.
- Appearance presets ("Comfort", "Compact", "Night reading") and a live mini-preview in the sheet.
- Outline: highlight the current section (scroll spy) and allow collapsing levels.
- Smooth transitions between preview and edit (tray cross-fade, title subtitle animation).

### QA / reliability
- Espresso-Web or UI Automator flows on an emulator in CI (`reactivecircus/android-emulator-runner`):
  open MD → edit → bold → save → reopen → byte compare; discard flow; rotation with unsaved edits.
- A corpus of real-world Markdown (READMEs with HTML, tables, footnotes, front matter, CRLF, emoji,
  RTL text) for golden round-trip tests: unchanged document must serialize identically.
- Fuzz the serializer in jsdom with random DOM edits; assert no exception and stable re-serialization.

### Performance engineer
- Cache rendered HTML per file (key: path + mtime + size + appearance-independent) to make re-opens instant.
- After save, **patch only changed blocks** instead of reloading the whole page.
- `content-visibility: auto` on top-level blocks for very long documents; measure first paint and
  scroll jank with Macrobenchmark / FrameMetrics and record numbers.
- Large TXT (> 250k chars): windowed viewer with page-in/page-out and windowed editing of a section.

### Accessibility specialist
- TalkBack labels and state for Bold/Italic toggles ("on/off"), history entries and outline items.
- Respect system font scale in the reader (map to the text-size setting) and test at 200 %.
- High-contrast page theme; check colour contrast of sepia/black themes (WCAG AA).
- Hardware keyboard: Ctrl+F search, Ctrl+Z/Y, Ctrl+B/I, Ctrl+S (save) — verify on a device.

### Security reviewer
- Replace the regex `HtmlSanitizer` for reader pages with an allowlist sanitizer and add a strict
  Content-Security-Policy meta tag (no remote scripts, no remote images by default).
- Links: show the full domain in the open-link dialog; block `javascript:`/`intent:` schemes.
- Images: load relative images through `WebViewAssetLoader` with a path guard limited to the
  document's folder (opt-in), never `allowFileAccess`.

## 5. Deliverables and acceptance

- Phase A table complete; every reported bug has a test or a written reason why it can't be automated.
- Chosen Phase B items implemented behind the constraints above, each with tests.
- CI green on the loop branch (jsdom + unit tests + debug/release assemble); link the run.
- Docs updated (CHANGELOG, TODO, `changelogs.md`, `docs/prompts/README.md`, SANITY_CHECKLIST items for
  anything that needs a device) and `docs/prompts/LOOP_09_PROMPT.md` written for the following loop.
