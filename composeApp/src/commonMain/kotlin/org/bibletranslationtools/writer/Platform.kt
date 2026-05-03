package org.bibletranslationtools.writer

import androidx.compose.ui.platform.ClipEntry
import org.bibletranslationtools.logger.GithubReporter
import java.io.File
import java.text.DecimalFormat
import java.util.Locale

data class AppInfo(
    val versionName: String,
    val versionCode: Int,
    val model: String,
    val device: String,
    val manufacturer: String
)

object AppConfig {
    var versionName: String = ""
        private set
    var versionCode: Int = 0
        private set
    var githubToken: String = ""
        private set

    fun init(
        versionName: String,
        versionCode: Int,
        githubToken: String
    ) {
        this.versionName = versionName
        this.versionCode = versionCode
        this.githubToken = githubToken
    }
}

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

    fun restart()
    fun exit()
    fun configureLogger(minLogLevel: Int)

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
expect fun textClipEntry(text: String): ClipEntry
