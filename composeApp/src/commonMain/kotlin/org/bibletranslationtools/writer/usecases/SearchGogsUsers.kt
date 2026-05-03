package org.bibletranslationtools.writer.usecases

import btt_writer.composeapp.generated.resources.Res
import btt_writer.composeapp.generated.resources.gogs_user_agent
import btt_writer.composeapp.generated.resources.pref_default_gogs_api
import org.bibletranslationtools.gogsclient.GogsAPI
import org.bibletranslationtools.gogsclient.User
import org.bibletranslationtools.writer.data.Preference
import org.bibletranslationtools.writer.data.getPref
import org.jetbrains.compose.resources.getString

class SearchGogsUsers(
    private val preference: Preference
) {
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
        return api.searchUsers(userQuery, limit, null)
    }
}