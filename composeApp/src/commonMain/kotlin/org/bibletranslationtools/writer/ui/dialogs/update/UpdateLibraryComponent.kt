package org.bibletranslationtools.writer.ui.dialogs.update

import btt_writer.composeapp.generated.resources.Res
import btt_writer.composeapp.generated.resources.check_for_updates
import btt_writer.composeapp.generated.resources.checking_for_updates
import btt_writer.composeapp.generated.resources.error
import btt_writer.composeapp.generated.resources.have_latest_app_update
import btt_writer.composeapp.generated.resources.import_index_failed
import btt_writer.composeapp.generated.resources.importing_index
import btt_writer.composeapp.generated.resources.options_update_failed
import btt_writer.composeapp.generated.resources.success
import btt_writer.composeapp.generated.resources.update_languages_success
import btt_writer.composeapp.generated.resources.updating_languages
import btt_writer.composeapp.generated.resources.updating_sources
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.doOnDestroy
import io.github.vinceglb.filekit.PlatformFile
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
import org.bibletranslationtools.writer.core.ComponentScope
import org.bibletranslationtools.writer.core.Progress
import org.bibletranslationtools.writer.core.ProgressManager
import org.bibletranslationtools.writer.core.ProgressOwner
import org.bibletranslationtools.writer.core.TaskHandle
import org.bibletranslationtools.writer.core.launchWithProgress
import org.bibletranslationtools.writer.displayName
import org.bibletranslationtools.writer.usecases.CheckForLatestRelease
import org.bibletranslationtools.writer.usecases.ImportIndex
import org.bibletranslationtools.writer.usecases.UpdateCatalogs
import org.bibletranslationtools.writer.usecases.UpdateSource
import org.jetbrains.compose.resources.getString
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

interface UpdateLibraryComponent {
    val state: StateFlow<State>
    val progress: StateFlow<Progress?>
    val event: Flow<Event>

    fun openDownloadSources()
    fun updateSources()
    fun importIndex(file: PlatformFile)
    fun downloadIndex()
    fun updateLanguages()
    fun checkAppUpdate()
    fun clearResult()
    fun clearLatestRelease()
    fun clearUpdateSourceResult()

    data class State(
        val resultMessage: Pair<String, String>? = null,
        val latestRelease: CheckForLatestRelease.Release? = null,
        val updateSourceResult: UpdateSource.Result? = null
    )

    sealed interface Event {
        data object IndexUpdated : Event
    }

    sealed interface Result {
        data object OpenDownloadSources : Result
    }
}

class DefaultUpdateLibraryComponent(
    componentContext: ComponentContext,
    triggerUpdate: Boolean = false,
    private val onResult: (UpdateLibraryComponent.Result) -> Unit
) : UpdateLibraryComponent,
    ComponentContext by componentContext,
    ComponentScope, ProgressOwner, KoinComponent {

    private val importIndex: ImportIndex by inject()
    private val updateCatalogs: UpdateCatalogs by inject()
    private val checkForLatestRelease: CheckForLatestRelease by inject()
    private val updateSource: UpdateSource by inject()

    override val coroutineScope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())

    private val progressManager = ProgressManager(coroutineScope)
    override val progress get() = progressManager.progress

    private val _state = MutableStateFlow(UpdateLibraryComponent.State())
    override val state = _state.asStateFlow()

    private val _event = Channel<UpdateLibraryComponent.Event>(Channel.BUFFERED)
    override val event = _event.receiveAsFlow()

    init {
        if (triggerUpdate) {
            updateSources()
        }

        lifecycle.doOnDestroy {
            coroutineScope.cancel()
        }
    }

    override suspend fun runTask(message: String?, block: suspend (TaskHandle) -> Unit) {
        progressManager.runTask(message, block)
    }

    override fun openDownloadSources() {
        onResult(UpdateLibraryComponent.Result.OpenDownloadSources)
    }

    override fun updateSources() {
        launchWithProgress(Res.string.updating_sources) { handle ->
            val result = withContext(Dispatchers.IO) {
                updateSource.execute { progress, details ->
                    handle.update(progress, handle.initialMessage, details)
                }
            }

            if (result.success) {
                _state.update { it.copy(updateSourceResult = result) }
            } else {
                updateResultMessage(
                    title = getString(Res.string.error),
                    message = getString(Res.string.options_update_failed)
                )
            }
        }
    }

    override fun importIndex(file: PlatformFile) {
        val filename = file.displayName
        val isSqlite = filename.contains(".sqlite", ignoreCase = true)

        launchWithProgress(Res.string.importing_index) {
            if (isSqlite) {
                val success = withContext(Dispatchers.IO) {
                    importIndex.import(file)
                }
                if (success) {
                    _event.trySend(UpdateLibraryComponent.Event.IndexUpdated)
                } else {
                    updateResultMessage(
                        title = getString(Res.string.error),
                        message = getString(Res.string.options_update_failed)
                    )
                }
            } else {
                updateResultMessage(
                    title = getString(Res.string.error),
                    message = getString(Res.string.import_index_failed)
                )
            }
        }
    }

    override fun downloadIndex() {
        launchWithProgress(Res.string.importing_index) { handle ->
            val success = withContext(Dispatchers.IO) {
                importIndex.download { progress, message ->
                    handle.update(progress, message)
                }
            }
            if (success) {
                _event.trySend(UpdateLibraryComponent.Event.IndexUpdated)
            } else {
                updateResultMessage(
                    title = getString(Res.string.error),
                    message = getString(Res.string.options_update_failed)
                )
            }
        }
    }

    override fun updateLanguages() {
        launchWithProgress(Res.string.updating_languages) { handle ->
            val result = withContext(Dispatchers.IO) {
                updateCatalogs.execute(true) { progress, details ->
                    handle.update(progress, handle.initialMessage, details)
                }
            }
            if (result.success) {
                updateResultMessage(
                    title = getString(Res.string.success),
                    message = getString(
                        Res.string.update_languages_success,
                        result.addedCount
                    )
                )
            } else {
                updateResultMessage(
                    title = getString(Res.string.error),
                    message = getString(Res.string.options_update_failed)
                )
            }
        }
    }

    override fun checkAppUpdate() {
        launchWithProgress(Res.string.checking_for_updates) {
            val result = withContext(Dispatchers.IO) {
                checkForLatestRelease.execute()
            }

            if (result.release != null) {
                _state.update { it.copy(latestRelease = result.release) }
            } else {
                updateResultMessage(
                    title = getString(Res.string.check_for_updates),
                    message = getString(Res.string.have_latest_app_update)
                )
            }
        }
    }

    override fun clearResult() {
        _state.update { it.copy(resultMessage = null) }
    }

    override fun clearLatestRelease() {
        _state.update { it.copy(latestRelease = null) }
    }

    override fun clearUpdateSourceResult() {
        _state.update { it.copy(updateSourceResult = null) }
    }

    private fun updateResultMessage(title: String, message: String) {
        _state.update {
            it.copy(resultMessage = title to message)
        }
    }
}
