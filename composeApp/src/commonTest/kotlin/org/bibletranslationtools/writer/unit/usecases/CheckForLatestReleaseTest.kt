package org.bibletranslationtools.writer.unit.usecases

import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.bibletranslationtools.writer.AppInfo
import org.bibletranslationtools.writer.Platform
import org.bibletranslationtools.writer.data.Preference
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

    private val server = MockWebServer()

    @Before
    fun setup() {
        MockKAnnotations.init(this)

        every { platform.isStoreVersion }.returns(false)
        every { platform.info }.returns(info)
        every { preference.getGithubRepoApi() }.returns(server.url("/api").toString())
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `test checkForLatestRelease when there is a new store release`() = runTest {
        server.enqueue(createReleaseResponse())

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
        server.enqueue(createReleaseResponse())

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
        server.enqueue(createReleaseResponse())

        every { info.versionCode }.returns(10)

        val result = CheckForLatestRelease(preference, platform).execute()

        assertNull(result.release)

        verify { preference.getGithubRepoApi() }
    }

    private fun createReleaseResponse(): MockResponse {
        val body = """
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

        return MockResponse()
            .addHeader("Content-Type", "application/json")
            .setBody(body)
            .setResponseCode(200)
    }
}