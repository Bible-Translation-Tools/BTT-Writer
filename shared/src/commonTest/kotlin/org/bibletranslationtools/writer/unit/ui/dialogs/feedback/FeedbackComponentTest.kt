package org.bibletranslationtools.writer.unit.ui.dialogs.feedback

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.bibletranslationtools.writer.Platform
import org.bibletranslationtools.writer.ui.dialogs.feedback.DefaultFeedbackComponent
import org.bibletranslationtools.writer.unit.ui.BaseComponentTest
import org.bibletranslationtools.writer.usecases.CheckForLatestRelease
import org.bibletranslationtools.writer.usecases.UploadFeedback
import org.jetbrains.compose.resources.getString
import org.junit.Before
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.dsl.module
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FeedbackComponentTest : BaseComponentTest() {

    private val checkForLatestRelease: CheckForLatestRelease = mockk(relaxed = true)
    private val uploadFeedback: UploadFeedback = mockk(relaxed = true)
    private val platform: Platform = mockk(relaxed = true)

    @Before
    fun setUpComponent() {
        mockkStatic("org.jetbrains.compose.resources.StringResourcesKt")
        coEvery { getString(any()) } returns "Mock String"

        startKoin {
            modules(
                module {
                    single { checkForLatestRelease }
                    single { uploadFeedback }
                    single { platform }
                }
            )
        }
    }

    private fun createComponent(): DefaultFeedbackComponent =
        createComponent { context ->
            DefaultFeedbackComponent(
                componentContext = context,
                initialMessage = "Initial Error Message"
            )
        }

    @Test
    fun testInitialization() {
        val component = createComponent()
        assertEquals("Initial Error Message", component.initialMessage)
    }

    @Test
    fun testReportBug() {
        runBlocking {
            val mockCheckResult = CheckForLatestRelease.Result(release = null)
            coEvery { checkForLatestRelease.execute() } returns mockCheckResult
            coEvery { uploadFeedback.execute(any(), any()) } returns true

            val component = createComponent()
            component.reportBug("Notes from user", "user@example.com")

            // Wait for coroutines running on Dispatchers.IO to finish
            delay(150)

            coVerify { checkForLatestRelease.execute() }
            coVerify { uploadFeedback.execute("Notes from user", "user@example.com") }
            assertTrue(component.state.value.success)
        }
    }

    @Test
    fun testClearError() {
        val component = createComponent()
        component.clearError()
        assertNull(component.state.value.uploadError)
    }
}
