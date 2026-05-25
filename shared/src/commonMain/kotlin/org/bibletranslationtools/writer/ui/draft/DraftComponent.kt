package org.bibletranslationtools.writer.ui.draft

import btt_writer.shared.generated.resources.Res
import btt_writer.shared.generated.resources.please_wait
import com.arkivanov.decompose.ComponentContext
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
import org.bibletranslationtools.resourcecatalog.library.models.SourceLanguage
import org.bibletranslationtools.resourcecatalog.library.models.Translation
import org.bibletranslationtools.resourcecontainer.ResourceContainer
import org.bibletranslationtools.writer.core.ComponentScope
import org.bibletranslationtools.writer.core.Progress
import org.bibletranslationtools.writer.core.ProgressManager
import org.bibletranslationtools.writer.core.ProgressOwner
import org.bibletranslationtools.writer.core.TaskHandle
import org.bibletranslationtools.writer.core.TranslationFormat
import org.bibletranslationtools.writer.core.Translator
import org.bibletranslationtools.writer.core.launchWithProgress
import org.bibletranslationtools.writer.rendering.Clickables
import org.bibletranslationtools.writer.rendering.RenderingGroup
import org.bibletranslationtools.writer.rendering.RenderingProvider
import org.bibletranslationtools.writer.rendering.VerseDisplay
import org.bibletranslationtools.writer.rendering.model.RenderNode
import org.bibletranslationtools.writer.usecases.ImportDraft
import org.bibletranslationtools.writer.utils.sortedNumerically
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

data class ChapterContent(
    val heading: String,
    val title: String,
    val renderNodes: List<RenderNode> = emptyList()
)

interface DraftComponent {

    val state: StateFlow<State>
    val progress: StateFlow<Progress?>

    fun onNavigateBack()
    fun getResourceContainer(rcSlug: String): ResourceContainer?
    fun getSourceLanguage(draftTranslation: ResourceContainer): SourceLanguage?
    suspend fun parseChapterContent(
        chapterSlug: String,
        container: ResourceContainer,
        renderingProvider: RenderingProvider
    ): ChapterContent
    fun importDraft(sourceContainer: ResourceContainer)

    fun onFinish()

    data class State(
        val draftTranslations: List<Translation> = emptyList(),
        val importResult: ImportDraft.Result? = null,
        val chapterContent: ChapterContent? = null
    )

    sealed interface Result {
        data object NavigateBack : Result
    }
}

class DefaultDraftComponent(
    componentContext: ComponentContext,
    translationId: String,
    private val onResult: (DraftComponent.Result) -> Unit
) : DraftComponent,
    ComponentContext by componentContext,
    KoinComponent, ComponentScope, ProgressOwner {

    companion object {
        private const val TAG = "DraftComponent"
    }

    private val translator: Translator by inject()
    private val catalogClient: ResourceCatalogClient by inject()
    private val importDraft: ImportDraft by inject()

    override val coroutineScope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())

    private val progressManager = ProgressManager(coroutineScope)
    override val progress get() = progressManager.progress

    private val _state = MutableStateFlow(DraftComponent.State())
    override val state: StateFlow<DraftComponent.State> = _state.asStateFlow()

    override suspend fun runTask(message: String?, block: suspend (TaskHandle) -> Unit) {
        progressManager.runTask(message, block)
    }

    init {
        loadDraftTranslations(translationId)

        lifecycle.doOnDestroy {
            coroutineScope.cancel()
        }
    }

    override fun onNavigateBack() {
        onResult(DraftComponent.Result.NavigateBack)
    }

    override fun getResourceContainer(rcSlug: String): ResourceContainer? {
        return try {
            catalogClient.openResourceContainer(rcSlug)
        } catch (e: Exception) {
            Logger.w(TAG, "Resource container not found: $rcSlug", e)
            null
        }
    }

    override fun getSourceLanguage(draftTranslation: ResourceContainer): SourceLanguage? {
        return try {
            catalogClient.library.getSourceLanguage(draftTranslation.info.language.slug)
        } catch (e: Exception) {
            Logger.w(TAG, "Source language not found: ${draftTranslation.info.language.slug}", e)
            null
        }
    }

    override suspend fun parseChapterContent(
        chapterSlug: String,
        container: ResourceContainer,
        renderingProvider: RenderingProvider
    ): ChapterContent = withContext(Dispatchers.IO) {

        var tempTitle = container.readChunk(chapterSlug, "title")
        if (tempTitle.isEmpty()) {
            tempTitle = container.readChunk(
                "front",
                "title"
            ) + " " + chapterSlug.toInt()
        }
        val title = tempTitle

        var chapterBody = ""
        val chunks = container.chunks(chapterSlug).sortedNumerically()
        for (chunk in chunks) {
            chapterBody += container.readChunk(chapterSlug, chunk)
        }

        val mimeType = container.info.contentMimeType
        val bodyFormat = TranslationFormat.parse(mimeType)

        val sourceRendering = RenderingGroup()
        var heading = ""

        if (Clickables.isClickableFormat(bodyFormat)) {
            val renderer = renderingProvider.setupRenderingGroup(
                bodyFormat,
                sourceRendering,
                verseDisplay = VerseDisplay.NUMBER,
                target = true
            )
            renderer.setSuppressLeadingMajorSectionHeadings(true)
            heading = renderer.getLeadingMajorSectionHeading(chapterBody)
        } else {
            sourceRendering.addEngine(renderingProvider.createDefaultRenderer())
        }

        sourceRendering.init(chapterBody)
        val renderNodes = sourceRendering.start()

        ChapterContent(
            heading = heading,
            title = title,
            renderNodes = renderNodes
        )
    }

    override fun importDraft(sourceContainer: ResourceContainer) {
        launchWithProgress(Res.string.please_wait) {
            val result = withContext(Dispatchers.IO) {
                importDraft.execute(sourceContainer){_,_->}
            }
            _state.update { it.copy(importResult = result) }
        }
    }

    override fun onFinish() {
        onResult(DraftComponent.Result.NavigateBack)
    }

    private fun loadDraftTranslations(targetTranslationId: String?) {
        launchWithProgress(Res.string.please_wait) {
            targetTranslationId?.let { id ->
                translator.getTargetTranslation(id)?.let { targetTranslation ->
                    val translations = catalogClient.library.findTranslations(
                        targetTranslation.targetLanguage.slug,
                        targetTranslation.projectId,
                        null,
                        "book",
                        null,
                        0,
                        -1
                    ).filter { it.resource.slug != "udb" }

                    _state.update {
                        it.copy(draftTranslations = translations)
                    }
                }
            }
        }
    }
}