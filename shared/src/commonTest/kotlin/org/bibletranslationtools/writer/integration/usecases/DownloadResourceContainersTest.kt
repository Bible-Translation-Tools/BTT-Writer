package org.bibletranslationtools.writer.integration.usecases

import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.bibletranslationtools.resourcecatalog.ResourceCatalogClient
import org.bibletranslationtools.writer.BaseIntegrationTest
import org.bibletranslationtools.writer.usecases.DownloadResourceContainers
import org.junit.After
import org.junit.Test
import org.koin.core.component.inject


class DownloadResourceContainersTest : BaseIntegrationTest() {

    private val downloadResourceContainers: DownloadResourceContainers by inject()
    private val catalogClient: ResourceCatalogClient by inject()

    override val needsLibrary = true

    @After
    fun tearDown() {
        runBlocking { directoryProvider.clearCache() }
    }

    @Test
    fun downloadResourceContainerSucceeded() = runTest {
        val translation = catalogClient.library.getTranslation("en_gen_ulb")
        assertNotNull("Translation should not be null", translation)

        var progressMessage: String? = null
        val onProgress: (Float, String?) -> Unit = { _, message ->
            progressMessage = message
        }

        val result = downloadResourceContainers.download(translation!!, onProgress)

        assertNotNull("Download result should not be null", result)
        assertTrue("Download result should be successful", result.success)
        assertTrue(
            "Download result should have at least one container",
            result.containers.isNotEmpty()
        )
        assertNotNull("Progress message should not be null", progressMessage)
    }

    @Test
    fun downloadResourceContainersSucceeded() = runTest {
        val translationIds = listOf("en_gen_ulb", "id_gen_ayt")

        var progressMessage: String? = null
        val onProgress: (Float, String?) -> Unit = { _, message ->
            progressMessage = message
        }

        val result = downloadResourceContainers.download(translationIds, onProgress)

        assertNotNull("Download result should not be null", result)

        assertTrue(
            "id_gen_ayt should be downloaded",
            result.downloadedTranslations.contains("id_gen_ayt")
        )
        assertTrue(
            "id_gen_ayt should be downloaded",
            result.downloadedContainers.map { it.slug }.contains("id_gen_ayt")
        )
        assertTrue(
            "id_gen_tn should be downloaded",
            result.downloadedContainers.map { it.slug }.contains("id_gen_tn")
        )
        assertTrue(
            "id_gen_tq should be downloaded",
            result.downloadedContainers.map { it.slug }.contains("id_gen_tq")
        )
        assertTrue(
            "id_bible_tw should be downloaded",
            result.downloadedContainers.map { it.slug }.contains("id_bible_tw")
        )
        assertTrue(
            "en_gen_ulb should be in downloaded translations list",
            result.downloadedTranslations.contains("en_gen_ulb")
        )
        assertTrue(
            "en_gen_ulb should be downloaded",
            result.downloadedContainers.map { it.slug }.contains("en_gen_ulb")
        )
        assertTrue(
            "en_gen_tn should be downloaded",
            result.downloadedContainers.map { it.slug }.contains("en_gen_tn")
        )
        assertTrue(
            "en_bible_tw should be downloaded",
            result.downloadedContainers.map { it.slug }.contains("en_bible_tw")
        )
        assertFalse(
            "en_gen_tq should not be downloaded",
            result.downloadedContainers.map { it.slug }.contains("en_gen_tq")
        )

        assertNotNull("Progress message should not be null", progressMessage)
    }

    @Test
    fun downloadNoneResourceContainers() = runTest {
        val result = downloadResourceContainers.download(listOf())

        assertNotNull("Download result should not be null", result)
        assertEquals(result.downloadedTranslations.size, 0)
        assertEquals(result.downloadedContainers.size, 0)
        assertEquals(result.failedSourceDownloads.size, 0)
        assertEquals(result.failedHelpsDownloads.size, 0)
        assertEquals(result.failureMessages.size, 0)
    }

    @Test
    fun downloadIncorrectResourceContainers() {
        val badTranslationIds = listOf("bad_tr_id1", "bad_tr_id2")
        val result = runBlocking { downloadResourceContainers.download(badTranslationIds) }

        assertNotNull("Download result should not be null", result)

        assertEquals(
            "Failed downloads should be 2",
            result.failedSourceDownloads.size, badTranslationIds.size
        )
        assertEquals(result.downloadedTranslations.size, 0)
        assertEquals(result.downloadedContainers.size, 0)
        assertEquals(result.failedHelpsDownloads.size, 0)
        assertEquals(result.failureMessages.size, badTranslationIds.size)

        assertTrue(
            "bad_tr_id1 should be in failure messages",
            result.failureMessages.containsKey("bad_tr_id1")
        )
        assertTrue(
            "bad_tr_id2 should be in failure messages",
            result.failureMessages.containsKey("bad_tr_id2")
        )
        assertTrue(
            "bad_tr_id1 and bad_tr_id2 should have failure messages",
            result.failureMessages.values.filter { it.isNotEmpty() }.size == 2
        )
        assertTrue(
            "bad_tr_id1 should be in failed sources list",
            result.failedSourceDownloads.contains("bad_tr_id1")
        )
        assertTrue(
            "bad_tr_id2 should be in failed sources list",
            result.failedSourceDownloads.contains("bad_tr_id2")
        )
    }
}
