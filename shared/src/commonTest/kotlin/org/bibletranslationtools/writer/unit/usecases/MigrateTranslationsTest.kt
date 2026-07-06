package org.bibletranslationtools.writer.unit.usecases

import io.github.vinceglb.filekit.PlatformFile
import java.io.File
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.bibletranslationtools.writer.DirectoryProvider
import org.bibletranslationtools.writer.core.TargetTranslationMigrator
import org.bibletranslationtools.writer.usecases.ImportProjects
import org.bibletranslationtools.writer.usecases.MigrateTranslations
import org.bibletranslationtools.writer.utils.FileUtilities
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class MigrateTranslationsTest {

    @MockK private lateinit var directoryProvider: DirectoryProvider
    @MockK private lateinit var importProjects: ImportProjects
    @MockK private lateinit var targetTranslationMigrator: TargetTranslationMigrator

    val onProgress: (Float, String?) -> Unit = {_,_->}

    @JvmField
    @Rule
    var tempDir: TemporaryFolder = TemporaryFolder()

    @Before
    fun setup() {
        MockKAnnotations.init(this)

        mockkObject(FileUtilities)
    }

    @After
    fun tearDown() {
        unmockkAll()
        tempDir.delete()
    }

    @Test
    fun `test migration is done`() = runTest {
        coEvery { directoryProvider.createTempDir(any()) }.returns(tempDir.newFolder())

        val translationsDir = tempDir.newFolder("translations")
        val backupsDir = tempDir.newFolder("backups")
        every { directoryProvider.translationsDir }.returns(translationsDir)
        every { directoryProvider.backupsDir }.returns(backupsDir)

        coJustRun { FileUtilities.copyDirectory(any(), any()) }
        every { FileUtilities.deleteQuietly(any()) }.returns(true)

        coEvery { importProjects.importProjects(any(), any(), any()) }.returns(mockk())

        val sourceFolderFile = tempDir.newFolder("source")
        File(sourceFolderFile, "translations").mkdir()
        File(sourceFolderFile, "backups").mkdir()
        val sourceFolder = PlatformFile(sourceFolderFile)

        MigrateTranslations(importProjects, directoryProvider, targetTranslationMigrator)
            .execute(sourceFolder, onProgress)

        coVerify(exactly = 2) { directoryProvider.createTempDir(any()) }
        coVerify(exactly = 2) { FileUtilities.copyDirectory(any(), any()) }
        verify(exactly = 2) { FileUtilities.deleteQuietly(any()) }

        coVerify { importProjects.importProjects(any(), any(), any()) }
    }
}