package org.bibletranslationtools.writer.unit.usecases

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.mockkObject
import io.mockk.runs
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.bibletranslationtools.writer.AppInfo
import org.bibletranslationtools.writer.Platform
import org.bibletranslationtools.writer.data.Preference
import org.bibletranslationtools.writer.network.HttpRequest
import org.bibletranslationtools.writer.usecases.CheckForLatestRelease
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class CheckForLatestReleaseTest {

    @MockK private lateinit var preference: Preference
    @MockK private lateinit var platform: Platform
    @MockK private lateinit var info: AppInfo

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        mockkObject(HttpRequest)
        every { HttpRequest.lastResponse = any() } just runs
        every { platform.isStoreVersion }.returns(false)
        every { platform.info }.returns(info)
        every { preference.getGithubRepoApi() }.returns("http://localhost/api")
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `test checkForLatestRelease when there is a new store release`() = runTest {
        every { HttpRequest.httpClient } returns createMockClient()
        every { platform.isStoreVersion }.returns(true)
        every { info.versionCode }.returns(1)
        every { platform.info }.returns(info)

        val result = CheckForLatestRelease(preference, platform).execute()

        assertNotNull(result.release)
        val release = result.release!!
        assertEquals("release", release.name)
        assertEquals("https://play.google.com/store/apps/details?id=org.bibletranslationtools.writer", release.downloadUrl)
        assertEquals(12345, release.downloadSize)
        assertEquals(10, release.build)
        verify { preference.getGithubRepoApi() }
    }

    @Test
    fun `test checkForLatestRelease when there is a new github android release`() = runTest {
        every { HttpRequest.httpClient } returns createMockClient()
        every { platform.isStoreVersion }.returns(false)
        every { info.versionCode }.returns(1)
        every { platform.info }.returns(info)

        val result = CheckForLatestRelease(preference, platform).execute()

        assertNotNull(result.release)
        val release = result.release!!
        assertEquals("release", release.name)
        assertEquals("/download.apk", release.downloadUrl)
        assertEquals(12345, release.downloadSize)
        assertEquals(10, release.build)
        verify { preference.getGithubRepoApi() }
    }

    @Test
    fun `test checkForLatestRelease when there is no new release`() = runTest {
        every { HttpRequest.httpClient } returns createMockClient()
        every { info.versionCode }.returns(10)

        val result = CheckForLatestRelease(preference, platform).execute()

        assertNull(result.release)
        verify { preference.getGithubRepoApi() }
    }

    private fun createMockClient() = HttpClient(MockEngine {
        respond(releaseResponseJson, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
    }) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    private val releaseResponseJson = """
        {
            "tag_name": "tag+10",
            "name": "release",
            "assets": [
                {
                    "browser_download_url": "/download.apk",
                    "size": 12345
                }
            ]
        }
    """.trimIndent()
}
