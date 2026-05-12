package org.bibletranslationtools.writer.core

import org.bibletranslationtools.logger.Logger
import org.bibletranslationtools.writer.utils.FileUtilities
import java.io.File


/**
 * Handles the importing of tstudio archives.
 * The importing is placed here to keep the Translator clean and organized.
 */
class ArchiveImporter(
    private val migrator: TargetTranslationMigrator,
    private val archiveMigrator: ArchiveMigrator
) {
    companion object {
        const val TAG = "ArchiveImporter"
    }

    /**
     * Prepares an archive for import with backwards compatible support.
     * @param expandedArchiveDir
     * @return an array of target translation directories that are ready and valid for import
     * @throws Exception
     */
    @Throws(Exception::class)
    suspend fun importArchive(expandedArchiveDir: File): List<File> {
        val validTargetTranslations = arrayListOf<File>()

        // retrieve target translations from archive
        val manifestFile = File(expandedArchiveDir, "manifest.json")
        val targetTranslationDirs = if (manifestFile.exists()) {
            val rawManifest = FileUtilities.readFileToString(manifestFile)
            archiveMigrator.migrateManifest(rawManifest)?.let { archiveManifest ->
                val manifest = ArchiveMigrator.json.decodeFromString<ArchiveManifest>(archiveManifest)
                manifest.targetTranslations.map {
                    File(expandedArchiveDir, it.path)
                }
            } ?: run {
                Logger.e(TAG, "Invalid manifest file $manifestFile")
                emptyList()
            }
        } else {
            throw IllegalArgumentException("Invalid or legacy projects are not supported")
        }

        // migrate target translations
        for (dir in targetTranslationDirs) {
            val migratedDir = migrator.migrate(dir)
            if (migratedDir != null) {
                validTargetTranslations.add(migratedDir)
            }
        }
        return validTargetTranslations
    }
}
