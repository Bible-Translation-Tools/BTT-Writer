package org.bibletranslationtools.writer

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.ClipEntry
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.name
import org.bibletranslationtools.logger.Context
import org.bibletranslationtools.logger.GithubReporter
import org.bibletranslationtools.logger.Logger
import oshi.SystemInfo
import java.awt.GraphicsEnvironment
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection
import java.awt.datatransfer.Transferable
import java.io.File
import java.net.InetSocketAddress
import java.net.Socket
import kotlin.system.exitProcess

class DesktopPlatform : Platform {

    companion object {
        private const val TAG = "DesktopPlatform"
    }

    private val systemInfo = SystemInfo()
    private val hardware = systemInfo.hardware.computerSystem
    private val memory = systemInfo.hardware.memory

    override val info: AppInfo
        get() = AppInfo(
            versionName = BuildInfo.VERSION_NAME,
            versionCode = BuildInfo.VERSION_CODE.toInt(),
            model = hardware.model.takeIf { it.isNotBlank() } ?: "Unknown Model",
            device = "${System.getProperty("os.name")} (${System.getProperty("os.arch")})",
            manufacturer = hardware.manufacturer.takeIf {
                it.isNotBlank()
            } ?: "Unknown Manufacturer"
        )

    override val isStoreVersion = false

    override val isNetworkAvailable: Boolean
        get() {
            return try {
                Socket().use { socket ->
                    socket.connect(
                        InetSocketAddress("8.8.8.8", 53),
                        1500
                    )
                    true
                }
            } catch (_: Exception) {
                false
            }
        }

    override val isAndroid = false

    override fun exit() {
        exitProcess(0)
    }

    override fun shareApp() {
        TODO("Not yet implemented")
    }

    override fun shareProject(file: File) {
        TODO("Not yet implemented")
    }

    override fun calculateSystemResources(): String {
        var message = "System Resources:\n"

        val numProcessors = Runtime.getRuntime().availableProcessors()
        message += "Number of processor cores: $numProcessors " +
                "(${Platform.MINIMUM_NUMBER_OF_PROCESSORS} required)\n"

        val maxMem = Runtime.getRuntime().maxMemory()
        message += "JVM max memory: ${getFormattedSize(maxMem)} " +
                "(${getFormattedSize(Platform.MINIMUM_REQUIRED_RAM)} required)\n"

        message += "Available memory on the system: " +
                "${getFormattedSize(memory.available)}\n"
        message += "Total memory on the system (OSHI): " +
                "${getFormattedSize(memory.total)}\n"

        message += "Low memory threshold on the system: N/A (Desktop)\n"
        message += "Low memory state on the system: N/A (Desktop)\n"

        message += "Manufacturer: ${hardware.manufacturer.takeIf { it.isNotBlank() } ?: "Unknown"}\n"
        message += "Model: ${hardware.model.takeIf { it.isNotBlank() } ?: "Unknown"}\n"
        message += "Version: ${systemInfo.operatingSystem.family}\n"
        message += "Version Release: ${systemInfo.operatingSystem.versionInfo.version}\n"

        if (!GraphicsEnvironment.isHeadless()) {
            val toolkit = Toolkit.getDefaultToolkit()
            val screenSize = toolkit.screenSize
            val dpi = toolkit.screenResolution

            message += "\nScreen size ${screenSize.height}H*${screenSize.width}W"
            message += ", dpi: ${dpi}X*${dpi}Y"
        } else {
            message += "\nScreen size: Headless environment (No display)"
        }

        Logger.i(TAG, "system resources check:\n$message")

        return message
    }

    override fun getTotalRam(): Long {
        val gb = memory.total / (1024.0 * 1024.0 * 1024.0)
        return (kotlin.math.round(gb * 100) / 100).toLong()
    }
}

actual fun getGithubReporter(
    repoUrl: String,
    oAuthToken: String
): GithubReporter {
    val context = Context(versionName = "", udid = "")
    return GithubReporter(
        repositoryUrl = repoUrl,
        githubOauth2Token = oAuthToken,
        context = context
    )
}

@OptIn(ExperimentalComposeUiApi::class)
actual fun textClipEntry(text: String, label: String?): ClipEntry =
    ClipEntry(StringSelection(text))
@OptIn(ExperimentalComposeUiApi::class)
actual fun ClipEntry.textOrNull(): String? = runCatching {
    val transferable = nativeClipEntry as? Transferable ?: return@runCatching null
    if (transferable.isDataFlavorSupported(DataFlavor.stringFlavor)) {
        transferable.getTransferData(DataFlavor.stringFlavor) as? String
    } else null
}.getOrNull()

actual val ClipEntry.label: String?
    get() = null  // AWT doesn't have a label concept

actual fun getSupportedUsfmExtensions(): FileKitType.File =
    FileKitType.File(extensions = listOf("usfm", "txt", "zip"))

actual fun getSupportedTstudioExtensions(): FileKitType.File =
    FileKitType.File(extensions = listOf("tstudio", "zip"))

actual val PlatformFile.displayName: String
    get() = this.name