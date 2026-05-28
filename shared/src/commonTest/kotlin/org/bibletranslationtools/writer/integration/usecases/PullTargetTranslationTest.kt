package org.bibletranslationtools.writer.integration.usecases

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertNull
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
import org.bibletranslationtools.writer.usecases.PullTargetTranslation
import org.eclipse.jgit.api.MergeResult
import org.eclipse.jgit.api.PullCommand
import org.eclipse.jgit.api.PullResult
import org.eclipse.jgit.api.errors.TransportException
import org.eclipse.jgit.errors.NoRemoteRepositoryException
import org.eclipse.jgit.merge.MergeStrategy
import org.eclipse.jgit.transport.URIish
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.koin.core.component.inject


class PullTargetTranslationTest : BaseIntegrationTest() {

    private val catalogClient: ResourceCatalogClient by inject()
    private val profile: Profile by inject()
    private val importProjects: ImportProjects by inject()
    private val pullTargetTranslation: PullTargetTranslation by inject()
    private val gogsLogin: GogsLogin by inject()
    private val preference: Preference by inject()
    private val platform: Platform by inject()

    private lateinit var targetTranslation: TargetTranslation

    override val needsLibrary = true

    @Before
    fun setUp() {
        every { profile.gogsUser } returns null  // default: no user logged in

        every {
            preference.getPref(Preference.KEY_PREF_GOGS_API, any(), String::class)
        } returns server.url("/api").toString()

        mockkConstructor(PullCommand::class)

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
    fun testPullTargetTranslationAuthorizedNewRepo() {
        loginGogsUser()

        var progressMessage: String? = null
        val onProgress: (Float, String?) -> Unit = { _, message ->
            progressMessage = message
        }

        processRepoResponse()

        every { anyConstructed<PullCommand>().call() }
            .throws(Exception("New repo doesn't have a branch yet."))

        val result = runBlocking {
            pullTargetTranslation.execute(
                targetTranslation,
                MergeStrategy.RECURSIVE,
                null,
                onProgress
            )
        }

        assertEquals(
            "Pull status should be UNKNOWN",
            PullTargetTranslation.Status.UNKNOWN,
            result.status
        )
        assertNotNull("Message should not be null", result.message)
        assertNotNull("Progress message should not be null", progressMessage)
    }

    @Test
    fun testPullTargetTranslationAuthorizedExistingRepo() {
        loginGogsUser()

        var progressMessage: String? = null
        val onProgress: (Float, String?) -> Unit = { _, message ->
            progressMessage = message
        }

        processRepoResponse()

        val pullResult: PullResult = mockk()
        val mergeResult: MergeResult = mockk()
        val conflicts = mapOf(
            "01.txt" to arrayOf(intArrayOf(0, 0)),
        )

        every { mergeResult.conflicts }.returns(conflicts)
        every { pullResult.mergeResult }.returns(mergeResult)
        every { anyConstructed<PullCommand>().call() }.returns(pullResult)

        val result = runBlocking {
            pullTargetTranslation.execute(
                targetTranslation,
                MergeStrategy.RECURSIVE,
                null,
                onProgress
            )
        }

        assertEquals(
            "Pull status should be MERGE_CONFLICTS",
            PullTargetTranslation.Status.MERGE_CONFLICTS,
            result.status
        )
        assertNotNull("Message should not be null", result.message)
        assertNotNull("Progress message should not be null", progressMessage)
    }

    @Test
    fun testPullTargetTranslationAuthorizedExistingRepoNoMergeConflicts() {
        loginGogsUser()

        var progressMessage: String? = null
        val onProgress: (Float, String?) -> Unit = { _, message ->
            progressMessage = message
        }

        processRepoResponse()

        val pullResult: PullResult = mockk()
        val mergeResult: MergeResult = mockk()

        every { mergeResult.conflicts }.returns(mapOf())
        every { pullResult.mergeResult }.returns(mergeResult)
        every { anyConstructed<PullCommand>().call() }.returns(pullResult)

        val result = runBlocking {
            pullTargetTranslation.execute(
                targetTranslation,
                MergeStrategy.RECURSIVE,
                null,
                onProgress
            )
        }

        assertEquals(
            "Pull status should be UP_TO_DATE",
            PullTargetTranslation.Status.UP_TO_DATE,
            result.status
        )
        assertNotNull("Message should not be null", result.message)
        assertNotNull("Progress message should not be null", progressMessage)
    }

    @Test
    fun testPullTargetTranslationAuthorizedConflicts() {
        loginGogsUser()

        var progressMessage: String? = null
        val onProgress: (Float, String?) -> Unit = { _, message ->
            progressMessage = message
        }

        processRepoResponse()

        val pullResult: PullResult = mockk()
        val mergeResult: MergeResult = mockk()
        val conflicts = mapOf(
            "manifest.json" to arrayOf(intArrayOf(0, 0)),
            "LICENSE.md" to arrayOf(intArrayOf(0, 0)),
        )

        every { mergeResult.conflicts }.returns(conflicts)
        every { pullResult.mergeResult }.returns(mergeResult)
        every { anyConstructed<PullCommand>().call() }.returns(pullResult)

        val result = runBlocking {
            pullTargetTranslation.execute(
                targetTranslation,
                MergeStrategy.RECURSIVE,
                null,
                onProgress
            )
        }

        assertEquals(
            "Pull status should be MERGE_CONFLICTS",
            PullTargetTranslation.Status.MERGE_CONFLICTS,
            result.status
        )
        assertNotNull("Message should not be null", result.message)
        assertNotNull("Progress message should not be null", progressMessage)
    }

    @Test
    fun testPullTargetTranslationUnAuthorizedFails() {
        var progressMessage: String? = null
        val onProgress: (Float, String?) -> Unit = { _, message ->
            progressMessage = message
        }

        val result = runBlocking {
            pullTargetTranslation.execute(
                targetTranslation,
                MergeStrategy.RECURSIVE,
                null,
                onProgress
            )
        }

        assertEquals(
            "Pull status should be AUTH_FAILURE",
            PullTargetTranslation.Status.AUTH_FAILURE,
            result.status
        )
        assertNotNull("Message should not be null", result.message)
        assertNull("Progress message should be null", progressMessage)
    }

    @Test
    fun testPullTargetTranslationAuthorizationFailed() {
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
        every { anyConstructed<PullCommand>().call() }
            .throws(exception)

        val result = runBlocking {
            pullTargetTranslation.execute(
                targetTranslation,
                MergeStrategy.RECURSIVE,
                null,
                onProgress
            )
        }

        assertEquals(
            "Pull status should be AUTH_FAILURE",
            PullTargetTranslation.Status.AUTH_FAILURE,
            result.status
        )
        assertNotNull("Message should not be null", result.message)
        assertNotNull("Progress message should not be null", progressMessage)
    }

    @Test
    fun testPullTargetTranslationRemoteNotFound() {
        loginGogsUser()

        var progressMessage: String? = null
        val onProgress: (Float, String?) -> Unit = { _, message ->
            progressMessage = message
        }

        processRepoResponse()

        val exception = TransportException(
            "New repo doesn't have a branch yet.",
            NoRemoteRepositoryException(URIish(), "No remote")
        )
        every { anyConstructed<PullCommand>().call() }
            .throws(exception)

        val result = runBlocking {
            pullTargetTranslation.execute(
                targetTranslation,
                MergeStrategy.RECURSIVE,
                null,
                onProgress
            )
        }

        assertEquals(
            "Pull status should be NO_REMOTE_REPO",
            PullTargetTranslation.Status.NO_REMOTE_REPO,
            result.status
        )
        assertNotNull("Message should not be null", result.message)
        assertNotNull("Progress message should not be null", progressMessage)
    }

    @Test
    fun testPullTargetTranslationUnknownTransportException() {
        loginGogsUser()

        var progressMessage: String? = null
        val onProgress: (Float, String?) -> Unit = { _, message ->
            progressMessage = message
        }

        processRepoResponse()

        every { anyConstructed<PullCommand>().call() }
            .throws(TransportException("An error occurred."))

        val result = runBlocking {
            pullTargetTranslation.execute(
                targetTranslation,
                MergeStrategy.RECURSIVE,
                null,
                onProgress
            )
        }

        assertEquals(
            "Pull status should be UNKNOWN",
            PullTargetTranslation.Status.UNKNOWN,
            result.status
        )
        assertNotNull("Message should not be null", result.message)
        assertNotNull("Progress message should not be null", progressMessage)
    }

    @Test
    fun testPullTargetTranslationOutOfMemoryError() {
        loginGogsUser()

        var progressMessage: String? = null
        val onProgress: (Float, String?) -> Unit = { _, message ->
            progressMessage = message
        }

        processRepoResponse()

        every { anyConstructed<PullCommand>().call() }
            .throws(OutOfMemoryError("Out of memory."))

        val result = runBlocking {
            pullTargetTranslation.execute(
                targetTranslation,
                MergeStrategy.RECURSIVE,
                null,
                onProgress
            )
        }

        assertEquals(
            "Pull status should be OUT_OF_MEMORY",
            PullTargetTranslation.Status.OUT_OF_MEMORY,
            result.status
        )
        assertNotNull("Message should not be null", result.message)
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
        server.enqueue(
            MockResponse.Builder()
            .body(repoResponse)
            .addHeader("Content-Type", "application/json")
                .build()) // fetch extra repo
    }
}
