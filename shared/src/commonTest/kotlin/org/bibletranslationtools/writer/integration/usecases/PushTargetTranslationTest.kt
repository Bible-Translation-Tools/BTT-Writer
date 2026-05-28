package org.bibletranslationtools.writer.integration.usecases

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.runBlocking
import mockwebserver3.MockResponse
import org.bibletranslationtools.resourcecatalog.ResourceCatalogClient
import org.bibletranslationtools.writer.BaseIntegrationTest
import org.bibletranslationtools.writer.Platform
import org.bibletranslationtools.writer.TestUtils
import org.bibletranslationtools.writer.core.Profile
import org.bibletranslationtools.writer.core.TargetTranslation
import org.bibletranslationtools.writer.data.Preference
import org.bibletranslationtools.writer.usecases.GogsLogin
import org.bibletranslationtools.writer.usecases.ImportProjects
import org.bibletranslationtools.writer.usecases.PushTargetTranslation
import org.eclipse.jgit.api.PushCommand
import org.eclipse.jgit.api.errors.TransportException
import org.eclipse.jgit.errors.NoRemoteRepositoryException
import org.eclipse.jgit.transport.PushResult
import org.eclipse.jgit.transport.RemoteRefUpdate
import org.eclipse.jgit.transport.URIish
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.koin.core.component.inject


class PushTargetTranslationTest : BaseIntegrationTest() {

    private val importProjects: ImportProjects by inject()
    private val catalogClient: ResourceCatalogClient by inject()
    private val profile: Profile by inject()
    private val gogsLogin: GogsLogin by inject()
    private val pushTargetTranslation: PushTargetTranslation by inject()
    private val preference: Preference by inject()
    private val platform: Platform by inject()

    private lateinit var targetTranslation: TargetTranslation

    override val needsLibrary = true

    @Before
    fun setUp() {
        every { profile.gogsUser } returns null  // default: no user logged in

        mockkConstructor(PushCommand::class)

        every {
            preference.getPref(Preference.KEY_PREF_GOGS_API, any(), String::class)
        } returns server.url("/api/").toString()

        targetTranslation = runBlocking {
            TestUtils.importTargetTranslation(
                catalogClient,
                platform,
                directoryProvider,
                profile,
                importProjects,
                "aae",
                "usfm/mrk.usfm"
            )!!
        }
    }

    @After
    fun tearDown() {
        runBlocking { directoryProvider.clearCache() }
        profile.logout()
    }

    @Test
    fun testPushTargetTranslationAuthorized() {
        loginGogsUser()

        var progressMessage: String? = null
        val onProgress: (Float, String?) -> Unit = { _, message ->
            progressMessage = message
        }

        processRepoResponse()

        val pushResult: PushResult = mockk()
        val refUpdate: RemoteRefUpdate = mockk {
            every { status }.returns(RemoteRefUpdate.Status.OK)
            every { remoteName }.returns("test_repo")
        }
        every { pushResult.remoteUpdates }.returns(listOf(refUpdate))
        every { anyConstructed<PushCommand>().call() }.returns(listOf(pushResult))

        val result = runBlocking {
            pushTargetTranslation.execute(targetTranslation, onProgress)
        }

        assertEquals(
            "Push should fail, because remote exists but not synced with local",
            PushTargetTranslation.Status.OK,
            result.status
        )
        assertFalse("Push should not be rejected", result.status.isRejected)
        assertNotNull("Result message should not be null", result.message)
        assertNotNull("Progress message should not be null", progressMessage)
    }

    @Test
    fun testPushTargetTranslationNotSynced() {
        loginGogsUser()

        var progressMessage: String? = null
        val onProgress: (Float, String?) -> Unit = { _, message ->
            progressMessage = message
        }

        processRepoResponse()

        val pushResult: PushResult = mockk()
        val refUpdate: RemoteRefUpdate = mockk {
            every { status }.returns(RemoteRefUpdate.Status.REJECTED_NONFASTFORWARD)
            every { remoteName }.returns("test_repo")
        }
        every { pushResult.remoteUpdates }.returns(listOf(refUpdate))
        every { anyConstructed<PushCommand>().call() }.returns(listOf(pushResult))

        val result = runBlocking {
            pushTargetTranslation.execute(targetTranslation, onProgress)
        }

        assertEquals(
            "Push should fail, because remote exists but not synced with local",
            PushTargetTranslation.Status.REJECTED_NON_FAST_FORWARD,
            result.status
        )
        assertTrue("Push should be rejected", result.status.isRejected)
        assertNotNull("Result message should not be null", result.message)
        assertNotNull("Progress message should not be null", progressMessage)
    }

    @Test
    fun testPushTargetTranslationRefDeleteNotAllowed() {
        loginGogsUser()

        var progressMessage: String? = null
        val onProgress: (Float, String?) -> Unit = { _, message ->
            progressMessage = message
        }

        processRepoResponse()

        val pushResult: PushResult = mockk()
        val refUpdate: RemoteRefUpdate = mockk {
            every { status }.returns(RemoteRefUpdate.Status.REJECTED_NODELETE)
            every { remoteName }.returns("test_repo")
        }
        every { pushResult.remoteUpdates }.returns(listOf(refUpdate))
        every { anyConstructed<PushCommand>().call() }.returns(listOf(pushResult))

        val result = runBlocking {
            pushTargetTranslation.execute(targetTranslation, onProgress)
        }

        assertEquals(
            "Push should fail, because remote doesn't allow deleting refs",
            PushTargetTranslation.Status.REJECTED_NODELETE,
            result.status
        )
        assertTrue("Push should be rejected", result.status.isRejected)
        assertNotNull("Result message should not be null", result.message)
        assertNotNull("Progress message should not be null", progressMessage)
    }

    @Test
    fun testPushTargetTranslationRemoteChanged() {
        loginGogsUser()

        var progressMessage: String? = null
        val onProgress: (Float, String?) -> Unit = { _, message ->
            progressMessage = message
        }

        processRepoResponse()

        val pushResult: PushResult = mockk()
        val refUpdate: RemoteRefUpdate = mockk {
            every { status }.returns(RemoteRefUpdate.Status.REJECTED_REMOTE_CHANGED)
            every { remoteName }.returns("test_repo")
        }
        every { pushResult.remoteUpdates }.returns(listOf(refUpdate))
        every { anyConstructed<PushCommand>().call() }.returns(listOf(pushResult))

        val result = runBlocking {
            pushTargetTranslation.execute(targetTranslation, onProgress)
        }

        assertEquals(
            "Push should fail, because remote changed during push",
            PushTargetTranslation.Status.REJECTED_REMOTE_CHANGED,
            result.status
        )
        assertTrue("Push should be rejected", result.status.isRejected)
        assertNotNull("Result message should not be null", result.message)
        assertNotNull("Progress message should not be null", progressMessage)
    }

    @Test
    fun testPushTargetTranslationRejectedByOtherReason() {
        loginGogsUser()

        var progressMessage: String? = null
        val onProgress: (Float, String?) -> Unit = { _, message ->
            progressMessage = message
        }

        processRepoResponse()

        val pushResult: PushResult = mockk()
        val refUpdate: RemoteRefUpdate = mockk {
            every { status }.returns(RemoteRefUpdate.Status.REJECTED_OTHER_REASON)
            every { remoteName }.returns("test_repo")
            every { message }.returns("Unknown reason.")
        }
        every { pushResult.remoteUpdates }.returns(listOf(refUpdate))
        every { anyConstructed<PushCommand>().call() }.returns(listOf(pushResult))

        val result = runBlocking {
            pushTargetTranslation.execute(targetTranslation, onProgress)
        }

        assertEquals(
            "Push should fail for other reason",
            PushTargetTranslation.Status.REJECTED_OTHER_REASON,
            result.status
        )
        assertTrue("Push should be rejected", result.status.isRejected)
        assertNotNull("Result message should not be null", result.message)
        assertNotNull("Progress message should not be null", progressMessage)
    }

    @Test
    fun testPushTargetTranslationNotRejected() {
        loginGogsUser()

        var progressMessage: String? = null
        val onProgress: (Float, String?) -> Unit = { _, message ->
            progressMessage = message
        }

        processRepoResponse()

        val pushResult: PushResult = mockk()
        val refUpdate: RemoteRefUpdate = mockk {
            every { status }.returns(RemoteRefUpdate.Status.NON_EXISTING)
            every { remoteName }.returns("test_repo")
            every { message }.returns("Unknown reason.")
        }
        every { pushResult.remoteUpdates }.returns(listOf(refUpdate))
        every { anyConstructed<PushCommand>().call() }.returns(listOf(pushResult))

        val result = runBlocking {
            pushTargetTranslation.execute(targetTranslation, onProgress)
        }

        assertEquals(
            "Push should fail for other reason",
            PushTargetTranslation.Status.UNKNOWN,
            result.status
        )
        assertFalse("Push should not be rejected", result.status.isRejected)
        assertNotNull("Result message should not be null", result.message)
        assertNotNull("Progress message should not be null", progressMessage)
    }

    @Test
    fun testPushTargetTranslationUnAuthorized() {
        var progressMessage: String? = null
        val onProgress: (Float, String?) -> Unit = { _, message ->
            progressMessage = message
        }

        val result = runBlocking {
            pushTargetTranslation.execute(targetTranslation, onProgress)
        }

        assertEquals(
            "Fails when there is no auth user",
            PushTargetTranslation.Status.AUTH_FAILURE,
            result.status
        )
        assertNull("Result message should be null", result.message)
        assertNull("Progress message should be null", progressMessage)
    }

    @Test
    fun testPushTargetTranslationAuthorizationFails() {
        loginGogsUser()

        var progressMessage: String? = null
        val onProgress: (Float, String?) -> Unit = { _, message ->
            progressMessage = message
        }

        processRepoResponse()

        val exception = TransportException(
            "An error occurred.",
            Exception(
                Exception("Auth fail")
            )
        )
        every { anyConstructed<PushCommand>().call() }
            .throws(exception)

        val result = runBlocking {
            pushTargetTranslation.execute(targetTranslation, onProgress)
        }

        assertEquals(
            "Push should fail",
            PushTargetTranslation.Status.AUTH_FAILURE,
            result.status
        )
        assertNull("Result message should be null", result.message)
        assertNotNull("Progress message should not be null", progressMessage)
    }

    @Test
    fun testPushTargetTranslationToPrivateRepoFails() {
        loginGogsUser()

        var progressMessage: String? = null
        val onProgress: (Float, String?) -> Unit = { _, message ->
            progressMessage = message
        }

        processRepoResponse()

        val exception = TransportException(
            "An error occurred.",
            Exception("Push to private repo is not permitted")
        )
        every { anyConstructed<PushCommand>().call() }
            .throws(exception)

        val result = runBlocking {
            pushTargetTranslation.execute(targetTranslation, onProgress)
        }

        assertEquals(
            "Push should fail",
            PushTargetTranslation.Status.AUTH_FAILURE,
            result.status
        )
        assertNull("Result message should be null", result.message)
        assertNotNull("Progress message should not be null", progressMessage)
    }

    @Test
    fun testPushTargetTranslationNoRemoteException() {
        loginGogsUser()

        var progressMessage: String? = null
        val onProgress: (Float, String?) -> Unit = { _, message ->
            progressMessage = message
        }

        processRepoResponse()

        val exception = TransportException(
            "An error occurred.",
            NoRemoteRepositoryException(URIish(), "Remote doesn't exist")
        )
        every { anyConstructed<PushCommand>().call() }
            .throws(exception)

        val result = runBlocking {
            pushTargetTranslation.execute(targetTranslation, onProgress)
        }

        assertEquals(
            "Push should fail",
            PushTargetTranslation.Status.NO_REMOTE_REPO,
            result.status
        )
        assertNull("Result message should be null", result.message)
        assertNotNull("Progress message should not be null", progressMessage)
    }

    @Test
    fun testPushTargetTranslationUnknownTransportException() {
        loginGogsUser()

        var progressMessage: String? = null
        val onProgress: (Float, String?) -> Unit = { _, message ->
            progressMessage = message
        }

        processRepoResponse()

        val exception = TransportException(
            "An error occurred.",
            Exception("Remote doesn't exist")
        )
        every { anyConstructed<PushCommand>().call() }
            .throws(exception)

        val result = runBlocking {
            pushTargetTranslation.execute(targetTranslation, onProgress)
        }

        assertEquals(
            "Push should fail",
            PushTargetTranslation.Status.UNKNOWN,
            result.status
        )
        assertNull("Result message should be null", result.message)
        assertNotNull("Progress message should not be null", progressMessage)
    }

    @Test
    fun testPushTargetTranslationOutOfMemoryError() {
        loginGogsUser()

        var progressMessage: String? = null
        val onProgress: (Float, String?) -> Unit = { _, message ->
            progressMessage = message
        }

        processRepoResponse()

        every { anyConstructed<PushCommand>().call() }
            .throws(OutOfMemoryError("An error occurred."))

        val result = runBlocking {
            pushTargetTranslation.execute(targetTranslation, onProgress)
        }

        assertEquals(
            "Push should fail",
            PushTargetTranslation.Status.OUT_OF_MEMORY,
            result.status
        )
        assertNull("Result message should be null", result.message)
        assertNotNull("Progress message should not be null", progressMessage)
    }

    @Test
    fun testPushTargetTranslationGenericError() {
        loginGogsUser()

        var progressMessage: String? = null
        val onProgress: (Float, String?) -> Unit = { _, message ->
            progressMessage = message
        }

        processRepoResponse()

        every { anyConstructed<PushCommand>().call() }
            .throws(Exception("An error occurred."))

        val result = runBlocking {
            pushTargetTranslation.execute(targetTranslation, onProgress)
        }

        assertEquals(
            "Push should fail",
            PushTargetTranslation.Status.UNKNOWN,
            result.status
        )
        assertNull("Result message should be null", result.message)
        assertNotNull("Progress message should not be null", progressMessage)
    }

    private fun loginGogsUser() {
        val user = runBlocking {
            TestUtils.simulateLoginGogsUser(platform, server, gogsLogin, "test")
        }
        every { profile.gogsUser } returns user
        profile.gogsUser = user
    }

    private fun processRepoResponse() {
        val repoResponse = """
            {
                "name": "${targetTranslation.id}",
                "ssh_url": "http://example.com/repo.git",
                "owner": {
                    "username": "${profile.gogsUser!!.username}"
                }
            }
        """.trimIndent()
        val reposResponse = """
            {"data": [$repoResponse], "ok": true}
        """.trimIndent()

        server.enqueue(MockResponse()) // create repo response
        server.enqueue(MockResponse.Builder()
            .body(reposResponse)
            .addHeader("Content-Type", "application/json")
            .build()) // fetch repos response
        server.enqueue(MockResponse.Builder()
            .body(repoResponse)
            .addHeader("Content-Type", "application/json")
            .build()) // fetch extra repo
    }
}
