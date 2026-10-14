package com.example.core.io

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.IOException

class AtomicFileWriterTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun atomicallyReplacesAnExistingFileWithTheCompleteNewContent() {
        val target = File(temporaryFolder.root, "document.txt").apply { writeText("old content") }
        AtomicFileWriter.write(target) { output -> output.write("new content".toByteArray(Charsets.UTF_8)) }
        assertEquals("new content", target.readText())
        assertEquals(listOf("document.txt"), temporaryFolder.root.listFiles()?.map { it.name }?.sorted())
    }

    @Test
    fun failedWriteLeavesOriginalBytesUntouchedAndRemovesTemporaryFiles() {
        val target = File(temporaryFolder.root, "document.txt").apply { writeText("important original") }
        try {
            AtomicFileWriter.write(target) { output ->
                output.write("partial replacement".toByteArray(Charsets.UTF_8))
                throw IOException("simulated write failure")
            }
            throw AssertionError("Expected the simulated write failure")
        } catch (expected: IOException) {
            assertEquals("simulated write failure", expected.message)
        }
        assertEquals("important original", target.readText())
        assertFalse(temporaryFolder.root.listFiles().orEmpty().any { it.name.startsWith(".fc-") })
    }

    @Test
    fun createsANewFileAndSupportsAnEmptyPayload() {
        val target = File(temporaryFolder.root, "empty.bin")
        AtomicFileWriter.writeBytes(target, byteArrayOf())
        assertTrue(target.exists())
        assertEquals(0L, target.length())
    }

    @Test
    fun rejectsAMissingParentDirectoryWithoutCreatingAPartialTarget() {
        val missingParent = File(temporaryFolder.root, "missing")
        val target = File(missingParent, "document.txt")
        try {
            AtomicFileWriter.writeBytes(target, "payload".toByteArray())
            throw AssertionError("Expected a missing-parent error")
        } catch (expected: IOException) {
            assertTrue(expected.message.orEmpty().contains("parent directory missing"))
        }
        assertFalse(target.exists())
        assertFalse(missingParent.exists())
    }
}
