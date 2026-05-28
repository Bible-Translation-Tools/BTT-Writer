package org.bibletranslationtools.writer.unit.ui.draft

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.bibletranslationtools.resourcecatalog.ResourceCatalogClient
import org.bibletranslationtools.resourcecontainer.ResourceContainer
import org.bibletranslationtools.writer.core.TargetTranslation
import org.bibletranslationtools.writer.core.Translator
import org.bibletranslationtools.writer.rendering.RenderingProvider
import org.bibletranslationtools.writer.ui.draft.DefaultDraftComponent
import org.bibletranslationtools.writer.ui.draft.DraftComponent
import org.bibletranslationtools.writer.unit.ui.BaseComponentTest
import org.bibletranslationtools.writer.unit.ui.awaitState
import org.bibletranslationtools.writer.usecases.ImportDraft
import org.junit.Before
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.dsl.module
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class DraftComponentTest : BaseComponentTest() {

    private val translator: Translator = mockk(relaxed = true)
    private val catalogClient: ResourceCatalogClient = mockk(relaxed = true)
    private val importDraft: ImportDraft = mockk(relaxed = true)

    private var resultReceived: DraftComponent.Result? = null

    private val mockTarget = mockk<TargetTranslation>(relaxed = true) {
        every { id } returns "target-1"
        every { targetLanguage.slug } returns "en"
        every { projectId } returns "gen"
    }

    private val mockResource = mockk<org.bibletranslationtools.resourcecontainer.Resource>(relaxed = true) {
        every { slug } returns "reg"
    }

    private val mockCatalogTranslation = mockk<org.bibletranslationtools.resourcecatalog.library.models.Translation>(relaxed = true) {
        every { resource } returns mockResource
    }

    @Before
    fun setUpComponent() {
        coEvery { translator.getTargetTranslation("target-1") } returns mockTarget
        every {
            catalogClient.library.findTranslations(
                any(), any(), any(), any(), any(), any(), any()
            )
        } returns listOf(mockCatalogTranslation)

        startKoin {
            modules(
                module {
                    single { translator }
                    single { catalogClient }
                    single { importDraft }
                }
            )
        }
        resultReceived = null
    }

    private fun createComponent(): DefaultDraftComponent =
        createComponent { componentContext ->
            DefaultDraftComponent(
                componentContext = componentContext,
                translationId = "target-1",
                onResult = { resultReceived = it }
            )
        }

    @Test
    fun testInitializationLoadsDraftTranslations() {
        runBlocking {
            val component = createComponent()
            component.state.awaitState { it.draftTranslations.isNotEmpty() }

            assertEquals(1, component.state.value.draftTranslations.size)
            assertEquals("reg", component.state.value.draftTranslations.first().resource.slug)
        }
    }

    @Test
    fun testNavigateBack() {
        val component = createComponent()
        component.onNavigateBack()
        assertEquals(DraftComponent.Result.NavigateBack, resultReceived)
    }

    @Test
    fun testOnFinish() {
        val component = createComponent()
        component.onFinish()
        assertEquals(DraftComponent.Result.NavigateBack, resultReceived)
    }

    @Test
    fun testGetResourceContainer() {
        val component = createComponent()
        val mockContainer = mockk<ResourceContainer>(relaxed = true)
        every { catalogClient.openResourceContainer("en_gen") } returns mockContainer

        val result = component.getResourceContainer("en_gen")
        assertNotNull(result)
        assertEquals(mockContainer, result)
    }

    @Test
    fun testImportDraft() {
        runBlocking {
            val component = createComponent()
            val mockContainer = mockk<ResourceContainer>(relaxed = true)
            val mockImportResult = mockk<ImportDraft.Result>(relaxed = true)

            coEvery { importDraft.execute(mockContainer, any()) } returns mockImportResult

            component.importDraft(mockContainer)

            component.state.awaitState { it.importResult != null }
            assertEquals(mockImportResult, component.state.value.importResult)
        }
    }

    @Test
    fun testParseChapterContent() {
        runBlocking {
            val component = createComponent()
            val mockContainer = mockk<ResourceContainer>(relaxed = true) {
                every { readChunk("1", "title") } returns "Genesis 1"
                every { chunks("1") } returns listOf("01", "02")
                every { readChunk("1", "01") } returns "In the beginning "
                every { readChunk("1", "02") } returns "God created the heavens."
                every { info.contentMimeType } returns "text/usfm"
            }
            val mockRenderingProvider = mockk<RenderingProvider>(relaxed = true)

            val chapterContent = component.parseChapterContent("1", mockContainer, mockRenderingProvider)

            assertEquals("Genesis 1", chapterContent.title)
            assertEquals(1, chapterContent.renderNodes.size)
        }
    }
}
