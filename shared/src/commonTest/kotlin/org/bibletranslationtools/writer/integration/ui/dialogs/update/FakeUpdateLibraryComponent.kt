package org.bibletranslationtools.writer.integration.ui.dialogs.update

import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import org.bibletranslationtools.writer.core.Progress
import org.bibletranslationtools.writer.ui.dialogs.update.UpdateLibraryComponent

class FakeUpdateLibraryComponent : UpdateLibraryComponent {
    override val state = MutableStateFlow(UpdateLibraryComponent.State())
    override val progress = MutableStateFlow<Progress?>(null)
    override val event: Flow<UpdateLibraryComponent.Event> = MutableSharedFlow()

    var openDownloadSourcesCalled = false
    var updateSourcesCalled = false
    var downloadIndexCalled = false
    var updateLanguagesCalled = false
    var checkAppUpdateCalled = false
    var clearResultCalled = false

    override fun openDownloadSources() { openDownloadSourcesCalled = true }
    override fun updateSources() { updateSourcesCalled = true }
    override fun importIndex(file: PlatformFile) {}
    override fun downloadIndex() { downloadIndexCalled = true }
    override fun updateLanguages() { updateLanguagesCalled = true }
    override fun checkAppUpdate() { checkAppUpdateCalled = true }
    override fun clearResult() { clearResultCalled = true }
    override fun clearLatestRelease() {}
    override fun clearUpdateSourceResult() {}
}
