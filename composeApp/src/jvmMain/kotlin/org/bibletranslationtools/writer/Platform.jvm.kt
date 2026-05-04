package org.bibletranslationtools.writer

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.ClipEntry
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.name
import org.bibletranslationtools.logger.Context
import org.bibletranslationtools.logger.GithubReporter
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection
import java.awt.datatransfer.Transferable
import java.io.File

class DesktopPlatform(
    private val directoryProvider: DirectoryProvider
) : Platform {

    override val info: AppInfo
        get() = AppInfo(
            versionName = AppConfig.versionName,
            versionCode = AppConfig.versionCode,
            model = "Build.MODEL",
            device = "Build.DEVICE",
            manufacturer = "Build.MANUFACTURER"
        )

    override val isStoreVersion = false

    override val isNetworkAvailable: Boolean
        get() = TODO("Not yet implemented")

    override fun restart() {
        TODO("Not yet implemented")
    }

    override fun exit() {
        TODO("Not yet implemented")
    }

    override fun shareApp() {
        TODO("Not yet implemented")
    }

    override fun shareProject(file: File) {
        TODO("Not yet implemented")
    }

    override fun calculateSystemResources(): String {
        TODO("Not yet implemented")
    }

    override fun getTotalRam(): Long {
        TODO("Not yet implemented")
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