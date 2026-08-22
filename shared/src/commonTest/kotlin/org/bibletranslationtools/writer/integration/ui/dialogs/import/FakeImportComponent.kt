package org.bibletranslationtools.writer.integration.ui.dialogs.import

import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import org.bibletranslationtools.writer.core.Progress
import org.bibletranslationtools.writer.ui.dialogs.import.ImportComponent
import org.bibletranslationtools.writer.ui.home.RepositoryItem
import java.io.File

class FakeImportComponent : ImportComponent {
    override val state = MutableStateFlow(ImportComponent.State())
    override val progress = MutableStateFlow<Progress?>(null)
    override val event: Flow<ImportComponent.Event> = MutableSharedFlow()

    override fun importUsfm(file: PlatformFile) {}
    override fun importProject(file: PlatformFile, overwrite: Boolean) {}
    override fun importSource(file: PlatformFile, overwrite: Boolean) {}
    override fun importBackup(backup: File) {}
    override fun importRepo(repo: RepositoryItem, accepted: Boolean, overwrite: Boolean) {}
    override fun searchRepositories(user: String, repo: String) {}
    override fun registerKeys() {}
    override fun clearResult() {}
    override fun clearMergeConflict() {}
    override fun clearSourceConflict() {}
    override fun clearImportRepo() {}
}
