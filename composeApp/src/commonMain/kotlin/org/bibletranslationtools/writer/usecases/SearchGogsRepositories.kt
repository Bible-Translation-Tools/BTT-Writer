package org.bibletranslationtools.writer.usecases

import btt_writer.composeapp.generated.resources.Res
import btt_writer.composeapp.generated.resources.gogs_user_agent
import btt_writer.composeapp.generated.resources.pref_default_gogs_api
import org.bibletranslationtools.gogsclient.GogsAPI
import org.bibletranslationtools.gogsclient.Repository
import org.bibletranslationtools.writer.data.Preference
import org.bibletranslationtools.writer.data.getPref
import org.jetbrains.compose.resources.getString

class SearchGogsRepositories(
    private val preference: Preference
) {
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

        // fetch additional information about the repos (clone urls)
        for (repo in repos) {
            val extraRepo = api.getRepo(repo, null)
            if (extraRepo != null) {
                repositories.add(extraRepo)
            }
        }

        return repositories
    }
}