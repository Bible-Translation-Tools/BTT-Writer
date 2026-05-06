package org.bibletranslationtools.writer.ui.dialogs.feedback

import btt_writer.composeapp.generated.resources.Res
import btt_writer.composeapp.generated.resources.checking_for_updates
import btt_writer.composeapp.generated.resources.input_required
import btt_writer.composeapp.generated.resources.internet_not_available
import btt_writer.composeapp.generated.resources.upload_feedback_failed
import btt_writer.composeapp.generated.resources.uploading_feedback
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
import org.bibletranslationtools.writer.Platform
import org.bibletranslationtools.writer.core.ComponentScope
import org.bibletranslationtools.writer.core.Progress
import org.bibletranslationtools.writer.core.ProgressManager
import org.bibletranslationtools.writer.core.ProgressOwner
import org.bibletranslationtools.writer.core.TaskHandle
import org.bibletranslationtools.writer.core.launchWithProgress
import org.bibletranslationtools.writer.usecases.CheckForLatestRelease
import org.bibletranslationtools.writer.usecases.UploadFeedback
import org.jetbrains.compose.resources.getString
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

interface FeedbackComponent {
    val state: StateFlow<FeedbackState>
    val progress: StateFlow<Progress?>
    val event: Flow<FeedbackEvent>

    val initialMessage: String

    fun reportBug(message: String)
    fun uploadFeedback(message: String)
    fun clearError()
    fun clearRelease()

    data class FeedbackState(
        val message: String = "",
        val release: CheckForLatestRelease.Release? = null,
        val uploadError: String? = null,
        val success: Boolean = false
    )

    sealed interface FeedbackEvent {
        data class SnackbarMessage(val message: String) : FeedbackEvent
    }
}

class DefaultFeedbackComponent(
    componentContext: ComponentContext,
    override val initialMessage: String = ""
) : FeedbackComponent,
    ComponentContext by componentContext,
    ComponentScope, ProgressOwner, KoinComponent {

    private val checkForLatestRelease: CheckForLatestRelease by inject()
    private val uploadFeedback: UploadFeedback by inject()
    private val platform: Platform by inject()

    override val coroutineScope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())

    private val progressManager = ProgressManager(coroutineScope)
    override val progress get() = progressManager.progress

    private val _state = MutableStateFlow(FeedbackComponent.FeedbackState())
    override val state = _state.asStateFlow()

    private val _event = Channel<FeedbackComponent.FeedbackEvent>(Channel.BUFFERED)
    override val event = _event.receiveAsFlow()

    init {
        lifecycle.doOnDestroy {
            coroutineScope.cancel()
        }
    }

    override suspend fun runTask(message: String?, block: suspend (TaskHandle) -> Unit) {
        progressManager.runTask(message, block)
    }

    override fun reportBug(message: String) {
        launchWithProgress { handle ->
            if (message.isEmpty()) {
                val msg = getString(Res.string.input_required)
                _event.trySend(FeedbackComponent.FeedbackEvent.SnackbarMessage(msg))
                return@launchWithProgress
            }

            _state.update { it.copy(message = message) }

            checkForLatestRelease(handle)
        }
    }

    override fun uploadFeedback(message: String) {
        launchWithProgress { handle ->
            if (message.isEmpty()) {
                val msg = getString(Res.string.input_required)
                _event.trySend(FeedbackComponent.FeedbackEvent.SnackbarMessage(msg))
                return@launchWithProgress
            }

            doUploadFeedback(message, handle)
        }
    }

    override fun clearError() {
        _state.update { it.copy(uploadError = null) }
    }

    override fun clearRelease() {
        _state.update { it.copy(release = null) }
    }

    private suspend fun checkForLatestRelease(handle: TaskHandle) {
        handle.update(-1f, getString(Res.string.checking_for_updates))
        val result = withContext(Dispatchers.IO) {
            checkForLatestRelease.execute()
        }
        if (result.release != null) {
            _state.update { it.copy(release = result.release) }
        } else {
            doUploadFeedback(_state.value.message, handle)
        }
    }

    private suspend fun doUploadFeedback(message: String, handle: TaskHandle) {
        handle.update(-1f, getString(Res.string.uploading_feedback))
        val success = withContext(Dispatchers.IO) {
            uploadFeedback.execute(message)
        }
        if (success) {
            _state.update { it.copy(success = true) }
        } else {
            val msg = if (platform.isNetworkAvailable) {
                getString(Res.string.upload_feedback_failed)
            } else {
                getString(Res.string.internet_not_available)
            }
            _state.update { it.copy(uploadError = msg) }
        }
    }
}


