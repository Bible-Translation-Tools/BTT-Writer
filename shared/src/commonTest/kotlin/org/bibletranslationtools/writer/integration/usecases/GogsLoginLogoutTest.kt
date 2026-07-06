package org.bibletranslationtools.writer.integration.usecases

import io.mockk.every
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.runBlocking
import mockwebserver3.MockResponse
import org.bibletranslationtools.gogsclient.User
import org.bibletranslationtools.writer.BaseIntegrationTest
import org.bibletranslationtools.writer.Platform
import org.bibletranslationtools.writer.TestUtils
import org.bibletranslationtools.writer.core.Profile
import org.bibletranslationtools.writer.data.Preference
import org.bibletranslationtools.writer.usecases.GogsLogin
import org.bibletranslationtools.writer.usecases.GogsLogout
import org.junit.Before
import org.junit.Test
import org.koin.core.component.inject


class GogsLoginLogoutTest : BaseIntegrationTest() {

    private val gogsLogin: GogsLogin by inject()
    private val gogsLogout: GogsLogout by inject()
    private val profile: Profile by inject()
    private val preference: Preference by inject()
    private val platform: Platform by inject()

    private val username = "test"
    @Before
    fun setUp() {
        every {
            preference.getPref(Preference.KEY_PREF_GOGS_API, any(), String::class)
        } returns server.url("/api/").toString()
    }

    @Test
    fun testGogsLogin() {
        val user = runBlocking { loginUserWithPassword("Test User") }
        assertEquals("Test User", user.fullName)
    }

    @Test
    fun testGogsLoginWithoutFullName() {
        val user = runBlocking { loginUserWithPassword() }
        assertEquals("", user.fullName)
    }

    @Test
    fun testGogsLoginWithWrongCredentials() {
        val result = runBlocking { gogsLogin.execute("btt-test", "incorrect_password") }

        assertNull("User should be null", result.user)
    }

    @Test
    fun testGogsLogout() {
        runBlocking {
            val userBefore = loginUserWithPassword()
            profile.gogsUser = userBefore

            server.enqueue(MockResponse.Builder().code(204).build()) // delete token response

            gogsLogout.execute()

            val userAfter = loginUserWithPassword()

            println(userBefore.token)
            println(userAfter.token)

            assertFalse(
                "Token should be updated after logout",
                userBefore.token == userAfter.token
            )
            assertEquals("User should be the same", userBefore.username, userAfter.username)
        }
    }

    private suspend fun loginUserWithPassword(fullName: String? = null): User {
        server.enqueue(createLoginResponse(fullName))
        server.enqueue(createGetTokenResponse())
        server.enqueue(MockResponse.Builder().code(204).build()) // Delete token response
        server.enqueue(createTokenResponse())

        val result = gogsLogin.execute("username", "password", fullName)

        assertNotNull("User should not be null", result.user)
        assertEquals(username, result.user!!.username)
        assertNotNull("Token should not be null", result.user.token)
        assertTrue(
            "Token name should contain build model",
            result.user.token?.name?.contains(platform.udid) == true
        )

        return result.user
    }

    private fun createLoginResponse(fullName: String? = null): MockResponse {
        val body = """
            {"id": 1, "username": "$username", "full_name": "${fullName ?: ""}"}
        """.trimIndent()

        return MockResponse.Builder()
            .body(body)
            .addHeader("Content-Type", "application/json")
            .code(200)
            .build()
    }

    private suspend fun createGetTokenResponse(): MockResponse {
        val body = """
            [{"id": 1, "name": "${TestUtils.getTokenStub(platform)}", "sha1": "${TestUtils.generateHash()}"}]
        """.trimIndent()

        return MockResponse.Builder()
            .body(body)
            .addHeader("Content-Type", "application/json")
            .code(200)
            .build()
    }

    private suspend fun createTokenResponse(): MockResponse {
        val body = """
            {"id": 1, "name": "${TestUtils.getTokenStub(platform)}", "sha1": "${TestUtils.generateHash()}"}
        """.trimIndent()

        return MockResponse.Builder()
            .body(body)
            .addHeader("Content-Type", "application/json")
            .code(201)
            .build()
    }
}