package com.example.ui.screens

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DocumentPreviewFormattingTest {

    @Test
    fun markdownCodeFencesEachHaveAnIndependentCopyAction() {
        val html = formatSimpleMarkdownToHtml(
            "Introduction\n\n\u0060\u0060\u0060kotlin\nval answer = 42\n\u0060\u0060\u0060\n\nText\n\n\u0060\u0060\u0060sql\nselect 1;\n\u0060\u0060\u0060"
        )

        assertTrue(html.contains("fileclaw-md-code-0"))
        assertTrue(html.contains("fileclaw-md-code-1"))
        assertTrue(html.contains("Copy code"))
        assertTrue(html.contains("val answer = 42"))
        assertTrue(html.contains("select 1;"))
        assertFalse(html.contains("<code>val answer"))
    }

    @Test
    fun textFencesAreSeparatedFromOrdinaryTextAndEscapedSafely() {
        val html = formatFencedTextToHtml(
            "Before code\n\n\u0060\u0060\u0060python\nprint(\"<safe>\")\n\u0060\u0060\u0060\n\nAfter code"
        )

        assertTrue(html.contains("Before code"))
        assertTrue(html.contains("After code"))
        assertTrue(html.contains("fileclaw-txt-code-0"))
        assertTrue(html.contains("Copy code"))
        assertTrue(html.contains("print(&quot;&lt;safe&gt;&quot;)"))
        assertFalse(html.contains("print(\"<safe>\")"))
    }

    @Test
    fun unclosedTextFenceStillRendersACopyableBlock() {
        val html = formatFencedTextToHtml("Header\n\u0060\u0060\u0060js\nconsole.log(1)")

        assertTrue(html.contains("fileclaw-txt-code-0"))
        assertTrue(html.contains("console.log(1)"))
        assertTrue(html.contains("Copy code"))
    }
}
