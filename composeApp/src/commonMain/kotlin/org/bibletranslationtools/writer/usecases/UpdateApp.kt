package org.bibletranslationtools.writer.usecases

import org.bibletranslationtools.logger.Logger
import org.bibletranslationtools.resourcecatalog.ResourceCatalogClient
import org.bibletranslationtools.resourcecontainer.ResourceContainer
import org.bibletranslationtools.writer.DirectoryProvider
import org.bibletranslationtools.writer.Platform
import org.bibletranslationtools.writer.core.TargetTranslationMigrator
import org.bibletranslationtools.writer.core.Translator
import org.bibletranslationtools.writer.data.Preference
import org.bibletranslationtools.writer.data.getPref
import org.bibletranslationtools.writer.data.setPref
import org.bibletranslationtools.writer.utils.FileUtilities
import java.io.File

class UpdateApp(
    private val preference: Preference,
    private val directoryProvider: DirectoryProvider,
    private val catalogClient: ResourceCatalogClient,
    private val backupRC: BackupRC,
    private val translator: Translator,
    private val migrator: TargetTranslationMigrator,
    private val platform: Platform
) {
    private var updateLibrary = true

    companion object {
        val TAG = UpdateApp::javaClass.name
    }

    suspend fun execute(onProgress: (Float, String?) -> Unit = {_,_->}) {
        var lastVersionCode = preference.getPref(
            "last_version_code",
            0
        )
        val newInstall = lastVersionCode == 0

        // use current version if fresh install
        lastVersionCode = if (lastVersionCode == 0) platform.info.versionCode else lastVersionCode

        // record latest version
        preference.setPref(Preference.LAST_VERSION_CODE, platform.info.versionCode)

        // check if update is possible
        if (platform.info.versionCode > lastVersionCode) {
            performUpdates(lastVersionCode, onProgress)
        } else {
            // update if not deployed or if a fresh install
            updateLibrary = !catalogClient.isLibraryDeployed || newInstall
        }

        if (updateLibrary) {
            // preserve manually imported source translations
            val translations = catalogClient.library.getImportedTranslations()
            val backupFiles = arrayListOf<File>()
            if (translations.isNotEmpty()) {
                Logger.i(TAG, "Backing up imported RCs")
            }
            for (t in translations) {
                try {
                    backupFiles.add(backupRC.backupResourceContainer(t))
                } catch (_: Exception) {
                    Logger.e(TAG, "Failed exporting rc " + t.resourceContainerSlug)
                }
            }

            try {
                catalogClient.closeLibrary()
                directoryProvider.deleteLibrary()
            } catch (e: Exception) {
                Logger.w(TAG, "Failed to delete library: ${e.message}")
            }

            try {
                directoryProvider.deployDefaultLibrary()

                // restore backups
                if (backupFiles.isNotEmpty()) Logger.i(TAG, "Restoring backed up RCs")
                for (f in backupFiles) {
                    // TRICKY: the backup generates closed RCs but the import requires RCs to be opened.
                    val opened = File("$f.tmp")
                    try {
                        ResourceContainer.open(f, opened)
                        catalogClient.importResourceContainer(opened)
                    } catch (_: Exception) {
                        Logger.e(TAG, "Failed to restore RC from $f")
                    }
                    FileUtilities.deleteQuietly(opened)
                }
            } catch (e: java.lang.Exception) {
                Logger.w(TAG, "Failed to restore backups", e)
            }

            catalogClient.openLibrary()
        }

        migrateTargetTranslations()
        updateBuildNumbers()
    }

    /**
     * Performs required updates between the two app versions
     * @param lastVersion
     * @param onProgress
     */
    private fun performUpdates(
        lastVersion: Int,
        onProgress: (Float, String?) -> Unit = {_,_->}
    ) {
        // perform migrations
        // Nothing to migrate so far
    }

    /**
     * Updates the target translations
     * NOTE: we used to do this manually, but now we run this every time so we don't have to manually
     * add a new migration path each time
     */
    private suspend fun migrateTargetTranslations() {
        // TRICKY: we manually list the target translations because they won't be viewable until updated
        val translatorDir: File = translator.path
        val dirs = translatorDir.listFiles { pathname ->
            pathname.isDirectory && pathname.name != "cache"
        }
        if (dirs != null) {
            for (tt in dirs) {
                Logger.i(TAG, "Migrating: $tt")
                if (migrator.migrate(tt) == null) {
                    Logger.w(
                        TAG,
                        "Failed to migrate the target translation " + tt.name
                    )
                }
            }
        }

        // commit migration changes
        for (tt in translator.getTargetTranslations()) {
            try {
                tt.unlockRepo() // TRICKY: prune dangling locks
                tt.commitSync()
            } catch (e: java.lang.Exception) {
                Logger.e(
                    TAG,
                    "Failed to commit migration changes to target translation " + tt.id,
                    e
                )
            }
        }
    }

    /**
     * Updates the generator information for the target translations
     */
    private suspend fun updateBuildNumbers() {
        for (tt in translator.getTargetTranslations()) {
            try {
                tt.updateGenerator(platform.info.versionCode.toString())
            } catch (_: java.lang.Exception) {
                Logger.e(
                    TAG,
                    "Failed to update the generator in the target translation " + tt.id
                )
            }
        }
    }
}