package org.bibletranslationtools.writer.usecases

import btt_writer.composeapp.generated.resources.Res
import btt_writer.composeapp.generated.resources.copying_file
import btt_writer.composeapp.generated.resources.migrating_translation
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.isDirectory
import org.bibletranslationtools.writer.DirectoryProvider
import org.bibletranslationtools.writer.core.TargetTranslationMigrator
import org.bibletranslationtools.writer.displayName
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
        FileUtilities.copyDirectory(
            appDataFolder,
            PlatformFile(tempTranslations)
        ) {
            it.isDirectory() && it.displayName == directoryProvider.translationsDir.name
        }

        migrateTranslations(tempTranslations, onProgress)
        importTranslations(tempTranslations, onProgress)

        // Migrate backups
        val tempBackups = directoryProvider.createTempDir("backups")
        FileUtilities.copyDirectory(
            appDataFolder,
            PlatformFile(tempBackups)
        ) {
            it.isDirectory() && it.displayName == directoryProvider.backupsDir.name
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