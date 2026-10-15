# FILE CLAW — LOOP 9 NEXT-LOOP PROMPT
## Four-format document reader: evidence-led refinement

This is a planning prompt for the next iteration after Loop 8 has been implemented and its CI results inspected. Keep scope strictly limited to PDF, DOCX, TXT and Markdown preview.

### First: review before proposing work
- Read the Loop 8 PR diff, CI artifacts, implementation report, TODO and device smoke-test notes.
- Run a short cross-functional review as separate passes: Android/Compose, PDF rendering, DOCX/WebView, search/navigation, print/export, accessibility/design, security/performance/QA.
- Build a defect list with reproduction steps, severity, affected format/device, evidence and regression test. Do not invent issues or mark an untested hypothesis as a finding.
- Prioritize any unresolved Loop 8 acceptance criterion before adding new capabilities.

### Candidate workstreams — choose only evidence-backed items
1. Search correctness and navigation: repeated/Unicode matches, normalized whitespace, match highlights, page/heading jumps, large-document latency.
2. Rendering fidelity: DOCX page fit/zoom and tables/images, Markdown fenced blocks and long lines, PDF night-mode behavior without photo/diagram damage.
3. Print/export reliability: pagination, Unicode fonts, image fidelity, resource cleanup, Android print service errors and export collision safety.
4. Focus mode and accessibility: system-bar/inset correctness, BackHandler, screen reader labels, font scaling and state restoration.
5. Large-document performance: measure memory, cold/warm opening time, search latency and scroll responsiveness on representative devices before optimizing.

### Rules
- Pick at most three tightly related workstreams, chosen by measured severity and user impact.
- No unrelated formats, file manager redesign, broad refactor, speculative dependencies or cosmetic change that regresses the restored TXT/MD UI.
- Preserve safe WebView defaults and existing file/data integrity behavior.
- Add deterministic regression tests for every repaired defect.
- Keep loop prompts numbered and named `LOOP_XX_*.md`; update the loop index/catalog if one exists.
- Use Hermes Agent only if executable and useful in the current environment; do not integrate it into the app runtime or claim an agent swarm ran unless verified.

### Verification
Run unit tests, JaCoCo core report/verification, lint, Detekt, debug APK build and applicable UI/instrumentation tests. Inspect workflow logs/artifacts. Record the exact commit SHA and run URL. Separate CI results from real-device smoke tests; never claim device verification without it.

### Delivery
Create a new branch from the verified Loop 8 head, write a file-level plan before changes, keep the PR draft until required CI is green, update changelog/TODO/report, and do not merge or publish automatically.
