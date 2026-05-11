package org.bibletranslationtools.writer.usecases

import btt_writer.composeapp.generated.resources.Res
import btt_writer.composeapp.generated.resources.gogs_token_name
import btt_writer.composeapp.generated.resources.gogs_user_agent
import btt_writer.composeapp.generated.resources.pref_default_gogs_api
import org.bibletranslationtools.gogsclient.GogsAPI
import org.bibletranslationtools.gogsclient.Token
import org.bibletranslationtools.gogsclient.User
import org.bibletranslationtools.logger.Logger
import org.bibletranslationtools.writer.Platform
import org.bibletranslationtools.writer.data.Preference
import org.bibletranslationtools.writer.data.getPref
import org.bibletranslationtools.writer.utils.ifNotNullOrEmpty
import org.jetbrains.compose.resources.getString

class GogsLogin(
    private val platform: Platform,
    private val preference: Preference
) {
    private lateinit var api: GogsAPI

    suspend fun execute(
        username: String,
        password: String,
        fullName: String? = null
    ): LoginResult {
        val apiUrl = preference.getPref(
            Preference.KEY_PREF_GOGS_API,
            getString(Res.string.pref_default_gogs_api)
        )
        api = GogsAPI(
            apiUrl = apiUrl,
            userAgent = getString(Res.string.gogs_user_agent)
        )
        val authUser = User(username = username, password = password)
        val tokenName = getTokenStub()

        // get user
        var user = api.getUser(authUser, authUser)
        if (user != null) {
            val tokenId = getTokenId(tokenName, authUser)
            if (tokenId != -1) {
                // Delete (if exists) matching token for this device on server
                deleteToken(tokenId, authUser)
            }

            // Create a new token
            val t = Token(name = tokenName, scopes = listOf("write:repository", "write:user"), id = 0)
            user = user.copy(token = api.createToken(t, authUser))

            // validate access token
            if (user.token == null) {
                val response = api.getLastResponse()
                val code = response?.code ?: -1
                val message = response?.message.ifNotNullOrEmpty { ", message: $it" }
                Logger.w(
                    GogsLogin::class.java.name,
                    "gogs api responded with code $code$message"
                )
                return LoginResult(null)
            }

            // set missing full_name
            if (user.fullName.isNullOrEmpty() && !fullName.isNullOrEmpty()) {
                user = user.copy(fullName = fullName)
                val updatedUser = api.editUser(user, authUser)
                if (updatedUser == null) {
                    val response = api.getLastResponse()
                    val code = response?.code ?: -1
                    val message = response?.message.ifNotNullOrEmpty { ", message: $it" }
                    Logger.w(
                        GogsLogin::class.java.name,
                        "The full_name could not be updated gogs api responded with $code$message"
                    )
                }
            }
        }

        return LoginResult(user)
    }

    private suspend fun getTokenStub(): String {
        val defaultTokenName = getString(Res.string.gogs_token_name)
        val androidId = platform.info.device.lowercase()
        val nickname = platform.udid
        val tokenSuffix = String.format("%s_%s__%s", platform.info.manufacturer, nickname, androidId)
        return (defaultTokenName + "__" + tokenSuffix).replace(" ", "_")
    }

    private suspend fun getTokenId(tokenName: String, user: User): Int {
        return api.listTokens(user)
            .find { it.name == tokenName }
            ?.id ?: -1
    }

    private suspend fun deleteToken(tokenId: Int, user: User) {
        val deleted = api.deleteToken(tokenId, user)
        if (!deleted) {
            val response = api.getLastResponse()
            val code = response?.code ?: -1
            val message = response?.message.ifNotNullOrEmpty { ", message: $it" }
            Logger.w(
                GogsLogin::class.java.name,
                "Delete access token - gogs api responded with code $code$message"
            )
        }
    }

    data class LoginResult(val user: User?)
}