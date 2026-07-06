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
import org.bibletranslationtools.logger.LogLevel
import org.bibletranslationtools.logger.Logger
import org.bibletranslationtools.writer.BaseIntegrationTest
import org.bibletranslationtools.writer.getHttpReporter
import org.bibletranslationtools.writer.usecases.UploadFeedback
import org.junit.After
import org.junit.Before
import org.junit.Test

class UploadFeedbackTest : BaseIntegrationTest() {

    @Before
    fun setUp() {
        Logger.configure(directoryProvider.logFile, LogLevel.getLevel(0))

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
    fun testUploadFeedback() {
        server.enqueue(MockResponse.Builder().body("{success: true}").code(200).build())

        Logger.i("UploadFeedbackTest", "This is an info log.")
        Logger.w("UploadFeedbackTest", "This is a warning log.")
        Logger.e("UploadFeedbackTest", "This is an error log.")

        assertTrue(
            "Log file should not be empty",
            directoryProvider.logFile.length() > 0
        )

        val uploadFeedback = UploadFeedback(directoryProvider)
        val notes = "This is a test note"
        val uploaded = runBlocking { uploadFeedback.execute(notes, "") }

        assertTrue("Feedback should be uploaded", uploaded)

        val request = server.takeRequest().body!!.string(Charsets.UTF_8)

        assertTrue("Upload request body contains message", request.contains(notes))
        assertTrue("Upload request body contains info log", request.contains("This is an info log."))
        assertTrue("Upload request body contains warning log", request.contains("This is a warning log."))
        assertTrue("Upload request body contains error log", request.contains("This is an error log."))
        assertFalse(
            "Log file should not contain old logs after successful upload",
            directoryProvider.logFile.readText().contains("This is an error log.")
        )
        assertTrue(
            "Log file should contain new log message",
            directoryProvider.logFile.readText().contains("Submitted bug report")
        )
    }

    @Test
    fun testUploadFailsOnServerDown() {
        server.enqueue(MockResponse.Builder().body("{success: true}").code(500).build())

        Logger.i("UploadFeedbackTest", "This is an info log.")
        Logger.w("UploadFeedbackTest", "This is a warning log.")
        Logger.e("UploadFeedbackTest", "This is an error log.")

        assertTrue(
            "Log file should not be empty",
            directoryProvider.logFile.length() > 0
        )

        val uploadFeedback = UploadFeedback(directoryProvider)
        val notes = "This is a test note"
        val uploaded = runBlocking { uploadFeedback.execute(notes, "") }

        assertFalse("Upload should be failed", uploaded)
        assertTrue(
            "Log file should remain after failed upload",
            directoryProvider.logFile.length() > 0
        )
    }
}
