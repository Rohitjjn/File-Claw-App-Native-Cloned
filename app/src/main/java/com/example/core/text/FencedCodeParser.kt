package com.example.core.text

/** A lossless segment of text outside or inside a fenced Markdown-style code block. */
sealed interface FencedCodeSegment {
    val text: String

    data class Plain(override val text: String) : FencedCodeSegment
    data class Code(val language: String, override val text: String) : FencedCodeSegment
}

/**
 * A segment together with its exact position in the source.
 * [start] until [end] covers the whole segment (for code: including both fence lines);
 * [contentStart] until [contentEnd] covers only [FencedCodeSegment.text].
 */
data class RangedFencedSegment(
    val segment: FencedCodeSegment,
    val start: Int,
    val end: Int,
    val contentStart: Int,
    val contentEnd: Int
)

/**
 * Splits text on line-started backtick or tilde fences without interpreting or changing code content.
 * The fence lines are omitted from [FencedCodeSegment.Code]; line endings inside the code are kept.
 * An unmatched opening fence is treated as a code block through end-of-file, matching Markdown.
 */
object FencedCodeParser {
    private data class Line(val start: Int, val end: Int, val text: String)
    private data class Fence(val marker: Char, val length: Int, val language: String)

    fun split(source: String): List<FencedCodeSegment> = splitWithRanges(source).map { it.segment }

    /** Same split as [split], plus the source offsets of every segment (used to map views back to the file). */
    fun splitWithRanges(source: String): List<RangedFencedSegment> {
        if (source.isEmpty()) return listOf(RangedFencedSegment(FencedCodeSegment.Plain(""), 0, 0, 0, 0))

        val segments = mutableListOf<RangedFencedSegment>()
        var cursor = 0
        var scan = 0
        while (scan < source.length) {
            val openingLine = readLine(source, scan)
            val opening = parseOpening(openingLine.text)
            if (opening == null) {
                scan = openingLine.end
                continue
            }

            if (openingLine.start > cursor) {
                segments += plain(source, cursor, openingLine.start)
            }

            val codeStart = openingLine.end
            var closing: Line? = null
            var searchAt = codeStart
            while (searchAt < source.length) {
                val candidate = readLine(source, searchAt)
                if (isClosing(candidate.text, opening)) {
                    closing = candidate
                    break
                }
                searchAt = candidate.end
            }

            if (closing == null) {
                segments += RangedFencedSegment(
                    FencedCodeSegment.Code(opening.language, source.substring(codeStart)),
                    openingLine.start, source.length, codeStart, source.length
                )
                return segments
            }

            segments += RangedFencedSegment(
                FencedCodeSegment.Code(opening.language, source.substring(codeStart, closing.start)),
                openingLine.start, closing.end, codeStart, closing.start
            )
            cursor = closing.end
            scan = cursor
        }

        if (cursor < source.length) segments += plain(source, cursor, source.length)
        return segments
    }

    private fun plain(source: String, start: Int, end: Int) =
        RangedFencedSegment(FencedCodeSegment.Plain(source.substring(start, end)), start, end, start, end)

    private fun parseOpening(line: String): Fence? {
        val start = line.indexOfFirst { !it.isWhitespace() }
        if (start < 0 || line[start] !in charArrayOf('`', '~')) return null
        val marker = line[start]
        var end = start
        while (end < line.length && line[end] == marker) end++
        val length = end - start
        if (length < 3) return null
        return Fence(marker, length, line.substring(end).trim())
    }

    private fun isClosing(line: String, opening: Fence): Boolean {
        val start = line.indexOfFirst { !it.isWhitespace() }
        if (start < 0 || line[start] != opening.marker) return false
        var end = start
        while (end < line.length && line[end] == opening.marker) end++
        return end - start >= opening.length && line.substring(end).isBlank()
    }

    private fun readLine(source: String, start: Int): Line {
        val newline = source.indexOf('\n', start)
        val end = if (newline < 0) source.length else newline + 1
        var contentEnd = if (newline < 0) end else newline
        if (contentEnd > start && source[contentEnd - 1] == '\r') contentEnd--
        return Line(start, end, source.substring(start, contentEnd))
    }
}
