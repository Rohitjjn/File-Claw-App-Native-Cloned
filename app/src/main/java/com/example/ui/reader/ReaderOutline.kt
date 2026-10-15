package com.example.ui.reader

import com.example.ui.component.HeadingItem
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.parser.MarkdownParser

/** Builds the Document Outline for Markdown (real headings) and TXT (conservative heuristics). */
object ReaderOutline {
    private const val MAX_HEADINGS = 2000
    private const val PROTECT_BASE = 0xE000

    private val atxPattern = Regex("^ {0,3}(#{1,6})[ \\t]+(.+?)[ \\t]*#*[ \\t]*$")
    private val chapterPattern = Regex("^(chapter|part|book|appendix|section|prologue|epilogue)\\b(\\s+[\\w.:-]+)?.*$", RegexOption.IGNORE_CASE)
    private val numberedPattern = Regex("^(\\d{1,3}(?:\\.\\d{1,3}){0,3})\\.?[ \\t]+(\\p{Lu}.*)$")
    private val inlineLink = Regex("!?\\[([^\\]]*)]\\([^)]*\\)")
    private val refLink = Regex("!?\\[([^\\]]*)]\\[[^\\]]*]")
    private val htmlTag = Regex("<[^>]+>")
    private val emphasis = Regex("(\\*\\*|__|~~|[*_`])")
    private val escape = Regex("\\\\([!-/:-@\\[-`{-~])")

    /** Headings in document order, using the same parser as the renderer so offsets line up. */
    fun markdown(text: String): List<HeadingItem> {
        if (text.isEmpty()) return emptyList()
        val frontMatter = ReaderHtml.frontMatterEnd(text)
        val parsed = if (frontMatter > 0) ReaderHtml.mask(text, frontMatter) else text
        val root = try {
            MarkdownParser(GFMFlavourDescriptor(), false).buildMarkdownTreeFromString(parsed)
        } catch (e: Exception) {
            return plainText(text)
        }
        val lines = LineIndex(text)
        val result = ArrayList<HeadingItem>()
        fun visit(node: ASTNode, depth: Int) {
            if (result.size >= MAX_HEADINGS || depth > 64) return
            val level = headingLevel(node)
            if (level > 0) {
                val content = node.children.firstOrNull {
                    it.type == MarkdownTokenTypes.ATX_CONTENT || it.type == MarkdownTokenTypes.SETEXT_CONTENT
                }
                val raw = if (content != null) parsed.substring(content.startOffset, content.endOffset) else ""
                val title = plainTitle(raw)
                if (title.isNotEmpty()) {
                    result += HeadingItem(level = level, title = title, lineNumber = lines.lineOf(node.startOffset), sourceOffset = node.startOffset)
                }
                return
            }
            for (child in node.children) visit(child, depth + 1)
        }
        visit(root, 0)
        return result
    }

    /**
     * Section-like lines of a plain text file: Markdown-style # headings, underlined (=== / ---)
     * titles, "Chapter 3" style lines, short numbered titles ("2.1 Results") and short ALL CAPS lines
     * that stand on their own. Lines inside fenced code are ignored.
     */
    fun plainText(text: String): List<HeadingItem> {
        if (text.isEmpty()) return emptyList()
        val lines = text.split('\n').map { it.trimEnd('\r') }
        val starts = IntArray(lines.size)
        var cursor = 0
        for (i in lines.indices) {
            starts[i] = cursor
            val nl = text.indexOf('\n', cursor)
            cursor = if (nl < 0) text.length else nl + 1
        }

        val result = ArrayList<HeadingItem>()
        var fence: String? = null
        for (i in lines.indices) {
            if (result.size >= MAX_HEADINGS) break
            val line = lines[i]
            val trimmed = line.trim()
            if (fence != null) {
                if (trimmed.startsWith(fence)) fence = null
                continue
            }
            if (trimmed.startsWith("```") || trimmed.startsWith("~~~")) {
                fence = trimmed.take(3)
                continue
            }
            if (trimmed.isEmpty()) continue
            val prevBlank = i == 0 || lines[i - 1].isBlank()
            val next = lines.getOrNull(i + 1)?.trim()
            val nextBlank = next == null || next.isEmpty()

            val atx = atxPattern.matchEntire(line)
            if (atx != null) {
                val title = plainTitle(atx.groupValues[2])
                if (title.isNotEmpty()) result += HeadingItem(atx.groupValues[1].length, title, i + 1, starts[i])
                continue
            }
            if (next != null && trimmed.length <= 120 && !trimmed.startsWith("-") && !trimmed.startsWith("=")) {
                if (next.length >= 3 && next.all { it == '=' }) {
                    result += HeadingItem(1, plainTitle(trimmed), i + 1, starts[i]); continue
                }
                if (next.length >= 3 && next.all { it == '-' } && prevBlank) {
                    result += HeadingItem(2, plainTitle(trimmed), i + 1, starts[i]); continue
                }
            }
            if (!prevBlank || trimmed.length > 80) continue
            if (chapterPattern.matches(trimmed) && trimmed.split(Regex("\\s+")).size <= 10 && !trimmed.endsWith(",")) {
                val level = if (trimmed.startsWith("section", ignoreCase = true)) 2 else 1
                result += HeadingItem(level, trimmed, i + 1, starts[i]); continue
            }
            val numbered = numberedPattern.matchEntire(trimmed)
            if (numbered != null && nextBlank && trimmed.length <= 70 && trimmed.last() !in ".,;:") {
                val depth = numbered.groupValues[1].count { it == '.' } + 1
                result += HeadingItem(depth.coerceIn(1, 4), trimmed, i + 1, starts[i]); continue
            }
            if (nextBlank && isAllCapsTitle(trimmed)) {
                result += HeadingItem(1, trimmed, i + 1, starts[i])
            }
        }
        return result
    }

    private fun isAllCapsTitle(line: String): Boolean {
        if (line.length !in 3..60) return false
        if (line.first() in "-*+>|#" || line.first().isDigit() && line.length < 6) return false
        val letters = line.count { it.isLetter() }
        if (letters < 3) return false
        if (line.any { it.isLowerCase() }) return false
        return line.count { it.isLetterOrDigit() || it.isWhitespace() } >= line.length * 0.7
    }

    private fun headingLevel(node: ASTNode): Int = when (node.type) {
        MarkdownElementTypes.ATX_1, MarkdownElementTypes.SETEXT_1 -> 1
        MarkdownElementTypes.ATX_2, MarkdownElementTypes.SETEXT_2 -> 2
        MarkdownElementTypes.ATX_3 -> 3
        MarkdownElementTypes.ATX_4 -> 4
        MarkdownElementTypes.ATX_5 -> 5
        MarkdownElementTypes.ATX_6 -> 6
        else -> 0
    }

    /** Heading text without Markdown syntax: links keep their label, emphasis markers and tags go. */
    fun plainTitle(raw: String): String {
        var t = raw.trim()
        t = t.replace(Regex("[ \\t]+#+[ \\t]*$"), "").let { if (it.matches(Regex("#+"))) "" else it }
        t = t.replace(inlineLink, "$1").replace(refLink, "$1").replace(htmlTag, "")
        // Protect backslash-escaped punctuation (\* stays a literal star) while emphasis markers are removed.
        t = escape.replace(t) { match -> (PROTECT_BASE + match.groupValues[1][0].code).toChar().toString() }
        t = t.replace(emphasis, "")
        t = buildString(t.length) {
            for (ch in t) append(if (ch.code in PROTECT_BASE until PROTECT_BASE + 128) (ch.code - PROTECT_BASE).toChar() else ch)
        }
        return t.replace(Regex("\\s+"), " ").trim()
    }

    /** Maps character offsets to 1-based line numbers. */
    private class LineIndex(text: String) {
        private val starts: IntArray

        init {
            val list = ArrayList<Int>()
            list += 0
            for (i in text.indices) if (text[i] == '\n') list += i + 1
            starts = list.toIntArray()
        }

        fun lineOf(offset: Int): Int {
            var lo = 0
            var hi = starts.size - 1
            while (lo < hi) {
                val mid = (lo + hi + 1) ushr 1
                if (starts[mid] <= offset) lo = mid else hi = mid - 1
            }
            return lo + 1
        }
    }
}
