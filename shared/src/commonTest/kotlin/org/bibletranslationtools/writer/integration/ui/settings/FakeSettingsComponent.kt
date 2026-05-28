package org.bibletranslationtools.writer.integration.ui.settings

import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import org.bibletranslationtools.writer.core.Progress
import org.bibletranslationtools.writer.ui.settings.SettingsComponent

class FakeSettingsComponent : SettingsComponent {
    override val state = MutableStateFlow(SettingsComponent.State())
    override val progress = MutableStateFlow<Progress?>(null)
    override val event = MutableSharedFlow<SettingsComponent.Event>()

    override var appVersion: String = "1.0.0-mock"

    var migrateOldAppDataCalled = false
    var checkForLatestReleaseCalled = false
    var setCheckHardwareEnabledCalledWith: Boolean? = null
    var setTmLinksEnabledCalledWith: Boolean? = null
    var dismissUpdateResultDialogCalled = false
    var updateColorThemeCalledWith: String? = null
    var updateTranslationTypefaceCalledWith: String? = null
    var updateTranslationFontSizeCalledWith: String? = null
    var updateSourceTypefaceCalledWith: String? = null
    var updateSourceFontSizeCalledWith: String? = null
    var onContentServerChangedCalledWith: String? = null
    var updateGogsApiUrlCalledWith: String? = null
    var updateMediaServerUrlCalledWith: String? = null
    var updateReaderServerUrlCalledWith: String? = null
    var updateAccountCreationUrlCalledWith: String? = null
    var updateLanguageUrlCalledWith: String? = null
    var updateIndexSqliteUrlCalledWith: String? = null
    var updateTmLinksUrlCalledWith: String? = null
    var updateBackupIntervalCalledWith: String? = null
    var updateLoggingLevelCalledWith: String? = null

    var onNavigateBackCalled = false
    var openDeveloperToolsCalled = false
    var onMigrationFinishedCalled = false
    var onLogoutCalled = false

    override fun migrateOldAppData(dir: PlatformFile) {
        migrateOldAppDataCalled = true
    }

    override fun checkForLatestRelease() {
        checkForLatestReleaseCalled = true
    }

    override fun setCheckHardwareEnabled(enabled: Boolean) {
        setCheckHardwareEnabledCalledWith = enabled
    }

    override fun setTmLinksEnabled(enabled: Boolean) {
        setTmLinksEnabledCalledWith = enabled
    }

    override fun dismissUpdateResultDialog() {
        dismissUpdateResultDialogCalled = true
    }

    override fun updateColorTheme(newValue: String) {
        updateColorThemeCalledWith = newValue
    }

    override fun updateTranslationTypeface(newFileName: String) {
        updateTranslationTypefaceCalledWith = newFileName
    }

    override fun updateTranslationFontSize(newValue: String) {
        updateTranslationFontSizeCalledWith = newValue
    }

    override fun updateSourceTypeface(newValue: String) {
        updateSourceTypefaceCalledWith = newValue
    }

    override fun updateSourceFontSize(newValue: String) {
        updateSourceFontSizeCalledWith = newValue
    }

    override fun onContentServerChanged(newValue: String) {
        onContentServerChangedCalledWith = newValue
    }

    override fun updateGogsApiUrl(newValue: String) {
        updateGogsApiUrlCalledWith = newValue
    }

    override fun updateMediaServerUrl(newValue: String) {
        updateMediaServerUrlCalledWith = newValue
    }

    override fun updateReaderServerUrl(newValue: String) {
        updateReaderServerUrlCalledWith = newValue
    }

    override fun updateAccountCreationUrl(newValue: String) {
        updateAccountCreationUrlCalledWith = newValue
    }

    override fun updateLanguageUrl(newValue: String) {
        updateLanguageUrlCalledWith = newValue
    }

    override fun updateIndexSqliteUrl(newValue: String) {
        updateIndexSqliteUrlCalledWith = newValue
    }

    override fun updateTmLinksUrl(newValue: String) {
        updateTmLinksUrlCalledWith = newValue
    }

    override fun updateBackupInterval(newValue: String) {
        updateBackupIntervalCalledWith = newValue
    }

    override fun updateLoggingLevel(newValue: String) {
        updateLoggingLevelCalledWith = newValue
    }

    override fun onNavigateBack() {
        onNavigateBackCalled = true
    }

    override fun openDeveloperTools() {
        openDeveloperToolsCalled = true
    }

    override fun onMigrationFinished() {
        onMigrationFinishedCalled = true
    }

    override fun onLogout() {
        onLogoutCalled = true
    }
}
