# FILE CLAW — LOOP 7 EXECUTION PROMPT
## Team plan: semantic outline navigation, reliability and test hardening

### Mission
Review the Loop 6 diff and run build/test gates before extending behavior. Treat Loop 6 as unverified until real Gradle output is captured. Fix reproducible build failures first. Do not merge automatically or overwrite another agent's work.

### Agent roles
1. Android/Compose lead: lifecycle/insets, BackHandler, accessibility, recomposition and scroll preservation.
2. Document-format engineer: verify actual PDF viewer APIs and DOCX/OOXML heading rendering. Prefer real PDF bookmarks over guessed outline entries.
3. Reliability engineer: atomic save/export, filename collisions, cache/index refresh and failure handling.
4. QA/release engineer: tests/build/lint, regression cases, diff audit and truthful docs.

### Category 1 — Semantic outline/navigation
- Upgrade PDF page-list fallback to actual PDF bookmark entries only if the installed PDF dependency exposes a verified API; otherwise retain explicit page navigation as a transparent fallback.
- Ensure DOCX Heading 1–6, Title and Subtitle items jump to the correct rendered DOM heading even when ordinary paragraphs appear between headings. Confirm semantic heading ordinal maps to DOM heading ordinal.
- Test Markdown Unicode, duplicate headings, raw HTML, blockquotes, tilde fences and fenced code. Preserve zoom and reading position when changing outline target.
- Empty states must be accurate; never infer headings from capitalization or paragraph length.

### Category 2 — Reliability and performance
- Test AtomicFileWriter and Save As for write failures, unwritable directories, original-path collisions, existing copy filenames, Unicode filenames and repeated exports.
- Verify successful saves refresh recent-file metadata and the search index once; failed saves must not report success or clear dirty editor state.
- Inspect large Markdown/code WebView theme changes for unnecessary reloads, scroll resets and excessive HTML regeneration.
- Validate DOCX zoom down to 25 percent on a real device, then zoom back in without clipping or layout errors.
- Only fix DOCX page count using a verified renderer signal; never fabricate a total.

### Category 3 — Tests, docs, and one bounded code-quality improvement
- Add deterministic tests for Markdown heading edge cases, DOCX heading target mapping, PDF page fallback and atomic save/export collision cases.
- Update docs only after checking actual code and recorded test/build output.
- Inspect app/src/main/java/com/example/core/text/PdfTextSearcher.kt and its test file. If a supported issue is found, harden whitespace normalization across PDF text operators and add line-break, empty-query and case-insensitive tests. Keep the change limited to those one or two files; explain evidence or skip speculative fixes.
- Avoid broad refactors and do not touch vendored/minified JS without a proven defect.

### Required protocol
1. Inspect status, branch/head, Loop 6 diff, Gradle wrapper, CI workflows and installed PDF/DOCX APIs.
2. Create feat/loop-7-semantic-outline from the verified Loop 6 branch head. Never force-push or reuse another agent's branch.
3. Before editing, record a concise file plan, compatibility risks and test matrix.
4. Implement small reviewable commits; leave unrelated cleanup out of scope.
5. Run and report exact outputs for ./gradlew :app:testDebugUnitTest, ./gradlew testDebugUnitTest if configured, ./gradlew compile_applet only if the harness defines it, ./gradlew assembleDebug, and relevant lint/static checks.
6. If tooling/network blocks a command, report the actual error. Do not claim a pass for an unrun command; repair all reproducible failures.
7. Update docs and open a draft PR with user-visible changes, files changed, risks, exact command outcomes and remaining issues. Do not merge.

### Acceptance criteria
- PDF uses real bookmarks where available and an explicitly labelled page fallback otherwise.
- Markdown and DOCX outlines jump accurately and avoid false headings.
- Saves and exports do not corrupt data or overwrite unrelated copies; successful edits refresh history/index metadata.
- Appearance settings persist without avoidable scroll resets.
- Tests, builds, changelog and TODO agree with actual evidence.
- Any adjacent quality fix remains small, evidence-based and regression-tested.
