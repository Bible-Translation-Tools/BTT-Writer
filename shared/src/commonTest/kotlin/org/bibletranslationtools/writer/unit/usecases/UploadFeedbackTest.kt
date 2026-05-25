package org.bibletranslationtools.writer.unit.usecases

import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.bibletranslationtools.logger.HttpReporter
import org.bibletranslationtools.logger.Logger
import org.bibletranslationtools.logger.ReporterError
import org.bibletranslationtools.writer.DirectoryProvider
import org.bibletranslationtools.writer.getHttpReporter
import org.bibletranslationtools.writer.usecases.UploadFeedback
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class UploadFeedbackTest {

    @MockK private lateinit var directoryProvider: DirectoryProvider

    private lateinit var mockReporter: HttpReporter

    @JvmField
    @Rule
    var tempDir: TemporaryFolder = TemporaryFolder()

    @Before
    fun setup() {
        MockKAnnotations.init(this)

        every { directoryProvider.logFile }.returns(tempDir.newFile("test.log"))

        mockkObject(Logger)
        every { Logger.i(any(), any()) }.just(runs)
        every { Logger.w(any(), any()) }.just(runs)
        every { Logger.w(any(), any(), any()) }.just(runs)

        mockReporter = mockk()
        mockkStatic(::getHttpReporter)
        every { getHttpReporter(any(), any(), any()) } returns mockReporter
    }

    @After
    fun tearDown() {
        unmockkAll()
        tempDir.delete()
    }

    @Test
    fun `test upload feedback successfully`() = runTest {
        coEvery { mockReporter.reportBug(any(), any<File>()) }.returns(true)

        val success = UploadFeedback(directoryProvider).execute("Notes", "")

        assertTrue(success)

        coVerify { mockReporter.reportBug(any(), any<File>()) }
    }

    @Test
    fun `test upload feedback failed, server error`() = runTest {
        coEvery { mockReporter.reportBug(any(), any<File>()) }.returns(false)
        every { mockReporter.getLastResponse() }.returns(ReporterError(500, "Internal Server Error"))

        val success = UploadFeedback(directoryProvider).execute("Notes", "")

        assertFalse(success)

        coVerify { mockReporter.reportBug(any(), any<File>()) }
    }

    @Test
    fun `test upload feedback network error`() = runTest {
        coEvery { mockReporter.reportBug(any(), any<File>()) }.returns(false)
        every { mockReporter.getLastResponse() }.returns(ReporterError(-1, "Connection refused"))

        val success = UploadFeedback(directoryProvider).execute("Notes", "")

        assertFalse(success)

        coVerify { mockReporter.reportBug(any(), any<File>()) }
    }
}
