package com.example.core.text

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.util.zip.DeflaterOutputStream

class PdfTextSearcherTest {

    @Test
    fun testSearchInUncompressedPdfStream() {
        val pdfContent = """
            %PDF-1.4
            1 0 obj
            << /Length 40 >>
            stream
            BT
            /F1 12 Tf
            (Hello from FileClaw PDF Search) Tj
            ET
            endstream
            endobj
            trailer
            << /Root 1 0 R >>
            %%EOF
        """.trimIndent()

        val results = PdfTextSearcher.searchInBytes(pdfContent.toByteArray(), "fileclaw")
        assertEquals(1, results.size)
        assertEquals(1, results[0].pageNumber)
        assertTrue(results[0].matchedText.contains("fileclaw", ignoreCase = true))
    }

    @Test
    fun testSearchInFlateCompressedPdfStream() {
        val rawStream = "BT /F1 12 Tf (Confidential Financial Report 2026) Tj ET".toByteArray()
        val bos = ByteArrayOutputStream()
        DeflaterOutputStream(bos).use { it.write(rawStream) }
        val compressed = bos.toByteArray()
        val prefix = "%PDF-1.4\n1 0 obj\n<< /Filter /FlateDecode >>\nstream\n".toByteArray()
        val suffix = "\nendstream\nendobj\n%%EOF".toByteArray()

        val results = PdfTextSearcher.searchInBytes(prefix + compressed + suffix, "confidential")
        assertEquals(1, results.size)
        assertTrue(results[0].matchedText.contains("confidential", ignoreCase = true))
    }

    @Test
    fun normalizesMultilineQueriesAndSpacingBetweenPdfTextRuns() {
        val pdfContent = """
            %PDF-1.4
            stream
            BT
            (CONFIDENTIAL) Tj
            (Financial   Report) Tj
            ET
            endstream
            %%EOF
        """.trimIndent()

        val results = PdfTextSearcher.searchInBytes(
            pdfContent.toByteArray(),
            "  confidential\n\t financial   report "
        )

        assertEquals(1, results.size)
        assertTrue(results.single().matchedText.contains("confidential financial report"))
    }

    @Test
    fun handlesEmptyQueriesAndNonPositiveResultLimits() {
        val pdfContent = "%PDF-1.4\nstream\n(ordinary text)\nendstream\n%%EOF".toByteArray()
        assertTrue(PdfTextSearcher.searchInBytes(pdfContent, "").isEmpty())
        assertTrue(PdfTextSearcher.searchInBytes(pdfContent, " \n\t ").isEmpty())
        assertTrue(PdfTextSearcher.searchInBytes(pdfContent, "ordinary", maxResults = 0).isEmpty())
        assertTrue(PdfTextSearcher.searchInBytes(pdfContent, "ordinary", maxResults = -1).isEmpty())
    }

    @Test
    fun normalizesWhitespaceInFallbackScanAndKeepsSearchCaseInsensitive() {
        val rawPdf = "%PDF-1.4 trailer /Subject (Quarterly\n\tFinancial   REVIEW) %%EOF".toByteArray()
        val results = PdfTextSearcher.searchInBytes(rawPdf, "QUARTERLY financial review")
        assertEquals(1, results.size)
        assertTrue(results.single().matchedText.contains("quarterly financial review"))
    }

    @Test
    fun enforcesTheRequestedResultLimit() {
        val pdfContent = """
            %PDF-1.4
            stream
            (repeatable query one) Tj
            endstream
            stream
            (repeatable query two) Tj
            endstream
            %%EOF
        """.trimIndent()
        assertEquals(1, PdfTextSearcher.searchInBytes(pdfContent.toByteArray(), "repeatable query", maxResults = 1).size)
    }

    @Test
    fun testSearchWithNoMatchesReturnsEmpty() {
        val pdfContent = "%PDF-1.4\nstream\n(Just some ordinary text)\nendstream\n%%EOF"
        assertTrue(PdfTextSearcher.searchInBytes(pdfContent.toByteArray(), "nonexistent_query").isEmpty())
    }
}
