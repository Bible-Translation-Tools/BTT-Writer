package org.bibletranslationtools.writer.integration.usecases

import io.mockk.every
import kotlinx.coroutines.runBlocking
import mockwebserver3.MockResponse
import org.bibletranslationtools.writer.BaseIntegrationTest
import org.bibletranslationtools.writer.data.Preference
import org.bibletranslationtools.writer.usecases.SearchGogsUsers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.koin.core.component.inject


class SearchGogsUsersTest : BaseIntegrationTest() {

    private val preference: Preference by inject()
    private val searchGogsUsers: SearchGogsUsers by inject()

    @Before
    fun setUp() {
        every {
            preference.getPref(Preference.KEY_PREF_GOGS_API, any(), String::class)
        } returns server.url("/search").toString()
    }

    @Test
    fun searchParticularUser() = runBlocking {
        val user = "test"
        var progressMessage: String? = null
        val onProgress: (Float, String?) -> Unit = { _, message ->
            progressMessage = message
        }

        val successResponse = """
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
            .body(successResponse)
            .addHeader("Content-Type", "application/json")
            .code(200)
            .build())

        val gogsUser = searchGogsUsers.execute(user, 1, onProgress).singleOrNull()

        assertNotNull("Gogs user should not be null", gogsUser)
        assertEquals("Ids should match", gogsUser?.id, 222)
        assertEquals("Usernames should match", gogsUser?.username, user)
        assertTrue("Progress message should not be empty", !progressMessage.isNullOrEmpty())
    }

    @Test
    fun searchMultipleUsersByQuery() = runBlocking {
        val successResponse = """
            {
                "data": [
                    {
                        "id": 222,
                        "full_name": "Test",
                        "email": "test@noreply.example.org",
                        "username": "test"
                    },
                    {
                        "id": 333,
                        "full_name": "Test 2",
                        "email": "test2@noreply.example.org",
                        "username": "test2"
                    }
                ],
                "ok": true
            }
        """.trimIndent()
        server.enqueue(MockResponse.Builder()
            .body(successResponse)
            .addHeader("Content-Type", "application/json")
            .code(200)
            .build())

        val userQuery = "test"
        val gogsUsers = searchGogsUsers.execute(userQuery, 3)

        assertTrue("Gogs users should not be empty", gogsUsers.isNotEmpty())

        val expectedUsers = gogsUsers.filter { it.username.contains(userQuery) }
        assertEquals("Number of users should match", expectedUsers.size, gogsUsers.size)
    }

    @Test
    fun searchNonExistentUsers() = runBlocking {
        val successResponse = """
            {
                "data": [],
                "ok": true
            }
        """.trimIndent()
        server.enqueue(MockResponse.Builder()
            .body(successResponse)
            .addHeader("Content-Type", "application/json")
            .code(200)
            .build())

        val userQuery = "non-existent-user"
        val gogsUsers = searchGogsUsers.execute(userQuery, 3)
        assertTrue("Gogs users should be empty", gogsUsers.isEmpty())
    }
}
