package org.bibletranslationtools.writer.usecases

import btt_writer.composeapp.generated.resources.Res
import btt_writer.composeapp.generated.resources.importing_draft
import org.bibletranslationtools.resourcecontainer.ResourceContainer
import org.bibletranslationtools.writer.core.Profile
import org.bibletranslationtools.writer.core.TargetTranslation
import org.bibletranslationtools.writer.core.Translator
import org.jetbrains.compose.resources.getString

class ImportDraft(
    private val translator: Translator,
    private val profile: Profile
) {
    suspend fun execute(
        draftTranslation: ResourceContainer,
        onProgress: (Float, String?) -> Unit = {_,_->}
    ): Result {
        onProgress(-1f, getString(Res.string.importing_draft))

        val targetTranslation = translator.importDraftTranslation(
            profile.nativeSpeaker,
            draftTranslation
        )

        return Result(targetTranslation)
    }

    data class Result(val targetTranslation: TargetTranslation?)
}