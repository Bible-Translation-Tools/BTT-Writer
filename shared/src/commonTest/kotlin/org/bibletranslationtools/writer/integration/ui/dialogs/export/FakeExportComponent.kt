package org.bibletranslationtools.writer.integration.ui.dialogs.export

import io.github.vinceglb.filekit.PlatformFile
import io.mockk.mockk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import org.bibletranslationtools.writer.core.Progress
import org.bibletranslationtools.writer.core.TargetTranslation
import org.bibletranslationtools.writer.ui.dialogs.export.ExportComponent

class FakeExportComponent : ExportComponent {
    override val state = MutableStateFlow(ExportComponent.State())
    override val event: Flow<ExportComponent.Event> = MutableSharedFlow()
    override val progress = MutableStateFlow<Progress?>(null)
    override val targetTranslation: TargetTranslation = mockk(relaxed = true)
    override val projectName = "Test Project"
    override val projectTitle = "Test Project Title"
    override val showPrint = false

    var exportToAppCalled = false
    var openExportToCloudCalled = false

    override fun onMergeConflict() {}
    override fun showFeedbackDialog(message: String) {}
    override fun printPdf(file: PlatformFile, includeImages: Boolean, includeIncomplete: Boolean) {}
    override fun exportUsfm(file: PlatformFile) {}
    override fun exportProject(file: PlatformFile) {}
    override fun exportToApp() { exportToAppCalled = true }
    override fun openExportToCloud() { openExportToCloudCalled = true }
    override fun logout(thenLogin: Boolean) {}
    override fun registerKeys() {}
    override fun resetToMaster() {}
    override fun clearInfoMessage() {}
    override fun clearErrorMessage() {}
    override fun clearUploadSuccess() {}
    override fun clearMergeConflict() {}
}
