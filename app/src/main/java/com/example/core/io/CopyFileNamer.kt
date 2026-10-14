package com.example.core.io

import java.io.File
import java.io.IOException

/**
 * Selects a new Save As destination without replacing the source or any previous export.
 * The requested name is used unchanged when available; collisions get "(copy)", "(copy 2)", etc.
 */
object CopyFileNamer {

    fun nextAvailable(directory: File, sourceFile: File, requestedName: String): File {
        if (!directory.isDirectory) throw IOException("destination directory does not exist")
        val name = requestedName.trim()
        require(name.isNotEmpty()) { "file name must not be empty" }
        require(File(name).name == name && '/' !in name && '\\' !in name && name != "." && name != "..") {
            "file name must not contain a path"
        }

        fun collides(candidate: File): Boolean = candidate.exists() || sameFile(candidate, sourceFile)

        val requested = File(directory, name)
        if (!collides(requested)) return requested

        val dot = name.lastIndexOf('.')
        val hasExtension = dot > 0 && dot < name.lastIndex
        val stem = if (hasExtension) name.substring(0, dot) else name
        val extension = if (hasExtension) name.substring(dot) else ""

        var copyNumber = 1
        while (copyNumber < Int.MAX_VALUE) {
            val suffix = if (copyNumber == 1) " (copy)" else " (copy $copyNumber)"
            val candidate = File(directory, "$stem$suffix$extension")
            if (!collides(candidate)) return candidate
            copyNumber++
        }
        throw IOException("no available copy filename")
    }

    private fun sameFile(first: File, second: File): Boolean =
        try {
            first.canonicalFile == second.canonicalFile
        } catch (_: IOException) {
            first.absoluteFile == second.absoluteFile
        }
}
