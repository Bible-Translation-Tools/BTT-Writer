package org.bibletranslationtools.writer.integration.usecases

import kotlinx.coroutines.test.runTest
import org.bibletranslationtools.writer.BaseIntegrationTest
import org.bibletranslationtools.writer.usecases.CloneRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.koin.test.inject
import java.io.File

class CloneRepositoryTest : BaseIntegrationTest() {

    private val cloneRepository: CloneRepository by inject()

    @Test
    fun cloneRepositorySuccessfully() = runTest {
        val cloneUrl = "https://wacs.bibletranslationtools.org/WycliffeAssociates/en_ulb.git"
        var progressMessage: String? = null
        val onProgress: (Float, String?) -> Unit = { _, message ->
            progressMessage = message
        }

        val result = cloneRepository.execute(cloneUrl, onProgress)

        assertNotNull("Clone repository result should not be null", result)
        assertNotNull("Progress message should not be null", progressMessage)
        assertEquals(CloneRepository.Status.SUCCESS, result.status)
        assertEquals(cloneUrl, result.cloneUrl)
        assertTrue(result.cloneDir!!.exists())

        val gitDir: File? = result.cloneDir.listFiles()?.find { it.name == ".git" }
        assertNotNull("Git directory should not be null", gitDir)

        val manifestFile: File? = result.cloneDir.listFiles()?.find { it.name == "manifest.yaml" }
        assertNotNull("Manifest file should not be null", manifestFile)
        assertTrue(manifestFile!!.length() > 0)
    }

    @Test
    fun cloneNonExistingRepositoryFailed() = runTest {
        val cloneUrl = "https://wacs.bibletranslationtools.org/WycliffeAssociates/non_existing_repo.git"

        var progressMessage: String? = null
        val onProgress: (Float, String?) -> Unit = { _, message ->
            progressMessage = message
        }

        val result = cloneRepository.execute(cloneUrl, onProgress)

        assertNotNull("Clone repository result should not be null", result)
        assertNotNull("Progress message should not be null", progressMessage)
        assertEquals(CloneRepository.Status.NO_REMOTE_REPO, result.status)
        assertEquals(cloneUrl, result.cloneUrl)
        assertNull(result.cloneDir)
    }
}
