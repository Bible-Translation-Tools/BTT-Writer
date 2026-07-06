package org.bibletranslationtools.writer.unit.ui.home

import io.github.vinceglb.filekit.PlatformFile
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.bibletranslationtools.resourcecatalog.ResourceCatalogClient
import org.bibletranslationtools.resourcecatalog.library.models.Translation
import org.bibletranslationtools.resourcecontainer.Project
import org.bibletranslationtools.writer.DirectoryProvider
import org.bibletranslationtools.writer.Platform
import org.bibletranslationtools.writer.core.Profile
import org.bibletranslationtools.writer.core.TargetTranslation
import org.bibletranslationtools.writer.core.Translator
import org.bibletranslationtools.writer.data.Preference
import org.bibletranslationtools.writer.data.getPref
import org.bibletranslationtools.writer.data.setPref
import org.bibletranslationtools.writer.ui.dialogs.export.ExportComponent
import org.bibletranslationtools.writer.ui.dialogs.import.ImportComponent
import org.bibletranslationtools.writer.ui.dialogs.import.ImportUsfmComponent
import org.bibletranslationtools.writer.ui.dialogs.update.UpdateLibraryComponent
import org.bibletranslationtools.writer.ui.home.BookSort
import org.bibletranslationtools.writer.ui.home.DefaultHomeComponent
import org.bibletranslationtools.writer.ui.home.HomeComponent
import org.bibletranslationtools.writer.ui.home.ProjectSort
import org.bibletranslationtools.writer.ui.home.TranslationItem
import org.bibletranslationtools.writer.ui.navigation.RootComponent
import org.bibletranslationtools.writer.unit.ui.BaseComponentTest
import org.bibletranslationtools.writer.unit.ui.awaitEvent
import org.bibletranslationtools.writer.unit.ui.awaitState
import org.bibletranslationtools.writer.usecases.BackupRC
import org.bibletranslationtools.writer.usecases.DownloadResourceContainers
import org.bibletranslationtools.writer.usecases.GetAvailableSources
import org.bibletranslationtools.writer.usecases.GogsLogout
import org.bibletranslationtools.writer.usecases.TranslationProgress
import org.bibletranslationtools.writer.usecases.UpdateSource
import org.jetbrains.compose.resources.getString
import org.junit.Before
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.dsl.module
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HomeComponentTest : BaseComponentTest() {

    private val translator: Translator = mockk(relaxed = true)
    private val preference: Preference = mockk(relaxed = true)
    private val calculateProgress: TranslationProgress = mockk(relaxed = true)
    private val profile: Profile = mockk(relaxed = true)
    private val gogsLogout: GogsLogout = mockk(relaxed = true)
    private val backupRC: BackupRC = mockk(relaxed = true)
    private val catalogClient: ResourceCatalogClient = mockk(relaxed = true)
    private val platform: Platform = mockk(relaxed = true)
    private val getAvailableSources: GetAvailableSources = mockk(relaxed = true)
    private val downloadResourceContainers: DownloadResourceContainers = mockk(relaxed = true)
    private val directoryProvider: DirectoryProvider = mockk(relaxed = true)
    private val updateSource: UpdateSource = mockk(relaxed = true)

    private val sharedFlow = MutableSharedFlow<RootComponent.SharedEvent>()
    private var resultReceived: HomeComponent.Result? = null

    private val mockProject = mockk<Project>(relaxed = true) {
        every { name } returns "Genesis"
    }
    private val mockTranslation = mockk<Translation>(relaxed = true) {
        every { project } returns mockProject
    }

    @Before
    fun setUpComponent() {
        mockkStatic("org.jetbrains.compose.resources.StringResourcesKt")
        coEvery { getString(any()) } returns "Mock String"
        coEvery { getString(any(), *anyVararg()) } returns "Mock String"

        every { preference.getPref(any(), any(), any()) } answers { args[1]!! }
        every { catalogClient.library.getTranslation(any()) } returns mockTranslation
        every { catalogClient.library.getProject(any(), any(), any()) } returns mockProject

        startKoin {
            modules(
                module {
                    single { translator }
                    single { preference }
                    single { calculateProgress }
                    single { profile }
                    single { gogsLogout }
                    single { backupRC }
                    single { catalogClient }
                    single { platform }
                    single { getAvailableSources }
                    single { downloadResourceContainers }
                    single { directoryProvider }
                    single { updateSource }
                }
            )
        }
        resultReceived = null
    }

    private fun createComponent(): DefaultHomeComponent = createComponent { context ->
        DefaultHomeComponent(
            componentContext = context,
            sharedFlow = sharedFlow,
            onResult = { resultReceived = it }
        )
    }

    @Test
    fun testInitializationLoadsPreferencesAndProjects() {
        runBlocking {
            every { translator.lastFocusTargetTranslation } returns null
            every { preference.getPref("sort_by_project", 0) } returns ProjectSort.LanguageThenProject.ordinal
            every { preference.getPref("sort_by_book", 0) } returns BookSort.Alphabetical.ordinal

            val mockTarget = mockk<TargetTranslation>(relaxed = true) {
                every { id } returns "target-1"
                every { projectId } returns "gen"
                every { targetLanguageName } returns "English"
                every { resourceSlug } returns "reg"
            }
            coEvery { translator.getTargetTranslations() } returns listOf(mockTarget)
            coEvery { calculateProgress.execute(mockTarget) } returns 0.5f

            val component = createComponent()

            // Wait for projects list to load in state
            component.state.awaitState { it.translations.isNotEmpty() }

            assertEquals(ProjectSort.LanguageThenProject, component.state.value.projectSort)
            assertEquals(BookSort.Alphabetical, component.state.value.bookSort)
            assertEquals(1, component.state.value.translations.size)
            assertEquals("target-1", component.state.value.translations.first().translation.id)
            assertEquals(0.5f, component.state.value.translations.first().progress)
        }
    }

    @Test
    fun testLastOpenedProjectAutoOpens() {
        runBlocking {
            every { translator.lastFocusTargetTranslation } returns "last-opened-id"
            val mockTarget = mockk<TargetTranslation>(relaxed = true) {
                every { id } returns "last-opened-id"
            }
            coEvery { translator.getTargetTranslation("last-opened-id") } returns mockTarget

            createComponent()

            // Yield coroutines to trigger getLastOpened logic in init
            delayYield()

            assertEquals(HomeComponent.Result.OpenProject("last-opened-id", false), resultReceived)
        }
    }

    @Test
    fun testDeleteProject() {
        runBlocking {
            every { translator.lastFocusTargetTranslation } returns null
            val mockTarget = mockk<TargetTranslation>(relaxed = true) {
                every { id } returns "target-to-delete"
                every { projectId } returns "gen"
            }
            coEvery { translator.getTargetTranslations() } returns listOf(mockTarget)

            val component = createComponent()
            component.state.awaitState { it.translations.isNotEmpty() }

            val itemToDelete = component.state.value.translations.first()
            component.deleteProject(itemToDelete)

            // Wait for translations to be empty
            component.state.awaitState { it.translations.isEmpty() }

            coVerify { backupRC.backupTargetTranslation(mockTarget, false) }
            coVerify { translator.deleteTargetTranslation("target-to-delete") }
            coVerify { preference.clearTargetTranslationSettings("target-to-delete") }
        }
    }

    @Test
    fun testChangeSorting() {
        runBlocking {
            every { translator.lastFocusTargetTranslation } returns null
            val component = createComponent()

            component.changeProjectSort(ProjectSort.ProgressThenProject)
            assertEquals(ProjectSort.ProgressThenProject, component.state.value.projectSort)
            verify { preference.setPref("sort_by_project", ProjectSort.ProgressThenProject.ordinal) }

            component.changeBookSort(BookSort.Alphabetical)
            assertEquals(BookSort.Alphabetical, component.state.value.bookSort)
            verify { preference.setPref("sort_by_book", BookSort.Alphabetical.ordinal) }
        }
    }

    @Test
    fun testDialogNavigation() {
        runBlocking {
            every { translator.lastFocusTargetTranslation } returns null
            val component = createComponent()

            assertNull(component.dialogSlot.value.child)

            // Feedback
            component.showFeedbackDialog()
            assertTrue(component.dialogSlot.value.child?.instance is HomeComponent.DialogChild.Feedback)

            // Dismiss
            component.dismissDialog()
            assertNull(component.dialogSlot.value.child)

            // Export
            component.showExportDialog("target-1", true)
            assertTrue(component.dialogSlot.value.child?.instance is HomeComponent.DialogChild.Export)

            // Dismiss again
            component.dismissDialog()
            assertNull(component.dialogSlot.value.child)
        }
    }

    @Test
    fun testLogout() {
        runBlocking {
            every { translator.lastFocusTargetTranslation } returns null
            val component = createComponent()

            component.logout()

            // Wait for Logout result
            delayYield()

            coVerify { gogsLogout.execute() }
            coVerify { profile.logout() }
            assertEquals(HomeComponent.Result.Logout, resultReceived)
        }
    }

    @Test
    fun testSharedFlowEvents() {
        runBlocking {
            every { translator.lastFocusTargetTranslation } returns null
            val component = createComponent()

            val job = launch {
                sharedFlow.emit(RootComponent.SharedEvent.SnackbarMessage("Hello World"))
            }

            val event = component.event.awaitEvent { it is HomeComponent.Event.SnackbarMessage }
            assertEquals("Hello World", (event as HomeComponent.Event.SnackbarMessage).message)

            job.cancel()
        }
    }

    @Test
    fun testProjectInfoState() {
        runBlocking {
            every { translator.lastFocusTargetTranslation } returns null
            val component = createComponent()
            val mockItem = mockk<TranslationItem>(relaxed = true)

            assertNull(component.state.value.projectInfo)
            component.showProjectInfo(mockItem)
            assertEquals(mockItem, component.state.value.projectInfo)

            component.hideProjectInfo()
            assertNull(component.state.value.projectInfo)
        }
    }

    @Test
    fun testLoadWithProgress() {
        runBlocking {
            every { translator.lastFocusTargetTranslation } returns null
            val mockTarget = mockk<TargetTranslation>(relaxed = true) {
                every { id } returns "target-1"
                every { projectId } returns "gen"
                every { targetLanguageName } returns "English"
            }
            coEvery { translator.getTargetTranslation("target-1") } returns mockTarget
            coEvery { calculateProgress.execute(mockTarget) } returns 0.75f

            val component = createComponent()
            component.loadWithProgress(listOf("target-1"))

            component.state.awaitState { it.translations.any { item -> item.translation.id == "target-1" } }
            val loaded = component.state.value.translations.first { it.translation.id == "target-1" }
            assertEquals(0.75f, loaded.progress)
        }
    }

    @Test
    fun testDirectCallbacks() {
        runBlocking {
            var lastFocusTranslation: String? = null
            every { translator.lastFocusTargetTranslation } answers { lastFocusTranslation }
            every { translator.lastFocusTargetTranslation = any() } answers { lastFocusTranslation = firstArg() }

            val component = createComponent()

            component.onNewTranslation()
            assertEquals(HomeComponent.Result.OpenNewTranslation, resultReceived)

            component.onChangeTranslationLanguage(listOf("en"), "target-1")
            assertEquals(HomeComponent.Result.ChangeTranslationLanguage(listOf("en"), "target-1"), resultReceived)

            component.openSettings()
            assertEquals(HomeComponent.Result.OpenSettings, resultReceived)

            component.publishProject("target-1")
            assertEquals(HomeComponent.Result.PublishProject("target-1"), resultReceived)

            component.openProject("target-1", true)
            assertEquals(HomeComponent.Result.OpenProject("target-1", true), resultReceived)
            assertEquals("target-1", component.lastFocusTargetTranslation)

            component.exitApp()
            assertEquals(HomeComponent.Result.ExitApp, resultReceived)

            component.openLogin()
            assertEquals(HomeComponent.Result.OpenLogin, resultReceived)
        }
    }

    @Test
    fun testDialogsActivation() {
        runBlocking {
            every { translator.lastFocusTargetTranslation } returns null
            val component = createComponent()
            val mockFile = mockk<PlatformFile>(relaxed = true)

            assertNull(component.dialogSlot.value.child)

            // Import
            component.importProject(mockFile)
            assertTrue(component.dialogSlot.value.child?.instance is HomeComponent.DialogChild.Import)

            component.dismissDialog()
            assertNull(component.dialogSlot.value.child)

            // Update library
            component.requestUpdateLibrary()
            assertTrue(component.dialogSlot.value.child?.instance is HomeComponent.DialogChild.UpdateLibrary)
        }
    }

    @Test
    fun testShareAndExportToApp() {
        runBlocking {
            every { translator.lastFocusTargetTranslation } returns null
            val component = createComponent()

            component.shareApp()
            // Wait for coroutine inside shareApp to run
            delayYield()
            verify { platform.shareApp() }

            val mockFile = File("dummy")
            component.exportToApp(mockFile)
            verify { platform.shareProject(mockFile) }
        }
    }

    @Test
    fun testBackCallbackTriggersExitDialog() {
        runBlocking {
            every { translator.lastFocusTargetTranslation } returns null
            val component = createComponent { context ->
                DefaultHomeComponent(
                    componentContext = context,
                    sharedFlow = sharedFlow,
                    onResult = { resultReceived = it }
                )
            }

            val dispatcher = component.backHandler as? com.arkivanov.essenty.backhandler.BackDispatcher
            dispatcher?.back()

            val event = component.event.awaitEvent { it is HomeComponent.Event.ShowExitDialog }
            assertEquals(HomeComponent.Event.ShowExitDialog, event)
        }
    }

    @Test
    fun testDuplicateProjectEvent() {
        runBlocking {
            every { translator.lastFocusTargetTranslation } returns null
            val mockTarget = mockk<TargetTranslation>(relaxed = true) {
                every { id } returns "target-1"
                every { targetLanguageName } returns "English"
                every { projectId } returns "gen"
            }
            coEvery { translator.getTargetTranslation("target-1") } returns mockTarget
            val mockProj = mockk<Project>(relaxed = true) {
                every { name } returns "Genesis"
            }
            every { catalogClient.library.getTranslation(any()) } returns null
            every { catalogClient.library.getProject("English", "gen", true) } returns mockProj

            val component = createComponent()

            val job = launch {
                sharedFlow.emit(RootComponent.SharedEvent.DuplicateProject("target-1"))
            }

            val event = component.event.awaitEvent { it is HomeComponent.Event.SnackbarMessage }
            assertTrue((event as HomeComponent.Event.SnackbarMessage).message.contains("Mock String"))

            job.cancel()
        }
    }

    @Test
    fun testOnImportResultMergeConflict() {
        runBlocking {
            every { translator.lastFocusTargetTranslation } returns null
            val component = createComponent()

            // MergeConflict should call openProject(result.translationId, true)
            invokePrivateMethod(component, "onImportResult", ImportComponent.Result.MergeConflict("target-1"))

            assertEquals(HomeComponent.Result.OpenProject("target-1", true), resultReceived)
        }
    }

    @Test
    fun testOnImportResultProjectsImported() {
        runBlocking {
            every { translator.lastFocusTargetTranslation } returns null
            
            val mockTarget = mockk<TargetTranslation>(relaxed = true) {
                every { id } returns "target-1"
                every { projectId } returns "gen"
                every { targetLanguageName } returns "English"
            }
            coEvery { translator.getTargetTranslations() } returns listOf(mockTarget)
            coEvery { translator.getTargetTranslation("target-1") } returns mockTarget

            val component = createComponent()

            // ProjectsImported should reload translations
            invokePrivateMethod(component, "onImportResult", ImportComponent.Result.ProjectsImported(listOf("target-1")))

            component.state.awaitState { it.translations.any { item -> item.translation.id == "target-1" } }
            assertTrue(component.state.value.translations.any { it.translation.id == "target-1" })
        }
    }

    @Test
    fun testOnImportResultOpenUsfmImport() {
        runBlocking {
            every { translator.lastFocusTargetTranslation } returns null
            val component = createComponent()
            val mockFile = mockk<PlatformFile>(relaxed = true)

            // OpenUsfmImport should activate ImportUsfm child dialog
            invokePrivateMethod(component, "onImportResult", ImportComponent.Result.OpenUsfmImport(mockFile))

            assertTrue(component.dialogSlot.value.child?.instance is HomeComponent.DialogChild.ImportUsfm)
        }
    }

    @Test
    fun testOnUpdateLibraryResult() {
        runBlocking {
            every { translator.lastFocusTargetTranslation } returns null
            val component = createComponent()

            // OpenDownloadSources should activate DownloadSources child dialog
            invokePrivateMethod(component, "onUpdateLibraryResult", UpdateLibraryComponent.Result.OpenDownloadSources)

            assertTrue(component.dialogSlot.value.child?.instance is HomeComponent.DialogChild.DownloadSources)
        }
    }

    @Test
    fun testOnImportUsfmResult() {
        runBlocking {
            every { translator.lastFocusTargetTranslation } returns null
            val component = createComponent()
            val mockTarget = mockk<TargetTranslation>(relaxed = true) {
                every { id } returns "target-1"
                every { projectId } returns "gen"
                every { targetLanguageName } returns "English"
            }
            coEvery { translator.getTargetTranslations() } returns listOf(mockTarget)
            coEvery { translator.getTargetTranslation("target-1") } returns mockTarget

            // ProjectsImported should reload projects
            invokePrivateMethod(component, "onImportUsfmResult", ImportUsfmComponent.Result.ProjectsImported(listOf("target-1")))
            component.state.awaitState { it.translations.any { item -> item.translation.id == "target-1" } }
            assertTrue(component.state.value.translations.any { it.translation.id == "target-1" })

            // MergeConflict should open project
            invokePrivateMethod(component, "onImportUsfmResult", ImportUsfmComponent.Result.MergeConflict("target-1"))
            assertEquals(HomeComponent.Result.OpenProject("target-1", true), resultReceived)
        }
    }

    @Test
    fun testOnExportResult() {
        runBlocking {
            every { translator.lastFocusTargetTranslation } returns null
            val component = createComponent()

            // ExportToApp
            val mockFile = File("dummy")
            invokePrivateMethod(component, "onExportResult", ExportComponent.Result.ExportToApp(mockFile))
            verify { platform.shareProject(mockFile) }

            // OpenLogin
            invokePrivateMethod(component, "onExportResult", ExportComponent.Result.OpenLogin)
            assertEquals(HomeComponent.Result.OpenLogin, resultReceived)

            // Logout
            invokePrivateMethod(component, "onExportResult", ExportComponent.Result.Logout)
            // Wait for logout async
            delayYield()
            assertEquals(HomeComponent.Result.Logout, resultReceived)

            // MergeConflict
            invokePrivateMethod(component, "onExportResult", ExportComponent.Result.MergeConflict("target-1"))
            assertEquals(HomeComponent.Result.OpenProject("target-1", true), resultReceived)

            // OpenFeedback
            invokePrivateMethod(component, "onExportResult", ExportComponent.Result.OpenFeedback("message"))
            assertTrue(component.dialogSlot.value.child?.instance is HomeComponent.DialogChild.Feedback)
        }
    }

    private fun invokePrivateMethod(instance: Any, name: String, vararg args: Any?) {
        val method = instance::class.java.declaredMethods.first { it.name == name }
        method.isAccessible = true
        method.invoke(instance, *args)
    }

    private suspend fun delayYield() {
        delay(50)
    }
}
