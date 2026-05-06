package org.bibletranslationtools.writer.unit.usecases

import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.bibletranslationtools.logger.GithubReporter
import org.bibletranslationtools.logger.Logger
import org.bibletranslationtools.writer.DirectoryProvider
import org.bibletranslationtools.writer.data.Preference
import org.bibletranslationtools.writer.usecases.UploadFeedback
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.IOException

class UploadFeedbackTest {

    @MockK private lateinit var preference: Preference
    @MockK private lateinit var directoryProvider: DirectoryProvider

    @JvmField
    @Rule
    var tempDir: TemporaryFolder = TemporaryFolder()

    @Before
    fun setup() {
        MockKAnnotations.init(this)

        every { preference.getGithubBugReportRepo() }.returns("/github")

        mockkStatic(Logger::class)
        every { directoryProvider.logFile }.returns(tempDir.newFile("test.log"))

        mockkConstructor(GithubReporter::class)
    }

    @After
    fun tearDown() {
        unmockkAll()
        tempDir.delete()
    }

    @Test
    fun `test upload feedback successfully`() = runTest {
        coEvery { anyConstructed<GithubReporter>().reportBug(any(), any<File>()) }
            .returns(true)

        val success = UploadFeedback(
            preference,
            directoryProvider
        ).execute("Notes")

        assertTrue(success)

        verify { preference.getGithubBugReportRepo() }
        coVerify { anyConstructed<GithubReporter>().reportBug(any(), any<File>()) }
    }

    @Test
    fun `test upload feedback failed, server error`() = runTest {
        coEvery { anyConstructed<GithubReporter>().reportBug(any(), any<File>()) }
            .returns(false)

        val success = UploadFeedback(
            preference,
            directoryProvider
        ).execute("Notes")

        assertFalse(success)

        verify { preference.getGithubBugReportRepo() }
        coVerify { anyConstructed<GithubReporter>().reportBug(any(), any<File>()) }
    }

    @Test
    fun `test upload feedback throws exception`() = runTest {
        coEvery { anyConstructed<GithubReporter>().reportBug(any(), any<File>()) }
            .throws(IOException("An error occurred."))

        val success = UploadFeedback(
            preference,
            directoryProvider
        ).execute("Notes")

        assertFalse(success)

        verify { preference.getGithubBugReportRepo() }
        coVerify { anyConstructed<GithubReporter>().reportBug(any(), any<File>()) }
    }

    @Test
    fun `test upload feedback, no github token`() = runTest {
        val success = UploadFeedback(
            preference,
            directoryProvider
        ).execute("Notes")

        assertFalse(success)

        verify { preference.getGithubBugReportRepo() }
        coVerify(inverse = true) { anyConstructed<GithubReporter>().reportBug(any(), any<File>()) }
    }
}