package org.bibletranslationtools.writer.integration.ui.devtools

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import org.bibletranslationtools.writer.core.Progress
import org.bibletranslationtools.writer.ui.devtools.DevToolsComponent

class FakeDevToolsComponent : DevToolsComponent {
    override val versionName: String = "1.0.0-mock"
    override val versionCode: Int = 42
    override val udid: String = "mock-udid-1234"

    override val state = MutableStateFlow(DevToolsComponent.State())
    override val event = MutableSharedFlow<DevToolsComponent.Event>()
    override val progress = MutableStateFlow<Progress?>(null)

    var loadToolsCalled = false
    var readErrorLogCalled = false
    var clearKeysRegeneratedCalled = false
    var calculateSystemResourcesCalled = false
    var navigateBackCalled = false

    override fun loadTools() {
        loadToolsCalled = true
    }

    override fun readErrorLog() {
        readErrorLogCalled = true
    }

    override fun clearKeysRegenerated() {
        clearKeysRegeneratedCalled = true
    }

    override fun calculateSystemResources(): String {
        calculateSystemResourcesCalled = true
        return "Calculated mock system resources"
    }

    override fun navigateBack() {
        navigateBackCalled = true
    }
}
