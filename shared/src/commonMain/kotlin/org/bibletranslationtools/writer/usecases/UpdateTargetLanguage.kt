package org.bibletranslationtools.writer.usecases

import org.bibletranslationtools.logger.Logger
import org.bibletranslationtools.resourcecatalog.ResourceCatalogClient
import org.bibletranslationtools.resourcecatalog.library.models.TargetLanguage
import org.bibletranslationtools.writer.core.Translator

/**
 * Updates a target language in the library and propagates the change
 * to all existing target translations that use this language.
 */
class UpdateTargetLanguage(
    private val catalogClient: ResourceCatalogClient,
    private val translator: Translator
) {
    companion object {
        private const val TAG = "UpdateTargetLanguage"
    }

    suspend fun execute(language: TargetLanguage): Boolean {
        val library = catalogClient.library
        val updated = if (library.isTempTargetLanguage(language.slug)) {
            library.addTempTargetLanguage(language)
        } else {
            library.addTargetLanguage(language)
        }
        if (!updated) {
            Logger.w(TAG, "Failed to update target language ${language.slug}")
            return false
        }

        for (translation in translator.getTargetTranslations()) {
            if (translation.targetLanguageId != language.slug) continue
            try {
                translation.changeTargetLanguage(language)
            } catch (e: Exception) {
                // keep going: one broken project must not block the others
                Logger.e(TAG, "Failed to update project ${translation.id}", e)
            }
        }
        return true
    }
}
