package org.bibletranslationtools.writer.unit.ui.newtranslation

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.bibletranslationtools.resourcecatalog.ResourceCatalogClient
import org.bibletranslationtools.resourcecatalog.library.models.CategoryEntry
import org.bibletranslationtools.resourcecatalog.library.models.TargetLanguage
import org.bibletranslationtools.writer.Platform
import org.bibletranslationtools.writer.core.Profile
import org.bibletranslationtools.writer.core.TargetTranslation
import org.bibletranslationtools.writer.core.Translator
import org.bibletranslationtools.writer.data.Preference
import org.bibletranslationtools.writer.ui.newtranslation.DefaultNewTranslationComponent
import org.bibletranslationtools.writer.ui.newtranslation.NewTranslationComponent
import org.bibletranslationtools.writer.ui.newtranslation.ScreenStep
import org.bibletranslationtools.writer.unit.ui.BaseComponentTest
import org.bibletranslationtools.writer.unit.ui.awaitState
import org.bibletranslationtools.writer.usecases.MergeTargetTranslation
import org.junit.Before
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.dsl.module
import kotlin.test.assertEquals

class NewTranslationComponentTest : BaseComponentTest() {

    private val mergeTargetTranslation: MergeTargetTranslation = mockk(relaxed = true)
    private val preference: Preference = mockk(relaxed = true)
    private val catalogClient: ResourceCatalogClient = mockk(relaxed = true)
    private val translator: Translator = mockk(relaxed = true)
    private val profile: Profile = mockk(relaxed = true)
    private val platform: Platform = mockk(relaxed = true)

    private var resultReceived: NewTranslationComponent.Result? = null

    private val mockLangEn = mockk<TargetLanguage>(relaxed = true) {
        every { slug } returns "en"
        every { name } returns "English"
    }
    private val mockLangEs = mockk<TargetLanguage>(relaxed = true) {
        every { slug } returns "es"
        every { name } returns "Spanish"
    }

    private val mockCategory = mockk<CategoryEntry>(relaxed = true) {
        every { id } returns 101L
        every { slug } returns "gen"
        every { name } returns "Genesis"
    }

    @Before
    fun setUpComponent() {
        every { platform.deviceLanguageCode } returns "en"
        coEvery { catalogClient.library.getTargetLanguages() } returns listOf(mockLangEs, mockLangEn)
        every { catalogClient.library.getProjectCategories(any(), any(), any()) } returns listOf(mockCategory)
        every { preference.getPref(any(), any(), any()) } answers { args[1]!! }

        startKoin {
            modules(
                module {
                    single { mergeTargetTranslation }
                    single { preference }
                    single { catalogClient }
                    single { translator }
                    single { profile }
                    single { platform }
                }
            )
        }
        resultReceived = null
    }

    private fun createComponent(
        disabledLanguages: List<String> = emptyList(),
        translationId: String? = null
    ): DefaultNewTranslationComponent = createComponent { context ->
        DefaultNewTranslationComponent(
            componentContext = context,
            disabledLanguages = disabledLanguages,
            translationId = translationId,
            onResult = { resultReceived = it }
        )
    }

    @Test
    fun testInitializationLoadsLanguages() {
        runBlocking {
            val component = createComponent(disabledLanguages = listOf("fr"))
            component.state.awaitState { it.languages.isNotEmpty() }

            assertEquals(2, component.state.value.languages.size)
            // Sorted by slug: en, then es
            assertEquals("en", component.state.value.languages[0].slug)
            assertEquals("es", component.state.value.languages[1].slug)
            assertEquals(listOf("fr"), component.state.value.disabledLanguages)
        }
    }

    @Test
    fun testOnLanguageSelectedNewTranslation() {
        runBlocking {
            val component = createComponent()
            component.state.awaitState { it.languages.isNotEmpty() }

            component.onLanguageSelected(mockLangEn)

            // Should display projects screen step
            assertEquals(ScreenStep.PROJECT, component.state.value.screenStep)
            assertEquals(1, component.state.value.categories.size)
            assertEquals("gen", component.state.value.categories.first().slug)
            assertEquals(mockLangEn, component.selectedTargetLanguage)
        }
    }

    @Test
    fun testOnProjectSelectedSuccess() {
        runBlocking {
            val component = createComponent()
            component.state.awaitState { it.languages.isNotEmpty() }

            component.onLanguageSelected(mockLangEn)

            val mockTarget = mockk<TargetTranslation>(relaxed = true) {
                every { id } returns "en_gen_text_reg"
            }
            coEvery { translator.getTargetTranslation(any()) } returns null
            coEvery { translator.createTargetTranslation(any(), any(), any(), any(), any(), any()) } returns mockTarget

            component.onProjectSelected("gen")

            // Wait for onResult
            delayYield()

            assertEquals(NewTranslationComponent.Result.Success, resultReceived)
            coVerify { translator.createTargetTranslation(any(), mockLangEn, "gen", any(), any(), any()) }
        }
    }

    @Test
    fun testOnProjectSelectedDuplicate() {
        runBlocking {
            val component = createComponent()
            component.state.awaitState { it.languages.isNotEmpty() }

            component.onLanguageSelected(mockLangEn)

            val mockTarget = mockk<TargetTranslation>(relaxed = true) {
                every { id } returns "en_gen_text_reg"
            }
            coEvery { translator.getTargetTranslation("en_gen_text_reg") } returns mockTarget

            component.onProjectSelected("gen")

            delayYield()

            assertEquals(NewTranslationComponent.Result.Duplicate("en_gen_text_reg"), resultReceived)
        }
    }

    @Test
    fun testCategoryNavigation() {
        runBlocking {
            val component = createComponent()
            component.state.awaitState { it.languages.isNotEmpty() }

            component.onLanguageSelected(mockLangEn)

            assertEquals(listOf(0L), component.state.value.categoryStack)

            component.onCategorySelected(101L)
            assertEquals(listOf(0L, 101L), component.state.value.categoryStack)

            component.onCategoryBack()
            assertEquals(listOf(0L), component.state.value.categoryStack)
        }
    }

    @Test
    fun testSearchFiltering() {
        runBlocking {
            val component = createComponent()
            component.state.awaitState { it.languages.isNotEmpty() }

            component.onSearch("Spanish")
            assertEquals("Spanish", component.state.value.searchQuery)
            assertEquals(1, component.state.value.filteredLanguages.size)
            assertEquals("es", component.state.value.filteredLanguages.first().slug)
        }
    }

    @Test
    fun testNavigateBack() {
        val component = createComponent()
        component.navigateBack()
        assertEquals(NewTranslationComponent.Result.NavigateBack, resultReceived)
    }

    private suspend fun delayYield() {
        delay(50)
    }
}
