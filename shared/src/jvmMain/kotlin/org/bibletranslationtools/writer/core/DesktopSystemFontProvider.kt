package org.bibletranslationtools.writer.core

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.platform.Font
import org.bibletranslationtools.logger.Logger
import org.bibletranslationtools.writer.DirectoryProvider
import java.io.File

/**
 * Lists fonts installed on the desktop OS by scanning the standard font
 * directories for Windows, macOS and Linux, plus user-imported fonts in
 * [DirectoryProvider.fontsDir].
 */
class DesktopSystemFontProvider(
    private val directoryProvider: DirectoryProvider
) : SystemFontProvider {

    override fun listSystemFonts(): List<SystemFont> {
        return runCatching {
            fontDirectories()
                .asSequence()
                .filter { it.isDirectory }
                .flatMap { it.walkTopDown().filter { f -> f.isFile } }
                .filter { it.extension.lowercase() in FONT_EXTENSIONS }
                .distinctBy { it.absolutePath }
                .map { SystemFont(displayName = displayName(it), path = it.absolutePath) }
                .sortedBy { it.displayName }
                .toList()
        }.getOrElse { e ->
            Logger.w(TAG, "Failed to list system fonts", e as? Exception)
            emptyList()
        }
    }

    override fun loadFontFamily(path: String): FontFamily? = runCatching {
        val file = File(path)
        if (!file.exists()) null else FontFamily(Font(file))
    }.getOrElse { e ->
        Logger.w(TAG, "Failed to load font $path", e as? Exception)
        null
    }

    override fun fontDisplayName(path: String): String? = runCatching {
        val file = File(path)
        if (!file.exists()) null else displayName(file)
    }.getOrNull()

    private fun fontDirectories(): List<File> {
        val osName = System.getProperty("os.name").orEmpty().lowercase()
        val home = System.getProperty("user.home").orEmpty()
        return when {
            "win" in osName -> listOf(
                File(System.getenv("WINDIR") ?: "C:\\Windows", "Fonts"),
                File(home, "AppData\\Local\\Microsoft\\Windows\\Fonts")
            )
            "mac" in osName || "darwin" in osName -> listOf(
                File("/System/Library/Fonts"),
                File("/Library/Fonts"),
                File(home, "Library/Fonts")
            )
            else -> listOf(
                File("/usr/share/fonts"),
                File("/usr/local/share/fonts"),
                File(home, ".fonts"),
                File(home, ".local/share/fonts")
            )
        } + directoryProvider.fontsDir
    }

    /** Real font name from the file's `name` table, falling back to a prettified filename. */
    private fun displayName(file: File): String =
        runCatching { FontNameReader.readDisplayName(file.readBytes()) }.getOrNull()
            ?: prettifyName(file.nameWithoutExtension)

    private fun prettifyName(raw: String): String =
        raw.replace('_', ' ').replace('-', ' ').trim()

    companion object {
        private const val TAG = "DesktopSystemFontProvider"
        private val FONT_EXTENSIONS = setOf("ttf", "otf")
    }
}
