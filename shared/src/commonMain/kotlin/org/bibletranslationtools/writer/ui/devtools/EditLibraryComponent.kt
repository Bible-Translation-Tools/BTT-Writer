package org.bibletranslationtools.writer.ui.devtools

import btt_writer.shared.generated.resources.Res
import btt_writer.shared.generated.resources.edit_library_saving
import btt_writer.shared.generated.resources.please_wait
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.backhandler.BackCallback
import com.arkivanov.essenty.lifecycle.doOnDestroy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import org.bibletranslationtools.logger.Logger
import org.bibletranslationtools.resourcecatalog.ResourceCatalogClient
import org.bibletranslationtools.resourcecatalog.library.models.TargetLanguage
import org.bibletranslationtools.writer.core.ComponentScope
import org.bibletranslationtools.writer.core.Progress
import org.bibletranslationtools.writer.core.ProgressManager
import org.bibletranslationtools.writer.core.ProgressOwner
import org.bibletranslationtools.writer.core.TaskHandle
import org.bibletranslationtools.writer.core.launchWithProgress
import org.bibletranslationtools.writer.usecases.UpdateTargetLanguage
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Developer tool: pick a target language from the library and edit it.
 */
interface EditLibraryComponent {

    val state: StateFlow<State>
    val progress: StateFlow<Progress?>

    fun onSearch(query: String)
    fun onLanguageSelected(language: TargetLanguage)
    fun clearSelected()
    fun save(language: TargetLanguage)
    fun clearSaveResult()
    fun restart()
    fun navigateBack()

    data class State(
        val searchQuery: String = "",
        val languages: List<TargetLanguage> = emptyList(),
        val filteredLanguages: List<TargetLanguage> = emptyList(),
        val regions: List<String> = emptyList(),
        val selected: TargetLanguage? = null,
        val saveResult: Boolean? = null
    )

    sealed interface Result {
        data object NavigateBack : Result
        data object Restart : Result
    }
}

class DefaultEditLibraryComponent(
    componentContext: ComponentContext,
    private val onResult: (EditLibraryComponent.Result) -> Unit
) : EditLibraryComponent,
    ComponentContext by componentContext,
    KoinComponent, ProgressOwner, ComponentScope {

    companion object {
        private const val TAG = "EditLibraryComponent"
    }

    private val catalogClient: ResourceCatalogClient by inject()
    private val updateTargetLanguage: UpdateTargetLanguage by inject()

    override val coroutineScope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())

    private val progressManager = ProgressManager(coroutineScope)
    override val progress get() = progressManager.progress

    private val _state = MutableStateFlow(EditLibraryComponent.State())
    override val state: StateFlow<EditLibraryComponent.State> = _state.asStateFlow()

    // system back returns from the form to the list, like the top-bar arrow
    private val backCallback = BackCallback(isEnabled = false) { clearSelected() }

    init {
        backHandler.register(backCallback)
        lifecycle.doOnDestroy {
            coroutineScope.cancel()
        }
        loadLanguages()
    }

    override suspend fun runTask(message: String?, block: suspend (TaskHandle) -> Unit) {
        progressManager.runTask(message, block)
    }

    private fun loadLanguages() {
        launchWithProgress(Res.string.please_wait) {
            val languages = withContext(Dispatchers.IO) {
                catalogClient.library.getTargetLanguages().sortedBy { it.slug }
            }
            val regions = languages.map { it.region }.filter { it.isNotBlank() }.distinct().sorted()
            _state.update {
                it.copy(languages = languages, filteredLanguages = languages, regions = regions)
            }
        }
    }

    override fun onSearch(query: String) {
        val lowerQuery = query.trim().lowercase()
        _state.update { state ->
            state.copy(
                searchQuery = query,
                filteredLanguages = state.languages.filter {
                    lowerQuery.isEmpty()
                            || it.slug.lowercase().startsWith(lowerQuery)
                            || it.name.lowercase().contains(lowerQuery)
                }
            )
        }
    }

    override fun onLanguageSelected(language: TargetLanguage) {
        _state.update { it.copy(selected = language) }
        backCallback.isEnabled = true
    }

    override fun clearSelected() {
        _state.update { it.copy(selected = null) }
        backCallback.isEnabled = false
    }

    override fun save(language: TargetLanguage) {
        launchWithProgress(Res.string.edit_library_saving) {
            val result = withContext(Dispatchers.IO) {
                try {
                    updateTargetLanguage.execute(language)
                } catch (e: Exception) {
                    Logger.e(TAG, "Failed to save target language ${language.slug}", e)
                    false
                }
            }
            _state.update { it.copy(saveResult = result) }
        }
    }

    override fun clearSaveResult() {
        _state.update { it.copy(saveResult = null) }
    }

    override fun restart() {
        onResult(EditLibraryComponent.Result.Restart)
    }

    override fun navigateBack() {
        onResult(EditLibraryComponent.Result.NavigateBack)
    }
}
