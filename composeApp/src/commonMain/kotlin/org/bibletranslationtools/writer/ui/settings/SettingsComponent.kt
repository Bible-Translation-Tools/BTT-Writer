package org.bibletranslationtools.writer.ui.settings

import btt_writer.composeapp.generated.resources.Res
import btt_writer.composeapp.generated.resources.backup_intervals_values_array
import btt_writer.composeapp.generated.resources.checking_for_updates
import btt_writer.composeapp.generated.resources.content_server_account_create_urls_array
import btt_writer.composeapp.generated.resources.content_server_git_server_api_values_array
import btt_writer.composeapp.generated.resources.content_server_git_server_port_values_array
import btt_writer.composeapp.generated.resources.content_server_index_sqlite_url_array
import btt_writer.composeapp.generated.resources.content_server_lang_names_url_array
import btt_writer.composeapp.generated.resources.content_server_media_server_values_array
import btt_writer.composeapp.generated.resources.content_server_names_array
import btt_writer.composeapp.generated.resources.content_server_reader_server_values_array
import btt_writer.composeapp.generated.resources.content_server_values_array
import btt_writer.composeapp.generated.resources.font_size_values_array
import btt_writer.composeapp.generated.resources.log_out
import btt_writer.composeapp.generated.resources.migrating_translations
import btt_writer.composeapp.generated.resources.pref_backup_interval_titles
import btt_writer.composeapp.generated.resources.pref_color_theme_titles
import btt_writer.composeapp.generated.resources.pref_default_backup_interval
import btt_writer.composeapp.generated.resources.pref_default_color_theme
import btt_writer.composeapp.generated.resources.pref_default_create_account_url
import btt_writer.composeapp.generated.resources.pref_default_git_server_port
import btt_writer.composeapp.generated.resources.pref_default_gogs_api
import btt_writer.composeapp.generated.resources.pref_default_index_sqlite_url
import btt_writer.composeapp.generated.resources.pref_default_language_url
import btt_writer.composeapp.generated.resources.pref_default_logging_level
import btt_writer.composeapp.generated.resources.pref_default_media_server
import btt_writer.composeapp.generated.resources.pref_default_reader_server
import btt_writer.composeapp.generated.resources.pref_default_tm_url
import btt_writer.composeapp.generated.resources.pref_default_translation_typeface
import btt_writer.composeapp.generated.resources.pref_default_typeface_size
import btt_writer.composeapp.generated.resources.pref_logging_level_titles
import btt_writer.composeapp.generated.resources.pref_typeface_size_titles
import btt_writer.composeapp.generated.resources.pref_typeface_titles
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.doOnDestroy
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.bibletranslationtools.logger.LogLevel
import org.bibletranslationtools.logger.Logger
import org.bibletranslationtools.writer.DirectoryProvider
import org.bibletranslationtools.writer.Platform
import org.bibletranslationtools.writer.core.ComponentScope
import org.bibletranslationtools.writer.core.Profile
import org.bibletranslationtools.writer.core.Progress
import org.bibletranslationtools.writer.core.ProgressManager
import org.bibletranslationtools.writer.core.ProgressOwner
import org.bibletranslationtools.writer.core.TaskHandle
import org.bibletranslationtools.writer.core.Typography
import org.bibletranslationtools.writer.core.launchWithProgress
import org.bibletranslationtools.writer.data.Preference
import org.bibletranslationtools.writer.data.getPref
import org.bibletranslationtools.writer.data.setPref
import org.bibletranslationtools.writer.usecases.CheckForLatestRelease
import org.bibletranslationtools.writer.usecases.DownloadLatestRelease
import org.bibletranslationtools.writer.usecases.GogsLogout
import org.bibletranslationtools.writer.usecases.MigrateTranslations
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.getStringArray
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

interface SettingsComponent {

    val state: StateFlow<State>
    val progress: StateFlow<Progress?>
    val event: Flow<Event>

    val appVersion: String

    fun migrateOldAppData(dir: PlatformFile)
    fun checkForLatestRelease()
    fun setCheckHardwareEnabled(enabled: Boolean)
    fun setTmLinksEnabled(enabled: Boolean)
    fun downloadLatestRelease(release: CheckForLatestRelease.Release)
    fun dismissUpdateResultDialog()
    fun updateColorTheme(newValue: String)
    fun updateTranslationTypeface(newFileName: String)
    fun updateTranslationFontSize(newValue: String)
    fun updateSourceTypeface(newValue: String)
    fun updateSourceFontSize(newValue: String)
    fun onContentServerChanged(newValue: String)
    fun updateGitServerPort(newValue: String)
    fun updateGogsApiUrl(newValue: String)
    fun updateMediaServerUrl(newValue: String)
    fun updateReaderServerUrl(newValue: String)
    fun updateAccountCreationUrl(newValue: String)
    fun updateLanguageUrl(newValue: String)
    fun updateIndexSqliteUrl(newValue: String)
    fun updateTmLinksUrl(newValue: String)
    fun updateBackupInterval(newValue: String)
    fun updateLoggingLevel(newValue: String)

    fun onNavigateBack()
    fun openDeveloperTools()
    fun onMigrationFinished()
    fun onLogout()

    data class State(
        // General Prefs
        val themeNames: List<String> = emptyList(),
        val themeValues: List<String> = emptyList(),
        val currentThemeValue: String = "",
        val currentThemeName: String = "",

        // Font Data
        val availableFontNames: List<String> = emptyList(),
        val availableFonts: List<String> = emptyList(),
        val fontSizeNames: List<String> = emptyList(),
        val fontSizeValues: List<String> = emptyList(),
        val currentTranslationFontValue: String = "",
        val currentTranslationFontName: String = "",
        val currentTranslationFontSizeValue: String = "",
        val currentTranslationFontSizeName: String = "",
        val currentSourceFontValue: String = "",
        val currentSourceFontName: String = "",
        val currentSourceFontSizeValue: String = "",
        val currentSourceFontSizeName: String = "",

        // Server Prefs
        val contentServerNames: List<String> = emptyList(),
        val contentServerValues: List<String> = emptyList(),
        val currentGogsApiUrl: String = "",
        val currentContentServerValue: String = "",
        val currentContentServerName: String = "",
        val gitServerPort: String = "",
        val mediaServerUrl: String = "",
        val readerServerUrl: String = "",
        val accountCreationUrl: String = "",
        val languagesUrl: String = "",
        val indexSqliteUrl: String = "",
        val tmLinksUrl: String = "",

        // Advanced Prefs
        val checkHardwareEnabled: Boolean = false,
        val tmLinksEnabled: Boolean = false,
        val backupIntervalNames: List<String> = emptyList(),
        val backupIntervalValues: List<String> = emptyList(),
        val currentBackupIntervalValue: String = "-1",
        val currentBackupIntervalName: String = "",
        val loggingLevelNames: List<String> = emptyList(),
        val loggingLevelValues: List<String> = emptyList(),
        val currentLoggingLevelValue: String = "",
        val currentLoggingLevelName: String = "",

        val releaseResult: CheckForLatestRelease.Result? = null,
        val migrationFinished: Boolean = false
    )

    sealed interface Event {
        data object OnLogout : Event
    }

    sealed interface Result {
        data object NavigateBack : Result
        data object OpenDeveloperTools : Result
        data object MigrationFinished : Result
        data object Logout : Result
        data class ThemeUpdated(val theme: String) : Result
    }
}

class DefaultSettingsComponent(
    componentContext: ComponentContext,
    private val onResult: (SettingsComponent.Result) -> Unit
) : SettingsComponent, ComponentContext by componentContext,
    KoinComponent, ProgressOwner, ComponentScope {

    private val checkForLatestRelease: CheckForLatestRelease by inject()
    private val downloadLatestRelease: DownloadLatestRelease by inject()
    private val profile: Profile by inject()
    private val logout: GogsLogout by inject()
    private val migrateTranslations: MigrateTranslations by inject()
    private val preference: Preference by inject()
    private val directoryProvider: DirectoryProvider by inject()
    private val typography: Typography by inject()
    //private val backupController: BackupController by inject()
    private val platform: Platform by inject()


    override val coroutineScope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())

    private val progressManager = ProgressManager(coroutineScope)
    override val progress get() = progressManager.progress

    private val _state = MutableStateFlow(SettingsComponent.State())
    override val state: StateFlow<SettingsComponent.State> = _state.asStateFlow()

    private val _event = Channel<SettingsComponent.Event>(Channel.BUFFERED)
    override val event = _event.receiveAsFlow()

    override val appVersion: String
        get() = "${platform.info.versionName} - ${platform.info.versionCode}"

    init {
        loadInitialPreferences()
        loadTypefaces()

        lifecycle.doOnDestroy {
            coroutineScope.cancel()
        }
    }

    override suspend fun runTask(message: String?, block: suspend (TaskHandle) -> Unit) {
        progressManager.runTask(message, block)
    }

    private fun loadInitialPreferences() {
        launchWithProgress {
            val themeNames = getStringArray(Res.array.pref_color_theme_titles)
            val themeValues = Preference.Theme.entries.map { it.value }
            val themeValue = preference.getPref(
                Preference.KEY_PREF_COLOR_THEME,
                getString(Res.string.pref_default_color_theme)
            )
            val themeIndex = themeValues.indexOf(themeValue).takeIf { it >= 0 } ?: 1
            val themeName = themeNames.getOrNull(themeIndex) ?: themeValue

            // Fonts

            val targetFontValue = preference.getPref(
                Preference.KEY_PREF_TRANSLATION_TYPEFACE,
                getString(Res.string.pref_default_translation_typeface)
            )
            val sourceFontValue = preference.getPref(
                Preference.KEY_PREF_SOURCE_TYPEFACE,
                getString(Res.string.pref_default_translation_typeface)
            )

            val sizeNames = getStringArray(Res.array.pref_typeface_size_titles)
            val sizeValues = getStringArray(Res.array.font_size_values_array)

            val translationSizeValue = preference.getPref(
                Preference.KEY_PREF_TRANSLATION_TYPEFACE_SIZE,
                getString(Res.string.pref_default_typeface_size)
            )
            val sourceSizeValue = preference.getPref(
                Preference.KEY_PREF_SOURCE_TYPEFACE_SIZE,
                getString(Res.string.pref_default_typeface_size)
            )

            val translationSizeIndex = sizeValues.indexOf(translationSizeValue).takeIf { it >= 0 } ?: 1
            val translationSizeName = sizeNames.getOrNull(translationSizeIndex) ?: translationSizeValue
            val sourceSizeIndex = sizeValues.indexOf(sourceSizeValue).takeIf { it >= 0 } ?: 1
            val sourceSizeName = sizeNames.getOrNull(sourceSizeIndex) ?: sourceSizeValue

            // Server

            val serverNames = getStringArray(Res.array.content_server_names_array)
            val serverValues = getStringArray(Res.array.content_server_values_array)

            val savedServerValue = preference.getPref(
                Preference.KEY_PREF_CONTENT_SERVER,
                serverValues.firstOrNull() ?: "wacs_value"
            )
            val savedIndex = serverValues.indexOf(savedServerValue).takeIf { it >= 0 } ?: 0
            val savedServerName = serverNames.getOrNull(savedIndex) ?: ""
            val gitPort = preference.getPref(
                Preference.KEY_PREF_GIT_SERVER_PORT,
                getString(Res.string.pref_default_git_server_port)
            )
            val gogsApiUrl = preference.getPref(
                Preference.KEY_PREF_GOGS_API,
                getString(Res.string.pref_default_gogs_api)
            )
            val mediaServerUrl = preference.getPref(
                Preference.KEY_PREF_MEDIA_SERVER,
                getString(Res.string.pref_default_media_server)
            )
            val readerServerUrl = preference.getPref(
                Preference.KEY_PREF_READER_SERVER,
                getString(Res.string.pref_default_reader_server)
            )
            val accountCreationUrl = preference.getPref(
                Preference.KEY_PREF_CREATE_ACCOUNT_URL,
                getString(Res.string.pref_default_create_account_url)
            )
            val languagesUrl = preference.getPref(
                Preference.KEY_PREF_LANGUAGES_URL,
                getString(Res.string.pref_default_language_url)
            )
            val indexSqliteUrl = preference.getPref(
                Preference.KEY_PREF_INDEX_SQLITE_URL,
                getString(Res.string.pref_default_index_sqlite_url)
            )
            val tmLinksUrl = preference.getPref(
                Preference.KEY_PREF_TM_URL,
                getString(Res.string.pref_default_tm_url)
            )

            // Advanced

            val checkHardwareEnabled = preference.getPref(
                Preference.KEY_PREF_CHECK_HARDWARE,
                true
            )
            val tmLinksEnabled = preference.getPref(
                Preference.KEY_PREF_ENABLE_TM_LINKS,
                false
            )

            val intervalNames = getStringArray(Res.array.pref_backup_interval_titles)
            val intervalValues = getStringArray(Res.array.backup_intervals_values_array)
            val savedIntervalValue = preference.getPref(
                Preference.KEY_PREF_BACKUP_INTERVAL,
                getString(Res.string.pref_default_backup_interval)
            )
            val intervalIndex = intervalValues.indexOf(savedIntervalValue).takeIf { it >= 0 } ?: 1
            val savedIntervalName = intervalNames.getOrNull(intervalIndex) ?: savedIntervalValue

            val loggingNames = getStringArray(Res.array.pref_logging_level_titles)
            val loggingValues = LogLevel.entries.map { it.label }
            val savedLoggingValue = preference.getPref(
                Preference.KEY_PREF_LOGGING_LEVEL,
                getString(Res.string.pref_default_logging_level)
            )
            val loggingIndex = loggingValues.indexOf(savedLoggingValue).takeIf { it >= 0 } ?: 2
            val savedLoggingName = loggingNames.getOrNull(loggingIndex) ?: savedLoggingValue

            _state.update { state ->
                state.copy(
                    themeNames = themeNames,
                    themeValues = themeValues,
                    currentThemeValue = themeValue,
                    currentThemeName = themeName,
                    fontSizeNames = sizeNames,
                    fontSizeValues = sizeValues,
                    currentTranslationFontValue = targetFontValue,
                    currentTranslationFontSizeValue = translationSizeValue,
                    currentTranslationFontSizeName = translationSizeName,
                    currentSourceFontValue = sourceFontValue,
                    currentSourceFontSizeValue = sourceSizeValue,
                    currentSourceFontSizeName = sourceSizeName,
                    contentServerNames = serverNames,
                    contentServerValues = serverValues,
                    currentContentServerValue = savedServerValue,
                    currentContentServerName = savedServerName,
                    gitServerPort = gitPort,
                    currentGogsApiUrl = gogsApiUrl,
                    mediaServerUrl = mediaServerUrl,
                    readerServerUrl = readerServerUrl,
                    accountCreationUrl = accountCreationUrl,
                    languagesUrl = languagesUrl,
                    indexSqliteUrl = indexSqliteUrl,
                    tmLinksUrl = tmLinksUrl,
                    checkHardwareEnabled = checkHardwareEnabled,
                    tmLinksEnabled = tmLinksEnabled,
                    backupIntervalNames = intervalNames,
                    backupIntervalValues = intervalValues,
                    currentBackupIntervalValue = savedIntervalValue,
                    currentBackupIntervalName = savedIntervalName,
                    loggingLevelNames = loggingNames,
                    loggingLevelValues = loggingValues,
                    currentLoggingLevelValue = savedLoggingValue,
                    currentLoggingLevelName = savedLoggingName
                )
            }
        }
    }

    private fun loadTypefaces() {
        launchWithProgress {
            val fontNames = getStringArray(Res.array.pref_typeface_titles)
            val loadedFonts = typography.getFontNames()
            val defaultFont = getString(Res.string.pref_default_translation_typeface)

            _state.update { state ->
                val translationIndex = loadedFonts.indexOf(state.currentTranslationFontValue)
                val translationFontName = fontNames.getOrNull(translationIndex) ?: defaultFont
                val sourceIndex = loadedFonts.indexOf(state.currentSourceFontValue)
                val sourceFontName = fontNames.getOrNull(sourceIndex) ?: defaultFont

                state.copy(
                    availableFonts = loadedFonts,
                    availableFontNames = fontNames,
                    currentTranslationFontName = translationFontName,
                    currentSourceFontName = sourceFontName
                )
            }
        }
    }

    override fun updateColorTheme(newValue: String) {
        preference.setPref(Preference.KEY_PREF_COLOR_THEME, newValue)

        val index = _state.value.themeValues.indexOf(newValue)
        val newName = _state.value.themeNames.getOrNull(index) ?: newValue

        _state.update {
            it.copy(
                currentThemeValue = newValue,
                currentThemeName = newName
            )
        }

        onResult(SettingsComponent.Result.ThemeUpdated(newValue))
    }

    override fun updateTranslationTypeface(newFileName: String) {
        preference.setPref(Preference.KEY_PREF_TRANSLATION_TYPEFACE, newFileName)

        val newName = _state.value.availableFonts.find {
            it == newFileName
        } ?: "Default"

        _state.update {
            it.copy(
                currentTranslationFontValue = newFileName,
                currentTranslationFontName = newName
            )
        }
    }

    override fun updateTranslationFontSize(newValue: String) {
        preference.setPref(Preference.KEY_PREF_TRANSLATION_TYPEFACE_SIZE, newValue)

        val index = _state.value.fontSizeValues.indexOf(newValue)
        val newName = _state.value.fontSizeNames.getOrNull(index) ?: newValue

        _state.update {
            it.copy(
                currentTranslationFontSizeValue = newValue,
                currentTranslationFontSizeName = newName
            )
        }
    }

    override fun updateSourceFontSize(newValue: String) {
        preference.setPref(Preference.KEY_PREF_SOURCE_TYPEFACE_SIZE, newValue)

        val index = _state.value.fontSizeValues.indexOf(newValue)
        val newName = _state.value.fontSizeNames.getOrNull(index) ?: newValue

        _state.update {
            it.copy(
                currentSourceFontSizeValue = newValue,
                currentSourceFontSizeName = newName
            )
        }
    }

    override fun updateSourceTypeface(newValue: String) {
        preference.setPref(Preference.KEY_PREF_SOURCE_TYPEFACE, newValue)

        val newName = _state.value.availableFonts.find { it == newValue } ?: "Default"

        _state.update {
            it.copy(
                currentSourceFontValue = newValue,
                currentSourceFontName = newName
            )
        }
    }

    override fun checkForLatestRelease() {
        launchWithProgress(Res.string.checking_for_updates) {
            val result = withContext(Dispatchers.IO) {
                checkForLatestRelease.execute()
            }
            _state.update {
                it.copy(releaseResult = result)
            }
        }
    }

    override fun dismissUpdateResultDialog() {
        _state.update { it.copy(releaseResult = null) }
    }

    override fun migrateOldAppData(dir: PlatformFile) {
        launchWithProgress(Res.string.migrating_translations) { handle ->
            withContext(Dispatchers.IO) {
                migrateTranslations.execute(dir) { progress, message ->
                    handle.update(progress, message)
                }
            }
            _state.update {
                it.copy(migrationFinished = true)
            }
        }
    }

    override fun updateGitServerPort(newValue: String) {
        preference.setPref(Preference.KEY_PREF_GIT_SERVER_PORT, newValue)
        _state.update { it.copy(gitServerPort = newValue) }
    }

    override fun updateGogsApiUrl(newValue: String) {
        preference.setPref(Preference.KEY_PREF_GOGS_API, newValue)
        _state.update { it.copy(currentGogsApiUrl = newValue) }
    }

    override fun updateMediaServerUrl(newValue: String) {
        preference.setPref(Preference.KEY_PREF_MEDIA_SERVER, newValue)
        _state.update { it.copy(mediaServerUrl = newValue) }
    }

    override fun updateReaderServerUrl(newValue: String) {
        preference.setPref(Preference.KEY_PREF_READER_SERVER, newValue)
        _state.update { it.copy(readerServerUrl = newValue) }
    }

    override fun updateAccountCreationUrl(newValue: String) {
        preference.setPref(Preference.KEY_PREF_CREATE_ACCOUNT_URL, newValue)
        _state.update { it.copy(accountCreationUrl = newValue) }
    }

    override fun updateLanguageUrl(newValue: String) {
        preference.setPref(Preference.KEY_PREF_LANGUAGES_URL, newValue)
        _state.update { it.copy(languagesUrl = newValue) }
    }

    override fun updateIndexSqliteUrl(newValue: String) {
        preference.setPref(Preference.KEY_PREF_INDEX_SQLITE_URL, newValue)
        _state.update { it.copy(indexSqliteUrl = newValue) }
    }

    override fun updateTmLinksUrl(newValue: String) {
        preference.setPref(Preference.KEY_PREF_TM_URL, newValue)
        _state.update { it.copy(tmLinksUrl = newValue) }
    }

    override fun setCheckHardwareEnabled(enabled: Boolean) {
        preference.setPref(Preference.KEY_PREF_CHECK_HARDWARE, enabled)
        _state.update { it.copy(checkHardwareEnabled = enabled) }
    }

    override fun setTmLinksEnabled(enabled: Boolean) {
        preference.setPref(Preference.KEY_PREF_ENABLE_TM_LINKS, enabled)
        _state.update { it.copy(tmLinksEnabled = enabled) }
    }

    override fun updateBackupInterval(newValue: String) {
        preference.setPref(Preference.KEY_PREF_BACKUP_INTERVAL, newValue)

        val index = _state.value.backupIntervalValues.indexOf(newValue)
        val newName = _state.value.backupIntervalNames.getOrNull(index) ?: newValue

        _state.update {
            it.copy(
                currentBackupIntervalValue = newValue,
                currentBackupIntervalName = newName
            )
        }

        //backupController.restartServiceIfRunning()
    }

    override fun updateLoggingLevel(newValue: String) {
        preference.setPref(Preference.KEY_PREF_LOGGING_LEVEL, newValue)

        val index = _state.value.loggingLevelValues.indexOf(newValue)
        val newName = _state.value.loggingLevelNames.getOrNull(index) ?: newValue

        _state.update {
            it.copy(
                currentLoggingLevelValue = newValue,
                currentLoggingLevelName = newName
            )
        }

        Logger.configure(
            directoryProvider.logFile,
            LogLevel.getLevel(newValue)
        )
    }

    override fun downloadLatestRelease(release: CheckForLatestRelease.Release) {
        downloadLatestRelease.execute(release)
    }

    override fun onContentServerChanged(newValue: String) {
        coroutineScope.launch {
            val serverValues = getStringArray(Res.array.content_server_values_array)
            val serverNames = getStringArray(Res.array.content_server_names_array)
            val gitPorts = getStringArray(Res.array.content_server_git_server_port_values_array)
            val gitApiUrls = getStringArray(Res.array.content_server_git_server_api_values_array)
            val mediaUrls = getStringArray(Res.array.content_server_media_server_values_array)
            val readerUrls = getStringArray(Res.array.content_server_reader_server_values_array)
            val createAccountUrls = getStringArray(Res.array.content_server_account_create_urls_array)
            val langNameUrls = getStringArray(Res.array.content_server_lang_names_url_array)
            val indexSqliteUrls = getStringArray(Res.array.content_server_index_sqlite_url_array)

            val index = serverValues.indexOf(newValue)
            if (index == -1) return@launch

            preference.setPref(Preference.KEY_PREF_CONTENT_SERVER, newValue)
            preference.setPref(Preference.KEY_PREF_GIT_SERVER_PORT, gitPorts[index])
            preference.setPref(Preference.KEY_PREF_GOGS_API, gitApiUrls[index])
            preference.setPref(Preference.KEY_PREF_MEDIA_SERVER, mediaUrls[index])
            preference.setPref(Preference.KEY_PREF_READER_SERVER, readerUrls[index])
            preference.setPref(Preference.KEY_PREF_CREATE_ACCOUNT_URL, createAccountUrls[index])
            preference.setPref(Preference.KEY_PREF_LANGUAGES_URL, langNameUrls[index])
            preference.setPref(Preference.KEY_PREF_INDEX_SQLITE_URL, indexSqliteUrls[index])

            _state.update { state ->
                state.copy(
                    currentContentServerValue = newValue,
                    currentContentServerName = serverNames[index],
                    gitServerPort = gitPorts[index],
                    mediaServerUrl = mediaUrls[index],
                    currentGogsApiUrl = gitApiUrls[index],
                    readerServerUrl = readerUrls[index],
                    accountCreationUrl = createAccountUrls[index],
                    languagesUrl = langNameUrls[index],
                    indexSqliteUrl = indexSqliteUrls[index]
                )
            }

            logout()
        }
    }

    override fun onNavigateBack() {
        onResult(SettingsComponent.Result.NavigateBack)
    }

    override fun openDeveloperTools() {
        onResult(SettingsComponent.Result.OpenDeveloperTools)
    }

    override fun onMigrationFinished() {
        onResult(SettingsComponent.Result.MigrationFinished)
    }

    override fun onLogout() {
        onResult(SettingsComponent.Result.Logout)
    }

    private fun logout() {
        if (profile.gogsUser != null) {
            launchWithProgress(Res.string.log_out) {
                withContext(Dispatchers.IO) {
                    logout.execute()
                    profile.logout()
                }
                _event.trySend(SettingsComponent.Event.OnLogout)
            }
        }
    }
}