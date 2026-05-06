package org.bibletranslationtools.writer.ui.settings

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import btt_writer.composeapp.generated.resources.Res
import btt_writer.composeapp.generated.resources.action_settings
import btt_writer.composeapp.generated.resources.apk_update_available
import btt_writer.composeapp.generated.resources.check_for_app_updates
import btt_writer.composeapp.generated.resources.check_for_updates
import btt_writer.composeapp.generated.resources.content_server
import btt_writer.composeapp.generated.resources.description_migrate_old_app
import btt_writer.composeapp.generated.resources.download_latest_apk
import btt_writer.composeapp.generated.resources.have_latest_app_update
import btt_writer.composeapp.generated.resources.header_advanced
import btt_writer.composeapp.generated.resources.header_general
import btt_writer.composeapp.generated.resources.header_legal
import btt_writer.composeapp.generated.resources.header_synchronization
import btt_writer.composeapp.generated.resources.label_ok
import btt_writer.composeapp.generated.resources.license_pdf
import btt_writer.composeapp.generated.resources.migrating_complete
import btt_writer.composeapp.generated.resources.pref_description_check_hardware_requirements
import btt_writer.composeapp.generated.resources.pref_description_enable_tm_links
import btt_writer.composeapp.generated.resources.pref_title_check_hardware_requirements
import btt_writer.composeapp.generated.resources.pref_title_enable_tm_links
import btt_writer.composeapp.generated.resources.software_licenses
import btt_writer.composeapp.generated.resources.statement_of_faith
import btt_writer.composeapp.generated.resources.title_color_theme
import btt_writer.composeapp.generated.resources.title_create_account_url
import btt_writer.composeapp.generated.resources.title_developer_tools
import btt_writer.composeapp.generated.resources.title_git_server_port
import btt_writer.composeapp.generated.resources.title_gogs_api
import btt_writer.composeapp.generated.resources.title_index_sqlite_url
import btt_writer.composeapp.generated.resources.title_language_url
import btt_writer.composeapp.generated.resources.title_logging_level
import btt_writer.composeapp.generated.resources.title_media_server
import btt_writer.composeapp.generated.resources.title_migrate_old_app
import btt_writer.composeapp.generated.resources.title_reader_server
import btt_writer.composeapp.generated.resources.title_source_typeface
import btt_writer.composeapp.generated.resources.title_source_typeface_size
import btt_writer.composeapp.generated.resources.title_tm_url
import btt_writer.composeapp.generated.resources.title_translation_typeface
import btt_writer.composeapp.generated.resources.title_typeface_size
import btt_writer.composeapp.generated.resources.translation_guidlines
import btt_writer.composeapp.generated.resources.version
import btt_writer.composeapp.generated.resources.view_license_agreement
import btt_writer.composeapp.generated.resources.view_software_licenses
import btt_writer.composeapp.generated.resources.view_statement_of_faith
import btt_writer.composeapp.generated.resources.view_translation_guidelines
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.FileKitDialogSettings
import io.github.vinceglb.filekit.dialogs.compose.rememberDirectoryPickerLauncher
import org.bibletranslationtools.writer.ui.dialogs.BaseDialog
import org.bibletranslationtools.writer.ui.dialogs.ConfirmDialog
import org.bibletranslationtools.writer.ui.dialogs.LegalDocumentDialog
import org.bibletranslationtools.writer.ui.dialogs.ProgressDialog
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    component: SettingsComponent
) {
    val state by component.state.collectAsStateWithLifecycle()
    val progress by component.progress.collectAsStateWithLifecycle()

    var showThemeDialog by rememberSaveable { mutableStateOf(false) }
    var showGogsApiDialog by rememberSaveable { mutableStateOf(false) }
    var showTranslationFontDialog by rememberSaveable { mutableStateOf(false) }
    var showTranslationFontSizeDialog by rememberSaveable { mutableStateOf(false) }
    var showSourceFontDialog by rememberSaveable { mutableStateOf(false) }
    var showSourceFontSizeDialog by rememberSaveable { mutableStateOf(false) }

    var showContentServerDialog by rememberSaveable { mutableStateOf(false) }
    var showGitPortDialog by rememberSaveable { mutableStateOf(false) }
    var showMediaServerUrlDialog by rememberSaveable { mutableStateOf(false) }
    var showReaderServerUrlDialog by rememberSaveable { mutableStateOf(false) }
    var showAccountCreationUrlDialog by rememberSaveable { mutableStateOf(false) }
    var showLanguageUrlDialog by rememberSaveable { mutableStateOf(false) }
    var showIndexSqliteUrlDialog by rememberSaveable { mutableStateOf(false) }
    var showTmLinksUrlDialog by rememberSaveable { mutableStateOf(false) }

    var showBackupIntervalDialog by rememberSaveable { mutableStateOf(false) }
    var showLoggingLevelDialog by rememberSaveable { mutableStateOf(false) }

    var openLegalDocumentId by rememberSaveable { mutableStateOf<String?>(null) }

    val openDirectoryLauncher = rememberDirectoryPickerLauncher(
        directory = PlatformFile("Downloads"),
        dialogSettings = FileKitDialogSettings.createDefault()
    ) { directory: PlatformFile? ->
        directory?.let(component::migrateOldAppData)
    }

    val uriHandler = LocalUriHandler.current

    LaunchedEffect(component) {
        component.event.collect {
            when (it) {
                is SettingsComponent.Event.OnLogout -> component.onLogout()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(Res.string.action_settings))
                },
                navigationIcon = {
                    IconButton(onClick = component::onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "back",
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(contentPadding = paddingValues) {
            
            // --- GENERAL PREFERENCES ---
            item { PreferenceCategoryHeader(stringResource(Res.string.header_general)) }

            item {
                ClickablePreference(
                    title = stringResource(Res.string.title_color_theme),
                    summary = state.currentThemeName,
                    onClick = { showThemeDialog = true }
                )
            }

            item { HorizontalDivider() }

            item {
                ClickablePreference(
                    title = stringResource(Res.string.title_translation_typeface),
                    summary = state.currentTranslationFontName,
                    onClick = { showTranslationFontDialog = true }
                )
            }

            item { HorizontalDivider() }

            item {
                ClickablePreference(
                    title = stringResource(Res.string.title_typeface_size),
                    summary = state.currentTranslationFontSizeName,
                    onClick = { showTranslationFontSizeDialog = true }
                )
            }

            item { HorizontalDivider() }

            item {
                ClickablePreference(
                    title = stringResource(Res.string.title_source_typeface),
                    summary = state.currentSourceFontName,
                    onClick = { showSourceFontDialog = true }
                )
            }

            item { HorizontalDivider() }

            item {
                ClickablePreference(
                    title = stringResource(Res.string.title_source_typeface_size),
                    summary = state.currentSourceFontSizeName,
                    onClick = { showSourceFontSizeDialog = true }
                )
            }

            item { HorizontalDivider() }

            item {
                ListItem(
                    headlineContent = { Text(stringResource(Res.string.version)) },
                    supportingContent = { Text(component.appVersion) }
                )
            }

            item { HorizontalDivider() }

            item {
                ClickablePreference(
                    title = stringResource(Res.string.check_for_updates),
                    summary = stringResource(Res.string.check_for_app_updates),
                    onClick = { component.checkForLatestRelease() }
                )
            }

            // --- SERVER PREFERENCES ---
            item { PreferenceCategoryHeader(stringResource(Res.string.header_synchronization)) }

            item {
                ClickablePreference(
                    title = stringResource(Res.string.content_server),
                    summary = state.currentContentServerName,
                    onClick = { showContentServerDialog = true }
                )
            }

            item { HorizontalDivider() }

            item {
                ClickablePreference(
                    title = stringResource(Res.string.title_git_server_port),
                    summary = state.gitServerPort,
                    onClick = { showGitPortDialog = true }
                )
            }

            item { HorizontalDivider() }

            item {
                ClickablePreference(
                    title = stringResource(Res.string.title_gogs_api),
                    summary = state.currentGogsApiUrl,
                    onClick = { showGogsApiDialog = true }
                )
            }

            item { HorizontalDivider() }

            item {
                ClickablePreference(
                    title = stringResource(Res.string.title_media_server),
                    summary = state.mediaServerUrl,
                    onClick = { showMediaServerUrlDialog = true }
                )
            }

            item { HorizontalDivider() }

            item {
                ClickablePreference(
                    title = stringResource(Res.string.title_reader_server),
                    summary = state.readerServerUrl,
                    onClick = { showReaderServerUrlDialog = true }
                )
            }

            item { HorizontalDivider() }

            item {
                ClickablePreference(
                    title = stringResource(Res.string.title_create_account_url),
                    summary = state.accountCreationUrl,
                    onClick = { showAccountCreationUrlDialog = true }
                )
            }

            item { HorizontalDivider() }

            item {
                ClickablePreference(
                    title = stringResource(Res.string.title_language_url),
                    summary = state.languagesUrl,
                    onClick = { showLanguageUrlDialog = true }
                )
            }

            item { HorizontalDivider() }

            item {
                ClickablePreference(
                    title = stringResource(Res.string.title_index_sqlite_url),
                    summary = state.indexSqliteUrl,
                    onClick = { showIndexSqliteUrlDialog = true }
                )
            }

            item { HorizontalDivider() }

            item {
                ClickablePreference(
                    title = stringResource(Res.string.title_tm_url),
                    summary = state.tmLinksUrl,
                    onClick = { showTmLinksUrlDialog = true }
                )
            }

            // --- LEGAL PREFERENCES ---
            item { PreferenceCategoryHeader(stringResource(Res.string.header_legal)) }

            item {
                ClickablePreference(
                    title = stringResource(Res.string.view_license_agreement),
                    onClick = { openLegalDocumentId = Res.string.license_pdf.key }
                )
            }

            item { HorizontalDivider() }

            item {
                ClickablePreference(
                    title = stringResource(Res.string.view_statement_of_faith),
                    onClick = { openLegalDocumentId = Res.string.statement_of_faith.key }
                )
            }

            item { HorizontalDivider() }

            item {
                ClickablePreference(
                    title = stringResource(Res.string.view_translation_guidelines),
                    onClick = { openLegalDocumentId = Res.string.translation_guidlines.key }
                )
            }

            item { HorizontalDivider() }

            item {
                ClickablePreference(
                    title = stringResource(Res.string.view_software_licenses),
                    onClick = { openLegalDocumentId = Res.string.software_licenses.key }
                )
            }

            // --- ADVANCED PREFERENCES ---
            item { PreferenceCategoryHeader(stringResource(Res.string.header_advanced)) }

            item {
                ClickablePreference(
                    title = stringResource(Res.string.title_migrate_old_app),
                    summary = stringResource(Res.string.description_migrate_old_app),
                    onClick = { openDirectoryLauncher.launch() }
                )
            }

            item { HorizontalDivider() }

            item {
                CheckboxPreference(
                    title = stringResource(Res.string.pref_title_check_hardware_requirements),
                    summary = stringResource(Res.string.pref_description_check_hardware_requirements),
                    checked = state.checkHardwareEnabled,
                    onCheckedChange = { component.setCheckHardwareEnabled(it) }
                )
            }

            item { HorizontalDivider() }

            item {
                CheckboxPreference(
                    title = stringResource(Res.string.pref_title_enable_tm_links),
                    summary = stringResource(Res.string.pref_description_enable_tm_links),
                    checked = state.tmLinksEnabled,
                    onCheckedChange = { component.setTmLinksEnabled(it) }
                )
            }

            item { HorizontalDivider() }

            item {
                ClickablePreference(
                    title = "Backup Interval",
                    summary = state.currentBackupIntervalName,
                    onClick = { showBackupIntervalDialog = true }
                )
            }

            item { HorizontalDivider() }

            item {
                ClickablePreference(
                    title = stringResource(Res.string.title_logging_level),
                    summary = state.currentLoggingLevelName,
                    onClick = { showLoggingLevelDialog = true }
                )
            }

            item { HorizontalDivider() }

            item {
                ClickablePreference(
                    title = stringResource(Res.string.title_developer_tools),
                    onClick = component::openDeveloperTools
                )
            }
        }
    }

    state.releaseResult?.let { resultObj ->
        if (resultObj.release != null) {
            ConfirmDialog(
                title = stringResource(Res.string.apk_update_available),
                message = stringResource(Res.string.download_latest_apk),
                onConfirm = {
                    uriHandler.openUri(resultObj.release.downloadUrl)
                    component.dismissUpdateResultDialog()
                },
                onDismiss = { component.dismissUpdateResultDialog() },
                confirmText = stringResource(Res.string.label_ok)
            )
        } else {
            BaseDialog(
                onDismiss = { component.dismissUpdateResultDialog() },
                title = stringResource(Res.string.check_for_updates),
                message = stringResource(Res.string.have_latest_app_update)
            ) {
                TextButton(
                    onClick = { component.dismissUpdateResultDialog() }
                ) {
                    Text(stringResource(Res.string.label_ok))
                }
            }
        }
    }

    if (state.migrationFinished) {
        BaseDialog(
            onDismiss = component::onMigrationFinished,
            title = "",
            message = stringResource(Res.string.migrating_complete)
        ) { onBaseDismiss ->
            TextButton(onClick = onBaseDismiss) {
                Text(stringResource(Res.string.label_ok))
            }
        }
    }

    if (showThemeDialog) {
        ListPreferenceDialog(
            title = stringResource(Res.string.title_color_theme),
            entries = state.themeNames,
            entryValues = state.themeValues,
            selectedValue = state.currentThemeValue,
            onValueSelected = { newValue ->
                showThemeDialog = false
                component.updateColorTheme(newValue)
            },
            onDismissRequest = { showThemeDialog = false }
        )
    }

    if (showTranslationFontDialog) {
        ListPreferenceDialog(
            title = stringResource(Res.string.title_translation_typeface),
            entries = state.availableFontNames,
            entryValues = state.availableFonts,
            selectedValue = state.currentTranslationFontValue,
            onValueSelected = { newFileName ->
                component.updateTranslationTypeface(newFileName)
                showTranslationFontDialog = false
            },
            onDismissRequest = { showTranslationFontDialog = false }
        )
    }

    if (showTranslationFontSizeDialog) {
        ListPreferenceDialog(
            title = stringResource(Res.string.title_typeface_size),
            entries = state.fontSizeNames,
            entryValues = state.fontSizeValues,
            selectedValue = state.currentTranslationFontSizeValue,
            onValueSelected = { newSizeValue ->
                component.updateTranslationFontSize(newSizeValue)
                showTranslationFontSizeDialog = false
            },
            onDismissRequest = { showTranslationFontSizeDialog = false }
        )
    }

    if (showSourceFontDialog) {
        ListPreferenceDialog(
            title = stringResource(Res.string.title_source_typeface),
            entries = state.availableFontNames,
            entryValues = state.availableFonts,
            selectedValue = state.currentSourceFontValue,
            onValueSelected = { newFileName ->
                component.updateSourceTypeface(newFileName)
                showSourceFontDialog = false
            },
            onDismissRequest = { showSourceFontDialog = false }
        )
    }

    if (showSourceFontSizeDialog) {
        ListPreferenceDialog(
            title = stringResource(Res.string.title_source_typeface_size),
            entries = state.fontSizeNames,
            entryValues = state.fontSizeValues,
            selectedValue = state.currentSourceFontSizeValue,
            onValueSelected = { newSizeValue ->
                component.updateSourceFontSize(newSizeValue)
                showSourceFontSizeDialog = false
            },
            onDismissRequest = { showSourceFontSizeDialog = false }
        )
    }

    if (showContentServerDialog) {
        ListPreferenceDialog(
            title = stringResource(Res.string.content_server),
            entries = state.contentServerNames,
            entryValues = state.contentServerValues,
            selectedValue = state.currentContentServerValue,
            onValueSelected = { newServerValue ->
                component.onContentServerChanged(newServerValue)
                showContentServerDialog = false
            },
            onDismissRequest = { showContentServerDialog = false }
        )
    }

    if (showGitPortDialog) {
        EditTextPreferenceDialog(
            title = stringResource(Res.string.title_git_server_port),
            initialValue = state.gitServerPort,
            onValueSaved = { newValue ->
                component.updateGitServerPort(newValue)
                showGitPortDialog = false
            },
            onDismissRequest = { showGitPortDialog = false }
        )
    }

    if (showGogsApiDialog) {
        EditTextPreferenceDialog(
            title = stringResource(Res.string.title_gogs_api),
            initialValue = state.currentGogsApiUrl,
            onValueSaved = { newValue ->
                component.updateGogsApiUrl(newValue)
            },
            onDismissRequest = { showGogsApiDialog = false }
        )
    }

    if (showMediaServerUrlDialog) {
        EditTextPreferenceDialog(
            title = stringResource(Res.string.title_media_server),
            initialValue = state.mediaServerUrl,
            onValueSaved = { newValue ->
                component.updateMediaServerUrl(newValue)
                showMediaServerUrlDialog = false
            },
            onDismissRequest = { showMediaServerUrlDialog = false }
        )
    }

    if (showReaderServerUrlDialog) {
        EditTextPreferenceDialog(
            title = stringResource(Res.string.title_reader_server),
            initialValue = state.readerServerUrl,
            onValueSaved = { newValue ->
                component.updateReaderServerUrl(newValue)
                showReaderServerUrlDialog = false
            },
            onDismissRequest = { showReaderServerUrlDialog = false }
        )
    }

    if (showAccountCreationUrlDialog) {
        EditTextPreferenceDialog(
            title = stringResource(Res.string.title_create_account_url),
            initialValue = state.accountCreationUrl,
            onValueSaved = { newValue ->
                component.updateAccountCreationUrl(newValue)
                showAccountCreationUrlDialog = false
            },
            onDismissRequest = { showAccountCreationUrlDialog = false }
        )
    }

    if (showLanguageUrlDialog) {
        EditTextPreferenceDialog(
            title = stringResource(Res.string.title_language_url),
            initialValue = state.languagesUrl,
            onValueSaved = { newValue ->
                component.updateLanguageUrl(newValue)
                showLanguageUrlDialog = false
            },
            onDismissRequest = { showLanguageUrlDialog = false }
        )
    }

    if (showIndexSqliteUrlDialog) {
        EditTextPreferenceDialog(
            title = stringResource(Res.string.title_index_sqlite_url),
            initialValue = state.indexSqliteUrl,
            onValueSaved = { newValue ->
                component.updateIndexSqliteUrl(newValue)
                showIndexSqliteUrlDialog = false
            },
            onDismissRequest = { showIndexSqliteUrlDialog = false }
        )
    }

    if (showTmLinksUrlDialog) {
        EditTextPreferenceDialog(
            title = stringResource(Res.string.title_tm_url),
            initialValue = state.tmLinksUrl,
            onValueSaved = { newValue ->
                component.updateTmLinksUrl(newValue)
                showTmLinksUrlDialog = false
            },
            onDismissRequest = { showTmLinksUrlDialog = false }
        )
    }

    if (showBackupIntervalDialog) {
        ListPreferenceDialog(
            title = "Backup Interval",
            entries = state.backupIntervalNames,
            entryValues = state.backupIntervalValues,
            selectedValue = state.currentBackupIntervalValue,
            onValueSelected = { newIntervalValue ->
                component.updateBackupInterval(newIntervalValue)
                showBackupIntervalDialog = false
            },
            onDismissRequest = { showBackupIntervalDialog = false }
        )
    }

    if (showLoggingLevelDialog) {
        ListPreferenceDialog(
            title = stringResource(Res.string.title_logging_level),
            entries = state.loggingLevelNames,
            entryValues = state.loggingLevelValues,
            selectedValue = state.currentLoggingLevelValue,
            onValueSelected = { newLevelValue ->
                component.updateLoggingLevel(newLevelValue)
                showLoggingLevelDialog = false
            },
            onDismissRequest = { showLoggingLevelDialog = false }
        )
    }

    openLegalDocumentId?.let { resourceId ->
        LegalDocumentDialog(
            htmlResourceId = resourceId,
            onDismissRequest = { openLegalDocumentId = null }
        )
    }

    progress?.let {
        ProgressDialog(
            message = it.message,
            progress = it.value
        )
    }
}