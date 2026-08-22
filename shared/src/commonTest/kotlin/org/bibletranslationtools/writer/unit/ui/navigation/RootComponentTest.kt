package org.bibletranslationtools.writer.unit.ui.navigation

import io.github.vinceglb.filekit.PlatformFile
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.bibletranslationtools.logger.Logger
import org.bibletranslationtools.resourcecatalog.ResourceCatalogClient
import org.bibletranslationtools.writer.DirectoryProvider
import org.bibletranslationtools.writer.Platform
import org.bibletranslationtools.writer.core.Profile
import org.bibletranslationtools.writer.core.Translator
import org.bibletranslationtools.writer.data.Preference
import org.bibletranslationtools.writer.ui.navigation.DefaultRootComponent
import org.bibletranslationtools.writer.ui.navigation.RootComponent
import org.bibletranslationtools.writer.unit.ui.BaseComponentTest
import org.bibletranslationtools.writer.usecases.MigrateTranslations
import org.bibletranslationtools.writer.usecases.UpdateApp
import org.bibletranslationtools.writer.utils.RuntimeWrapper
import org.junit.Before
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.dsl.module
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RootComponentTest : BaseComponentTest() {

    private val preference: Preference = mockk(relaxed = true)
    private val platform: Platform = mockk(relaxed = true)
    private val directoryProvider: DirectoryProvider = mockk(relaxed = true)
    private val migrateTranslations: MigrateTranslations = mockk()
    private val updateApp: UpdateApp = mockk()
    private val profile: Profile = mockk(relaxed = true)
    private val translator: Translator = mockk(relaxed = true)
    private val catalogClient: ResourceCatalogClient = mockk(relaxed = true)

    private var appExited = false

    @Before
    fun setUpComponent() {
        mockkObject(RuntimeWrapper)
        mockkObject(Logger)

        every { RuntimeWrapper.availableProcessors } returns 4
        every { RuntimeWrapper.maxMemory } returns 1024L * 1024 * 1024
        every { Logger.listStacktraces() } returns emptyList()

        every { preference.getPref(any(), any(), any()) } answers { args[1]!! }

        startKoin {
            modules(
                module {
                    single { preference }
                    single { platform }
                    single { directoryProvider }
                    single { migrateTranslations }
                    single { updateApp }
                    single { profile }
                    single { translator }
                    single { catalogClient }
                }
            )
        }
        appExited = false
    }

    private fun createComponent(): DefaultRootComponent = createComponent { context ->
        DefaultRootComponent(
            componentContext = context,
            onExitApp = { appExited = true }
        )
    }

    @Test
    fun testInitialStackIsSplash() {
        val component = createComponent()
        val activeChild = component.stack.value.active.instance

        assertTrue(activeChild is RootComponent.Child.Splash)
    }

    @Test
    fun testNavigationToProfile() {
        val component = createComponent()
        component.openProfile(thenLogin = false)

        val activeChild = component.stack.value.active.instance
        assertTrue(activeChild is RootComponent.Child.Profile)
    }

    @Test
    fun testNavigationToTranslate() {
        val component = createComponent()
        component.openTranslate(translationId = "test_project", startWithMergeFilter = false)

        val activeChild = component.stack.value.active.instance
        assertTrue(activeChild is RootComponent.Child.Translate)
    }

    @Test
    fun testOnBackPressedPopsStack() {
        val component = createComponent()
        // Use openTranslate which uses bringToFront and pushes onto backstack
        component.openTranslate(translationId = "test_project", startWithMergeFilter = false)

        assertTrue(component.stack.value.active.instance is RootComponent.Child.Translate)

        component.onBackPressed()

        assertTrue(component.stack.value.active.instance is RootComponent.Child.Splash)
    }

    @Test
    fun testOnDeepLinkEmitsImportEvent() = runBlocking {
        val component = createComponent()
        val mockFile = mockk<PlatformFile>()

        val events = mutableListOf<RootComponent.SharedEvent>()
        val job = launch {
            component.sharedFlow.collect { events.add(it) }
        }
        // Yield to allow the collector to register
        delay(50)

        component.onDeepLink(mockFile)

        // Yield to allow emission processing
        delay(50)

        assertEquals(1, events.size)
        val event = events.first()
        assertTrue(event is RootComponent.SharedEvent.ImportProject)
        assertEquals(mockFile, event.file)

        job.cancel()
    }
}
