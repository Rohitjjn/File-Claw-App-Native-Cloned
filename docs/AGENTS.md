# AGENTS.md: how the improvement loop works

This file lets the next run resume with full context. It is the operating manual for the repository's improvement loop; versioned prompt history and the next prompt are indexed in `docs/prompts/README.md`.

## Roles and truthful tool use
Architect · Reader/Analyzer · Planner · Coder · Designer · FileFormat · QA/Tester · Reviewer · DevOps · Docs · Release are useful review perspectives, not agents that are automatically present. Do not claim a multi-agent team ran unless an actual runner produced verifiable work. The user requested Hermes Agent from `https://github.com/NousResearch/hermes-agent`; it was inspected but was **not cloned, installed, or run** in Loop 6. Use it in a later loop only if a configured runner/provider is actually available. Do not request or store credentials in chat.

Arena Agent Mode did not expose the underlying model identifier in this session. Report that limitation; do not attribute this work to a guessed model such as Claude Sonnet 5 or claim Hermes used a model.

## State to read on every run (in this order)
`docs/AGENTS.md` → `docs/TODO.md` (KNOWN ISSUES first) → `docs/CHANGELOG.md` → `docs/ROADMAP.md` → `docs/ARCHITECTURE.md`, `docs/DESIGN.md`, `docs/SECURITY_AUDIT.md`, `docs/SANITY_CHECKLIST.md` → `docs/prompts/README.md` → source.
Never recreate docs; update them. Add a CHANGELOG entry with **measured** numbers and explicitly label unverified work.

## Commands
```
./gradlew testDebugUnitTest jacocoCoreVerify   # tests + 80% core coverage gate
./gradlew lintDebug detekt                      # static analysis
./gradlew assembleDebug                         # APK
kotlinc + tools/bench/CoreBench.kt              # JVM benchmarks (old vs new)
kotlinc + tools/GenFormatDocs.kt                # regenerate the format matrix
```

## Environment lessons from cycle 1 (important for the next run)
- The authoring sandbox had **no Android SDK and no Maven/Google/Gradle-distribution access**, only GitHub. Gradle could not run there. If the
  next run has the same limits, repeat the strategy: keep new logic in `core/` (pure Kotlin), compile with a GitHub-sourced `kotlinc`, run JUnit tests,
  measure with the JaCoCo CLI, and clearly label everything else *not verified*. If the next run **can** run Gradle, do the Quality Gate first and
  fix compile fallout before adding features.
- `/bin/sh` does not brace-expand: use `bash -c` or separate `mkdir` args.
- Kotlin supports nested block comments: never write `*/` or `/*` inside a KDoc (e.g. `image/*`).
- `InputStream.readNBytes` is not available below API 33; `java.nio.file` is fine on API 26+.
- Byte signature checks must mask with `and 0xFF` before comparing (signed-byte bug found by tests).
- **Loop 6 environment:** `java` and `kotlinc` were not installed; `ANDROID_HOME` was unset. `gradlew` is tracked without its executable bit (`100644`); invoking it through `bash gradlew testDebugUnitTest --no-daemon` reached the wrapper but failed because Java was unavailable. Do not treat the Gradle suite as run.
- Node.js 22.22.3 was available. Extracted PDF.js/DOCX/preview JavaScript and bundled PDF.js module syntax checks passed; these do not verify WebView or Android runtime behavior.

## Decisions taken without asking (cycle 1)
1. Excel became **view-only** (editing flattened workbooks); CSV/TSV keep editing with explicit save.
2. HTML preview scripts default **off** with a per-file opt-in.
3. RAR stays unsupported (previous owner dropped junrar), but reported honestly.
4. Files opened from other apps are edited as an in-app copy (with a visible message) until write-back is built.
5. Glass design shipped as tokens + cards + app bar + permission screen; root-level backdrop deferred because opaque Scaffolds + NavHost crossfades would overlap.
6. Gradle wrapper moved 8.5 → 8.7 (AGP 8.4.2 needs ≥ 8.6).
7. Preview cap kept at 250k chars (unmeasured perf), made safe via read-only flag rather than raising it.

## Loop 6 decisions (PDF, DOCX, TXT, Markdown only)
8. Large TXT/Markdown previews stay bounded to the existing preview window. Full-file search streams the file, counts all matches, stores at most 10,000 navigable byte offsets, and loads a bounded excerpt at the selected result; it does not retain the entire document in memory. The complete large-file viewer/pager remains a Loop 7 goal.
9. TXT/Markdown print and copy use a bounded 2,000,000-character load for truncated files; larger files fail with a visible message rather than silently printing/copying only the preview excerpt. Small, already-loaded files use the detected source charset.
10. Bundled PDF.js 4.10.38 is served locally through `WebViewAssetLoader`; no remote PDF assets were added. Its Apache, CMap, and font notices are retained and listed in `THIRD_PARTY_NOTICES.md`.
11. DOCX needs file access only to load bundled `android_asset/editor/` resources; cross-file and content access are disabled, and non-editor top-level navigation is blocked. PDF.js uses its own local asset path and the exact PDF file path.
12. Current work is scoped only to PDF, DOCX, TXT, and Markdown. The next prompt remains within those four formats.

## Loop 7 decisions (Markdown / TXT, user-directed)
13. Markdown and TXT are edited **in place on the preview screen**; the separate editor screen remains for other text types. Edit mode reuses the preview page, so it looks the same (no line numbers).
14. Markdown editing is WYSIWYG with **source-preserving save**: each rendered top-level block carries `data-b/data-s/data-e`; untouched blocks are written back from the original source byte for byte and only edited/new blocks are converted to Markdown (`assets/reader/reader.js`). Front matter and raw HTML blocks are non-editable islands. A Markdown source mode is available from the edit menu.
15. Line endings are restored line by line: unchanged lines keep their exact terminator, changed lines use the file's dominant one.
16. Undo/redo is a snapshot history in the page (max 100 steps / ~12M chars, original always kept). It is not the browser's native undo stack; formatting uses `execCommand`.
17. Drafts survive activity recreation via `MainViewModel.inlineEditDraft` (memory only). Process-death recovery is a Loop 8 item.
18. Viewer appearance is one global setting for MD and TXT (SharedPreferences `reader_appearance`), applied with CSS variables and CSS `zoom` (25–500 %).
19. System bar icon colour follows the app theme (`isAppInDarkTheme`), not the phone's night mode.

## Loop 7 environment lessons
- Still no Java/Android SDK in the sandbox. **GitHub Actions is the compiler**: push to the `arena/**` branch and `.github/workflows/android-ci.yml` runs jsdom tests, unit tests, the Kotlin→jsdom save round trip and debug/release assembly. Workflow-file pushes were accepted with the provided token.
- Job logs live on `results-receiver.actions.githubusercontent.com`, which the sandbox cannot reach. The workflow therefore turns Kotlin errors (`e: ...`), Gradle "What went wrong" lines, failed tests (from JUnit XML) and round-trip failures into `::error::` annotations; read them with `gh api repos/<owner>/<repo>/check-runs/<job-id>/annotations`.
- The first CI run exposed two pre-existing Loop 6 test problems (a compile error and a failing overlap assertion) that had kept the unit-test suite from ever running; both were fixed.
- jsdom (npm) works in the sandbox for DOM-level tests; it has no `execCommand`, `caretRangeFromPoint`, layout or IME, so those paths still need a device.
- Icon availability was checked against the Compose 1.6 icon list (`androidx/androidx` at commit `7b4652f`, `compose/material/material/icons/generator`), since Maven Google is unreachable.

## Release checklist
Version bump (`app/build.gradle.kts`) · CHANGELOG · TODO/ROADMAP · regenerate FILE_FORMATS · SANITY_CHECKLIST run · zip + `PATCH_NOTES.md` · next-loop prompt.
