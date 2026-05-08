package org.bibletranslationtools.writer.ui.dialogs.export

import btt_writer.composeapp.generated.resources.Res
import btt_writer.composeapp.generated.resources.backup_to_sd
import btt_writer.composeapp.generated.resources.change_detected
import btt_writer.composeapp.generated.resources.creating_repository
import btt_writer.composeapp.generated.resources.download_failed
import btt_writer.composeapp.generated.resources.downloading_images
import btt_writer.composeapp.generated.resources.downloading_images_for_print_failed
import btt_writer.composeapp.generated.resources.error
import btt_writer.composeapp.generated.resources.export_failed
import btt_writer.composeapp.generated.resources.export_success
import btt_writer.composeapp.generated.resources.exporting
import btt_writer.composeapp.generated.resources.internet_not_available
import btt_writer.composeapp.generated.resources.log_out
import btt_writer.composeapp.generated.resources.merge_request
import btt_writer.composeapp.generated.resources.pref_default_reader_server
import btt_writer.composeapp.generated.resources.print_failed
import btt_writer.composeapp.generated.resources.print_success
import btt_writer.composeapp.generated.resources.printing
import btt_writer.composeapp.generated.resources.pulling_repo
import btt_writer.composeapp.generated.resources.push_rejected
import btt_writer.composeapp.generated.resources.registering_keys
import btt_writer.composeapp.generated.resources.success
import btt_writer.composeapp.generated.resources.target_translation_not_found
import btt_writer.composeapp.generated.resources.title_export_usfm
import btt_writer.composeapp.generated.resources.translation_export_failed
import btt_writer.composeapp.generated.resources.upload_failed
import btt_writer.composeapp.generated.resources.uploading
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
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.bibletranslationtools.logger.Logger
import org.bibletranslationtools.resourcecatalog.ResourceCatalogClient
import org.bibletranslationtools.resourcecontainer.Project
import org.bibletranslationtools.writer.DirectoryProvider
import org.bibletranslationtools.writer.Platform
import org.bibletranslationtools.writer.core.ComponentScope
import org.bibletranslationtools.writer.core.MergeConflictsHandler
import org.bibletranslationtools.writer.core.Profile
import org.bibletranslationtools.writer.core.Progress
import org.bibletranslationtools.writer.core.ProgressManager
import org.bibletranslationtools.writer.core.ProgressOwner
import org.bibletranslationtools.writer.core.TargetTranslation
import org.bibletranslationtools.writer.core.TaskHandle
import org.bibletranslationtools.writer.core.Translator
import org.bibletranslationtools.writer.core.launchWithProgress
import org.bibletranslationtools.writer.data.Preference
import org.bibletranslationtools.writer.data.getPref
import org.bibletranslationtools.writer.displayName
import org.bibletranslationtools.writer.usecases.CreateRepository
import org.bibletranslationtools.writer.usecases.DownloadImages
import org.bibletranslationtools.writer.usecases.ExportProjects
import org.bibletranslationtools.writer.usecases.GogsLogout
import org.bibletranslationtools.writer.usecases.PullTargetTranslation
import org.bibletranslationtools.writer.usecases.PushTargetTranslation
import org.bibletranslationtools.writer.usecases.RegisterSSHKeys
import org.bibletranslationtools.writer.utils.getStringBlocking
import org.eclipse.jgit.merge.MergeStrategy
import org.jetbrains.compose.resources.getString
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File

data class DialogMessage(
    val title: String,
    val message: String
)

data class UploadSuccess(
    val url: String,
    val details: String? = null
)

interface ExportComponent {

    val state: StateFlow<State>
    val event: Flow<Event>
    val progress: StateFlow<Progress?>

    val targetTranslation: TargetTranslation
    val projectName: String
    val projectTitle: String
    val showPrint: Boolean

    fun onMergeConflict()
    fun showFeedbackDialog(message: String)

    fun printPdf(file: PlatformFile, includeImages: Boolean, includeIncomplete: Boolean)
    fun exportUsfm(file: PlatformFile)
    fun exportProject(file: PlatformFile)
    fun exportToApp()
    fun openExportToCloud()
    fun logout(thenLogin: Boolean)
    fun registerKeys()
    fun resetToMaster()
    fun clearInfoMessage()
    fun clearErrorMessage()
    fun clearUploadSuccess()
    fun clearMergeConflict()

    data class State(
        val info: DialogMessage? = null,
        val uploadError: DialogMessage? = null,
        val mergeConflict: DialogMessage? = null,
        val uploadSuccess: UploadSuccess? = null
    )

    sealed interface Event {
        data class SnackbarMessage(val message: String) : Event
        data object AuthRequested : Event
    }

    sealed interface Result {
        data class Error(val text: String) : Result
        data class ExportToApp(val file: File) : Result
        data object OpenLogin : Result
        data object Logout : Result
        data class MergeConflict(val translationId: String) : Result
        data class OpenFeedback(val message: String) : Result
    }
}

class DefaultExportComponent(
    componentContext: ComponentContext,
    translationId: String,
    override val showPrint: Boolean,
    private val onResult: (ExportComponent.Result) -> Unit
) : ExportComponent,
    ComponentContext by componentContext,
    KoinComponent, ComponentScope, ProgressOwner {

    private val export: ExportProjects by inject()
    private val downloadImages: DownloadImages by inject()
    private val translator: Translator by inject()
    private val profile: Profile by inject()
    private val directoryProvider: DirectoryProvider by inject()
    private val catalogClient: ResourceCatalogClient by inject()
    private val gogsLogout: GogsLogout by inject()
    private val createRepository: CreateRepository by inject()
    private val pullTargetTranslation: PullTargetTranslation by inject()
    private val pushTargetTranslation: PushTargetTranslation by inject()
    private val registerSSHKeys: RegisterSSHKeys by inject()
    private val preference: Preference by inject()
    private val platform: Platform by inject()

    override val coroutineScope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())

    private val progressManager = ProgressManager(coroutineScope)
    override val progress get() = progressManager.progress

    private val _state = MutableStateFlow(ExportComponent.State())
    override val state: StateFlow<ExportComponent.State> = _state.asStateFlow()

    private val _event = Channel<ExportComponent.Event>(Channel.BUFFERED)
    override val event = _event.receiveAsFlow()

    override lateinit var targetTranslation: TargetTranslation
    override lateinit var projectName: String
    override lateinit var projectTitle: String

    companion object {
        private const val TAG = "ExportComponent"
    }

    init {
        val translation = runBlocking {
            translator.getTargetTranslation(translationId)
        }

        if (translation == null) {
            val error = getStringBlocking(Res.string.target_translation_not_found)
            onResult(ExportComponent.Result.Error(error))
        } else {
            targetTranslation = translation
            projectName = getProject()?.name ?: targetTranslation.projectId
            projectTitle = "$projectName - ${targetTranslation.targetLanguageName}"
        }

        lifecycle.doOnDestroy {
            coroutineScope.cancel()
        }
    }

    override suspend fun runTask(message: String?, block: suspend (TaskHandle) -> Unit) {
        progressManager.runTask(message, block)
    }

    override fun onMergeConflict() {
        onResult(ExportComponent.Result.MergeConflict(targetTranslation.id))
    }

    override fun showFeedbackDialog(message: String) {
        onResult(ExportComponent.Result.OpenFeedback(message))
    }

    override fun printPdf(file: PlatformFile, includeImages: Boolean, includeIncomplete: Boolean) {
        launchWithProgress { handle ->
            if (!validateUriExtension(file, Translator.PDF_EXTENSION)) {
                reportExportFailed()
                return@launchWithProgress
            }

            val printingMsg = getString(Res.string.printing, projectTitle)

            handle.update(-1f, printingMsg)

            val imagesDir = if (includeImages) {
                handle.update(
                    value = 1f,
                    message = getString(Res.string.downloading_images)
                )
                withContext(Dispatchers.IO) {
                    downloadImages.download { progress, message ->
                        handle.update(progress, message)
                    }
                }
            } else null

            if (includeImages && imagesDir?.exists() == false) {
                val title = getString(Res.string.download_failed)
                val message = getString(Res.string.downloading_images_for_print_failed)
                _state.update {
                    it.copy(info = DialogMessage(title, message))
                }
                return@launchWithProgress
            }

            handle.update(-1f, printingMsg)

            val result = withContext(Dispatchers.IO) {
                export.exportPDF(
                    targetTranslation,
                    file,
                    includeImages,
                    includeIncomplete,
                    imagesDir
                )
            }

            val (title, message) = if (result.success) {
                val title = getString(Res.string.success)
                val message = getString(
                    Res.string.print_success,
                    result.file.displayName
                )
                title to message
            } else {
                val title = getString(Res.string.error)
                val message = getString(Res.string.print_failed)
                title to message
            }

            _state.update {
                it.copy(info = DialogMessage(title, message))
            }
        }
    }

    override fun exportUsfm(file: PlatformFile) {
        launchWithProgress(Res.string.exporting) {
            if (!validateUriExtension(file, Translator.USFM_EXTENSION)) {
                reportExportFailed()
                return@launchWithProgress
            }

            val result = withContext(Dispatchers.IO) {
                export.exportUSFM(targetTranslation, file)
            }

            val title = getString(Res.string.title_export_usfm)
            val message = if (result.success) {
                getString(Res.string.export_success, result.file.displayName)
            } else {
                getString(Res.string.export_failed)
            }

            _state.update {
                it.copy(info = DialogMessage(title, message))
            }
        }
    }

    override fun exportProject(file: PlatformFile) {
        launchWithProgress(Res.string.exporting) {
            if (!validateUriExtension(file, Translator.TSTUDIO_EXTENSION)) {
                reportExportFailed()
                return@launchWithProgress
            }

            val result = withContext(Dispatchers.IO) {
                export.exportProject(targetTranslation, file)
            }

            val title = getString(Res.string.backup_to_sd)
            val message = if (result.success) {
                getString(Res.string.export_success, result.file.displayName)
            } else {
                getString(Res.string.export_failed)
            }

            _state.update {
                it.copy(info = DialogMessage(title, message))
            }
        }
    }

    override fun exportToApp() {
        launchWithProgress(Res.string.exporting) {
            val exportFile = withContext(Dispatchers.IO) {
                try {
                    val filename = "${targetTranslation.id}.${Translator.TSTUDIO_EXTENSION}"
                    val exportFile = File(directoryProvider.sharingDir, filename)
                    export.exportProject(targetTranslation, exportFile)
                    exportFile
                } catch (e: Exception) {
                    Logger.e(
                        TAG,
                        "Failed to export the target translation ${targetTranslation.id}",
                        e
                    )
                    null
                }
            }

            if (exportFile?.exists() == true) {
                onResult(ExportComponent.Result.ExportToApp(exportFile))
            } else {
                _event.trySend(
                    ExportComponent.Event.SnackbarMessage(
                        getString(Res.string.translation_export_failed)
                    ))
            }
        }
    }

    override fun openExportToCloud() {
        launchWithProgress { handle ->
            pullTargetTranslation(MergeStrategy.RECURSIVE, handle)
        }
    }

    override fun logout(thenLogin: Boolean) {
        launchWithProgress(Res.string.log_out) {
            withContext(Dispatchers.IO) {
                gogsLogout.execute()
                profile.logout()
            }

            if (thenLogin) {
                onResult(ExportComponent.Result.OpenLogin)
            } else {
                onResult(ExportComponent.Result.Logout)
            }
        }
    }

    override fun registerKeys() {
        forceRegisterSSHKeys()
    }

    override fun resetToMaster() {
        launchWithProgress {
            withContext(Dispatchers.IO) {
                targetTranslation.resetToMasterBackup()
            }
        }
    }

    override fun clearInfoMessage() {
        _state.update { it.copy(info = null) }
    }

    override fun clearErrorMessage() {
        _state.update { it.copy(uploadError = null) }
    }

    override fun clearUploadSuccess() {
        _state.update { it.copy(uploadSuccess = null) }
    }

    override fun clearMergeConflict() {
        _state.update { it.copy(mergeConflict = null) }
    }

    private suspend fun pullTargetTranslation(strategy: MergeStrategy, handle: TaskHandle) {
        handle.update(
            value = -1f,
            message = getString(Res.string.pulling_repo)
        )
        val result = withContext(Dispatchers.IO) {
            pullTargetTranslation.execute(
                targetTranslation,
                strategy,
                null
            ) { progress, message ->
                handle.update(progress, message)
            }
        }

        when (result.status) {
            PullTargetTranslation.Status.UP_TO_DATE,
            PullTargetTranslation.Status.UNKNOWN -> {
                Logger.i(
                    TAG,
                    "Changes on the server were synced with ${targetTranslation.id}"
                )
                pushTargetTranslation(handle)
            }
            PullTargetTranslation.Status.AUTH_FAILURE -> {
                Logger.i(TAG, "Authentication failed: ${result.message}")
                if (!directoryProvider.hasSSHKeys()) {
                    registerSSHKeys(false, handle)
                } else {
                    _event.trySend(ExportComponent.Event.AuthRequested)
                }
            }
            PullTargetTranslation.Status.NO_REMOTE_REPO -> {
                Logger.i(
                    TAG,
                    "The repository ${targetTranslation.id} could not be found"
                )
                createRepository(handle)
            }
            PullTargetTranslation.Status.MERGE_CONFLICTS -> {
                Logger.i(
                    TAG,
                    "The server contains conflicting changes for ${targetTranslation.id}"
                )
                val conflicted = MergeConflictsHandler.isTranslationMergeConflicted(
                    targetTranslation.id,
                    translator
                )
                if (!conflicted) {
                    // probably the manifest or license gave a false positive
                    Logger.i(
                        TAG,
                        "Changes on the server were synced with ${targetTranslation.id}"
                    )
                    pushTargetTranslation(handle)
                } else {
                    val title = getString(Res.string.change_detected)
                    val message = getString(
                        Res.string.merge_request,
                        targetTranslation.projectId,
                        targetTranslation.targetLanguageName
                    )
                    _state.update {
                        it.copy(mergeConflict = DialogMessage(title, message))
                    }
                }
            }
            else -> {
                reportUploadFailed()
            }
        }
    }

    private suspend fun pushTargetTranslation(handle: TaskHandle) {
        handle.update(
            value = -1f,
            message = getString(Res.string.uploading)
        )
        val result = withContext(Dispatchers.IO) {
            pushTargetTranslation.execute(targetTranslation) { progress, message ->
                handle.update(progress, message)
            }
        }
        when {
            result.status == PushTargetTranslation.Status.OK -> {
                Logger.i(
                    TAG,
                    "The target translation " + targetTranslation.id + " was pushed to the server"
                )
                reportUploadSuccess(result.message)
            }
            result.status == PushTargetTranslation.Status.AUTH_FAILURE -> {
                Logger.w(TAG, "Authentication failed: ${result.message}")
                _event.trySend(ExportComponent.Event.AuthRequested)
            }
            result.status.isRejected -> {
                Logger.w(TAG, "Push Rejected: ${result.message}")
                val title = getString(Res.string.upload_failed)
                val message = getString(Res.string.push_rejected)
                _state.update {
                    it.copy(mergeConflict = DialogMessage(title, message))
                }
            }
            else -> reportUploadFailed()
        }
    }

    private fun forceRegisterSSHKeys() {
        launchWithProgress { handle ->
            registerSSHKeys(true, handle)
        }
    }

    private suspend fun registerSSHKeys(force: Boolean, handle: TaskHandle) {
        handle.update(
            value = -1f,
            message = getString(Res.string.registering_keys)
        )
        val registered = withContext(Dispatchers.IO) {
            registerSSHKeys.execute(force) { progress, message ->
                handle.update(progress, message)
            }
        }
        if (registered) {
            Logger.i(TAG, "SSH keys were registered with the server")
            pullTargetTranslation(MergeStrategy.RECURSIVE, handle)
        } else {
            _event.trySend(ExportComponent.Event.AuthRequested)
        }
    }

    private suspend fun createRepository(handle: TaskHandle) {
        handle.update(
            value = -1f,
            message = getString(Res.string.creating_repository)
        )
        val created = createRepository.execute(targetTranslation) { progress, message ->
            handle.update(progress, message)
        }
        if (created) {
            Logger.i(
                TAG,
                "A new repository " + targetTranslation.id + " was created on the server"
            )
            pullTargetTranslation(MergeStrategy.RECURSIVE, handle)
        } else {
            reportUploadFailed()
        }
    }

    private fun validateUriExtension(file: PlatformFile, extension: String): Boolean {
        val filename = file.displayName
        val filenameRegex = Regex(".*\\.$extension(\\s\\(\\d+\\))?$")
        return filename.matches(filenameRegex)
    }

    private suspend fun reportExportFailed() {
        val title = getString(Res.string.export_failed)
        val exportFailed = getString(Res.string.export_failed)
        _state.update { it.copy(info = DialogMessage(title, exportFailed)) }
    }

    private suspend fun reportUploadFailed() {
        val title = getString(Res.string.export_failed)
        val noInternet = getString(Res.string.internet_not_available)
        val exportFailed = getString(Res.string.export_failed)

        if (!platform.isNetworkAvailable) {
            _state.update { it.copy(info = DialogMessage(title, noInternet)) }
        } else {
            _state.update { it.copy(uploadError = DialogMessage(title, exportFailed)) }
        }
    }

    private suspend fun reportUploadSuccess(details: String?) {
        val apiURL = preference.getPref(
            Preference.KEY_PREF_READER_SERVER,
            getString(Res.string.pref_default_reader_server)
        )
        val url = apiURL + "/" + profile.gogsUser?.username + "/" + targetTranslation.id
        val success = UploadSuccess(
            url = url,
            details = details
        )
        _state.update { it.copy(uploadSuccess = success) }
    }

    private fun getProject(): Project? {
        return catalogClient.library.getProject(
            platform.deviceLanguageCode,
            targetTranslation.projectId,
            true
        )
    }
}