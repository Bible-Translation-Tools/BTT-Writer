package org.bibletranslationtools.writer

import android.app.Activity
import android.app.ActivityManager
import android.content.ClipData
import android.content.Context
import android.content.Context.ACTIVITY_SERVICE
import android.content.Context.CONNECTIVITY_SERVICE
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.provider.OpenableColumns
import androidx.compose.ui.platform.ClipEntry
import androidx.core.content.FileProvider
import btt_writer.composeapp.generated.resources.Res
import btt_writer.composeapp.generated.resources.send_to
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.context
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.toAndroidUri
import io.github.vinceglb.filekit.name
import kotlinx.coroutines.runBlocking
import org.bibletranslationtools.logger.GithubReporter
import org.bibletranslationtools.logger.Logger
import org.bibletranslationtools.writer.Platform.Companion.GB
import org.bibletranslationtools.writer.Platform.Companion.KB
import org.bibletranslationtools.writer.Platform.Companion.MB
import org.bibletranslationtools.writer.Platform.Companion.TB
import org.bibletranslationtools.writer.utils.FileUtilities
import org.bibletranslationtools.writer.utils.RuntimeWrapper
import org.jetbrains.compose.resources.getString
import java.io.File
import java.io.RandomAccessFile

class AndroidPlatform(
    private val context: Context,
    private val directoryProvider: DirectoryProvider
) : Platform {

    override val info: AppInfo
        get() = AppInfo(
            versionName = BuildInfo.VERSION_NAME,
            versionCode = BuildInfo.VERSION_CODE.toInt(),
            model = Build.MODEL,
            device = Build.DEVICE,
            manufacturer = Build.MANUFACTURER
        )

    override val isNetworkAvailable: Boolean
        get() {
            val cm = context.getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
            val net = cm.activeNetwork ?: return false
            val actNet = cm.getNetworkCapabilities(net) ?: return false
            return when {
                actNet.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                actNet.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                else -> false
            }
        }

    override val isStoreVersion: Boolean
        get() {
            val installer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                context.packageManager.getInstallSourceInfo(context.packageName).installingPackageName
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getInstallerPackageName(context.packageName)
            }
            return !installer.isNullOrEmpty()
        }

    override val isAndroid = true

    override fun exit() {
        (context as? Activity)?.finishAffinity()
    }

    override fun shareApp() {
        val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        pInfo.applicationInfo?.let { info ->
            val apkFile = File(info.publicSourceDir)
            val exportFile = File(
                directoryProvider.sharingDir, info.loadLabel(
                    context.packageManager
                ).toString() + "_" + pInfo.versionName + ".apk"
            )
            FileUtilities.copyFile(apkFile, exportFile)
            shareArchive(exportFile)
        }
    }

    override fun shareProject(file: File) {
        shareArchive(file)
    }

    override fun calculateSystemResources(): String {
        val am = context.getSystemService(ACTIVITY_SERVICE) as ActivityManager
        var message = "System Resources:\n"
        val numProcessors = RuntimeWrapper.availableProcessors
        message += "Number of processor cores: $numProcessors " +
                "(${Platform.MINIMUM_NUMBER_OF_PROCESSORS} required)\n"
        val maxMem = RuntimeWrapper.maxMemory
        message += "JVM max memory: ${getFormattedSize(maxMem)} " +
                "(${getFormattedSize(Platform.MINIMUM_REQUIRED_RAM)} required)\n"

        val memoryInfo = ActivityManager.MemoryInfo()
        am.getMemoryInfo(memoryInfo)
        message += "Available memory on the system: " +
                "${getFormattedSize(memoryInfo.availMem)}\n"
        message += "Total memory on the system (getMemoryInfo): " +
                "${getFormattedSize(memoryInfo.totalMem)}\n"
        message += "Total memory on the system (/proc/meminfo): " +
                "${getFormattedSize(getTotalRam())}\n"
        message += "Low memory threshold on the system: " +
                "${getFormattedSize(memoryInfo.threshold)}\n"
        message += "Low memory state on the system: ${memoryInfo.lowMemory}\n"

        message += "Manufacturer: ${Build.MANUFACTURER}\n"
        message += "Model: ${info.model}\n"
        message += "Version: ${Build.VERSION.SDK_INT}\n"
        message += "Version Release: ${Build.VERSION.RELEASE}\n"

        val displayMetrics = context.resources.displayMetrics
        message += "\nScreen size ${displayMetrics.heightPixels}H*${displayMetrics.widthPixels}W"
        message += ", density: ${displayMetrics.density}"
        message += ", dpi: ${displayMetrics.xdpi}X*${displayMetrics.ydpi}Y"

        Logger.i(this.javaClass.simpleName, "system resources check:\n$message")

        return message
    }

    override fun getTotalRam(): Long {
        var lastValue: Long = 0
        try {
            RandomAccessFile("/proc/meminfo", "r").use { reader ->
                val load = reader.readLine()

                val parts = load.trim().split("\\s+".toRegex())
                val value = parts[1]
                val units = parts[2]
                val unitsFirst = units.substring(0, 1)

                var totalRam = value.toDouble()

                if ("T".equals(unitsFirst, ignoreCase = true)) {
                    totalRam *= TB.toDouble()
                } else if ("G".equals(unitsFirst, ignoreCase = true)) {
                    totalRam *= GB.toDouble()
                } else if ("M".equals(unitsFirst, ignoreCase = true)) {
                    totalRam *= MB.toDouble()
                } else if ("K".equals(unitsFirst, ignoreCase = true)) {
                    totalRam *= KB.toDouble()
                }
                lastValue = totalRam.toLong()
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }

        return lastValue
    }

    private fun shareArchive(file: File) {
        if (!file.exists()) return

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            setDataAndType(uri, "application/zip")

            putExtra(Intent.EXTRA_STREAM, uri)

            clipData = ClipData.newRawUri(null, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val sendTo = runBlocking { getString(Res.string.send_to) }

        context.startActivity(
            Intent.createChooser(intent, sendTo),
        )
    }
}

actual fun getGithubReporter(
    repoUrl: String,
    oAuthToken: String
) = GithubReporter(
    repositoryUrl = repoUrl,
    githubOauth2Token = oAuthToken,
    context = FileKit.context
)

actual fun textClipEntry(text: String, label: String?): ClipEntry =
    ClipEntry(ClipData.newPlainText(label ?: "text", text))
actual fun ClipEntry.textOrNull(): String? {
    val data = clipData
    if (data.itemCount == 0) return null
    return data.getItemAt(0).text?.toString()
}
actual val ClipEntry.label: String?
    get() = clipData.description.label?.toString()

// Android doesn't filter by unrecognized mime types (.usfm, .tstudio)
// That's why we need to allow all extensions
actual fun getSupportedUsfmExtensions(): FileKitType.File =
    FileKitType.File()

actual fun getSupportedTstudioExtensions(): FileKitType.File =
    FileKitType.File()

actual val PlatformFile.displayName: String
    get() {
        val uri = toAndroidUri()
        val context = FileKit.context

        return when (uri.scheme) {
            "file" -> uri.lastPathSegment.orEmpty()
            "content" -> {
                context.contentResolver
                    .query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                    ?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                            if (index >= 0) cursor.getString(index) else null
                        } else null
                    } ?: name
            }
            else -> name
        }
    }