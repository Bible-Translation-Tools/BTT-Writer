package org.bibletranslationtools.writer.integration.ui.home

import com.arkivanov.decompose.router.slot.ChildSlot
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.bibletranslationtools.writer.core.Progress
import org.bibletranslationtools.writer.core.TargetTranslation
import org.bibletranslationtools.writer.ui.home.BookSort
import org.bibletranslationtools.writer.ui.home.HomeComponent
import org.bibletranslationtools.writer.ui.home.ProjectSort
import org.bibletranslationtools.writer.ui.home.TranslationItem
import java.io.File

class FakeHomeComponent : HomeComponent {

    val _state = MutableStateFlow(HomeComponent.HomeState())
    override val state: StateFlow<HomeComponent.HomeState> = _state

    override val event: Flow<HomeComponent.Event> = MutableSharedFlow()
    override val progress: StateFlow<Progress?> = MutableStateFlow(null)
    override val dialogSlot: Value<ChildSlot<*, HomeComponent.DialogChild>> =
        MutableValue(ChildSlot<Any, HomeComponent.DialogChild>())
    override val projectSortOptions: List<ProjectSort> = ProjectSort.entries
    override val bookSortOptions: List<BookSort> = BookSort.entries

    override var lastFocusTargetTranslation: String? = null

    var onNewTranslationCalled = false

    override suspend fun getLastOpened(): TargetTranslation? = null
    override fun deleteProject(project: TranslationItem) {}
    override fun changeResourceType(project: TranslationItem, resourceSlug: String) {}
    override fun confirmResourceMerge() {}
    override fun dismissResourceMerge() {}
    override fun changeProjectSort(sort: ProjectSort) {}
    override fun changeBookSort(sort: BookSort) {}
    override fun showProjectInfo(item: TranslationItem) {}
    override fun importProject(file: PlatformFile) {}
    override fun loadProjects() {}
    override fun loadWithProgress(translationIds: List<String>) {}
    override fun hideProjectInfo() {}
    override fun requestUpdateLibrary() {}
    override fun showFeedbackDialog() {}
    override fun showImportDialog(projectFile: PlatformFile?) {}
    override fun showUpdateLibraryDialog(triggerUpdate: Boolean) {}
    override fun showExportDialog(translationId: String, showPrint: Boolean) {}
    override fun dismissDialog() {}
    override fun onNewTranslation() { onNewTranslationCalled = true }
    override fun onChangeTranslationLanguage(disabledLanguages: List<String>, translationId: String?) {}
    override fun openSettings() {}
    override fun publishProject(translationId: String) {}
    override fun openProject(translationId: String, mergeConflictFilterOn: Boolean) {}
    override fun exitApp() {}
    override fun shareApp() {}
    override fun exportToApp(file: File) {}
    override fun openLogin() {}
    override fun logout() {}
}
