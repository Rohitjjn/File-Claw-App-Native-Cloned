package com.example.core.text

/**
 * A stable, zero-based navigation target for a document outline.
 * Markdown and DOCX targets are heading ordinals; PDF viewers can use page indexes.
 */
data class DocumentOutlineItem(
    val title: String,
    val level: Int,
    val targetIndex: Int
)

/** Small, dependency-free outline parser shared by the native document viewer and tests. */
object DocumentOutlineParser {
    private val headingPattern = Regex("^\\s{0,3}(#{1,6})\\s+(.+?)\\s*#*\\s*$")

    fun fromMarkdown(markdown: String): List<DocumentOutlineItem> {
        val outline = mutableListOf<DocumentOutlineItem>()
        var insideFence = false
        var fenceMarker: Char? = null

        markdown.lineSequence().forEach { line ->
            val trimmed = line.trimStart()
            val marker = trimmed.firstOrNull()
            if (marker == '\u0060' || marker == '~') {
                val markerLength = trimmed.takeWhile { it == marker }.length
                if (markerLength >= 3) {
                    if (!insideFence) {
                        insideFence = true
                        fenceMarker = marker
                    } else if (fenceMarker == marker) {
                        insideFence = false
                        fenceMarker = null
                    }
                    return@forEach
                }
            }
            if (insideFence) return@forEach

            val match = headingPattern.matchEntire(line) ?: return@forEach
            val title = match.groupValues[2].replace(Regex("\\s+#+\\s*$"), "").trim()
            if (title.isNotEmpty()) {
                outline += DocumentOutlineItem(
                    title = title,
                    level = match.groupValues[1].length,
                    targetIndex = outline.size
                )
            }
        }
        return outline
    }
}
