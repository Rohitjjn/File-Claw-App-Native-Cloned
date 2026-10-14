# FILE CLAW — LOOP 8: PREMIUM FOUR-FORMAT DOCUMENT VIEWERS
## Focused execution prompt — PDF, DOCX, TXT, Markdown only

### Scope lock
Only improve preview/search/navigation/dark mode/zoom/immersive UI/code copying/printing for PDF, DOCX, TXT and Markdown. Do not refactor image, spreadsheet, archive, media or other viewers. Do not bump release versions, merge or publish a release in this loop.

### Product objective
A premium, mobile-first document reader: search reaches actual matches with accurate counts/page labels; Print/Export to PDF produces real non-empty output; DOCX fits a mobile screen and responds immediately to dark mode; immersive mode has no black top/bottom strip; TXT/Markdown keep the prior familiar layout and expose per-fence copy buttons.

### Fifty-role engineering team model (10 squads × 5 specialties)
Architect assigns one owner to each squad, 5 reviewers/builders per squad, and one integrator who owns final commits:
1. **Architecture & Compose:** lead architect, Compose state, navigation/back stack, insets/window policy, API compatibility.
2. **PDF engine:** text extraction, physical-page mapping, query/index design, password/corruption handling, PrintDocumentAdapter.
3. **DOCX engine:** OOXML extraction, docx-preview/WebView lifecycle, dark styling, zoom/fit, heading/match navigation.
4. **TXT/Markdown:** legacy UI preservation, Markdown rendering, fence parsing, HTML escaping, code-copy UX.
5. **Search UX:** match counts, next/previous, one-character input, Unicode/whitespace normalization, loading/cancellation.
6. **Print/export:** HTML pagination, PDF page copying, print job lifecycle, permissions/errors, output integrity.
7. **Premium UI/accessibility:** visual system, contrast, touch targets, TalkBack labels, small-screen layouts.
8. **Performance/security:** large documents, memory/I/O, WebView script/link policy, offline dependency review, licensing.
9. **QA:** unit fixtures, Compose/WebView testability, device smoke tests, CI triage, regression audit.
10. **Docs/release discipline:** changelog/TODO, build workflow, prompt librarian, PR reviewer, release readiness.

This is a 50-role responsibility model, not a claim that 50 agents are concurrently available in this chat.

### Hermes Agent orchestration
- Optional orchestration tool: [NousResearch/Hermes Agent](https://github.com/NousResearch/hermes-agent). Its README documents isolated subagent delegation and its repository lists the MIT license ([license](https://github.com/NousResearch/hermes-agent/blob/main/LICENSE)).
- If Hermes is installed in the executing environment, verify configured models/tools, parallel-agent limits, cost and permissions, then spawn isolated review workstreams. Have one integrator apply changes; do not let agents overwrite the same file concurrently.
- Do not add Hermes as an Android app dependency, do not pass signing/production secrets, and never claim 50 live agents were launched unless runtime evidence confirms it.

### Category 1 — Search for all four formats
- PDF: use PDFBox-Android or another verified PDF text engine, not raw stream ordinals; map hits to physical pages. Case-insensitive + whitespace-normalized query; accept one-character searches. A scanned/image-only PDF must say it contains no searchable text; OCR is out of scope unless separately justified.
- DOCX: parse OOXML text off the main thread. Connect search results to the rendered WebView's find-in-page and keep visible match counts consistent. Do not confuse heading-outline index with text-search index.
- TXT/Markdown: search the complete content supplied to the reader. Previous/next must call actual WebView/native match navigation, not only update a counter. Clear highlights and reset stale match state when query/file changes.
- Add cancellation/stale-result protection for changing queries/files. Do not block the UI while extracting PDF or DOCX text.

### Category 2 — restore TXT/Markdown UI and per-block copying
- Restore the last known-good TXT/Markdown presentation from Git history; do not recreate it from screenshots alone.
- Keep TXT text readable. If it contains triple-backtick fences, render each fence as a separately bordered code card, with **Copy code** inside that card. Copy only the block contents, preserve internal line breaks, and exclude the fence markers and surrounding prose.
- Markdown fenced blocks each get their own copy button and language label. Escape all rendered source HTML to prevent content injection.
- Maintain search, font sizing, tap-to-toggle chrome and scroll position without reloading the whole document on every keystroke.

### Category 3 — Print / export to PDF
- PDF: route to a real PDF print adapter; never read binary PDF bytes as text. Preserve vector/selectable text if the supported library allows it; handle encrypted PDFs as a clear error/password path.
- DOCX: wait until formatted rendering completes, then print its WebView. If rendering fell back to plain extracted text, print that actual fallback rather than a blank WebView.
- TXT/Markdown: print the complete rendered/current document, including line breaks, tables/headings and code cards where supported.
- Show useful errors for unreadable files, PrintManager failure, cancellation or render failure. Never silently print empty content. Keep print job ownership/lifecycle safe when a WebView is disposed.

### Category 4 — Focus mode, zoom, dark mode
- Immersive mode hides the app bars and system bars together; use transparent system bars/edge-to-edge layout while hidden, and restore original window colors/inset flags on exit/disposal. No black strip above the document.
- DOCX: fit common portrait pages to mobile width by default, keep pinch range 0.25×–5×, allow panning at zoom, and provide readable table/text dark styles. Dark-mode toggling updates the existing DOM without reopening the file.
- PDF: prefer the PDF engine's supported night renderer or a measured luminance-aware approach; do not use broad color filters that make scanned pages/photos look like a negative.
- Verify rotation, Back, theme switch and immersive enter/exit on small and large screens.

### Category 5 — CI, tests and prompt archive
- Add regression tests for PDF physical-page mapping/whitespace/one-character search, DOCX searchable content wiring, Markdown/TXT multiple + unclosed fences, HTML escaping, and print dispatch/routing.
- Run `./gradlew :app:testDebugUnitTest`, `./gradlew :app:jacocoCoreReport :app:jacocoCoreVerify`, `./gradlew :app:lintDebug`, `./gradlew :app:detekt`, and `./gradlew :app:assembleDebug`. Run `compile_applet` only if it exists in `./gradlew tasks --all`.
- Inspect the actual branch-head GitHub Actions run and attach artifacts. Do not mark CI successful while pending.
- Organize loop prompts under `docs/prompts/` with filenames such as `LOOP-08-four-format-premium-viewers.md` and `LOOP-09-four-format-reader-polish.md`. Keep old prompts available as archives/compatibility links.
- Update changelog/TODO/build guide and open a draft PR. No merge, app-version bump or release from this loop.

### Required protocol and acceptance
1. Inspect current branch/head, Git history for old TXT/Markdown presentation, relevant source/dependencies, tests and workflow.
2. Produce a file-level plan and risk/test matrix before edits.
3. Have squads inspect independently; integrate small sequential commits with one owner.
4. Run the tests/build/CI listed above; record exact outcomes and fix reproducible failures.
5. Manual-test all four formats on an Android device; CI cannot validate physical pinch gestures, insets and print UI.
6. Keep PR draft until checks and manual smoke tests pass.
Acceptance means: all four formats search/print accurately; TXT/MD code-block copying is isolated; DOCX fit/dark mode works; PDF dark mode preserves usable image content; focus mode draws edge-to-edge; docs accurately distinguish verified and pending.
