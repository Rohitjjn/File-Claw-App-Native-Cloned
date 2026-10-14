package com.example.core.text

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.Locale
import java.util.zip.InflaterInputStream

data class PdfSearchResult(
    val pageNumber: Int,
    val matchedText: String
)

/**
 * Lightweight PDF content-stream scanner, including /FlateDecode streams.
 * This is not a full PDF text engine: font encodings and exact physical page ownership
 * are not always recoverable from raw stream order alone.
 */
object PdfTextSearcher {

    fun searchInPdf(file: File, query: String, maxResults: Int = 100): List<PdfSearchResult> {
        if (!file.exists() || query.isBlank() || maxResults <= 0) return emptyList()
        return try {
            searchInBytes(file.readBytes(), query, maxResults)
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun searchInBytes(bytes: ByteArray, query: String, maxResults: Int = 100): List<PdfSearchResult> {
        if (bytes.isEmpty() || query.isBlank() || maxResults <= 0) return emptyList()
        val normalizedQuery = normalizeSearchText(query)
        if (normalizedQuery.isEmpty()) return emptyList()

        val results = mutableListOf<PdfSearchResult>()
        for ((streamIndex, stream) in extractStreams(bytes).withIndex()) {
            val text = normalizeSearchText(extractTextFromStream(stream))
            val matchIndex = text.indexOf(normalizedQuery)
            if (matchIndex >= 0) {
                val start = (matchIndex - SNIPPET_CONTEXT_CHARS).coerceAtLeast(0)
                val end = (matchIndex + normalizedQuery.length + SNIPPET_CONTEXT_CHARS).coerceAtMost(text.length)
                results.add(
                    PdfSearchResult(
                        pageNumber = streamIndex + 1,
                        matchedText = text.substring(start, end)
                    )
                )
                if (results.size >= maxResults) return results
            }
        }

        // Retain the legacy raw-byte fallback for uncompressed PDFs.
        if (results.isEmpty()) {
            val rawText = normalizeSearchText(String(bytes, Charsets.ISO_8859_1))
            if (rawText.contains(normalizedQuery)) {
                results.add(PdfSearchResult(pageNumber = 1, matchedText = normalizedQuery))
            }
        }
        return results
    }

    /** Apply the same locale-stable, whitespace-collapsing normalization to queries and PDF text. */
    internal fun normalizeSearchText(value: String): String =
        value.lowercase(Locale.ROOT).replace(WHITESPACE, " ").trim()

    private fun extractStreams(bytes: ByteArray): List<ByteArray> {
        val streams = mutableListOf<ByteArray>()
        val streamTag = "stream".toByteArray(Charsets.US_ASCII)
        val endstreamTag = "endstream".toByteArray(Charsets.US_ASCII)
        var idx = 0
        while (idx < bytes.size - streamTag.size) {
            val streamPos = indexOf(bytes, streamTag, idx)
            if (streamPos == -1) break
            var start = streamPos + streamTag.size
            if (start < bytes.size && bytes[start] == '\r'.code.toByte()) start++
            if (start < bytes.size && bytes[start] == '\n'.code.toByte()) start++
            val endstreamPos = indexOf(bytes, endstreamTag, start)
            if (endstreamPos == -1) break
            var end = endstreamPos
            if (end > start && bytes[end - 1] == '\n'.code.toByte()) end--
            if (end > start && bytes[end - 1] == '\r'.code.toByte()) end--
            if (end > start) {
                val streamBytes = bytes.copyOfRange(start, end)
                streams.add(tryDecompress(streamBytes) ?: streamBytes)
            }
            idx = endstreamPos + endstreamTag.size
        }
        return streams
    }

    private fun tryDecompress(data: ByteArray): ByteArray? = try {
        InflaterInputStream(ByteArrayInputStream(data)).use { inflater ->
            val buffer = ByteArray(4096)
            val out = ByteArrayOutputStream()
            while (true) {
                val read = inflater.read(buffer)
                if (read == -1) break
                out.write(buffer, 0, read)
            }
            out.toByteArray()
        }
    } catch (_: Exception) {
        null
    }

    private fun extractTextFromStream(stream: ByteArray): String {
        val str = String(stream, Charsets.ISO_8859_1)
        val sb = StringBuilder()
        var i = 0
        while (i < str.length) {
            if (str[i] == '(') {
                val end = findMatchingParen(str, i)
                if (end != -1) {
                    sb.append(unescapePdfString(str.substring(i + 1, end))).append(' ')
                    i = end
                }
            } else if (str[i] == '<' && i + 1 < str.length && str[i + 1] != '<') {
                val end = str.indexOf('>', i)
                if (end != -1) {
                    sb.append(decodeHexString(str.substring(i + 1, end).trim())).append(' ')
                    i = end
                }
            }
            i++
        }
        return sb.toString()
    }

    private fun findMatchingParen(str: String, start: Int): Int {
        var depth = 1
        var i = start + 1
        var escaped = false
        while (i < str.length) {
            val c = str[i]
            if (escaped) {
                escaped = false
            } else if (c == '\\') {
                escaped = true
            } else if (c == '(') {
                depth++
            } else if (c == ')') {
                depth--
                if (depth == 0) return i
            }
            i++
        }
        return -1
    }

    private fun unescapePdfString(s: String): String {
        val sb = StringBuilder()
        var i = 0
        while (i < s.length) {
            val c = s[i]
            if (c == '\\' && i + 1 < s.length) {
                val next = s[i + 1]
                when (next) {
                    'n' -> sb.append('\n')
                    'r' -> sb.append('\r')
                    't' -> sb.append('\t')
                    'b' -> sb.append('\b')
                    'f' -> sb.append('\u000C')
                    '(', ')', '\\' -> sb.append(next)
                    else -> sb.append(next)
                }
                i += 2
                continue
            }
            sb.append(c)
            i++
        }
        return sb.toString()
    }

    private fun decodeHexString(hex: String): String {
        val sb = StringBuilder()
        var i = 0
        while (i < hex.length - 1) {
            val code = hex.substring(i, i + 2).toIntOrNull(16)
            if (code != null && code in 32..126) sb.append(code.toChar())
            i += 2
        }
        return sb.toString()
    }

    private fun indexOf(source: ByteArray, target: ByteArray, fromIndex: Int): Int {
        if (target.isEmpty() || fromIndex >= source.size) return -1
        val max = source.size - target.size
        if (fromIndex > max) return -1
        for (i in fromIndex..max) {
            var j = 0
            while (j < target.size && source[i + j] == target[j]) j++
            if (j == target.size) return i
        }
        return -1
    }

    private val WHITESPACE = Regex("\\s+")
    private const val SNIPPET_CONTEXT_CHARS = 20
}
