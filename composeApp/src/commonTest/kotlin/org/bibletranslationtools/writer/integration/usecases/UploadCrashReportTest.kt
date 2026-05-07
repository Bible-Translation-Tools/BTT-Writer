package org.bibletranslationtools.writer.integration.usecases

import io.mockk.every
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.bibletranslationtools.logger.Logger
import org.bibletranslationtools.writer.BaseIntegrationTest
import org.bibletranslationtools.writer.data.Preference
import org.bibletranslationtools.writer.usecases.UploadCrashReport
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.koin.core.component.inject
import java.io.File

class UploadCrashReportTest : BaseIntegrationTest() {

    private val preference: Preference by inject()

    private val server = MockWebServer()

    private lateinit var crashDir: File

    @Before
    fun setUp() {
        crashDir = runBlocking { directoryProvider.createTempDir("crashes") }
        Logger.registerGlobalExceptionHandler(crashDir)

        every {
            preference.getGithubBugReportRepo()
        } answers {
            server.url("/issues").toString()
        }
    }

    @After
    fun tearDown() {
        runBlocking { directoryProvider.clearCache() }
    }

    @Test
    fun testUploadCrashReport() = runTest {
        createStackTraces()

        server.enqueue(MockResponse().setBody("{success: true}").setResponseCode(200))

        val message = "Test crash report"
        val uploadCrashReport = UploadCrashReport(directoryProvider, preference)
        val reported = uploadCrashReport.execute(message)

        assertTrue("Upload success when response code 200", reported)

        val request = server.takeRequest().body.readString(Charsets.UTF_8)

        assertTrue(
            "Upload request body contains message",
            request.contains(message)
        )
        assertTrue(
            "Upload request body contains stacktrace",
            request.contains("This is a crash")
        )
        assertTrue(
            "Upload request body contains environment",
            request.contains("Environment")
        )

        assertTrue(
            "Crash dir is empty after successful upload",
            crashDir.listFiles()?.isEmpty() ?: true
        )
    }

    @Test
    fun crashReportFailsWhenNoCrashes() = runTest {
        deleteStackTraces()

        server.enqueue(MockResponse().setBody("{success: true}").setResponseCode(200))

        val message = "Test crash report"
        val uploadCrashReport = UploadCrashReport(directoryProvider, preference)
        val reported = uploadCrashReport.execute(message)

        assertFalse("Upload failed when no crash files", reported)
    }

    @Test
    fun testUploadCrashServerDown() = runTest {
        createStackTraces()

        server.enqueue(MockResponse().setResponseCode(500))

        val message = "Test crash report"
        val uploadCrashReport = UploadCrashReport(directoryProvider, preference)
        val reported = uploadCrashReport.execute(message)

        assertFalse("Upload fails when response code 500", reported)
    }

    private fun createStackTraces() {
        val crash = runBlocking {
            directoryProvider.createTempFile("crash", ".stacktrace", crashDir)
        }
        crash.writeText("This is a crash")
    }

    private fun deleteStackTraces() {
        Logger.flush()
    }
}
