package org.bibletranslationtools.writer.core

import org.bibletranslationtools.logger.Logger
import org.bibletranslationtools.resourcecatalog.ResourceCatalogClient
import org.bibletranslationtools.resourcecatalog.library.models.TargetLanguage
import org.bibletranslationtools.resourcecontainer.Resource
import org.bibletranslationtools.resourcecontainer.ResourceContainer
import org.bibletranslationtools.writer.DirectoryProvider
import org.bibletranslationtools.writer.Platform
import org.bibletranslationtools.writer.data.Preference
import org.bibletranslationtools.writer.data.getPrefOrNull
import org.bibletranslationtools.writer.data.setPrefOrNull
import org.bibletranslationtools.writer.rendering.USXtoUSFMConverter
import org.bibletranslationtools.writer.usecases.BackupRC
import org.bibletranslationtools.writer.utils.FileUtilities
import java.io.File
import java.io.IOException

/**
 * Created by joel on 8/29/2015.
 */
class Translator (
    private val profile: Profile,
    private val preference: Preference,
    private val directoryProvider: DirectoryProvider,
    private val backupRC: BackupRC,
    private val catalogClient: ResourceCatalogClient,
    private val platform: Platform
) {
    /**
     * Returns the root directory to the target translations
     */
    val path
        get() = directoryProvider.translationsDir

    suspend fun getTargetTranslations(): List<TargetTranslation> {
        val dirs = path.listFiles { file ->
            file.isDirectory && !file.name.equals("cache", ignoreCase = true)
        }.orEmpty()

        return dirs.mapNotNull { getTargetTranslation(it.name) }
    }
        /**
         * Returns an array of all active translations
         * @return
         */


    /**
     * Returns an array of all active translation IDs - this does not hold in memory each manifest.
     * Requires less memory to just get a count of items.
     * @return
     */
    suspend fun getTargetTranslationIDs(): List<String> {
        return getTargetTranslations().map { it.id }
    }

    val targetTranslationFileNames: Array<String>
        /**
         * Returns an array of all translation File names - this does not verify the list.
         * @return
         */
        get() {
            val translations: MutableList<String> = ArrayList()
            path.list { dir, filename ->
                if (!filename.equals("cache", ignoreCase = true) && File(
                        dir,
                        filename
                    ).isDirectory
                ) {
                    translations.add(filename)
                }
                false
            }

            return translations.toTypedArray<String>()
        }

    /**
     * Get and set the last focused target translation
     * @return
     */
    var lastFocusTargetTranslation: String?
        get() = preference.getPrefOrNull<String>(Preference.LAST_TRANSLATION)
        set(targetTranslationId) {
            preference.setPrefOrNull(Preference.LAST_TRANSLATION, targetTranslationId)
        }

    /**
     * Creates a new Target Translation. If one already exists it will return it without changing anything.
     * @param nativeSpeaker the human translator
     * @param targetLanguage the language that is being translated into
     * @param projectSlug the project that is being translated
     * @param resourceType the type of translation that is occurring
     * @param resourceSlug the resource that is being created
     * @param translationFormat the format of the translated text
     * @return A new or existing Target Translation
     */
    suspend fun createTargetTranslation(
        nativeSpeaker: NativeSpeaker,
        targetLanguage: TargetLanguage,
        projectSlug: String,
        resourceType: ResourceType,
        resourceSlug: String,
        translationFormat: TranslationFormat
    ): TargetTranslation {
        // TRICKY: force deprecated formats to use new formats
        var format = translationFormat
        if (format == TranslationFormat.USX) {
            format = TranslationFormat.USFM
        } else if (format == TranslationFormat.DEFAULT) {
            format = TranslationFormat.MARKDOWN
        }

        val targetTranslationId = TargetTranslation.generateTargetTranslationId(
            targetLanguage.slug,
            projectSlug,
            resourceType,
            resourceSlug
        )
        val targetTranslation = getTargetTranslation(targetTranslationId)
        if (targetTranslation == null) {
            val targetTranslationDir = File(this.path, targetTranslationId)
            try {
                return TargetTranslation.create(
                    directoryProvider,
                    platform,
                    nativeSpeaker,
                    format,
                    targetLanguage,
                    projectSlug,
                    resourceType,
                    resourceSlug,
                    targetTranslationDir
                )
            } catch (e: Exception) {
                Logger.e(TAG, "Failed to create target translation", e)
            }
        }
        return targetTranslation!!
    }

    private fun setTargetTranslationAuthor(targetTranslation: TargetTranslation?) {
        if (profile.loggedIn && targetTranslation != null) {
            var name = profile.fullName
            var email = ""
            profile.gogsUser?.let {
                name = it.fullName ?: ""
                email = it.email ?: ""
            }
            targetTranslation.setAuthor(name, email)
        }
    }

    /**
     * Returns a target translation if it exists
     * @param targetTranslationId
     * @return
     */
    suspend fun getTargetTranslation(targetTranslationId: String): TargetTranslation? {
        return targetTranslationId.let {
            val targetTranslationDir = File(path, targetTranslationId)
            val targetTranslation = TargetTranslation.open(targetTranslationDir) {
                // Try to back up and delete corrupt project
                try {
                    backupRC.backupTargetTranslation(targetTranslationDir)
                    deleteTargetTranslation(targetTranslationDir)
                } catch (ex: java.lang.Exception) {
                    ex.printStackTrace()
                }
            }
            setTargetTranslationAuthor(targetTranslation)
            targetTranslation
        }
    }

    /**
     * Deletes a target translation from the device
     * @param targetTranslationId
     */
    fun deleteTargetTranslation(targetTranslationId: String?) {
        if (targetTranslationId != null) {
            val targetTranslationDir = File(path, targetTranslationId)
            FileUtilities.safeDelete(targetTranslationDir)
        }
    }

    /**
     * Deletes a target translation from the device
     * @param projectDir
     */
    fun deleteTargetTranslation(projectDir: File) {
        if (projectDir.exists()) {
            FileUtilities.safeDelete(projectDir)
        }
    }

    /**
     * Imports a draft translation into a target translation.
     * A new target translation will be created if one does not already exist.
     * This is a lengthy operation and should be run within a task
     * @param draftTranslation the draft translation to be imported
     * @return
     */
    suspend fun importDraftTranslation(
        nativeSpeaker: NativeSpeaker,
        draftTranslation: ResourceContainer,
    ): TargetTranslation {
        val targetLanguage = catalogClient.library.getTargetLanguage(draftTranslation.language.slug)
        // TRICKY: for now we only support "regular" or "obs" "text" translations
        // TODO: we should technically check if the project contains more than one resource
        //  when determining if it needs a regular slug or not.
        val resourceSlug =
            if (draftTranslation.project.slug == "obs") "obs" else Resource.REGULAR_SLUG

        val format = TranslationFormat.parse(draftTranslation.contentMimeType)
        val translation = createTargetTranslation(
            nativeSpeaker,
            targetLanguage!!,
            draftTranslation.project.slug,
            ResourceType.TEXT,
            resourceSlug,
            format
        )

        // convert legacy usx format to usfm
        val convertToUSFM = format == TranslationFormat.USX

        try {
            // commit local changes to history
            translation.commitSync()

            // begin import
            translation.applyProjectTitleTranslation(
                draftTranslation.readChunk("front", "title")
            )
            for (cSlug in draftTranslation.chapters()) {
                val ct = translation.getChapterTranslation(cSlug)
                translation.applyChapterTitleTranslation(
                    ct,
                    draftTranslation.readChunk(cSlug, "title")
                )
                translation.applyChapterReferenceTranslation(
                    ct,
                    draftTranslation.readChunk(cSlug, "reference")
                )
                for (fSlug in draftTranslation.chunks(cSlug)) {
                    val body = draftTranslation.readChunk(cSlug, fSlug)
                    val text = if (convertToUSFM) USXtoUSFMConverter.doConversion(body)
                        .toString() else body
                    translation.applyFrameTranslation(
                        translation.getFrameTranslation(cSlug, fSlug, format),
                        text
                    )
                }
            }
            // TODO: 3/23/2016 also import the front and back matter along with project title
            translation.setParentDraft(draftTranslation)
            translation.commitSync()
        } catch (e: IOException) {
            Logger.e(TAG, "Failed to import target translation", e)
            // TODO: 1/20/2016 revert changes
        } catch (e: Exception) {
            Logger.e(
                TAG,
                "Failed to save target translation before importing target translation",
                e
            )
        }
        return translation
    }

    suspend fun getConflictingTargetTranslation(file: File): TargetTranslation? {
        var conflictingTranslation: TargetTranslation? = null

        val targetTranslation = TargetTranslation.open(file)
        if (targetTranslation != null) {
            // TRICKY: the correct id is pulled from the manifest to avoid propagating bad folder names
            val targetTranslationId = targetTranslation.id
            val destTargetTranslationDir = File(path, targetTranslationId)

            // check if target already exists
            conflictingTranslation = TargetTranslation.open(destTargetTranslationDir, null)
        }
        return conflictingTranslation
    }

    /**
     * This will move a target translation into the root dir.
     * Any existing target translation will be replaced
     * @param tempTargetTranslation
     * @throws IOException
     */
    @Throws(IOException::class)
    fun restoreTargetTranslation(tempTargetTranslation: TargetTranslation?) {
        if (tempTargetTranslation != null) {
            val destDir = File(path, tempTargetTranslation.id)
            FileUtilities.safeDelete(destDir)
            FileUtilities.moveOrCopyQuietly(tempTargetTranslation.path, destDir)
        }
    }

    companion object {
        private const val TAG = "Translator"
        const val TSTUDIO_EXTENSION = "tstudio"
        const val ZIP_EXTENSION = "zip"
        const val USFM_EXTENSION = "usfm"
        const val TXT_EXTENSION = "usfm"
        const val PDF_EXTENSION = "pdf"
    }
}
