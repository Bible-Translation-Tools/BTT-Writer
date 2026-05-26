package org.bibletranslationtools.writer.unit.ui.dialogs.download

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.runBlocking
import org.bibletranslationtools.resourcecatalog.library.models.Translation
import org.bibletranslationtools.resourcecontainer.Language
import org.bibletranslationtools.writer.ui.dialogs.download.DefaultDownloadSourcesComponent
import org.bibletranslationtools.writer.ui.dialogs.download.DownloadListItem
import org.bibletranslationtools.writer.ui.dialogs.download.FilterMode
import org.bibletranslationtools.writer.unit.ui.BaseComponentTest
import org.bibletranslationtools.writer.unit.ui.awaitState
import org.bibletranslationtools.writer.usecases.DownloadResourceContainers
import org.bibletranslationtools.writer.usecases.GetAvailableSources
import org.jetbrains.compose.resources.getString
import org.junit.Before
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.dsl.module
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DownloadSourcesComponentTest : BaseComponentTest() {

    private val getAvailableSources: GetAvailableSources = mockk(relaxed = true)
    private val downloadResourceContainers: DownloadResourceContainers = mockk(relaxed = true)

    @Before
    fun setUpComponent() {
        mockkStatic("org.jetbrains.compose.resources.StringResourcesKt")
        coEvery { getString(any()) } returns "Mock String"

        startKoin {
            modules(
                module {
                    single { getAvailableSources }
                    single { downloadResourceContainers }
                }
            )
        }
    }

    private fun createComponent(): DefaultDownloadSourcesComponent =
        createComponent { context ->
            DefaultDownloadSourcesComponent(
                componentContext = context
            )
        }

    @Test
    fun testInitializationLoadsSources() {
        runBlocking {
            val mockLanguage = mockk<Language>(relaxed = true) {
                every { name } returns "English"
                every { slug } returns "en"
            }
            val mockTranslation = mockk<Translation>(relaxed = true) {
                every { language } returns mockLanguage
            }
            val mockResult = GetAvailableSources.Result(
                sources = listOf(mockTranslation),
                byLanguage = mapOf("en" to listOf(0)),
                otBooks = emptyMap(),
                ntBooks = emptyMap(),
                otherBooks = emptyMap()
            )
            coEvery { getAvailableSources.execute(any()) } returns mockResult

            val component = createComponent()
            component.state.awaitState { it.listItems.isNotEmpty() }

            // Verify filter category contains the English language item
            val listItems = component.state.value.listItems
            assertTrue(listItems.isNotEmpty())
            val firstCategory = listItems.first() as DownloadListItem.FilterCategory
            assertEquals("en", firstCategory.id)
            assertEquals("English (en)", firstCategory.title)
        }
    }

    @Test
    fun testFilterModeChange() {
        runBlocking {
            val component = createComponent()
            component.onFilterModeChanged(FilterMode.ByBook)
            component.state.awaitState { it.filterMode == FilterMode.ByBook }

            assertEquals(FilterMode.ByBook, component.state.value.filterMode)
        }
    }
}
