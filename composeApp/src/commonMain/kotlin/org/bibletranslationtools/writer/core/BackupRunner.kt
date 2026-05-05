package org.bibletranslationtools.writer.core

import kotlinx.coroutines.delay
import org.bibletranslationtools.logger.Logger
import org.bibletranslationtools.writer.usecases.BackupRC
import org.bibletranslationtools.writer.utils.RepoUtils
import org.eclipse.jgit.api.errors.JGitInternalException

interface BackupScheduler {
    fun start(intervalMinutes: Int)
    fun stop()
    fun restart(intervalMinutes: Int) {
        stop()
        start(intervalMinutes)
    }
    val isRunning: Boolean
}

interface BackupNotifier {
    fun onBackupComplete()
}

class BackupRunner(
    private val translator: Translator,
    private val backupRC: BackupRC,
) {
    private var executingBackup = false

    suspend fun runBackup(): Boolean {
        if (executingBackup) return false

        executingBackup = true
        var backupPerformed = false

        Logger.i(TAG, "Checking for changes")

        val targetTranslations = translator.targetTranslationFileNames

        for (filename in targetTranslations) {
            delay(1000)

            val t = translator.getTargetTranslation(filename)
            if (t == null) {
                Logger.i(TAG, "Skipping invalid translation: $filename")
                continue
            }

            // commit pending changes
            try {
                t.commitSync(".", false)
            } catch (e: Exception) {
                if (e is JGitInternalException) {
                    Logger.w(TAG, "History corrupt in ${t.id}. Repairing...", e)
                    RepoUtils.recover(t)
                } else {
                    Logger.w(TAG, "Could not commit changes to ${t.id}", e)
                }
            }

            // run backup if there are translations
            if (t.numTranslated > 0) {
                try {
                    val success = backupRC.backupTargetTranslation(t, false)
                    if (success) {
                        Logger.i(TAG, "${t.id} backed up")
                        backupPerformed = true
                    }
                } catch (e: Exception) {
                    Logger.e(TAG, "Could not backup ${t.id}", e)
                }
            }
        }

        Logger.i(TAG, "Finished backup check.")
        executingBackup = false
        return backupPerformed
    }

    companion object {
        private const val TAG = "BackupRunner"
    }
}