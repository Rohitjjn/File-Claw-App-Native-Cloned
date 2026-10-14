package com.example.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PdfDocumentSearchTest {

    @Test
    fun mapsMatchesToActualPageIndexesInsteadOfStreamOrdinals() {
        val pages = listOf(
            "Cover page",
            "No result here",
            "The CONFIDENTIAL    financial\nreport is on this page",
            "Appendix"
        )

        val result = PdfDocumentSearch.findPageMatches(pages, " confidential\tfinancial report ")

        assertEquals(1, result.size)
        assertEquals(3, result.single().pageNumber)
        assertTrue(result.single().matchedText.contains("confidential financial report"))
    }

    @Test
    fun searchIsCaseInsensitiveAndNormalizesWhitespace() {
        val results = PdfDocumentSearch.findPageMatches(
            listOf("First LINE\n\nsecond line", "unrelated"),
            "FIRST   line second\tline"
        )
        assertEquals(listOf(1), results.map { it.pageNumber })
    }

    @Test
    fun ignoresEmptyQueriesAndNonPositiveLimits() {
        val pages = listOf("some document text")
        assertTrue(PdfDocumentSearch.findPageMatches(pages, "").isEmpty())
        assertTrue(PdfDocumentSearch.findPageMatches(pages, "  \n\t ").isEmpty())
        assertTrue(PdfDocumentSearch.findPageMatches(pages, "document", 0).isEmpty())
        assertTrue(PdfDocumentSearch.findPageMatches(pages, "document", -1).isEmpty())
    }

    @Test
    fun returnsOneNavigationTargetPerMatchingPage() {
        val pages = listOf("word word word", "other", "word appears again")
        val results = PdfDocumentSearch.findPageMatches(pages, "word")
        assertEquals(listOf(1, 3), results.map { it.pageNumber })
    }
}
