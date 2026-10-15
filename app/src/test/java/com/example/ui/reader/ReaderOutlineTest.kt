package com.example.ui.reader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReaderOutlineTest {

    @Test
    fun markdownHeadingsUseTheRealParser() {
        val md = "# Intro\n\nText\n\nSetext Title\n------------\n\n```\n# not a heading\n```\n\n- item\n\n  ### Nested **bold** [link](x)\n"
        val headings = ReaderOutline.markdown(md)
        assertEquals(listOf("Intro", "Setext Title", "Nested bold link"), headings.map { it.title })
        assertEquals(listOf(1, 2, 3), headings.map { it.level })
        assertEquals(1, headings[0].lineNumber)
        assertEquals(5, headings[1].lineNumber)
        assertEquals(0, headings[0].sourceOffset)
        assertTrue(md.startsWith("Setext", headings[1].sourceOffset))
    }

    @Test
    fun markdownOutlineIgnoresFrontMatter() {
        val md = "---\ntitle: x\n---\n# Real\n"
        assertEquals(listOf("Real"), ReaderOutline.markdown(md).map { it.title })
    }

    @Test
    fun plainTextOutlineFindsSectionsConservatively() {
        val text = listOf(
            "MY NOTES",
            "",
            "Chapter 1: Getting started",
            "",
            "Some text. 1. this is not a heading because it is a sentence.",
            "",
            "2.1 Results",
            "",
            "Title",
            "=====",
            "```",
            "# not a heading",
            "```",
            "# Real heading",
            "- LIST ITEM IN CAPS"
        ).joinToString("\n")
        val titles = ReaderOutline.plainText(text).map { it.title to it.lineNumber }
        assertEquals(
            listOf("MY NOTES" to 1, "Chapter 1: Getting started" to 3, "2.1 Results" to 7, "Title" to 9, "Real heading" to 14),
            titles
        )
    }

    @Test
    fun plainTitleStripsMarkdownButKeepsEscapes() {
        assertEquals("Hello world", ReaderOutline.plainTitle("**Hello** _world_ ##"))
        assertEquals("a*b", ReaderOutline.plainTitle("a\\*b"))
        assertEquals("Docs", ReaderOutline.plainTitle("[Docs](https://x)"))
    }
}
