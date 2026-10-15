package com.example.ui.reader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReaderHtmlTest {

    private val blockRange = Regex("data-b=\"(\\d+)\" data-s=\"(\\d+)\" data-e=\"(\\d+)\"")

    private fun ranges(html: String): List<Triple<Int, Int, Int>> =
        blockRange.findAll(html).map { Triple(it.groupValues[1].toInt(), it.groupValues[2].toInt(), it.groupValues[3].toInt()) }.toList()

    @Test
    fun markdownBlocksCarryExactSourceRangesInOrder() {
        val md = "# Title\n\nSome *text* here.\n\n- one\n- two\n\n```kotlin\nval x = 1\n```\n\n> quote\n"
        val body = ReaderHtml.markdownBody(md)
        val found = ranges(body.html)
        assertEquals(5, body.blockCount)
        assertEquals(listOf(0, 1, 2, 3, 4), found.map { it.first })
        val slices = found.map { md.substring(it.second, it.third) }
        assertEquals("# Title", slices[0].trim())
        assertTrue(slices[1].startsWith("Some *text* here."))
        assertTrue(slices[2].startsWith("- one"))
        assertTrue(slices[3].startsWith("```kotlin"))
        assertTrue(slices[4].startsWith("> quote"))
        // ranges never overlap and are ascending
        found.zipWithNext().forEach { (a, b) -> assertTrue(a.third <= b.second) }
    }

    @Test
    fun linkDefinitionsStayOutOfRenderedBlocks() {
        val md = "See [docs][d].\n\n[d]: https://example.com\n\nAfter\n"
        val html = ReaderHtml.markdownBody(md).html
        assertEquals(2, ranges(html).size)
        assertTrue(html.contains("href=\"https://example.com\""))
    }

    @Test
    fun codeBlocksAreDecoratedAndTaskBoxesSurviveSanitising() {
        val md = "```js\nlet a = '<b>';\n```\n\n- [ ] todo\n- [x] done\n"
        val html = ReaderHtml.markdownBody(md).html
        assertTrue(html.contains("class=\"code-block-container fc-b\""))
        assertTrue(html.contains("<span class=\"code-lang\">js</span>"))
        assertTrue(html.contains("contenteditable=\"false\""))
        assertTrue(html.contains("&lt;b&gt;"))
        assertTrue(html.contains("class=\"fc-task\" data-checked=\"false\""))
        assertTrue(html.contains("class=\"fc-task\" data-checked=\"true\""))
        assertFalse(html.contains("<input"))
    }

    @Test
    fun scriptsInMarkdownAreRemoved() {
        val html = ReaderHtml.markdownBody("Hi <script>alert(1)</script> <img src=x onerror=alert(2)>\n").html
        assertFalse(html.contains("<script"))
        assertFalse(html.contains("onerror"))
    }

    @Test
    fun frontMatterIsDetectedMaskedAndProtected() {
        val md = "---\ntitle: Notes\ntags: [a]\n---\n# Heading\n"
        val end = ReaderHtml.frontMatterEnd(md)
        assertEquals("---\ntitle: Notes\ntags: [a]\n---", md.substring(0, end))
        val html = ReaderHtml.markdownBody(md).html
        assertTrue(html.contains("data-k=\"frontmatter\""))
        assertTrue(html.contains("<h1"))
        assertFalse(html.contains("<hr"))
        val masked = ReaderHtml.mask(md, end)
        assertEquals(md.length, masked.length)
        assertEquals(md.count { it == '\n' }, masked.count { it == '\n' })
    }

    @Test
    fun horizontalRuleStartIsNotFrontMatter() {
        assertEquals(0, ReaderHtml.frontMatterEnd("---\nJust a paragraph\n---\n"))
        assertEquals(0, ReaderHtml.frontMatterEnd("No front matter\n"))
    }

    @Test
    fun malformedInputNeverThrows() {
        val nasty = "| a | b |\n|---|\n| 1 | 2 | 3 | 4 |\n" + "> ".repeat(200) + "deep\n" + "[".repeat(500)
        val body = ReaderHtml.markdownBody(nasty)
        assertTrue(body.blockCount > 0)
    }

    @Test
    fun plainTextSegmentsMapBackToSource() {
        val text = "\nfirst\n```sh\necho <hi>\n```\nlast"
        val html = ReaderHtml.plainTextBody(text)
        // a leading newline is doubled because the HTML parser drops the first one after <pre>
        assertTrue(html.contains("<pre class=\"plain-text fc-seg\" data-s=\"0\" data-e=\"7\">\n\nfirst\n</pre>"))
        assertTrue(html.contains("data-s=\"7\" data-e=\"27\" data-cs=\"13\" data-ce=\"23\""))
        assertTrue(html.contains("echo &lt;hi&gt;"))
        assertTrue(html.contains("<pre class=\"plain-text fc-seg\" data-s=\"27\" data-e=\"31\">last</pre>"))
    }

    @Test
    fun jsonStringIsSafeInsideScriptTags() {
        val encoded = ReaderHtml.jsonString("a\"b\\c\n</script><!-- \u2028 & \u0001")
        assertEquals("\"a\\\"b\\\\c\\n\\u003c/script\\u003e\\u003c!-- \\u2028 \\u0026 \\u0001\"", encoded)
    }

    @Test
    fun pageEmbedsSourceConfigAndInitialAppearance() {
        val appearance = ReaderAppearance(zoomPercent = 150, theme = ReaderTheme.SEPIA)
            .resolve(appDark = false, appFontSetting = "Medium", kind = ReaderKind.MARKDOWN)
        val page = ReaderHtml.page(ReaderKind.MARKDOWN, "<p>x</p>", "x</script>", "/*css*/", "/*js*/", appearance, 4, 0.5f)
        assertTrue(page.contains("data-theme=\"sepia\""))
        assertTrue(page.contains("<body style=\"zoom: 1.5\">"))
        assertTrue(page.contains("\"kind\":\"md\""))
        assertTrue(page.contains("\"restoreScroll\":0.5"))
        assertTrue(page.contains("\"x\\u003c/script\\u003e\""))
        assertFalse(page.contains("x</script>"))
    }
}
