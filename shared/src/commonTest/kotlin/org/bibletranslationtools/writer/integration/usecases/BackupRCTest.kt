package org.bibletranslationtools.writer.integration.usecases

import kotlinx.coroutines.test.runTest
import org.bibletranslationtools.resourcecatalog.ResourceCatalogClient
import org.bibletranslationtools.resourcecatalog.library.models.Translation
import org.bibletranslationtools.writer.BaseIntegrationTest
import org.bibletranslationtools.writer.Platform
import org.bibletranslationtools.writer.TestUtils
import org.bibletranslationtools.writer.core.Translator.Companion.TSTUDIO_EXTENSION
import org.bibletranslationtools.writer.core.Translator.Companion.ZIP_EXTENSION
import org.bibletranslationtools.writer.usecases.BackupRC
import org.bibletranslationtools.writer.usecases.ImportProjects
import org.bibletranslationtools.writer.utils.FileUtilities
import org.bibletranslationtools.writer.utils.Zip
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.koin.test.inject
import java.io.File


class BackupRCTest : BaseIntegrationTest() {

    override val needsLibrary = true

    private val backupRC: BackupRC by inject()
    private val catalogClient: ResourceCatalogClient by inject()
    private val importProjects: ImportProjects by inject()
    private val platform: Platform by inject()
    private val profile: org.bibletranslationtools.writer.core.Profile by inject()

    private var tempDir: File? = null

    @After
    fun tearDown() {
        FileUtilities.deleteQuietly(tempDir)
        deleteBackups()
        FileUtilities.deleteQuietly(directoryProvider.translationsDir)
    }

    @Test
    fun testBackupResourceContainer() = runTest {
        val source = "source/fa_jud_nmv.zip"
        val rcTranslation = importSourceTranslation(source)

        assertNotNull(rcTranslation)

        val rcFile = backupRC.backupResourceContainer(rcTranslation!!)

        assertNotNull("RC file should not be null", rcFile)
        assertTrue("RC file should exist", rcFile.exists())

        val id = "fa_jud_nmv"
        val backupFiles = directoryProvider.backupsDir.listFiles()

        assertNotNull("Backups dir should not be null", backupFiles)
        assertTrue("Backups dir should not be empty", backupFiles!!.isNotEmpty())

        val backupFile = backupFiles.firstOrNull {
            it.name.startsWith(id) && it.name.endsWith(".tsrc")
        }
        assertNotNull("Backup with id exists", backupFile)
    }

    @Test
    fun testBackupTargetTranslation() = runTest {
        val source = "usfm/mrk.usfm"
        val targetTranslation = TestUtils.importTargetTranslation(
            catalogClient,
            platform,
            directoryProvider,
            profile,
            importProjects,
            "aae",
            source
        )

        assertNotNull("Target translation should not be null", targetTranslation)

        val backedUp = backupRC.backupTargetTranslation(targetTranslation, false)
        assertTrue("Backup should succeed", backedUp)

        val id = targetTranslation!!.id
        val backupFiles = directoryProvider.backupsDir.listFiles()

        assertNotNull("Backups dir should not be null", backupFiles)
        assertTrue("Backups dir should not be empty", backupFiles!!.isNotEmpty())

        val backupFile = backupFiles.firstOrNull {
            it.name.startsWith(id) && it.name.endsWith(TSTUDIO_EXTENSION)
        }
        assertNotNull("Backup with id exists", backupFile)
    }

    @Test
    fun testBackupTargetTranslationOrphan() = runTest {
        val source = "usfm/mrk.usfm"
        val targetTranslation = TestUtils.importTargetTranslation(
            catalogClient,
            platform,
            directoryProvider,
            profile,
            importProjects,
            "aae",
            source
        )

        assertNotNull("Target translation should not be null", targetTranslation)

        val backedUp = backupRC.backupTargetTranslation(targetTranslation, true)
        assertTrue("Backup should succeed", backedUp)

        val id = targetTranslation!!.id
        val backupFiles = directoryProvider.backupsDir.listFiles()

        assertNotNull("Backups dir should not be null", backupFiles)
        assertTrue("Backups dir should not be empty", backupFiles.isNotEmpty())

        val backupFile = backupFiles.firstOrNull {
            it.name.startsWith(id) && it.name.endsWith(ZIP_EXTENSION)
        }
        assertNotNull("Backup with id exists", backupFile)
    }

    @Test
    fun testBackupTargetTranslationDir() = runTest {
        val source = "usfm/19-PSA.usfm"
        val targetTranslation = TestUtils.importTargetTranslation(
            catalogClient,
            platform,
            directoryProvider,
            profile,
            importProjects,
            "aae",
            source
        )

        assertNotNull("Target translation should not be null", targetTranslation)

        val backedUp = backupRC.backupTargetTranslation(targetTranslation!!.path)

        assertTrue("Backup should succeed", backedUp)
    }

    private suspend fun importSourceTranslation(path: String): Translation? {
        return TestUtils.getResourceStream(path).use {
            try {
                tempDir = directoryProvider.createTempDir("tempRc")

                assertNotNull("tempDir should not be null", tempDir)
                assertTrue("tempDir should exist", tempDir!!.exists())

                Zip.unzipFromStream(it, tempDir!!)

                assertFalse("tempDir should not be empty", tempDir!!.listFiles().isNullOrEmpty())

                val rc = catalogClient.importResourceContainer(tempDir!!)

                assertNotNull("rc should not be null", rc)

                catalogClient.library.getTranslation(rc.slug)
            } catch (_: Exception) {
                null
            }
        }
    }

    private fun deleteBackups() {
        val backupFiles = directoryProvider.backupsDir.listFiles()
        if (backupFiles != null) {
            for (file in backupFiles) {
                FileUtilities.safeDelete(file)
            }
        }
    }
}
