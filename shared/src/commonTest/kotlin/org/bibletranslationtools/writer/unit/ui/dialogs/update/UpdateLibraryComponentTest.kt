package org.bibletranslationtools.writer.unit.ui.dialogs.update

import io.github.vinceglb.filekit.PlatformFile
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.runBlocking
import org.bibletranslationtools.writer.ui.dialogs.update.DefaultUpdateLibraryComponent
import org.bibletranslationtools.writer.ui.dialogs.update.UpdateLibraryComponent
import org.bibletranslationtools.writer.unit.ui.BaseComponentTest
import org.bibletranslationtools.writer.unit.ui.awaitEvent
import org.bibletranslationtools.writer.unit.ui.awaitState
import org.bibletranslationtools.writer.usecases.CheckForLatestRelease
import org.bibletranslationtools.writer.usecases.ImportIndex
import org.bibletranslationtools.writer.usecases.UpdateCatalogs
import org.bibletranslationtools.writer.usecases.UpdateSource
import org.jetbrains.compose.resources.getString
import org.junit.Before
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.dsl.module
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class UpdateLibraryComponentTest : BaseComponentTest() {

    private val importIndex: ImportIndex = mockk(relaxed = true)
    private val updateCatalogs: UpdateCatalogs = mockk(relaxed = true)
    private val checkForLatestRelease: CheckForLatestRelease = mockk(relaxed = true)
    private val updateSource: UpdateSource = mockk(relaxed = true)

    private var resultReceived: UpdateLibraryComponent.Result? = null

    @Before
    fun setUpComponent() {
        mockkStatic("org.jetbrains.compose.resources.StringResourcesKt")
        coEvery { getString(any()) } returns "Mock String"
        coEvery { getString(any(), *anyVararg()) } returns "Mock String"

        startKoin {
            modules(
                module {
                    single { importIndex }
                    single { updateCatalogs }
                    single { checkForLatestRelease }
                    single { updateSource }
                }
            )
        }
        resultReceived = null
    }

    private fun createComponent(triggerUpdate: Boolean = false): DefaultUpdateLibraryComponent =
        createComponent { context ->
            DefaultUpdateLibraryComponent(
                componentContext = context,
                triggerUpdate = triggerUpdate,
                onResult = { resultReceived = it }
            )
        }

    @Test
    fun testOpenDownloadSources() {
        val component = createComponent()
        component.openDownloadSources()
        assertNotNull(resultReceived)
        assertTrue(resultReceived is UpdateLibraryComponent.Result.OpenDownloadSources)
    }

    @Test
    fun testUpdateSourcesSuccess() {
        runBlocking {
            val mockResult = UpdateSource.Result(success = true, updatedCount = 2, addedCount = 1)
            coEvery { updateSource.execute(any()) } returns mockResult

            val component = createComponent()
            component.updateSources()

            component.state.awaitState { it.updateSourceResult != null }
            assertEquals(mockResult, component.state.value.updateSourceResult)
        }
    }

    @Test
    fun testImportIndexSqliteSuccess() {
        runBlocking {
            coEvery { importIndex.import(any()) } returns true

            val tempFile = File.createTempFile("test", ".sqlite")
            tempFile.deleteOnExit()
            val platformFile = PlatformFile(tempFile)

            val component = createComponent()
            component.importIndex(platformFile)

            val event = component.event.awaitEvent { it is UpdateLibraryComponent.Event.IndexUpdated }
            assertNotNull(event)

            tempFile.delete()
        }
    }

    @Test
    fun testImportIndexNotSqlite() {
        runBlocking {
            val tempFile = File.createTempFile("test", ".txt")
            tempFile.deleteOnExit()
            val platformFile = PlatformFile(tempFile)

            val component = createComponent()
            component.importIndex(platformFile)

            component.state.awaitState { it.resultMessage != null }
            assertNotNull(component.state.value.resultMessage)
            coVerify(exactly = 0) { importIndex.import(any()) }

            tempFile.delete()
        }
    }

    @Test
    fun testDownloadIndexSuccess() {
        runBlocking {
            coEvery { importIndex.download(any()) } returns true

            val component = createComponent()
            component.downloadIndex()

            val event = component.event.awaitEvent { it is UpdateLibraryComponent.Event.IndexUpdated }
            assertNotNull(event)
        }
    }

    @Test
    fun testUpdateLanguagesSuccess() {
        runBlocking {
            val mockResult = UpdateCatalogs.Result(success = true, addedCount = 3)
            coEvery { updateCatalogs.execute(any(), any()) } returns mockResult

            val component = createComponent()
            component.updateLanguages()

            component.state.awaitState { it.resultMessage != null }
            assertNotNull(component.state.value.resultMessage)
        }
    }

    @Test
    fun testCheckAppUpdateWithRelease() {
        runBlocking {
            val mockRelease = CheckForLatestRelease.Release(
                name = "v1.0.0",
                downloadUrl = "http://download.url",
                downloadSize = 1024,
                build = 2
            )
            coEvery { checkForLatestRelease.execute() } returns CheckForLatestRelease.Result(mockRelease)

            val component = createComponent()
            component.checkAppUpdate()

            component.state.awaitState { it.latestRelease != null }
            assertEquals(mockRelease, component.state.value.latestRelease)
        }
    }

    @Test
    fun testClearMethods() {
        runBlocking {
            val mockRelease = CheckForLatestRelease.Release(
                name = "v1.0.0",
                downloadUrl = "http://download.url",
                downloadSize = 1024,
                build = 2
            )
            coEvery { checkForLatestRelease.execute() } returns CheckForLatestRelease.Result(mockRelease)

            val component = createComponent()
            component.checkAppUpdate()
            component.state.awaitState { it.latestRelease != null }

            component.clearLatestRelease()
            component.state.awaitState { it.latestRelease == null }
            assertEquals(component.state.value.latestRelease, null)
        }
    }
}
