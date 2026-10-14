package com.example.utils

import android.content.Context
import com.example.core.text.PdfSearchResult
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import java.io.File
import java.util.Locale

/**
 * Full PDF text-layer search backed by PDFBox-Android.
 *
 * The old byte/stream scanner could not correctly decode many embedded font encodings and used
 * stream order as a page number. This implementation asks PDFBox for each physical page's text.
 * Scanned image-only PDFs correctly return no text hits; OCR is a separate future capability.
 */
object PdfDocumentSearch {

    fun search(
        context: Context,
        file: File,
        query: String,
        password: String? = null,
        maxPages: Int = 500
    ): List<PdfSearchResult> {
        if (!file.isFile || !file.canRead() || query.isBlank() || maxPages <= 0) return emptyList()
        return try {
            PDFBoxResourceLoader.init(context.applicationContext)
            loadDocument(file, password).use { document ->
                val stripper = PDFTextStripper().apply { sortByPosition = true }
                val pages = ArrayList<String>(document.numberOfPages)
                for (page in 1..document.numberOfPages) {
                    stripper.startPage = page
                    stripper.endPage = page
                    pages += stripper.getText(document)
                }
                findPageMatches(pages, query, maxPages)
            }
        } catch (_: Exception) {
            // Never crash the preview for unsupported, corrupt, or still-locked PDFs.
            emptyList()
        }
    }

    internal fun findPageMatches(
        pageTexts: List<String>,
        query: String,
        maxPages: Int = 500
    ): List<PdfSearchResult> {
        val normalizedQuery = normalizeSearchText(query)
        if (normalizedQuery.isEmpty() || maxPages <= 0) return emptyList()

        val results = ArrayList<PdfSearchResult>()
        for ((index, pageText) in pageTexts.withIndex()) {
            val normalizedPage = normalizeSearchText(pageText)
            val matchAt = normalizedPage.indexOf(normalizedQuery)
            if (matchAt < 0) continue

            val start = (matchAt - SNIPPET_CONTEXT).coerceAtLeast(0)
            val end = (matchAt + normalizedQuery.length + SNIPPET_CONTEXT).coerceAtMost(normalizedPage.length)
            results += PdfSearchResult(pageNumber = index + 1, matchedText = normalizedPage.substring(start, end))
            if (results.size >= maxPages) break
        }
        return results
    }

    internal fun normalizeSearchText(value: String): String =
        value.lowercase(Locale.ROOT).replace(WHITESPACE, " ").trim()

    internal fun loadDocument(file: File, password: String?): PDDocument =
        if (password.isNullOrEmpty()) PDDocument.load(file) else PDDocument.load(file, password)

    private val WHITESPACE = Regex("\\s+")
    private const val SNIPPET_CONTEXT = 36
}
