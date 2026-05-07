package org.bibletranslationtools.writer.usecases

import btt_writer.composeapp.generated.resources.Res
import btt_writer.composeapp.generated.resources.gogs_user_agent
import btt_writer.composeapp.generated.resources.pref_default_gogs_api
import org.bibletranslationtools.gogsclient.GogsAPI
import org.bibletranslationtools.gogsclient.Repository
import org.bibletranslationtools.logger.Logger
import org.bibletranslationtools.writer.core.Profile
import org.bibletranslationtools.writer.core.TargetTranslation
import org.bibletranslationtools.writer.data.Preference
import org.bibletranslationtools.writer.data.getPref
import org.jetbrains.compose.resources.getString

class CreateRepository(
    private val prefRepo: Preference,
    private val profile: Profile
) {
    suspend fun execute(
        targetTranslation: TargetTranslation,
        onProgress: (Float, String?) -> Unit = {_,_->}
    ): Boolean {
        onProgress(-1f, "Preparing location on server")

        val api = GogsAPI(
            apiUrl = prefRepo.getPref(
                Preference.KEY_PREF_GOGS_API,
                getString(Res.string.pref_default_gogs_api)
            ),
            userAgent = getString(Res.string.gogs_user_agent)
        )
        profile.gogsUser?.let { user ->
            val templateRepo = Repository(targetTranslation.id)
            val repo = api.createRepo(templateRepo, user)
            val response = api.getLastResponse()

            val alreadyExists = response?.code == 409
            val created = repo != null && response?.success == true

            if (created || alreadyExists) {
                return true
            } else {
                Logger.w(
                    this.javaClass.name,
                    "Failed to create repository " + targetTranslation.id + ". Gogs responded with " + response?.code + ": " + response?.message
                )
            }
        }

        return false
    }
}