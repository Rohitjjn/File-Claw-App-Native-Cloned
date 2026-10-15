package com.example.core.text

import org.junit.Assert.assertEquals
import org.junit.Test

class FencedCodeParserTest {
    @Test
    fun extractsOnlyTextBetweenOpeningAndClosingFenceLines() {
        val source = "Before\n\n```kotlin\nval answer = 42\nprintln(answer)\n```\nAfter"

        val segments = FencedCodeParser.split(source)

        assertEquals(
            listOf(
                FencedCodeSegment.Plain("Before\n\n"),
                FencedCodeSegment.Code("kotlin", "val answer = 42\nprintln(answer)\n"),
                FencedCodeSegment.Plain("After")
            ),
            segments
        )
    }

    @Test
    fun supportsMultipleBlocksAndPreservesPlainTextAndLineEndings() {
        val source = "first\r\n```sh\r\necho hi\r\n```\r\nsecond\n```\nraw\n```"

        val segments = FencedCodeParser.split(source)

        assertEquals(
            listOf(
                FencedCodeSegment.Plain("first\r\n"),
                FencedCodeSegment.Code("sh", "echo hi\r\n"),
                FencedCodeSegment.Plain("second\n"),
                FencedCodeSegment.Code("", "raw\n")
            ),
            segments
        )
    }

    @Test
    fun unmatchedFenceCopiesContentThroughEndOfFile() {
        val source = "```python\nprint('safe')"

        assertEquals(
            listOf(FencedCodeSegment.Code("python", "print('safe')")),
            FencedCodeParser.split(source)
        )
    }

    @Test
    fun ignoresInlineBackticksAndShortLineFences() {
        val source = "Inline `code` and\n``two ticks`` stay plain"

        assertEquals(listOf(FencedCodeSegment.Plain(source)), FencedCodeParser.split(source))
    }

    @Test
    fun supportsTildeFenceAndRequiresMatchingMarkerType() {
        val source = "before\n~~~yaml\nkey: value\n```\n~~~~\nafter"

        assertEquals(
            listOf(
                FencedCodeSegment.Plain("before\n"),
                FencedCodeSegment.Code("yaml", "key: value\n```\n"),
                FencedCodeSegment.Plain("after")
            ),
            FencedCodeParser.split(source)
        )
    }

    @Test
    fun longerFenceCanContainTripleBackticks() {
        val source = "````text\n``` is inside\n````\n"

        assertEquals(
            listOf(FencedCodeSegment.Code("text", "``` is inside\n")),
            FencedCodeParser.split(source)
        )
    }

    @Test
    fun rangesCoverTheSourceExactly() {
        val source = "intro\r\n```kt\r\nval a = 1\r\n```\r\noutro"
        val ranged = FencedCodeParser.splitWithRanges(source)

        assertEquals(3, ranged.size)
        assertEquals(0, ranged.first().start)
        assertEquals(source.length, ranged.last().end)
        ranged.zipWithNext().forEach { (a, b) -> assertEquals(a.end, b.start) }
        ranged.forEach { assertEquals(it.segment.text, source.substring(it.contentStart, it.contentEnd)) }
        val code = ranged[1]
        assertEquals("```kt\r\nval a = 1\r\n```\r\n", source.substring(code.start, code.end))
        assertEquals(FencedCodeParser.split(source), ranged.map { it.segment })
    }

    @Test
    fun emptySourceHasOneEmptyRange() {
        val ranged = FencedCodeParser.splitWithRanges("")
        assertEquals(1, ranged.size)
        assertEquals(0, ranged[0].end)
    }
}
