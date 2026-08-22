package org.bibletranslationtools.writer.integration.ui.dialogs.source

import io.mockk.mockk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import org.bibletranslationtools.writer.core.Progress
import org.bibletranslationtools.writer.core.TargetTranslation
import org.bibletranslationtools.writer.ui.dialogs.source.RCItem
import org.bibletranslationtools.writer.ui.dialogs.source.SelectSourcesComponent

class FakeSelectSourcesComponent : SelectSourcesComponent {
    override val state = MutableStateFlow(SelectSourcesComponent.State())
    override val event: Flow<SelectSourcesComponent.Event> = MutableSharedFlow()
    override val progress = MutableStateFlow<Progress?>(null)
    override val targetTranslation: TargetTranslation = mockk(relaxed = true)

    var onConfirmSourcesCalled = false
    var onUpdateSourcesCalled = false

    override fun onUpdateSources() { onUpdateSourcesCalled = true }
    override fun onConfirmSources() { onConfirmSourcesCalled = true }
    override fun toggleSelection(source: RCItem) {}
    override fun downloadSource(source: RCItem) {}
    override fun deleteSource(source: RCItem) {}
}
