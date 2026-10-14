# FILE CLAW — LOOP 9: FOUR-FORMAT READER POLISH
## Next-loop brainstorming + execution prompt

### Scope
Only PDF, DOCX, TXT and Markdown preview/search/navigation/print/zoom/accessibility. Do not change any other file type. Begin by reading Loop 8 PR feedback, CI logs and actual artifact reports; fix confirmed regressions first.

### Team brainstorming priorities
1. **PDF search quality:** show a result list/snippet with true physical page number, highlight the exact occurrence in the selectable text layer, navigate previous/next, preserve current zoom/page position, and clearly distinguish text PDFs from image-only scans.
2. **DOCX fit and fidelity:** add an explicit Fit width / Fit page control, test portrait/landscape pages, wide tables, headers/footers and embedded images, preserve zoom and scroll when toggling dark mode, and expose accessibility-friendly page navigation.
3. **TXT/Markdown reading continuity:** preserve scroll position and current match when toggling rendered/source view; ensure Markdown tables/nested lists/Unicode and fenced blocks are stable; distinguish copy-block from copy-all actions.
4. **Print/export confidence:** add a predictable print preview/title, ensure WebView readiness and adapter lifetime, offer clear failure status, and verify PDF page ranges. Never print binary bytes through a text path or silently print empty content.
5. **Accessibility/premium UX:** TalkBack descriptions, minimum touch targets, keyboard/DPAD search navigation, high-contrast dark/light palettes, large font sizes and unobtrusive focus-mode controls.
6. **Performance/security:** measure search/open latency on large files; allow cancellation/progress for expensive extraction; keep untrusted HTML scripts disabled and block unsafe navigation; document dependency licenses.
7. **Regression and observability:** Compose/UI tests if supported, stable pure Kotlin fixtures, build artifacts, actionable failure messages, and truthful performance reporting rather than invented benchmarks.

### Team model and Hermes
Use the 10 × 5 squads (Architecture, PDF, DOCX, TXT/Markdown, Search, Print, Visual/accessibility, Performance/security, QA, Docs/release) described in Loop 8. Use [Hermes Agent](https://github.com/NousResearch/hermes-agent) only if installed/configured; verify actual concurrency and tools. An assigned 50-role matrix is not evidence of 50 running agents. One integrator must own changes to shared viewer files.

### Execution protocol
1. Read `docs/prompts/README.md`, current Loop 8 PR/CI, `docs/TODO.md` and relevant source first.
2. Create `feat/loop-9-four-format-reader-polish` from the verified current head.
3. Publish file-level plan, risks and test matrix before implementation.
4. Implement only reproducible/user-visible improvements in small commits.
5. Run unit tests, JaCoCo coverage, lint, detekt and debug APK assembly; run `compile_applet` only if defined. Link the exact workflow and report artifacts.
6. Device-test PDF search highlights/navigation, DOCX fit/dark mode, TXT/MD isolated fence copy, immersive insets and print/export for all four formats.
7. Update changelog/TODO/prompt index and open a draft PR; do not merge or publish a release.

### Acceptance criteria
- Search results/counts/nav agree for multi-match and one-character queries.
- DOCX fit/dark changes do not reopen or lose location.
- TXT/Markdown block copy copies exactly the requested block and no surrounding content.
- PDF night rendering remains legible for text, photos and scans.
- Print output is non-empty, readable and page counts/ranges are honest.
- Build/test evidence and documentation correspond to the exact latest branch head.
