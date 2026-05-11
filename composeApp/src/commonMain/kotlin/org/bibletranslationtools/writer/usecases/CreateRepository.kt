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
import org.bibletranslationtools.writer.utils.ifNotNullOrEmpty
import org.jetbrains.compose.resources.getString

class CreateRepository(
    private val prefRepo: Preference,
    private val profile: Profile
) {
    companion object {
        private const val TAG = "CreateRepository"
    }

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
                val id = targetTranslation.id
                val code = response?.code ?: -1
                val message = response?.message.ifNotNullOrEmpty { ", message: $it" }
                Logger.w(
                    TAG,
                    "Failed to create repository $id. Gogs responded with code $code$message"
                )
            }
        }

        return false
    }
}