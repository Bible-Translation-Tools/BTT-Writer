package org.bibletranslationtools.writer

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.bibletranslationtools.logger.Logger
import org.bibletranslationtools.writer.utils.FileUtilities
import org.bibletranslationtools.writer.utils.Zip
import java.io.File
import java.nio.file.Files

class TestDirectoryProvider : DirectoryProvider {
    private val root = Files.createTempDirectory("btt_writer_test_").toFile()

    override val internalAppDir = File(root, "internal").apply { mkdirs() }
    override val externalAppDir = File(root, "external").apply { mkdirs() }
    override val cacheDir = File(root, "cache").apply { mkdirs() }

    override suspend fun deployDefaultLibrary() {
        Logger.i(TAG, "Deploying the default library to " + containersDir.parentFile)

        withContext(Dispatchers.IO) {
            // delete old database first
            FileUtilities.deleteQuietly(databaseFile)

            TestUtils.getResourceStream("index.sqlite").use { input ->
                databaseFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            // Delete old journal to avoid corrupt database errors
            val shmFile = File(databaseFile.absolutePath + "-shm")
            if (shmFile.exists()) { FileUtilities.deleteQuietly(shmFile) }
            val walFile = File(databaseFile.absolutePath + "-wal")
            if (walFile.exists()) { FileUtilities.deleteQuietly(walFile) }
            val journalFile = File(databaseFile.absolutePath + "-journal")
            if (journalFile.exists()) { FileUtilities.deleteQuietly(journalFile) }

            // extract resource containers
            containersDir.mkdirs()

            TestUtils.getResourceStream("containers.zip").use { input ->
                Zip.unzipFromStream(input, containersDir)
            }
        }
    }

    fun cleanup() {
        root.deleteRecursively()
    }

    companion object {
        const val TAG = "TestDirectoryProvider"
    }
}
