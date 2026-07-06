package org.bibletranslationtools.writer.usecases

import btt_writer.shared.generated.resources.Res
import btt_writer.shared.generated.resources.pref_default_language_url
import org.bibletranslationtools.logger.Logger
import org.bibletranslationtools.resourcecatalog.ResourceCatalogClient
import org.bibletranslationtools.resourcecatalog.library.models.Catalog
import org.bibletranslationtools.resourcecatalog.library.models.CatalogType
import org.bibletranslationtools.writer.data.Preference
import org.bibletranslationtools.writer.data.getPref
import org.jetbrains.compose.resources.getString

class UpdateCatalogs(
    private val catalogClient: ResourceCatalogClient,
    private val preference: Preference
) {
    data class Result(val success: Boolean, val addedCount: Int)

    companion object {
        private const val TAG = "UpdateCatalogs"
    }

    suspend fun execute(
        force: Boolean,
        onProgress: (Float, String?) -> Unit = {_,_->}
    ): Result {
        var addedCount = 0
        var success = false

        var targetLanguages = catalogClient.library.getTargetLanguages()
        val initialLanguages = HashSet<String>()
        for (l in targetLanguages) {
            initialLanguages.add(l.slug)
        }

        Logger.i(TAG, "Initial target languages count: " + targetLanguages.size)
        Logger.i(
            TAG,
            "Unique target languages slug count: " + initialLanguages.size
        )

        try {
            val catalogs = if (force) {
                val languageUrl = preference.getPref(
                    Preference.KEY_PREF_LANGUAGES_URL,
                    getString(Res.string.pref_default_language_url)
                )
                val tempLanguagesUrl = "https://td.unfoldingword.org/api/templanguages/"
                val approvedLanguagesUrl = "https://td.unfoldingword.org/api/templanguages/assignment/changed/"

                buildList {
                    add(Catalog(CatalogType.TARGET_LANGUAGES, languageUrl, 0))
                    add(Catalog(CatalogType.TEMP_LANGUAGES, tempLanguagesUrl, 0))
                    add(Catalog(CatalogType.APPROVED_LANGUAGES, approvedLanguagesUrl, 0))
                }
            } else emptyList()
            catalogClient.updateCatalogs(catalogs) { value, message ->
                onProgress(value, message)
            }
            success = true
        } catch (e: Exception) {
            Logger.w(TAG, "Failed to update catalogs", e)
        }

        if (success) {
            targetLanguages = catalogClient.library.getTargetLanguages()
            Logger.i(
                TAG,
                "Final target languages count: " + targetLanguages.size
            )
            for (l in targetLanguages) {
                if (!initialLanguages.contains(l.slug)) {
                    addedCount++
                    Logger.i(
                        TAG,
                        "New target languages " + addedCount + ": " + l.slug
                    )
                }
            }
        }

        return Result(success, addedCount)
    }
}