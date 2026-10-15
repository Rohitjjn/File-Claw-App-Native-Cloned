package com.example.core.text

/**
 * Pure text utilities shared by the document-preview search and fenced-code copy UI.
 * Offsets are UTF-16 offsets, matching Kotlin String/WebView indexing.
 */
object PreviewTextUtils {
    data class FencedCodeBlock(
        val language: String,
        val code: String,
        val openingLine: Int
    )

    /** Finds non-overlapping, case-insensitive query occurrences up to [maxResults]. */
    fun findMatchOffsets(content: String, query: String, maxResults: Int = 500): List<Int> {
        if (content.isEmpty() || query.isBlank() || maxResults <= 0) return emptyList()
        val matches = ArrayList<Int>()
        var from = 0
        while (from <= content.length - query.length && matches.size < maxResults) {
            val index = content.indexOf(query, startIndex = from, ignoreCase = true)
            if (index < 0) break
            matches += index
            from = index + query.length.coerceAtLeast(1)
        }
        return matches
    }

    /**
     * Extracts fenced blocks from plain text/Markdown. Supports backtick and tilde fences
     * of length three or greater and returns only the interior, preserving indentation
     * and internal line breaks (without the fence delimiters).
     */
    fun extractFencedCodeBlocks(content: String): List<FencedCodeBlock> {
        if (content.isEmpty()) return emptyList()
        val lines = content.split('\n')
        val blocks = mutableListOf<FencedCodeBlock>()
        var index = 0

        while (index < lines.size) {
            val openingLine = lines[index].removeSuffix("\r")
            val opening = OPENING_FENCE.matchEntire(openingLine)
            if (opening == null) {
                index++
                continue
            }

            val fence = opening.groupValues[1]
            val info = opening.groupValues[2].trim()
            val language = info.substringBefore(' ').ifBlank { "CODE" }
            val fenceChar = fence.first()
            val minimumFenceLength = fence.length
            var closingIndex = index + 1

            while (closingIndex < lines.size) {
                val candidate = lines[closingIndex].removeSuffix("\r")
                if (CLOSING_FENCE.matchEntire(candidate)?.groupValues?.get(1)?.let {
                        it.first() == fenceChar && it.length >= minimumFenceLength
                    } == true
                ) break
                closingIndex++
            }

            if (closingIndex >= lines.size) {
                // An unclosed fence is ordinary text; don't offer to copy a partial block.
                index++
                continue
            }

            val innerLines = lines.subList(index + 1, closingIndex)
            val code = if (innerLines.isEmpty()) "" else innerLines.joinToString("\n") + "\n"
            blocks += FencedCodeBlock(language = language, code = code, openingLine = index + 1)
            index = closingIndex + 1
        }
        return blocks
    }

    private val OPENING_FENCE = Regex("^[ \\t]*((?:`{3,}|~{3,}))[ \\t]*([^\\r\\n]*)$")
    private val CLOSING_FENCE = Regex("^[ \\t]*(`{3,}|~{3,})[ \\t]*$")
}
