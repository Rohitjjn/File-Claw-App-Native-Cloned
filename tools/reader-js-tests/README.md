# Reader runtime tests

Tests for `app/src/main/assets/reader/reader.js`, the script inside the Markdown/TXT reader page
(search, navigation, appearance and the in-place editor, including source-preserving Markdown save).

```sh
cd tools/reader-js-tests
npm install
npm test
```

They run in jsdom, so they cover DOM logic and serialization. Real typing, IME, `execCommand`
and layout behaviour still need a device or emulator (see docs/SANITY_CHECKLIST.md).
