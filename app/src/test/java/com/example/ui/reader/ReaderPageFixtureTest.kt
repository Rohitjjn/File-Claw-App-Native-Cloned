package com.example.ui.reader

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Builds real reader pages (Kotlin + intellij-markdown) for a corpus of documents. When the
 * READER_FIXTURE_DIR environment variable is set (CI), the pages are written there and
 * tools/reader-js-tests/roundtrip.js loads each one in jsdom and checks that entering edit mode and
 * saving without changes reproduces the source byte for byte.
 */
class ReaderPageFixtureTest {

    private val corpus = linkedMapOf(
        "basic.md" to "# Title\n\nSome **bold**, *italic*, `code` and ~~strike~~ text.\nA soft-wrapped second line.\n\n## Section two\n\nLast paragraph with a [link](https://example.com \"Example\") and <https://auto.example>.\n",
        "lists.md" to "- one\n- two\n  - nested *a*\n  - nested b\n- three\n\n3. third\n4. fourth\n\n* star list\n\n- [ ] todo item\n- [x] done item\n",
        "code.md" to "Intro:\n\n```kotlin\nfun main() {\n    println(\"<hi>\")\n}\n```\n\n    indented code\n    block\n\n~~~\ntilde fence\n~~~\n",
        "quotes-tables.md" to "> A quote\n> with two lines\n>\n> > nested\n\n| Left | Center | Right |\n|:-----|:------:|------:|\n| a | b | c |\n| `x|y` | **bold** | 3 |\n\n***\n\n___\n",
        "html-refs.md" to "<div align=\"center\">\n  <b>Centered</b>\n</div>\n\nText with [a ref][r] and ![img][i].\n\n[r]: https://ref.example\n[i]: data:image/png;base64,AAAA\n\n<!-- a comment -->\n\nEnd.\n",
        "frontmatter.md" to "---\ntitle: Notes\ntags: [a, b]\n---\n\n# After front matter\n\nBody.\n",
        "crlf.md" to "# CRLF doc\r\n\r\nLine one\r\nline two\r\n\r\n- item\r\n- item 2\r\n",
        "edge.md" to "Setext H1\n=========\n\nSetext H2\n---------\n\nHard break with spaces  \nand backslash\\\nend.\n\nEscapes: \\*not italic\\* and 2 * 3 * 4 and snake_case_name.\n\nEmoji \uD83D\uDE00 and unicode \u00e9\u00e8 and RTL \u05e9\u05dc\u05d5\u05dd.\n\nCosts $5 and $10.\n\n\n\nTrailing blank lines above.\n",
        "no-trailing-newline.md" to "Just one paragraph without a final newline",
        "mixed-eol.md" to "# Mixed\r\n\r\nCRLF paragraph\r\n\nLF paragraph\n\n- a\r\n- b\n",
        "lone-cr.txt" to "old mac\rline two\rline three",
        "notes.txt" to "Plain notes\n\nfirst line\n\tindented with tab\n```sh\necho \"<x>\"\n```\nafter code\r\nwindows line\n",
        "empty.txt" to ""
    )

    @Test
    fun buildsPagesForTheRoundTripCorpus() {
        val root = generateSequence(File("").absoluteFile) { it.parentFile }
            .firstOrNull { File(it, "src/main/assets/reader/reader.js").exists() || File(it, "app/src/main/assets/reader/reader.js").exists() }
            ?: error("reader assets not found")
        val assets = if (File(root, "src/main/assets/reader").exists()) File(root, "src/main/assets/reader") else File(root, "app/src/main/assets/reader")
        val css = File(assets, "reader.css").readText()
        val js = File(assets, "reader.js").readText()
        val outDir = System.getenv("READER_FIXTURE_DIR")?.takeIf { it.isNotBlank() }?.let { File(it).apply { mkdirs() } }

        for ((name, text) in corpus) {
            val markdown = name.endsWith(".md")
            val kind = if (markdown) ReaderKind.MARKDOWN else ReaderKind.TEXT
            val body = if (markdown) ReaderHtml.markdownBody(text).html else ReaderHtml.plainTextBody(text)
            val appearance = ReaderAppearance().resolve(appDark = false, appFontSetting = "Medium", kind = kind)
            val page = ReaderHtml.page(kind, body, text, css, js, appearance, 4, 0f)
            assertTrue(name, page.contains("id=\"fc-source\""))
            outDir?.let { File(it, "$name.html").writeText(page) }
        }
    }
}
