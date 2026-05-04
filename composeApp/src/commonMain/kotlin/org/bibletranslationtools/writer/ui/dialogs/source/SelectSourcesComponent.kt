package org.bibletranslationtools.writer.ui.dialogs.source

import btt_writer.composeapp.generated.resources.Res
import btt_writer.composeapp.generated.resources.download_complete
import btt_writer.composeapp.generated.resources.download_failed
import btt_writer.composeapp.generated.resources.target_translation_not_found
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.doOnDestroy
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
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.bibletranslationtools.logger.Logger
import org.bibletranslationtools.resourcecatalog.ResourceCatalogClient
import org.bibletranslationtools.resourcecatalog.library.models.Translation
import org.bibletranslationtools.writer.Platform
import org.bibletranslationtools.writer.core.ComponentScope
import org.bibletranslationtools.writer.core.ContainerCache
import org.bibletranslationtools.writer.core.Progress
import org.bibletranslationtools.writer.core.ProgressManager
import org.bibletranslationtools.writer.core.ProgressOwner
import org.bibletranslationtools.writer.core.TargetTranslation
import org.bibletranslationtools.writer.core.TaskHandle
import org.bibletranslationtools.writer.core.Translator
import org.bibletranslationtools.writer.core.launchWithProgress
import org.bibletranslationtools.writer.data.Preference
import org.bibletranslationtools.writer.usecases.DownloadResourceContainers
import org.bibletranslationtools.writer.utils.getStringBlocking
import org.jetbrains.compose.resources.getString
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

const val MAX_SOURCE_ITEMS = 3

data class SourceTabItem(
    val tag: String,
    val title: String,
    val language: String,
    val direction: String
)

data class RCItem(
    val title: String,
    val sourceTranslation: Translation?,
    val selected: Boolean,
    val downloaded: Boolean,
    val hasUpdates: Boolean = false,
    val checkedUpdates: Boolean = false
) {
    val containerSlug: String? = sourceTranslation?.resourceContainerSlug
}

interface SelectSourcesComponent {

    val state: StateFlow<State>
    val event: Flow<Event>
    val progress: StateFlow<Progress?>

    val targetTranslation: TargetTranslation

    fun onUpdateSources()
    fun onConfirmSources()
    fun toggleSelection(source: RCItem)
    fun downloadSource(source: RCItem)
    fun deleteSource(source: RCItem)

    data class State(
        val sources: List<RCItem> = emptyList()
    )

    sealed interface Event {
        data class SnackbarMessage(val message: String) : Event
    }

    sealed interface Result {
        data class Error(val text: String) : Result
        data class ConfirmedSources(val sources: Set<String>) : Result
        data object UpdateSources : Result
    }
}

class DefaultSelectSourcesComponent(
    componentContext: ComponentContext,
    translationId: String,
    private val onResult: (SelectSourcesComponent.Result) -> Unit
) : SelectSourcesComponent,
    ComponentContext by componentContext,
    KoinComponent, ComponentScope, ProgressOwner {

    private val catalogClient: ResourceCatalogClient by inject()
    private val preference: Preference by inject()
    private val downloadResourceContainers: DownloadResourceContainers by inject()
    private val translator: Translator by inject()

    override val coroutineScope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())

    private val progressManager = ProgressManager(coroutineScope)
    override val progress get() = progressManager.progress

    private val _state = MutableStateFlow(SelectSourcesComponent.State())
    override val state: StateFlow<SelectSourcesComponent.State> = _state.asStateFlow()

    private val _event = Channel<SelectSourcesComponent.Event>(Channel.BUFFERED)
    override val event = _event.receiveAsFlow()

    override lateinit var targetTranslation: TargetTranslation

    override suspend fun runTask(message: String?, block: suspend (TaskHandle) -> Unit) {
        progressManager.runTask(message, block)
    }

    init {
        val translation = runBlocking {
            translator.getTargetTranslation(translationId)
        }

        if (translation == null) {
            val error = getStringBlocking(Res.string.target_translation_not_found)
            onResult(SelectSourcesComponent.Result.Error(error))
        } else {
            targetTranslation = translation
            loadAvailableSources()
        }

        lifecycle.doOnDestroy {
            coroutineScope.cancel()
        }
    }

    override fun onConfirmSources() {
        val sourceIds = _state.value.sources
            .filter { it.selected }
            .mapNotNull { it.containerSlug }
            .toSet()
        onResult(SelectSourcesComponent.Result.ConfirmedSources(sourceIds))
    }

    override fun onUpdateSources() {
        onResult(SelectSourcesComponent.Result.UpdateSources)
    }

    override fun toggleSelection(source: RCItem) {
        val stackFull = state.value.sources.filter { it.selected }.size == MAX_SOURCE_ITEMS

        if (!stackFull || source.selected) {
            _state.update { state ->
                state.copy(
                    sources = state.sources.map {
                        if (it.containerSlug == source.containerSlug) {
                            it.copy(selected = !it.selected)
                        } else {
                            it
                        }
                    }
                )
            }
        }
    }

    override fun downloadSource(source: RCItem) {
        val translation = source.sourceTranslation ?: return

        launchWithProgress { handle ->
            val result = withContext(Dispatchers.IO) {
                downloadResourceContainers.download(translation) { progress, message ->
                    handle.update(progress, message)
                }
            }

            for (rc in result.containers) {
                // reset cached containers that were downloaded
                ContainerCache.remove(rc.slug)
            }

            val message = if (result.success) {
                getString(Res.string.download_complete)
            } else {
                getString(Res.string.download_failed)
            }

            _event.trySend(SelectSourcesComponent.Event.SnackbarMessage(message))

            loadAvailableSources()
        }
    }

    override fun deleteSource(source: RCItem) {
        coroutineScope.launch {
            source.containerSlug?.let {
                catalogClient.deleteResourceContainer(it)
                loadAvailableSources()
            }
        }
    }

    private fun loadAvailableSources() {
        launchWithProgress { handle ->
            val rcItems = withContext(Dispatchers.IO) {
                val items = arrayListOf<RCItem>()
                // add selected source translations
                val sourceTranslationSlugs = getOpenSources()
                for (slug in sourceTranslationSlugs) {
                    val st = catalogClient.library.getTranslation(slug)
                    if (st != null) {
                        handle.update(-1f, st.resourceContainerSlug)
                        items.add(addSource(st, true))
                    }
                }

                val availableTranslations = catalogClient.library.findTranslations(
                    null,
                    targetTranslation.projectId,
                    null,
                    "book",
                    null,
                    Platform.MIN_CHECKING_LEVEL,
                    -1
                )
                for (sourceTranslation in availableTranslations) {
                    handle.update(-1f, sourceTranslation.resourceContainerSlug)
                    if (!items.map { it.containerSlug }.contains(sourceTranslation.resourceContainerSlug)) {
                        items.add(addSource(sourceTranslation, false))
                    }
                }
                items
            }
            _state.update {
                it.copy(sources = rcItems)
            }
        }
    }

    private fun getOpenSources(): List<String> {
        return preference.getOpenSourceTranslations(targetTranslation.id)
    }

    private fun addSource(sourceTranslation: Translation, selected: Boolean): RCItem {
        val title = sourceTranslation.language.name + " (" + sourceTranslation.language.slug + ") - " + sourceTranslation.resource.name
        val isDownloaded = catalogClient.resourceContainerExists(sourceTranslation.resourceContainerSlug)
        var hasUpdates = false
        var checkedUpdates = false

        if (selected) { // see if there are updates available to download
            Logger.i(
                this::class.java.simpleName,
                "Checking for updates on " + sourceTranslation.resourceContainerSlug
            )
            try {
                ContainerCache.cache(catalogClient, sourceTranslation.resourceContainerSlug)?.let { container ->
                    val lastModified: Int = catalogClient.getResourceContainerLastModified(
                        container.language.slug,
                        container.project.slug,
                        container.resource.slug
                    )
                    hasUpdates = (lastModified > container.modifiedAt)
                    Logger.i(
                        this::class.java.simpleName,
                        "Checking for updates on " + sourceTranslation.resourceContainerSlug + " finished, needs updates: " + hasUpdates
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            checkedUpdates = true
        }

        return RCItem(
            title = title,
            sourceTranslation = sourceTranslation,
            selected = selected,
            downloaded = isDownloaded,
            hasUpdates = hasUpdates,
            checkedUpdates = checkedUpdates
        )
    }
}