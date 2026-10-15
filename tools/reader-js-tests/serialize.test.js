const { mdPage, txtPage, build, eq, ok, done } = require('./harness');

const CODE = (lang, body) => `<div class="code-block-container fc-b" data-k="code"><div class="code-header" contenteditable="false"><span class="code-lang">${lang || 'Code'}</span><button class="copy-btn" type="button">Copy code</button></div><div class="code-content"><pre><code${lang ? ` class="language-${lang}"` : ''}>${body}</code></pre></div></div>`;

const BASE = [
  { src: '# Title', html: '<h1 data-sp="0:7">Title</h1>', k: 'h' },
  { src: 'Some __odd__ *formatting* kept   as-is.', html: '<p>Some <strong>odd</strong> <em>formatting</em> kept   as-is.</p>', k: 'p' },
  { src: '* one\n* two', html: '<ul><li>one</li><li>two</li></ul>', k: 'list' },
  { src: '```js\nconst a = 1;\n```', html: CODE('js', 'const a = 1;\n'), k: 'code' },
  { src: '> quoted *text*', html: '<blockquote><p>quoted <em>text</em></p></blockquote>', k: 'quote' },
  { src: '| a | b |\n|---|:-:|\n| 1 | 2 |', html: '<table><thead><tr><th>a</th><th align="center">b</th></tr></thead><tbody><tr><td>1</td><td align="center">2</td></tr></tbody></table>', k: 'table' },
  { src: '***', html: '<hr />', k: 'hr' },
  { src: 'Last [link](https://example.com) here.', html: '<p>Last <a href="https://example.com">link</a> here.</p>', k: 'p' }
];

// 1) untouched document round-trips byte for byte
{
  const p = mdPage(BASE);
  ok(p.FC.editor.enter('rich'), 'enter rich');
  eq(p.FC.editor.serialize(), p.src, 'unchanged rich doc is byte-identical');
  // wrappers were unwrapped onto single elements
  eq(p.root.children[0].nodeName, 'H1', 'single-element wrapper unwrapped');
  eq(p.root.children[0].getAttribute('data-b'), '0', 'data-b moved to element');
  eq(p.root.children[0].id, 'title', 'heading id assigned');
}

// 2) editing one paragraph keeps every other block byte-identical
{
  const p = mdPage(BASE);
  p.FC.editor.enter('rich');
  const last = p.root.children[7];
  last.firstChild.nodeValue = 'Final ';
  const out = p.FC.editor.serialize();
  ok(out.includes('Some __odd__ *formatting* kept   as-is.'), 'clean block preserved verbatim');
  ok(out.includes('* one\n* two'), 'clean list preserved verbatim');
  ok(out.includes('Final [link](https://example.com) here.'), 'edited paragraph re-serialized: ' + out);
  ok(out.includes('***'), 'clean hr verbatim');
  eq(out.split('\n\n').length, p.src.split('\n\n').length, 'same number of blocks');
}

// 3) splitting a paragraph (browser clones attributes) never duplicates source
{
  const p = mdPage([{ src: 'Alpha beta', html: '<p>Alpha beta</p>' }, { src: 'Next', html: '<p>Next</p>' }]);
  p.FC.editor.enter('rich');
  const para = p.root.children[0];
  const clone = para.cloneNode(false);
  clone.textContent = 'beta';
  para.textContent = 'Alpha';
  para.parentNode.insertBefore(clone, para.nextSibling);
  eq(p.FC.editor.serialize(), 'Alpha\n\nbeta\n\nNext\n', 'split paragraph');
}

// 4) deleting a block merges the surrounding gaps; link definitions in gaps survive
{
  const p = mdPage([
    { src: 'First', html: '<p>First</p>' },
    { src: 'Middle', html: '<p>Middle</p>' },
    { src: 'Last [x][ref]', html: '<p>Last <a href="https://r.example">x</a></p>' }
  ], { seps: ['\n\n[ref]: https://r.example\n\n', '\n\n'] });
  p.FC.editor.enter('rich');
  p.root.removeChild(p.root.children[1]);
  eq(p.FC.editor.serialize(), 'First\n\n[ref]: https://r.example\n\nLast [x][ref]\n', 'deleted block + link definition kept');
}

// 5) new paragraph after a heading separated by a single newline gets a blank line
{
  const p = mdPage([{ src: '# Head', html: '<h1>Head</h1>' }, { src: 'Body', html: '<p>Body</p>' }], { seps: ['\n'] });
  p.FC.editor.enter('rich');
  eq(p.FC.editor.serialize(), '# Head\nBody\n', 'clean single-newline gap preserved');
  const np = p.d.createElement('p');
  np.textContent = 'Inserted';
  p.root.insertBefore(np, p.root.children[1]);
  eq(p.FC.editor.serialize(), '# Head\n\nInserted\n\nBody\n', 'new block gets blank lines both sides');
}

// 6) a new list right after an existing list uses the other bullet so they stay separate lists
{
  const p = mdPage([{ src: '- a\n- b', html: '<ul><li>a</li><li>b</li></ul>', k: 'list' }]);
  p.FC.editor.enter('rich');
  const ul = p.d.createElement('ul');
  ul.innerHTML = '<li>c</li>';
  p.root.appendChild(ul);
  eq(p.FC.editor.serialize(), '- a\n- b\n\n* c\n', 'adjacent lists alternate bullets');
}

// 7) inline formatting from tags and inline styles
{
  const p = mdPage([{ src: 'x', html: '<p>x</p>' }]);
  p.FC.editor.enter('rich');
  const para = p.root.children[0];
  para.innerHTML = 'a <b>bold </b>text <i>it</i> <span style="font-weight: bold;">sb</span> <b>B<i>BI</i></b> <strike>gone</strike>';
  eq(p.FC.editor.serialize(), 'a **bold** text *it* **sb B*BI*** ~~gone~~\n', 'inline formats + delimiter whitespace');
}

// 8) escaping of markdown-significant text
{
  const p = mdPage([{ src: 'x', html: '<p>x</p>' }]);
  p.FC.editor.enter('rich');
  const para = p.root.children[0];
  para.innerHTML = '# not a heading<br>1. not a list<br>2*3 snake_case _under_ [br] &lt;div&gt; - dash';
  eq(p.FC.editor.serialize(), '\\# not a heading\\\n1\\. not a list\\\n2\\*3 snake_case \\_under\\_ \\[br\\] \\<div> - dash\n', 'escapes');
}

// 9) links: nested formatting + autolinks
{
  const p = mdPage([{ src: 'x', html: '<p>x</p>' }]);
  p.FC.editor.enter('rich');
  p.root.children[0].innerHTML = 'See <a href="https://a.example/x">the <b>docs</b></a> or <a href="https://b.example">https://b.example</a>.';
  eq(p.FC.editor.serialize(), 'See [the **docs**](https://a.example/x) or <https://b.example>.\n', 'links');
}

// 10) code blocks: edited content, language, fence longer than inner backticks
{
  const p = mdPage([{ src: '```py\nprint(1)\n```', html: CODE('py', 'print(1)\n'), k: 'code' }]);
  p.FC.editor.enter('rich');
  const code = p.root.querySelector('code');
  code.textContent = 'print(2)\nx = "```"\n';
  eq(p.FC.editor.serialize(), '````py\nprint(2)\nx = "```"\n````\n', 'edited code block');
}

// 11) tables keep alignment and escape pipes
{
  const p = mdPage([BASE[5]]);
  p.FC.editor.enter('rich');
  p.root.querySelectorAll('td')[0].textContent = '1|x';
  eq(p.FC.editor.serialize(), '| a | b |\n| --- | :---: |\n| 1\\|x | 2 |\n', 'edited table');
}

// 12) task lists: toggling a checkbox writes [x]
{
  const p = mdPage([{ src: '- [ ] todo\n- [x] done', html: '<ul><li class="task-list-item"><span class="fc-task" data-checked="false" contenteditable="false"></span> todo</li><li class="task-list-item"><span class="fc-task" data-checked="true" contenteditable="false"></span> done</li></ul>', k: 'list' }]);
  p.FC.editor.enter('rich');
  eq(p.FC.editor.serialize(), p.src, 'task list untouched');
  p.root.querySelector('.fc-task').setAttribute('data-checked', 'true');
  eq(p.FC.editor.serialize(), '- [x] todo\n- [x] done\n', 'task toggled');
}

// 13) nested lists (browser indent creates ul directly inside ul) and ordered start
{
  const p = mdPage([{ src: '3. a\n4. b', html: '<ol start="3"><li>a</li><li>b</li></ol>', k: 'list' }]);
  p.FC.editor.enter('rich');
  const ol = p.root.children[0];
  const nested = p.d.createElement('ol');
  nested.innerHTML = '<li>inner</li>';
  ol.insertBefore(nested, ol.children[1]);
  eq(p.FC.editor.serialize(), '3. a\n   1. inner\n4. b\n', 'nested ordered list');
}

// 14) blockquote edit and heading trailing hashes
{
  const p = mdPage([BASE[4], { src: '## H', html: '<h2>H</h2>' }]);
  p.FC.editor.enter('rich');
  p.root.querySelector('blockquote p').innerHTML = 'line one<br>line <em>two</em>';
  p.root.querySelector('h2').textContent = 'C# ##';
  eq(p.FC.editor.serialize(), '> line one\\\n> line *two*\n\n## C# \\#\\#\n', 'quote + heading');
}

// 15) CRLF documents stay CRLF
{
  const p = mdPage([{ src: 'One', html: '<p>One</p>' }, { src: 'Two', html: '<p>Two</p>' }], { seps: ['\r\n\r\n'], tail: '\r\n' });
  p.FC.editor.enter('rich');
  eq(p.FC.editor.serialize(), 'One\r\n\r\nTwo\r\n', 'crlf untouched');
  p.root.children[1].textContent = 'Deux';
  eq(p.FC.editor.serialize(), 'One\r\n\r\nDeux\r\n', 'crlf edited');
}

// 16) front matter + html blocks are protected (always written from source)
{
  const p = mdPage([
    { src: '---\ntitle: x\n---', html: '<pre class="fc-frontmatter">---\ntitle: x\n---</pre>', k: 'frontmatter' },
    { src: '<div align="center">Hi</div>', html: '<div class="fc-html-block" contenteditable="false"><div align="center">Hi</div></div>', k: 'html' },
    { src: 'Text', html: '<p>Text</p>' }
  ]);
  p.FC.editor.enter('rich');
  p.root.children[2].textContent = 'Changed';
  eq(p.FC.editor.serialize(), '---\ntitle: x\n---\n\n<div align="center">Hi</div>\n\nChanged\n', 'protected blocks');
}

// 17) everything deleted, then typed text
{
  const p = mdPage(BASE.slice(0, 3));
  p.FC.editor.enter('rich');
  p.root.innerHTML = 'fresh start';
  eq(p.FC.editor.serialize(), 'fresh start\n', 'replace everything');
}

// 18) plainTextOf structures
{
  const p = build('txt', '', '');
  const div = p.d.createElement('div');
  const t = (html) => { div.innerHTML = html; return p.T.plainTextOf(div); };
  eq(t('a<div>b</div><div><br></div><div>c</div>'), 'a\nb\n\nc', 'chrome div lines');
  eq(t('abc\n<br>'), 'abc\n', 'trailing placeholder br');
  eq(t('<br>'), '', 'lonely br');
  eq(t('a<br>b<br><br>'), 'a\nb\n', 'br lines');
  eq(t('x\n\ty  z'), 'x\n\ty  z', 'raw whitespace kept');
}

// 19) TXT editing round-trip + CRLF + preview restore
{
  const src = 'line 1\r\nline 2\r\n```sh\r\necho hi\r\n```\r\nend\r\n';
  // segments for the test (plain, code, plain)
  const codeStart = src.indexOf('```sh'), bodyStart = src.indexOf('echo'), close = src.indexOf('```\r\nend'), closeEnd = close + 5;
  const p = txtPage(src, [
    { s: 0, e: codeStart },
    { code: true, s: codeStart, e: closeEnd, cs: bodyStart, ce: close, lang: 'sh' },
    { s: closeEnd, e: src.length }
  ]);
  const previewHtml = p.root.innerHTML;
  ok(p.FC.editor.enter('plain'), 'enter plain');
  const host = p.root.querySelector('.fc-plain-editor');
  ok(!!host, 'plain editor created');
  eq(host.textContent, src.replace(/\r\n/g, '\n'), 'editor shows normalized source');
  eq(p.FC.editor.serialize(), src, 'untouched txt identical (crlf restored)');
  host.textContent = host.textContent.replace('line 2', 'line two');
  eq(p.FC.editor.serialize(), src.replace('line 2', 'line two'), 'edited txt');
  p.FC.editor.exit();
  eq(p.root.innerHTML, previewHtml, 'preview restored after exit');
}

// 20) history: commit, undo, redo, jump, labels
{
  const p = mdPage([{ src: 'Hello', html: '<p>Hello</p>' }]);
  p.FC.editor.enter('rich');
  const para = p.root.children[0];
  para.textContent = 'Hello world';
  ok(p.T.commit(), 'commit 1');
  para.textContent = 'Hello';
  ok(p.T.commit(), 'commit 2');
  const st = p.T.state();
  eq(st.history.length, 3, 'three entries');
  eq(st.history[1].label, 'Typed \u201cworld\u201d', 'typed label');
  eq(st.history[2].label, 'Deleted \u201cworld\u201d', 'deleted label');
  p.FC.editor.undo();
  eq(p.root.children[0].textContent, 'Hello world', 'undo');
  p.FC.editor.redo();
  eq(p.root.children[0].textContent, 'Hello', 'redo');
  p.FC.editor.jump(0);
  eq(p.FC.editor.serialize(), 'Hello\n', 'jump to original');
  const lastState = JSON.parse(p.calls.filter(c => c[0] === 'onEditorState').pop()[1]);
  eq(lastState.dirty, false, 'not dirty at original');
  eq(lastState.canRedo, true, 'can redo after jump back');
  p.root.children[0].textContent = 'Changed';
  p.T.commit();
  eq(p.T.state().history.length, 2, 'new edit after jump drops redo branch');
  p.FC.editor.exit();
  eq(p.root.innerHTML.includes('Hello'), true, 'exit restores original DOM');
}

// 21) search highlights and clean removal keep the DOM identical
{
  const p = mdPage(BASE);
  const before = p.root.innerHTML;
  const n = p.FC.search('one');
  eq(n, 1, 'search count');
  ok(p.root.querySelectorAll('mark').length === 1, 'mark inserted');
  p.FC.clearSearch();
  eq(p.root.innerHTML, before, 'search cleanup restores DOM');
  const count = p.calls.filter(c => c[0] === 'onSearchResultCount').length;
  ok(count >= 2, 'search results reported');
}

// 22) describeChange
{
  const p = mdPage([{ src: 'x', html: '<p>x</p>' }]);
  eq(p.T.describeChange('abc', 'abXc'), 'Typed \u201cX\u201d', 'insert');
  eq(p.T.describeChange('abc', 'ac'), 'Deleted \u201cb\u201d', 'delete');
  eq(p.T.describeChange('abc', 'aZc'), 'Replaced \u201cb\u201d with \u201cZ\u201d', 'replace');
  eq(p.T.describeChange('abc', 'abc'), 'Formatting', 'formatting');
  eq(p.T.describeChange('ab', 'a\nb'), 'New line', 'newline');
}

// 23) reader ready + link click bridge
{
  const p = mdPage(BASE);
  ok(p.calls.some(c => c[0] === 'onReaderReady'), 'ready reported');
  const a = p.root.querySelector('a');
  a.dispatchEvent(new p.w.MouseEvent('click', { bubbles: true, cancelable: true }));
  ok(p.calls.some(c => c[0] === 'onLinkClicked' && c[1] === 'https://example.com'), 'link click forwarded');
  p.root.querySelector('p').dispatchEvent(new p.w.MouseEvent('click', { bubbles: true, cancelable: true }));
  ok(p.calls.some(c => c[0] === 'onDocumentTap'), 'tap forwarded');
}

// 24) requestContent path + plain bold markers
{
  const p = txtPage('hello world\n', [{ s: 0, e: 12 }]);
  p.FC.editor.enter('plain');
  const host = p.root.querySelector('.fc-plain-editor');
  const range = p.d.createRange();
  range.setStart(host.firstChild, 6); range.setEnd(host.firstChild, 11);
  const sel = p.w.getSelection(); sel.removeAllRanges(); sel.addRange(range);
  p.FC.editor.bold();
  eq(p.FC.editor.serialize(), 'hello **world**\n', 'plain bold wraps selection');
  p.FC.editor.bold();
  eq(p.FC.editor.serialize(), 'hello world\n', 'plain bold toggles off');
  p.FC.editor.requestContent(7);
  const c = p.calls.filter(x => x[0] === 'onEditorContent').pop();
  eq(c && c[1], 7, 'content id');
  eq(c && c[2], 'hello world\n', 'content text');
}

// 25) plain multi-line indent / outdent
{
  const src = 'a\nb\nc\n';
  const p = txtPage(src, [{ s: 0, e: src.length }]);
  p.FC.editor.enter('plain');
  const host = p.root.querySelector('.fc-plain-editor');
  const range = p.d.createRange();
  range.setStart(host.firstChild, 0); range.setEnd(host.firstChild, 3);
  const sel = p.w.getSelection(); sel.removeAllRanges(); sel.addRange(range);
  p.FC.editor.tab(false);
  eq(p.FC.editor.serialize(), '    a\n    b\nc\n', 'indent selected lines');
  p.FC.editor.tab(true);
  eq(p.FC.editor.serialize(), 'a\nb\nc\n', 'outdent selected lines');
}

done();
