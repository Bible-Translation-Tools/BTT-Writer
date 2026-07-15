package org.bibletranslationtools.writer.core

import android.graphics.fonts.SystemFonts
import android.os.Build
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import org.bibletranslationtools.logger.Logger
import org.bibletranslationtools.writer.DirectoryProvider
import java.io.File

/**
 * Lists fonts installed on the Android system plus user-imported fonts in
 * [DirectoryProvider.fontsDir]. Uses [SystemFonts.getAvailableFonts] on API 29+,
 * falling back to scanning `/system/fonts` on older releases.
 */
class AndroidSystemFontProvider(
    private val directoryProvider: DirectoryProvider
) : SystemFontProvider {

    override fun listSystemFonts(): List<SystemFont> {
        return runCatching {
            val systemFiles = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                SystemFonts.getAvailableFonts().mapNotNull { it.file }
            } else {
                File("/system/fonts").listFiles()?.toList() ?: emptyList()
            }
            val importedFiles = directoryProvider.fontsDir.listFiles()?.toList() ?: emptyList()
            (systemFiles + importedFiles)
                .filter { it.extension.lowercase() in FONT_EXTENSIONS }
                .distinctBy { it.absolutePath }
                .map { SystemFont(displayName = displayName(it), path = it.absolutePath) }
                .sortedBy { it.displayName }
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

    /** Real font name from the file's `name` table, falling back to a prettified filename. */
    private fun displayName(file: File): String =
        runCatching { FontNameReader.readDisplayName(file.readBytes()) }.getOrNull()
            ?: prettifyName(file.nameWithoutExtension)

    private fun prettifyName(raw: String): String =
        raw.replace('_', ' ').replace('-', ' ').trim()

    companion object {
        private const val TAG = "AndroidSystemFontProvider"
        private val FONT_EXTENSIONS = setOf("ttf", "otf")
    }
}
