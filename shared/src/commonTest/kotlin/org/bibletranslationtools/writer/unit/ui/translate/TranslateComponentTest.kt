package org.bibletranslationtools.writer.unit.ui.translate

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.resume
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableSharedFlow
import org.bibletranslationtools.resourcecatalog.ResourceCatalogClient
import org.bibletranslationtools.writer.Platform
import org.bibletranslationtools.writer.core.TargetTranslation
import org.bibletranslationtools.writer.core.TranslationViewMode
import org.bibletranslationtools.writer.core.Translator
import org.bibletranslationtools.writer.data.Preference
import org.bibletranslationtools.writer.ui.navigation.RootComponent
import org.bibletranslationtools.writer.ui.translate.DefaultTranslateComponent
import org.bibletranslationtools.writer.ui.translate.TranslateComponent
import org.bibletranslationtools.writer.unit.ui.BaseComponentTest
import org.junit.Before
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.dsl.module
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TranslateComponentTest : BaseComponentTest() {

    private val translator: Translator = mockk(relaxed = true)
    private val preference: Preference = mockk(relaxed = true)
    private val catalogClient: ResourceCatalogClient = mockk(relaxed = true)
    private val platform: Platform = mockk(relaxed = true)

    private val sharedFlow = MutableSharedFlow<RootComponent.SharedEvent>()
    private var resultReceived: TranslateComponent.Result? = null

    private val mockTarget = mockk<TargetTranslation>(relaxed = true) {
        every { id } returns "target-1"
        every { targetLanguage.slug } returns "en"
        every { targetLanguageName } returns "English"
        every { projectId } returns "gen"
        every { sourceTranslations } returns listOf("reg")
        every { numTranslated } returns 5
    }

    @Before
    fun setUpComponent() {
        startKoin {
            modules(
                module {
                    single { translator }
                    single { preference }
                    single { catalogClient }
                    single { platform }
                }
            )
        }
        resultReceived = null
    }

    private fun createComponent(
        translationId: String = "target-1",
        initialViewMode: TranslationViewMode? = null,
        autoResume: Boolean = true
    ): Pair<DefaultTranslateComponent, LifecycleRegistry> {
        val lifecycle = LifecycleRegistry()
        val context = DefaultComponentContext(lifecycle)
        val component = DefaultTranslateComponent(
            componentContext = context,
            translationId = translationId,
            initialViewMode = initialViewMode,
            conflictFilterOn = false,
            sharedFlow = sharedFlow,
            onResult = { resultReceived = it }
        )
        if (autoResume) {
            lifecycle.resume()
        }
        return component to lifecycle
    }

    @Test
    fun testInitializationTranslationNotFound() {
        coEvery { translator.getTargetTranslation("unknown") } returns null

        createComponent(translationId = "unknown", autoResume = false)

        assertTrue(resultReceived is TranslateComponent.Result.Error)
    }

    @Test
    fun testInitializationSuccess() {
        coEvery { translator.getTargetTranslation("target-1") } returns mockTarget
        every { preference.getLastViewMode("target-1") } returns TranslationViewMode.CHUNK
        every { preference.getOpenSourceTranslations("target-1") } returns listOf("reg")

        val (component, _) = createComponent()

        // Active child should be Chunk mode
        val activeChild = component.stack.value.active.instance
        assertTrue(activeChild is TranslateComponent.Child.Chunk)
        assertEquals(TranslationViewMode.CHUNK, component.currentViewMode.value)
    }

    @Test
    fun testOpenReadMode() {
        coEvery { translator.getTargetTranslation("target-1") } returns mockTarget
        every { preference.getLastViewMode("target-1") } returns TranslationViewMode.CHUNK

        val (component, _) = createComponent()
        component.openReadMode()

        val activeChild = component.stack.value.active.instance
        assertTrue(activeChild is TranslateComponent.Child.Read)
        assertEquals(TranslationViewMode.READ, component.currentViewMode.value)
    }

    @Test
    fun testOpenChunkMode() {
        coEvery { translator.getTargetTranslation("target-1") } returns mockTarget
        every { preference.getLastViewMode("target-1") } returns TranslationViewMode.READ

        val (component, _) = createComponent()
        component.openChunkMode()

        val activeChild = component.stack.value.active.instance
        assertTrue(activeChild is TranslateComponent.Child.Chunk)
        assertEquals(TranslationViewMode.CHUNK, component.currentViewMode.value)
    }

    @Test
    fun testOpenReviewMode() {
        coEvery { translator.getTargetTranslation("target-1") } returns mockTarget
        every { preference.getLastViewMode("target-1") } returns TranslationViewMode.CHUNK

        val (component, _) = createComponent()
        component.openReviewMode(conflictFilterOn = true)

        val activeChild = component.stack.value.active.instance
        assertTrue(activeChild is TranslateComponent.Child.Review)
        assertEquals(TranslationViewMode.REVIEW, component.currentViewMode.value)
    }
}
