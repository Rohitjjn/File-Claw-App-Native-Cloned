package com.example.ui.reader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReaderTextStatsTest {

    @Test
    fun identicalTextHasNoChanges() {
        val summary = ReaderTextStats.summarize("a b\nc", "a b\nc")
        assertTrue(summary.identical)
        assertEquals(3, summary.words)
    }

    @Test
    fun modifiedLineCountsAsOneRemovedAndOneAdded() {
        val summary = ReaderTextStats.summarize("one\ntwo\nthree", "one\n2\nthree\nfour")
        assertEquals(2, summary.addedLines)
        assertEquals(1, summary.removedLines)
    }

    @Test
    fun crlfAndLfCompareByLines() {
        val summary = ReaderTextStats.summarize("a\r\nb", "a\nb\nc")
        assertEquals(1, summary.addedLines)
        assertEquals(0, summary.removedLines)
    }

    @Test
    fun relativeTimeBuckets() {
        val clock: (Long) -> String = { "clock" }
        assertEquals("just now", ReaderTextStats.relativeTime(1_000, 5_000, clock))
        assertEquals("30 s ago", ReaderTextStats.relativeTime(0, 30_000, clock))
        assertEquals("2 min ago", ReaderTextStats.relativeTime(0, 125_000, clock))
        assertEquals("clock", ReaderTextStats.relativeTime(0, 4_000_000, clock))
    }
}
