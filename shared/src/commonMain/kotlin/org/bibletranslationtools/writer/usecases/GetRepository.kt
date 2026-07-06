package org.bibletranslationtools.writer.usecases

import org.bibletranslationtools.gogsclient.Repository
import org.bibletranslationtools.logger.Logger
import org.bibletranslationtools.writer.core.Profile
import org.bibletranslationtools.writer.core.TargetTranslation

class GetRepository(
    private val createRepository: CreateRepository,
    private val searchRepository: SearchGogsRepositories,
    private val profile: Profile
) {
    companion object {
        private const val TAG = "GetRepository"
    }

    suspend fun execute(
        translation: TargetTranslation,
        onProgress: (Float, String?) -> Unit = {_,_->}
    ): Repository? {
        onProgress(-1f, "Getting repository")

        val user = profile.gogsUser ?: run {
            Logger.e(TAG, "Gogs user is not set")
            return null
        }

        // Create repository
        // If it exists, will do nothing
        createRepository.execute(translation, onProgress)

        // Search for repository
        // There could be more than one repo, which name can contain requested repo name.
        // For example: en_ulb_mat_txt, custom_en_ulb_mat_text, en_ulb_mat_text_l3, etc.
        // Setting limit to 100 should be enough to cover most of the cases.
        val repositories = try {
            searchRepository.execute(
                user.id,
                translation.id,
                100,
                onProgress
            )
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to get the list of repositories", e)
            emptyList()
        }

        return repositories.find {
            it.owner?.username == user.username && it.name == translation.id
        }
    }
}