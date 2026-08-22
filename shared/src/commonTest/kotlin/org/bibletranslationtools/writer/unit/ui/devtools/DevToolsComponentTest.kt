package org.bibletranslationtools.writer.unit.ui.devtools

import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.bibletranslationtools.logger.LogEntry
import org.bibletranslationtools.logger.LogLevel
import org.bibletranslationtools.logger.Logger
import org.bibletranslationtools.resourcecatalog.ResourceCatalogClient
import org.bibletranslationtools.writer.DirectoryProvider
import org.bibletranslationtools.writer.Platform
import org.bibletranslationtools.writer.data.Preference
import org.bibletranslationtools.writer.ui.devtools.DefaultDevToolsComponent
import org.bibletranslationtools.writer.ui.devtools.DevToolsComponent
import org.bibletranslationtools.writer.unit.ui.BaseComponentTest
import org.junit.Before
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.dsl.module
import java.util.Date
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class DevToolsComponentTest : BaseComponentTest() {

    private val directoryProvider: DirectoryProvider = mockk(relaxed = true)
    private val catalogClient: ResourceCatalogClient = mockk(relaxed = true)
    private val platform: Platform = mockk(relaxed = true)
    private val preference: Preference = mockk(relaxed = true)

    private var resultReceived: DevToolsComponent.Result? = null

    @Before
    fun setUpComponent() {
        mockkObject(Logger)
        mockkObject(LogLevel.Companion)

        every { platform.info.versionName } returns "1.0.0"
        every { platform.info.versionCode } returns 10
        every { platform.udid } returns "test-device-id"
        every { platform.calculateSystemResources() } returns "CPU: 4, RAM: 8GB"
        every { preference.getPref(any(), any(), any()) } answers { args[1]!! }

        startKoin {
            modules(
                module {
                    single { directoryProvider }
                    single { catalogClient }
                    single { platform }
                    single { preference }
                }
            )
        }
        resultReceived = null
    }

    private fun createComponent(): DefaultDevToolsComponent = createComponent { context ->
        DefaultDevToolsComponent(
            componentContext = context,
            onResult = { resultReceived = it }
        )
    }

    @Test
    fun testInitialProperties() {
        val component = createComponent()
        assertEquals("1.0.0", component.versionName)
        assertEquals(10, component.versionCode)
        assertEquals("test-device-id", component.udid)
        assertEquals("CPU: 4, RAM: 8GB", component.calculateSystemResources())
    }

    @Test
    fun testLoadTools() {
        runBlocking {
            val component = createComponent()
            assertEquals(0, component.state.value.tools.size)

            component.loadTools()

            delayYield()

            assertEquals(5, component.state.value.tools.size)
            assertEquals("Regenerate SSH keys", component.state.value.tools[0].name)
            assertEquals("Read debugging log", component.state.value.tools[1].name)
        }
    }

    @Test
    fun testReadErrorLog() {
        runBlocking {
            val logEntries = listOf(
                LogEntry(Date(), LogLevel.Info, "test tag", "info message", ""),
                LogEntry(Date(), LogLevel.Warning, "test tag", "warning message", "")
            )
            every { Logger.getLogEntries() } returns logEntries
            every { preference.getPref(Preference.KEY_PREF_LOGGING_LEVEL, LogLevel.Info.name, String::class) } returns LogLevel.Warning.name
            every { LogLevel.getLevel(any<String>()) } returns LogLevel.Warning

            val component = createComponent()
            component.readErrorLog()

            delayYield()

            val logsInState = component.state.value.logs
            assertEquals(1, logsInState.size)
            assertEquals("warning message", logsInState.first().message)
        }
    }

    @Test
    fun testNavigateBack() {
        val component = createComponent()
        component.navigateBack()
        assertEquals(DevToolsComponent.Result.NavigateBack, resultReceived)
    }

    @Test
    fun testSimulateCrashThrowsException() {
        runBlocking {
            val component = createComponent()
            component.loadTools()
            delayYield()

            val simulateCrashItem = component.state.value.tools.first { it.name == "Simulate crash" }
            assertFailsWith<IllegalStateException> {
                simulateCrashItem.action()
            }
        }
    }

    @Test
    fun testClearKeysRegenerated() {
        runBlocking {
            val component = createComponent()
            component.loadTools()
            delayYield()

            val regenerateItem = component.state.value.tools.first { it.name == "Regenerate SSH keys" }
            regenerateItem.action()

            delayYield()
            assertEquals(true, component.state.value.keysRegenerated)

            component.clearKeysRegenerated()
            assertNull(component.state.value.keysRegenerated)

            coVerify { directoryProvider.generateSSHKeys("test-device-id") }
        }
    }

    @Test
    fun testDeleteLibrary() {
        runBlocking {
            val component = createComponent()
            component.loadTools()
            delayYield()

            val deleteLibraryItem = component.state.value.tools.first { it.name == "Delete Library" }
            deleteLibraryItem.action()

            delayYield()

            coVerify { catalogClient.closeLibrary() }
            coVerify { directoryProvider.deleteLibrary() }
            coVerify { directoryProvider.deployDefaultLibrary() }
            coVerify { catalogClient.openLibrary() }
        }
    }

    private suspend fun delayYield() {
        delay(50)
    }
}
