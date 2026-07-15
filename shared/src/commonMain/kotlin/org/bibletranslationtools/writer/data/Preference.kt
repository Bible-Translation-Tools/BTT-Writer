package org.bibletranslationtools.writer.data

import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.coroutines.getBooleanOrNullFlow
import com.russhwolf.settings.coroutines.getDoubleOrNullFlow
import com.russhwolf.settings.coroutines.getFloatOrNullFlow
import com.russhwolf.settings.coroutines.getIntOrNullFlow
import com.russhwolf.settings.coroutines.getLongOrNullFlow
import com.russhwolf.settings.coroutines.getStringOrNullFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.bibletranslationtools.writer.core.Migration
import org.bibletranslationtools.writer.core.TranslationViewMode
import java.util.Locale
import kotlin.reflect.KClass

class Preference(private val settings: ObservableSettings) {

    enum class Theme(val value: String) {
        LIGHT("light"),
        DARK("dark"),
        SYSTEM("system");

        companion object {
            fun of(value: String?): Theme =
                entries.firstOrNull { it.value == value } ?: SYSTEM
        }
    }

    companion object {
        // Pref keys
        const val ROOT_CATALOG_API = "root_catalog_api"
        const val GITHUB_REPO_API = "github_repo_api"
        const val LAST_VIEW_MODE = "last_view_mode_"
        const val LAST_FOCUS_CHAPTER = "last_focus_chapter_"
        const val LAST_FOCUS_FRAME = "last_focus_frame_"
        const val LAST_TRANSLATION = "last_translation"
        const val LAST_VERSION_CODE = "last_version_code"
        const val OPEN_SOURCE_TRANSLATIONS = "open_source_translations_"
        const val SELECTED_SOURCE_TRANSLATION = "selected_source_translation_"
        const val PROFILE = "profile"
        const val LAST_BACKUP = "last_backup_"
        const val LAST_UPLOADED = "last_uploaded_"

        const val KEY_PREF_TRANSLATION_TYPEFACE = "translation_typeface"
        const val KEY_PREF_TRANSLATION_TYPEFACE_SIZE = "typeface_size"
        const val KEY_PREF_SOURCE_TYPEFACE = "source_typeface"
        const val KEY_PREF_SOURCE_TYPEFACE_SIZE = "source_typeface_size"
        const val KEY_PREF_GOGS_API = "gogs_api"
        const val KEY_PREF_INDEX_SQLITE_URL = "index_sqlite_url"
        const val KEY_PREF_LANGUAGES_URL = "lang_names_url"
        const val KEY_PREF_MEDIA_SERVER = "media_server"
        const val KEY_PREF_READER_SERVER = "reader_server"
        const val KEY_PREF_CREATE_ACCOUNT_URL = "create_account_url"
        const val KEY_PREF_COLOR_THEME = "color_theme"
        const val KEY_PREF_CONTENT_SERVER = "content_server"
        const val KEY_PREF_TM_URL = "tm_url"
        const val KEY_PREF_CHECK_HARDWARE = "check_hardware_requirements"
        const val KEY_PREF_ENABLE_TM_LINKS = "enable_tm_links"
        const val KEY_PREF_GL_MODE = "gl_mode"
        const val KEY_PREF_BACKUP_INTERVAL = "backup_interval"
        const val KEY_PREF_LOGGING_LEVEL = "logging_level"
        const val KEY_PREF_MIGRATE_OLD_APP = "migrate_old_app"

        // Default values
        const val GITHUB_REPO_API_URL = "https://api.github.com/repos/Bible-Translation-Tools/BTT-Writer-Android"
        const val ROOT_CATALOG_API_URL = "/ts/txt/2/catalog.json"

        const val HELPDESK_WEBHOOK_URL = "https://helpdesk.techadvancement.com/wp-json/fluent-support/v2/public/incoming_webhook/"
        const val DEFAULT_HELPDESK_EMAIL = "bttwriter-desktop-feedback@techadvancement.com"
    }

    /**
     * Returns the value of a user preference, or null if not set.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> getPrefOrNull(key: String, type: KClass<T>): T? {
        if (!settings.hasKey(key)) return null
        return when (type) {
            String::class  -> settings.getString(key, "")        as T
            Boolean::class -> settings.getBoolean(key, false)    as T
            Int::class     -> settings.getInt(key, 0)            as T
            Long::class    -> settings.getLong(key, 0L)          as T
            Float::class   -> settings.getFloat(key, 0f)         as T
            Double::class  -> settings.getDouble(key, 0.0)       as T
            else -> throw IllegalArgumentException(
                "Unsupported preference type: ${type.simpleName}"
            )
        }
    }

    /**
     * Returns the value of a user preference, or [defaultValue] if not set.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> getPref(key: String, defaultValue: T, type: KClass<T>): T {
        return when (type) {
            String::class  -> settings.getString(key, defaultValue as String)   as T
            Boolean::class -> settings.getBoolean(key, defaultValue as Boolean) as T
            Int::class     -> settings.getInt(key, defaultValue as Int)         as T
            Long::class    -> settings.getLong(key, defaultValue as Long)       as T
            Float::class   -> settings.getFloat(key, defaultValue as Float)     as T
            Double::class  -> settings.getDouble(key, defaultValue as Double)   as T
            else -> throw IllegalArgumentException(
                "Unsupported preference type: ${type.simpleName}"
            )
        }
    }

    /**
     * Returns the value of a user preference, or [defaultValue] if not set.
     * Unlike the non-nullable overload, [defaultValue] itself can be null,
     * so the return type is nullable too.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> getPrefOrNull(key: String, defaultValue: T?, type: KClass<T>): T? {
        if (!settings.hasKey(key)) return defaultValue
        return when (type) {
            String::class  -> settings.getString(key, "")      as T
            Boolean::class -> settings.getBoolean(key, false)  as T
            Int::class     -> settings.getInt(key, 0)          as T
            Long::class    -> settings.getLong(key, 0L)        as T
            Float::class   -> settings.getFloat(key, 0f)       as T
            Double::class  -> settings.getDouble(key, 0.0)     as T
            else -> throw IllegalArgumentException(
                "Unsupported preference type: ${type.simpleName}"
            )
        }
    }

    /**
     * Saves a user preference.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> setPref(key: String, value: T, type: KClass<T>) {
        when (type) {
            String::class  -> settings.putString(key, value as String)
            Boolean::class -> settings.putBoolean(key, value as Boolean)
            Int::class     -> settings.putInt(key, value as Int)
            Long::class    -> settings.putLong(key, value as Long)
            Float::class   -> settings.putFloat(key, value as Float)
            Double::class  -> settings.putDouble(key, value as Double)
            else -> throw IllegalArgumentException(
                "Unsupported preference type: ${type.simpleName}"
            )
        }
    }

    /**
     * Saves a user preference, or removes the key if [value] is null.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> setPrefOrNull(key: String, value: T?, type: KClass<T>) {
        if (value == null) {
            settings.remove(key)
            return
        }
        when (type) {
            String::class  -> settings.putString(key, value as String)
            Boolean::class -> settings.putBoolean(key, value as Boolean)
            Int::class     -> settings.putInt(key, value as Int)
            Long::class    -> settings.putLong(key, value as Long)
            Float::class   -> settings.putFloat(key, value as Float)
            Double::class  -> settings.putDouble(key, value as Double)
            else -> throw IllegalArgumentException(
                "Unsupported preference type: ${type.simpleName}"
            )
        }
    }

    /**
     * Returns a [Flow] that emits whenever the preference changes.
     * Emits null if the key is not set.
     */
    @OptIn(ExperimentalSettingsApi::class)
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> getPrefFlowOrNull(key: String, type: KClass<T>): Flow<T?> {
        return when (type) {
            String::class  -> settings.getStringOrNullFlow(key)  as Flow<T?>
            Boolean::class -> settings.getBooleanOrNullFlow(key) as Flow<T?>
            Int::class     -> settings.getIntOrNullFlow(key)     as Flow<T?>
            Long::class    -> settings.getLongOrNullFlow(key)    as Flow<T?>
            Float::class   -> settings.getFloatOrNullFlow(key)   as Flow<T?>
            Double::class  -> settings.getDoubleOrNullFlow(key)  as Flow<T?>
            else -> throw IllegalArgumentException(
                "Unsupported preference type: ${type.simpleName}"
            )
        }
    }

    /**
     * Returns a [Flow] that emits whenever the preference changes,
     * falling back to [defaultValue] if the key is not set.
     */
    fun <T : Any> getPrefFlow(key: String, defaultValue: T, type: KClass<T>): Flow<T> {
        return getPrefFlowOrNull(key, type).map { it ?: defaultValue }
    }

    fun remove(key: String) = settings.remove(key)

    fun clearAll() = settings.clear()

    fun hasKey(key: String) = settings.hasKey(key)

    // ---------------- Specific settings ---------------- //

    /**
     * Returns the last view mode of the target translation.
     * The default view mode will be returned if there is no recorded last view mode
     *
     * @param targetTranslationId
     * @return
     */
    fun getLastViewMode(targetTranslationId: String): TranslationViewMode {
        try {
            val modeName = getPref(
                LAST_VIEW_MODE + targetTranslationId,
                TranslationViewMode.READ.name
            )
            return TranslationViewMode.valueOf(modeName.uppercase(Locale.getDefault()))
        } catch (_: Exception) {
        }
        return TranslationViewMode.READ
    }

    /**
     * Sets the last opened view mode for a target translation
     * @param targetTranslationId
     * @param viewMode
     */
    fun setLastViewMode(targetTranslationId: String, viewMode: TranslationViewMode) {
        setPref(LAST_VIEW_MODE + targetTranslationId, viewMode.name.uppercase())
    }

    /**
     * Returns the id of the chapter that was last in focus for this target translation
     * @param targetTranslationId
     * @return
     */
    fun getLastFocusChapterId(targetTranslationId: String): String? {
        return getPrefOrNull(LAST_FOCUS_CHAPTER + targetTranslationId, null)
    }

    /**
     * Returns the id of the frame that was last in focus for this target translation
     * @param targetTranslationId
     * @return
     */
    fun getLastFocusFrameId(targetTranslationId: String): String? {
        return getPrefOrNull(LAST_FOCUS_FRAME + targetTranslationId, null)
    }

    /**
     * Sets the last focused chapter and frame for a target translation
     * @param targetTranslationId
     * @param chapterId
     * @param frameId
     */
    fun setLastFocus(targetTranslationId: String, chapterId: String?, frameId: String?) {
        setPrefOrNull(LAST_FOCUS_CHAPTER + targetTranslationId, chapterId)
        setPrefOrNull(LAST_FOCUS_FRAME + targetTranslationId, frameId)
    }

    /**
     * Returns an array of open source translation tabs on a target translation
     * @param targetTranslationId
     * @return
     */
    fun getOpenSourceTranslations(targetTranslationId: String): List<String> {
        val idSet = getPrefOrNull<String>(
            OPEN_SOURCE_TRANSLATIONS + targetTranslationId
        )?.trim()

        if (idSet.isNullOrEmpty()) {
            return listOf()
        } else {
            val ids = idSet.split("\\|".toRegex()).toMutableList()
            for (i in ids.indices) {
                Migration.migrateSourceTranslationSlug(ids[i])?.let {
                    ids[i] = it
                }
            }
            return ids
        }
    }

    /**
     * Adds a source translation to the list of open tabs on a target translation
     * @param targetTranslationId
     * @param sourceTranslationId
     */
    fun addOpenSourceTranslation(
        targetTranslationId: String,
        sourceTranslationId: String
    ) {
        val newIdSet = getOpenSourceTranslations(targetTranslationId)
            .mapNotNull {
                if (it != sourceTranslationId) "$it|" else null
            } + sourceTranslationId

        setPref(OPEN_SOURCE_TRANSLATIONS + targetTranslationId, newIdSet.joinToString(""))
    }

    /**
     * Removes a source translation from the list of open tabs on a target translation
     * @param targetTranslationId
     * @param sourceTranslationId
     */
    fun removeOpenSourceTranslation(
        targetTranslationId: String,
        sourceTranslationId: String
    ) {
        val sourceTranslationIds = getOpenSourceTranslations(targetTranslationId)

        // unset selected tab if the removed translation was selected
        if (sourceTranslationId == getSelectedSourceTranslationId(targetTranslationId)) {
            setSelectedSourceTranslation(targetTranslationId, null)
        }

        val newIdSet = sourceTranslationIds
            .filter { it != sourceTranslationId }
            .joinToString("|")
            .ifEmpty { null }

        setPrefOrNull(OPEN_SOURCE_TRANSLATIONS + targetTranslationId, newIdSet)
    }

    /**
     * Returns the selected open source translation tab on the target translation
     * If there is no selection the first open tab will be set as the selected tab
     * @param targetTranslationId
     * @return
     */
    fun getSelectedSourceTranslationId(targetTranslationId: String): String? {
        var selectedSourceTranslationId = getPrefOrNull<String>(
            SELECTED_SOURCE_TRANSLATION + targetTranslationId
        )

        if (selectedSourceTranslationId.isNullOrEmpty()) {
            // default to first tab
            val openSourceTranslationIds = getOpenSourceTranslations(targetTranslationId)
            if (openSourceTranslationIds.isNotEmpty()) {
                selectedSourceTranslationId = openSourceTranslationIds[0]
                setSelectedSourceTranslation(targetTranslationId, selectedSourceTranslationId)
            }
        }
        return Migration.migrateSourceTranslationSlug(selectedSourceTranslationId)
    }

    /**
     * Sets or removes the selected open source translation tab on a target translation
     * @param targetTranslationId
     * @param sourceTranslationId if null the selection will be unset
     */
    fun setSelectedSourceTranslation(
        targetTranslationId: String,
        sourceTranslationId: String?
    ) {
        setPrefOrNull(
            SELECTED_SOURCE_TRANSLATION + targetTranslationId,
            Migration.migrateSourceTranslationSlug(sourceTranslationId)
        )
    }

    fun moveTargetTranslationAppSettings(
        targetTranslationId: String,
        newTargetTranslationId: String
    ) {
        val sources = getOpenSourceTranslations(targetTranslationId)
        for (source in sources) {
            addOpenSourceTranslation(newTargetTranslationId, source)
        }

        val source = getSelectedSourceTranslationId(targetTranslationId)
        setSelectedSourceTranslation(newTargetTranslationId, source)

        val lastFocusChapterId = getLastFocusChapterId(targetTranslationId)
        val lastFocusFrameId = getLastFocusFrameId(targetTranslationId)
        setLastFocus(newTargetTranslationId, lastFocusChapterId, lastFocusFrameId)

        val lastViewMode = getLastViewMode(targetTranslationId)
        setLastViewMode(newTargetTranslationId, lastViewMode)

        clearTargetTranslationSettings(targetTranslationId)
    }

    /**
     * Removes all settings for a target translation
     * @param targetTranslationId
     */
    fun clearTargetTranslationSettings(targetTranslationId: String) {
        setPrefOrNull<String>(SELECTED_SOURCE_TRANSLATION + targetTranslationId, null)
        setPrefOrNull<String>(OPEN_SOURCE_TRANSLATIONS + targetTranslationId, null)
        setPrefOrNull<String>(LAST_FOCUS_FRAME + targetTranslationId, null)
        setPrefOrNull<String>(LAST_FOCUS_CHAPTER + targetTranslationId, null)
        setPrefOrNull<String>(LAST_VIEW_MODE + targetTranslationId, null)
    }

    fun getGithubRepoApi(): String {
        return getPref(GITHUB_REPO_API, GITHUB_REPO_API_URL)
    }

    fun getRootCatalogApi(): String {
        return getPref(ROOT_CATALOG_API, ROOT_CATALOG_API_URL)
    }
}

inline fun <reified T : Any> Preference.getPrefOrNull(key: String): T? =
    getPrefOrNull(key, T::class)

inline fun <reified T : Any> Preference.getPref(key: String, defaultValue: T): T =
    getPref(key, defaultValue, T::class)

inline fun <reified T : Any> Preference.getPrefOrNull(key: String, defaultValue: T?): T? =
    getPrefOrNull(key, defaultValue, T::class)

inline fun <reified T : Any> Preference.setPref(key: String, value: T) =
    setPref(key, value, T::class)

inline fun <reified T : Any> Preference.setPrefOrNull(key: String, value: T?) =
    setPrefOrNull(key, value, T::class)

inline fun <reified T : Any> Preference.getPrefFlowOrNull(key: String): Flow<T?> =
    getPrefFlowOrNull(key, T::class)

inline fun <reified T : Any> Preference.getPrefFlow(key: String, defaultValue: T): Flow<T> =
    getPrefFlow(key, defaultValue, T::class)