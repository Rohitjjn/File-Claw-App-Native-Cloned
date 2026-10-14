package com.example.core.io

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class CopyFileNamerTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun keepsRequestedNameWhenNoCollisionExists() {
        val dir = temporaryFolder.newFolder("exports")
        val source = File(dir, "source.txt").apply { writeText("original") }
        val target = CopyFileNamer.nextAvailable(dir, source, "edited.txt")
        assertEquals(File(dir, "edited.txt"), target)
        assertFalse(target.exists())
    }

    @Test
    fun neverOverwritesTheSourceWhenSaveAsUsesItsName() {
        val dir = temporaryFolder.root
        val source = File(dir, "report.docx").apply { writeText("original bytes") }
        val target = CopyFileNamer.nextAvailable(dir, source, "report.docx")
        assertEquals("report (copy).docx", target.name)
        assertEquals("original bytes", source.readText())
        assertFalse(target.exists())
    }

    @Test
    fun incrementsTheCopySuffixWhenAnEarlierExportAlreadyExists() {
        val dir = temporaryFolder.root
        val source = File(dir, "source.txt").apply { writeText("source") }
        File(dir, "report.txt").writeText("previous export")
        File(dir, "report (copy).txt").writeText("another export")
        val target = CopyFileNamer.nextAvailable(dir, source, "report.txt")
        assertEquals("report (copy 2).txt", target.name)
        assertEquals("previous export", File(dir, "report.txt").readText())
        assertEquals("another export", File(dir, "report (copy).txt").readText())
    }

    @Test
    fun rejectsPathTraversalInsteadOfWritingOutsideTheDestination() {
        val dir = temporaryFolder.root
        val source = File(dir, "source.txt").apply { writeText("source") }
        try {
            CopyFileNamer.nextAvailable(dir, source, "../outside.txt")
            throw AssertionError("Expected a path-bearing filename to be rejected")
        } catch (expected: IllegalArgumentException) {
            assertTrue(expected.message.orEmpty().contains("path"))
        }
    }

    @Test
    fun supportsExtensionlessNamesAndHiddenFiles() {
        val dir = temporaryFolder.root
        val source = File(dir, "source").apply { writeText("source") }
        File(dir, "notes").writeText("collision")
        File(dir, ".env").writeText("secret")
        assertEquals("notes (copy)", CopyFileNamer.nextAvailable(dir, source, "notes").name)
        assertEquals(".env (copy)", CopyFileNamer.nextAvailable(dir, source, ".env").name)
    }
}
