const { mdPage, txtPage, eq, ok, done } = require('./harness');
function one(html) {
  const p = mdPage([{ src: 'x', html: '<p>x</p>' }]);
  p.FC.editor.enter('rich');
  p.root.innerHTML = html;
  return p.FC.editor.serialize();
}
eq(one('<p>a</p><p><br></p><p>b</p>'), 'a\n\nb\n', 'empty paragraph skipped');
eq(one('<div>chrome div para</div><div>second</div>'), 'chrome div para\n\nsecond\n', 'div paragraphs');
eq(one('<ul><li><p>a</p></li><li><p>b</p></li></ul>'), '- a\n\n- b\n', 'loose list');
eq(one('<ul><li><p>a</p><ul><li>b</li></ul></li></ul>'), '- a\n  - b\n', 'li paragraph + nested list');
eq(one('<p><img alt="pic" src="data:image/png;base64,AAA" title="T"></p>'), '![pic](data:image/png;base64,AAA "T")\n', 'image');
eq(one('<p>use <code>a`b</code> now</p>'), 'use ``a`b`` now\n', 'inline code with backtick');
eq(one('<p>press <kbd>Ctrl</kbd>+<kbd>S</kbd></p>'), 'press <kbd>Ctrl</kbd>+<kbd>S</kbd>\n', 'inline html kept');
eq(one('<p>area <span class="math" inline = "true">x^2</span></p>'), 'area $x^2$\n', 'inline math');
eq(one('<p><a href="https://x.com/a(b)" title="T">t</a></p>'), '[t](<https://x.com/a(b)> "T")\n', 'link title + parens');
eq(one('<p>costs $5 and $10</p>'), 'costs \\$5 and $10\n', 'dollar escape');
eq(one('<h2>From para</h2>'), '## From para\n', 'new heading');
eq(one('<blockquote><ul><li>q1</li><li>q2</li></ul></blockquote>'), '> - q1\n> - q2\n', 'list in quote');
eq(one('<blockquote>bare text in quote</blockquote>'), '> bare text in quote\n', 'quote with inline content');
eq(one('<p>tab\u2003here and&nbsp;nbsp</p>'), 'tab\u2003here and nbsp\n', 'em-space kept, nbsp normalised');
eq(one('<p><b>bold</b><i>it</i></p>'), '**bold***it*\n', 'adjacent bold+italic');
eq(one('<p>under_score_word and _x</p>'), 'under_score_word and \\_x\n', 'underscore rules');
eq(one('<ol><li>one</li><li>two</li></ol><ol><li>three</li></ol>'), '1. one\n2. two\n\n1) three\n', 'adjacent ordered lists alternate delimiter');
eq(one('<p>a<br>b<br></p>'), 'a\\\nb\n', 'trailing br dropped, inner br kept');
eq(one('<p>   leading spaces</p>'), 'leading spaces\n', 'leading spaces trimmed');
eq(one('<p>- not list</p><p>+ plus</p><p>==</p>'), '\\- not list\n\n\\+ plus\n\n\\==\n', 'line start escapes');
eq(one('<table><tr><td>a<br>b</td><td><code>x|y</code></td></tr></table>'), '| a<br>b | `x\\|y` |\n| --- | --- |\n', 'table br + pipes in code');
eq(one('<div class="code-block-container" data-k="code"><div class="code-header" contenteditable="false"><span class="code-lang">Code</span><button class="copy-btn">Copy code</button></div><div class="code-content"><pre><code>plain\n</code></pre></div></div>'), '```\nplain\n```\n', 'new code container');

// Undo restores exact original after many edits; serialize equals source
{
  const src = ['# T', 'para one', '- a\n- b'];
  const p = mdPage([
    { src: src[0], html: '<h1>T</h1>' },
    { src: src[1], html: '<p>para one</p>' },
    { src: src[2], html: '<ul><li>a</li><li>b</li></ul>' }
  ]);
  p.FC.editor.enter('rich');
  for (let i = 0; i < 5; i++) { p.root.children[1].textContent = 'para ' + i; p.T.commit(); }
  for (let i = 0; i < 5; i++) p.FC.editor.undo();
  eq(p.FC.editor.serialize(), p.src, 'undo all returns byte-identical source');
}

// history cap keeps the original entry
{
  const p = mdPage([{ src: 'x', html: '<p>x</p>' }]);
  p.FC.editor.enter('rich');
  for (let i = 0; i < 130; i++) { p.root.children[0].textContent = 'v' + i; p.T.commit(); }
  const st = p.T.state();
  eq(st.history.length, 100, 'history capped at 100');
  eq(st.history[0].label, 'Original', 'original pinned');
  p.FC.editor.jump(0);
  eq(p.FC.editor.serialize(), 'x\n', 'original still restorable');
}

// TXT: scroll/position helpers
{
  const src = 'alpha\nbeta\ngamma\n';
  const p = txtPage(src, [{ s: 0, e: src.length }]);
  const seg = p.root.querySelector('.fc-seg');
  const pos = p.T.locateText(seg, 6);
  eq(pos.node.nodeValue.slice(pos.offset, pos.offset + 4), 'beta', 'locateText');
  eq(p.T.textOffsetOf(seg, pos.node, pos.offset), 6, 'textOffsetOf inverse');
  ok(p.FC.scrollToLine(2) === true, 'scrollToLine returns true');
  eq(p.d.querySelectorAll('.fc-marker').length, 1, 'flash marker present');
}
// Mixed line endings survive an unchanged save, and only edited lines change (plain editor)
{
  const src = 'a\nb\r\nc\rd\n';
  const p = txtPage(src, [{ s: 0, e: src.length }]);
  p.FC.editor.enter('plain');
  eq(p.FC.editor.serialize(), src, 'mixed endings untouched');
  const host = p.root.querySelector('.fc-plain-editor');
  host.textContent = host.textContent.replace('c', 'C');
  eq(p.FC.editor.serialize(), 'a\nb\r\nC\nd\n', 'only the edited line takes the dominant ending');
}

// Mixed line endings in Markdown: verbatim blocks/gaps keep their bytes, generated text uses the dominant one
{
  const p = mdPage([{ src: 'One', html: '<p>One</p>' }, { src: 'Two', html: '<p>Two</p>' }, { src: 'Three', html: '<p>Three</p>' }],
    { seps: ['\r\n\r\n', '\n\n'], tail: '\r\n' });
  p.FC.editor.enter('rich');
  eq(p.FC.editor.serialize(), p.src, 'mixed markdown untouched');
  const np = p.d.createElement('p');
  np.textContent = 'New';
  p.root.appendChild(np);
  eq(p.FC.editor.serialize(), 'One\r\n\r\nTwo\n\nThree\r\n\r\nNew\r\n', 'new block uses CRLF, old gaps verbatim');
}
done();
