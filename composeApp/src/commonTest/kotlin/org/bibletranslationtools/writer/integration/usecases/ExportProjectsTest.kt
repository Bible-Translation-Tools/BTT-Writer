package org.bibletranslationtools.writer.integration.usecases

import io.github.vinceglb.filekit.PlatformFile
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.bibletranslationtools.logger.Logger
import org.bibletranslationtools.resourcecatalog.ResourceCatalogClient
import org.bibletranslationtools.resourcecatalog.library.models.TargetLanguage
import org.bibletranslationtools.writer.BaseIntegrationTest
import org.bibletranslationtools.writer.Platform
import org.bibletranslationtools.writer.TestUtils
import org.bibletranslationtools.writer.core.ProcessUSFM
import org.bibletranslationtools.writer.core.Profile
import org.bibletranslationtools.writer.core.TargetTranslation
import org.bibletranslationtools.writer.core.Translator.Companion.PDF_EXTENSION
import org.bibletranslationtools.writer.core.Translator.Companion.TSTUDIO_EXTENSION
import org.bibletranslationtools.writer.core.Translator.Companion.USFM_EXTENSION
import org.bibletranslationtools.writer.usecases.ExportProjects
import org.bibletranslationtools.writer.usecases.ImportProjects
import org.bibletranslationtools.writer.utils.Zip
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.koin.core.component.inject
import java.io.File
import java.io.FileInputStream

class ExportProjectsTest : BaseIntegrationTest() {

    private val exportProjects: ExportProjects by inject()
    private val catalogClient: ResourceCatalogClient by inject()
    private val profile: Profile by inject()
    private val importProjects: ImportProjects by inject()
    private val platform: Platform by inject()
    private val processUSFM: ProcessUSFM by inject()

    private var targetTranslation: TargetTranslation? = null
    private var targetLanguage: TargetLanguage? = null

    override val needsLibrary = true

    @Before
    fun setUp() {
        targetLanguage = catalogClient.library.getTargetLanguage("aa")
        targetTranslation = runBlocking {
            TestUtils.importTargetTranslation(
                catalogClient,
                platform,
                directoryProvider,
                profile,
                importProjects,
                "aa",
                "usfm/mrk.usfm"
            )
        }
    }

    @After
    fun tearDown() {
        Logger.flush()
        runBlocking { directoryProvider.clearCache() }
    }

    @Test
    fun testExportProjectFromTargetTranslationToFile() = runTest {
        assertNotNull("Target translation should not be null", targetTranslation)

        val tempFile = directoryProvider.createTempFile("aa_mrk_text_reg", ".tstudio")

        exportProjects.exportProject(targetTranslation!!, tempFile)

        testExportedProjectCorrect(tempFile)
    }

    @Test
    fun testExportProjectFromTargetTranslationWithBadHead() = runTest {
        assertNotNull("Target translation should not be null", targetTranslation)

        // Commit to force git initialization
        targetTranslation!!.commitSync(".", false)

        // Make HEAD file bad
        val headFile = targetTranslation!!.path.listFiles()?.singleOrNull {
            it.name == ".git"
        }?.listFiles()?.firstOrNull {
            it.name == "HEAD"
        }
        assertNotNull("Head file should exist", headFile)
        headFile?.writeText("some_bad_data")

        val tempFile = directoryProvider.createTempFile("aa_mrk_text_reg", ".tstudio")

        exportProjects.exportProject(targetTranslation!!, tempFile)

        testExportedProjectCorrect(tempFile)
    }

    @Test
    fun testExportProjectFromTargetTranslationToUri() = runTest {
        assertNotNull("Target translation should not be null", targetTranslation)

        val tempFile = directoryProvider.createTempFile("aa_mrk_text_reg", ".tstudio")
        val tempFileUri = PlatformFile(tempFile)

        val result = exportProjects.exportProject(targetTranslation!!, tempFileUri)

        assertTrue("Result should be successful", result.success)
        assertEquals("Result URI should match", tempFileUri, result.file)
        assertEquals("Result export type should be PROJECT", ExportProjects.ExportType.PROJECT, result.exportType)

        testExportedProjectCorrect(tempFile)
    }

    @Test
    fun testExportProjectFromDirToFile() = runTest {
        assertNotNull("Target translation should not be null", targetTranslation)

        val tempFile = directoryProvider.createTempFile("aa_mrk_text_reg", ".tstudio")

        exportProjects.exportProject(targetTranslation!!.path, tempFile)

        testExportedProjectCorrect(tempFile)
    }

    @Test
    fun testExportProjectToUSFM() = runTest {
        assertNotNull("Target translation should not be null", targetTranslation)

        val tempFile = directoryProvider.createTempFile("aa_mrk_text_reg", ".usfm")
        val tempFileUri = PlatformFile(tempFile)

        val result = exportProjects.exportUSFM(targetTranslation!!, tempFileUri)

        assertTrue("Result should be successful", result.success)
        assertEquals("Result URI should match", tempFileUri, result.file)
        assertEquals("Result export type should be PROJECT", ExportProjects.ExportType.USFM, result.exportType)

        testExportedUSFMFileCorrect(tempFile)
    }

    @Test
    fun testExportProjectToPDF() = runTest {
        assertNotNull("Target translation should not be null", targetTranslation)

        val tempFile = directoryProvider.createTempFile("aa_mrk_text_reg", ".pdf")
        val tempFileUri = PlatformFile(tempFile)

        val tempDir = directoryProvider.createTempDir("images")

        val result = exportProjects.exportPDF(
            targetTranslation!!,
            tempFileUri,
            includeImages = true,
            includeIncompleteFrames = true,
            tempDir
        )

        assertTrue("Result should be successful", result.success)
        assertEquals("Result URI should match", tempFileUri, result.file)
        assertEquals("Result export type should be PROJECT", ExportProjects.ExportType.PDF, result.exportType)

        assertTrue(
            "Images dir should be empty for non-obs project",
            tempDir.listFiles().isNullOrEmpty()
        )

        assertEquals("This should be PDF file", tempFile.extension, PDF_EXTENSION)
        assertTrue("Temp file should exist", tempFile.exists())
        assertTrue("Temp file should not be empty", tempFile.length() > 0)
    }

    private suspend fun testExportedProjectCorrect(file: File) {
        assertEquals("This should be project file", file.extension, TSTUDIO_EXTENSION)
        assertTrue("Temp file should exist", file.exists())
        assertTrue("Temp file should not be empty", file.length() > 0)

        val projectDirs = directoryProvider.createTempDir("project")

        FileInputStream(file).use {
            Zip.unzipFromStream(it, projectDirs)
        }

        assertTrue("projectDirs should exist", projectDirs.exists())
        assertFalse("Project dirs should not be empty", projectDirs.listFiles().isNullOrEmpty())

        // Find targetTranslation directory
        val projectDir = projectDirs.listFiles()?.first {
            it.name == targetTranslation!!.id
        }!!

        assertNotNull("Project dir should exist", projectDir)

        val destTranslation = TargetTranslation.open(projectDir, null)

        assertNotNull("Destination target translation should exist", destTranslation)
        assertEquals(
            "Destination target translation should match",
            targetTranslation!!.id,
            destTranslation!!.id
        )
    }

    private suspend fun testExportedUSFMFileCorrect(file: File) {
        assertEquals("This should be usfm file", file.extension, USFM_EXTENSION)
        assertTrue("Temp file should exist", file.exists())
        assertTrue("Temp file should not be empty", file.length() > 0)

        assertNotNull("Target language should not be null", targetLanguage)

        val session = processUSFM.startImport(targetLanguage!!, PlatformFile(file))

        assertTrue("USFM process should be successful", session.isSuccess)
        assertEquals("Import projects should be 1", 1, session.importedProjects.size)

        val projectId = session.importedProjects.first().name
        val (project, language) = projectId.split("-")

        assertEquals("Project ID should match", targetTranslation!!.projectId, project)
        assertEquals("Language ID should match", targetLanguage!!.slug, language)
    }
}
