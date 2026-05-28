package org.bibletranslationtools.writer.unit.ui.publish

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.bibletranslationtools.resourcecatalog.ResourceCatalogClient
import org.bibletranslationtools.resourcecatalog.library.models.SourceLanguage
import org.bibletranslationtools.resourcecatalog.library.models.TargetLanguage
import org.bibletranslationtools.writer.Platform
import org.bibletranslationtools.writer.core.Profile
import org.bibletranslationtools.writer.core.TargetTranslation
import org.bibletranslationtools.writer.core.TranslationFormat
import org.bibletranslationtools.writer.core.Translator
import org.bibletranslationtools.writer.core.Validation
import org.bibletranslationtools.writer.data.Preference
import org.bibletranslationtools.writer.ui.publish.DefaultPublishComponent
import org.bibletranslationtools.writer.ui.publish.PublishComponent
import org.bibletranslationtools.writer.unit.ui.BaseComponentTest
import org.bibletranslationtools.writer.unit.ui.awaitState
import org.bibletranslationtools.writer.usecases.ValidateProject
import org.jetbrains.compose.resources.getString
import org.junit.Before
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.dsl.module
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PublishComponentTest : BaseComponentTest() {

    private val translator: Translator = mockk(relaxed = true)
    private val catalogClient: ResourceCatalogClient = mockk(relaxed = true)
    private val validateProject: ValidateProject = mockk(relaxed = true)
    private val profile: Profile = mockk(relaxed = true)
    private val platform: Platform = mockk(relaxed = true)
    private val preference: Preference = mockk(relaxed = true)

    private var resultReceived: PublishComponent.Result? = null

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
                    single { catalogClient }
                    single { validateProject }
                    single { profile }
                    single { platform }
                    single { preference }
                }
            )
        }
        resultReceived = null
    }

    private fun createComponent(translationId: String = "target-1"): DefaultPublishComponent =
        createComponent { context ->
            DefaultPublishComponent(
                componentContext = context,
                translationId = translationId,
                onResult = { resultReceived = it }
            )
        }

    @Test
    fun testInitializationTranslationNotFound() {
        runBlocking {
            coEvery { translator.getTargetTranslation("unknown") } returns null

            createComponent(translationId = "unknown")
            assertNotNull(resultReceived)
            assertTrue(resultReceived is PublishComponent.Result.Error)
        }
    }

    @Test
    fun testInitializationChooseSourceTranslationError() {
        runBlocking {
            every { preference.getSelectedSourceTranslationId("target-1") } returns null
            every { catalogClient.library.getProject(any(), any(), any()) } returns null

            createComponent()
            assertNotNull(resultReceived)
            assertTrue(resultReceived is PublishComponent.Result.Error)
        }
    }

    @Test
    fun testInitializationSuccess() {
        runBlocking {
            every { preference.getSelectedSourceTranslationId("target-1") } returns "en-ulb"
            coEvery { validateProject.execute("target-1", "en-ulb") } returns emptyList()

            val component = createComponent()
            component.state.awaitState { !it.isLoading }

            assertEquals(mockTarget, component.targetTranslation)
            assertTrue(component.state.value.validations.isEmpty())
        }
    }

    @Test
    fun testNavigateBack() {
        runBlocking {
            every { preference.getSelectedSourceTranslationId("target-1") } returns "en-ulb"

            val component = createComponent()
            component.navigateBack()

            assertNotNull(resultReceived)
            assertTrue(resultReceived is PublishComponent.Result.NavigateBack)
        }
    }

    @Test
    fun testOpenReview() {
        runBlocking {
            every { preference.getSelectedSourceTranslationId("target-1") } returns "en-ulb"

            val component = createComponent()
            val mockSourceLang = mockk<SourceLanguage>(relaxed = true)
            val mockTargetLang = mockk<TargetLanguage>(relaxed = true)
            val item = Validation.InvalidFrame(
                title = "Error Frame",
                titleLanguage = mockSourceLang,
                body = "Error detail",
                bodyLanguage = mockTargetLang,
                bodyFormat = TranslationFormat.USFM,
                targetTranslationId = "target-1",
                chapterId = "1",
                frameId = "1"
            )

            component.openReview(item)

            val start = System.currentTimeMillis()
            while (resultReceived == null) {
                check(System.currentTimeMillis() - start < 2000) { "Timeout waiting for openReview result" }
                delay(10)
            }

            coVerify { preference.setLastViewMode("target-1", org.bibletranslationtools.writer.core.TranslationViewMode.REVIEW) }
            coVerify { preference.setLastFocus("target-1", "1", "1") }
            assertNotNull(resultReceived)
            assertTrue(resultReceived is PublishComponent.Result.OpenReview)
        }
    }

    @Test
    fun testShowExportDialog() {
        runBlocking {
            every { preference.getSelectedSourceTranslationId("target-1") } returns "en-ulb"

            val component = createComponent()
            component.showExportDialog()

            val slot = component.dialogSlot.value
            assertNotNull(slot.child)
            assertTrue(slot.child!!.instance is PublishComponent.DialogChild.Export)
        }
    }

    @Test
    fun testDismissDialog() {
        runBlocking {
            every { preference.getSelectedSourceTranslationId("target-1") } returns "en-ulb"

            val component = createComponent()
            component.showExportDialog()
            assertNotNull(component.dialogSlot.value.child)

            component.dismissDialog()
            assertEquals(component.dialogSlot.value.child, null)
        }
    }
}
