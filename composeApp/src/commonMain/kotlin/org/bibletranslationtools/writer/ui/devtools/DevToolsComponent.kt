package org.bibletranslationtools.writer.ui.devtools

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Warning
import btt_writer.composeapp.generated.resources.Res
import btt_writer.composeapp.generated.resources.check_system_resources
import btt_writer.composeapp.generated.resources.check_system_resources_hint
import btt_writer.composeapp.generated.resources.delete_library
import btt_writer.composeapp.generated.resources.delete_library_hint
import btt_writer.composeapp.generated.resources.deleting_library
import btt_writer.composeapp.generated.resources.please_wait
import btt_writer.composeapp.generated.resources.read_debug_log
import btt_writer.composeapp.generated.resources.read_debug_log_hint
import btt_writer.composeapp.generated.resources.reading_logs
import btt_writer.composeapp.generated.resources.recreate_keys
import btt_writer.composeapp.generated.resources.regenerate_ssh_keys
import btt_writer.composeapp.generated.resources.regenerate_ssh_keys_hint
import btt_writer.composeapp.generated.resources.simulate_crash
import btt_writer.composeapp.generated.resources.simulating_crash
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.bibletranslationtools.logger.LogEntry
import org.bibletranslationtools.logger.Logger
import org.bibletranslationtools.resourcecatalog.ResourceCatalogClient
import org.bibletranslationtools.writer.DirectoryProvider
import org.bibletranslationtools.writer.Platform
import org.bibletranslationtools.writer.core.ComponentScope
import org.bibletranslationtools.writer.core.Progress
import org.bibletranslationtools.writer.core.ProgressManager
import org.bibletranslationtools.writer.core.ProgressOwner
import org.bibletranslationtools.writer.core.TaskHandle
import org.bibletranslationtools.writer.core.launchWithProgress
import org.jetbrains.compose.resources.getString
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

interface DevToolsComponent {

    val versionName: String
    val versionCode: Int
    val udid: String

    val state: StateFlow<State>
    val event: Flow<Event>
    val progress: StateFlow<Progress?>

    fun loadTools()
    fun readErrorLog()
    fun clearKeysRegenerated()
    fun calculateSystemResources(): String

    fun navigateBack()

    data class State(
        val versionName: String = "",
        val versionCode: String = "",
        val udid: String = "",
        val tools: List<ToolItem> = emptyList(),
        val logs: List<LogEntry> = emptyList(),
        val keysRegenerated: Boolean? = null,
    )

    sealed class Event {
        data object ReadLog : Event()
        data object CheckSystemResources : Event()
    }

    sealed interface Result {
        data object NavigateBack : Result
    }
}

class DefaultDevToolsComponent(
    componentContext: ComponentContext,
    private val onResult: (DevToolsComponent.Result) -> Unit
) : DevToolsComponent,
    ComponentContext by componentContext,
    KoinComponent, ProgressOwner, ComponentScope {

    companion object {
        val TAG = DevToolsComponent::javaClass.name
    }

    private val directoryProvider: DirectoryProvider by inject()
    private val catalogClient: ResourceCatalogClient by inject()
    private val platform: Platform by inject()

    override val coroutineScope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())

    private val progressManager = ProgressManager(coroutineScope)
    override val progress get() = progressManager.progress

    private val _state = MutableStateFlow(DevToolsComponent.State())
    override val state: StateFlow<DevToolsComponent.State> = _state.asStateFlow()

    private val _event = Channel<DevToolsComponent.Event>()
    override val event = _event.receiveAsFlow()

    override val versionName = platform.info.versionName
    override val versionCode = platform.info.versionCode
    override val udid = platform.udid

    init {
        lifecycle.doOnDestroy {
            coroutineScope.cancel()
        }
    }

    override suspend fun runTask(message: String?, block: suspend (TaskHandle) -> Unit) {
        progressManager.runTask(message, block)
    }

    override fun loadTools() {
        launchWithProgress(Res.string.please_wait) {
            val list = listOf(
                getGenerateSSHKeysItem(),
                readLogItem(),
                simulateCrashItem(),
                checkSystemResourcesItem(),
                deleteLibraryItem()
            )
            _state.update { it.copy(tools = list) }
        }
    }

    override fun readErrorLog() {
        launchWithProgress(Res.string.reading_logs) {
            val logs = withContext(Dispatchers.IO) {
                Logger.getLogEntries()
            }
            _state.update { it.copy(logs = logs) }
        }
    }

    override fun clearKeysRegenerated() {
        _state.update { it.copy(keysRegenerated = null) }
    }

    override fun calculateSystemResources(): String {
        return platform.calculateSystemResources()
    }

    override fun navigateBack() {
        onResult(DevToolsComponent.Result.NavigateBack)
    }

    private suspend fun getGenerateSSHKeysItem(): ToolItem {
        return ToolItem(
            getString(Res.string.regenerate_ssh_keys),
            getString(Res.string.regenerate_ssh_keys_hint),
            Icons.Outlined.Security,
            action = ::generateSSHKeys
        )
    }

    private suspend fun readLogItem(): ToolItem {
        return ToolItem(
            getString(Res.string.read_debug_log),
            getString(Res.string.read_debug_log_hint),
            Icons.Outlined.Description
        ) {
            coroutineScope.launch {
                _event.send(DevToolsComponent.Event.ReadLog)
            }
        }
    }

    private suspend fun simulateCrashItem(): ToolItem {
        val simulatingCrash = getString(Res.string.simulating_crash)
        return ToolItem(
            getString(Res.string.simulate_crash),
            "",
            Icons.Outlined.Warning
        ) {
            throw IllegalStateException(simulatingCrash)
        }
    }

    private suspend fun checkSystemResourcesItem(): ToolItem {
        return ToolItem(
            getString(Res.string.check_system_resources),
            getString(Res.string.check_system_resources_hint),
            Icons.Outlined.Description
        ) {
            coroutineScope.launch {
                _event.send(DevToolsComponent.Event.CheckSystemResources)
            }
        }
    }

    private suspend fun deleteLibraryItem(): ToolItem {
        return ToolItem(
            getString(Res.string.delete_library),
            getString(Res.string.delete_library_hint),
            Icons.Outlined.Delete
        ) {
            deleteLibrary()
        }
    }

    private fun generateSSHKeys() {
        launchWithProgress(Res.string.recreate_keys) {
            val generated = withContext(Dispatchers.IO) {
                directoryProvider.generateSSHKeys(platform.udid)
                true
            }
            _state.update { it.copy(keysRegenerated = generated) }
        }
    }

    private fun deleteLibrary() {
        launchWithProgress(Res.string.deleting_library) {
            withContext(Dispatchers.IO) {
                catalogClient.closeLibrary()
                try {
                    directoryProvider.deleteLibrary()
                    directoryProvider.deployDefaultLibrary()
                } catch (e: Exception) {
                    Logger.w(TAG, "Failed to delete library", e)
                } finally {
                    catalogClient.openLibrary()
                }
            }
        }
    }
}