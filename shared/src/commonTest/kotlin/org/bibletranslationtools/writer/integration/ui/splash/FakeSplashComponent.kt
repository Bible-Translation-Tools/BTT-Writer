package org.bibletranslationtools.writer.integration.ui.splash

import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.bibletranslationtools.writer.core.Progress
import org.bibletranslationtools.writer.ui.splash.SplashComponent

class FakeSplashComponent : SplashComponent {

    val _state = MutableStateFlow(SplashComponent.State())
    override val state: StateFlow<SplashComponent.State> = _state

    val _progress = MutableStateFlow<Progress?>(null)
    override val progress: StateFlow<Progress?> = _progress

    val _event = MutableSharedFlow<SplashComponent.Event>()
    override val event: Flow<SplashComponent.Event> = _event

    var onHardwareWarningContinuedCalled = false
    var onHardwareWarningDismissedAndSavedCalled = false
    var onMigrationAcceptedCalled = false
    var onMigrationDeclinedCalled = false
    var lastMigrateDir: PlatformFile? = null

    override fun onHardwareWarningContinued() {
        onHardwareWarningContinuedCalled = true
    }

    override fun onHardwareWarningDismissedAndSaved() {
        onHardwareWarningDismissedAndSavedCalled = true
    }

    override fun onMigrationAccepted() {
        onMigrationAcceptedCalled = true
    }

    override fun onMigrationDeclined() {
        onMigrationDeclinedCalled = true
    }

    override fun performMigrate(dir: PlatformFile?) {
        lastMigrateDir = dir
    }
}
