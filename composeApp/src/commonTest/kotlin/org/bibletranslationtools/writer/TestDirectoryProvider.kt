package org.bibletranslationtools.writer

import java.io.File
import java.nio.file.Files

class TestDirectoryProvider : DirectoryProvider {
    private val root = Files.createTempDirectory("btt_writer_test_").toFile()

    override val internalAppDir = File(root, "internal").apply { mkdirs() }
    override val externalAppDir = File(root, "external").apply { mkdirs() }
    override val cacheDir = File(root, "cache").apply { mkdirs() }
}
