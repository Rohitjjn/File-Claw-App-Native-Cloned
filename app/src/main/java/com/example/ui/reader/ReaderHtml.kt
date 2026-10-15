package com.example.ui.reader

import com.example.core.text.FencedCodeParser
import com.example.core.text.FencedCodeSegment
import com.example.core.text.HtmlSanitizer
import org.intellij.markdown.IElementType
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.flavours.gfm.GFMElementTypes
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.html.HtmlGenerator
import org.intellij.markdown.parser.LinkMap
import org.intellij.markdown.parser.MarkdownParser

/** Which reader page to build. MARKDOWN_SOURCE shows raw Markdown in the plain-text editor. */
enum class ReaderKind(val jsName: String) {
    MARKDOWN("md"),
    TEXT("txt"),
    MARKDOWN_SOURCE("mdsrc")
}

/**
 * Builds the self-contained HTML pages for the Markdown/TXT reader (see assets/reader/).
 *
 * Markdown is rendered one top-level block at a time and every block is tagged with the source range
 * it came from. That lets the in-place editor write untouched blocks back byte for byte and lets
 * "Go to line" and the outline find the right element. Pure Kotlin so it can be unit tested.
 */
object ReaderHtml {

    private val positionedTags = setOf("h1", "h2", "h3", "h4", "h5", "h6", "p", "li", "tr", "blockquote", "table", "ul", "ol", "code")
    private val taskCheckbox = Regex("<input type=\"checkbox\" class=\"task-list-item-checkbox\"( checked)? disabled />")
    private val languageClass = Regex("class=\"language-([^\"\\s]+)")
    private val yamlKey = Regex("^[A-Za-z0-9_][A-Za-z0-9_ .-]*:(\\s|$)")

    private val positionCustomizer: (ASTNode, CharSequence, Iterable<CharSequence?>) -> Iterable<CharSequence?> =
        { node, tagName, attributes ->
            if (tagName.toString() in positionedTags) attributes + "data-sp=\"${node.startOffset}:${node.endOffset}\"" else attributes
        }

    /** Result of rendering Markdown into tagged block HTML. */
    data class MarkdownBody(val html: String, val blockCount: Int)

    /**
     * End offset of a leading YAML front matter block ("---" ... "---"/"..."), excluding the newline
     * after the closing fence, or 0 when the document has none.
     */
    fun frontMatterEnd(text: String): Int {
        if (!(text.startsWith("---\n") || text.startsWith("---\r\n"))) return 0
        var lineStart = text.indexOf('\n') + 1
        var sawKey = false
        var lines = 0
        while (lineStart < text.length && lines < 400) {
            val newline = text.indexOf('\n', lineStart)
            val lineEnd = if (newline < 0) text.length else newline
            val line = text.substring(lineStart, lineEnd).trimEnd('\r')
            if (line == "---" || line == "...") {
                return if (sawKey) lineStart + line.length else 0
            }
            if (yamlKey.containsMatchIn(line)) sawKey = true
            if (newline < 0) return 0
            lineStart = newline + 1
            lines++
        }
        return 0
    }

    /** Blanks out [0, end) while keeping every line break, so parser offsets stay identical. */
    fun mask(text: String, end: Int): String {
        val chars = text.toCharArray()
        for (i in 0 until end.coerceAtMost(chars.size)) {
            if (chars[i] != '\n' && chars[i] != '\r') chars[i] = ' '
        }
        return String(chars)
    }

    fun markdownBody(text: String): MarkdownBody {
        val frontMatter = frontMatterEnd(text)
        val parsed = if (frontMatter > 0) mask(text, frontMatter) else text
        val flavour = GFMFlavourDescriptor()
        // assertionsEnabled = false: malformed input falls back to plain blocks instead of throwing.
        val root = MarkdownParser(flavour, false).buildMarkdownTreeFromString(parsed)
        val providers = flavour.createHtmlGeneratingProviders(LinkMap.buildLinkMap(root, parsed), null)
        val renderer = HtmlGenerator.DefaultTagRenderer(positionCustomizer, false)
        val out = StringBuilder(text.length * 2 + 256)
        var index = 0

        if (frontMatter > 0) {
            out.append("<div class=\"fc-b\" data-b=\"0\" data-s=\"0\" data-e=\"").append(frontMatter)
                .append("\" data-k=\"frontmatter\" contenteditable=\"false\"><pre class=\"fc-frontmatter\">")
                .append(escapeHtml(text.substring(0, frontMatter)))
                .append("</pre></div>")
            index++
        }

        for (child in root.children) {
            val type = child.type
            if (type == MarkdownTokenTypes.EOL || type == MarkdownTokenTypes.WHITE_SPACE || type == MarkdownElementTypes.LINK_DEFINITION) continue
            if (child.endOffset <= frontMatter) continue
            val kind = kindOf(type)
            val rawHtml = try {
                HtmlGenerator(parsed, child, providers, false).generateHtml(renderer)
            } catch (e: Exception) {
                fallbackBlock(parsed, child)
            } catch (e: StackOverflowError) {
                fallbackBlock(parsed, child)
            }
            if (rawHtml.isBlank()) continue
            val html = HtmlSanitizer.sanitize(
                rawHtml.replace(taskCheckbox) { match ->
                    "<span class=\"fc-task\" data-checked=\"${match.groupValues[1].isNotEmpty()}\" contenteditable=\"false\"></span>"
                },
                keepImages = true
            )
            if (html.isBlank()) continue
            val range = "data-b=\"$index\" data-s=\"${child.startOffset}\" data-e=\"${child.endOffset}\""
            when (kind) {
                "code" -> {
                    val language = languageClass.find(html)?.groupValues?.get(1).orEmpty()
                    out.append("<div class=\"code-block-container fc-b\" ").append(range).append(" data-k=\"code\">")
                        .append(codeHeader(language))
                        .append("<div class=\"code-content\">").append(html).append("</div></div>")
                }
                "html" -> out.append("<div class=\"fc-b fc-html-block\" ").append(range)
                    .append(" data-k=\"html\" contenteditable=\"false\">").append(html).append("</div>")
                else -> out.append("<div class=\"fc-b\" ").append(range).append(" data-k=\"").append(kind).append("\">")
                    .append(html).append("</div>")
            }
            index++
        }
        return MarkdownBody(out.toString(), index)
    }

    /** TXT body: plain text segments plus fenced code cards, each tagged with its source range. */
    fun plainTextBody(text: String): String {
        val out = StringBuilder(text.length + text.length / 8 + 256)
        for (ranged in FencedCodeParser.splitWithRanges(text)) {
            when (val segment = ranged.segment) {
                is FencedCodeSegment.Plain -> {
                    if (segment.text.isEmpty()) continue
                    out.append("<pre class=\"plain-text fc-seg\" data-s=\"").append(ranged.start)
                        .append("\" data-e=\"").append(ranged.end).append("\">")
                    // The HTML parser drops one newline right after <pre>; repeat it so offsets stay exact.
                    if (segment.text.startsWith("\n")) out.append('\n')
                    out.append(escapeHtml(segment.text)).append("</pre>")
                }
                is FencedCodeSegment.Code -> {
                    val language = safeLanguage(segment.language)
                    out.append("<div class=\"code-block-container fc-seg\" data-s=\"").append(ranged.start)
                        .append("\" data-e=\"").append(ranged.end)
                        .append("\" data-cs=\"").append(ranged.contentStart)
                        .append("\" data-ce=\"").append(ranged.contentEnd).append("\">")
                        .append(codeHeader(language))
                        .append("<div class=\"code-content\"><pre><code")
                    if (language.isNotEmpty()) out.append(" class=\"language-").append(language).append('"')
                    out.append('>').append(escapeHtml(segment.text)).append("</code></pre></div></div>")
                }
            }
        }
        if (out.isEmpty()) out.append("<pre class=\"plain-text fc-seg\" data-s=\"0\" data-e=\"0\"></pre>")
        return out.toString()
    }

    /** Raw Markdown shown as one plain segment (Markdown source editing mode). */
    fun sourceBody(text: String): String {
        val out = StringBuilder(text.length + 128)
        out.append("<pre class=\"plain-text fc-seg\" data-s=\"0\" data-e=\"").append(text.length).append("\">")
        if (text.startsWith("\n")) out.append('\n')
        out.append(escapeHtml(text)).append("</pre>")
        return out.toString()
    }

    /**
     * Wraps a body in the reader page. [css] and [js] are the asset contents; [appearance] is applied
     * both as initial inline styles (no flash of the wrong theme) and again by the script.
     */
    fun page(
        kind: ReaderKind,
        body: String,
        source: String,
        css: String,
        js: String,
        appearance: ResolvedAppearance,
        tabSize: Int,
        restoreScroll: Float
    ): String {
        val htmlClass = if (kind == ReaderKind.MARKDOWN) "kind-md" else "kind-txt"
        val config = buildString {
            append("{\"kind\":").append(jsonString(kind.jsName))
            append(",\"tabSize\":").append(tabSize.coerceIn(1, 8))
            append(",\"restoreScroll\":").append(if (restoreScroll.isFinite()) restoreScroll.coerceIn(0f, 1f) else 0f)
            append(",\"appearance\":").append(appearance.json)
            append('}')
        }
        return buildString(body.length + source.length + css.length + js.length + 1024) {
            append("<!DOCTYPE html><html class=\"").append(htmlClass).append(if (appearance.wrapCode) " fc-wrap-code" else "")
            append("\" data-theme=\"").append(appearance.themeKey)
            append("\" data-wrap=\"").append(if (appearance.wrapText) "on" else "off")
            append("\" style=\"").append(escapeHtml(appearance.rootStyle)).append("\">")
            append("<head><meta charset=\"utf-8\">")
            append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1, minimum-scale=0.25, maximum-scale=5, user-scalable=yes\">")
            append("<style>").append(css).append("</style>")
            append("<script>window.FC_CONFIG = ").append(config).append(";</script>")
            append("</head><body")
            if (appearance.zoomPercent != 100) append(" style=\"zoom: ").append(appearance.zoomPercent / 100f).append('"')
            append("><main id=\"document-content\">").append(body).append("</main>")
            append("<script type=\"application/json\" id=\"fc-source\">").append(jsonString(source)).append("</script>")
            append("<script>").append(js).append("</script>")
            append("</body></html>")
        }
    }

    /** JSON string literal that is also safe inside an HTML script element. */
    fun jsonString(value: String): String {
        val out = StringBuilder(value.length + 16)
        out.append('"')
        for (ch in value) {
            when (ch) {
                '"' -> out.append("\\\"")
                '\\' -> out.append("\\\\")
                '\n' -> out.append("\\n")
                '\r' -> out.append("\\r")
                '\t' -> out.append("\\t")
                '<' -> out.append("\\u003c")
                '>' -> out.append("\\u003e")
                '&' -> out.append("\\u0026")
                '\u2028' -> out.append("\\u2028")
                '\u2029' -> out.append("\\u2029")
                else -> if (ch < ' ') out.append("\\u").append(ch.code.toString(16).padStart(4, '0')) else out.append(ch)
            }
        }
        out.append('"')
        return out.toString()
    }

    fun escapeHtml(value: String): String {
        val out = StringBuilder(value.length + 16)
        for (ch in value) {
            when (ch) {
                '&' -> out.append("&amp;")
                '<' -> out.append("&lt;")
                '>' -> out.append("&gt;")
                '"' -> out.append("&quot;")
                '\'' -> out.append("&#39;")
                else -> out.append(ch)
            }
        }
        return out.toString()
    }

    private fun codeHeader(language: String): String =
        "<div class=\"code-header\" contenteditable=\"false\"><span class=\"code-lang\">" +
            escapeHtml(language.ifEmpty { "Code" }) +
            "</span><button class=\"copy-btn\" type=\"button\">Copy code</button></div>"

    private fun safeLanguage(raw: String): String =
        raw.split(' ').firstOrNull().orEmpty().filter { it.isLetterOrDigit() || it == '_' || it == '-' || it == '+' || it == '#' }.take(32)

    private fun fallbackBlock(text: String, node: ASTNode): String =
        "<pre class=\"fc-raw\">" + escapeHtml(text.substring(node.startOffset, node.endOffset)) + "</pre>"

    private fun kindOf(type: IElementType): String = when (type) {
        MarkdownElementTypes.ATX_1, MarkdownElementTypes.ATX_2, MarkdownElementTypes.ATX_3,
        MarkdownElementTypes.ATX_4, MarkdownElementTypes.ATX_5, MarkdownElementTypes.ATX_6,
        MarkdownElementTypes.SETEXT_1, MarkdownElementTypes.SETEXT_2 -> "h"
        MarkdownElementTypes.PARAGRAPH -> "p"
        MarkdownElementTypes.ORDERED_LIST, MarkdownElementTypes.UNORDERED_LIST -> "list"
        MarkdownElementTypes.BLOCK_QUOTE -> "quote"
        MarkdownElementTypes.CODE_FENCE, MarkdownElementTypes.CODE_BLOCK -> "code"
        MarkdownElementTypes.HTML_BLOCK -> "html"
        MarkdownTokenTypes.HORIZONTAL_RULE -> "hr"
        GFMElementTypes.TABLE -> "table"
        GFMElementTypes.BLOCK_MATH -> "math"
        else -> "other"
    }
}
