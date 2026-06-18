package org.bibletranslationtools.writer.unit.usecases

import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.runs
import io.mockk.unmockkAll
import io.mockk.verify
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.test.runTest
import org.bibletranslationtools.resourcecatalog.ResourceCatalogClient
import org.bibletranslationtools.resourcecatalog.library.models.Translation
import org.bibletranslationtools.resourcecontainer.Language
import org.bibletranslationtools.resourcecontainer.Project
import org.bibletranslationtools.resourcecontainer.Resource
import org.bibletranslationtools.writer.DirectoryProvider
import org.bibletranslationtools.writer.core.ArchiveMigrator
import org.bibletranslationtools.writer.core.Profile
import org.bibletranslationtools.writer.core.TargetTranslation
import org.bibletranslationtools.writer.data.Preference
import org.bibletranslationtools.writer.usecases.BackupRC
import org.bibletranslationtools.writer.usecases.ExportProjects
import org.bibletranslationtools.writer.utils.FileUtilities
import org.bibletranslationtools.writer.utils.Zip
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import java.io.File
import kotlin.io.path.createTempDirectory

class BackupRCTest {

    @MockK private lateinit var directoryProvider: DirectoryProvider
    @MockK private lateinit var migrator: ArchiveMigrator
    @MockK private lateinit var exportProjects: ExportProjects
    @MockK private lateinit var profile: Profile
    @MockK private lateinit var catalogClient: ResourceCatalogClient
    @MockK private lateinit var translation: Translation
    @MockK private lateinit var language: Language
    @MockK private lateinit var project: Project
    @MockK private lateinit var resource: Resource
    @MockK private lateinit var targetTranslation: TargetTranslation
    @MockK(relaxed = true) private lateinit var preference: Preference

    private lateinit var backupRC: BackupRC
    private lateinit var backupsDir: File

    @Before
    fun setup() {
        MockKAnnotations.init(this)

        backupRC = BackupRC(
            directoryProvider,
            migrator,
            exportProjects,
            profile,
            catalogClient,
            preference
        )

        mockkObject(FileUtilities)
        mockkObject(Zip)

        every { FileUtilities.deleteQuietly(any()) }.returns(true)
        every { FileUtilities.copyFile(any(), any()) } just runs

        backupsDir = createTempDirectory(prefix = "backup-rc-test").toFile()
        every { directoryProvider.backupsDir }.returns(backupsDir)
        every { profile.nativeSpeaker }.returns(mockk())

        every { translation.language } returns language
        every { translation.project } returns project
        every { translation.resource } returns resource
    }

    @After
    fun tearDown() {
        unmockkAll()
        backupsDir.deleteRecursively()
    }

    @Test
    fun `test backupResourceContainer with valid translation`() {
        every { language.slug } returns("fa")
        every { project.slug } returns("mrk")
        every { resource.slug } returns("nmv")

        every { translation.resourceContainerSlug } returns "fa_mrk_nmv"

        every {
            catalogClient.exportResourceContainer(
                any(),
                "fa",
                "mrk",
                "nmv"
            )
        } just runs

        val backupFile = backupRC.backupResourceContainer(translation)

        assertEquals(File(backupsDir, "fa_mrk_nmv.tsrc").path, backupFile.path)

        verify {
            catalogClient.exportResourceContainer(
                any(),
                "fa",
                "mrk",
                "nmv"
            )
        }
    }

    @Test
    fun `test backupResourceContainer throws exception`() {
        every { language.slug } returns("fa")
        every { project.slug } returns("mrk")
        every { resource.slug } returns("nmv")

        every { translation.resourceContainerSlug } returns "fa_mrk_nmv"

        every {
            catalogClient.exportResourceContainer(
                any(),
                "fa",
                "mrk",
                "nmv"
            )
        }.throws(Exception("backup failed"))

        assertThrows("backup failed", Exception::class.java) {
            backupRC.backupResourceContainer(translation)
        }

        verify {
            catalogClient.exportResourceContainer(
                any(),
                "fa",
                "mrk",
                "nmv"
            )
        }
    }

    @Test
    fun `test backupTargetTranslation with valid targetTranslation`() = runTest {
        val tempFile: File = mockk()
        every { tempFile.exists() }.returns(true)
        every { tempFile.isFile }.returns(true)

        // plant existing backup so commitHash + migrateManifest path runs
        val existingBackup = File(backupsDir, "aa_mrk_text_reg.tstudio")
        existingBackup.writeText("dummy")

        val migratedManifest = """
            {
              "package_version": 2,
              "timestamp": 0,
              "generator": {"name": "test", "build": "1"},
              "target_translations": [
                {"id": "aa_mrk_text_reg", "path": ".", "direction": "ltr", "commit_hash": "older"}
              ]
            }
        """.trimIndent()

        every { Zip.read(existingBackup, ArchiveMigrator.MANIFEST_JSON) } returns "{}"
        coEvery { migrator.migrateManifest("{}") } returns migratedManifest

        every { targetTranslation.id }.returns("aa_mrk_text_reg")
        every { targetTranslation.commitHash }.returns("abcdefghijklmnopqrstuvwxyz")
        coEvery {
            directoryProvider.createTempFile(
                "aa_mrk_text_reg",
                ".tstudio",
                null
            )
        }.returns(tempFile)
        every { targetTranslation.setDefaultContributor(any()) } just runs
        coEvery { exportProjects.exportProject(targetTranslation, tempFile) }.returns(mockk())

        val success = backupRC.backupTargetTranslation(targetTranslation, false)

        assertTrue(success)

        verify { tempFile.exists() }
        verify { tempFile.isFile }
        verify { targetTranslation.id }
        verify { targetTranslation.commitHash }
        verify { Zip.read(existingBackup, ArchiveMigrator.MANIFEST_JSON) }
        coVerify { migrator.migrateManifest("{}") }
        coVerify {
            directoryProvider.createTempFile(
                "aa_mrk_text_reg",
                ".tstudio",
                null
            )
        }
        verify { targetTranslation.setDefaultContributor(any()) }
        coVerify { exportProjects.exportProject(targetTranslation, tempFile) }
    }

    @Test
    fun `test backupTargetTranslation orphaned`() = runTest {
        val tempFile: File = mockk()
        every { tempFile.exists() }.returns(true)
        every { tempFile.isFile }.returns(true)

        every { targetTranslation.id }.returns("aa_mrk_text_reg")
        every { targetTranslation.commitHash }.returns("abcdefghijklmnopqrstuvwxyz")
        coEvery { directoryProvider.createTempFile(any(), any(), null) }.returns(tempFile)
        every { targetTranslation.setDefaultContributor(any()) } just runs
        coEvery { exportProjects.exportProject(targetTranslation, tempFile) }.returns(mockk())

        val success = backupRC.backupTargetTranslation(targetTranslation, true)

        assertTrue(success)

        verify { tempFile.exists() }
        verify { tempFile.isFile }
        verify { targetTranslation.id }
        verify(exactly = 0) { targetTranslation.commitHash }
        coVerify { directoryProvider.createTempFile(any(), any(), null) }
        verify { targetTranslation.setDefaultContributor(any()) }
        coVerify { exportProjects.exportProject(targetTranslation, tempFile) }
    }

    @Test
    fun `test backupTargetTranslation from a directory`() = runTest {
        val projectDir: File = mockk()
        every { projectDir.name }.returns("aa_mrk_text_reg")
        val tempFile: File = mockk()
        every { tempFile.exists() }.returns(true)
        every { tempFile.isFile }.returns(true)

        coEvery { directoryProvider.createTempFile(any(), any(), null) }.returns(tempFile)
        every { exportProjects.exportProject(projectDir, tempFile) } just runs

        val success = backupRC.backupTargetTranslation(projectDir)

        assertTrue(success)

        verify { projectDir.name }
        verify { tempFile.exists() }
        verify { tempFile.isFile }
        coVerify { directoryProvider.createTempFile(any(), any(), null) }
        verify { exportProjects.exportProject(projectDir, tempFile) }
        verify { FileUtilities.deleteQuietly(any()) }
    }

    @Test
    fun `test backupTargetTranslation with no targetTranslation fails`() = runTest {
        val success = backupRC.backupTargetTranslation(null, false)

        assertFalse(success)
    }
}