package org.bibletranslationtools.writer.ui.dialogs.import

import btt_writer.shared.generated.resources.Res
import btt_writer.shared.generated.resources.importing_usfm
import btt_writer.shared.generated.resources.invalid_book_name_prompt
import btt_writer.shared.generated.resources.invalid_file
import btt_writer.shared.generated.resources.missing_book_name_prompt
import btt_writer.shared.generated.resources.reading_usfm
import btt_writer.shared.generated.resources.selected_language
import btt_writer.shared.generated.resources.title_import_usfm_error
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.doOnDestroy
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.bibletranslationtools.resourcecatalog.ResourceCatalogClient
import org.bibletranslationtools.resourcecatalog.library.models.CategoryEntry
import org.bibletranslationtools.resourcecatalog.library.models.TargetLanguage
import org.bibletranslationtools.writer.Platform
import org.bibletranslationtools.writer.core.ComponentScope
import org.bibletranslationtools.writer.core.ImportUsfmSession
import org.bibletranslationtools.writer.core.MergeConflictsHandler
import org.bibletranslationtools.writer.core.MissingNameItem
import org.bibletranslationtools.writer.core.ProcessUSFM
import org.bibletranslationtools.writer.core.Progress
import org.bibletranslationtools.writer.core.ProgressManager
import org.bibletranslationtools.writer.core.ProgressOwner
import org.bibletranslationtools.writer.core.TargetTranslation
import org.bibletranslationtools.writer.core.TaskHandle
import org.bibletranslationtools.writer.core.Translator
import org.bibletranslationtools.writer.core.launchWithProgress
import org.bibletranslationtools.writer.displayName
import org.bibletranslationtools.writer.usecases.ImportProjects
import org.jetbrains.compose.resources.getString
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.lang.String.format
import java.util.Locale

enum class UsfmStep {
    LANGUAGE,
    PROMPT_BOOK_NAME,
    PROCESSED,
    DONE
}

interface ImportUsfmComponent {

    val state: StateFlow<State>
    val progress: StateFlow<Progress?>

    fun languageSelected(language: TargetLanguage)
    fun bookSelected(projectId: String)
    fun categorySelected(categoryId: Long)
    fun search(query: String)
    fun navigateBack()
    fun skipBook()
    fun confirmImport()
    fun mergeImport(overwrite: Boolean)
    fun onProjectsImported(translationIds: List<String>)

    data class State(
        val step: UsfmStep = UsfmStep.LANGUAGE,
        val file: PlatformFile? = null,
        val targetLanguage: TargetLanguage? = null,
        val processedResult: String = "",
        val infoMessage: Pair<String, String>? = null,
        val importSuccess: Boolean = false,
        val importedTranslationIds: List<String> = emptyList(),
        val currentMissingItem: MissingNameItem? = null,
        val currentMissingDescription: String = "",
        val missingNamePrompt: String? = null,
        val existentTranslations: List<TargetTranslation> = emptyList(),
        val languages: List<TargetLanguage> = emptyList(),
        val filteredLanguages: List<TargetLanguage> = emptyList(),
        val categories: List<CategoryEntry> = emptyList(),
        val filteredCategories: List<CategoryEntry> = emptyList(),
        val categoryStack: List<Long> = listOf(0L),
        val started: Boolean = false
    )

    sealed interface Result {
        data class ProjectsImported(val translationIds: List<String>) : Result
        data class MergeConflict(val translationId: String) : Result
    }
}

class DefaultImportUsfmComponent(
    componentContext: ComponentContext,
    file: PlatformFile,
    private val onResult: (ImportUsfmComponent.Result) -> Unit
) : ImportUsfmComponent,
    ComponentContext by componentContext,
    KoinComponent, ComponentScope, ProgressOwner {

    private val translator: Translator by inject()
    private val importProjects: ImportProjects by inject()
    private val catalogClient: ResourceCatalogClient by inject()
    private val processUSFM: ProcessUSFM by inject()
    private val platform: Platform by inject()

    override val coroutineScope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())

    private val progressManager = ProgressManager(coroutineScope)
    override val progress get() = progressManager.progress

    private val _state = MutableStateFlow(ImportUsfmComponent.State())
    override val state: StateFlow<ImportUsfmComponent.State> = _state.asStateFlow()

    private var session: ImportUsfmSession? = null
    private var missingNameCounter = 0

    init {
        coroutineScope.launch {
            startImport(file)
        }

        lifecycle.doOnDestroy {
            coroutineScope.cancel()
        }
    }

    override suspend fun runTask(message: String?, block: suspend (TaskHandle) -> Unit) {
        progressManager.runTask(message, block)
    }

    override fun languageSelected(language: TargetLanguage) {
        processFile(language)
    }

    override fun bookSelected(projectId: String) {
        setBook(projectId)
    }

    override fun categorySelected(categoryId: Long) {
        navigateToCategory(categoryId)
    }

    override fun search(query: String) {
        val languages = _state.value.languages
        if (query.isEmpty()) {
            _state.update { it.copy(filteredLanguages = languages) }
            return
        }
        val lowerQuery = query.lowercase(Locale.getDefault())
        val filtered = languages.filter { language ->
            language.slug.lowercase(Locale.getDefault()).startsWith(lowerQuery) ||
                    language.name.lowercase(Locale.getDefault()).contains(lowerQuery)
        }
        val sorted = filtered.sortedWith(Comparator { lhs, rhs ->
            var lhId = lhs.slug
            var rhId = rhs.slug
            if (lhId.lowercase(Locale.getDefault()).startsWith(lowerQuery)) {
                lhId = "!!$lhId"
            }
            if (rhId.lowercase(Locale.getDefault()).startsWith(lowerQuery)) {
                rhId = "!!$rhId"
            }
            if (lhs.name.lowercase(Locale.getDefault()).startsWith(lowerQuery)) {
                lhId = "!$lhId"
            }
            if (rhs.name.lowercase(Locale.getDefault()).startsWith(lowerQuery)) {
                rhId = "!$rhId"
            }
            lhId.compareTo(rhId, ignoreCase = true)
        })
        _state.update { it.copy(filteredLanguages = sorted) }
    }

    override fun navigateBack() {
        val stack = _state.value.categoryStack
        if (stack.size <= 1) return
        val parentId = stack[stack.size - 2]
        val categories = catalogClient.library.getProjectCategories(
            parentId, platform.deviceLanguageCode, "all"
        )
        _state.update {
            it.copy(
                categories = categories,
                filteredCategories = categories,
                categoryStack = stack.dropLast(1)
            )
        }
    }

    override fun skipBook() {
        promptNextName()
    }

    override fun confirmImport() {
        doImport(false)
    }

    override fun mergeImport(overwrite: Boolean) {
        doImport(overwrite)
    }

    override fun onProjectsImported(translationIds: List<String>) {
        session?.cleanup()
        onResult(ImportUsfmComponent.Result.ProjectsImported(translationIds))
    }

    private suspend fun startImport(file: PlatformFile) {
        if (_state.value.started) return
        val filename = file.displayName
        val isUsfm = filename.contains(Translator.USFM_EXTENSION, ignoreCase = true)
        val isTxt = filename.contains(Translator.TXT_EXTENSION, ignoreCase = true)
        val isZip = filename.contains(Translator.ZIP_EXTENSION, ignoreCase = true)
        if (isUsfm || isTxt || isZip) {
            launchWithProgress {
                val languages = withContext(Dispatchers.IO) {
                    catalogClient.library.getTargetLanguages().sortedBy { it.slug }
                }

                _state.update {
                    ImportUsfmComponent.State(
                        started = true,
                        file = file,
                        step = UsfmStep.LANGUAGE,
                        languages = languages,
                        filteredLanguages = languages
                    )
                }
            }
        } else {
            val title = getString(Res.string.title_import_usfm_error)
            val message = "${getString(Res.string.invalid_file)}\n$filename"
            _state.update {
                it.copy(
                    started = true,
                    step = UsfmStep.DONE,
                    infoMessage = title to message
                )
            }
        }
    }

    private fun processFile(language: TargetLanguage) {
        val file = _state.value.file ?: return
        _state.update { it.copy(targetLanguage = language) }

        launchWithProgress(Res.string.reading_usfm) { handle ->
            val newSession = withContext(Dispatchers.IO) {
                processUSFM.startImport(language, file) { value, message ->
                    handle.update(value, message)
                }
            }

            session = newSession

            if (newSession.booksMissingNames.isNotEmpty()) {
                missingNameCounter = newSession.booksMissingNames.size
                promptNextName()
            } else {
                showResults()
            }
        }
    }

    private fun promptNextName() {
        coroutineScope.launch {
            val currentSession = session ?: return@launch
            if (missingNameCounter <= 0) {
                showResults()
                return@launch
            }
            missingNameCounter--
            val item = currentSession.booksMissingNames[missingNameCounter]
            val description = currentSession.getShortFilePath(item.description ?: "")

            val prompt = if (item.invalidName != null) {
                getString(Res.string.invalid_book_name_prompt, description, item.invalidName)
            } else {
                getString(Res.string.missing_book_name_prompt, description)
            }

            val categories = catalogClient.library.getProjectCategories(
                0L, platform.deviceLanguageCode, "all"
            )
            _state.update {
                it.copy(
                    step = UsfmStep.PROMPT_BOOK_NAME,
                    currentMissingItem = item,
                    currentMissingDescription = description,
                    missingNamePrompt = prompt,
                    categories = categories,
                    filteredCategories = categories,
                    categoryStack = listOf(0L)
                )
            }
        }
    }

    private fun setBook(projectId: String) {
        val currentSession = session ?: return
        val item = _state.value.currentMissingItem ?: return
        if (item.contents != null && item.description != null) {
            launchWithProgress(Res.string.reading_usfm) { _ ->
                withContext(Dispatchers.IO) {
                    currentSession.processText(
                        book = item.contents,
                        name = item.description,
                        useName = projectId
                    )
                }
                promptNextName()
            }
        } else {
            promptNextName()
        }
    }

    private suspend fun showResults() {
        val currentSession = session ?: return
        val language = _state.value.targetLanguage ?: return
        val languageLabel = format(
            getString(Res.string.selected_language),
            "${language.slug} - ${language.name}"
        )
        val results = currentSession.summary()
        val message = "$languageLabel\n$results"

        val existentTranslations = checkExistentTranslations()

        val infoMessage = if (!currentSession.isSuccess) {
            val title = getString(Res.string.title_import_usfm_error)
            title to message
        } else null

        val step = if (currentSession.isSuccess) UsfmStep.PROCESSED else UsfmStep.DONE

        _state.update {
            it.copy(
                step = step,
                infoMessage = infoMessage,
                processedResult = message,
                existentTranslations = existentTranslations
            )
        }
    }

    private suspend fun checkExistentTranslations(): List<TargetTranslation> {
        val imports = session?.importedProjects ?: return emptyList()
        return imports.mapNotNull { translator.getConflictingTargetTranslation(it) }
    }

    private fun doImport(overwrite: Boolean) {
        val currentSession = session ?: return

        launchWithProgress(Res.string.importing_usfm) { handle ->
            val result = withContext(Dispatchers.IO) {
                importProjects.importProjects(
                    currentSession.importedProjects,
                    overwrite
                ) { progress, message ->
                    handle.update(progress, message)
                }
            }

            // Show merge conflict if there is a single one
            result.conflictingTargetTranslations.singleOrNull()?.let {
                val hasConflicts = MergeConflictsHandler.isTranslationMergeConflicted(
                    it.id,
                    translator
                )
                if (hasConflicts) {
                    currentSession.cleanup()
                    onResult(ImportUsfmComponent.Result.MergeConflict(it.id))
                    return@launchWithProgress
                }
            }

            _state.update {
                it.copy(
                    step = UsfmStep.DONE,
                    importSuccess = result.success,
                    importedTranslationIds = result.targetTranslations.map { t -> t.id }
                )
            }
        }
    }

    private fun navigateToCategory(categoryId: Long) {
        val categories = catalogClient.library.getProjectCategories(
            categoryId, platform.deviceLanguageCode, "all"
        )
        _state.update {
            it.copy(
                categories = categories,
                filteredCategories = categories,
                categoryStack = it.categoryStack + categoryId
            )
        }
    }
}