package org.bibletranslationtools.writer.integration.usecases

import io.github.vinceglb.filekit.PlatformFile
import io.mockk.every
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.bibletranslationtools.resourcecatalog.ResourceCatalogClient
import org.bibletranslationtools.writer.BaseIntegrationTest
import org.bibletranslationtools.writer.data.Preference
import org.bibletranslationtools.writer.usecases.ImportIndex
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Test
import org.koin.test.inject


class DownloadIndexTest : BaseIntegrationTest() {

    override val needsLibrary = true

    private val downloadIndex: ImportIndex by inject()
    private val catalogClient: ResourceCatalogClient by inject()
    private val preference: Preference by inject()

    @Before
    fun setUp() {
        every {
            preference.getPref(Preference.KEY_PREF_INDEX_SQLITE_URL, any(), String::class)
        } returns "https://writer-resources.bibletranslationtools.org/index.sqlite"
        runBlocking { directoryProvider.clearCache() }
    }

    @Test
    fun downloadIndexSucceeds() = runTest {
        var progressMessage: String? = null
        val onProgress: (Float, String?) -> Unit = { _, message ->
            progressMessage = message
        }

        val languagesBefore = catalogClient.library.getTargetLanguages()
        assertTrue("Languages before should not be empty", languagesBefore.isNotEmpty())

        val downloaded = downloadIndex.download(onProgress)

        assertTrue("Download result should be true", downloaded)
        assertNotNull("Progress message should not be null", progressMessage)

        val newLibrary = ResourceCatalogClient(
            directoryProvider.databaseFile,
            directoryProvider.containersDir
        )
        val languagesAfter = newLibrary.library.getTargetLanguages()

        assertTrue("Languages after should not be empty", languagesAfter.isNotEmpty())
        assertNotEquals(
            "Target languages should have changed",
            languagesBefore.size, languagesAfter.size
        )
    }

    @Test
    fun importIndexSucceeds() = runTest {
        val languagesBefore = catalogClient.library.getTargetLanguages()
        assertTrue("Languages before should not be empty", languagesBefore.isNotEmpty())

        val indexFile = directoryProvider.createTempFile("index", ".sqlite")
        directoryProvider.databaseFile.inputStream().use { input ->
            indexFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        val imported = downloadIndex.import(PlatformFile(indexFile))

        assertTrue("Import result should be true", imported)

        val newLibrary = ResourceCatalogClient(
            directoryProvider.databaseFile,
            directoryProvider.containersDir
        )
        val languagesAfter = newLibrary.library.getTargetLanguages()
        assertTrue("Languages after should not be empty", languagesAfter.isNotEmpty())
    }
}
