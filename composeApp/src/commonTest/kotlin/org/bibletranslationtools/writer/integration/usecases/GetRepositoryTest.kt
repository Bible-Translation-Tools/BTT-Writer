package org.bibletranslationtools.writer.integration.usecases

import io.mockk.every
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.bibletranslationtools.resourcecatalog.ResourceCatalogClient
import org.bibletranslationtools.writer.BaseIntegrationTest
import org.bibletranslationtools.writer.Platform
import org.bibletranslationtools.writer.TestUtils
import org.bibletranslationtools.writer.core.Profile
import org.bibletranslationtools.writer.core.TargetTranslation
import org.bibletranslationtools.writer.data.Preference
import org.bibletranslationtools.writer.usecases.GetRepository
import org.bibletranslationtools.writer.usecases.GogsLogin
import org.bibletranslationtools.writer.usecases.ImportProjects
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.koin.core.component.inject


class GetRepositoryTest : BaseIntegrationTest() {

    private val catalogClient: ResourceCatalogClient by inject()
    private val profile: Profile by inject()
    private val getRepository: GetRepository by inject()
    private val gogsLogin: GogsLogin by inject()
    private val preference: Preference by inject()
    private val importProjects: ImportProjects by inject()
    private val platform: Platform by inject()

    private lateinit var targetTranslation: TargetTranslation

    private val server = MockWebServer()

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
    }

    @Test
    fun getRepositorySucceeds() = runTest {
        loginGogsUser()
        processRepoResponse(targetTranslation.id)

        val repo = getRepository.execute(targetTranslation)

        assertNotNull("Repository should not be null", repo)

        assertEquals(targetTranslation.id, repo!!.name)
    }

    @Test
    fun getRepositoryThatIsNotExactNameFails() = runTest {
        loginGogsUser()
        processRepoResponse("${targetTranslation.id}_L3")

        val repo = getRepository.execute(targetTranslation)

        assertNull("Repository should be null", repo)
    }

    @Test
    fun getRepositoryNotAuthorizedFails() = runTest {
        val repo = getRepository.execute(targetTranslation)

        assertNull("Repository should be null", repo)
    }

    private fun loginGogsUser() = runTest {
        profile.gogsUser = TestUtils.simulateLoginGogsUser(
            platform,
            server,
            gogsLogin,
            "test"
        )
    }

    private fun processRepoResponse(id: String) {
        val repoResponse = """
            {
                "name": "$id",
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
        server.enqueue(MockResponse()
            .setBody(reposResponse)
            .addHeader("Content-Type", "application/json")
        ) // fetch repos response
        server.enqueue(MockResponse()
            .setBody(repoResponse)
            .addHeader("Content-Type", "application/json")
        ) // fetch extra repo
    }
}