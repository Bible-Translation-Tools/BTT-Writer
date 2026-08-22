package org.bibletranslationtools.writer.integration.usecases

import io.mockk.every
import kotlinx.coroutines.runBlocking
import mockwebserver3.MockResponse
import org.bibletranslationtools.writer.BaseIntegrationTest
import org.bibletranslationtools.writer.data.Preference
import org.bibletranslationtools.writer.usecases.AdvancedGogsRepoSearch
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.koin.test.inject


class AdvancedGogsRepoSearchTest : BaseIntegrationTest() {

    private val advancedGogsRepoSearch: AdvancedGogsRepoSearch by inject()
    private val preference: Preference by inject()

    @Before
    fun setUp() {
        every {
            preference.getPref(Preference.KEY_PREF_GOGS_API, any(), String::class)
        } returns server.url("/search").toString()
    }

    @Test
    fun searchReposByUser() {
        val user = "test"

        server.enqueue(createUsersResponse())
        server.enqueue(createReposResponse())
        server.enqueue(createRepoResponse())

        var progressMessage: String? = null
        val onProgress: (Float, String?) -> Unit = { _, message ->
            progressMessage = message
        }
        val repos = runBlocking { advancedGogsRepoSearch.execute(user, "", 5, onProgress) }

        assertTrue(repos.isNotEmpty())
        assertFalse(progressMessage.isNullOrEmpty())
    }

    @Test
    fun searchReposByRepoName() {
        val repo = "_gen_"

        server.enqueue(createReposResponse())
        server.enqueue(createRepoResponse())

        val repos = runBlocking { advancedGogsRepoSearch.execute("", repo, 5) }
        assertTrue(repos.isNotEmpty())
    }

    @Test
    fun searchReposByUserAndRepoName() {
        val user = "mxaln"
        val repo = "_gen_"

        server.enqueue(createUsersResponse())
        server.enqueue(createReposResponse())
        server.enqueue(createRepoResponse())

        val repos = runBlocking { advancedGogsRepoSearch.execute(user, repo, 5) }
        assertTrue(repos.isNotEmpty())
    }

    @Test
    fun searchNonExistentUser() {
        val user = "non-existent-user"

        server.enqueue(createEmptyDataResponse())

        val repos = runBlocking { advancedGogsRepoSearch.execute(user, "", 5) }
        assertTrue(repos.isEmpty())
    }

    @Test
    fun searchNonExistentRepo() {
        val repo = "non-existent-repo"

        server.enqueue(createEmptyDataResponse())

        val repos = runBlocking { advancedGogsRepoSearch.execute("", repo, 5) }
        assertTrue(repos.isEmpty())
    }

    private fun createRepoResponse(): MockResponse {
        val body = """
            {
                "id": 111,
                "name": "fr_gen_text_reg",
                "html_url": "http://example.com/fr_gen_text_reg",
                "clone_url": "http://example.com/fr_gen_text_reg.git",
                "ssh_url": "ssh://example.com/fr_gen_text_reg.git",
                "isPrivate": false
            }
        """.trimIndent()

        return MockResponse.Builder()
            .body(body)
            .addHeader("Content-Type", "application/json")
            .code(200)
            .build()
    }

    private fun createReposResponse(): MockResponse {
        val body = """
            {
                "data": [
                    {
                        "id": 111,
                        "name": "fr_gen_text_reg",
                        "isPrivate": false
                    }
                ],
                "ok": true
            }
        """.trimIndent()

        return MockResponse.Builder()
            .body(body)
            .addHeader("Content-Type", "application/json")
            .code(200)
            .build()
    }

    private fun createUsersResponse(): MockResponse {
        val body = """
            {
                "data": [
                    {
                        "id": 111,
                        "full_name": "Test",
                        "email": "test@noreply.example.org",
                        "username": "test"
                    }
                ],
                "ok": true
            }
        """.trimIndent()

        return MockResponse.Builder()
            .body(body)
            .addHeader("Content-Type", "application/json")
            .code(200)
            .build()
    }

    private fun createEmptyDataResponse(): MockResponse {
        val body = """
            {
                "data": [],
                "ok": true
            }
        """.trimIndent()

        return MockResponse.Builder()
            .body(body)
            .addHeader("Content-Type", "application/json")
            .code(200)
            .build()
    }
}
