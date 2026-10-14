package com.example.core.text

import org.junit.Assert.assertEquals
import org.junit.Test

class DocumentOutlineParserTest {
    @Test
    fun parsesNestedMarkdownHeadingsAndSkipsFencedCode() {
        val markdown = listOf(
            "# Overview",
            "Body text.",
            "## Details",
            "\u0060\u0060\u0060markdown",
            "### This is example code, not a real heading",
            "\u0060\u0060\u0060",
            "### Deep dive",
            "#### Edge cases"
        ).joinToString("\n")

        val outline = DocumentOutlineParser.fromMarkdown(markdown)

        assertEquals(listOf("Overview", "Details", "Deep dive", "Edge cases"), outline.map { it.title })
        assertEquals(listOf(1, 2, 3, 4), outline.map { it.level })
        assertEquals(listOf(0, 1, 2, 3), outline.map { it.targetIndex })
    }

    @Test
    fun ignoresSetextTextAndEmptyHeadingMarkers() {
        val outline = DocumentOutlineParser.fromMarkdown("Paragraph\n---\n#   \n### Valid")
        assertEquals(listOf("Valid"), outline.map { it.title })
        assertEquals(0, outline.single().targetIndex)
    }
}
