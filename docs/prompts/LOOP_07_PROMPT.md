# File Claw — Loop 7: document-reader reliability and scale

## Mission and non-negotiable scope
Improve only the existing **PDF, DOCX, TXT, and Markdown** previews. Do not add or refactor unrelated file formats. Preserve the app's offline-first and data-safety guarantees. Never claim a feature works unless its relevant checks actually ran.

## First: establish the real baseline
1. Read `docs/AGENTS.md`, the **KNOWN ISSUES** section of `docs/TODO.md`, `docs/CHANGELOG.md`, `docs/ROADMAP.md`, `docs/ARCHITECTURE.md`, `docs/DESIGN.md`, `docs/SECURITY_AUDIT.md`, `docs/SANITY_CHECKLIST.md`, and `docs/prompts/README.md` before editing.
2. Check Java, Kotlin, Android SDK, Gradle wrapper permissions, and network availability. Run the project's real build/test commands before changing code when possible. If the environment cannot compile Android, report the exact blocker and use only checks that are actually available; do not present static inspection as a build.
3. Check whether Hermes Agent can genuinely be run in this environment. The user previously requested the official `NousResearch/hermes-agent` repository for multi-agent work. Use it only if its runner and required model/provider configuration are available and the run can be verified. Otherwise continue honestly as a single agent and say Hermes was not run. Do not invent subagents, model IDs, credentials, or results.
4. Do not guess which AI model produced the work. Report the exact model identifier only if the environment exposes it; otherwise state that Arena Agent Mode did not expose it.

## Loop 7 priorities

### P0 — bounded large-file text/Markdown preview and search
- Build on `TextFileSearchEngine` and `TextWindowReader` without loading an entire large user file into a Kotlin string or WebView DOM.
- Replace the current first-window-plus-search-excerpt behavior for large TXT/Markdown files with a real paged/virtualized reading experience that supports forward/backward scrolling, stable line/offset positions, and direct jumps to search hits.
- Preserve Markdown structure across page boundaries (including fenced code blocks, lists, tables, headings, and links) so a page does not lose rendering context merely because a fence or block began in an earlier window.
- Keep full-document search bounded: count all hits, make navigation beyond the currently retained hit offsets possible, and visibly highlight the selected result. Show a clear, truthful status if a configured result cap is reached.
- Test UTF-8, BOMs, CRLF, UTF-16LE/BE, supplementary Unicode, queries spanning read boundaries, matches near EOF, many repeated hits, cancellation, and changing the query while a scan is in flight. Verify byte-offset-to-window mapping against real files.

### P1 — search correctness across document structure
- DOCX: search all rendered pages and across adjacent styled text runs where the visible text is contiguous; maintain correct counts, next/previous navigation, active highlighting, and cancellation when the query or document changes.
- PDF: test highlight geometry for rotated pages, CJK/CMap text, ligatures, multi-run matches, columns, zoom changes, and night mode. Ensure query changes cannot publish stale results and that page navigation stays synchronized.
- TXT/Markdown: make Unicode case handling consistent between the native full-file index and the in-WebView highlighter. Avoid counting Markdown syntax that is not visible, or clearly define and test the chosen search semantics.
- Add focused pure-Kotlin tests for indexing, bounds, encodings, and offset mapping, plus WebView/instrumented tests for highlighting and navigation.

### P1 — DOCX reader controls
- Keep zoom default at 100%, exact entry from 25% through 500%, reset to 100%, and verify zooming out reveals more page content rather than merely shrinking a clipped layer.
- Verify dark mode across text, tables, images, hyperlinks, highlights, page backgrounds, system-bar contrast, rotation, and print styles. Preserve document layout and ensure system bars/window flags restore when focus mode exits.

### P1 — print/export reliability for all four formats
- Exercise Android's actual PrintManager preview and Save as PDF for PDF, DOCX, TXT, and Markdown, checking all pages, page breaks, fonts, colors, images, and cancellation.
- Move expensive Markdown/DOCX formatting off the UI thread where safe. Keep file reads bounded and show a clear message if a file exceeds the supported print/export limit; do not silently print only a preview excerpt.
- Test on at least one real Android device or emulator and record OS/API level and observed result. Static helper inspection alone is not a print verification.

### P2 — reader polish and security
- Test focus mode in portrait/landscape and with display cutouts: content fills the full canvas, status/navigation bars have correct contrast, and previous window state is restored.
- Confirm no remote assets or new `INTERNET` permission; keep TXT/Markdown HTML escaped/sanitized, block untrusted WebView navigation, and retain bundled PDF.js/font/CMap notices.
- Add accessibility labels and TalkBack behavior for match counts, previous/next, code-block copy, zoom, and print actions.
- Measure preview memory and first-result latency on representative small, medium, and large documents; report device/JVM context and regressions.

## Verification and documentation
- Run `./gradlew testDebugUnitTest jacocoCoreVerify`, `./gradlew lintDebug detekt`, and `./gradlew assembleDebug` when executable and dependencies/SDK are available. Run relevant instrumented or Compose tests on a device/emulator. Record each exact command and result, including blocked commands.
- Update `docs/AGENTS.md`, `docs/TODO.md`, `docs/CHANGELOG.md`, `docs/ROADMAP.md`, `THIRD_PARTY_NOTICES.md`, and this prompt index. Do not invent benchmark numbers or claim a multi-agent run that did not happen.
- Keep loop scope restricted to PDF, DOCX, TXT, and Markdown. Leave other formats alone.

## Next-loop handoff
At the end of Loop 7, write the next versioned prompt in `docs/prompts/` with a filename that identifies its loop. Brainstorm the next highest-impact improvements for these same four formats, read the updated repository notes first, and include the limits, tests, measured results, and actual tools/models used. Preserve this handoff rule without claiming a team or model that was not actually used.
