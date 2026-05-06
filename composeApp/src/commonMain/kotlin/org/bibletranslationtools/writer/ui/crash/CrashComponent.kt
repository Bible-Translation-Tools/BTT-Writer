package org.bibletranslationtools.writer.ui.crash

import btt_writer.composeapp.generated.resources.Res
import btt_writer.composeapp.generated.resources.checking_for_updates
import btt_writer.composeapp.generated.resources.uploading
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
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

interface CrashComponent {

    val state: StateFlow<CrashState>
    val progress: StateFlow<Progress?>
    val event: Flow<Event>

    val isNetworkAvailable: Boolean

    fun checkForLatestRelease()
    fun clearLatestRelease()
    fun uploadCrashReport()
    fun updateNotes(notes: String)
    fun flushAndRestart()

    data class CrashState(
        val notes: String = "",
        val success: Boolean = false,
        val latestRelease: CheckForLatestRelease.Release? = null
    )

    sealed interface Event {
        data object UploadError : Event
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

    override fun checkForLatestRelease() {
        launchWithProgress(Res.string.checking_for_updates) {
            val result = withContext(Dispatchers.IO) {
                checkForLatestRelease.execute()
            }
            if (result.release != null) {
                _state.update { it.copy(latestRelease = result.release) }
            } else {
                uploadCrashReport()
            }
        }
    }

    override fun clearLatestRelease() {
        _state.update { it.copy(latestRelease = null) }
    }

    override fun uploadCrashReport() {
        val notes = state.value.notes.ifBlank { return }

        launchWithProgress(Res.string.uploading) {
            val uploaded = withContext(Dispatchers.IO) {
                uploadCrashReport.execute(notes)
            }
            if (uploaded) {
                _state.update { it.copy(success = true) }
            } else {
                _event.trySend(CrashComponent.Event.UploadError)
            }
        }
    }

    override fun updateNotes(notes: String) {
        _state.update { it.copy(notes = notes) }
    }

    override fun flushAndRestart() {
        Logger.flush()
        onResult(CrashComponent.Result.Restart)
    }
}