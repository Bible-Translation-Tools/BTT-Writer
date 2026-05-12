package org.bibletranslationtools.writer.usecases

import org.bibletranslationtools.resourcecatalog.ResourceCatalogClient
import org.bibletranslationtools.resourcecatalog.library.models.Translation
import org.bibletranslationtools.resourcecontainer.ResourceContainer
import org.bibletranslationtools.writer.DirectoryProvider
import org.bibletranslationtools.writer.core.ArchiveManifest
import org.bibletranslationtools.writer.core.ArchiveMigrator
import org.bibletranslationtools.writer.core.Profile
import org.bibletranslationtools.writer.core.TargetTranslation
import org.bibletranslationtools.writer.core.Translator
import org.bibletranslationtools.writer.utils.FileUtilities
import org.bibletranslationtools.writer.utils.Zip
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BackupRC (
    private val directoryProvider: DirectoryProvider,
    private val archiveMigrator: ArchiveMigrator,
    private val exportProjects: ExportProjects,
    private val profile: Profile,
    private val catalogClient: ResourceCatalogClient
) {
    fun backupResourceContainer(translation: Translation): File {
        val dest = File(
            directoryProvider.backupsDir,
            translation.resourceContainerSlug + "." + ResourceContainer.FILE_EXTENSION
        )
        catalogClient.exportResourceContainer(
            dest,
            translation.language.slug,
            translation.project.slug,
            translation.resource.slug
        )
        return dest
    }

    @Throws(Exception::class)
    suspend fun backupTargetTranslation(
        targetTranslation: TargetTranslation?,
        orphaned: Boolean
    ): Boolean {
        if (targetTranslation != null) {
            var name = targetTranslation.id
            val sdf = SimpleDateFormat("yyyy-MM-dd_HH.mm.ss", Locale.US)
            if (orphaned) {
                name += "." + sdf.format(Date())
            }

            var archiveExtension = Translator.TSTUDIO_EXTENSION
            if (orphaned) {
                archiveExtension = Translator.ZIP_EXTENSION
            }

            // backup locations
            val backup = File(directoryProvider.backupsDir, "$name.$archiveExtension")

            // check if we need to back up
            if (!orphaned) {
                if (backup.exists()) {
                    Zip.read(backup, ArchiveMigrator.MANIFEST_JSON)?.let { rawManifest ->
                        archiveMigrator.migrateManifest(rawManifest)?.let { migratedManifest ->
                            val manifest = ArchiveMigrator.json.decodeFromString<ArchiveManifest>(migratedManifest)
                            if (manifest.targetTranslations.firstOrNull()?.commitHash == targetTranslation.commitHash) {
                                return false
                            }
                        }
                    }
                }
            }

            // run backup
            var temp: File? = null
            try {
                temp = directoryProvider.createTempFile(name, ".$archiveExtension")
                targetTranslation.setDefaultContributor(profile.nativeSpeaker)
                exportProjects.exportProject(targetTranslation, temp)
                if (temp.exists() && temp.isFile) {
                    // copy into backup locations
                    backup.parentFile?.mkdirs()

                    FileUtilities.copyFile(temp, backup)
                    return true
                }
            } finally {
                FileUtilities.deleteQuietly(temp)
            }
        }
        return false
    }

    /**
     * Creates a backup of a project directory in all the right places
     * @param projectDir the project directory that will be backed up
     * @return true if the backup was actually performed
     */
    @Throws(Exception::class)
    suspend fun backupTargetTranslation(projectDir: File): Boolean {
        val sdf = SimpleDateFormat("yyyy-MM-dd_HH.mm.ss", Locale.US)
        val name = projectDir.name + "." + sdf.format(Date())

        // backup locations
        val backup = File(directoryProvider.backupsDir, name + "." + Translator.ZIP_EXTENSION)

        // run backup
        var temp: File? = null
        try {
            temp = directoryProvider.createTempFile(name, "." + Translator.ZIP_EXTENSION)
            exportProjects.exportProject(projectDir, temp)
            if (temp.exists() && temp.isFile) {
                // copy into backup locations
                backup.parentFile?.mkdirs()

                FileUtilities.copyFile(temp, backup)
                return true
            }
        } finally {
            FileUtilities.deleteQuietly(temp)
        }

        return false
    }
}