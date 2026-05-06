package org.bibletranslationtools.writer.unit.usecases

import btt_writer.composeapp.generated.resources.Res
import btt_writer.composeapp.generated.resources.gogs_public_key_name
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkObject
import io.mockk.runs
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.bibletranslationtools.gogsclient.GogsAPI
import org.bibletranslationtools.gogsclient.PublicKey
import org.bibletranslationtools.gogsclient.Response
import org.bibletranslationtools.writer.DirectoryProvider
import org.bibletranslationtools.writer.Platform
import org.bibletranslationtools.writer.core.Profile
import org.bibletranslationtools.writer.data.Preference
import org.bibletranslationtools.writer.TestUtils
import org.bibletranslationtools.writer.usecases.RegisterSSHKeys
import org.bibletranslationtools.writer.utils.FileUtilities
import org.jetbrains.compose.resources.getString
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

class RegisterSSHKeysTest {

    @MockK private lateinit var profile: Profile
    @MockK private lateinit var directoryProvider: DirectoryProvider
    @MockK private lateinit var preference: Preference
    @MockK private lateinit var platform: Platform

    private val onProgress = mockk<(Float, String?) -> Unit>(relaxed = true)

    @Before
    fun setup() {
        MockKAnnotations.init(this)

        every { platform.udid }.returns("1234567890")

        mockkObject(FileUtilities)
        every { FileUtilities.readFileToString(any()) }.returns("public_key_string")

        mockkConstructor(GogsAPI::class)
        coEvery { anyConstructed<GogsAPI>().listPublicKeys(any()) }.returns(listOf())
        coEvery { anyConstructed<GogsAPI>().deletePublicKey(any(), any()) }.returns(true)
        coEvery { anyConstructed<GogsAPI>().createPublicKey(any(), any()) }.returns(mockk())

        every { onProgress(any(), any()) }.just(runs)
        coEvery { directoryProvider.generateSSHKeys(any()) }.just(runs)
        every { directoryProvider.publicKey }.returns(mockk())

        every { preference.getPref(any(), any(), String::class) }
            .returns("/api")
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `test register ssh keys authorized, no force`() = runTest {
        every { profile.gogsUser }.returns(mockk())
        every { directoryProvider.hasSSHKeys() }.returns(true)

        val success = RegisterSSHKeys(
            profile,
            directoryProvider,
            preference,
            platform
        ).execute(false, onProgress)

        assertTrue(success)

        verifyCommonCalls()

        coVerify(exactly = 0) { anyConstructed<GogsAPI>().deletePublicKey(any(), any()) }
        verify(exactly = 0) { anyConstructed<GogsAPI>().getLastResponse() }
        coVerify(exactly = 0) { directoryProvider.generateSSHKeys(any()) }
    }

    @Test
    fun `test register ssh keys not authorized`() = runTest {
        every { profile.gogsUser }.returns(null)

        val success = RegisterSSHKeys(
            profile,
            directoryProvider,
            preference,
            platform
        ).execute(false, onProgress)

        assertFalse(success)

        verify { profile.gogsUser }
        verify { platform.udid }
        verify { onProgress(any(), "Authenticating") }

        coVerify(exactly = 0) { anyConstructed<GogsAPI>().listPublicKeys(any()) }
        coVerify(exactly = 0) { directoryProvider.generateSSHKeys(any()) }
    }

    @Test
    fun `test register ssh keys, no ssh keys`() = runTest {
        every { profile.gogsUser }.returns(mockk())
        every { directoryProvider.hasSSHKeys() }.returns(false)

        val success = RegisterSSHKeys(
            profile,
            directoryProvider,
            preference,
            platform
        ).execute(false, onProgress)

        assertTrue(success)

        verifyCommonCalls()

        coVerify { directoryProvider.generateSSHKeys(any()) }
        coVerify(exactly = 0) { anyConstructed<GogsAPI>().deletePublicKey(any(), any()) }
        verify(exactly = 0) { anyConstructed<GogsAPI>().getLastResponse() }
    }

    @Test
    fun `test register ssh keys, force generate`() = runTest {
        every { profile.gogsUser }.returns(mockk())
        every { directoryProvider.hasSSHKeys() }.returns(true)

        val success = RegisterSSHKeys(
            profile,
            directoryProvider,
            preference,
            platform
        ).execute(true, onProgress)

        assertTrue(success)

        verifyCommonCalls()

        coVerify { directoryProvider.generateSSHKeys(any()) }
        coVerify(exactly = 0) { anyConstructed<GogsAPI>().deletePublicKey(any(), any()) }
        verify(exactly = 0) { anyConstructed<GogsAPI>().getLastResponse() }
    }

    @Test
    fun `test register ssh keys, read key fails`() = runTest {
        every { profile.gogsUser }.returns(mockk())
        every { directoryProvider.hasSSHKeys() }.returns(true)

        every { FileUtilities.readFileToString(any()) }.throws(IOException("An error occurred."))

        val success = RegisterSSHKeys(
            profile,
            directoryProvider,
            preference,
            platform
        ).execute(false, onProgress)

        assertFalse(success)

        verify { profile.gogsUser }
        verify { directoryProvider.hasSSHKeys() }
        coVerify(exactly = 0) { directoryProvider.generateSSHKeys(any()) }
        coVerify(exactly = 0) { anyConstructed<GogsAPI>().listPublicKeys(any()) }
        coVerify(exactly = 0) { anyConstructed<GogsAPI>().deletePublicKey(any(), any()) }
        verify(exactly = 0) { anyConstructed<GogsAPI>().getLastResponse() }
    }

    @Test
    fun `test register ssh keys, delete app public keys`() = runTest {
        every { profile.gogsUser }.returns(mockk())
        every { directoryProvider.hasSSHKeys() }.returns(true)

        val publicKey: PublicKey = mockk {
            every { title }.returns("${getString(Res.string.gogs_public_key_name)} ${platform.udid}")
        }
        coEvery { anyConstructed<GogsAPI>().listPublicKeys(any()) }.returns(listOf(publicKey))

        val success = RegisterSSHKeys(
            profile,
            directoryProvider,
            preference,
            platform
        ).execute(false, onProgress)

        assertTrue(success)

        verify { profile.gogsUser }
        verify { directoryProvider.hasSSHKeys() }
        coVerify { anyConstructed<GogsAPI>().listPublicKeys(any()) }
        coVerify { anyConstructed<GogsAPI>().deletePublicKey(any(), any()) }
        coVerify(exactly = 0) { directoryProvider.generateSSHKeys(any()) }
        verify(exactly = 0) { anyConstructed<GogsAPI>().getLastResponse() }
    }

    @Test
    fun `test register ssh keys, delete custom public keys fails`() = runTest {
        every { profile.gogsUser }.returns(mockk())
        every { directoryProvider.hasSSHKeys() }.returns(true)

        val publicKey: PublicKey = mockk {
            every { title }.returns("my personal key")
        }
        coEvery { anyConstructed<GogsAPI>().listPublicKeys(any()) }.returns(listOf(publicKey))

        val success = RegisterSSHKeys(
            profile,
            directoryProvider,
            preference,
            platform
        ).execute(false, onProgress)

        assertTrue(success)

        verify { profile.gogsUser }
        verify { directoryProvider.hasSSHKeys() }
        coVerify { anyConstructed<GogsAPI>().listPublicKeys(any()) }
        coVerify(exactly = 0) { anyConstructed<GogsAPI>().deletePublicKey(any(), any()) }
        coVerify(exactly = 0) { directoryProvider.generateSSHKeys(any()) }
        verify(exactly = 0) { anyConstructed<GogsAPI>().getLastResponse() }
    }

    @Test
    fun `test register ssh keys, create new key fails`() = runTest {
        every { profile.gogsUser }.returns(mockk())
        every { directoryProvider.hasSSHKeys() }.returns(true)

        coEvery { anyConstructed<GogsAPI>().createPublicKey(any(), any()) }.returns(null)

        val response: Response = mockk {
            every { code }.returns(500)
            every { message }.returns("Internal Server Error")
        }
        TestUtils.setPropertyReflection(response, "exception", Exception("Error!"))
        every { anyConstructed<GogsAPI>().getLastResponse() }.returns(response)

        val success = RegisterSSHKeys(
            profile,
            directoryProvider,
            preference,
            platform
        ).execute(false, onProgress)

        assertFalse(success)

        verify { profile.gogsUser }
        verify { directoryProvider.hasSSHKeys() }
        coVerify { anyConstructed<GogsAPI>().listPublicKeys(any()) }
        coVerify { anyConstructed<GogsAPI>().createPublicKey(any(), any()) }
        verify { anyConstructed<GogsAPI>().getLastResponse() }
        coVerify(exactly = 0) { anyConstructed<GogsAPI>().deletePublicKey(any(), any()) }
        coVerify(exactly = 0) { directoryProvider.generateSSHKeys(any()) }
    }

    private fun verifyCommonCalls() {
        verify { profile.gogsUser }
        verify { directoryProvider.hasSSHKeys() }
        verify { platform.udid }
        verify { FileUtilities.readFileToString(any()) }
        coVerify { anyConstructed<GogsAPI>().listPublicKeys(any()) }
        coVerify { anyConstructed<GogsAPI>().createPublicKey(any(), any()) }
        verify { directoryProvider.publicKey }
        verify { onProgress(any(), "Authenticating") }
    }
}