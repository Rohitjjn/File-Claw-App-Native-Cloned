/* jsdom harness for app/src/main/assets/reader/reader.js (Markdown/TXT reader + in-place editor). */
const fs = require('fs');
const { JSDOM } = require('jsdom');
const path = require('path');
const JS = fs.readFileSync(path.join(__dirname, '..', '..', 'app', 'src', 'main', 'assets', 'reader', 'reader.js'), 'utf8');

function jsonForScript(s) {
  return JSON.stringify(s).replace(/</g, '\\u003c').replace(/\u2028/g, '\\u2028').replace(/\u2029/g, '\\u2029');
}

/**
 * blocks: [{ src: 'markdown source of the block', html: 'rendered html', k: 'p' }]
 * seps: separators between blocks (default '\n\n'); lead / tail text.
 */
function mdPage(blocks, opts = {}) {
  const seps = opts.seps || [];
  let src = opts.lead || '';
  let body = '';
  blocks.forEach((b, i) => {
    if (i > 0) src += (seps[i - 1] !== undefined ? seps[i - 1] : '\n\n');
    const s = src.length;
    src += b.src;
    const e = src.length;
    if (b.k === 'code') {
      body += b.html.replace('<div class="code-block-container fc-b"', `<div class="code-block-container fc-b" data-b="${i}" data-s="${s}" data-e="${e}"`);
    } else {
      body += `<div class="fc-b" data-b="${i}" data-s="${s}" data-e="${e}" data-k="${b.k || 'p'}">${b.html}</div>`;
    }
  });
  src += (opts.tail !== undefined ? opts.tail : '\n');
  return build('md', src, body, opts);
}

function build(kind, src, body, opts = {}) {
  const html = `<!doctype html><html class="kind-${kind === 'md' ? 'md' : 'txt'}"><head>
<script>window.FC_CONFIG = ${JSON.stringify({ kind, tabSize: 4 })};</script>
</head><body><main id="document-content">${body}</main>
<script type="application/json" id="fc-source">${jsonForScript(src)}</script>
<script>${JS}</script></body></html>`;
  const calls = [];
  const dom = new JSDOM(html, {
    runScripts: 'dangerously',
    beforeParse(window) {
      window.scrollTo = () => {};
      window.scrollBy = () => {};
      window.FCBridge = new Proxy({}, {
        get(_, name) { return (...args) => { calls.push([name, ...args]); return undefined; }; },
        has() { return true; }
      });
    }
  });
  const w = dom.window;
  w.FC._test.init();
  return { dom, w, d: w.document, src, calls, FC: w.FC, T: w.FC._test, root: w.document.getElementById('document-content') };
}

function txtPage(src, segments) {
  // segments: computed like the Kotlin builder (plain only, for tests)
  let body = '';
  segments.forEach(seg => {
    if (seg.code) {
      body += `<div class="code-block-container fc-seg" data-s="${seg.s}" data-e="${seg.e}" data-cs="${seg.cs}" data-ce="${seg.ce}"><div class="code-header" contenteditable="false"><span class="code-lang">${seg.lang || 'Code'}</span><button class="copy-btn" type="button">Copy code</button></div><div class="code-content"><pre><code>${esc(src.slice(seg.cs, seg.ce))}</code></pre></div></div>`;
    } else {
      body += `<pre class="plain-text fc-seg" data-s="${seg.s}" data-e="${seg.e}">${esc(src.slice(seg.s, seg.e))}</pre>`;
    }
  });
  return build('txt', src, body);
}
function esc(s) { return s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;'); }

let failures = 0, passes = 0;
function eq(actual, expected, name) {
  if (actual === expected) { passes++; }
  else {
    failures++;
    console.log('FAIL: ' + name);
    console.log('  expected: ' + JSON.stringify(expected));
    console.log('  actual  : ' + JSON.stringify(actual));
  }
}
function ok(cond, name) { if (cond) passes++; else { failures++; console.log('FAIL: ' + name); } }
function done() { console.log(`\n${passes} passed, ${failures} failed`); process.exit(failures ? 1 : 0); }
module.exports = { mdPage, txtPage, build, eq, ok, done };
