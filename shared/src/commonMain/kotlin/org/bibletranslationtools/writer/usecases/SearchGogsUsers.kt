package org.bibletranslationtools.writer.usecases

import btt_writer.shared.generated.resources.Res
import btt_writer.shared.generated.resources.gogs_user_agent
import btt_writer.shared.generated.resources.pref_default_gogs_api
import org.bibletranslationtools.gogsclient.GogsAPI
import org.bibletranslationtools.gogsclient.User
import org.bibletranslationtools.writer.data.Preference
import org.bibletranslationtools.writer.data.getPref
import org.bibletranslationtools.writer.utils.ifNotNullOrEmpty
import org.jetbrains.compose.resources.getString

class SearchGogsUsers(
    private val preference: Preference
) {
    companion object {
        const val TAG = "SearchGogsUsers"
    }

    @Throws(Exception::class)
    suspend fun execute(
        userQuery: String,
        limit: Int,
        onProgress: (Float, String?) -> Unit = {_,_->}
    ): List<User> {
        onProgress(-1f, "Searching for users")

        val api = GogsAPI(
            apiUrl = preference.getPref(
                Preference.KEY_PREF_GOGS_API,
                getString(Res.string.pref_default_gogs_api)
            ),
            userAgent = getString(Res.string.gogs_user_agent)
        )

        val users = api.searchUsers(userQuery, limit, null)
        val response = api.getLastResponse()

        if (response != null) {
            val code = response.code
            val message = response.message.ifNotNullOrEmpty { ", message: $it" }
            throw Exception("Failed to get the list of users. Gogs responded with code $code$message")
        }

        return users
    }
}