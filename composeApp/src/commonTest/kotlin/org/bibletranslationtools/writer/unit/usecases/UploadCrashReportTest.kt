package org.bibletranslationtools.writer.unit.usecases

import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.mockkConstructor
import io.mockk.mockkObject
import io.mockk.runs
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.bibletranslationtools.logger.GithubReporter
import org.bibletranslationtools.logger.Logger
import org.bibletranslationtools.writer.DirectoryProvider
import org.bibletranslationtools.writer.data.Preference
import org.bibletranslationtools.writer.usecases.UploadCrashReport
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.IOException

class UploadCrashReportTest {

    @MockK private lateinit var directoryProvider: DirectoryProvider
    @MockK private lateinit var preference: Preference

    @JvmField
    @Rule
    var tempDir: TemporaryFolder = TemporaryFolder()

    @Before
    fun setup() {
        MockKAnnotations.init(this)

        every { preference.getGithubBugReportRepo() }.returns("/github")

        every { directoryProvider.logFile }.returns(tempDir.newFile("test.log"))

        mockkObject(Logger)
        every { Logger.listStacktraces() }.returns(emptyList())
        every { Logger.flush() }.just(runs)

        mockkConstructor(GithubReporter::class)
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

        coEvery { anyConstructed<GithubReporter>().reportCrash(any(), any<File>(), any()) }
            .returns(true)

        val success = UploadCrashReport(
            directoryProvider,
            preference
        ).execute("test message")

        assertTrue(success)

        verify { preference.getGithubBugReportRepo() }
        coVerify { anyConstructed<GithubReporter>().reportCrash(any(), any<File>(), any()) }
        verify { Logger.flush() }
    }

    @Test
    fun `test upload crash report failed, server error`() = runTest {
        val stacktrace = tempDir.newFile("stacktrace.txt")
        every { Logger.listStacktraces() }.returns(listOf(stacktrace))

        coEvery { anyConstructed<GithubReporter>().reportCrash(any(), any<File>(), any()) }
            .returns(false)

        val success = UploadCrashReport(
            directoryProvider,
            preference
        ).execute("test message")

        assertFalse(success)

        verify { preference.getGithubBugReportRepo() }
        coVerify { anyConstructed<GithubReporter>().reportCrash(any(), any<File>(), any()) }
        verify(inverse = true) { Logger.flush() }
    }

    @Test
    fun `test upload crash report, no stack traces`() = runTest {
        val success = UploadCrashReport(
            directoryProvider,
            preference
        ).execute("test message")

        assertFalse(success)

        verify { preference.getGithubBugReportRepo() }
        coVerify(inverse = true) { anyConstructed<GithubReporter>().reportCrash(any(), any<File>(), any()) }
        verify(inverse = true) { Logger.flush() }
    }

    @Test
    fun `test upload crash report throws exception`() = runTest {
        val stacktrace = tempDir.newFile("stacktrace.txt")
        every { Logger.listStacktraces() }.returns(listOf(stacktrace))

        coEvery { anyConstructed<GithubReporter>().reportCrash(any(), any<File>(), any()) }
            .throws(IOException("An error occurred."))

        val success = UploadCrashReport(
            directoryProvider,
            preference
        ).execute("test message")

        assertFalse(success)

        verify { preference.getGithubBugReportRepo() }
        coVerify { anyConstructed<GithubReporter>().reportCrash(any(), any<File>(), any()) }
        verify(inverse = true) { Logger.flush() }
    }

    @Test
    fun `test upload crash report, no github token`() = runTest {
        val success = UploadCrashReport(
            directoryProvider,
            preference
        ).execute("test message")

        assertFalse(success)

        verify { preference.getGithubBugReportRepo() }
        coVerify(inverse = true) { anyConstructed<GithubReporter>().reportCrash(any(), any<File>(), any()) }
        verify(inverse = true) { Logger.flush() }
    }
}