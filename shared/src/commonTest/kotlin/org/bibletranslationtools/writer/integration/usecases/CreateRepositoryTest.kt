package org.bibletranslationtools.writer.integration.usecases

import io.mockk.every
import kotlinx.coroutines.runBlocking
import mockwebserver3.MockResponse
import org.bibletranslationtools.resourcecatalog.ResourceCatalogClient
import org.bibletranslationtools.writer.BaseIntegrationTest
import org.bibletranslationtools.writer.Platform
import org.bibletranslationtools.writer.TestUtils
import org.bibletranslationtools.writer.core.Profile
import org.bibletranslationtools.writer.core.TargetTranslation
import org.bibletranslationtools.writer.data.Preference
import org.bibletranslationtools.writer.usecases.CreateRepository
import org.bibletranslationtools.writer.usecases.GogsLogin
import org.bibletranslationtools.writer.usecases.ImportProjects
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.koin.core.component.inject


class CreateRepositoryTest : BaseIntegrationTest() {

    private val catalogClient: ResourceCatalogClient by inject()
    private val profile: Profile by inject()
    private val createRepository: CreateRepository by inject()
    private val gogsLogin: GogsLogin by inject()
    private val preference: Preference by inject()
    private val importProjects: ImportProjects by inject()
    private val platform: Platform by inject()

    private lateinit var targetTranslation: TargetTranslation

    override val needsLibrary = true

    @Before
    fun setUp() {
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
    fun createRepositoryWithAuthenticationSucceeds() = runBlocking {
        loginGogsUser()

        createRepoResponse(201)

        val created = createRepository.execute(targetTranslation)

        assertTrue("Repository should be created when authenticated", created)
    }

    @Test
    fun createRepositoryThatAlreadyExistsSucceeds() = runBlocking {
        loginGogsUser()

        createRepoResponse(409)

        val created = createRepository.execute(targetTranslation)

        assertTrue("Repository should be created when authenticated", created)
    }

    @Test
    fun createRepositoryServerError() = runBlocking {
        loginGogsUser()

        createRepoResponse(500)

        val created = createRepository.execute(targetTranslation)

        assertFalse("Repository should not be created", created)
    }

    @Test
    fun createRepositoryWithoutAuthenticationFails() = runBlocking {
        var progressMessage: String? = null
        val onProgress: (Float, String?) -> Unit = { _, message ->
            progressMessage = message
        }

        createRepoResponse(403)

        val created = createRepository.execute(targetTranslation, onProgress)

        assertFalse("Repository should not be created when not authenticated", created)
        assertNotNull("Progress message should not be null", progressMessage)
    }

    private fun loginGogsUser()  = runBlocking{
        profile.gogsUser = TestUtils.simulateLoginGogsUser(
            platform,
            server,
            gogsLogin,
            "test"
        )
    }

    private fun createRepoResponse(responseCode: Int) {
        val body = """
            {
                "name": "${targetTranslation.id}",
                "ssh_url": "http://example.com/repo.git",
                "owner": {
                    "username": "${profile.gogsUser!!.username}"
                }
            }
        """.trimIndent()

        server.enqueue(MockResponse.Builder()
            .body(body)
            .addHeader("Content-Type", "application/json")
            .code(responseCode)
            .build()
        )
    }
}