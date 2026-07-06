package org.bibletranslationtools.writer.ui.crash

import btt_writer.shared.generated.resources.Res
import btt_writer.shared.generated.resources.checking_for_updates
import btt_writer.shared.generated.resources.input_required
import btt_writer.shared.generated.resources.uploading
import btt_writer.shared.generated.resources.uploading_feedback
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
import kotlinx.coroutines.withContext
import org.bibletranslationtools.logger.Logger
import org.bibletranslationtools.writer.Platform
import org.bibletranslationtools.writer.core.ComponentScope
import org.bibletranslationtools.writer.core.Progress
import org.bibletranslationtools.writer.core.ProgressManager
import org.bibletranslationtools.writer.core.ProgressOwner
import org.bibletranslationtools.writer.core.TaskHandle
import org.bibletranslationtools.writer.core.launchWithProgress
import org.bibletranslationtools.writer.usecases.CheckForLatestRelease
import org.bibletranslationtools.writer.usecases.UploadCrashReport
import org.jetbrains.compose.resources.getString
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

interface CrashComponent {

    val state: StateFlow<CrashState>
    val progress: StateFlow<Progress?>
    val event: Flow<Event>

    val isNetworkAvailable: Boolean

    fun clearLatestRelease()
    fun sendCrashReport(notes: String, email: String)
    fun flushAndRestart()

    data class CrashState(
        val notes: String = "",
        val email: String = "",
        val success: Boolean = false,
        val release: CheckForLatestRelease.Release? = null
    )

    sealed interface Event {
        data object UploadError : Event
        data class SnackbarMessage(val message: String) : Event
    }

    sealed interface Result {
        data object Restart : Result
        data object Exit : Result
    }
}

class DefaultCrashComponent(
    componentContext: ComponentContext,
    private val onResult: (CrashComponent.Result) -> Unit
) : CrashComponent,
        ComponentContext by componentContext,
        KoinComponent, ComponentScope, ProgressOwner {

    private val checkForLatestRelease: CheckForLatestRelease by inject()
    private val uploadCrashReport: UploadCrashReport by inject()
    private val platform: Platform by inject()

    override val coroutineScope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())

    private val progressManager = ProgressManager(coroutineScope)
    override val progress get() = progressManager.progress

    private val _state = MutableStateFlow(CrashComponent.CrashState())
    override val state: StateFlow<CrashComponent.CrashState> = _state.asStateFlow()

    private val _event = Channel<CrashComponent.Event>(Channel.BUFFERED)
    override val event = _event.receiveAsFlow()

    override val isNetworkAvailable: Boolean get() = platform.isNetworkAvailable

    init {
        lifecycle.doOnDestroy {
            coroutineScope.cancel()
        }
    }

    override suspend fun runTask(message: String?, block: suspend (TaskHandle) -> Unit) {
        progressManager.runTask(message, block)
    }

    override fun clearLatestRelease() {
        _state.update { it.copy(release = null) }
    }

    override fun sendCrashReport(notes: String, email: String) {
        launchWithProgress(Res.string.uploading) { handle ->
            if (notes.isBlank()) {
                val msg = getString(Res.string.input_required)
                _event.trySend(CrashComponent.Event.SnackbarMessage(msg))
                return@launchWithProgress
            }

            _state.update { it.copy(notes = notes, email = email) }

            checkForLatestRelease(handle)
        }
    }

    override fun flushAndRestart() {
        Logger.flush()
        onResult(CrashComponent.Result.Restart)
    }

    private suspend fun checkForLatestRelease(handle: TaskHandle) {
        handle.update(-1f, getString(Res.string.checking_for_updates))
        val result = withContext(Dispatchers.IO) {
            checkForLatestRelease.execute()
        }
        if (result.release != null) {
            _state.update { it.copy(release = result.release) }
        } else {
            doSendCrashReport(handle)
        }
    }

    private suspend fun doSendCrashReport(handle: TaskHandle) {
        val notes = _state.value.notes
        val email = _state.value.email

        handle.update(-1f, getString(Res.string.uploading_feedback))

        val uploaded = withContext(Dispatchers.IO) {
            uploadCrashReport.execute(notes, email)
        }
        if (uploaded) {
            _state.update { it.copy(success = true) }
        } else {
            _event.trySend(CrashComponent.Event.UploadError)
        }
    }
}