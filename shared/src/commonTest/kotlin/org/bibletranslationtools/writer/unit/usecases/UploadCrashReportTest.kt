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
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.bibletranslationtools.logger.HttpReporter
import org.bibletranslationtools.logger.Logger
import org.bibletranslationtools.logger.ReporterError
import org.bibletranslationtools.writer.DirectoryProvider
import org.bibletranslationtools.writer.getHttpReporter
import org.bibletranslationtools.writer.usecases.UploadCrashReport
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class UploadCrashReportTest {

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
        every { Logger.listStacktraces() }.returns(emptyList())
        every { Logger.flush() }.just(runs)
        every { Logger.e(any(), any()) }.just(runs)

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
    fun `test upload crash report successfully`() = runTest {
        val stacktrace = tempDir.newFile("stacktrace.txt")
        every { Logger.listStacktraces() }.returns(listOf(stacktrace))

        coEvery { mockReporter.reportCrash(any(), any<File>(), any()) }.returns(true)

        val success = UploadCrashReport(directoryProvider).execute("test message", "")

        assertTrue(success)

        coVerify { mockReporter.reportCrash(any(), any<File>(), any()) }
        verify { Logger.flush() }
    }

    @Test
    fun `test upload crash report failed, server error`() = runTest {
        val stacktrace = tempDir.newFile("stacktrace.txt")
        every { Logger.listStacktraces() }.returns(listOf(stacktrace))

        coEvery { mockReporter.reportCrash(any(), any<File>(), any()) }.returns(false)
        every { mockReporter.getLastResponse() }.returns(ReporterError(500, "Internal Server Error"))

        val success = UploadCrashReport(directoryProvider).execute("test message", "")

        assertFalse(success)

        coVerify { mockReporter.reportCrash(any(), any<File>(), any()) }
        verify(inverse = true) { Logger.flush() }
    }

    @Test
    fun `test upload crash report, no stack traces`() = runTest {
        val success = UploadCrashReport(directoryProvider).execute("test message", "")

        assertFalse(success)

        coVerify(inverse = true) { mockReporter.reportCrash(any(), any<File>(), any()) }
        verify(inverse = true) { Logger.flush() }
    }

    @Test
    fun `test upload crash report network error`() = runTest {
        val stacktrace = tempDir.newFile("stacktrace.txt")
        every { Logger.listStacktraces() }.returns(listOf(stacktrace))

        coEvery { mockReporter.reportCrash(any(), any<File>(), any()) }.returns(false)
        every { mockReporter.getLastResponse() }.returns(ReporterError(-1, "Connection refused"))

        val success = UploadCrashReport(directoryProvider).execute("test message", "")

        assertFalse(success)

        coVerify { mockReporter.reportCrash(any(), any<File>(), any()) }
        verify(inverse = true) { Logger.flush() }
    }
}
