package org.bibletranslationtools.writer.integration.usecases

import io.github.vinceglb.filekit.PlatformFile
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.bibletranslationtools.logger.Logger
import org.bibletranslationtools.writer.BaseIntegrationTest
import org.bibletranslationtools.writer.TestUtils
import org.bibletranslationtools.writer.core.Translator.Companion.TSTUDIO_EXTENSION
import org.bibletranslationtools.writer.core.Translator.Companion.USFM_EXTENSION
import org.bibletranslationtools.writer.displayName
import org.bibletranslationtools.writer.usecases.ImportProjects
import org.bibletranslationtools.writer.utils.Zip
import org.junit.After
import org.junit.Test
import org.koin.core.component.inject
import java.io.File


class ImportProjectsTest : BaseIntegrationTest() {

    private val importProjects: ImportProjects by inject()

    override val needsLibrary = true

    @After
    fun tearDown() {
        Logger.flush()
        runBlocking { directoryProvider.clearCache() }
    }

    @Test
    fun testImportProjectFile() = runTest {
        val projectFile = getProjectFile()

        val result = importProjects.importProject(projectFile, false)

        assertNotNull("Result should not be null", result)
        assertTrue("Import should be successful", result!!.isSuccess)
        assertEquals("Imported slug should match", "aa_mrk_text_reg", result.importedSlug)
        assertFalse("There should be no merge conflict", result.mergeConflict)
        assertFalse("Project should not already exist", result.alreadyExists)


        // Import project again
        val result2 = importProjects.importProject(projectFile, false)

        assertNotNull("Result should not be null", result2)
        assertTrue("Import should be successful", result2!!.isSuccess)
        assertEquals("Imported slug should match", "aa_mrk_text_reg", result2.importedSlug)
        assertTrue("There should be merge conflict", result2.mergeConflict)
        assertTrue("Project should already exist", result2.alreadyExists)

        // Import project again with overwrite flag
        val result3 = importProjects.importProject(projectFile, true)

        assertNotNull("Result should not be null", result3)
        assertTrue("Import should be successful", result3!!.isSuccess)
        assertEquals("Imported slug should match", "aa_mrk_text_reg", result3.importedSlug)
        assertFalse("There should be no merge conflict", result3.mergeConflict)
        assertTrue("Project should already exist", result3.alreadyExists)
    }

    @Test
    fun testImportProjectDirectory() = runTest {
        val projectDirectory = TestUtils.getResourceStream("exports/aa_mrk_text_reg.tstudio").use {
            val tempDir = directoryProvider.createTempDir("test")
            Zip.unzipFromStream(it, tempDir)
            tempDir
        }

        val result = importProjects.importProject(projectDirectory, false)

        assertNotNull("Result should not be null", result)
        assertTrue("Import should be successful", result!!.isSuccess)
        assertEquals("Imported slug should match", "aa_mrk_text_reg", result.importedSlug)
        assertFalse("There should be no merge conflict", result.mergeConflict)
        assertFalse("Project should not already exist", result.alreadyExists)
    }

    @Test
    fun testImportProjectUsfmAsFileFails() = runTest {
        val projectFile = getUSFMFile()

        val result = importProjects.importProject(projectFile, false)

        assertNotNull("Result should not be null", result)
        assertFalse("Import should not be successful", result!!.isSuccess)
        assertNull("Imported slug should be null", result.importedSlug)
        assertFalse("There should be no merge conflict", result.mergeConflict)
    }

    @Test
    fun testImportIncorrectDirectoryFails() = runTest {
        val wrongProjectDirectory = TestUtils.getResourceStream("source/fa_jud_nmv.zip").use {
            val tempDir = directoryProvider.createTempDir("test")
            Zip.unzipFromStream(it, tempDir)
            tempDir
        }

        val result = importProjects.importProject(wrongProjectDirectory, false)

        assertNotNull("Result should not be null", result)
        assertFalse("Import should not be successful", result!!.isSuccess)
        assertNull("Imported slug should be null", result.importedSlug)
        assertFalse("There should be no merge conflict", result.mergeConflict)
        assertFalse("Project should not already exist", result.alreadyExists)
    }

    @Test
    fun testImportProjectUri() = runTest {
        val projectFile = getProjectFile()
        val projectUri = PlatformFile(projectFile)

        var progressMessage: String? = null
        val onProgress: (Float, String?) -> Unit = { _, message ->
            progressMessage = message
        }

        val result = importProjects.importProject(projectUri, false, onProgress)

        assertNotNull("Result should not be null", result)
        assertTrue("Import should be successful", result.success)
        assertNotNull("Progress message should not be null", progressMessage)
        assertEquals("Imported slug should match", "aa_mrk_text_reg", result.importedSlug)
        assertFalse("There should be no merge conflict", result.hasMergeConflict)
        assertFalse("Project should not already exist", result.alreadyExists)
        assertTrue(
            "Path should have with tstudio extension",
            result.file.displayName.endsWith(TSTUDIO_EXTENSION)
        )
        assertEquals("Uri should match", projectUri, result.file)
        assertFalse("File name should not be invalid", result.invalidFileName)
    }

    @Test
    fun testImportUSFMUriShouldFail() = runTest {
        val projectFile = getUSFMFile()
        val projectUri = PlatformFile(projectFile)

        var progressMessage: String? = null
        val onProgress: (Float, String?) -> Unit = { _, message ->
            progressMessage = message
        }

        val result = importProjects.importProject(projectUri, false, onProgress)

        assertNotNull("Result should not be null", result)
        assertFalse("Import should not be successful", result.success)
        assertNotNull("Progress message should not be null", progressMessage)
        assertNull("Imported slug should be null", result.importedSlug)
        assertFalse("There should be no merge conflict", result.hasMergeConflict)
        assertFalse("Project should not already exist", result.alreadyExists)
        assertTrue(
            "Path should have with usfm extension",
            result.file.displayName.endsWith(USFM_EXTENSION)
        )
        assertEquals("Uri should match", projectUri, result.file)
        assertTrue("File name should be invalid", result.invalidFileName)
    }

    @Test
    fun testImportSourceTextFromUriDir() = runTest {
        val sourceDir = getSourceDir()
        val sourceDirUri = PlatformFile(sourceDir)

        assertTrue("Source dir should exist", sourceDir.exists())
        assertTrue("Source dir should be a directory", sourceDir.isDirectory)
        assertTrue(
            "Source dir should have files",
            sourceDir.listFiles()?.isNotEmpty() ?: false
        )

        val result = importProjects.importSource(sourceDirUri, false)

        assertTrue("Import should be successful", result.success)
        assertFalse("There should be no merge conflict", result.hasConflict)
        assertNull("Message should be null", result.error)
        assertNull("Uri should be null", result.file)

        // Import again
        val sourceDir2 = getSourceDir()
        val sourceDir2Uri = PlatformFile(sourceDir2)
        val result2 = importProjects.importSource(sourceDir2Uri, false)

        assertFalse("Import should not be successful", result2.success)
        assertTrue("There should be merge conflict", result2.hasConflict)
        assertNotNull("Error should not be null", result2.error)
        assertNotNull("Uri should not be null", result2.file)

        val sourceFiles = sourceDir2.listFiles()?.map { it.name }?.sorted() ?: emptyList()
        assertTrue("Source files should exist", sourceFiles.isNotEmpty())

        // Overwrite source from result target dir
        val result3 = importProjects.importSource(result2.file!!, true)
        assertTrue("Import should be successful", result3.success)
        assertFalse("There should be no merge conflict", result3.hasConflict)
        assertNull("Message should be null", result3.error)
        assertNull("Uri should be null", result3.file)
    }

    @Test
    fun testImportSourceTextFromUriFileShouldFail() = runTest {
        val sourceFile = getSourceFile()
        val sourceFileUri = PlatformFile(sourceFile)

        val result = importProjects.importSource(sourceFileUri, false)

        assertFalse("Import should not be successful", result.success)
        assertFalse("There should be no merge conflict", result.hasConflict)
        assertNotNull("Message should not be null", result.error)
        assertEquals("File should be equal", sourceFileUri, result.file)
    }

    private suspend fun getProjectFile(): File {
        return TestUtils.getResourceStream("exports/aa_mrk_text_reg.tstudio").use {
            val tempFile = directoryProvider.createTempFile("test", ".tstudio")
            tempFile.outputStream().use { output ->
                it.copyTo(output)
            }
            tempFile
        }
    }

    private suspend fun getUSFMFile(): File {
        return TestUtils.getResourceStream("usfm/18-JOB.usfm").use {
            val tempFile = directoryProvider.createTempFile("test", ".usfm")
            tempFile.outputStream().use { output ->
                it.copyTo(output)
            }
            tempFile
        }
    }

    private suspend fun getSourceFile(): File {
        return TestUtils.getResourceStream("source/fa_jud_nmv.zip").use {
            val tempFile = directoryProvider.createTempFile("test", ".zip")
            tempFile.outputStream().use { output ->
                it.copyTo(output)
            }
            tempFile
        }
    }

    private suspend fun getSourceDir(): File {
        return getSourceFile().inputStream().use {
            val sourceDir = directoryProvider.createTempDir("test")
            Zip.unzipFromStream(it, sourceDir)
            sourceDir
        }
    }
}
