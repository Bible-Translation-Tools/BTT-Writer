package org.bibletranslationtools.writer.integration.ui.draft

import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.bibletranslationtools.resourcecatalog.library.models.SourceLanguage
import org.bibletranslationtools.resourcecontainer.ResourceContainer
import org.bibletranslationtools.writer.core.Progress
import org.bibletranslationtools.writer.rendering.RenderingProvider
import org.bibletranslationtools.writer.ui.draft.ChapterContent
import org.bibletranslationtools.writer.ui.draft.DraftComponent

class FakeDraftComponent : DraftComponent {
    override val state = MutableStateFlow(DraftComponent.State())
    override val progress = MutableStateFlow<Progress?>(null)

    var onNavigateBackCalled = false
    var getResourceContainerCalledWith: String? = null
    var getSourceLanguageCalledWith: ResourceContainer? = null
    var parseChapterContentCalled = false
    var importDraftCalledWith: ResourceContainer? = null
    var onFinishCalled = false

    override fun onNavigateBack() {
        onNavigateBackCalled = true
    }

    override fun getResourceContainer(rcSlug: String): ResourceContainer {
        getResourceContainerCalledWith = rcSlug
        return mockk(relaxed = true)
    }

    override fun getSourceLanguage(draftTranslation: ResourceContainer): SourceLanguage {
        getSourceLanguageCalledWith = draftTranslation
        return mockk(relaxed = true)
    }

    override suspend fun parseChapterContent(
        chapterSlug: String,
        container: ResourceContainer,
        renderingProvider: RenderingProvider
    ): ChapterContent {
        parseChapterContentCalled = true
        return ChapterContent(heading = "Mock Heading", title = "Mock Title")
    }

    override fun importDraft(sourceContainer: ResourceContainer) {
        importDraftCalledWith = sourceContainer
    }

    override fun onFinish() {
        onFinishCalled = true
    }
}
