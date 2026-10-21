package org.bibletranslationtools.writer.unit.ui.devtools

import com.arkivanov.essenty.backhandler.BackDispatcher
import io.mockk.every
import io.mockk.mockk
import org.bibletranslationtools.resourcecatalog.ResourceCatalogClient
import org.bibletranslationtools.resourcecatalog.library.models.TargetLanguage
import org.bibletranslationtools.writer.ui.devtools.DefaultEditLibraryComponent
import org.bibletranslationtools.writer.ui.devtools.EditLibraryComponent
import org.bibletranslationtools.writer.unit.ui.BaseComponentTest
import org.bibletranslationtools.writer.usecases.UpdateTargetLanguage
import org.junit.Before
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.dsl.module
import kotlin.test.assertEquals
import kotlin.test.assertNull

class EditLibraryComponentTest : BaseComponentTest() {

    private val catalogClient: ResourceCatalogClient = mockk(relaxed = true)
    private val updateTargetLanguage: UpdateTargetLanguage = mockk(relaxed = true)
    private val language = TargetLanguage("aa", "Afar", "ltr")

    private var resultReceived: EditLibraryComponent.Result? = null

    @Before
    fun setUpComponent() {
        every { catalogClient.library.getTargetLanguages() } returns listOf(language)
        startKoin {
            modules(module {
                single { catalogClient }
                single { updateTargetLanguage }
            })
        }
    }

    private fun createComponent() = createComponent { context ->
        DefaultEditLibraryComponent(context) { resultReceived = it }
    }

    @Test
    fun systemBackReturnsFromFormToList() {
        val component = createComponent()
        val dispatcher = component.backHandler as BackDispatcher

        component.onLanguageSelected(language)
        assertEquals(language, component.state.value.selected)

        assertEquals(true, dispatcher.back())
        assertNull(component.state.value.selected)
        assertNull(resultReceived)

        // nothing selected: the screen itself gets popped by the router
        assertEquals(false, dispatcher.back())
    }
}
