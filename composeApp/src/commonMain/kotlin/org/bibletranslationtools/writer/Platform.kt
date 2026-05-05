package org.bibletranslationtools.writer

import androidx.compose.ui.platform.ClipEntry
import btt_writer.composeapp.generated.resources.Res
import btt_writer.composeapp.generated.resources.pref_default_logging_level
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.FileKitType
import org.bibletranslationtools.logger.GithubReporter
import org.bibletranslationtools.logger.LogLevel
import org.bibletranslationtools.logger.Logger
import org.bibletranslationtools.writer.data.Preference
import org.bibletranslationtools.writer.data.getPref
import org.bibletranslationtools.writer.utils.FileUtilities
import org.bibletranslationtools.writer.utils.getStringBlocking
import java.io.File
import java.io.IOException
import java.text.DecimalFormat
import java.util.Locale

data class AppInfo(
    val versionName: String,
    val versionCode: Int,
    val model: String,
    val device: String,
    val manufacturer: String
)

interface Platform {
    val info: AppInfo
    val udid: String
        get() = info.model.lowercase().replace(" ", "_")
    val isStoreVersion: Boolean
    val deviceLanguageCode: String
        get() {
            val code = Locale.getDefault().language
            return code.replace("[_-]$".toRegex(), "")
        }
    val isNetworkAvailable: Boolean

    fun exit()

    fun shareApp()
    fun shareProject(file: File)

    fun calculateSystemResources(): String
    fun getTotalRam(): Long
    fun getFormattedSize(bytes: Long): String {
        if (bytes / GB > 0) return formatWithUnits(bytes.toDouble() / GB, "GB")
        if (bytes / MB > 0) return formatWithUnits(bytes.toDouble() / MB, "MB")
        if (bytes / KB > 0) return formatWithUnits(bytes.toDouble() / KB, "KB")
        return bytes.toString() + "B"
    }

    fun formatWithUnits(size: Double, units: String): String {
        if (size >= 100) return (size + 0.5).toLong().toString() + units
        val decimalFormat = if (size >= 10) DecimalFormat("#.#") else DecimalFormat("#.##")
        return decimalFormat.format(size) + units
    }

    fun initLogger(preference: Preference, directoryProvider: DirectoryProvider) {
        preference.getPref(
            Preference.KEY_PREF_LOGGING_LEVEL,
            getStringBlocking(Res.string.pref_default_logging_level)
        ).let { minLogLevel ->
            Logger.configure(
                directoryProvider.logFile,
                LogLevel.getLevel(minLogLevel)
            )
        }

        val dir = File(directoryProvider.externalAppDir, "crashes")
        if (!dir.exists()) {
            try {
                FileUtilities.forceMkdir(dir)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        Logger.registerGlobalExceptionHandler(dir)
    }

    companion object {
        const val KB: Long = 1024
        const val MB: Long = KB * KB
        const val GB: Long = MB * KB
        const val TB: Long = GB * KB

        const val MIN_CHECKING_LEVEL: Int = 3
        // 96 MB, Minimum RAM needed for reliable operation
        const val MINIMUM_REQUIRED_RAM: Long = (96 * 1024 * 1024).toLong()
        // Minimum number of processors needed for reliable operations
        const val MINIMUM_NUMBER_OF_PROCESSORS: Long = 2
    }
}

expect fun getGithubReporter(repoUrl: String, oAuthToken: String): GithubReporter
expect fun textClipEntry(text: String, label: String? = null): ClipEntry
expect fun ClipEntry.textOrNull(): String?
expect val ClipEntry.label: String?

expect fun getSupportedUsfmExtensions(): FileKitType.File
expect fun getSupportedTstudioExtensions(): FileKitType.File
expect val PlatformFile.displayName: String
