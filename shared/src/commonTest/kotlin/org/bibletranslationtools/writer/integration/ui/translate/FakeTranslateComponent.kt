package org.bibletranslationtools.writer.integration.ui.translate

import com.arkivanov.decompose.Child
import com.arkivanov.decompose.router.slot.ChildSlot
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import org.bibletranslationtools.writer.core.Progress
import org.bibletranslationtools.writer.core.TargetTranslation
import org.bibletranslationtools.writer.core.TranslationViewMode
import org.bibletranslationtools.writer.ui.translate.TranslateComponent
import org.bibletranslationtools.writer.ui.translate.read.ReadModeComponent

class FakeTranslateComponent : TranslateComponent {
    private val activeChild = Child.Created(
        configuration = TranslateComponent.Config.Read,
        instance = TranslateComponent.Child.Read(
            mockk(relaxed = true) {
                every { state } returns MutableStateFlow(ReadModeComponent.State())
                every { items } returns MutableStateFlow(emptyList())
                every { progress } returns MutableStateFlow(null)
            }
        )
    )
    override val stack: Value<ChildStack<*, TranslateComponent.Child>> = MutableValue(
        ChildStack(active = activeChild)
    )

    override val dialogSlot: Value<ChildSlot<*, TranslateComponent.DialogChild>> = MutableValue(
        ChildSlot<Any, TranslateComponent.DialogChild>()
    )

    override val state = MutableStateFlow(TranslateComponent.State())
    override val sharedState = MutableStateFlow(TranslateComponent.SharedState())
    override val progress = MutableStateFlow<Progress?>(null)

    private val _event = Channel<TranslateComponent.Event>(Channel.BUFFERED)
    override val event: Flow<TranslateComponent.Event> = _event.receiveAsFlow()
    override val eventSender: SendChannel<TranslateComponent.Event> = _event

    override val currentViewMode = MutableValue(TranslationViewMode.READ)
    override val targetTranslation: TargetTranslation = mockk(relaxed = true)

    var openViewModeCalledWith: TranslationViewMode? = null
    var openReadModeCalled = false
    var openChunkModeCalled = false
    var openReviewModeCalledWith: Boolean? = null
    var restartAutoCommitTimerCalled = false
    var updateMergeFilterCalledWith: Boolean? = null
    var removeSourceCalledWith: String? = null
    var selectSourceCalledWith: String? = null
    var saveLastFocusCalledWith: Pair<String, String?>? = null
    var openHomeCalledWith: Boolean? = null
    var openDraftCalledWith: String? = null
    var openPublishProjectCalledWith: String? = null
    var openSettingsCalled = false
    var showFeedbackDialogCalled = false
    var showSelectSourcesDialogCalled = false
    var showExportDialogCalledWith: Boolean? = null
    var dismissDialogCalled = false

    override fun openViewMode(viewMode: TranslationViewMode) {
        openViewModeCalledWith = viewMode
    }

    override fun openReadMode() {
        openReadModeCalled = true
        currentViewMode.value = TranslationViewMode.READ
    }

    override fun openChunkMode() {
        openChunkModeCalled = true
        currentViewMode.value = TranslationViewMode.CHUNK
    }

    override fun openReviewMode(conflictFilterOn: Boolean) {
        openReviewModeCalledWith = conflictFilterOn
        currentViewMode.value = TranslationViewMode.REVIEW
    }

    override fun restartAutoCommitTimer() {
        restartAutoCommitTimerCalled = true
    }

    override fun updateMergeFilter(on: Boolean) {
        updateMergeFilterCalledWith = on
    }

    override fun removeSource(sourceId: String) {
        removeSourceCalledWith = sourceId
    }

    override fun selectSource(sourceId: String) {
        selectSourceCalledWith = sourceId
    }

    override fun saveLastFocus(chapterId: String, frameId: String?) {
        saveLastFocusCalledWith = Pair(chapterId, frameId)
    }

    override fun openHome(withUpdate: Boolean) {
        openHomeCalledWith = withUpdate
    }

    override fun openDraft(translationId: String) {
        openDraftCalledWith = translationId
    }

    override fun openPublishProject(translationId: String) {
        openPublishProjectCalledWith = translationId
    }

    override fun openSettings() {
        openSettingsCalled = true
    }

    override fun showFeedbackDialog() {
        showFeedbackDialogCalled = true
    }

    override fun showSelectSourcesDialog() {
        showSelectSourcesDialogCalled = true
    }

    override fun showExportDialog(startFromPrint: Boolean) {
        showExportDialogCalledWith = startFromPrint
    }

    override fun dismissDialog() {
        dismissDialogCalled = true
    }
}
