package org.bibletranslationtools.writer.unit.ui.settings

import btt_writer.shared.generated.resources.Res
import btt_writer.shared.generated.resources.backup_intervals_values_array
import btt_writer.shared.generated.resources.content_server_account_create_urls_array
import btt_writer.shared.generated.resources.content_server_git_server_api_values_array
import btt_writer.shared.generated.resources.content_server_index_sqlite_url_array
import btt_writer.shared.generated.resources.content_server_lang_names_url_array
import btt_writer.shared.generated.resources.content_server_media_server_values_array
import btt_writer.shared.generated.resources.content_server_names_array
import btt_writer.shared.generated.resources.content_server_reader_server_values_array
import btt_writer.shared.generated.resources.content_server_values_array
import btt_writer.shared.generated.resources.font_size_values_array
import btt_writer.shared.generated.resources.pref_backup_interval_titles
import btt_writer.shared.generated.resources.pref_color_theme_titles
import btt_writer.shared.generated.resources.pref_logging_level_titles
import btt_writer.shared.generated.resources.pref_typeface_size_titles
import btt_writer.shared.generated.resources.pref_typeface_titles
import io.github.vinceglb.filekit.PlatformFile
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.runBlocking
import org.bibletranslationtools.writer.AppInfo
import org.bibletranslationtools.writer.DirectoryProvider
import org.bibletranslationtools.writer.Platform
import org.bibletranslationtools.writer.core.BackupScheduler
import org.bibletranslationtools.writer.core.Profile
import org.bibletranslationtools.writer.core.Typography
import org.bibletranslationtools.writer.data.Preference
import org.bibletranslationtools.writer.ui.settings.DefaultSettingsComponent
import org.bibletranslationtools.writer.ui.settings.SettingsComponent
import org.bibletranslationtools.writer.unit.ui.BaseComponentTest
import org.bibletranslationtools.writer.unit.ui.awaitState
import org.bibletranslationtools.writer.usecases.CheckForLatestRelease
import org.bibletranslationtools.writer.usecases.GogsLogout
import org.bibletranslationtools.writer.usecases.MigrateTranslations
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.getStringArray
import org.junit.Before
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.dsl.module
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SettingsComponentTest : BaseComponentTest() {

    private val checkForLatestRelease: CheckForLatestRelease = mockk(relaxed = true)
    private val profile: Profile = mockk(relaxed = true)
    private val logout: GogsLogout = mockk(relaxed = true)
    private val migrateTranslations: MigrateTranslations = mockk(relaxed = true)
    private val preference: Preference = mockk(relaxed = true)
    private val directoryProvider: DirectoryProvider = mockk(relaxed = true)
    private val typography: Typography = mockk(relaxed = true)
    private val backupScheduler: BackupScheduler = mockk(relaxed = true)
    private val platform: Platform = mockk(relaxed = true)

    private var resultReceived: SettingsComponent.Result? = null

    @Before
    fun setUpComponent() {
        mockkStatic("org.jetbrains.compose.resources.StringResourcesKt")
        mockkStatic("org.jetbrains.compose.resources.StringArrayResourcesKt")
        coEvery { getString(any()) } returns "Mock String"
        coEvery { getString(any(), *anyVararg()) } returns "Mock String"

        coEvery { getStringArray(Res.array.pref_color_theme_titles) } returns listOf("Light", "Dark")
        coEvery { getStringArray(Res.array.pref_typeface_size_titles) } returns listOf("Small", "Large")
        coEvery { getStringArray(Res.array.font_size_values_array) } returns listOf("14", "18")
        coEvery { getStringArray(Res.array.content_server_names_array) } returns listOf("WACS", "Gitea")
        coEvery { getStringArray(Res.array.content_server_values_array) } returns listOf("wacs_value", "gitea_value")
        coEvery { getStringArray(Res.array.pref_backup_interval_titles) } returns listOf("Never", "5 Minutes")
        coEvery { getStringArray(Res.array.backup_intervals_values_array) } returns listOf("-1", "300")
        coEvery { getStringArray(Res.array.pref_logging_level_titles) } returns listOf("Debug", "Info", "Warning")
        coEvery { getStringArray(Res.array.pref_typeface_titles) } returns listOf("System Default")

        val mockUrls = listOf("http://api1.url", "http://api2.url")
        coEvery { getStringArray(Res.array.content_server_git_server_api_values_array) } returns mockUrls
        coEvery { getStringArray(Res.array.content_server_media_server_values_array) } returns mockUrls
        coEvery { getStringArray(Res.array.content_server_reader_server_values_array) } returns mockUrls
        coEvery { getStringArray(Res.array.content_server_account_create_urls_array) } returns mockUrls
        coEvery { getStringArray(Res.array.content_server_lang_names_url_array) } returns mockUrls
        coEvery { getStringArray(Res.array.content_server_index_sqlite_url_array) } returns mockUrls

        every { platform.info } returns AppInfo(
            versionName = "1.0.0",
            versionCode = 1,
            model = "Mock Model",
            device = "Mock Device",
            manufacturer = "Mock Manufacturer",
            generator = "Mock Generator"
        )
        every { directoryProvider.logFile } returns mockk<File>(relaxed = true)
        every { typography.getFontNames() } returns listOf("font1.ttf")

        every { preference.getPref(any(), any(), any()) } answers { args[1]!! }

        startKoin {
            modules(
                module {
                    single { checkForLatestRelease }
                    single { profile }
                    single { logout }
                    single { migrateTranslations }
                    single { preference }
                    single { directoryProvider }
                    single { typography }
                    single { backupScheduler }
                    single { platform }
                }
            )
        }
        resultReceived = null
    }

    private fun createComponent(): DefaultSettingsComponent = createComponent { context ->
        DefaultSettingsComponent(
            componentContext = context,
            onResult = { resultReceived = it }
        )
    }

    @Test
    fun testInitializationLoadsPreferences() {
        runBlocking {
            every { preference.getPref(Preference.KEY_PREF_COLOR_THEME, any(), String::class) } returns "dark"
            every { preference.getPref(Preference.KEY_PREF_TRANSLATION_TYPEFACE, any(), String::class) } returns "font1.ttf"

            val component = createComponent()
            component.state.awaitState { it.currentThemeValue.isNotEmpty() }

            assertEquals("dark", component.state.value.currentThemeValue)
            assertEquals("font1.ttf", component.state.value.currentTranslationFontValue)
        }
    }

    @Test
    fun testUpdateColorTheme() {
        runBlocking {
            val component = createComponent()
            component.state.awaitState { it.themeValues.isNotEmpty() }

            component.updateColorTheme("dark")

            assertEquals("dark", component.state.value.currentThemeValue)
            assertEquals(SettingsComponent.Result.ThemeUpdated("dark"), resultReceived)
        }
    }

    @Test
    fun testUpdateTranslationTypeface() {
        runBlocking {
            val component = createComponent()
            component.state.awaitState { it.availableFonts.isNotEmpty() }

            component.updateTranslationTypeface("font1.ttf")

            assertEquals("font1.ttf", component.state.value.currentTranslationFontValue)
        }
    }

    @Test
    fun testLoadsSystemFontsWithDisplayNames() {
        runBlocking {
            every { typography.getFontNames() } returns listOf("font1.ttf", "/sys/Arial.ttf")
            every { typography.getSystemFonts() } returns listOf(
                org.bibletranslationtools.writer.core.SystemFont("Arial", "/sys/Arial.ttf")
            )

            val component = createComponent()
            component.state.awaitState { it.availableFonts.size > 1 }

            val fonts = component.state.value.availableFonts
            val names = component.state.value.availableFontNames
            assertTrue("/sys/Arial.ttf" in fonts, "system font path listed")
            assertEquals("Arial", names[fonts.indexOf("/sys/Arial.ttf")])
        }
    }

    @Test
    fun testFontsSortedAlphabeticallyByDisplayName() {
        runBlocking {
            coEvery { getStringArray(Res.array.pref_typeface_titles) } returns listOf("Zeta", "Alpha")
            every { typography.getFontNames() } returns listOf("zfont.ttf", "afont.ttf", "/sys/Mango.ttf")
            every { typography.getSystemFonts() } returns listOf(
                org.bibletranslationtools.writer.core.SystemFont("Mango", "/sys/Mango.ttf")
            )

            val component = createComponent()
            component.state.awaitState { it.availableFonts.size > 2 }

            assertEquals(
                listOf("Alpha", "Mango", "Zeta"),
                component.state.value.availableFontNames
            )
            assertEquals(
                listOf("afont.ttf", "/sys/Mango.ttf", "zfont.ttf"),
                component.state.value.availableFonts
            )
        }
    }

    @Test
    fun testImportFontCopiesFileAndRefreshesList() {
        runBlocking {
            val file: PlatformFile = mockk(relaxed = true)
            coEvery { directoryProvider.copyFile(any(), any()) } returns mockk(relaxed = true)
            every { typography.getFontNames() } returnsMany listOf(
                listOf("font1.ttf"),
                listOf("font1.ttf", "/fonts/Custom.ttf")
            )
            every { typography.getSystemFonts() } returns listOf(
                org.bibletranslationtools.writer.core.SystemFont("Custom", "/fonts/Custom.ttf")
            )

            val component = createComponent()
            component.state.awaitState { it.availableFonts.isNotEmpty() }

            component.importFont(file)
            component.state.awaitState { state -> state.availableFonts.any { it == "/fonts/Custom.ttf" } }

            coVerify { directoryProvider.copyFile(file, any()) }
            assertTrue("/fonts/Custom.ttf" in component.state.value.availableFonts)
        }
    }

    @Test
    fun testImportFontShowsConfirmationDialogWithFontName() {
        runBlocking {
            val file: PlatformFile = mockk(relaxed = true)
            val importedFile = mockk<File>(relaxed = true)
            every { importedFile.absolutePath } returns "/fonts/Custom.ttf"
            coEvery { directoryProvider.copyFile(any(), any()) } returns importedFile
            every { typography.getFontNames() } returnsMany listOf(
                listOf("font1.ttf"),
                listOf("font1.ttf", "/fonts/Custom.ttf")
            )
            every { typography.getSystemFonts() } returns listOf(
                org.bibletranslationtools.writer.core.SystemFont("Custom", "/fonts/Custom.ttf")
            )

            val component = createComponent()
            component.state.awaitState { it.availableFonts.isNotEmpty() }

            component.importFont(file)
            component.state.awaitState { it.importedFontName != null }
            assertEquals("Custom", component.state.value.importedFontName)

            component.dismissImportFontDialog()
            assertEquals(null, component.state.value.importedFontName)
        }
    }

    @Test
    fun testUpdateTypefaceShowsDisplayName() {
        runBlocking {
            every { typography.getFontNames() } returns listOf("font1.ttf", "/sys/Arial.ttf")
            every { typography.getSystemFonts() } returns listOf(
                org.bibletranslationtools.writer.core.SystemFont("Arial", "/sys/Arial.ttf")
            )

            val component = createComponent()
            component.state.awaitState { it.availableFonts.size > 1 }

            component.updateTranslationTypeface("/sys/Arial.ttf")
            assertEquals("Arial", component.state.value.currentTranslationFontName)

            component.updateSourceTypeface("/sys/Arial.ttf")
            assertEquals("Arial", component.state.value.currentSourceFontName)
        }
    }

    @Test
    fun testUpdateTranslationFontSize() {
        runBlocking {
            val component = createComponent()
            component.state.awaitState { it.fontSizeValues.isNotEmpty() }

            component.updateTranslationFontSize("18")

            assertEquals("18", component.state.value.currentTranslationFontSizeValue)
        }
    }

    @Test
    fun testCheckForLatestRelease() {
        runBlocking {
            val mockResult = CheckForLatestRelease.Result(null)
            coEvery { checkForLatestRelease.execute() } returns mockResult

            val component = createComponent()
            component.checkForLatestRelease()

            component.state.awaitState { it.releaseResult != null }
            assertEquals(mockResult, component.state.value.releaseResult)
        }
    }

    @Test
    fun testMigrateOldAppData() {
        runBlocking {
            val tempFile = File.createTempFile("migration_dir", "")
            tempFile.deleteOnExit()
            val platformFile = PlatformFile(tempFile)

            val component = createComponent()
            component.migrateOldAppData(platformFile)

            component.state.awaitState { it.migrationFinished }
            assertTrue(component.state.value.migrationFinished)

            tempFile.delete()
        }
    }

    @Test
    fun testUpdateBackupInterval() {
        runBlocking {
            val component = createComponent()
            component.state.awaitState { it.backupIntervalValues.isNotEmpty() }

            component.updateBackupInterval("300")

            assertEquals("300", component.state.value.currentBackupIntervalValue)
            coVerify { backupScheduler.restart(300) }
        }
    }

    @Test
    fun testOnContentServerChanged() {
        runBlocking {
            val component = createComponent()
            component.state.awaitState { it.contentServerValues.isNotEmpty() }

            component.onContentServerChanged("gitea_value")

            component.state.awaitState { it.currentContentServerValue == "gitea_value" }
            assertEquals("gitea_value", component.state.value.currentContentServerValue)
        }
    }

    @Test
    fun testCallbacks() {
        runBlocking {
            val component = createComponent()

            component.onNavigateBack()
            assertEquals(SettingsComponent.Result.NavigateBack, resultReceived)

            component.openDeveloperTools()
            assertEquals(SettingsComponent.Result.OpenDeveloperTools, resultReceived)

            component.onMigrationFinished()
            assertEquals(SettingsComponent.Result.MigrationFinished, resultReceived)

            component.onLogout()
            assertEquals(SettingsComponent.Result.Logout, resultReceived)
        }
    }
}
