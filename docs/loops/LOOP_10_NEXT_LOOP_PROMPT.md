# FILE CLAW — LOOP 10 NEXT-LOOP PROMPT
## Four-format preview: device-evidence follow-up

### Mission
Continue only after Loop 9's Android CI result has been inspected and the user has tested the latest APK where possible. Scope remains strictly PDF, DOCX, TXT and Markdown preview/search/focus/print/export.

### First: collect evidence
- Read `docs/AGENTS.md`, `docs/TODO.md` (KNOWN ISSUES first), `docs/CHANGELOG.md`, Loop 8/9 prompts, the current PR diff and latest CI artifacts.
- Collect the user's real-device observations, Android version/navigation mode, file samples (without sensitive content), expected result, actual result and reproduction steps.
- Treat CI and phone testing as separate evidence. Never mark device behavior verified without a reported/recorded device test.
- Inspect any Loop 9 regression, especially temporary WebView print lifecycle, title/content HTML escaping, Unicode search offsets and fenced-code edge cases.

### Scope: select at most three evidence-backed items
1. Search correctness/navigation in PDF, DOCX, TXT and Markdown: counts must match the renderer's real searchable content; previous/next must reach the intended occurrence; disclose renderer limitations instead of presenting heuristic results as exact.
2. Print/export reliability: verify pagination and Unicode, temporary WebView lifetime/disposal, unavailable print services, repeated print jobs, cancellation and safe failure feedback.
3. Focus/appearance/rendering: verify edge-to-edge/insets, Back handling, state restoration, DOCX zoom/theme updates and PDF night-mode behavior using text-heavy and image-heavy samples.

Only select workstreams with a reproducible defect or measurable evidence. Do not add unrelated formats, file-manager redesign, broad refactors or speculative dependencies. Preserve the restored native TXT/MD layout and safe WebView defaults.

### Engineering requirements
- Write a file-level plan and risk/test matrix before edits.
- Add deterministic regression tests for each repaired defect; include Unicode, repeated matches, long documents, CRLF, fenced code and failure cleanup where relevant.
- Keep temporary Android resources scoped and released at the correct lifecycle boundary; don't destroy a WebView before Android's print adapter completes.
- Don't report successful export merely because the system print sheet opened.
- Avoid large-document memory duplication and keep error states user-visible.
- Use Hermes Agent only if executable and useful; do not claim parallel agent runs unless they actually happened.

### Verification gate
Run and inspect the latest commit's GitHub Actions:
```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:jacocoCoreReport :app:jacocoCoreVerify
./gradlew :app:lintDebug
./gradlew :app:detekt
./gradlew :app:assembleDebug
```
Use the existing Android workflow; don't add a duplicate workflow unless a demonstrated coverage gap requires one. Record exact SHA, run URL, job conclusions, artifact names and test failures. Add UI/instrumentation tests if available. If local Android execution is unavailable, state that plainly.

### Manual test matrix
- PDF: text-searchable, scanned/image-only, image-rich, multi-page, malformed/password-protected; search first/middle/last page; print/export; light/dark; focus.
- DOCX: multiple pages, headings, tables, inline formatting, images, wide page; repeated/Unicode search; zoom/fit; theme switch without reopening; print/export.
- TXT: repeated/Unicode matches, CRLF, large file, fenced code with indentation and embedded backticks; copy exact code; print/export.
- Markdown: headings, lists, tables, links, multiple code fences and long lines; search/navigation, copy, appearance and print/export.
- Android: gesture and 3-button navigation; rotation/background/return; system light/dark; repeat print jobs and cancellation.

### Delivery
- Update TODO, changelog and implementation report with verified/unverified status.
- Keep the PR draft until all applicable CI gates pass and acceptance gaps are clearly reported.
- Do not merge or publish automatically. The user performs final phone testing and decides when to merge.
