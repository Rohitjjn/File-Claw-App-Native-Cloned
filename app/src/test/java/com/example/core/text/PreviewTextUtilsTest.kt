package com.example.core.text

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PreviewTextUtilsTest {
    @Test
    fun findsAllOccurrencesCaseInsensitively() {
        assertEquals(listOf(0, 7, 16), PreviewTextUtils.findMatchOffsets("Word x WORD and word", "word"))
    }

    @Test
    fun handlesEmptyQueriesAndNonPositiveLimits() {
        assertTrue(PreviewTextUtils.findMatchOffsets("sample text", "  ").isEmpty())
        assertTrue(PreviewTextUtils.findMatchOffsets("sample text", "text", 0).isEmpty())
    }

    @Test
    fun extractsOnlyFencedBlockContentsAndPreservesIndentation() {
        val source = "Before\n```kotlin\n  val x = 1\n  println(x)\n```\nAfter"
        val blocks = PreviewTextUtils.extractFencedCodeBlocks(source)
        assertEquals(1, blocks.size)
        assertEquals("kotlin", blocks.single().language)
        assertEquals("  val x = 1\n  println(x)\n", blocks.single().code)
        assertEquals(2, blocks.single().openingLine)
    }

    @Test
    fun supportsTildeFencesAndMultipleBlocks() {
        val source = "~~~sql\nselect 1;\n~~~\nplain\n```\nsecond\n```"
        val blocks = PreviewTextUtils.extractFencedCodeBlocks(source)
        assertEquals(2, blocks.size)
        assertEquals("sql", blocks[0].language)
        assertEquals("select 1;\n", blocks[0].code)
        assertEquals("CODE", blocks[1].language)
        assertEquals("second\n", blocks[1].code)
    }

    @Test
    fun ignoresUnclosedFences() {
        assertTrue(PreviewTextUtils.extractFencedCodeBlocks("before\n```js\nalert(1)").isEmpty())
    }

    @Test
    fun findsRepeatedUnicodeMatchesAtUtf16Offsets() {
        val content = "Café 🙂 CAFÉ café"
        assertEquals(listOf(0, 8, 13), PreviewTextUtils.findMatchOffsets(content, "café"))
    }

    @Test
    fun respectsMaximumMatchCountForRepeatedTerms() {
        assertEquals(listOf(0, 2), PreviewTextUtils.findMatchOffsets("aaaaaa", "aa", maxResults = 2))
    }

    @Test
    fun supportsCrLfFencesAndFenceLengthRules() {
        val source = "before\r\n````kotlin\r\nval marker = \"```\"\r\n````\r\nafter"
        val blocks = PreviewTextUtils.extractFencedCodeBlocks(source)
        assertEquals(1, blocks.size)
        assertEquals("kotlin", blocks.single().language)
        assertEquals("val marker = \"```\"\n", blocks.single().code)
    }

    @Test
    fun ignoresShorterFenceWhenClosingLongFence() {
        val source = "````kotlin\nline 1\n```\nline 2\n````"
        val blocks = PreviewTextUtils.extractFencedCodeBlocks(source)
        assertEquals(1, blocks.size)
        assertEquals("line 1\n```\nline 2\n", blocks.single().code)
    }
}
