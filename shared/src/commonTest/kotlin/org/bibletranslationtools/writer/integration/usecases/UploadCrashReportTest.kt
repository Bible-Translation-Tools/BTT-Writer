package org.bibletranslationtools.writer.integration.usecases

import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.runBlocking
import mockwebserver3.MockResponse
import org.bibletranslationtools.logger.Context
import org.bibletranslationtools.logger.HttpReporter
import org.bibletranslationtools.logger.Logger
import org.bibletranslationtools.writer.BaseIntegrationTest
import org.bibletranslationtools.writer.getHttpReporter
import org.bibletranslationtools.writer.usecases.UploadCrashReport
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.File

class UploadCrashReportTest : BaseIntegrationTest() {

    private lateinit var crashDir: File

    @Before
    fun setUp() {
        crashDir = runBlocking { directoryProvider.createTempDir("crashes") }
        Logger.registerGlobalExceptionHandler(crashDir)

        mockkStatic(::getHttpReporter)
        every { getHttpReporter(any(), any(), any()) } returns
            HttpReporter.noAuth(server.url("/issues").toString(), Context("test", "test"))
    }

    @After
    fun tearDown() {
        unmockkAll()
        runBlocking { directoryProvider.clearCache() }
    }

    @Test
    fun testUploadCrashReport() {
        createStackTraces()

        server.enqueue(MockResponse.Builder().body("{success: true}").code(200).build())

        val message = "Test crash report"
        val uploadCrashReport = UploadCrashReport(directoryProvider)
        val reported = runBlocking { uploadCrashReport.execute(message, "") }

        assertTrue("Upload success when response code 200", reported)

        val request = server.takeRequest().body!!.string(Charsets.UTF_8)

        assertTrue("Upload request body contains message", request.contains(message))
        assertTrue("Upload request body contains stacktrace", request.contains("This is a crash"))
        assertTrue("Upload request body contains environment", request.contains("Environment"))

        assertTrue(
            "Crash dir is empty after successful upload",
            crashDir.listFiles()?.isEmpty() ?: true
        )
    }

    @Test
    fun crashReportFailsWhenNoCrashes() {
        deleteStackTraces()

        server.enqueue(MockResponse.Builder().body("{success: true}").code(200).build())

        val message = "Test crash report"
        val uploadCrashReport = UploadCrashReport(directoryProvider)
        val reported = runBlocking { uploadCrashReport.execute(message, "") }

        assertFalse("Upload failed when no crash files", reported)
    }

    @Test
    fun testUploadCrashServerDown() {
        createStackTraces()

        server.enqueue(MockResponse.Builder().code(500).build())

        val message = "Test crash report"
        val uploadCrashReport = UploadCrashReport(directoryProvider)
        val reported = runBlocking { uploadCrashReport.execute(message, "") }

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
