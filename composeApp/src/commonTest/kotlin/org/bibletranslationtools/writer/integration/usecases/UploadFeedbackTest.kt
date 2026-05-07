package org.bibletranslationtools.writer.integration.usecases

import io.mockk.every
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.bibletranslationtools.logger.LogLevel
import org.bibletranslationtools.logger.Logger
import org.bibletranslationtools.writer.BaseIntegrationTest
import org.bibletranslationtools.writer.data.Preference
import org.bibletranslationtools.writer.usecases.UploadFeedback
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.koin.core.component.inject

class UploadFeedbackTest : BaseIntegrationTest() {

    private val preference: Preference by inject()

    private val server = MockWebServer()


    @Before
    fun setUp() {
        Logger.configure(directoryProvider.logFile, LogLevel.getLevel(0))

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
    fun testUploadFeedback() = runTest {
        server.enqueue(MockResponse().setBody("{success: true}").setResponseCode(200))

        // create some logs
        Logger.i("UploadFeedbackTest", "This is an info log.")
        Logger.w("UploadFeedbackTest", "This is a warning log.")
        Logger.e("UploadFeedbackTest", "This is an error log.")

        assertTrue(
            "Log file should not be empty",
            directoryProvider.logFile.length() > 0
        )

        val uploadFeedback = UploadFeedback(preference, directoryProvider)
        val notes = "This is a test note"
        val uploaded = uploadFeedback.execute(notes)

        assertTrue("Feedback should be uploaded", uploaded)

        val request = server.takeRequest().body.readString(Charsets.UTF_8)

        assertTrue(
            "Upload request body contains message",
            request.contains(notes)
        )
        assertTrue(
            "Upload request body contains info log",
            request.contains("This is an info log.")
        )
        assertTrue(
            "Upload request body contains warning log",
            request.contains("This is a warning log.")
        )
        assertTrue(
            "Upload request body contains error log",
            request.contains("This is an error log.")
        )
        assertFalse(
            "Log file should not contain old logs after successful upload",
            directoryProvider.logFile.readText().contains("This is an error log.")
        )
        assertTrue(
            "Log file should not contain new log message",
            directoryProvider.logFile.readText().contains("Submitted bug report"))
    }

    @Test
    fun testUploadFailsOnServerDown() = runTest {
        server.enqueue(MockResponse().setBody("{success: true}").setResponseCode(500))

        // create some logs
        Logger.i("UploadFeedbackTest", "This is an info log.")
        Logger.w("UploadFeedbackTest", "This is a warning log.")
        Logger.e("UploadFeedbackTest", "This is an error log.")

        assertTrue(
            "Log file should not be empty",
            directoryProvider.logFile.length() > 0
        )

        val uploadFeedback = UploadFeedback(preference, directoryProvider)
        val notes = "This is a test note"
        val uploaded = uploadFeedback.execute(notes)

        assertFalse("Upload should be failed", uploaded)
        assertTrue(
            "Log file should remain after failed upload",
            directoryProvider.logFile.length() > 0
        )
    }
}
