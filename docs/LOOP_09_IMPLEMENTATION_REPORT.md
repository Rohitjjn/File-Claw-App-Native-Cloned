# Loop 9 Implementation Report

## Scope
Loop 9 is a narrow reliability follow-up for the existing PDF, DOCX, TXT and Markdown preview work. It does not introduce unrelated formats or redesign the file browser.

## Changes committed
- `app/src/main/java/com/example/utils/FilePrintHelper.kt`
  - TXT/Markdown printing now reports a missing print service and catches print-start exceptions.
  - Both the document body and title are HTML-escaped before they are interpolated into the print document.
  - A wrapper around the WebView's `PrintDocumentAdapter` forwards layout/write lifecycle calls and destroys the temporary WebView after `onFinish`, rather than disposing it immediately after opening the print sheet.
- `app/src/test/java/com/example/core/text/PreviewTextUtilsTest.kt`
  - Added cases for repeated Unicode matches and UTF-16 offsets, max-result limits, CRLF fenced blocks, embedded backticks and fence-length rules.
- `docs/TODO.md` and `docs/CHANGELOG.md`
  - Recorded Loop 9 work and kept CI/device verification explicitly pending.
- `docs/loops/LOOP_10_NEXT_LOOP_PROMPT.md`
  - Added the next evidence-led prompt for the same four document formats.

## Verification
- Latest Loop 8 CI was green on commit `64a482dd035fb95ffb2888e7f8407010f5a5d909`, including unit tests, core coverage, lint, Detekt and debug APK assembly.
- That result does **not** verify Loop 9. A fresh Android workflow run must pass on the final Loop 9 commit.
- No real-device testing has been performed by this implementation pass.
- Printing behavior still needs manual testing on Android, including repeated print jobs, cancellation and actual PDF saving from the system print UI.

## Acceptance status
- Source changes committed: yes.
- Loop 9 CI verified: pending.
- APK artifact for final Loop 9 commit: pending CI.
- Real-device smoke tests: pending user.
- PR merge: intentionally not performed.
