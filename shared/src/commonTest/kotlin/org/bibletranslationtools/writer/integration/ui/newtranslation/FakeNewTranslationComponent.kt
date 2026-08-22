package org.bibletranslationtools.writer.integration.ui.newtranslation

import kotlinx.coroutines.flow.MutableStateFlow
import org.bibletranslationtools.resourcecatalog.library.models.TargetLanguage
import org.bibletranslationtools.writer.core.Progress
import org.bibletranslationtools.writer.ui.newtranslation.MergeConflict
import org.bibletranslationtools.writer.ui.newtranslation.NewTranslationComponent
import org.bibletranslationtools.writer.ui.newtranslation.TranslationTypeOption

class FakeNewTranslationComponent : NewTranslationComponent {
    override val state = MutableStateFlow(NewTranslationComponent.State())
    override val progress = MutableStateFlow<Progress?>(null)

    var onLanguageSelectedCalledWith: TargetLanguage? = null
    var onProjectSelectedCalledWith: String? = null
    var onCategorySelectedCalledWith: Long? = null
    var onCategoryBackCalled = false
    var onTypeSelectedCalledWith: TranslationTypeOption? = null
    var onTypeBackCalled = false
    var onSearchCalledWith: String? = null
    var mergeTranslationCalledWith: MergeConflict? = null
    var clearMergeConflictCalled = false
    var navigateBackCalled = false

    override fun onLanguageSelected(targetLanguage: TargetLanguage) {
        onLanguageSelectedCalledWith = targetLanguage
    }

    override fun onProjectSelected(projectId: String) {
        onProjectSelectedCalledWith = projectId
    }

    override fun onCategorySelected(categoryId: Long) {
        onCategorySelectedCalledWith = categoryId
    }

    override fun onCategoryBack() {
        onCategoryBackCalled = true
    }

    override fun onTypeSelected(option: TranslationTypeOption) {
        onTypeSelectedCalledWith = option
    }

    override fun onTypeBack() {
        onTypeBackCalled = true
    }

    override fun onSearch(query: String) {
        onSearchCalledWith = query
    }

    override fun mergeTranslation(mergeConflict: MergeConflict) {
        mergeTranslationCalledWith = mergeConflict
    }

    override fun clearMergeConflict() {
        clearMergeConflictCalled = true
    }

    override fun navigateBack() {
        navigateBackCalled = true
    }
}
