package org.bibletranslationtools.writer.integration.ui.crash

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.bibletranslationtools.writer.core.Progress
import org.bibletranslationtools.writer.ui.crash.CrashComponent

class FakeCrashComponent : CrashComponent {

    val _state = MutableStateFlow(CrashComponent.CrashState())
    override val state: StateFlow<CrashComponent.CrashState> = _state

    override val progress: StateFlow<Progress?> = MutableStateFlow(null)
    override val event: Flow<CrashComponent.Event> = MutableSharedFlow()
    override val isNetworkAvailable: Boolean = true

    var clearLatestReleaseCalled = false
    var sendCrashReportCalled = false
    var lastNotes: String? = null
    var lastEmail: String? = null
    var flushAndRestartCalled = false

    override fun clearLatestRelease() {
        clearLatestReleaseCalled = true
    }

    override fun sendCrashReport(notes: String, email: String) {
        sendCrashReportCalled = true
        lastNotes = notes
        lastEmail = email
    }

    override fun flushAndRestart() {
        flushAndRestartCalled = true
    }
}
