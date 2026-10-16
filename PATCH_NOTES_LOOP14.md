# File Claw — PATCH NOTES · Loop 14

**Branch:** `feat/loop-14-docx-pdf-viewer-reliability` (from `main` @ `dd4681eeb5695ca3e50f08597381b26b3e61288b`, tag `v10.0.5`)
**Deliverable:** upload-ready patch ZIP — only changed files, repo-relative paths. APK build happens via GitHub Actions (`Build and Release Android App`, suggested `version_name` input: `1.3.3`). Nothing was merged; PRs #21/#22 untouched.

---

## 1. DOCX root cause (proven, not inferred)

**Symptom:** every `.docx` opened as plain extracted text ("Clean Document View"), never as a Word-like document.

**Trace:** `.docx` → `FormatDetector` (magic bytes → `word/document.xml` → DOCX, correct) → `DocxSuccess` → `DocxPreviewWebView` → `loadUrl("https://appassets.androidplatform.net/editor/docx_editor.html")` → **main frame 404** → `onReceivedError(isForMainFrame)` → `DocxExtractor.extractText()` → plain-text fallback. The renderer itself was never reached.

**Root cause:** the Loop 12 streaming change registered the editor under
`WebViewAssetLoader` prefix `/editor/`:

```kotlin
.addPathHandler("/editor/", WebViewAssetLoader.AssetsPathHandler(context))   // 4687
loadUrl("https://appassets.androidplatform.net/editor/docx_editor.html")     // 4875
```

androidx.webkit **strips the registered prefix** before calling the handler
(`WebViewAssetLoader.getSuffixPath` → `path.replaceFirst(prefix, "")`), and
`AssetsPathHandler` opens the suffix directly as the asset path. So the loader
tried `assets.open("docx_editor.html")` while the asset lives at
`editor/docx_editor.html` → 404 on every open, deterministic, every device.
Source of truth: androidx `WebViewAssetLoader.java` (documented example:
register `/assets/`, load `/assets/www/index.html` → opens asset `www/index.html`).

**Regression window:** introduced in the v1.3.2 upload (commit `7d11163`, Loop 12).
The last known good implementation was `file:///android_asset/editor/docx_editor.html`
(Loop 10 build, commit `482b5a4` — verified in git history).

**Fix (minimal, documented pattern — same as the PDF reader):**

```kotlin
.addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(context))
loadUrl("https://appassets.androidplatform.net/assets/editor/docx_editor.html")
// whitelist: /assets/editor/ + /docx-src/ (streaming path semantics unchanged)
```

**Second regression (found while testing, not user-visible yet):** the Loop 12
rewrite of `docx_editor.html` dropped the `maxSearchHighlights` declaration
while `searchWord()` still read it → every matching DOCX query threw
`ReferenceError` and find never reported a count. All five search globals are
declared again (cap 5000, as in Loop 10).

## 2. Browser end-to-end verification (what we COULD run here)

No Android SDK/JDK exists in the patch environment — Kotlin compile/tests/APK
are delegated to GitHub Actions (as in Loops 10/12). What WAS run, against real
files in real headless Chromium (python http.server + agent-browser; rig under
`/home/z/my-project/scripts/`, outside the repo):

| Check | Result |
|---|---|
| DOCX renders as Word-like page card (shadow, margins) | PASS (screenshot captured) |
| Table (3×2), bold run, 11 paragraphs laid out | PASS |
| `searchWord("the")` / `searchWord("north")` counts | PASS (4 / 1) |
| `nextSearchMatch()` navigation | PASS (2/4) |
| `setZoom(2.5)` CSS zoom commit + horizontal scroll | PASS |
| DOCX dark-mode toggle (kept for DOCX) | PASS |
| DOCX skeleton shows until `renderAsync` lands | PASS |
| PDF opens via pdf.js, page 1 rendered crisp | PASS |
| Pages 2–3 stay skeleton placeholders until approached | PASS (virtualization intact) |
| `setPageRotation(90)` → landscape 1256×970 from portrait 1256×1625 | PASS |
| Rotation preserves scroll position (no page-1 reset) | PASS |
| Highlight rects re-map per rotation (numeric check 0°/90°/180°) | PASS (after fixing the search-time-mapping draft bug) |
| `setPdfZoom(50)` clamps to 8 (elements scale to fit×8) | PASS |
| LRU release: offscreen canvases zeroed, skeleton reappears | PASS |
| `node --check` (search.html classic + module, docx_editor.html) | PASS ×3 |
| AndroidManifest XML parse | PASS |
| Brace/paren balance, all touched Kotlin files | PASS |

## 3. PDF changes (current design preserved)

- **Toolbar (exact Loop 14 spec):** 1 Page indicator/jump (unchanged) → 2 **Rotate PDF** (Search's old slot; `Icons.Outlined.RotateRight`, "Rotate PDF 90 degrees") → 3 **Search** (Night's old slot) → 4 Print (unchanged) → 5 Share (untouched). Same 36dp targets.
- **Rotation model:** pure viewer state (0/90/180/270). `PdfPreview` pushes `setPageRotation(deg)` into the **already-loaded** reader; pages re-render from the authenticated `PDFDocumentProxy` with `page.rotate + pageRotation` baked into the pdf.js viewport. Old canvases stay visible until the rotated replacements swap atomically; scroll preserved via page + within-page-fraction anchor; highlights stored unrotated, mapped at draw time. **No reload, no WebView recreation, no password re-prompt** (process death still legitimately re-prompts).
- **Device rotation lifecycle:** `MainActivity` now declares `configChanges` (orientation, screenSize, screenLayout, smallestScreenSize, keyboardHidden, keyboard, uiMode, density, locale, layoutDirection) — the Activity/WebView/zoom/password survive device rotation and theme flips (closes BUG-003's mechanism). Compose recomposes from the new configuration.
- **Scrubber:** right-edge rail + thumb (44dp hit target), drag bubble `n / total`, mapping via `PdfRotationPolicy.pageFromFraction`, jump on release through `gotoPdfPage` (+ `pageJumpToken` so re-jumping to the same page fires). Pages passed over are never eagerly rendered. Synchronized with the page observer/indicator/jump dialog. **No historical implementation exists in git** (verified via `-S` sweeps across all branches) — equivalent implemented per the loop prompt.
- **Dark mode removed:** toolbar + menu toggles (menu item now DOCX-only), `setPdfNightMode` bridge, `body.dark` CSS, per-page invert/natural classifier, `PdfDarkModePolicy.kt` + 14 tests — all deleted. PDFs always light; DOCX (`isDocxNightMode`), TXT, MD unaffected.
- **Skeletons:** opening state = page-shaped shimmer cards (no spinner-only screen); per-page shimmer until a real canvas swaps in; clears on ready/password/error/retry/file change/renderer death.
- **8× zoom:** `maximum-scale=8` + JS clamps 8. Backing still clamped by the total RGBA budget, ratio cap 8 and 8192px dimension cap; resident LRU (3) untouched — memory stays bounded by design.

## 4. Files in the patch (repo-relative)

```
AndroidManifest.xml                                          (modified)
app/src/main/assets/editor/docx_editor.html                  (modified)
app/src/main/assets/pdfjs/search.html                        (modified)
app/src/main/java/com/example/core/format/PdfRotationPolicy.kt        (new)
app/src/main/java/com/example/core/format/PdfDarkModePolicy.kt        (deleted)
app/src/main/java/com/example/ui/screens/DocumentPreviewSupport.kt    (modified)
app/src/main/java/com/example/ui/screens/FilePreviewScreen.kt         (modified)
app/src/test/java/com/example/core/format/PdfRotationPolicyTest.kt    (new)
app/src/test/java/com/example/core/format/PdfDarkModePolicyTest.kt    (deleted)
app/src/test/java/com/example/ui/screens/DocxTextRecoveryTest.kt      (new)
changelogs.md / known-issues.md / docs/TODO.md / worklog.md /
docs/prompts/NEXT_LOOP_PROMPT.md                             (modified)
PATCH_NOTES_LOOP14.md                                        (new, this file)
```

Deleted files: when applying this ZIP via GitHub web upload, **delete
`PdfDarkModePolicy.kt` and `PdfDarkModePolicyTest.kt` manually** (web upload
cannot delete; use the GitHub file UI → "Delete file", or `git rm`).

Tests: 199 baseline − 14 removed + 16 (`PdfRotationPolicyTest`) + 4
(`DocxTextRecoveryTest`) = **205 expected in CI**.

## 5. Upload → build → verify (user steps)

1. Create branch `v1.3.3` from `main`.
2. Upload the ZIP contents to that branch **and delete the two PdfDarkModePolicy files**.
3. Actions → **Build and Release Android App** → `version_name=1.3.3` → Run.
4. Install the APK, then run the smoke list below.

**Device smoke checklist**
- [ ] DOCX (storage + inside zip): opens as formatted pages — NOT plain text; tables/images/lists render; find bar counts and navigates; pinch/±/percent/double-tap zoom; malformed DOCX shows the honest recovery banner with a working "Retry formatted view".
- [ ] PDF toolbar reads Page indicator · Rotate · Search · Print · Share.
- [ ] Rotate on a password PDF (middle page, active search): no re-prompt, page/anchor/highlights/zoom preserved, no blank flash; repeat through 90/180/270/0.
- [ ] Scrubber: drag first→last→first on a 500+ page file; bubble matches thumb; release jumps; `dumpsys meminfo` flat during drag.
- [ ] PDF under Android system dark mode stays fully light; DOCX dark mode still toggles.
- [ ] Pinch to 8×: sharp text (ratio-capped 1:1), horizontal + vertical pan, bounded memory (`dumpsys meminfo` before/after), no blank pages.
- [ ] configChanges sweep: rotate device on Home/Preview/Edit screens, toggle system theme, split-screen — no state loss, no crashed WebView.
- [ ] Loop 10/12 regressions still green: header find bar, IME edit tray, back-closes-find, search arrows 1→N both directions, heavy-PDF memory, renderer-crash recovery.

## 6. Known limitations (honest)

- Kotlin compile/tests/APK: **not run here** (no SDK/JDK) — CI must confirm 205 tests green.
- Rotation anchor restores the same relative position within the anchored page; exact pixel-perfect anchoring across orientation swaps is an approximation by design.
- The scrubber overlays the right margin; on very narrow screens with landscape pages the rail may overlap page edges slightly (still usable; feedback welcome).
- `configChanges` is the widest lifecycle change shipped so far — the device sweep above matters.
