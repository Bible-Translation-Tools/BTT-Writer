package org.bibletranslationtools.writer.integration.usecases

import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.bibletranslationtools.resourcecatalog.ResourceCatalogClient
import org.bibletranslationtools.resourcecatalog.library.models.Translation
import org.bibletranslationtools.writer.DirectoryProvider
import org.bibletranslationtools.writer.Platform
import org.bibletranslationtools.writer.TestUtils
import org.bibletranslationtools.writer.core.Profile
import org.bibletranslationtools.writer.core.Translator
import org.bibletranslationtools.writer.core.Translator.Companion.TSTUDIO_EXTENSION
import org.bibletranslationtools.writer.core.Translator.Companion.ZIP_EXTENSION
import org.bibletranslationtools.writer.data.Preference
import org.bibletranslationtools.writer.di.platformModule
import org.bibletranslationtools.writer.di.sharedModule
import org.bibletranslationtools.writer.usecases.BackupRC
import org.bibletranslationtools.writer.usecases.ImportProjects
import org.bibletranslationtools.writer.utils.FileUtilities
import org.bibletranslationtools.writer.utils.Zip
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.koin.core.component.inject
import org.koin.core.context.GlobalContext.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import java.io.File


class BackupRCTest : KoinTest {

    private val backupRC: BackupRC by inject()
    private val directoryProvider: DirectoryProvider by inject()
    private val catalogClient: ResourceCatalogClient by inject()
    private val importProjects: ImportProjects by inject()
    private val profile: Profile by inject()
    private val translator: Translator by inject()
    private val platform: Platform by inject()

    private var tempDir: File? = null

    @Before
    fun setup() {
        startKoin {
            modules(
                sharedModule,
                platformModule,
                module { single<Preference> { mockk(relaxed = true) } },
                module { single<Profile> { mockk(relaxed = true) } }
            )
        }
    }

    @After
    fun tearDown() {
        FileUtilities.deleteQuietly(tempDir)
        deleteBackups()
        runBlocking { directoryProvider.clearCache() }
        FileUtilities.deleteQuietly(directoryProvider.translationsDir)
        stopKoin()
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
            translator,
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
            translator,
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
            translator,
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
            } catch (e: Exception) {
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
