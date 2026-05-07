package org.bibletranslationtools.writer.utils

import org.bibletranslationtools.logger.Logger
import org.bibletranslationtools.writer.core.TargetTranslation
import java.io.File

object RepoUtils {
    val TAG = RepoUtils::javaClass.name

    /**
     * Attempts to recover from a corrupt git history.
     * @param targetTranslation Target translation to recover
     * @return Result of recovery
     */
    fun recover(targetTranslation: TargetTranslation?): Boolean {
        if (targetTranslation == null) return false
        Logger.w(TAG, "Recovering repository for " + targetTranslation.id)
        try {
            val gitDir = File(targetTranslation.path, ".git")
            if (FileUtilities.deleteQuietly(gitDir)) {
                targetTranslation.commitSync(".", false)
                Logger.i(TAG, "History repaired for " + targetTranslation.id)
                return true
            }
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to recover repository: ${targetTranslation.id}", e)
        }
        return false
    }
}