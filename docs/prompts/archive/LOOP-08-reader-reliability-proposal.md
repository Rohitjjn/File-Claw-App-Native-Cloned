# FILE CLAW — LOOP 8 EXECUTION PROMPT
## Team plan: reader correctness, import/export safety, and performance evidence

### Mission
Continue from the current unmerged Loop 6/7 PR only after the dedicated Android verification workflow completes. Start by reviewing its actual logs and report artifacts. Fix reproducible failures before adding behavior. Do not claim tests pass without run evidence.

### Team roles
1. Android/Compose lead — immersive system-bar lifecycle, BackHandler, accessibility and state restoration.
2. Document-engine engineer — inspect installed PDF/DOCX renderer APIs and validate heading/page mapping with real fixtures.
3. Data-integrity engineer — atomic saves, backups, safe export names, cache/index consistency and permission failures.
4. QA/performance engineer — triage CI, measure large-file behavior and define device smoke tests.

### Category 1 — Reader navigation correctness
- Validate Markdown outline indexing for existing HTML ids, Unicode, duplicate headings, blockquotes, raw HTML, and mixed backtick/tilde fences.
- Verify DOCX outline jumps with ordinary paragraphs interleaved, duplicate heading titles, and Heading 1–6/Title/Subtitle styles. Match normalized text first and use ordinal fallback only when necessary.
- Investigate real PDF bookmark/outline support in the installed PDF dependency. If there is no supported API, retain a clearly labelled page-jump fallback instead of fabricating bookmarks.
- Validate search on PDFs with compressed streams, escaped literal strings, multiple text runs, Unicode/font encodings and page-order ambiguity. Keep the distinction between estimated stream index and exact physical page explicit.

### Category 2 — Save/import/export reliability
- Expand atomic writer tests for replace fallback, failures during write/flush, temp-file cleanup and backup behavior.
- Exercise Save As with Unicode filenames, dotfiles, trailing dots, extensionless names, path separators, existing requested names, existing copy siblings, read-only destinations and source aliases.
- Confirm failed saves never clear editor dirty state and only successful writes update recents, indexes and notifications.
- Test files opened from another app and ensure messaging distinguishes the in-app copy from writing back to the original URI.

### Category 3 — Performance and one focused quality improvement
- Profile large PDF, Markdown and DOCX opening/search on a mid-range Android device. Record document size, cold/warm time, memory pressure and scroll smoothness instead of inventing performance targets.
- Make one tightly scoped improvement in the strongest measured hotspot. Add a deterministic regression test and avoid unrelated refactors.
- Keep the CI workflow test-focused: preserve JUnit, JaCoCo, lint and debug APK artifacts; do not sign or publish releases from the test job.
- Update docs after observing CI artifacts; do not mark checks complete until green.

### Required protocol
1. Inspect the current PR branch head, workflow run, failing-step logs, relevant sources and tests.
2. Create feat/loop-8-reader-reliability from the verified current branch head; never force-push another agent's work.
3. Record a file-level plan, risk list and test matrix before editing.
4. Implement small commits, keep compatibility and skip unrelated cleanup.
5. Run :app:testDebugUnitTest, :app:jacocoCoreReport :app:jacocoCoreVerify, :app:lintDebug, :app:detekt and :app:assembleDebug. Run compile_applet only if the Gradle build defines it.
6. For every failure, record the task and root error, fix reproducible issues, rerun and summarize final output.
7. Update changelog/TODO and PR summary with run-artifact links. Do not merge or publish a release automatically.

### Acceptance criteria
- Required available CI checks are green and report artifacts are retained.
- Markdown and DOCX outline jumps land on intended headings using real fixtures.
- PDF navigation uses verified capabilities and clearly labelled fallbacks.
- Save/export behavior is regression-tested; simulated failures leave originals unchanged.
- Performance improvement is backed by repeatable measurements and a regression test.
- Documentation accurately reflects verified status and remaining risks.
