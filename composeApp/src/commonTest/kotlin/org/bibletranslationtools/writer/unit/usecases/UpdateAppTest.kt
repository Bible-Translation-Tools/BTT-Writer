package org.bibletranslationtools.writer.unit.usecases

import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.runs
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.bibletranslationtools.resourcecatalog.ResourceCatalogClient
import org.bibletranslationtools.resourcecatalog.library.Index
import org.bibletranslationtools.resourcecatalog.library.models.Translation
import org.bibletranslationtools.resourcecontainer.ResourceContainer
import org.bibletranslationtools.writer.AppInfo
import org.bibletranslationtools.writer.DirectoryProvider
import org.bibletranslationtools.writer.Platform
import org.bibletranslationtools.writer.core.TargetTranslation
import org.bibletranslationtools.writer.core.TargetTranslationMigrator
import org.bibletranslationtools.writer.core.Translator
import org.bibletranslationtools.writer.data.Preference
import org.bibletranslationtools.writer.data.getPref
import org.bibletranslationtools.writer.data.setPref
import org.bibletranslationtools.writer.usecases.BackupRC
import org.bibletranslationtools.writer.usecases.UpdateApp
import org.bibletranslationtools.writer.utils.FileUtilities
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class UpdateAppTest {

    @MockK private lateinit var preference: Preference
    @MockK private lateinit var directoryProvider: DirectoryProvider
    @MockK private lateinit var catalogClient: ResourceCatalogClient
    @MockK private lateinit var backupRC: BackupRC
    @MockK private lateinit var translator: Translator
    @MockK private lateinit var migrator: TargetTranslationMigrator
    @MockK private lateinit var index: Index
    @MockK private lateinit var info: AppInfo
    @MockK private lateinit var platform: Platform

    private val onProgress = mockk<(Float, String?) -> Unit>(relaxed = true)

    @JvmField
    @Rule
    var tempDir: TemporaryFolder = TemporaryFolder()

    @Before
    fun setup() {
        MockKAnnotations.init(this)

        every { info.versionCode }.returns(10)
        every { platform.info }.returns(info)

        every { catalogClient.library } returns index
        every { preference.getPref("last_version_code", any<String>()) }
            .returns("1")
        every { preference.setPref(any(), any<Int>()) }.just(runs)
        every { preference.setPref(any(), any<String>()) }.just(runs)
        every { preference.getPref(
            Preference.KEY_PREF_LANGUAGES_URL,
            any<String>()
        ) }.returns("/lang_names.jsom")
        every { preference.getPref(
            Preference.KEY_PREF_TRANSLATION_TYPEFACE,
            any<String>()
        ) }.returns("font.ttf")
        every { preference.getPref(
            Preference.KEY_PREF_SOURCE_TYPEFACE,
            any<String>()
        ) }.returns("font.ttf")

        every { index.getImportedTranslations() }.returns(listOf())
        every { catalogClient.openLibrary() }.just(runs)
        every { catalogClient.closeLibrary() }.just(runs)
        coEvery { directoryProvider.deleteLibrary() }.just(runs)
        coEvery { directoryProvider.deployDefaultLibrary() }.just(runs)
        every { directoryProvider.internalAppDir }
            .returns(tempDir.newFolder("internal"))
        every { directoryProvider.cacheDir }.returns(tempDir.newFolder("cache"))

        mockkObject(ResourceContainer)
        coEvery { catalogClient.importResourceContainer(any()) }.returns(mockk())

        mockkObject(FileUtilities)
        every { FileUtilities.deleteQuietly(any()) }.returns(true)
        every { FileUtilities.moveOrCopyQuietly(any(), any()) }.returns(true)
        every { FileUtilities.copyDirectory(any<File>(), any(), any()) }.just(runs)
        every { FileUtilities.copyFile(any(), any()) }.just(runs)

        every { translator.path }.returns(tempDir.newFolder("translations"))

        mockkObject(TargetTranslation)
        coEvery { translator.getTargetTranslations() }.returns(listOf())

        every { onProgress(any(), any()) }.just(runs)

        justRun { catalogClient.openLibrary() }
    }

    @After
    fun tearDown() {
        unmockkAll()
        tempDir.delete()
    }

    @Test
    fun `test update app, fresh install`() = runTest {
        every { preference.getPref("last_version_code", any<Int>()) }
            .returns(0)
        every { catalogClient.isLibraryDeployed }.returns(true)

        UpdateApp(
            preference,
            directoryProvider,
            catalogClient,
            backupRC,
            translator,
            migrator,
            platform
        ).execute(onProgress)

        verify { catalogClient.isLibraryDeployed }

        verifyCommonStuff()
        verifyUpdateLibrary()
        verifyNoSourceTranslations()
    }

    @Test
    fun `test update app, install update`() = runTest {
        every { preference.getPref("last_version_code", any<Int>()) }
            .returns(9)
        every { catalogClient.isLibraryDeployed }.returns(true)

        UpdateApp(
            preference,
            directoryProvider,
            catalogClient,
            backupRC,
            translator,
            migrator,
            platform
        ).execute(onProgress)

        verify(exactly = 0) { catalogClient.isLibraryDeployed }

        verifyCommonStuff()
        verifyUpdateLibrary()
        verifyNoSourceTranslations()
    }

    @Test
    fun `test update app, backup imported sources`() = runTest {
        every { preference.getPref("last_version_code", any<Int>()) }
            .returns(10)
        every { catalogClient.isLibraryDeployed }.returns(false)

        val translation: Translation = mockk()
        every { index.getImportedTranslations() }.returns(listOf(translation))

        val file = tempDir.newFile("backup.zip")
        every { backupRC.backupResourceContainer(translation) }.returns(file)

        every { ResourceContainer.open(file, any()) }.returns(mockk())

        UpdateApp(
            preference,
            directoryProvider,
            catalogClient,
            backupRC,
            translator,
            migrator,
            platform
        ).execute(onProgress)

        verify { catalogClient.isLibraryDeployed }
        verifyCommonStuff()
        verifyUpdateLibrary()

        verify { backupRC.backupResourceContainer(translation) }
        verify { ResourceContainer.open(file, any()) }
        coVerify { catalogClient.importResourceContainer(any()) }
        verify { FileUtilities.deleteQuietly(any()) }
    }

    @Test
    fun `test update app, update target translations`() = runTest {
        every { preference.getPref("last_version_code", any<Int>()) }
            .returns(10)
        every { catalogClient.isLibraryDeployed }.returns(true)

        val targetTranslationDir = File(translator.path, "aa_mrk_text_ulb")
        targetTranslationDir.mkdirs()

        val targetTranslation: TargetTranslation = mockk {
            every { unlockRepo() }.returns(true)
            every { commitSync() }.returns(true)
            every { id }.returns("aa_mrk_text_ulb")
        }
        coEvery { translator.getTargetTranslations() }.returns(listOf(targetTranslation))

        coEvery { migrator.migrate(targetTranslationDir) }.returns(targetTranslationDir)

        UpdateApp(
            preference,
            directoryProvider,
            catalogClient,
            backupRC,
            translator,
            migrator,
            platform
        ).execute(onProgress)

        verify { catalogClient.isLibraryDeployed }
        verifyCommonStuff()
        verifyNoSourceTranslations()

        verify { translator.path }
        coVerify { migrator.migrate(targetTranslationDir) }
        verify { targetTranslation.unlockRepo() }
        verify { targetTranslation.commitSync() }
        verify { targetTranslation.id }
    }

    @Test
    fun `test update app, upgrade build numbers`() = runTest {
        every { preference.getPref("last_version_code", any<Int>()) }
            .returns(10)
        every { catalogClient.isLibraryDeployed }.returns(true)

        val targetTranslation: TargetTranslation = mockk {
            every { id }.returns("aa_mrk_text_ulb")
        }
        coEvery { translator.getTargetTranslations() }.returns(listOf(targetTranslation))

        UpdateApp(
            preference,
            directoryProvider,
            catalogClient,
            backupRC,
            translator,
            migrator,
            platform
        ).execute(onProgress)

        verify { catalogClient.isLibraryDeployed }
        verifyCommonStuff()
        verifyNoSourceTranslations()

        verify { targetTranslation.updateGenerator(any()) }
        verify { targetTranslation.id }
        verify { translator.path }
    }

    private fun verifyCommonStuff() {
        verify { preference.getPref("last_version_code", any<Int>()) }
        verify { info.versionCode }
    }

    private fun verifyUpdateLibrary(called: Boolean = true) {
        verify(inverse = !called) { index.getImportedTranslations() }
        verify(inverse = !called) { catalogClient.closeLibrary() }
        coVerify(inverse = !called) { directoryProvider.deleteLibrary() }
        coVerify(inverse = !called) { directoryProvider.deployDefaultLibrary() }
    }

    private fun verifyNoSourceTranslations() {
        verify(exactly = 0) { backupRC.backupResourceContainer(any()) }
        coVerify(exactly = 0) { catalogClient.importResourceContainer(any()) }
    }
}