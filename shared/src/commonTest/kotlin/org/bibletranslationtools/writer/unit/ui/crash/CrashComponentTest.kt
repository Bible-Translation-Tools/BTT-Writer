package org.bibletranslationtools.writer.unit.ui.crash

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.bibletranslationtools.writer.Platform
import org.bibletranslationtools.writer.ui.crash.CrashComponent
import org.bibletranslationtools.writer.ui.crash.DefaultCrashComponent
import org.bibletranslationtools.writer.unit.ui.BaseComponentTest
import org.bibletranslationtools.writer.unit.ui.awaitEvent
import org.bibletranslationtools.writer.unit.ui.awaitState
import org.bibletranslationtools.writer.usecases.CheckForLatestRelease
import org.bibletranslationtools.writer.usecases.UploadCrashReport
import org.junit.Before
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.dsl.module
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CrashComponentTest : BaseComponentTest() {

    private val checkForLatestRelease: CheckForLatestRelease = mockk()
    private val uploadCrashReport: UploadCrashReport = mockk()
    private val platform: Platform = mockk()

    private var resultReceived: CrashComponent.Result? = null

    @Before
    fun setUpComponent() {
        startKoin {
            modules(
                module {
                    single { checkForLatestRelease }
                    single { uploadCrashReport }
                    single { platform }
                }
            )
        }
        resultReceived = null
    }

    private fun createComponent(): DefaultCrashComponent = createComponent { context ->
        DefaultCrashComponent(
            componentContext = context,
            onResult = { resultReceived = it }
        )
    }

    @Test
    fun testInitialState() {
        val component = createComponent()
        val state = component.state.value

        assertEquals("", state.notes)
        assertEquals("", state.email)
        assertEquals(false, state.success)
        assertNull(state.release)
        assertNull(component.progress.value)
    }

    @Test
    fun testSendCrashReportValidationFailure() = runBlocking {
        val component = createComponent()

        val eventDeferred = async {
            component.event.awaitEvent { it is CrashComponent.Event.SnackbarMessage }
        }

        component.sendCrashReport(notes = "", email = "test@example.com")

        val event = eventDeferred.await()
        assertTrue(event is CrashComponent.Event.SnackbarMessage)
    }

    @Test
    fun testSendCrashReportNewReleaseAvailable() = runBlocking {
        val testRelease = CheckForLatestRelease.Release("v2.0", "http://download.url", 1024, 2)
        coEvery { checkForLatestRelease.execute() } returns CheckForLatestRelease.Result(testRelease)

        val component = createComponent()
        component.sendCrashReport(notes = "App crashed on startup", email = "test@example.com")

        component.state.awaitState { it.release == testRelease }

        val state = component.state.value
        assertEquals("App crashed on startup", state.notes)
        assertEquals("test@example.com", state.email)
        assertEquals(testRelease, state.release)
        assertEquals(false, state.success)
    }

    @Test
    fun testSendCrashReportSuccessNoNewRelease() = runBlocking {
        coEvery { checkForLatestRelease.execute() } returns CheckForLatestRelease.Result(null)
        coEvery { uploadCrashReport.execute("App crashed on startup", "test@example.com") } returns true

        val component = createComponent()
        component.sendCrashReport(notes = "App crashed on startup", email = "test@example.com")

        component.state.awaitState { it.success }

        val state = component.state.value
        assertEquals("App crashed on startup", state.notes)
        assertEquals("test@example.com", state.email)
        assertNull(state.release)
        assertEquals(true, state.success)
    }

    @Test
    fun testSendCrashReportFailureNoNewRelease() = runBlocking {
        coEvery { checkForLatestRelease.execute() } returns CheckForLatestRelease.Result(null)
        coEvery { uploadCrashReport.execute(any(), any()) } returns false

        val component = createComponent()

        val eventDeferred = async {
            component.event.awaitEvent { it is CrashComponent.Event.UploadError }
        }

        component.sendCrashReport(notes = "App crashed on startup", email = "test@example.com")

        val event = eventDeferred.await()
        assertTrue(event is CrashComponent.Event.UploadError)

        val state = component.state.value
        assertEquals(false, state.success)
    }

    @Test
    fun testFlushAndRestart() {
        val component = createComponent()
        component.flushAndRestart()

        assertEquals(CrashComponent.Result.Restart, resultReceived)
    }

    @Test
    fun testClearLatestRelease() = runBlocking {
        val testRelease = CheckForLatestRelease.Release("v2.0", "http://download.url", 1024, 2)
        coEvery { checkForLatestRelease.execute() } returns CheckForLatestRelease.Result(testRelease)

        val component = createComponent()
        component.sendCrashReport(notes = "Crash notes", email = "")

        component.state.awaitState { it.release != null }

        component.clearLatestRelease()
        assertNull(component.state.value.release)
    }
}
