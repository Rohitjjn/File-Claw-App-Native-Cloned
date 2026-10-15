# FILE CLAW — LOOP 8 EXECUTION PROMPT
## Premium document preview corrections — PDF, DOCX, TXT, Markdown only

### Mission
Continue on branch `feat/loop-8-document-preview-polish`. Work ONLY on preview, search, focus, print/export, zoom and theme behavior for `.pdf`, `.docx`, `.txt`, and `.md`. Do not redesign unrelated file viewers or change business logic outside these paths. Preserve the existing unmerged/merged Loop 6–7 work and make small reviewable commits.

### Team structure (parallel review, one integrating owner)
1. **Android/Compose lead:** focus-mode lifecycle, edge-to-edge system bars, insets, BackHandler, accessibility and screen restoration.
2. **Document rendering specialist:** PDF renderer APIs, DOCX WebView/docx-preview renderer, text/Markdown rendering and large-document constraints.
3. **Search engineer:** document-wide indexing/search, case/whitespace normalization, next/previous navigation, match counts and safe scrolling.
4. **Export engineer:** Android PrintManager adapters, PDF print/share, DOCX render-to-print, HTML escaping, resource cleanup and errors.
5. **UI/visual design reviewer:** restore the TXT/MD appearance from two loops before the regression, then improve polish without changing established layout unnecessarily.
6. **QA/security reviewer:** unit tests, offline/large-file tests, WebView safety, memory leaks and device smoke-test matrix.

Do not claim to have 50 active agents unless real independent agent runs occurred. Use Hermes Agent from https://github.com/NousResearch/hermes-agent as a reference/orchestration option only if the current environment can run it; do not add it as an Android runtime dependency. If the environment cannot execute agents, run the roles as separate review passes and disclose that limitation.

### Strict scope — the seven reported issues
1. **Search across entire document:** The search affordance must open a working search field for PDF, DOCX, TXT and MD. Search the whole logical document, not only visible text or the current page. Implement previous/next, current/total count, clear/empty states, Unicode and case-insensitive matching. Selecting a result must navigate to/highlight the actual occurrence or its page/section. PDF may use verified renderer search APIs; DOCX should search rendered text or extracted paragraphs and map results to real DOM nodes; TXT/MD must navigate the actual preview. Do not show fake counts.
2. **Restore TXT/MD UI regression:** Inspect git history and identify the exact version from two loops before the recent regression. Restore its layout and reading feel first. Keep working accessibility, large-file performance, search and appearance options. Do not replace it with an unverified visual guess. Document the source commit used.
3. **Copy individual fenced code blocks:** In Markdown and TXT previews, every real triple-backtick fenced block should have a small, consistent Copy control in that block's own preview card. Copy only the inner code text, excluding the opening/closing fences and surrounding prose. Preserve indentation, Unicode and trailing newlines. Provide success/error feedback. For TXT, detect fenced blocks without treating ordinary inline backticks as whole blocks.
4. **Functional Print and Export to PDF:** Provide real print/export paths for all four formats. PDF should print/share the original PDF or invoke its verified print adapter, not print its path as text. DOCX must print the rendered document, retaining page layout as far as the renderer supports. TXT/MD should create a readable paginated print document. Handle missing print services, render failures and temporary WebView cleanup. Never silently report success when Android only opened the system print sheet.
5. **Immersive focus mode:** Preview must use the available window height and width, including the status-bar area; remove the black strip at the top. Keep status/navigation icons legible, restore system bars on exit/disposal, and retain a subtle floating Back control plus an unobtrusive exit-focus affordance. Test PDF, DOCX, TXT and MD on gesture and three-button navigation. Avoid hiding bars globally for other screens.
6. **DOCX zoom and dark mode:** Provide predictable fit-to-width/fit-page and pinch zoom, including zooming out enough to see the whole page/document where feasible. Avoid clipping, horizontal overflow and broken scrolling. Theme changes must re-style existing rendered pages, table cells, paragraphs and inherited/inline text without requiring reopening. Preserve images and meaningful document structure; don't blindly invert photos.
7. **PDF dark mode:** Replace the private-field reflection/color-filter workaround with the installed PDF viewer's supported night-mode behavior where possible. The result should resemble a reading app: dark page background and comfortable text while preserving diagrams/photos. If the renderer cannot recolor scanned/image-only PDFs without damaging images, explicitly document that limitation and offer a safe alternative. Do not claim exact Google Drive parity without comparison testing.

### Engineering requirements
- Inspect the latest branch, prior commits, the current implementation, and existing tests before editing.
- Create a file-level plan and risk/test matrix first; avoid unrelated refactors.
- Prefer installed libraries and verified APIs. Any added dependency must be justified by compatibility, offline behavior, APK size, license, and maintenance.
- Keep WebView content safe: retain HTML sanitization, disable unnecessary JavaScript/file access, escape injected values, and never enable universal file access merely to make a preview work.
- Keep large-file handling bounded; do not duplicate huge document payloads into multiple strings or Base64 copies unnecessarily.
- Add regression tests for search normalization and match navigation, fenced-block extraction/copy payloads, export input routing and failure cleanup, DOCX theme/zoom behavior where testable, and focus-state restoration.
- Do not change any unrelated file-type preview.

### Verification — required
Run:
```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:jacocoCoreReport :app:jacocoCoreVerify
./gradlew :app:lintDebug
./gradlew :app:detekt
./gradlew :app:assembleDebug
```
Also run available instrumentation/UI tests. Add a dedicated workflow if CI cannot exercise relevant tests, but don't duplicate the existing Android verification workflow unnecessarily. Record actual run links, final APK artifact, test counts and failures. If local execution is impossible, trigger/inspect GitHub Actions and state what is still unverified.

### Manual smoke matrix
- PDF: 1-page and 100+ page searchable PDF, scanned PDF, image-rich PDF, malformed/password-protected PDF; search first/middle/last pages; print/export; dark/light; focus mode.
- DOCX: multi-page text, headings, tables, inline formatting, images, wide pages; search occurrences; fit width/page; pinch zoom; switch theme without reopening; print/export.
- TXT: small and large files, repeated/Unicode matches, fenced code with indentation and embedded backticks; search navigation, copy only a fenced block, print/export.
- MD: headings, links, lists, tables, multiple fenced code blocks and long code lines; search navigation, block copy, appearance options, print/export.
- Android UI: gesture navigation and 3-button navigation, rotation/background/return, light and dark system themes.

### Required docs and delivery
- Update `docs/CHANGELOG.md`, `docs/TODO.md`, and release/build documentation only where applicable.
- Keep each loop prompt as a separate, numbered file under `docs/loops/`; filenames must begin `LOOP_XX_` and match the loop number. Move/copy existing prompts only after verifying their contents and links; do not delete historical prompt material.
- Create a final implementation report containing changed files, tests/run links, APK artifact, known limitations and manual checks still required.
- Open a draft PR to `main`; do not merge or publish a release automatically.
- After Loop 8 verification, conduct a cross-functional brainstorming/review pass and write `docs/loops/LOOP_09_NEXT_LOOP_PROMPT.md`. Loop 9 must stay within these same four formats and be based on remaining verified defects, not speculative features.

### Acceptance gate
Do not mark a requirement complete until the code path and relevant test or smoke evidence exist. Distinguish implemented, automated-tested, CI-verified, and manually device-verified status.
