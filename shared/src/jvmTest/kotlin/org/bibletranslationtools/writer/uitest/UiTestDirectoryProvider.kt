package org.bibletranslationtools.writer.uitest

import org.bibletranslationtools.writer.DirectoryProvider
import java.io.File
import java.nio.file.Files

class UiTestDirectoryProvider : DirectoryProvider {
    private val root = Files.createTempDirectory("btt_writer_uitest_").toFile()

    override val internalAppDir = File(root, "internal").apply { mkdirs() }
    override val externalAppDir = File(root, "external").apply { mkdirs() }
    override val cacheDir = File(root, "cache").apply { mkdirs() }

    fun cleanup() {
        root.deleteRecursively()
    }
}
