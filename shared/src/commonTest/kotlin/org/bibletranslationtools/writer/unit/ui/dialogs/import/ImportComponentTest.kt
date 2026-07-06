package org.bibletranslationtools.writer.unit.ui.dialogs.import

import io.github.vinceglb.filekit.PlatformFile
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.bibletranslationtools.gogsclient.Repository
import org.bibletranslationtools.resourcecatalog.ResourceCatalogClient
import org.bibletranslationtools.resourcecontainer.Project
import org.bibletranslationtools.writer.DirectoryProvider
import org.bibletranslationtools.writer.Platform
import org.bibletranslationtools.writer.core.TargetTranslation
import org.bibletranslationtools.writer.core.TargetTranslationMigrator
import org.bibletranslationtools.writer.core.Translator
import org.bibletranslationtools.writer.displayName
import org.bibletranslationtools.writer.ui.dialogs.import.DefaultImportComponent
import org.bibletranslationtools.writer.ui.dialogs.import.ImportComponent
import org.bibletranslationtools.writer.ui.home.RepositoryItem
import org.bibletranslationtools.writer.unit.ui.BaseComponentTest
import org.bibletranslationtools.writer.unit.ui.awaitEvent
import org.bibletranslationtools.writer.unit.ui.awaitState
import org.bibletranslationtools.writer.usecases.AdvancedGogsRepoSearch
import org.bibletranslationtools.writer.usecases.CloneRepository
import org.bibletranslationtools.writer.usecases.ImportProjects
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

class ImportComponentTest : BaseComponentTest() {

    private val translator: Translator = mockk(relaxed = true)
    private val advancedGogsRepoSearch: AdvancedGogsRepoSearch = mockk(relaxed = true)
    private val cloneRepository: CloneRepository = mockk(relaxed = true)
    private val registerSSHKeys: RegisterSSHKeys = mockk(relaxed = true)
    private val importProjects: ImportProjects = mockk(relaxed = true)
    private val catalogClient: ResourceCatalogClient = mockk(relaxed = true)
    private val directoryProvider: DirectoryProvider = mockk(relaxed = true)
    private val targetTranslationMigrator: TargetTranslationMigrator = mockk(relaxed = true)
    private val platform: Platform = mockk(relaxed = true)

    private var resultReceived: ImportComponent.Result? = null

    private val mockBackupsDir = mockk<File>(relaxed = true) {
        every { listFiles() } returns emptyArray()
    }

    private val mockTarget = mockk<TargetTranslation>(relaxed = true) {
        every { id } returns "target-1"
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

        every { directoryProvider.backupsDir } returns mockBackupsDir
        coEvery { translator.getTargetTranslation(any()) } returns mockTarget
        every { catalogClient.library.getProject(any(), any(), any()) } returns mockProject

        startKoin {
            modules(
                module {
                    single { translator }
                    single { advancedGogsRepoSearch }
                    single { cloneRepository }
                    single { registerSSHKeys }
                    single { importProjects }
                    single { catalogClient }
                    single { directoryProvider }
                    single { targetTranslationMigrator }
                    single { platform }
                }
            )
        }
        resultReceived = null
    }

    private fun createComponent(projectFile: PlatformFile? = null): DefaultImportComponent =
        createComponent { context ->
            DefaultImportComponent(
                componentContext = context,
                projectFile = projectFile,
                onResult = { resultReceived = it }
            )
        }

    @Test
    fun testInitialization() {
        val component = createComponent()
        assertNotNull(component)
        assertTrue(component.state.value.backups.isEmpty())
    }

    @Test
    fun testClearMergeConflict() {
        val component = createComponent()
        component.clearMergeConflict()
        assertNull(component.state.value.mergeConflict)
    }

    @Test
    fun testClearMethods() {
        val component = createComponent()
        component.clearResult()
        assertNull(component.state.value.resultMessage)
        assertTrue(component.state.value.repositories.isEmpty())

        component.clearSourceConflict()
        assertNull(component.state.value.sourceConflict)

        component.clearImportRepo()
        assertNull(component.state.value.repoToImport)
    }

    @Test
    fun testImportUsfm() {
        val component = createComponent()
        val mockFile = mockk<PlatformFile>(relaxed = true)
        component.importUsfm(mockFile)
        assertTrue(resultReceived is ImportComponent.Result.OpenUsfmImport)
        assertEquals(mockFile, (resultReceived as ImportComponent.Result.OpenUsfmImport).file)
    }

    @Test
    fun testImportSourceSuccess() {
        runBlocking {
            val mockFile = mockk<PlatformFile>(relaxed = true) {
                every { displayName } returns "source_dir"
            }
            coEvery { importProjects.importSource(mockFile, any()) } returns
                ImportProjects.ImportSourceResult(success = true, hasConflict = false, file = mockFile)

            val component = createComponent()
            component.importSource(mockFile, false)

            component.state.awaitState(timeoutMs = 3000) { it.resultMessage != null }
            assertNotNull(component.state.value.resultMessage)
            assertEquals("Mock String", component.state.value.resultMessage?.first)
        }
    }

    @Test
    fun testImportSourceConflict() {
        runBlocking {
            val mockFile = mockk<PlatformFile>(relaxed = true)
            val conflictResult = ImportProjects.ImportSourceResult(success = false, hasConflict = true, file = mockFile)
            coEvery { importProjects.importSource(mockFile, any()) } returns conflictResult

            val component = createComponent()
            component.importSource(mockFile, false)

            component.state.awaitState(timeoutMs = 3000) { it.sourceConflict != null }
            assertEquals(conflictResult, component.state.value.sourceConflict)
        }
    }

    @Test
    fun testImportSourceError() {
        runBlocking {
            val mockFile = mockk<PlatformFile>(relaxed = true)
            coEvery { importProjects.importSource(mockFile, any()) } returns
                ImportProjects.ImportSourceResult(success = false, hasConflict = false, error = "Bad source format")

            val component = createComponent()
            component.importSource(mockFile, false)

            component.state.awaitState(timeoutMs = 3000) { it.resultMessage != null }
            assertEquals("Bad source format", component.state.value.resultMessage?.second)
        }
    }

    @Test
    fun testImportBackup() {
        runBlocking {
            val mockBackupFile = mockk<File>(relaxed = true) {
                every { name } returns "my_backup.tstudio"
            }
            every { any<PlatformFile>().displayName } returns "my_backup.tstudio"

            coEvery { importProjects.importProject(any<PlatformFile>(), any(), any()) } returns
                ImportProjects.ImportPlatformFileResult(
                    file = mockk(relaxed = true) { every { displayName } returns "my_backup.tstudio" },
                    importedSlug = "target-1",
                    success = true,
                    hasMergeConflict = false,
                    invalidFileName = false,
                    alreadyExists = false
                )

            val component = createComponent()
            component.importBackup(mockBackupFile)

            component.state.awaitState(timeoutMs = 3000) { it.resultMessage != null }
            assertTrue(resultReceived is ImportComponent.Result.ProjectsImported)
            assertEquals("target-1", (resultReceived as ImportComponent.Result.ProjectsImported).translationIds.first())
        }
    }

    @Test
    fun testImportProjectSuccess() {
        runBlocking {
            val mockFile = mockk<PlatformFile>(relaxed = true) {
                every { displayName } returns "project.tstudio"
            }
            coEvery { importProjects.importProject(mockFile, any(), any()) } returns
                ImportProjects.ImportPlatformFileResult(
                    file = mockFile,
                    importedSlug = "imported-slug",
                    success = true,
                    hasMergeConflict = false,
                    invalidFileName = false,
                    alreadyExists = false
                )

            val component = createComponent()
            component.importProject(mockFile, false)

            component.state.awaitState(timeoutMs = 3000) { it.resultMessage != null }
            assertTrue(resultReceived is ImportComponent.Result.ProjectsImported)
            assertEquals("imported-slug", (resultReceived as ImportComponent.Result.ProjectsImported).translationIds.first())
        }
    }

    @Test
    fun testImportProjectInvalidFileName() {
        runBlocking {
            val mockFile = mockk<PlatformFile>(relaxed = true) {
                every { displayName } returns "project.zip"
            }
            coEvery { importProjects.importProject(mockFile, any(), any()) } returns
                ImportProjects.ImportPlatformFileResult(
                    file = mockFile,
                    importedSlug = null,
                    success = false,
                    hasMergeConflict = false,
                    invalidFileName = true,
                    alreadyExists = false
                )

            val component = createComponent()
            component.importProject(mockFile, false)

            component.state.awaitState(timeoutMs = 3000) { it.resultMessage != null }
            assertNotNull(component.state.value.resultMessage)
        }
    }

    @Test
    fun testImportProjectInvalidExtension() {
        runBlocking {
            val mockFile = mockk<PlatformFile>(relaxed = true) {
                every { displayName } returns "project.txt"
            }

            val component = createComponent()
            component.importProject(mockFile, false)

            component.state.awaitState(timeoutMs = 3000) { it.resultMessage != null }
            assertNotNull(component.state.value.resultMessage)
            coVerify(exactly = 0) { importProjects.importProject(any(), any(), any()) }
        }
    }

    @Test
    fun testImportProjectAlreadyExistsMerge() {
        runBlocking {
            val mockFile = mockk<PlatformFile>(relaxed = true) {
                every { displayName } returns "project.tstudio"
            }
            coEvery { importProjects.importProject(mockFile, any(), any()) } returns
                ImportProjects.ImportPlatformFileResult(
                    file = mockFile,
                    importedSlug = "target-1",
                    success = true,
                    hasMergeConflict = true,
                    invalidFileName = false,
                    alreadyExists = true
                )

            val component = createComponent()
            component.importProject(mockFile, false)

            component.state.awaitState(timeoutMs = 3000) { it.mergeConflict != null }
            val conflict = component.state.value.mergeConflict
            assertNotNull(conflict)
            assertEquals(mockTarget, conflict.translation)
            assertTrue(conflict.hasMergeConflict)
        }
    }

    @Test
    fun testImportRepoNotSupportedRepo() {
        runBlocking {
            val repoItem = RepositoryItem(
                languageName = "English",
                projectName = "Genesis",
                targetTranslationSlug = "en_gen_text",
                languageCode = "en",
                languageDirection = "ltr",
                repoName = "user/en_gen_text",
                url = "http://repo.url",
                isPrivate = false,
                unsupportedTag = "Unsupported Tag"
            )

            val component = createComponent()
            component.importRepo(repoItem, accepted = false, overwrite = false)

            component.state.awaitState(timeoutMs = 3000) { it.repoToImport != null }
            assertEquals(repoItem, component.state.value.repoToImport)
        }
    }

    @Test
    fun testSearchRepositoriesSuccess() {
        runBlocking {
            val repo = Repository(name = "en_gen_text", fullName = "user/en_gen_text")
            coEvery { advancedGogsRepoSearch.execute(any(), any(), any(), any()) } returns listOf(repo)

            val component = createComponent()
            component.searchRepositories("user", "gen")

            component.state.awaitState(timeoutMs = 3000) { it.repositories.isNotEmpty() }
            assertEquals(1, component.state.value.repositories.size)
            assertEquals("en_gen_text", component.state.value.repositories.first().targetTranslationSlug)
        }
    }

    @Test
    fun testSearchRepositoriesException() {
        runBlocking {
            coEvery { advancedGogsRepoSearch.execute(any(), any(), any(), any()) } throws Exception("Search failed")

            val component = createComponent()
            component.searchRepositories("user", "gen")

            component.state.awaitState(timeoutMs = 3000) { it.resultMessage != null }
            assertTrue(component.state.value.repositories.isEmpty())
            assertNotNull(component.state.value.resultMessage)
        }
    }

    @Test
    fun testForceRegisterKeysSuccess() {
        runBlocking {
            val repoItem = RepositoryItem(
                languageName = "English",
                projectName = "Genesis",
                targetTranslationSlug = "en_gen_text",
                languageCode = "en",
                languageDirection = "ltr",
                repoName = "user/en_gen_text",
                url = "http://repo.url",
                isPrivate = false,
                unsupportedTag = ""
            )
            coEvery { registerSSHKeys.execute(true, any()) } returns true
            coEvery { cloneRepository.execute(any(), any()) } returns
                CloneRepository.Result(CloneRepository.Status.SUCCESS, "http://repo.url", null)

            val component = createComponent()
            val unsupportedRepoItem = repoItem.copy(unsupportedTag = "Unsupported")
            component.importRepo(unsupportedRepoItem, accepted = false, overwrite = false)
            component.state.awaitState(timeoutMs = 3000) { it.repoToImport != null }

            component.registerKeys()

            delay(300)
            coVerify { registerSSHKeys.execute(true, any()) }
            coVerify { cloneRepository.execute(repoItem.url, any()) }
        }
    }

    @Test
    fun testForceRegisterKeysFailure() {
        runBlocking {
            coEvery { registerSSHKeys.execute(true, any()) } returns false

            val component = createComponent()
            component.registerKeys()

            val event = component.event.awaitEvent(timeoutMs = 3000)
            assertTrue(event is ImportComponent.Event.AuthRequested)
        }
    }

    @Test
    fun testCloneRepositorySuccess() {
        runBlocking {
            val repoItem = RepositoryItem(
                languageName = "English",
                projectName = "Genesis",
                targetTranslationSlug = "en_gen_text",
                languageCode = "en",
                languageDirection = "ltr",
                repoName = "user/en_gen_text",
                url = "http://repo.url",
                isPrivate = false,
                unsupportedTag = ""
            )

            val mockTempDir = File.createTempFile("temp_dir", "")
            mockTempDir.delete()
            mockTempDir.mkdirs()
            mockTempDir.deleteOnExit()

            coEvery { cloneRepository.execute(repoItem.url, any()) } returns
                CloneRepository.Result(CloneRepository.Status.SUCCESS, repoItem.url, mockTempDir)

            val mockMigratedDir = File.createTempFile("migrated_dir", "")
            mockMigratedDir.delete()
            mockMigratedDir.mkdirs()
            mockMigratedDir.deleteOnExit()

            coEvery { targetTranslationMigrator.migrate(mockTempDir, any()) } returns mockMigratedDir
            coEvery { targetTranslationMigrator.migrate(mockTempDir) } returns mockMigratedDir

            mockkObject(TargetTranslation)
            coEvery { TargetTranslation.open(any(), any()) } returns mockTarget

            coEvery { translator.getTargetTranslation(any()) } returns null

            val component = createComponent()
            component.importRepo(repoItem, accepted = true, overwrite = false)

            component.state.awaitState(timeoutMs = 3000) { it.resultMessage != null }
            coVerify { translator.restoreTargetTranslation(mockTarget) }
            assertTrue(resultReceived is ImportComponent.Result.ProjectsImported)
            assertEquals(mockTarget.id, (resultReceived as ImportComponent.Result.ProjectsImported).translationIds.first())
        }
    }

    @Test
    fun testCloneRepositoryAuthFailureNoKeys() {
        runBlocking {
            val repoItem = RepositoryItem(
                languageName = "English",
                projectName = "Genesis",
                targetTranslationSlug = "en_gen_text",
                languageCode = "en",
                languageDirection = "ltr",
                repoName = "user/en_gen_text",
                url = "http://repo.url",
                isPrivate = false,
                unsupportedTag = ""
            )

            coEvery { cloneRepository.execute(repoItem.url, any()) } returns
                CloneRepository.Result(CloneRepository.Status.AUTH_FAILURE, repoItem.url, null)

            every { directoryProvider.hasSSHKeys() } returns false
            coEvery { registerSSHKeys.execute(false, any()) } returns true

            val component = createComponent()
            component.importRepo(repoItem, accepted = true, overwrite = false)

            delay(300)
            coVerify { registerSSHKeys.execute(false, any()) }
        }
    }

    @Test
    fun testCloneRepositoryAuthFailureWithKeys() {
        runBlocking {
            val repoItem = RepositoryItem(
                languageName = "English",
                projectName = "Genesis",
                targetTranslationSlug = "en_gen_text",
                languageCode = "en",
                languageDirection = "ltr",
                repoName = "user/en_gen_text",
                url = "http://repo.url",
                isPrivate = false,
                unsupportedTag = ""
            )

            coEvery { cloneRepository.execute(repoItem.url, any()) } returns
                CloneRepository.Result(CloneRepository.Status.AUTH_FAILURE, repoItem.url, null)

            every { directoryProvider.hasSSHKeys() } returns true

            val component = createComponent()
            component.importRepo(repoItem, accepted = true, overwrite = false)

            val event = component.event.awaitEvent(timeoutMs = 3000)
            assertTrue(event is ImportComponent.Event.AuthRequested)
            assertEquals(repoItem, component.state.value.repoToImport)
        }
    }

    @Test
    fun testCloneRepositoryMergeConflictNoMerge() {
        runBlocking {
            val repoItem = RepositoryItem(
                languageName = "English",
                projectName = "Genesis",
                targetTranslationSlug = "en_gen_text",
                languageCode = "en",
                languageDirection = "ltr",
                repoName = "user/en_gen_text",
                url = "http://repo.url",
                isPrivate = false,
                unsupportedTag = ""
            )

            val mockTempDir = File.createTempFile("temp_dir", "")
            mockTempDir.delete()
            mockTempDir.mkdirs()
            mockTempDir.deleteOnExit()

            coEvery { cloneRepository.execute(repoItem.url, any()) } returns
                CloneRepository.Result(CloneRepository.Status.SUCCESS, repoItem.url, mockTempDir)

            val mockMigratedDir = File.createTempFile("migrated_dir", "")
            mockMigratedDir.delete()
            mockMigratedDir.mkdirs()
            mockMigratedDir.deleteOnExit()

            coEvery { targetTranslationMigrator.migrate(mockTempDir, any()) } returns mockMigratedDir
            coEvery { targetTranslationMigrator.migrate(mockTempDir) } returns mockMigratedDir

            mockkObject(TargetTranslation)
            coEvery { TargetTranslation.open(any(), any()) } returns mockTarget

            val existingTarget = mockk<TargetTranslation>(relaxed = true) {
                every { id } returns "target-1"
                coEvery { merge(any(), any()) } returns false
            }
            coEvery { translator.getTargetTranslation(any()) } returns existingTarget
            coEvery { translator.getTargetTranslation("target-1") } returns existingTarget

            val component = createComponent()
            component.importRepo(repoItem, accepted = true, overwrite = false)

            component.state.awaitState(timeoutMs = 3000) { it.mergeConflict != null }
            val conflict = component.state.value.mergeConflict
            assertNotNull(conflict)
            assertEquals(existingTarget, conflict.translation)
            assertTrue(conflict.hasMergeConflict)
            assertTrue(conflict.isFromServer)
        }
    }

    @Test
    fun testMergeConflictCallbacksOverwrite() {
        runBlocking {
            val mockFile = mockk<PlatformFile>(relaxed = true) {
                every { displayName } returns "project.tstudio"
            }
            coEvery { importProjects.importProject(mockFile, any(), any()) } returns
                ImportProjects.ImportPlatformFileResult(
                    file = mockFile,
                    importedSlug = "target-1",
                    success = true,
                    hasMergeConflict = true,
                    alreadyExists = true,
                    invalidFileName = false
                )

            val component = createComponent()
            component.importProject(mockFile, false)

            component.state.awaitState(timeoutMs = 3000) { it.mergeConflict != null }
            val conflict = component.state.value.mergeConflict
            assertNotNull(conflict)

            coEvery { importProjects.importProject(mockFile, true, any()) } returns
                ImportProjects.ImportPlatformFileResult(
                    file = mockFile,
                    importedSlug = "target-1",
                    success = true,
                    hasMergeConflict = false,
                    alreadyExists = false,
                    invalidFileName = false
                )

            conflict.onOverwrite()

            delay(300)
            coVerify { importProjects.importProject(mockFile, true, any()) }
        }
    }

    @Test
    fun testMergeConflictCallbacksCancel() {
        runBlocking {
            val mockFile = mockk<PlatformFile>(relaxed = true) {
                every { displayName } returns "project.tstudio"
            }
            coEvery { importProjects.importProject(mockFile, any(), any()) } returns
                ImportProjects.ImportPlatformFileResult(
                    file = mockFile,
                    importedSlug = "target-1",
                    success = true,
                    hasMergeConflict = true,
                    alreadyExists = true,
                    invalidFileName = false
                )

            val component = createComponent()
            component.importProject(mockFile, false)

            component.state.awaitState(timeoutMs = 3000) { it.mergeConflict != null }
            val conflict = component.state.value.mergeConflict
            assertNotNull(conflict)

            conflict.onCancel()

            delay(300)
            coVerify { mockTarget.resetToMasterBackup() }
        }
    }

    @Test
    fun testMergeConflictCallbacksResolve() {
        runBlocking {
            val mockFile = mockk<PlatformFile>(relaxed = true) {
                every { displayName } returns "project.tstudio"
            }
            coEvery { importProjects.importProject(mockFile, any(), any()) } returns
                ImportProjects.ImportPlatformFileResult(
                    file = mockFile,
                    importedSlug = "target-1",
                    success = true,
                    hasMergeConflict = true,
                    alreadyExists = true,
                    invalidFileName = false
                )

            val component = createComponent()
            component.importProject(mockFile, false)

            component.state.awaitState(timeoutMs = 3000) { it.mergeConflict != null }
            val conflict = component.state.value.mergeConflict
            assertNotNull(conflict)

            conflict.onResolve()

            assertTrue(resultReceived is ImportComponent.Result.MergeConflict)
            assertEquals("target-1", (resultReceived as ImportComponent.Result.MergeConflict).translationId)
        }
    }
}
