package org.bibletranslationtools.writer.ui.dialogs.import

import btt_writer.composeapp.generated.resources.Res
import btt_writer.composeapp.generated.resources.cloning_repository
import btt_writer.composeapp.generated.resources.could_not_import
import btt_writer.composeapp.generated.resources.error
import btt_writer.composeapp.generated.resources.import_failed
import btt_writer.composeapp.generated.resources.import_from_door43
import btt_writer.composeapp.generated.resources.import_from_storage
import btt_writer.composeapp.generated.resources.import_source_text
import btt_writer.composeapp.generated.resources.import_success
import btt_writer.composeapp.generated.resources.importing_file
import btt_writer.composeapp.generated.resources.invalid_file
import btt_writer.composeapp.generated.resources.label_translation_notes
import btt_writer.composeapp.generated.resources.registering_keys
import btt_writer.composeapp.generated.resources.restore_failed
import btt_writer.composeapp.generated.resources.searching_repositories
import btt_writer.composeapp.generated.resources.success
import btt_writer.composeapp.generated.resources.title_import_success
import btt_writer.composeapp.generated.resources.translation_questions
import btt_writer.composeapp.generated.resources.translation_words
import btt_writer.composeapp.generated.resources.unsupported
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.doOnDestroy
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.name
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
import org.bibletranslationtools.gogsclient.Repository
import org.bibletranslationtools.logger.Logger
import org.bibletranslationtools.resourcecatalog.ResourceCatalogClient
import org.bibletranslationtools.writer.DirectoryProvider
import org.bibletranslationtools.writer.Platform
import org.bibletranslationtools.writer.core.ComponentScope
import org.bibletranslationtools.writer.core.Progress
import org.bibletranslationtools.writer.core.ProgressManager
import org.bibletranslationtools.writer.core.ProgressOwner
import org.bibletranslationtools.writer.core.TargetTranslation
import org.bibletranslationtools.writer.core.TargetTranslationMigrator
import org.bibletranslationtools.writer.core.TaskHandle
import org.bibletranslationtools.writer.core.Translator
import org.bibletranslationtools.writer.core.launchWithProgress
import org.bibletranslationtools.writer.ui.home.RepositoryItem
import org.bibletranslationtools.writer.usecases.AdvancedGogsRepoSearch
import org.bibletranslationtools.writer.usecases.CloneRepository
import org.bibletranslationtools.writer.usecases.ImportProjects
import org.bibletranslationtools.writer.usecases.RegisterSSHKeys
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File
import java.io.IOException
import java.security.InvalidParameterException

data class MergeConflict(
    val translation: TargetTranslation,
    val hasMergeConflict: Boolean,
    val isFromServer: Boolean,
    val onResolve: () -> Unit,
    val onOverwrite: () -> Unit,
    val onCancel: () -> Unit
)

interface ImportComponent {
    val state: StateFlow<State>
    val progress: StateFlow<Progress?>
    val event: Flow<Event>

    fun importUsfm(file: PlatformFile)
    fun importProject(file: PlatformFile, overwrite: Boolean)
    fun importSource(file: PlatformFile, overwrite: Boolean)
    fun importBackup(backup: File)
    fun importRepo(repo: RepositoryItem, accepted: Boolean, overwrite: Boolean)
    fun searchRepositories(user: String, repo: String)
    fun registerKeys()
    fun clearResult()
    fun clearMergeConflict()
    fun clearSourceConflict()
    fun clearImportRepo()

    data class State(
        val mergeConflict: MergeConflict? = null,
        val sourceConflict: ImportProjects.ImportSourceResult? = null,
        val resultMessage: Pair<String, String>? = null,
        val backups: List<File> = emptyList(),
        val repositories: List<RepositoryItem> = emptyList(),
        val repoToImport: RepositoryItem? = null
    )

    sealed interface Event {
        data object AuthRequested : Event
    }

    sealed interface Result {
        data class MergeConflict(val translationId: String) : Result
        data class ProjectsImported(val translationIds: List<String>) : Result
        data class OpenUsfmImport(val file: PlatformFile) : Result
    }
}

class DefaultImportComponent(
    componentContext: ComponentContext,
    projectFile: PlatformFile?,
    private val onResult: (ImportComponent.Result) -> Unit
) : ImportComponent,
    ComponentContext by componentContext,
    ComponentScope, ProgressOwner, KoinComponent {

    private val translator: Translator by inject()
    private val advancedGogsRepoSearch: AdvancedGogsRepoSearch by inject()
    private val cloneRepository: CloneRepository by inject()
    private val registerSSHKeys: RegisterSSHKeys by inject()
    private val importProjects: ImportProjects by inject()
    private val catalogClient: ResourceCatalogClient by inject()
    private val directoryProvider: DirectoryProvider by inject()
    private val targetTranslationMigrator: TargetTranslationMigrator by inject()
    private val platform: Platform by inject()

    override val coroutineScope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())

    private val progressManager = ProgressManager(coroutineScope)
    override val progress get() = progressManager.progress

    private val _state = MutableStateFlow(ImportComponent.State())
    override val state = _state.asStateFlow()

    private val _event = Channel<ImportComponent.Event>(Channel.BUFFERED)
    override val event = _event.receiveAsFlow()

    init {
        coroutineScope.launch {
            val backups = getBackupTranslations().sorted()
            _state.update { it.copy(backups = backups) }

            projectFile?.let { uri ->
                importProject(uri, false)
            }
        }

        lifecycle.doOnDestroy {
            coroutineScope.cancel()
        }
    }

    override suspend fun runTask(message: String?, block: suspend (TaskHandle) -> Unit) {
        progressManager.runTask(message, block)
    }

    override fun importUsfm(file: PlatformFile) {
        onResult(ImportComponent.Result.OpenUsfmImport(file))
    }

    override fun importProject(file: PlatformFile, overwrite: Boolean) {
        importProject(file, overwrite, Res.string.import_source_text)
    }

    override fun importSource(file: PlatformFile, overwrite: Boolean) {
        launchWithProgress(Res.string.import_source_text) {
            val result = withContext(Dispatchers.IO) {
                importProjects.importSource(file, overwrite)
            }

            when {
                result.success -> {
                    val dirName = file.name
                    updateResult(
                        getString(Res.string.success),
                        getString(Res.string.import_success) + " $dirName"
                    )
                }
                result.hasConflict -> {
                    _state.update { it.copy(sourceConflict = result) }
                }
                else -> {
                    updateResult(
                        getString(Res.string.could_not_import),
                        result.error ?: "Unknown error"
                    )
                }
            }
        }
    }

    override fun importBackup(backup: File) {
        val uri = PlatformFile(backup)
        importProject(uri, false, Res.string.importing_file, backup.name)
    }

    override fun importRepo(repo: RepositoryItem, accepted: Boolean, overwrite: Boolean) {
        launchWithProgress(Res.string.cloning_repository) { handle ->
            if (repo.isSupported || accepted) {
                cloneRepository(repo, overwrite, handle)
            } else {
                _state.update { it.copy(repoToImport = repo) }
            }
        }
    }

    override fun searchRepositories(user: String, repo: String) {
        launchWithProgress(Res.string.searching_repositories) { handle ->
            val result = withContext(Dispatchers.IO) {
                advancedGogsRepoSearch.execute(user, repo, 50) { progress, message ->
                    handle.update(progress, message)
                }.map { mapRepository(it) }
            }
            _state.update { it.copy(repositories = result) }
        }
    }

    override fun registerKeys() {
        forceRegisterSSHKeys()
    }

    override fun clearResult() {
        _state.update { it.copy(resultMessage = null, repositories = emptyList()) }
    }

    override fun clearMergeConflict() {
        _state.update { it.copy(mergeConflict = null) }
    }

    override fun clearSourceConflict() {
        _state.update { it.copy(sourceConflict = null) }
    }

    override fun clearImportRepo() {
        _state.update { it.copy(repoToImport = null) }
    }

    private fun importProject(
        file: PlatformFile,
        overwrite: Boolean,
        message: StringResource,
        vararg formatArgs: Any
    ) {
        launchWithProgress { handle ->
            handle.update(-1f, getString(message, formatArgs))

            val filename = file.name
            val isTstudio = filename.contains(Translator.TSTUDIO_EXTENSION, ignoreCase = true)
            val isZip = filename.contains(Translator.ZIP_EXTENSION, ignoreCase = true)
            if (isTstudio || isZip) {
                if (overwrite) resetToMaster()

                _state.update { it.copy(mergeConflict = null) }

                val result = withContext(Dispatchers.IO) {
                    importProjects.importProject(file, overwrite) { progress, message ->
                        handle.update(progress, message)
                    }
                }
                when {
                    result.success && result.alreadyExists && !overwrite -> {
                        _state.update {
                            it.copy(mergeConflict = MergeConflict(
                                translation = getTargetTranslation(
                                    result.importedSlug!!
                                ),
                                hasMergeConflict = result.hasMergeConflict,
                                isFromServer = false,
                                onResolve = {
                                    coroutineScope.launch {
                                        resolveMergeConflict()
                                    }
                                },
                                onOverwrite = {
                                    launchWithProgress {
                                        importProject(result.filePath, true)
                                    }
                                },
                                onCancel = {
                                    coroutineScope.launch {
                                        resetToMaster()
                                    }
                                }
                            ))
                        }
                    }
                    result.success -> {
                        result.importedSlug?.let {
                            onResult(ImportComponent.Result.ProjectsImported(listOf(it)))
                        }
                        updateResult(
                            getString(Res.string.import_from_storage),
                            getString(Res.string.import_success) +
                                    "\n${result.readablePath}"
                        )
                    }
                    result.invalidFileName -> {
                        updateResult(
                            getString(Res.string.import_from_storage),
                            getString(Res.string.invalid_file) +
                                    "\n${result.readablePath}"
                        )
                    }
                    else -> {
                        updateResult(
                            getString(Res.string.import_from_storage),
                            getString(Res.string.import_failed) +
                                    "\n${result.readablePath}"
                        )
                    }
                }
            } else {
                updateResult(
                    getString(Res.string.import_from_storage),
                    "${getString(Res.string.invalid_file)}\n$filename"
                )
            }
        }
    }

    private suspend fun mapRepository(repository: Repository): RepositoryItem {
        val repoName = repository.fullName.split("/".toRegex())
        var projectName = ""
        var languageName = ""
        var code = "en"
        var direction = "ltr"
        var unsupportedTag = ""
        var targetTranslationSlug = ""

        if (repoName.isNotEmpty()) {
            targetTranslationSlug = repoName[repoName.size - 1]
            try {
                val projectSlug = TargetTranslation.getProjectSlugFromId(targetTranslationSlug)
                val targetLanguageSlug = TargetTranslation.getTargetLanguageSlugFromId(
                    targetTranslationSlug
                )
                val resourceTypeSlug = TargetTranslation.getResourceTypeFromId(
                    targetTranslationSlug
                )
                if (resourceTypeSlug != "text") {
                    unsupportedTag = when (resourceTypeSlug) {
                        "tw" -> getString(Res.string.translation_words)
                        "tn" -> getString(Res.string.label_translation_notes)
                        "tq" -> getString(Res.string.translation_questions)
                        else -> getString(Res.string.unsupported)
                    }
                }

                val project = catalogClient.library.getProject(
                    languageSlug = platform.deviceLanguageCode,
                    projectSlug = projectSlug,
                    enableDefaultLanguage = true
                )
                projectName = project?.name ?: targetTranslationSlug
                val targetLanguage = catalogClient.library.getTargetLanguage(targetLanguageSlug)
                if (targetLanguage != null) {
                    languageName = targetLanguage.name
                    direction = targetLanguage.direction
                    code = targetLanguage.slug
                } else {
                    languageName = targetLanguageSlug
                }
            } catch (e: StringIndexOutOfBoundsException) {
                e.printStackTrace()
                projectName = targetTranslationSlug
                unsupportedTag = getString(Res.string.unsupported)
            }
        }

        return RepositoryItem(
            languageName,
            projectName,
            targetTranslationSlug,
            code,
            direction,
            repository.fullName,
            repository.htmlUrl,
            repository.isPrivate,
            unsupportedTag
        )
    }

    private suspend fun cloneRepository(
        repo: RepositoryItem,
        overwrite: Boolean,
        handle: TaskHandle
    ) {
        if (overwrite) resetToMaster()

        _state.update { it.copy(mergeConflict = null, repoToImport = null) }

        val result = withContext(Dispatchers.IO) {
            cloneRepository.execute(repo.url) { progress, message ->
                handle.update(progress, message)
            }
        }

        when (result.status) {
            CloneRepository.Status.SUCCESS -> {
                result.cloneDir?.let {
                    val clonedDir = targetTranslationMigrator.migrate(it)

                    Logger.i(this.javaClass.name, "Repository cloned from $it")

                    clonedDir?.let { tempRepoDir ->
                        TargetTranslation.open(tempRepoDir)?.let { tempTargetTranslation ->
                            val existingTargetTranslation = translator.getTargetTranslation(
                                tempTargetTranslation.id
                            )

                            if (existingTargetTranslation != null && !overwrite) {
                                try {
                                    val mergedWithoutConflicts = existingTargetTranslation.merge(
                                        tempRepoDir,
                                        null
                                    )
                                    _state.update { state ->
                                        state.copy(
                                            mergeConflict = MergeConflict(
                                                translation = existingTargetTranslation,
                                                hasMergeConflict = !mergedWithoutConflicts,
                                                isFromServer = true,
                                                onResolve = {
                                                    coroutineScope.launch {
                                                        resolveMergeConflict()
                                                    }
                                                },
                                                onOverwrite = {
                                                    launchWithProgress { handle ->
                                                        cloneRepository(
                                                            repo = repo,
                                                            overwrite = true,
                                                            handle = handle
                                                        )
                                                    }
                                                },
                                                onCancel = {
                                                    coroutineScope.launch {
                                                        resetToMaster()
                                                    }
                                                }
                                            )
                                        )
                                    }
                                } catch (e: Exception) {
                                    Logger.e(
                                        this.javaClass.name,
                                        "Failed to merge translation",
                                        e
                                    )
                                    reportImportFailed("Failed to merge translation")
                                }
                            } else {
                                try {
                                    translator.restoreTargetTranslation(tempTargetTranslation)
                                    onResult(
                                        ImportComponent.Result.ProjectsImported(
                                            listOf(tempTargetTranslation.id)
                                        )
                                    )
                                    updateResult(
                                        title = getString(Res.string.import_from_door43),
                                        message = getString(Res.string.title_import_success)
                                    )
                                } catch (e: IOException) {
                                    Logger.e(
                                        this.javaClass.name,
                                        "Failed to overite translation",
                                        e
                                    )
                                    reportImportFailed("Failed to overite translation")
                                }
                            }
                        } ?: run {
                            Logger.e(this.javaClass.name, "Failed to open the online backup")
                            reportImportFailed("Failed to open the online backup")
                        }
                    } ?: run {
                        Logger.e(this.javaClass.name, "Failed to migrate project")
                        reportImportFailed("Failed to migrate project")
                    }
                }
            }
            CloneRepository.Status.AUTH_FAILURE -> {
                Logger.i(this.javaClass.name, "Authentication failed")
                if (!directoryProvider.hasSSHKeys()) {
                    registerSSHKeys(false, handle) {
                        cloneRepository(repo, overwrite, handle)
                    }
                } else {
                    _state.update { it.copy(repoToImport = repo) }
                    _event.trySend(ImportComponent.Event.AuthRequested)
                }
            }
            else -> {
                updateResult(
                    title = getString(Res.string.error),
                    message = getString(Res.string.restore_failed)
                )
            }
        }
    }

    private suspend fun resetToMaster() {
        val mergeConflict = _state.value.mergeConflict ?: return
        withContext(Dispatchers.IO) {
            mergeConflict.translation.resetToMasterBackup()
        }
    }

    private suspend fun resolveMergeConflict() {
        val result = _state.value.mergeConflict ?: return
        if (result.hasMergeConflict) {
            onResult(ImportComponent.Result.MergeConflict(result.translation.id))
        } else {
            val title = if (result.isFromServer) {
                getString(Res.string.import_from_door43)
            } else getString(Res.string.import_from_storage)
            updateResult(
                title,
                getString(Res.string.import_success) +
                        "\n${result.translation.id}"
            )
        }
    }

    private fun updateResult(title: String, message: String) {
        _state.update { it.copy(resultMessage = title to message) }
    }

    private suspend fun getBackupTranslations(): List<File> {
        return withContext(Dispatchers.IO) {
            directoryProvider
                .backupsDir
                .listFiles()
                ?.asList()
                ?.filter { it.length() > 0 }
                ?: listOf()
        }
    }

    private fun forceRegisterSSHKeys() {
        launchWithProgress { handle ->
            registerSSHKeys(true, handle) {
                _state.value.repoToImport?.let {
                    cloneRepository(it, false, handle)
                }
            }
        }
    }

    private suspend fun registerSSHKeys(
        force: Boolean,
        handle: TaskHandle,
        onSuccess: suspend () -> Unit
    ) {
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
            Logger.i(this.javaClass.name, "SSH keys were registered with the server")
            onSuccess()
        } else {
            _event.trySend(ImportComponent.Event.AuthRequested)
        }
    }

    private suspend fun reportImportFailed(details: String) {
        updateResult(
            title = getString(Res.string.error),
            message = getString(Res.string.restore_failed, details)
        )
    }

    private suspend fun getTargetTranslation(translationID: String): TargetTranslation {
        return translator.getTargetTranslation(translationID)
            ?: throw InvalidParameterException("The target translation '$translationID' is invalid")
    }
}
