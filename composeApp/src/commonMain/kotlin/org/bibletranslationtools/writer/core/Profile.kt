package org.bibletranslationtools.writer.core

import io.ktor.serialization.JsonConvertException
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonDecodingException
import org.bibletranslationtools.gogsclient.Token
import org.bibletranslationtools.gogsclient.User
import org.bibletranslationtools.logger.Logger
import org.bibletranslationtools.writer.DirectoryProvider
import org.bibletranslationtools.writer.data.Preference
import org.bibletranslationtools.writer.data.setPref
import org.bibletranslationtools.writer.data.setPrefOrNull
import org.bibletranslationtools.writer.utils.FileUtilities
import org.bibletranslationtools.writer.utils.JsonLenient

/**
 * Represents a single user profile
 */
class Profile(
    private val prefs: Preference,
    private val directoryProvider: DirectoryProvider
) {
    @Serializable
    data class ProfileInfo(
        @SerialName("serial_version_uid")
        val versionUid: Long,
        @SerialName("full_name")
        val fullName: String = "",
        @SerialName("gogs_user")
        val gogsUser: User? = null,
        @SerialName("gogs_token")
        val gogsToken: Token? = null,
        @SerialName("terms_last_accepted")
        val termsLastAccepted: Int
    )

    /**
     * Returns the name of the translator.
     * The name from their gogs account will be used if it exists
     * @return
     */
    var fullName: String = ""
        get() = gogsUser?.fullName ?: field

    /**
     * Returns the gogs user associated with this profile
     */
    var gogsUser: User? = null

    val currentUser: String
        get() {
            return gogsUser?.username ?: fullName
        }

    /**
     * Returns the version of the terms of use accepted last time
     */
    var termsOfUseLastAccepted: Int = 0
        set(value) {
            field = value
            saveProfile()
        }

    /**
     * Returns true if the user is logged in
     */
    val loggedIn: Boolean
        get() = fullName.isEmpty().not()

    /**
     * Returns a native speaker version of this profile.
     * This is used when recording translators who contribute to a translation
     * @return
     */
    val nativeSpeaker: NativeSpeaker
        get() = NativeSpeaker(fullName)

    fun login(name: String, user: User? = null) {
        fullName = name
        gogsUser = user
        saveProfile()
    }

    /**
     * Logs the local user out of their account
     */
    fun logout() {
        fullName = ""
        gogsUser = null
        termsOfUseLastAccepted = 0
        deleteProfile()
    }

    /**
     * Save profile to the preferences
     */
    private fun saveProfile() {
        val profileString = this.toJSON()
        prefs.setPref(Preference.PROFILE, profileString)
    }

    /**
     * Deletes the profile from the preferences
     */
    private fun deleteProfile() {
        prefs.setPrefOrNull<String>(Preference.PROFILE, null)
        FileUtilities.deleteQuietly(directoryProvider.sshKeysDir)
    }

    /**
     * Returns the profile represented as a JSON object
     * @return
     */
    @Throws(JsonConvertException::class)
    fun toJSON(): String {
        val info = gogsUser?.let {
            ProfileInfo(
                versionUid = SERIAL_VERSION_UID,
                fullName = it.fullName ?: "",
                gogsUser = it,
                gogsToken = it.token,
                termsLastAccepted = termsOfUseLastAccepted
            )
        } ?: ProfileInfo(
            versionUid = SERIAL_VERSION_UID,
            fullName = fullName,
            termsLastAccepted = termsOfUseLastAccepted
        )

        return JsonLenient.encodeToString(info)
    }

    companion object {
        private const val TAG = "Profile"
        private const val SERIAL_VERSION_UID = 0L

        /**
         * Loads the user profile from JSON
         * @param json
         * @return
         * @throws Exception
         */
        @OptIn(ExperimentalSerializationApi::class)
        @Throws(Exception::class)
        fun fromJSON(
            prefs: Preference,
            directoryProvider: DirectoryProvider,
            json: String?
        ): Profile {
            var name = ""
            var user: User? = null
            var termsLastAccepted = 0

            try {
                val info: ProfileInfo? = json?.let { JsonLenient.decodeFromString(it) }

                info?.let {
                    if (it.versionUid != SERIAL_VERSION_UID) {
                        throw Exception("Unsupported profile version ${it.versionUid}. Expected $SERIAL_VERSION_UID")
                    }
                    user = it.gogsUser
                    name = it.fullName
                    termsLastAccepted = it.termsLastAccepted
                }

                if (user != null) {
                    user = user.copy(token = user.token)
                }
            } catch (e: JsonDecodingException) {
                Logger.w(TAG, "Malformed profile json string", e)
            }

            return Profile(prefs, directoryProvider).apply {
                fullName = name
                gogsUser = user
                termsOfUseLastAccepted = termsLastAccepted
            }
        }
    }
}
