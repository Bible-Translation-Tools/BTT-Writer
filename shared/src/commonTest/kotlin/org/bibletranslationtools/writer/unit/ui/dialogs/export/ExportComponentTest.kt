package org.bibletranslationtools.writer.unit.ui.dialogs.export

import io.github.vinceglb.filekit.PlatformFile
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.verify
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.bibletranslationtools.resourcecatalog.ResourceCatalogClient
import org.bibletranslationtools.resourcecontainer.Project
import org.bibletranslationtools.writer.DirectoryProvider
import org.bibletranslationtools.writer.Platform
import org.bibletranslationtools.writer.core.MergeConflictsHandler
import org.bibletranslationtools.writer.core.Profile
import org.bibletranslationtools.writer.core.TargetTranslation
import org.bibletranslationtools.writer.core.Translator
import org.bibletranslationtools.writer.data.Preference
import org.bibletranslationtools.writer.displayName
import org.bibletranslationtools.writer.ui.dialogs.export.DefaultExportComponent
import org.bibletranslationtools.writer.ui.dialogs.export.ExportComponent
import org.bibletranslationtools.writer.unit.ui.BaseComponentTest
import org.bibletranslationtools.writer.unit.ui.awaitEvent
import org.bibletranslationtools.writer.unit.ui.awaitState
import org.bibletranslationtools.writer.usecases.CreateRepository
import org.bibletranslationtools.writer.usecases.DownloadImages
import org.bibletranslationtools.writer.usecases.ExportProjects
import org.bibletranslationtools.writer.usecases.GogsLogout
import org.bibletranslationtools.writer.usecases.PullTargetTranslation
import org.bibletranslationtools.writer.usecases.PushTargetTranslation
import org.bibletranslationtools.writer.usecases.RegisterSSHKeys
import org.jetbrains.compose.resources.getString
import org.junit.Before
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.dsl.module
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ExportComponentTest : BaseComponentTest() {

    private val export: ExportProjects = mockk(relaxed = true)
    private val downloadImages: DownloadImages = mockk(relaxed = true)
    private val translator: Translator = mockk(relaxed = true)
    private val profile: Profile = mockk(relaxed = true)
    private val directoryProvider: DirectoryProvider = mockk(relaxed = true)
    private val catalogClient: ResourceCatalogClient = mockk(relaxed = true)
    private val gogsLogout: GogsLogout = mockk(relaxed = true)
    private val createRepository: CreateRepository = mockk(relaxed = true)
    private val pullTargetTranslation: PullTargetTranslation = mockk(relaxed = true)
    private val pushTargetTranslation: PushTargetTranslation = mockk(relaxed = true)
    private val registerSSHKeys: RegisterSSHKeys = mockk(relaxed = true)
    private val preference: Preference = mockk(relaxed = true)
    private val platform: Platform = mockk(relaxed = true)

    private var resultReceived: ExportComponent.Result? = null

    private val mockTarget = mockk<TargetTranslation>(relaxed = true) {
        every { id } returns "target-1"
        every { targetLanguageName } returns "English"
        every { projectId } returns "gen"
    }

    private val mockProject = mockk<Project>(relaxed = true) {
        every { name } returns "Genesis"
    }

    @Before
    fun setUpComponent() {
        mockkStatic("org.jetbrains.compose.resources.StringResourcesKt")
        mockkStatic(PlatformFile::displayName)
        coEvery { getString(any()) } returns "Mock String"
        coEvery { getString(any(), *anyVararg()) } returns "Mock String"

        every { preference.getPref(any(), any(), any()) } answers { args[1]!! }

        coEvery { translator.getTargetTranslation("target-1") } returns mockTarget
        every { catalogClient.library.getProject(any(), any(), any()) } returns mockProject
        every { platform.deviceLanguageCode } returns "en"

        // Default push result: OK
        coEvery { pushTargetTranslation.execute(any(), any()) } returns
            PushTargetTranslation.Result(PushTargetTranslation.Status.OK, null)

        startKoin {
            modules(
                module {
                    single { export }
                    single { downloadImages }
                    single { translator }
                    single { profile }
                    single { directoryProvider }
                    single { catalogClient }
                    single { gogsLogout }
                    single { createRepository }
                    single { pullTargetTranslation }
                    single { pushTargetTranslation }
                    single { registerSSHKeys }
                    single { preference }
                    single { platform }
                }
            )
        }
        resultReceived = null
    }

    private fun createComponent(): DefaultExportComponent =
        createComponent { context ->
            DefaultExportComponent(
                componentContext = context,
                translationId = "target-1",
                showPrint = true,
                onResult = { resultReceived = it }
            )
        }

    @Test
    fun testInitialization() {
        val component = createComponent()
        assertNotNull(component.targetTranslation)
        assertEquals("Genesis", component.projectName)
    }

    @Test
    fun testOnMergeConflict() {
        val component = createComponent()
        component.onMergeConflict()
        assertTrue(resultReceived is ExportComponent.Result.MergeConflict)
        assertEquals("target-1", (resultReceived as ExportComponent.Result.MergeConflict).translationId)
    }

    @Test
    fun testShowFeedbackDialog() {
        val component = createComponent()
        component.showFeedbackDialog("Test feedback")
        assertTrue(resultReceived is ExportComponent.Result.OpenFeedback)
        assertEquals("Test feedback", (resultReceived as ExportComponent.Result.OpenFeedback).message)
    }

    @Test
    fun testTranslationNotFoundReturnsError() {
        runBlocking {
            coEvery { translator.getTargetTranslation("missing-id") } returns null

            createComponent { context ->
                DefaultExportComponent(
                    componentContext = context,
                    translationId = "missing-id",
                    showPrint = false,
                    onResult = { resultReceived = it }
                )
            }

            assertTrue(resultReceived is ExportComponent.Result.Error)
        }
    }

    @Test
    fun testClearStateMethods() {
        runBlocking {
            val component = createComponent()

            // All four clear methods should set their respective state field to null
            component.clearInfoMessage()
            assertNull(component.state.value.info)

            component.clearErrorMessage()
            assertNull(component.state.value.uploadError)

            component.clearUploadSuccess()
            assertNull(component.state.value.uploadSuccess)

            component.clearMergeConflict()
            assertNull(component.state.value.mergeConflict)
        }
    }

    @Test
    fun testLogoutThenLogin() {
        runBlocking {
            val component = createComponent()
            component.logout(thenLogin = true)

            delay(300)

            coVerify { gogsLogout.execute() }
            verify { profile.logout() }
            assertTrue(resultReceived is ExportComponent.Result.OpenLogin)
        }
    }

    @Test
    fun testLogoutWithoutLogin() {
        runBlocking {
            val component = createComponent()
            component.logout(thenLogin = false)

            delay(300)

            coVerify { gogsLogout.execute() }
            verify { profile.logout() }
            assertTrue(resultReceived is ExportComponent.Result.Logout)
        }
    }

    @Test
    fun testExportUsfmSuccess() {
        runBlocking {
            val mockFile = mockk<PlatformFile>(relaxed = true) {
                every { displayName } returns "genesis.usfm"
            }

            val mockResult = mockk<ExportProjects.Result>(relaxed = true) {
                every { success } returns true
                every { file } returns mockFile
            }
            coEvery { export.exportUSFM(any(), any()) } returns mockResult

            val component = createComponent()
            component.exportUsfm(mockFile)

            component.state.awaitState { it.info != null }
            assertNotNull(component.state.value.info)
        }
    }

    @Test
    fun testExportUsfmWrongExtensionShowsError() {
        runBlocking {
            val mockFile = mockk<PlatformFile>(relaxed = true) {
                every { displayName } returns "genesis.txt"
            }
            val component = createComponent()
            component.exportUsfm(mockFile)

            component.state.awaitState { it.info != null }
            assertNotNull(component.state.value.info)
        }
    }

    @Test
    fun testExportProjectSuccess() {
        runBlocking {
            val mockFile = mockk<PlatformFile>(relaxed = true) {
                every { displayName } returns "genesis.tstudio"
            }

            val mockResult = mockk<ExportProjects.Result>(relaxed = true) {
                every { success } returns true
                every { file } returns mockFile
            }
            coEvery { export.exportProject(any<TargetTranslation>(), any<PlatformFile>(), any()) } returns mockResult

            val component = createComponent()
            component.exportProject(mockFile)

            component.state.awaitState { it.info != null }
            assertNotNull(component.state.value.info)
        }
    }

    @Test
    fun testExportProjectWrongExtensionShowsError() {
        runBlocking {
            val mockFile = mockk<PlatformFile>(relaxed = true) {
                every { displayName } returns "genesis.badext"
            }
            val component = createComponent()
            component.exportProject(mockFile)

            component.state.awaitState { it.info != null }
            assertNotNull(component.state.value.info)
        }
    }

    @Test
    fun testExportToAppFailureEmitsSnackbar() {
        runBlocking {
            val sharingDir = mockk<File>(relaxed = true)
            every { directoryProvider.sharingDir } returns sharingDir

            // Throw so the file is never created → snackbar
            coEvery { export.exportProject(any<TargetTranslation>(), any<File>()) } throws Exception("Export error")

            val component = createComponent()
            component.exportToApp()

            val event = component.event.awaitEvent(timeoutMs = 2000)
            assertTrue(event is ExportComponent.Event.SnackbarMessage)
        }
    }

    @Test
    fun testOpenExportToCloudPullUpToDateThenPushOk() {
        runBlocking {
            coEvery { pullTargetTranslation.execute(any(), any(), any(), any()) } returns
                PullTargetTranslation.Result(PullTargetTranslation.Status.UP_TO_DATE, null)

            coEvery { pushTargetTranslation.execute(any(), any()) } returns
                PushTargetTranslation.Result(PushTargetTranslation.Status.OK, "http://example.com/repo")

            every { profile.gogsUser } returns mockk(relaxed = true) {
                every { username } returns "testuser"
            }

            val component = createComponent()
            component.openExportToCloud()

            component.state.awaitState(timeoutMs = 3000) { it.uploadSuccess != null }
            assertNotNull(component.state.value.uploadSuccess)
        }
    }

    @Test
    fun testOpenExportToCloudPullAuthFailureNoKeys() {
        runBlocking {
            coEvery { pullTargetTranslation.execute(any(), any(), any(), any()) } returns
                PullTargetTranslation.Result(PullTargetTranslation.Status.AUTH_FAILURE, "Auth failed")

            every { directoryProvider.hasSSHKeys() } returns false
            // registerSSHKeys returns false → emit AuthRequested
            coEvery { registerSSHKeys.execute(any(), any()) } returns false

            val component = createComponent()
            component.openExportToCloud()

            val event = component.event.awaitEvent(timeoutMs = 3000)
            assertTrue(event is ExportComponent.Event.AuthRequested)
        }
    }

    @Test
    fun testOpenExportToCloudPullAuthFailureWithKeys() {
        runBlocking {
            coEvery { pullTargetTranslation.execute(any(), any(), any(), any()) } returns
                PullTargetTranslation.Result(PullTargetTranslation.Status.AUTH_FAILURE, "Auth failed")

            every { directoryProvider.hasSSHKeys() } returns true

            val component = createComponent()
            component.openExportToCloud()

            val event = component.event.awaitEvent(timeoutMs = 3000)
            assertTrue(event is ExportComponent.Event.AuthRequested)
        }
    }

    @Test
    fun testOpenExportToCloudMergeConflictSetsMergeConflictState() {
        runBlocking {
            coEvery { pullTargetTranslation.execute(any(), any(), any(), any()) } returns
                PullTargetTranslation.Result(PullTargetTranslation.Status.MERGE_CONFLICTS, null)

            mockkObject(MergeConflictsHandler)
            coEvery { MergeConflictsHandler.isTranslationMergeConflicted(any(), any()) } returns true

            val component = createComponent()
            component.openExportToCloud()

            component.state.awaitState(timeoutMs = 3000) { it.mergeConflict != null }
            assertNotNull(component.state.value.mergeConflict)
        }
    }

    @Test
    fun testRegisterKeysFails_EmitsAuthRequested() {
        runBlocking {
            coEvery { registerSSHKeys.execute(any(), any()) } returns false

            val component = createComponent()
            component.registerKeys()

            val event = component.event.awaitEvent(timeoutMs = 3000)
            assertTrue(event is ExportComponent.Event.AuthRequested)
        }
    }

    @Test
    fun testRegisterKeysSuccessTriggersCloudExport() {
        runBlocking {
            coEvery { registerSSHKeys.execute(any(), any()) } returns true

            coEvery { pullTargetTranslation.execute(any(), any(), any(), any()) } returns
                PullTargetTranslation.Result(PullTargetTranslation.Status.UP_TO_DATE, null)

            every { profile.gogsUser } returns mockk(relaxed = true) {
                every { username } returns "testuser"
            }

            val component = createComponent()
            component.registerKeys()

            component.state.awaitState(timeoutMs = 3000) { it.uploadSuccess != null }
            assertNotNull(component.state.value.uploadSuccess)
        }
    }

    @Test
    fun testResetToMasterCallsTargetTranslation() {
        runBlocking {
            val component = createComponent()
            component.resetToMaster()

            delay(300)
            verify { mockTarget.resetToMasterBackup() }
        }
    }

    @Test
    fun testPushRejectedSetsMergeConflictState() {
        runBlocking {
            coEvery { pullTargetTranslation.execute(any(), any(), any(), any()) } returns
                PullTargetTranslation.Result(PullTargetTranslation.Status.UP_TO_DATE, null)

            coEvery { pushTargetTranslation.execute(any(), any()) } returns
                PushTargetTranslation.Result(PushTargetTranslation.Status.REJECTED_NON_FAST_FORWARD, null)

            val component = createComponent()
            component.openExportToCloud()

            component.state.awaitState(timeoutMs = 3000) { it.mergeConflict != null }
            assertNotNull(component.state.value.mergeConflict)
        }
    }

    @Test
    fun testPushAuthFailureEmitsAuthEvent() {
        runBlocking {
            coEvery { pullTargetTranslation.execute(any(), any(), any(), any()) } returns
                PullTargetTranslation.Result(PullTargetTranslation.Status.UP_TO_DATE, null)

            coEvery { pushTargetTranslation.execute(any(), any()) } returns
                PushTargetTranslation.Result(PushTargetTranslation.Status.AUTH_FAILURE, null)

            val component = createComponent()
            component.openExportToCloud()

            val event = component.event.awaitEvent(timeoutMs = 3000)
            assertTrue(event is ExportComponent.Event.AuthRequested)
        }
    }
}
