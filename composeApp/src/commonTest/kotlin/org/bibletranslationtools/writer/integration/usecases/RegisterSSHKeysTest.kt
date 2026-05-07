package org.bibletranslationtools.writer.integration.usecases

import io.mockk.every
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.bibletranslationtools.gogsclient.User
import org.bibletranslationtools.writer.BaseIntegrationTest
import org.bibletranslationtools.writer.core.Profile
import org.bibletranslationtools.writer.data.Preference
import org.bibletranslationtools.writer.usecases.RegisterSSHKeys
import org.bibletranslationtools.writer.utils.FileUtilities
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Test
import org.koin.core.component.inject

class RegisterSSHKeysTest : BaseIntegrationTest() {

    private val registerSSHKeys: RegisterSSHKeys by inject()
    private val profile: Profile by inject()
    private val preference: Preference by inject()

    private val server = MockWebServer()

    @Before
    fun setUp() {
        every { profile.gogsUser } returns null  // default: no user logged in

        deleteSSHKeys()

        every {
            preference.getPref(Preference.KEY_PREF_GOGS_API, any(), String::class)
        } returns server.url("/api/").toString()

        val dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                return when (request.path) {
                    "/api/users/test/keys" -> createResponse("get_keys")
                    "/api/user/keys/test_key" -> createResponse("delete_key")
                    "/api/user/keys" -> createResponse("create_key")
                    else -> createResponse("not_found")
                }
            }
        }
        server.dispatcher = dispatcher
    }

    @Test
    fun testRegisterSSHKeys() = runTest {
        loginGogsUser()

        var progressMessage: String? = null
        val onProgress: (Float, String?) -> Unit = { _, message ->
            progressMessage = message
        }

        val hasSSHKeys = directoryProvider.hasSSHKeys()
        assertFalse("SSH keys should not exist", hasSSHKeys)

        val registered = registerSSHKeys.execute(false, onProgress)

        assertTrue("SSH keys registered ", registered)
        assertNotNull("Progress message should not be null", progressMessage)

        val publicKeyBefore = directoryProvider.publicKey.inputStream().use {
            it.bufferedReader().readText()
        }

        progressMessage = null
        val registered2 = registerSSHKeys.execute(true, onProgress)

        assertTrue("SSH keys registered with force flag", registered2)
        assertNotNull("Progress message should not be null", progressMessage)

        val publicKeyAfter = directoryProvider.publicKey.inputStream().use {
            it.bufferedReader().readText()
        }

        assertNotEquals(
            "Public key should be recreated when force flag used",
            publicKeyBefore,
            publicKeyAfter
        )
    }

    @Test
    fun testRegisterSSHKeys_noUser() = runTest {
        var progressMessage: String? = null
        val onProgress: (Float, String?) -> Unit = { _, message ->
            progressMessage = message
        }

        val registered = registerSSHKeys.execute(false, onProgress)

        assertFalse("SSH keys not registered without user", registered)
        assertNotNull("Progress message should not be null", progressMessage)
    }

    private fun loginGogsUser() {
        val user = User(username = "test", password = "test")
        every { profile.gogsUser } returns user
        profile.gogsUser = user
    }

    private fun deleteSSHKeys() {
        val keysDir = directoryProvider.sshKeysDir.listFiles()
        if (keysDir != null) {
            for (file in keysDir) {
                FileUtilities.deleteQuietly(file)
            }
        }
    }

    private fun createResponse(requestId: String): MockResponse {
        return when (requestId) {
            "get_keys" -> {
                val body = """
                [
                    {
                        "title": "test_title_1",
                        "key": "test_key_1"
                    },
                    {
                        "title": "test_title_2",
                        "key": "test_key_2"
                    }
                ]
                """.trimIndent()
                MockResponse()
                    .setBody(body)
                    .addHeader("Content-Type", "application/json")
                    .setResponseCode(200)
            }
            "delete_key" -> MockResponse().setResponseCode(204)
            "create_key" -> {
                val body = """
                {
                    "title": "test_title_1",
                    "key": "test_key_1"
                }
                """.trimIndent()
                MockResponse()
                    .setBody(body)
                    .addHeader("Content-Type", "application/json")
                    .setResponseCode(201)
            }
            else -> MockResponse().setResponseCode(404)
        }
    }
}
