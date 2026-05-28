package org.bibletranslationtools.writer.integration.ui.dialogs.feedback

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import org.bibletranslationtools.writer.core.Progress
import org.bibletranslationtools.writer.ui.dialogs.feedback.FeedbackComponent

class FakeFeedbackComponent : FeedbackComponent {
    override val state = MutableStateFlow(FeedbackComponent.State())
    override val progress = MutableStateFlow<Progress?>(null)
    override val event: Flow<FeedbackComponent.Event> = MutableSharedFlow()
    override val initialMessage = ""

    var reportBugCalledWith: Pair<String, String>? = null
    var clearErrorCalled = false
    var clearReleaseCalled = false

    override fun reportBug(notes: String, email: String) {
        reportBugCalledWith = Pair(notes, email)
    }

    override fun clearError() {
        clearErrorCalled = true
    }

    override fun clearRelease() {
        clearReleaseCalled = true
    }
}
