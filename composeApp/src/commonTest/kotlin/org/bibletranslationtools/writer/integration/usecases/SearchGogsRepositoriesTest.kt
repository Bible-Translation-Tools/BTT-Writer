package org.bibletranslationtools.writer.integration.usecases

import io.mockk.every
import kotlinx.coroutines.runBlocking
import mockwebserver3.MockResponse
import org.bibletranslationtools.writer.BaseIntegrationTest
import org.bibletranslationtools.writer.data.Preference
import org.bibletranslationtools.writer.usecases.SearchGogsRepositories
import org.bibletranslationtools.writer.usecases.SearchGogsUsers
import org.bibletranslationtools.writer.utils.JsonLenient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.koin.core.component.inject


class SearchGogsRepositoriesTest : BaseIntegrationTest() {

    private val preference: Preference by inject()
    private val searchGogsRepositories: SearchGogsRepositories by inject()
    private val searchGogsUsers: SearchGogsUsers by inject()

    @Before
    fun setUp() {
        every {
            preference.getPref(Preference.KEY_PREF_GOGS_API, any(), String::class)
        } returns server.url("/search").toString()
    }

    @Test
    fun searchReposByUser() = runBlocking {
        val userResponse = """
            {
                "data": [
                    {
                        "id": 222,
                        "full_name": "Test",
                        "email": "test@noreply.example.org",
                        "username": "test"
                    }
                ],
                "ok": true
            }
        """.trimIndent()
        server.enqueue(MockResponse.Builder()
            .body(userResponse)
            .addHeader("Content-Type", "application/json")
            .code(200)
            .build())

        val user = "test"
        val gogsUser = searchGogsUsers.execute(user, 1).singleOrNull()

        assertNotNull("Gogs user should not be null", gogsUser)
        assertEquals("Gogs user id should match", gogsUser?.id, 222)
        assertEquals("Gogs username should match", gogsUser?.username, user)

        val owner = gogsUser?.let { JsonLenient.encodeToString(it) }?.let { "\"owner\": $it" } ?: ""
        val repoResponse = """
            {
                "id": 222,
                "name": "test_repo",
                "html_url": "http://example.com/test_repo",
                "clone_url": "http://example.com/test_repo.git",
                "ssh_url": "ssh://example.com/test_repo.git",
                "private": false,
                $owner
            }
        """.trimIndent()

        val reposResponse = """
            {
                "data": [$repoResponse],
                "ok": true
            }
        """.trimIndent()

        server.enqueue(MockResponse.Builder()
            .body(reposResponse)
            .addHeader("Content-Type", "application/json")
            .code(200)
            .build())
        server.enqueue(MockResponse.Builder()
            .body(repoResponse)
            .addHeader("Content-Type", "application/json")
            .code(200)
            .build())

        var progressMessage: String? = null
        val onProgress: (Float, String?) -> Unit = { _, message ->
            progressMessage = message
        }
        val repos = searchGogsRepositories.execute(gogsUser!!.id, "", 3, onProgress)

        assertTrue("Repos should not be empty", repos.isNotEmpty())
        assertFalse("Progress message should not be empty", progressMessage.isNullOrEmpty())

        val repo = repos.first()
        assertEquals("Repo name should match","test_repo", repo.name)
        assertEquals("Repo htmlUrl should match","http://example.com/test_repo", repo.htmlUrl)
        assertEquals("Repo cloneUrl should match","http://example.com/test_repo.git", repo.cloneUrl)
        assertEquals("Repo sshUrl should match","ssh://example.com/test_repo.git", repo.sshUrl)
        assertFalse("Repo should not be private", repo.isPrivate)
        assertEquals("Repo owner should match", gogsUser.username, repo.owner?.username)
    }

    @Test
    fun searchReposByRepoName() = runBlocking {
        val repo1Response = """
            {
                "id": 111,
                "name": "aa_gen_text_reg",
                "html_url": "http://example.com/aa_gen_text_reg",
                "clone_url": "http://example.com/aa_gen_text_reg.git",
                "ssh_url": "ssh://example.com/aa_gen_text_reg.git",
                "isPrivate": false
            }
        """.trimIndent()

        val repo2Response = """
            {
                "id": 222,
                "name": "fr_gen_text_reg",
                "html_url": "http://example.com/fr_gen_text_reg",
                "clone_url": "http://example.com/fr_gen_text_reg.git",
                "ssh_url": "ssh://example.com/fr_gen_text_reg.git",
                "isPrivate": false
            }
        """.trimIndent()

        val reposResponse = """
            {
                "data": [
                    $repo1Response,
                    $repo2Response
                ],
                "ok": true
            }
        """.trimIndent()

        // There should be 3 request done
        // First request is to fetch repos by query
        // Second and third requests are to fetch additional data for found repos in the first request
        server.enqueue(MockResponse.Builder()
            .body(reposResponse)
            .addHeader("Content-Type", "application/json")
            .code(200)
            .build())
        server.enqueue(MockResponse.Builder()
            .body(repo1Response)
            .addHeader("Content-Type", "application/json")
            .code(200)
            .build())
        server.enqueue(MockResponse.Builder()
            .body(repo2Response)
            .addHeader("Content-Type", "application/json")
            .code(200)
            .build())

        val query = "_gen_"
        val repos = searchGogsRepositories.execute(0, query, 3)
        assertTrue("Repos should not be empty", repos.isNotEmpty())

        assertEquals("Repos size should match", 2, repos.size)
        val genRepos = repos.filter { it.name.contains(query) }
        assertEquals("Gen repos size should match", 2, genRepos.size)
    }

    @Test
    fun searchNonExistentRepo() = runBlocking {
        val reposResponse = """
            {
                "data": [],
                "ok": true
            }
        """.trimIndent()
        server.enqueue(MockResponse.Builder()
            .body(reposResponse)
            .addHeader("Content-Type", "application/json")
            .code(200)
            .build())

        val query = "non-existent-repo"
        val repos = searchGogsRepositories.execute(0, query, 3)
        assertTrue("Repos should be empty", repos.isEmpty())
    }
}
