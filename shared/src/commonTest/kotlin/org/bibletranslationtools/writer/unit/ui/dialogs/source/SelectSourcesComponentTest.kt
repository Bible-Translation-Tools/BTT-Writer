package org.bibletranslationtools.writer.unit.ui.dialogs.source

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.runBlocking
import org.bibletranslationtools.resourcecatalog.ResourceCatalogClient
import org.bibletranslationtools.resourcecatalog.library.models.Translation
import org.bibletranslationtools.writer.core.TargetTranslation
import org.bibletranslationtools.writer.core.Translator
import org.bibletranslationtools.writer.data.Preference
import org.bibletranslationtools.writer.ui.dialogs.source.DefaultSelectSourcesComponent
import org.bibletranslationtools.writer.ui.dialogs.source.SelectSourcesComponent
import org.bibletranslationtools.writer.unit.ui.BaseComponentTest
import org.bibletranslationtools.writer.unit.ui.awaitState
import org.bibletranslationtools.writer.usecases.DownloadResourceContainers
import org.jetbrains.compose.resources.getString
import org.junit.Before
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.dsl.module
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SelectSourcesComponentTest : BaseComponentTest() {

    private val translator: Translator = mockk(relaxed = true)
    private val preference: Preference = mockk(relaxed = true)
    private val catalogClient: ResourceCatalogClient = mockk(relaxed = true)
    private val downloadResourceContainers: DownloadResourceContainers = mockk(relaxed = true)

    private var resultReceived: SelectSourcesComponent.Result? = null

    private val mockTarget = mockk<TargetTranslation>(relaxed = true) {
        every { id } returns "target-1"
        every { projectId } returns "gen"
    }

    @Before
    fun setUpComponent() {
        mockkStatic("org.jetbrains.compose.resources.StringResourcesKt")
        coEvery { getString(any()) } returns "Mock String"
        coEvery { getString(any(), *anyVararg()) } returns "Mock String"

        coEvery { translator.getTargetTranslation("target-1") } returns mockTarget

        startKoin {
            modules(
                module {
                    single { translator }
                    single { preference }
                    single { catalogClient }
                    single { downloadResourceContainers }
                }
            )
        }
        resultReceived = null
    }

    private fun createComponent(translationId: String = "target-1"): DefaultSelectSourcesComponent =
        createComponent { context ->
            DefaultSelectSourcesComponent(
                componentContext = context,
                translationId = translationId,
                onResult = { resultReceived = it }
            )
        }

    @Test
    fun testInitializationLoadsSources() {
        runBlocking {
            every { preference.getOpenSourceTranslations("target-1") } returns listOf("en-ulb")
            val mockTranslation = mockk<Translation>(relaxed = true) {
                every { language.name } returns "English"
                every { language.slug } returns "en"
                every { resource.name } returns "Unlocked Literal Bible"
                every { resourceContainerSlug } returns "en-ulb"
            }
            coEvery { catalogClient.library.getTranslation("en-ulb") } returns mockTranslation
            coEvery { catalogClient.library.findTranslations(any(), any(), any(), any(), any(), any(), any()) } returns emptyList()
            coEvery { catalogClient.resourceContainerExists("en-ulb") } returns true

            val component = createComponent()
            component.state.awaitState { it.sources.isNotEmpty() }

            assertEquals(1, component.state.value.sources.size)
            val source = component.state.value.sources.first()
            assertEquals("en-ulb", source.containerSlug)
            assertTrue(source.selected)
            assertTrue(source.downloaded)
        }
    }

    @Test
    fun testInitializationTranslationNotFound() {
        runBlocking {
            coEvery { translator.getTargetTranslation("unknown") } returns null

            createComponent(translationId = "unknown")
            assertNotNull(resultReceived)
            assertTrue(resultReceived is SelectSourcesComponent.Result.Error)
        }
    }

    @Test
    fun testConfirmSources() {
        runBlocking {
            every { preference.getOpenSourceTranslations("target-1") } returns listOf("en-ulb")
            val mockTranslation = mockk<Translation>(relaxed = true) {
                every { language.name } returns "English"
                every { language.slug } returns "en"
                every { resource.name } returns "Unlocked Literal Bible"
                every { resourceContainerSlug } returns "en-ulb"
            }
            coEvery { catalogClient.library.getTranslation("en-ulb") } returns mockTranslation

            val component = createComponent()
            component.state.awaitState { it.sources.isNotEmpty() }

            component.onConfirmSources()
            assertNotNull(resultReceived)
            assertTrue(resultReceived is SelectSourcesComponent.Result.ConfirmedSources)
            val confirmed = (resultReceived as SelectSourcesComponent.Result.ConfirmedSources).sources
            assertTrue(confirmed.contains("en-ulb"))
        }
    }

    @Test
    fun testUpdateSources() {
        runBlocking {
            val component = createComponent()
            component.onUpdateSources()
            assertNotNull(resultReceived)
            assertTrue(resultReceived is SelectSourcesComponent.Result.UpdateSources)
        }
    }

    @Test
    fun testToggleSelection() {
        runBlocking {
            every { preference.getOpenSourceTranslations("target-1") } returns listOf("en-ulb")
            val mockTranslation = mockk<Translation>(relaxed = true) {
                every { language.name } returns "English"
                every { language.slug } returns "en"
                every { resource.name } returns "Unlocked Literal Bible"
                every { resourceContainerSlug } returns "en-ulb"
            }
            coEvery { catalogClient.library.getTranslation("en-ulb") } returns mockTranslation

            val component = createComponent()
            component.state.awaitState { it.sources.isNotEmpty() }

            val item = component.state.value.sources.first()
            assertTrue(item.selected)

            component.toggleSelection(item)
            component.state.awaitState { !it.sources.first().selected }
            assertTrue(!component.state.value.sources.first().selected)
        }
    }

    @Test
    fun testDeleteSource() {
        runBlocking {
            every { preference.getOpenSourceTranslations("target-1") } returns listOf("en-ulb")
            val mockTranslation = mockk<Translation>(relaxed = true) {
                every { language.name } returns "English"
                every { language.slug } returns "en"
                every { resource.name } returns "Unlocked Literal Bible"
                every { resourceContainerSlug } returns "en-ulb"
            }
            coEvery { catalogClient.library.getTranslation("en-ulb") } returns mockTranslation

            val component = createComponent()
            component.state.awaitState { it.sources.isNotEmpty() }

            val item = component.state.value.sources.first()
            component.deleteSource(item)

            coVerify { catalogClient.deleteResourceContainer("en-ulb") }
        }
    }
}
