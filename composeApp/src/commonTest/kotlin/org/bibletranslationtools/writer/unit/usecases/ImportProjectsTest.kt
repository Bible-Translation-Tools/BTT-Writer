package org.bibletranslationtools.writer.unit.usecases

import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.isDirectory
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkAll
import io.mockk.verify
import io.mockk.verifySequence
import kotlinx.coroutines.test.runTest
import org.bibletranslationtools.resourcecatalog.ResourceCatalogClient
import org.bibletranslationtools.resourcecontainer.Language
import org.bibletranslationtools.resourcecontainer.Project
import org.bibletranslationtools.resourcecontainer.Resource
import org.bibletranslationtools.resourcecontainer.ResourceContainer
import org.bibletranslationtools.writer.AppInfo
import org.bibletranslationtools.writer.DirectoryProvider
import org.bibletranslationtools.writer.Platform
import org.bibletranslationtools.writer.TestUtils
import org.bibletranslationtools.writer.core.ArchiveImporter
import org.bibletranslationtools.writer.core.MergeConflictsHandler
import org.bibletranslationtools.writer.core.TargetTranslation
import org.bibletranslationtools.writer.core.Translator
import org.bibletranslationtools.writer.displayName
import org.bibletranslationtools.writer.inputStream
import org.bibletranslationtools.writer.usecases.BackupRC
import org.bibletranslationtools.writer.usecases.ImportProjects
import org.bibletranslationtools.writer.utils.FileUtilities
import org.bibletranslationtools.writer.utils.Zip
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.InputStream

class ImportProjectsTest {

    @MockK private lateinit var translator: Translator
    @MockK private lateinit var backupRC: BackupRC
    @MockK private lateinit var directoryProvider: DirectoryProvider
    @MockK private lateinit var archiveImporter: ArchiveImporter
    @MockK private lateinit var catalogClient: ResourceCatalogClient
    @MockK private lateinit var platform: Platform
    @MockK private lateinit var info: AppInfo

    private val onProgress = mockk<(Float, String?) -> Unit>(relaxed = true)

    @JvmField
    @Rule
    var tempDir: TemporaryFolder = TemporaryFolder()

    private lateinit var tStudioFile: File
    private lateinit var pdfFile: File

    @Before
    fun setup() {
        MockKAnnotations.init(this)

        every { info.versionCode }.returns(1)
        every { platform.info }.returns(info)

        tStudioFile = tempDir.newFile("aa_mrk_text_ulb.tstudio")
        pdfFile = tempDir.newFile("aa_mrk_text_ulb.pdf")

        tStudioFile.writeText("tstudio")
        pdfFile.writeText("pdf")

        mockkObject(Zip)
        every { Zip.unzipFromStream(any(), any()) }.answers {
            val content = firstArg<InputStream>().bufferedReader().use { it.readText() }
            if (!content.contains("tstudio")) {
                throw Exception("Invalid file")
            }
        }

        mockkObject(TargetTranslation)
        mockkStatic(PlatformFile::inputStream)
        mockkStatic(PlatformFile::displayName)
        mockkStatic(PlatformFile::isDirectory)

        every { directoryProvider.cacheDir }.returns(tempDir.newFolder("cache"))
        every { translator.path }.returns(tempDir.newFolder("translations"))

        mockkObject(FileUtilities)
        every { FileUtilities.deleteQuietly(any()) }.returns(true)
        every { FileUtilities.moveOrCopyQuietly(any(), any()) }.returns(true)
        every { FileUtilities.safeDelete(any()) }.just(runs)
        coEvery { FileUtilities.copyDirectory(any<PlatformFile>(), any<PlatformFile>(), any()) }.just(runs)

        every { onProgress(any(), any()) }.just(runs)

        mockkObject(MergeConflictsHandler)
        mockkObject(ResourceContainer)

        coEvery { backupRC.backupTargetTranslation(any()) }.returns(true)
        coJustRun { translator.deleteTargetTranslation(any<File>()) }
    }

    @After
    fun tearDown() {
        unmockkAll()
        tempDir.delete()
    }

    @Test
    fun `test import new project from file`() = runTest {
        val dir = tempDir.newFolder("aa_mrk_text_ulb")
        val targetTranslation: TargetTranslation = mockk {
            every { id }.returns("aa_mrk_text_ulb")
            every { updateGenerator(any()) }.just(runs)
            every { commitSync() }.returns(true)
        }

        coEvery { archiveImporter.importArchive(any()) }
            .returns(listOf(dir))

        coEvery { TargetTranslation.open(dir, any()) }.returns(targetTranslation)

        val result = ImportProjects(
            translator,
            backupRC,
            directoryProvider,
            archiveImporter,
            catalogClient,
            platform
        ).importProject(tStudioFile, false)

        assertNotNull(result)
        requireNotNull(result)

        assertTrue(result.isSuccess)
        assertEquals(targetTranslation.id, result.importedSlug)
        assertFalse(result.alreadyExists)
        assertFalse(result.mergeConflict)

        verifyImportSuccess(targetTranslation)
    }

    @Test
    fun `test import new project from dir`() = runTest {
        val importDir = tempDir.newFolder("aa_mrk_text_ulb_import")
        val dir = tempDir.newFolder("aa_mrk_text_ulb")
        val targetTranslation: TargetTranslation = mockk {
            every { id }.returns("aa_mrk_text_ulb")
            every { commitSync() }.returns(true)
        }

        coEvery { archiveImporter.importArchive(any()) }
            .returns(listOf(dir))

        coEvery { TargetTranslation.open(dir, any()) }.returns(targetTranslation)

        val result = ImportProjects(
            translator,
            backupRC,
            directoryProvider,
            archiveImporter,
            catalogClient,
            platform
        ).importProject(importDir, false)

        assertNotNull(result)
        requireNotNull(result)

        assertTrue(result.isSuccess)
        assertEquals(targetTranslation.id, result.importedSlug)
        assertFalse(result.alreadyExists)
        assertFalse(result.mergeConflict)

        verify(exactly = 0) { Zip.unzipFromStream(any(), any()) }
        verify(exactly = 0) { directoryProvider.cacheDir }

        verify { translator.path }
        verify { FileUtilities.deleteQuietly(any()) }
        verify { targetTranslation.id }
        coVerify { archiveImporter.importArchive(any()) }
        coVerify { TargetTranslation.open(any(), any()) }
        verify { FileUtilities.deleteQuietly(any()) }
    }

    @Test
    fun `test import project over old one from file, no overwrite, merge success`() = runTest {
        val dir = tempDir.newFolder("aa_mrk_text_ulb")
        val targetTranslation: TargetTranslation = mockk {
            every { id }.returns("aa_mrk_text_ulb")
            every { updateGenerator(any()) }.just(runs)
        }
        val localTranslation: TargetTranslation = mockk {
            every { id }.returns("aa_mrk_text_ulb")
            every { commitSync() }.returns(true)
            coEvery { merge(any(), any()) }.returns(true)
            every { updateGenerator(any()) }.just(runs)
        }

        coEvery { archiveImporter.importArchive(any()) }
            .returns(listOf(dir))

        coEvery { TargetTranslation.open(any(), any()) }.answers {
            val translation = firstArg<File>()
            if (!translation.absolutePath.startsWith(translator.path.absolutePath)) {
                targetTranslation
            } else {
                // local translation exists
                localTranslation
            }
        }

        val result = ImportProjects(
            translator,
            backupRC,
            directoryProvider,
            archiveImporter,
            catalogClient,
            platform
        ).importProject(tStudioFile, false)

        assertNotNull(result)
        requireNotNull(result)

        assertTrue(result.isSuccess)
        assertEquals(targetTranslation.id, result.importedSlug)
        assertTrue(result.alreadyExists)
        assertFalse(result.mergeConflict)

        verifyImportSuccess(targetTranslation)

        verify { localTranslation.commitSync() }
        coVerify { localTranslation.merge(any(), any()) }
    }

    @Test
    fun `test import project over old one from file, no overwrite, merge fails`() = runTest {
        val dir = tempDir.newFolder("aa_mrk_text_ulb")
        val targetTranslation: TargetTranslation = mockk {
            every { id }.returns("aa_mrk_text_ulb")
            every { updateGenerator(any()) }.just(runs)
        }
        val localTranslation: TargetTranslation = mockk {
            every { id }.returns("aa_mrk_text_ulb")
            every { commitSync() }.returns(true)
            coEvery { merge(any(), any()) }.returns(false)
            every { updateGenerator(any()) }.just(runs)
        }

        coEvery { archiveImporter.importArchive(any()) }
            .returns(listOf(dir))

        coEvery { TargetTranslation.open(any(), any()) }.answers {
            val translation = firstArg<File>()
            if (!translation.absolutePath.startsWith(translator.path.absolutePath)) {
                targetTranslation
            } else {
                // local translation exists
                localTranslation
            }
        }

        val result = ImportProjects(
            translator,
            backupRC,
            directoryProvider,
            archiveImporter,
            catalogClient,
            platform
        ).importProject(tStudioFile, false)

        assertNotNull(result)
        requireNotNull(result)

        assertTrue(result.isSuccess)
        assertEquals(targetTranslation.id, result.importedSlug)
        assertTrue(result.alreadyExists)
        assertTrue(result.mergeConflict)

        verifyImportSuccess(targetTranslation)

        verify { localTranslation.commitSync() }
        coVerify { localTranslation.merge(any(), any()) }
    }

    @Test
    fun `test import project over old one from file with overwrite`() = runTest {
        val dir = tempDir.newFolder("aa_mrk_text_ulb")
        val targetTranslation: TargetTranslation = mockk {
            every { id }.returns("aa_mrk_text_ulb")
            every { updateGenerator(any()) }.just(runs)
        }
        val localTranslation: TargetTranslation = mockk {
            every { id }.returns("aa_mrk_text_ulb")
            every { commitSync() }.returns(true)
            coEvery { merge(any(), any()) }.returns(false)
            every { updateGenerator(any()) }.just(runs)
        }

        coEvery { archiveImporter.importArchive(any()) }
            .returns(listOf(dir))

        coEvery { TargetTranslation.open(any(), any()) }.answers {
            val translation = firstArg<File>()
            if (!translation.absolutePath.startsWith(translator.path.absolutePath)) {
                targetTranslation
            } else {
                // local translation exists
                localTranslation
            }
        }

        val result = ImportProjects(
            translator,
            backupRC,
            directoryProvider,
            archiveImporter,
            catalogClient,
            platform
        ).importProject(tStudioFile, true)

        assertNotNull(result)
        requireNotNull(result)

        assertTrue(result.isSuccess)
        assertEquals(targetTranslation.id, result.importedSlug)
        assertTrue(result.alreadyExists)
        assertFalse(result.mergeConflict)

        verifyImportSuccess(targetTranslation)

        // Merge should not happen
        verify(exactly = 0) { localTranslation.commitSync() }
        coVerify(exactly = 0) { localTranslation.merge(any(), any()) }
    }

    @Test
    fun `test import project from file, throws exception`() = runTest {
        coEvery { archiveImporter.importArchive(any()) }.throws(Exception("An error occurred!"))

        val result = ImportProjects(
            translator,
            backupRC,
            directoryProvider,
            archiveImporter,
            catalogClient,
            platform
        ).importProject(tStudioFile, false)

        assertNull(result)

        verifyImportFail()

        verify { FileUtilities.deleteQuietly(any()) }
        coVerify { archiveImporter.importArchive(any()) }
    }

    @Test
    fun `test import project invalid file`() = runTest {
        val result = ImportProjects(
            translator,
            backupRC,
            directoryProvider,
            archiveImporter,
            catalogClient,
            platform
        ).importProject(pdfFile, true)

        assertNull(result)

        verifyImportFail()

        verify(exactly = 0) { FileUtilities.deleteQuietly(any()) }
        coVerify(exactly = 0) { archiveImporter.importArchive(any()) }
    }

    @Test
    fun `test import projects from files`() = runTest {
        val project1 = tempDir.newFile("ru_mrk_text_ulb.tstudio")
        val project2 = tempDir.newFile("fr_gen_text_ulb.tstudio")
        val translation1: TargetTranslation = mockk {
            every { id }.returns("ru_mrk_text_ulb")
            every { commitSync() }.returns(true)
            every { updateGenerator(any()) }.just(runs)
        }
        val translation2: TargetTranslation = mockk {
            every { id }.returns("fr_gen_text_ulb")
            every { commitSync() }.returns(false)
            every { updateGenerator(any()) }.just(runs)
        }
        val localTranslation: TargetTranslation = mockk {
            every { id }.returns("fr_gen_text_ulb")
            every { commitSync() }.returns(true)
            coEvery { merge(any(), any()) }.returns(true)
            every { updateGenerator(any()) }.just(runs)
        }
        coEvery { translator.getConflictingTargetTranslation(any()) }.answers {
            val file = firstArg<File>()
            if (file.name == project2.name) localTranslation
            else null
        }

        coEvery { TargetTranslation.open(any(), any()) }.answers {
            val file = firstArg<File>()
            when (file.name) {
                project1.name -> translation1
                project2.name -> translation2
                "ru_mrk_text_ulb" -> translation1
                "fr_gen_text_ulb" -> localTranslation
                else -> null
            }
        }

        val result = ImportProjects(
            translator,
            backupRC,
            directoryProvider,
            archiveImporter,
            catalogClient,
            platform
        ).importProjects(
            listOf(project1, project2),
            false,
            onProgress
        )

        assertTrue(result.success)
        assertEquals(localTranslation.id, result.conflictingTargetTranslations.first().id)

        verifySequence {
            onProgress(0f, project1.name)
            onProgress(0.125f, project1.name)
            onProgress(0.5f, project2.name)
            onProgress(0.625f, project2.name)
            onProgress(0.75f, project2.name)
            onProgress(1f,  "Completed!")
        }

        coVerify { translator.getConflictingTargetTranslation(any()) }
    }

    @Test
    fun `test import projects from files with merge exception`() = runTest {
        val project = tempDir.newFile("id_mrk_text_ulb.tstudio")
        val translation: TargetTranslation = mockk {
            every { id }.returns("id_mrk_text_ulb")
            every { commitSync() }.returns(true)
            every { updateGenerator(any()) }.just(runs)
        }
        val localTranslation: TargetTranslation = mockk {
            every { id }.returns("id_mrk_text_ulb")
            every { commitSync() }.returns(true)
            coEvery { merge(any(), any()) }.throws(Exception("merge error"))
            every { updateGenerator(any()) }.just(runs)
        }
        coEvery { translator.getConflictingTargetTranslation(any()) }.returns(localTranslation)

        coEvery { TargetTranslation.open(any(), any()) }.answers {
            val file = firstArg<File>()
            when {
                file.name == project.name -> translation
                file.name == "id_mrk_text_ulb" -> localTranslation
                else -> null
            }
        }

        val result = ImportProjects(
            translator,
            backupRC,
            directoryProvider,
            archiveImporter,
            catalogClient,
            platform
        ).importProjects(
            listOf(project),
            false,
            onProgress
        )

        assertFalse(result.success)
        assertEquals(true, result.conflictingTargetTranslations.isEmpty())

        verifySequence {
            onProgress(0f, project.name)
            onProgress(0.25f, project.name)
            onProgress(0.5f, project.name)
            onProgress(1f, "Completed!")
        }

        coVerify { translator.getConflictingTargetTranslation(any()) }
    }

    @Test
    fun `test import project from uri`() = runTest {
        val file: PlatformFile = mockk {
            every { displayName }.returns("aa_mrk_text_ulb.tstudio")
        }

        every { file.inputStream() }.returns(tStudioFile.inputStream())

        val dir = tempDir.newFolder("aa_mrk_text_ulb")
        val targetTranslation: TargetTranslation = mockk {
            every { id }.returns("aa_mrk_text_ulb")
            every { updateGenerator(any()) }.just(runs)
            every { commitSync() }.returns(true)
        }

        coEvery { TargetTranslation.open(dir, any()) }.returns(targetTranslation)

        coEvery { archiveImporter.importArchive(any()) }
            .returns(listOf(dir))

        val result = ImportProjects(
            translator,
            backupRC,
            directoryProvider,
            archiveImporter,
            catalogClient,
            platform
        ).importProject(file, false, onProgress)

        assertTrue(result.success)
        assertEquals(file, result.file)
        assertEquals("aa_mrk_text_ulb.tstudio", result.file.displayName)
        assertEquals("aa_mrk_text_ulb", result.importedSlug)
        assertFalse(result.alreadyExists)
        assertFalse(result.hasMergeConflict)
        assertFalse(result.invalidFileName)

        verifyUriImport(targetTranslation)
    }

    @Test
    fun `test import project from uri with merge conflict`() = runTest {
        val platformFile: PlatformFile = mockk {
            every { displayName }.returns("aa_mrk_text_ulb.tstudio")
        }

        every { platformFile.inputStream() }.returns(tStudioFile.inputStream())

        val dir = tempDir.newFolder("aa_mrk_text_ulb")
        val targetTranslation: TargetTranslation = mockk {
            every { id }.returns("aa_mrk_text_ulb")
            every { updateGenerator(any()) }.just(runs)
        }
        val localTranslation: TargetTranslation = mockk {
            every { id }.returns("aa_mrk_text_ulb")
            every { commitSync() }.returns(true)
            coEvery { merge(any(), any()) }.returns(false)
            every { updateGenerator(any()) }.just(runs)
        }

        coEvery { TargetTranslation.open(any(), any()) }.answers {
            val file = firstArg<File>()
            if (!file.absolutePath.startsWith(translator.path.absolutePath)) {
                targetTranslation
            } else {
                // local translation exists
                localTranslation
            }
        }

        coEvery { archiveImporter.importArchive(any()) }
            .returns(listOf(dir))

        coEvery { MergeConflictsHandler.isTranslationMergeConflicted(any(), any()) }
            .returns(true)

        val result = ImportProjects(
            translator,
            backupRC,
            directoryProvider,
            archiveImporter,
            catalogClient,
            platform
        ).importProject(platformFile, false, onProgress)

        assertTrue(result.success)
        assertEquals(platformFile, result.file)
        assertEquals("aa_mrk_text_ulb.tstudio", result.file.displayName)
        assertEquals("aa_mrk_text_ulb", result.importedSlug)
        assertTrue(result.alreadyExists)
        assertTrue(result.hasMergeConflict)
        assertFalse(result.invalidFileName)

        verifyUriImport(targetTranslation)
    }

    @Test
    fun `test import project from uri with merge conflict overwrite`() = runTest {
        val platformFile: PlatformFile = mockk {
            every { displayName }.returns("aa_mrk_text_ulb.tstudio")
        }

        every { platformFile.inputStream() }.returns(tStudioFile.inputStream())

        val dir = tempDir.newFolder("aa_mrk_text_ulb")
        val targetTranslation: TargetTranslation = mockk {
            every { id }.returns("aa_mrk_text_ulb")
            every { updateGenerator(any()) }.just(runs)
        }
        val localTranslation: TargetTranslation = mockk {
            every { id }.returns("aa_mrk_text_ulb")
            every { commitSync() }.returns(true)
            coEvery { merge(any(), any()) }.returns(false)
            every { updateGenerator(any()) }.just(runs)
        }

        coEvery { TargetTranslation.open(any(), any()) }.answers {
            val file = firstArg<File>()
            if (!file.absolutePath.startsWith(translator.path.absolutePath)) {
                targetTranslation
            } else {
                // local translation exists
                localTranslation
            }
        }

        coEvery { archiveImporter.importArchive(any()) }
            .returns(listOf(dir))

        coEvery { MergeConflictsHandler.isTranslationMergeConflicted(any(), any()) }
            .returns(true)

        val result = ImportProjects(
            translator,
            backupRC,
            directoryProvider,
            archiveImporter,
            catalogClient,
            platform
        ).importProject(platformFile, true, onProgress)

        assertTrue(result.success)
        assertEquals(platformFile, result.file)
        assertEquals("aa_mrk_text_ulb.tstudio", result.file.displayName)
        assertEquals("aa_mrk_text_ulb", result.importedSlug)
        assertTrue(result.alreadyExists)
        assertFalse(result.hasMergeConflict)
        assertFalse(result.invalidFileName)

        verifyUriImport(targetTranslation)
    }

    @Test
    fun `test import project from invalid file uri`() = runTest {
        val file: PlatformFile = mockk {
            every { displayName }.returns("aa_mrk_text_ulb.pdf")
        }

        val result = ImportProjects(
            translator,
            backupRC,
            directoryProvider,
            archiveImporter,
            catalogClient,
            platform
        ).importProject(file, true, onProgress)

        assertFalse(result.success)
        assertEquals(file, result.file)
        assertEquals("aa_mrk_text_ulb.pdf", result.file.displayName)
        assertNull(result.importedSlug)
        assertFalse(result.alreadyExists)
        assertFalse(result.hasMergeConflict)
        assertTrue(result.invalidFileName)

        coVerify(exactly = 0) { TargetTranslation.open(any(), any()) }
        coVerify(exactly = 0) { archiveImporter.importArchive(any()) }
    }

    @Test
    fun `test import new source text from uri`() = runTest {
        val file: PlatformFile = mockk()
        every { file.isDirectory() }.returns(true)

        val srcDir = tempDir.newFolder("fa_mrk_nmv")
        coEvery { directoryProvider.createTempDir(any()) }.returns(srcDir)

        every { catalogClient.openResourceContainer(any()) }.throws(Exception("local rc not found."))
        coEvery { catalogClient.importResourceContainer(srcDir) }.returns(mockk())

        val tempRc: ResourceContainer = mockk {
            every { slug }.returns("en")
        }
        every { ResourceContainer.load(srcDir) }.returns(tempRc)

        val result = ImportProjects(
            translator,
            backupRC,
            directoryProvider,
            archiveImporter,
            catalogClient,
            platform
        ).importSource(file, false)

        assertTrue(result.success)
        assertFalse(result.hasConflict)
        assertNull(result.error)
        assertNull(result.file)

        coVerify { directoryProvider.createTempDir(any()) }
        coVerify { FileUtilities.copyDirectory(any<PlatformFile>(), any<PlatformFile>(), any()) }
        verify { catalogClient.openResourceContainer(any()) }
        coVerify { catalogClient.importResourceContainer(srcDir) }
        verify { ResourceContainer.load(srcDir) }
        verify { FileUtilities.deleteQuietly(any()) }
    }

    @Test
    fun `test import existing source text from uri fails`() = runTest {
        val file: PlatformFile = mockk()
        every { file.isDirectory() }.returns(true)

        val srcDir = tempDir.newFolder("fa_mrk_nmv")
        coEvery { directoryProvider.createTempDir(any()) }.returns(srcDir)

        every { catalogClient.openResourceContainer(any()) }.returns(mockk())
        coEvery { catalogClient.importResourceContainer(srcDir) }.returns(mockk())

        val tempRc = mockResourceContainer()
        every { ResourceContainer.load(srcDir) }.returns(tempRc)

        val expectedErrorMessage = "Overwrite Farsi - Mark - New Millennium Version?"

        val result = ImportProjects(
            translator,
            backupRC,
            directoryProvider,
            archiveImporter,
            catalogClient,
            platform
        ).importSource(file, false)

        assertFalse(result.success)
        assertTrue(result.hasConflict)
        assertEquals(expectedErrorMessage, result.error)
        assertEquals(file, result.file)

        coVerify { directoryProvider.createTempDir(any()) }
        coVerify { FileUtilities.copyDirectory(any<PlatformFile>(), any<PlatformFile>(), any()) }
        verify { catalogClient.openResourceContainer(any()) }
        coVerify(exactly = 0) { catalogClient.importResourceContainer(srcDir) }
        verify { ResourceContainer.load(srcDir) }
        verify { FileUtilities.deleteQuietly(any()) }
    }

    @Test
    fun `test import existing source text from uri overwrite`() = runTest {
        val file: PlatformFile = mockk()
        every { file.isDirectory() }.returns(true)

        val srcDir = tempDir.newFolder("fa_mrk_nmv")
        coEvery { directoryProvider.createTempDir(any()) }.returns(srcDir)

        every { catalogClient.openResourceContainer(any()) }.returns(mockk())
        coEvery { catalogClient.importResourceContainer(srcDir) }.returns(mockk())

        val tempRc = mockResourceContainer()
        TestUtils.setPropertyReflection(tempRc, "slug", "en")
        every { ResourceContainer.load(srcDir) }.returns(tempRc)

        val result = ImportProjects(
            translator,
            backupRC,
            directoryProvider,
            archiveImporter,
            catalogClient,
            platform
        ).importSource(file, true)

        assertTrue(result.success)
        assertFalse(result.hasConflict)
        assertNull(result.error)
        assertNull(result.file)

        coVerify { directoryProvider.createTempDir(any()) }
        coVerify { FileUtilities.copyDirectory(any<PlatformFile>(), any<PlatformFile>(), any()) }
        verify { catalogClient.openResourceContainer(any()) }
        coVerify { catalogClient.importResourceContainer(srcDir) }
        verify { ResourceContainer.load(srcDir) }
        verify { FileUtilities.deleteQuietly(any()) }
    }

    @Test
    fun `test import invalid source text from uri`() = runTest {
        val file: PlatformFile = mockk()
        every { file.isDirectory() }.returns(true)

        val srcDir = tempDir.newFolder("fa_mrk_nmv")
        coEvery { directoryProvider.createTempDir(any()) }.returns(srcDir)

        every { ResourceContainer.load(srcDir) }.throws(Exception("Invalid rc."))

        val expectedErrorMessage = "Invalid rc."

        val result = ImportProjects(
            translator,
            backupRC,
            directoryProvider,
            archiveImporter,
            catalogClient,
            platform
        ).importSource(file, false)

        assertFalse(result.success)
        assertFalse(result.hasConflict)
        assertEquals(expectedErrorMessage, result.error)
        assertNull(result.file)

        coVerify { directoryProvider.createTempDir(any()) }
        coVerify { FileUtilities.copyDirectory(any<PlatformFile>(), any<PlatformFile>(), any()) }
        verify(exactly = 0) { catalogClient.openResourceContainer(any()) }
        coVerify(exactly = 0) { catalogClient.importResourceContainer(srcDir) }
        verify { ResourceContainer.load(srcDir) }
        verify(exactly = 0) { FileUtilities.deleteQuietly(any()) }
    }

    @Test
    fun `test import source text from uri failed`() = runTest {
        val file: PlatformFile = mockk()
        every { file.isDirectory() }.returns(true)

        val srcDir = tempDir.newFolder("fa_mrk_nmv")
        coEvery { directoryProvider.createTempDir(any()) }.returns(srcDir)

        every { catalogClient.openResourceContainer(any()) }.throws(Exception("local rc not found."))
        coEvery { catalogClient.importResourceContainer(srcDir) }.throws(Exception("Failed to import rc."))

        val tempRc: ResourceContainer = mockk {
            every { slug }.returns("slug")
        }
        TestUtils.setPropertyReflection(tempRc, "slug", "en")
        every { ResourceContainer.load(srcDir) }.returns(tempRc)

        val expectedErrorMessage = "Failed to import rc."

        val result = ImportProjects(
            translator,
            backupRC,
            directoryProvider,
            archiveImporter,
            catalogClient,
            platform
        ).importSource(file, false)

        assertFalse(result.success)
        assertFalse(result.hasConflict)
        assertEquals(expectedErrorMessage, result.error)
        assertNull(result.file)

        coVerify { directoryProvider.createTempDir(any()) }
        coVerify { FileUtilities.copyDirectory(any<PlatformFile>(), any<PlatformFile>(), any()) }
        verify { catalogClient.openResourceContainer(any()) }
        coVerify { catalogClient.importResourceContainer(srcDir) }
        verify { ResourceContainer.load(srcDir) }
        verify { FileUtilities.deleteQuietly(any()) }
    }

    private fun verifyImportSuccess(targetTranslation: TargetTranslation) {
        verify { Zip.unzipFromStream(any(), any()) }
        verify { directoryProvider.cacheDir }
        verify { translator.path }
        verify { FileUtilities.deleteQuietly(any()) }
        verify { targetTranslation.id }
        coVerify { archiveImporter.importArchive(any()) }
        coVerify { TargetTranslation.open(any(), any()) }
        verify { FileUtilities.deleteQuietly(any()) }
    }

    private fun verifyImportFail() {
        verify { Zip.unzipFromStream(any(), any()) }
        verify { directoryProvider.cacheDir }
        verify(exactly = 0) { translator.path }
        coVerify(exactly = 0) { TargetTranslation.open(any(), any()) }
    }

    private fun verifyUriImport(targetTranslation: TargetTranslation) {
        verify { targetTranslation.id }
        coVerify { TargetTranslation.open(any(), any()) }
        coVerify { archiveImporter.importArchive(any()) }
    }

    private fun mockResourceContainer(): ResourceContainer {
        val mockLanguage: Language = mockk {
            every { name }.returns("Farsi")
        }
        val mockProject: Project = mockk {
            every { name }.returns("Mark")
        }
        val mockResource: Resource = mockk {
            every { name }.returns("New Millennium Version")
        }

        val rc: ResourceContainer = mockk {
            every { language }.returns(mockLanguage)
            every { project }.returns(mockProject)
            every { resource }.returns(mockResource)
            every { slug }.returns("fa_mrk_nmv")
        }

        return rc
    }
}