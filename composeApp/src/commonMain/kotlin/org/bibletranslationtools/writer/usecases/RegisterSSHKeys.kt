package org.bibletranslationtools.writer.usecases

import btt_writer.composeapp.generated.resources.Res
import btt_writer.composeapp.generated.resources.gogs_public_key_name
import btt_writer.composeapp.generated.resources.gogs_user_agent
import btt_writer.composeapp.generated.resources.pref_default_gogs_api
import org.bibletranslationtools.gogsclient.GogsAPI
import org.bibletranslationtools.gogsclient.PublicKey
import org.bibletranslationtools.logger.Logger
import org.bibletranslationtools.writer.DirectoryProvider
import org.bibletranslationtools.writer.Platform
import org.bibletranslationtools.writer.core.Profile
import org.bibletranslationtools.writer.data.Preference
import org.bibletranslationtools.writer.data.getPref
import org.bibletranslationtools.writer.utils.FileUtilities
import org.bibletranslationtools.writer.utils.ifNotNullOrEmpty
import org.jetbrains.compose.resources.getString
import java.io.IOException

class RegisterSSHKeys(
    private val profile: Profile,
    private val directoryProvider: DirectoryProvider,
    private val preference: Preference,
    private val platform: Platform
) {
    companion object {
        private const val TAG = "RegisterSSHKeys"
    }

    suspend fun execute(
        force: Boolean,
        onProgress: (Float, String?) -> Unit = {_,_->}
    ): Boolean {
        onProgress(-1f, "Authenticating")

        val keyName = getString(
            Res.string.gogs_public_key_name
        ) + " " + platform.udid

        val api = GogsAPI(
            apiUrl = preference.getPref(
                Preference.KEY_PREF_GOGS_API,
                getString(Res.string.pref_default_gogs_api)
            ),
            userAgent = getString(Res.string.gogs_user_agent)
        )

        profile.gogsUser?.let { user ->
            if (!directoryProvider.hasSSHKeys() || force) {
                directoryProvider.generateSSHKeys(platform.udid)
            }
            val keyString: String?
            try {
                keyString = FileUtilities.readFileToString(directoryProvider.publicKey).trim()
            } catch (e: IOException) {
                Logger.e(TAG, "Failed to retrieve the public key", e)
                return false
            }

            val keyTemplate = PublicKey(title = keyName, key = keyString)

            // delete old key
            val keys = api.listPublicKeys(user)
            for (k in keys) {
                if (k.title == keyTemplate.title) {
                    api.deletePublicKey(k, user)
                    break
                }
            }

            // create new key
            val key = api.createPublicKey(keyTemplate, user)
            if (key != null) {
                return true
            } else {
                val response = api.getLastResponse()
                val code = response?.code ?: -1
                val message = response?.message.ifNotNullOrEmpty { " message: $it" }
                Logger.w(
                    TAG,
                    "Failed to register the public key. Gogs responded with $code $message"
                )
            }
        }

        return false
    }
}