package org.bibletranslationtools.writer.usecases

import btt_writer.shared.generated.resources.Res
import btt_writer.shared.generated.resources.gogs_user_agent
import btt_writer.shared.generated.resources.pref_default_gogs_api
import org.bibletranslationtools.gogsclient.GogsAPI
import org.bibletranslationtools.gogsclient.Repository
import org.bibletranslationtools.writer.data.Preference
import org.bibletranslationtools.writer.data.getPref
import org.bibletranslationtools.writer.utils.ifNotNullOrEmpty
import org.jetbrains.compose.resources.getString

class SearchGogsRepositories(
    private val preference: Preference
) {
    @Throws(Exception::class)
    suspend fun execute(
        uid: Int,
        query: String,
        limit: Int,
        onProgress: (Float, String?) -> Unit = {_,_->}
    ): List<Repository> {
        onProgress(-1f, "Searching for repositories")
        val repositories = arrayListOf<Repository>()

        val repoQuery = query.ifEmpty { "_" }

        val api = GogsAPI(
            apiUrl = preference.getPref(
                Preference.KEY_PREF_GOGS_API,
                getString(Res.string.pref_default_gogs_api)
            ),
            userAgent = getString(Res.string.gogs_user_agent)
        )
        val repos = api.searchRepos(repoQuery, uid, limit)
        val response = api.getLastResponse()

        if (response != null) {
            val code = response.code
            val message = response.message.ifNotNullOrEmpty { ", message: $it" }
            throw Exception("Failed to get the list of repos. Gogs responded with code $code$message")
        }

        // fetch additional information about the repos (clone urls)
        for (repo in repos) {
            val extraRepo = api.getRepo(repo, null)
            val response = api.getLastResponse()

            if (response != null) {
                val code = response.code
                val message = response.message.ifNotNullOrEmpty { ", message: $it" }
                throw Exception("Failed to get the repo info. Gogs responded with code $code$message")
            }

            if (extraRepo != null) {
                repositories.add(extraRepo)
            }
        }

        return repositories
    }
}