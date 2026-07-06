package org.bibletranslationtools.writer.ui.newtranslation

import btt_writer.shared.generated.resources.Res
import btt_writer.shared.generated.resources.error
import btt_writer.shared.generated.resources.failed_to_create_target_translation
import btt_writer.shared.generated.resources.loading
import btt_writer.shared.generated.resources.target_translation_not_found
import btt_writer.shared.generated.resources.warn_existing_target_translation
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.doOnDestroy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.bibletranslationtools.resourcecatalog.ResourceCatalogClient
import org.bibletranslationtools.resourcecatalog.library.models.CategoryEntry
import org.bibletranslationtools.resourcecatalog.library.models.TargetLanguage
import org.bibletranslationtools.resourcecontainer.Project
import org.bibletranslationtools.writer.Platform
import org.bibletranslationtools.writer.core.ComponentScope
import org.bibletranslationtools.writer.core.MergeConflictsHandler
import org.bibletranslationtools.writer.core.Profile
import org.bibletranslationtools.writer.core.Progress
import org.bibletranslationtools.writer.core.ProgressManager
import org.bibletranslationtools.writer.core.ProgressOwner
import org.bibletranslationtools.writer.core.ResourceType
import org.bibletranslationtools.writer.core.TargetTranslation
import org.bibletranslationtools.writer.core.TaskHandle
import org.bibletranslationtools.writer.core.TranslationFormat
import org.bibletranslationtools.writer.core.Translator
import org.bibletranslationtools.writer.core.launchWithProgress
import org.bibletranslationtools.writer.data.Preference
import org.bibletranslationtools.writer.usecases.MergeTargetTranslation
import org.jetbrains.compose.resources.getString
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.Locale

enum class ScreenStep {
    LANGUAGE,
    PROJECT
}

data class MergeConflict(
    val sourceTranslation: TargetTranslation,
    val destinationTranslation: TargetTranslation,
    val message: String
)

interface NewTranslationComponent {

    val state: StateFlow<State>
    val progress: StateFlow<Progress?>

    fun onLanguageSelected(targetLanguage: TargetLanguage)
    fun onProjectSelected(projectId: String)
    fun onCategorySelected(categoryId: Long)
    fun onCategoryBack()
    fun onSearch(query: String)
    fun mergeTranslation(mergeConflict: MergeConflict)
    fun clearMergeConflict()

    fun navigateBack()

    data class State(
        val screenStep: ScreenStep = ScreenStep.LANGUAGE,
        val searchQuery: String = "",
        val languages: List<TargetLanguage> = emptyList(),
        val filteredLanguages: List<TargetLanguage> = emptyList(),
        val disabledLanguages: List<String> = emptyList(),
        val categories: List<CategoryEntry> = emptyList(),
        val filteredCategories: List<CategoryEntry> = emptyList(),
        val categoryStack: List<Long> = listOf(0L),
        val navigatingForward: Boolean = true,
        val mergeConflict: MergeConflict? = null
    )

    sealed interface Result {
        data object NavigateBack : Result
        data object Success : Result
        data class Error(val text: String) : Result
        data class Duplicate(val translationId: String) : Result
        data class MergeConflict(val translationId: String) : Result
    }
}

class DefaultNewTranslationComponent(
    componentContext: ComponentContext,
    private val disabledLanguages: List<String>,
    private val translationId: String?,
    private val onResult: (NewTranslationComponent.Result) -> Unit
) : NewTranslationComponent,
    ComponentContext by componentContext,
    KoinComponent, ComponentScope, ProgressOwner {

    private val mergeTargetTranslation: MergeTargetTranslation by inject()
    private val preference: Preference by inject()
    private val catalogClient: ResourceCatalogClient by inject()
    private val translator: Translator by inject()
    private val profile: Profile by inject()
    private val platform: Platform by inject()

    var selectedTargetLanguage: TargetLanguage? = null
        private set

    override val coroutineScope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())

    private val progressManager = ProgressManager(coroutineScope)
    override val progress get() = progressManager.progress

    private val _state = MutableStateFlow(NewTranslationComponent.State())
    override val state: StateFlow<NewTranslationComponent.State> = _state

    override suspend fun runTask(message: String?, block: suspend (TaskHandle) -> Unit) {
        progressManager.runTask(message, block)
    }

    init {
        launchWithProgress {
            val languages = withContext(Dispatchers.IO) {
                catalogClient.library.getTargetLanguages().sortedBy { it.slug }
            }
            _state.value = _state.value.copy(
                languages = languages,
                filteredLanguages = languages,
                disabledLanguages = disabledLanguages
            )
        }

        lifecycle.doOnDestroy {
            coroutineScope.cancel()
        }
    }

    override fun navigateBack() {
        onResult(NewTranslationComponent.Result.NavigateBack)
    }

    override fun onLanguageSelected(targetLanguage: TargetLanguage) {
        selectedTargetLanguage = targetLanguage

        if (translationId == null) {
            showProjectStep()
            return
        }

        launchWithProgress(Res.string.loading) {
            withContext(Dispatchers.IO) {
                val sourceTranslation = translator.getTargetTranslation(translationId)
                if (sourceTranslation == null) {
                    withContext(Dispatchers.Main) {
                        val error = getString(Res.string.target_translation_not_found, translationId)
                        onResult(NewTranslationComponent.Result.Error(error))
                    }
                    return@withContext
                }

                if (targetLanguage.slug == sourceTranslation.targetLanguage.slug) {
                    withContext(Dispatchers.Main) {
                        onResult(NewTranslationComponent.Result.Success)
                    }
                    return@withContext
                }

                val projectId = sourceTranslation.projectId
                val resourceSlug = sourceTranslation.resourceSlug
                val existingTranslation = getTargetTranslation(
                    TargetTranslation.generateTargetTranslationId(
                        targetLanguage.slug, projectId, ResourceType.TEXT, resourceSlug
                    )
                )

                existingTranslation?.let { translation ->
                    val message = getString(
                        Res.string.warn_existing_target_translation,
                        getProject(translation)?.name ?: "unknown",
                        translation.targetLanguageName
                    )
                    _state.update {
                        it.copy(mergeConflict = MergeConflict(
                            sourceTranslation = sourceTranslation,
                            destinationTranslation = translation,
                            message = message
                        ))
                    }
                } ?: run {
                    val originalId = sourceTranslation.id
                    sourceTranslation.changeTargetLanguage(targetLanguage)
                    sourceTranslation.normalizePath()
                    val newId = sourceTranslation.id
                    moveTargetTranslationAppSettings(originalId, newId)
                    withContext(Dispatchers.Main) {
                        onResult(NewTranslationComponent.Result.Success)
                    }
                }
            }
        }
    }

    override fun onProjectSelected(projectId: String) {
        coroutineScope.launch {
            val resourceSlug = if (projectId == "obs") "obs" else "reg"
            val existingTranslation = selectedTargetLanguage?.let { selected ->
                getTargetTranslation(
                    TargetTranslation.generateTargetTranslationId(
                        selected.slug, projectId, ResourceType.TEXT, resourceSlug
                    )
                )
            }

            if (existingTranslation == null) {
                val format = if (projectId == "obs") TranslationFormat.MARKDOWN else TranslationFormat.USFM
                val targetTranslation = createTargetTranslation(
                    projectId, ResourceType.TEXT, resourceSlug, format
                )
                if (targetTranslation != null) {
                    onResult(NewTranslationComponent.Result.Success)
                } else {
                    val error = getString(Res.string.failed_to_create_target_translation)
                    deleteTargetTranslation(projectId, resourceSlug)
                    onResult(NewTranslationComponent.Result.Error(error))
                }
            } else {
                onResult(NewTranslationComponent.Result.Duplicate(existingTranslation.id))
            }
        }
    }

    override fun onCategorySelected(categoryId: Long) {
        val categories = catalogClient.library.getProjectCategories(
            categoryId, platform.deviceLanguageCode, "all"
        )
        _state.value = _state.value.copy(
            searchQuery = "",
            categories = categories,
            filteredCategories = categories,
            categoryStack = _state.value.categoryStack + categoryId,
            navigatingForward = true
        )
    }

    override fun onCategoryBack() {
        val stack = _state.value.categoryStack
        if (stack.size <= 1) {
            _state.value = _state.value.copy(
                screenStep = ScreenStep.LANGUAGE,
                searchQuery = "",
                categories = emptyList(),
                filteredCategories = emptyList(),
                categoryStack = listOf(0L),
                navigatingForward = false
            )
            return
        }
        val newStack = stack.dropLast(1)
        val parentId = newStack.last()
        val categories = catalogClient.library.getProjectCategories(
            parentId, platform.deviceLanguageCode, "all"
        )
        _state.value = _state.value.copy(
            searchQuery = "",
            categories = categories,
            filteredCategories = categories,
            categoryStack = newStack,
            navigatingForward = false
        )
    }

    override fun onSearch(query: String) {
        _state.value = _state.value.copy(searchQuery = query)
        when (_state.value.screenStep) {
            ScreenStep.LANGUAGE -> filterLanguages(query)
            ScreenStep.PROJECT -> filterCategories(query)
        }
    }

    override fun mergeTranslation(mergeConflict: MergeConflict) {
        launchWithProgress {
            _state.update { it.copy(mergeConflict = null) }
            val result = withContext(Dispatchers.IO) {
                mergeTargetTranslation.execute(
                    mergeConflict.destinationTranslation,
                    mergeConflict.sourceTranslation,
                    true
                )
            }

            when (result.status) {
                MergeTargetTranslation.Status.MERGE_CONFLICTS -> {
                    preference.clearTargetTranslationSettings(
                        result.sourceTranslation.id
                    )
                    val hasConflicts = MergeConflictsHandler.isTranslationMergeConflicted(
                        result.destinationTranslation.id,
                        translator
                    )
                    if (hasConflicts) {
                        onResult(NewTranslationComponent.Result.MergeConflict(
                            result.destinationTranslation.id
                        ))
                    } else {
                        onResult(NewTranslationComponent.Result.Success)
                    }
                }
                MergeTargetTranslation.Status.SUCCESS -> {
                    preference.clearTargetTranslationSettings(
                        result.sourceTranslation.id
                    )
                    onResult(NewTranslationComponent.Result.Success)
                }
                else -> {
                    val error = getString(Res.string.error)
                    onResult(NewTranslationComponent.Result.Error(error))
                }
            }
        }
    }

    override fun clearMergeConflict() {
        _state.update { it.copy(mergeConflict = null) }
    }

    private fun filterLanguages(query: String) {
        val languages = _state.value.languages
        if (query.isEmpty()) {
            _state.value = _state.value.copy(filteredLanguages = languages)
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
        _state.value = _state.value.copy(filteredLanguages = sorted)
    }

    private fun filterCategories(query: String) {
        val categories = _state.value.categories
        if (query.isEmpty()) {
            _state.value = _state.value.copy(filteredCategories = categories)
            return
        }
        val lowerQuery = query.lowercase(Locale.getDefault())
        val filtered = categories.filter { category ->
            val slugMatch = category.slug.lowercase(Locale.getDefault()).startsWith(lowerQuery)
            val nameMatch = category.name.lowercase(Locale.getDefault()).startsWith(lowerQuery)
            val langMatch = category.sourceLanguageSlug.lowercase(Locale.getDefault())
                .startsWith(lowerQuery)
            val componentMatch = category.slug.split("-").any {
                it.lowercase(Locale.getDefault()).startsWith(lowerQuery)
            } || category.name.split(" ").any {
                it.lowercase(Locale.getDefault()).startsWith(lowerQuery)
            }
            slugMatch || nameMatch || langMatch || componentMatch
        }
        _state.value = _state.value.copy(filteredCategories = filtered)
    }

    private fun showProjectStep() {
        val categories = catalogClient.library.getProjectCategories(
            0L, platform.deviceLanguageCode, "all"
        )
        _state.value = _state.value.copy(
            screenStep = ScreenStep.PROJECT,
            searchQuery = "",
            categories = categories,
            filteredCategories = categories,
            categoryStack = listOf(0L),
            navigatingForward = true
        )
    }

    private fun getProject(targetTranslation: TargetTranslation): Project? {
        return catalogClient.library.getProject(
            platform.deviceLanguageCode,
            targetTranslation.projectId
        )
    }

    private suspend fun getTargetTranslation(translationId: String): TargetTranslation? {
        return translator.getTargetTranslation(translationId)
    }

    private fun moveTargetTranslationAppSettings(
        targetTranslationId: String,
        newTargetTranslationId: String
    ) {
        val sources = preference.getOpenSourceTranslations(targetTranslationId)
        for (source in sources) {
            preference.addOpenSourceTranslation(newTargetTranslationId, source)
        }

        val source = preference.getSelectedSourceTranslationId(targetTranslationId)
        preference.setSelectedSourceTranslation(newTargetTranslationId, source)

        val lastFocusChapterId = preference.getLastFocusChapterId(targetTranslationId)
        val lastFocusFrameId = preference.getLastFocusFrameId(targetTranslationId)
        preference.setLastFocus(newTargetTranslationId, lastFocusChapterId, lastFocusFrameId)

        val lastViewMode = preference.getLastViewMode(targetTranslationId)
        preference.setLastViewMode(newTargetTranslationId, lastViewMode)

        preference.clearTargetTranslationSettings(targetTranslationId)
    }

    private suspend fun createTargetTranslation(
        projectId: String,
        resourceType: ResourceType,
        resourceSlug: String,
        format: TranslationFormat
    ): TargetTranslation? {
        return selectedTargetLanguage?.let { targetLanguage ->
            val targetTranslation = translator.createTargetTranslation(
                profile.nativeSpeaker,
                targetLanguage,
                projectId,
                resourceType,
                resourceSlug,
                format
            )
            targetTranslation
        }
    }

    private fun deleteTargetTranslation(projectId: String, resourceSlug: String) {
        selectedTargetLanguage?.let { selected ->
            translator.deleteTargetTranslation(
                TargetTranslation.generateTargetTranslationId(
                    selected.slug, projectId, ResourceType.TEXT, resourceSlug
                )
            )
        }
    }
}