package org.bibletranslationtools.writer.unit.ui.splash

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.verify
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.bibletranslationtools.logger.Logger
import org.bibletranslationtools.writer.data.Preference
import org.bibletranslationtools.writer.ui.splash.DefaultSplashComponent
import org.bibletranslationtools.writer.ui.splash.SplashComponent
import org.bibletranslationtools.writer.unit.ui.BaseComponentTest
import org.bibletranslationtools.writer.usecases.MigrateTranslations
import org.bibletranslationtools.writer.usecases.UpdateApp
import org.bibletranslationtools.writer.utils.RuntimeWrapper
import org.junit.Before
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.dsl.module
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SplashComponentTest : BaseComponentTest() {

    private val preference: Preference = mockk(relaxed = true)
    private val migrateTranslations: MigrateTranslations = mockk()
    private val updateApp: UpdateApp = mockk()

    private var resultReceived: SplashComponent.Result? = null

    @Before
    fun setUpComponent() {
        mockkObject(RuntimeWrapper)
        mockkObject(Logger)

        // Default: passes hardware requirements
        every { RuntimeWrapper.availableProcessors } returns 4
        every { RuntimeWrapper.maxMemory } returns 1024L * 1024 * 1024 // 1GB
        every { Logger.listStacktraces() } returns emptyList()

        // Avoid ClassCastException on generic getPref
        every { preference.getPref(any(), any(), any()) } answers { args[1]!! }

        startKoin {
            modules(
                module {
                    single { preference }
                    single { migrateTranslations }
                    single { updateApp }
                }
            )
        }
        resultReceived = null
    }

    private fun createComponent(): DefaultSplashComponent = createComponent { context ->
        DefaultSplashComponent(
            componentContext = context,
            onResult = { resultReceived = it }
        )
    }

    private suspend fun awaitResult(timeoutMs: Long = 1000): SplashComponent.Result {
        val start = System.currentTimeMillis()
        while (resultReceived == null) {
            if (System.currentTimeMillis() - start > timeoutMs) {
                error("Timeout waiting for SplashComponent result")
            }
            delay(10)
        }
        return resultReceived!!
    }

    @Test
    fun testHardwareWarningTriggered() {
        // Mock hardware requirements failing (1 processor)
        every { RuntimeWrapper.availableProcessors } returns 1
        every { preference.getPref(Preference.KEY_PREF_CHECK_HARDWARE, true, Boolean::class) } returns true

        val component = createComponent()

        assertTrue(component.state.value.showHardwareWarning)
        assertFalse(component.state.value.showMigrationDialog)
    }

    @Test
    fun testHardwareWarningPassedByPreference() {
        // Even if processors are 1, check_hardware is disabled, so warning should not display
        every { RuntimeWrapper.availableProcessors } returns 1
        every { preference.getPref(Preference.KEY_PREF_CHECK_HARDWARE, true, Boolean::class) } returns false
        every { preference.getPref(Preference.KEY_PREF_MIGRATE_OLD_APP, false, Boolean::class) } returns false

        val component = createComponent()

        assertFalse(component.state.value.showHardwareWarning)
        assertTrue(component.state.value.showMigrationDialog)
    }

    @Test
    fun testMigrationDialogTriggered() {
        every { preference.getPref(Preference.KEY_PREF_CHECK_HARDWARE, true, Boolean::class) } returns true
        every { preference.getPref(Preference.KEY_PREF_MIGRATE_OLD_APP, false, Boolean::class) } returns false

        val component = createComponent()

        assertFalse(component.state.value.showHardwareWarning)
        assertTrue(component.state.value.showMigrationDialog)
    }

    @Test
    fun testNavigateToCrashReporterIfCrashesExist() {
        // Hardware warning passed, migration already shown, stacktraces exist
        every { preference.getPref(Preference.KEY_PREF_MIGRATE_OLD_APP, false, Boolean::class) } returns true
        every { Logger.listStacktraces() } returns listOf(File("dummy.stacktrace"))

        createComponent()

        assertEquals(SplashComponent.Result.NavigateToCrashReporter, resultReceived)
    }

    @Test
    fun testUpdateAppAndNavigateToProfileOnSuccess() = runBlocking {
        every { preference.getPref(Preference.KEY_PREF_MIGRATE_OLD_APP, false, Boolean::class) } returns true
        every { Logger.listStacktraces() } returns emptyList()

        coEvery { updateApp.execute(any()) } returns Unit

        createComponent()

        val result = awaitResult()
        assertEquals(SplashComponent.Result.NavigateToProfile, result)
        coVerify { updateApp.execute(any()) }
    }

    @Test
    fun testOnHardwareWarningDismissedAndSaved() {
        every { RuntimeWrapper.availableProcessors } returns 1
        every { preference.getPref(Preference.KEY_PREF_CHECK_HARDWARE, true, Boolean::class) } returns true
        every { preference.getPref(Preference.KEY_PREF_MIGRATE_OLD_APP, false, Boolean::class) } returns false

        val component = createComponent()
        assertTrue(component.state.value.showHardwareWarning)

        component.onHardwareWarningDismissedAndSaved()

        assertFalse(component.state.value.showHardwareWarning)
        verify { preference.setPref(Preference.KEY_PREF_CHECK_HARDWARE, false, Boolean::class) }
        assertTrue(component.state.value.showMigrationDialog)
    }

    @Test
    fun testOnMigrationDeclined() = runBlocking {
        every { preference.getPref(Preference.KEY_PREF_MIGRATE_OLD_APP, false, Boolean::class) } returns false
        coEvery { updateApp.execute(any()) } returns Unit

        val component = createComponent()
        assertTrue(component.state.value.showMigrationDialog)

        component.onMigrationDeclined()

        assertFalse(component.state.value.showMigrationDialog)
        val result = awaitResult()
        assertEquals(SplashComponent.Result.NavigateToProfile, result)
    }
}
