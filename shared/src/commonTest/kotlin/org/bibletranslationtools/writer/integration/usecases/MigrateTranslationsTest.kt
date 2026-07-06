package org.bibletranslationtools.writer.integration.usecases

import btt_writer.shared.generated.resources.Res
import btt_writer.shared.generated.resources.copying_file
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.bibletranslationtools.writer.BaseIntegrationTest
import org.bibletranslationtools.writer.TestUtils
import org.bibletranslationtools.writer.core.TargetTranslationMigrator
import org.bibletranslationtools.writer.usecases.ImportProjects
import org.bibletranslationtools.writer.usecases.MigrateTranslations
import org.bibletranslationtools.writer.utils.FileUtilities
import org.bibletranslationtools.writer.utils.Zip
import org.jetbrains.compose.resources.getString
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.koin.core.component.inject
import java.io.File


class MigrateTranslationsTest : BaseIntegrationTest() {

    private val importProjects: ImportProjects by inject()
    private val targetTranslationMigrator: TargetTranslationMigrator by inject()

    override val needsLibrary = true

    @After
    fun tearDown() {
        runBlocking { directoryProvider.clearCache() }
    }

    @Test
    fun migrateOldAppDataEmpty() = runTest {
        var progressMessage: String? = null
        val onProgress: (Float, String?) -> Unit = { _, message ->
            progressMessage = message
        }

        val sourceDir = PlatformFile(directoryProvider.createTempDir("BTTWriter"))

        MigrateTranslations(
            importProjects,
            directoryProvider,
            targetTranslationMigrator
        )
            .execute(sourceDir, onProgress)

        assertEquals("Completed!", progressMessage)
        assertEquals(0, directoryProvider.translationsDir.listFiles()?.size ?: 0)
    }

    @Test
    fun migrateOldAppDataWithTranslations() = runTest {
        var progressMessage: String? = null
        val onProgress: (Float, String?) -> Unit = { _, message ->
            progressMessage = message
        }

        val bttWriterDir = directoryProvider.createTempDir("BTTWriter")
        val sourceDir = PlatformFile(bttWriterDir)

        TestUtils.getResourceStream("exports/aa_jud_text_reg.tstudio").use { stream ->
            val tempDir = directoryProvider.createTempDir("temp")
            Zip.unzipFromStream(stream, tempDir)

            val translationDir = tempDir.listFiles()!!.first { it.isDirectory }
            FileUtilities.copyDirectory(translationDir, File(bttWriterDir, "translations/aa_jud_text_reg"), null)
        }

        MigrateTranslations(importProjects, directoryProvider, targetTranslationMigrator)
            .execute(sourceDir, onProgress)

        assertEquals("Completed!", progressMessage)
        assertEquals(1, directoryProvider.translationsDir.listFiles()?.size ?: 0)
    }

    @Test
    fun migrateOldAppDataWithBackups() = runTest {
        var progressMessage: String? = null
        val onProgress: (Float, String?) -> Unit = { _, message ->
            progressMessage = message
        }

        val bttWriterDir = directoryProvider.createTempDir("BTTWriter")
        val sourceDir = PlatformFile(bttWriterDir)

        TestUtils.getResourceStream("exports/aa_jud_text_reg.tstudio").use { stream ->
            val tempFile = File(bttWriterDir, "backups/aa_jud_text_reg.tstudio")
            FileUtilities.copyInputStreamToFile(stream, tempFile)
        }

        MigrateTranslations(importProjects, directoryProvider, targetTranslationMigrator)
            .execute(sourceDir, onProgress)

        val expectedMessage = getString(Res.string.copying_file, "aa_jud_text_reg.tstudio")

        assertEquals(expectedMessage, progressMessage)
        assertEquals(1, directoryProvider.backupsDir.listFiles()?.size ?: 0)
    }
}
