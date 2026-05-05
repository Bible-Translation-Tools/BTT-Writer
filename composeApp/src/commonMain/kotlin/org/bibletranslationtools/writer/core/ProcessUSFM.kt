package org.bibletranslationtools.writer.core

import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.bibletranslationtools.resourcecatalog.ResourceCatalogClient
import org.bibletranslationtools.resourcecatalog.library.models.TargetLanguage
import org.bibletranslationtools.writer.DirectoryProvider
import org.bibletranslationtools.writer.Platform
import java.io.File

/**
 * Factory for USFM import sessions.
 *
 * This service is stateless — each call to [startImport] returns a fresh
 * [ImportUsfmSession] that holds the per-import state (parsed books, missing
 * names, temp files) and exposes the operations needed to drive the import
 * flow: inspecting parse results, resolving missing book names interactively,
 * and reading the final list of importable project folders.
 *
 * Callers are responsible for invoking [ImportUsfmSession.cleanup] when the
 * session is no longer needed, to remove temporary files.
 *
 * Inject as a singleton via Koin.
 */
class ProcessUSFM(
    private val platform: Platform,
    private val directoryProvider: DirectoryProvider,
    private val profile: Profile,
    private val catalogClient: ResourceCatalogClient
) {
    /**
     * Starts a new import session. The returned [ImportUsfmSession] holds state
     * across multiple interactions (e.g. resolving missing book names) and
     * must be closed via [ImportUsfmSession.cleanup] when done.
     */
    suspend fun startImport(
        targetLanguage: TargetLanguage,
        file: PlatformFile,
        onProgress: (Float, String?) -> Unit = { _, _ -> }
    ): ImportUsfmSession = withContext(Dispatchers.IO) {
        val session = ImportUsfmSession(
            platform = platform,
            directoryProvider = directoryProvider,
            profile = profile,
            catalogClient = catalogClient,
            targetLanguage = targetLanguage,
            onProgress = onProgress
        )
        session.run(file)
        session
    }
}

/**
 * Result of a single USFM import operation.
 */
data class ImportResult(
    val success: Boolean,
    val importedProjects: List<File>,
    val booksMissingNames: List<MissingNameItem>,
    val summary: String
) {
    companion object {
        val EMPTY = ImportResult(
            false,
            emptyList(),
            emptyList(),
            ""
        )
    }
}