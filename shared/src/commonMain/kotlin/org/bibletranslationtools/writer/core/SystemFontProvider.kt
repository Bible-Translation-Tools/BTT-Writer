package org.bibletranslationtools.writer.core

import androidx.compose.ui.text.font.FontFamily

/**
 * A font installed on the host operating system.
 *
 * @param displayName human-readable name shown in the settings picker.
 * @param path absolute path to the font file; also the value persisted in preferences.
 */
data class SystemFont(
    val displayName: String,
    val path: String
)

/**
 * Discovers and loads fonts installed on the host operating system.
 *
 * Implemented per platform (Android scans [android.graphics.fonts.SystemFonts],
 * desktop scans the OS font directories) and provided through DI.
 */
interface SystemFontProvider {

    /**
     * Lists fonts installed on the system, sorted by [SystemFont.displayName].
     * Returns an empty list when scanning fails or no fonts are found.
     */
    fun listSystemFonts(): List<SystemFont>

    /**
     * Loads the font at [path] into a [FontFamily], or returns null when the
     * file is missing or cannot be parsed.
     */
    fun loadFontFamily(path: String): FontFamily?

    /**
     * Display name of the single font file at [path], without scanning the
     * system font directories. Returns null when the file is missing or
     * cannot be parsed.
     */
    fun fontDisplayName(path: String): String?
}
