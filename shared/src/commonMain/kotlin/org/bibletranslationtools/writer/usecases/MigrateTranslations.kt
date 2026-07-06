package org.bibletranslationtools.writer.usecases

import btt_writer.shared.generated.resources.Res
import btt_writer.shared.generated.resources.copying_file
import btt_writer.shared.generated.resources.migrating_translation
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.div
import io.github.vinceglb.filekit.exists
import org.bibletranslationtools.writer.DirectoryProvider
import org.bibletranslationtools.writer.core.TargetTranslationMigrator
import org.bibletranslationtools.writer.utils.FileUtilities
import org.jetbrains.compose.resources.getString
import java.io.File

class MigrateTranslations(
    private val importProjects: ImportProjects,
    private val directoryProvider: DirectoryProvider,
    private val targetTranslationMigrator: TargetTranslationMigrator
) {
    suspend fun execute(
        appDataFolder: PlatformFile,
        onProgress: (Float, String?) -> Unit = {_,_->}
    ) {
        // Migrate translations

        val tempTranslations = directoryProvider.createTempDir("translations")
        val kmpTranslationsSrc = appDataFolder / directoryProvider.translationsDir.name
        val desktopTranslationsDir = appDataFolder / "targetTranslations"

        val translationsSrc = when {
            kmpTranslationsSrc.exists() -> kmpTranslationsSrc
            desktopTranslationsDir.exists() -> desktopTranslationsDir
            else -> null
        }

        translationsSrc?.let {
            FileUtilities.copyDirectory(it, PlatformFile(tempTranslations))
        }

        migrateTranslations(tempTranslations, onProgress)
        importTranslations(tempTranslations, onProgress)

        // Migrate backups
        val tempBackups = directoryProvider.createTempDir("backups")
        val kmpBackupsSrc = appDataFolder / directoryProvider.backupsDir.name
        val autoBackupsSrc = appDataFolder / "automatic_backups"
        val backupsSrc = mutableListOf<PlatformFile>()

        if (kmpBackupsSrc.exists()) backupsSrc.add(kmpBackupsSrc)
        if (autoBackupsSrc.exists()) backupsSrc.add(autoBackupsSrc)

        backupsSrc.forEach { src ->
            FileUtilities.copyDirectory(src, PlatformFile(tempBackups))
        }

        copyBackups(tempBackups, onProgress)
    }

    private suspend fun migrateTranslations(
        translationsDir: File,
        onProgress: (Float, String?) -> Unit = {_,_->}
    ) {
        if (translationsDir.isDirectory) {
            translationsDir.listFiles()?.forEach { file ->
                if (file.name == "cache") return@forEach
                if (file.isDirectory) {
                    onProgress(
                        -1f,
                        getString(Res.string.migrating_translation, file.name)
                    )
                    targetTranslationMigrator.migrate(file)
                }
            }
        }
    }

    private suspend fun importTranslations(
        translationsDir: File,
        onProgress: (Float, String?) -> Unit = {_,_->}
    ) {
        if (translationsDir.isDirectory) {
            val translations = arrayListOf<File>()
            translationsDir.listFiles()?.forEach { file ->
                if (file.name == "cache") return@forEach
                if (file.isDirectory) {
                    translations.add(file)
                }
            }
            importProjects.importProjects(translations, false, onProgress)
            FileUtilities.deleteQuietly(translationsDir)
        }
    }

    private suspend fun copyBackups(
        backupsDir: File,
        onProgress: (Float, String?) -> Unit = {_,_->}
    ) {
        if (backupsDir.isDirectory) {
            backupsDir.listFiles()?.forEach { file ->
                if (file.isFile) {
                    val destFile = File(directoryProvider.backupsDir, file.name)
                    FileUtilities.copyFile(file, destFile)
                    onProgress(
                        -1f,
                        getString(Res.string.copying_file, destFile.name)
                    )
                }
            }
            FileUtilities.deleteQuietly(backupsDir)
        }
    }
}