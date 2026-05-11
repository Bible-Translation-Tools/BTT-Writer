package org.bibletranslationtools.writer.usecases

import btt_writer.composeapp.generated.resources.Res
import btt_writer.composeapp.generated.resources.gogs_user_agent
import btt_writer.composeapp.generated.resources.pref_default_gogs_api
import org.bibletranslationtools.gogsclient.GogsAPI
import org.bibletranslationtools.gogsclient.User
import org.bibletranslationtools.logger.Logger
import org.bibletranslationtools.writer.core.Profile
import org.bibletranslationtools.writer.data.Preference
import org.bibletranslationtools.writer.data.getPref
import org.bibletranslationtools.writer.utils.ifNotNullOrEmpty
import org.jetbrains.compose.resources.getString

class GogsLogout(
    private val preference: Preference,
    private val profile: Profile
) {
    private lateinit var api: GogsAPI

    companion object {
        private const val TAG = "GogsLogout"
    }

    suspend fun execute() {
        val apiUrl = preference.getPref(
            Preference.KEY_PREF_GOGS_API,
            getString(Res.string.pref_default_gogs_api)
        )
        api = GogsAPI(
            apiUrl = apiUrl,
            userAgent = getString(Res.string.gogs_user_agent)
        )

        // local user (non-server account)
        var user = profile.gogsUser ?: return
        val token = user.token ?: return

        val tokenName = token.name
        val tokenSha1 = token.toString()

        // uses Basic authorization scheme, token should be null
        user = user.copy(password = tokenSha1, token = null)

        val tokenId = getTokenId(user, tokenName)
        if (tokenId < 0) return
        deleteToken(user, tokenId)
    }

    private suspend fun getTokenId(user: User, tokenName: String): Int {
        return api.listTokens(user)
            .find { it.name == tokenName }
            ?.id ?: -1
    }

    private suspend fun deleteToken(user: User, tokenId: Int) {
        val deleted = api.deleteToken(tokenId, user)
        if (!deleted) {
            val response = api.getLastResponse()
            val code = response?.code ?: -1
            val message = response?.message.ifNotNullOrEmpty { ", message: $it" }
            Logger.w(
                TAG,
                "Delete access token - gogs api responded with code $code$message"
            )
        }
    }
}