package org.bibletranslationtools.writer.unit.ui.dialogs.import

import io.github.vinceglb.filekit.PlatformFile
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.runBlocking
import org.bibletranslationtools.resourcecatalog.ResourceCatalogClient
import org.bibletranslationtools.resourcecatalog.library.models.TargetLanguage
import org.bibletranslationtools.writer.Platform
import org.bibletranslationtools.writer.core.ProcessUSFM
import org.bibletranslationtools.writer.core.Translator
import org.bibletranslationtools.writer.ui.dialogs.import.DefaultImportUsfmComponent
import org.bibletranslationtools.writer.ui.dialogs.import.ImportUsfmComponent
import org.bibletranslationtools.writer.ui.dialogs.import.UsfmStep
import org.bibletranslationtools.writer.unit.ui.BaseComponentTest
import org.bibletranslationtools.writer.unit.ui.awaitState
import org.bibletranslationtools.writer.usecases.ImportProjects
import org.jetbrains.compose.resources.getString
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.dsl.module
import kotlin.test.assertEquals

class ImportUsfmComponentTest : BaseComponentTest() {

    private val translator: Translator = mockk(relaxed = true)
    private val importProjects: ImportProjects = mockk(relaxed = true)
    private val catalogClient: ResourceCatalogClient = mockk(relaxed = true)
    private val processUSFM: ProcessUSFM = mockk(relaxed = true)
    private val platform: Platform = mockk(relaxed = true)

    private lateinit var testFile: java.io.File
    private lateinit var platformFile: PlatformFile
    private var resultReceived: ImportUsfmComponent.Result? = null

    @Before
    fun setUpComponent() {
        mockkStatic("org.jetbrains.compose.resources.StringResourcesKt")
        coEvery { getString(any()) } returns "Mock String"

        testFile = java.io.File.createTempFile("genesis", ".usfm")
        testFile.deleteOnExit()
        platformFile = PlatformFile(testFile)

        startKoin {
            modules(
                module {
                    single { translator }
                    single { importProjects }
                    single { catalogClient }
                    single { processUSFM }
                    single { platform }
                }
            )
        }
        resultReceived = null
    }

    @After
    fun tearDownComponent() {
        if (::testFile.isInitialized && testFile.exists()) {
            testFile.delete()
        }
    }

    private fun createComponent(): DefaultImportUsfmComponent =
        createComponent { context ->
            DefaultImportUsfmComponent(
                componentContext = context,
                file = platformFile,
                onResult = { resultReceived = it }
            )
        }

    @Test
    fun testInitializationLoadsLanguages() {
        runBlocking {
            val mockTargetLanguage = mockk<TargetLanguage>(relaxed = true) {
                every { name } returns "English"
                every { slug } returns "en"
            }
            coEvery { catalogClient.library.getTargetLanguages() } returns listOf(mockTargetLanguage)

            val component = createComponent()
            component.state.awaitState { it.languages.isNotEmpty() }

            assertEquals(UsfmStep.LANGUAGE, component.state.value.step)
            assertEquals(1, component.state.value.languages.size)
            assertEquals("en", component.state.value.languages.first().slug)
        }
    }
}
