package org.bibletranslationtools.writer.core

import btt_writer.composeapp.generated.resources.Res
import btt_writer.composeapp.generated.resources.pref_default_translation_typeface
import btt_writer.composeapp.generated.resources.pref_default_typeface_size
import org.bibletranslationtools.writer.data.Preference
import org.bibletranslationtools.writer.data.getPref
import org.jetbrains.compose.resources.getString

enum class TextStyleType(val sizeMultiplier: Float) {
    NORMAL(1.0f),
    TITLE(1.3f),
    SUB(0.7f),
    TAB(0.5f)
}

/**
 * Created by mxaln on 2/25/2026.
 */
class Typography(private val preference: Preference) {

    private lateinit var defaultFontName: String
    private lateinit var defaultFontSize: String

    private val languageSubstituteFonts = mapOf(
        "default" to "noto_sans_multilanguage_regular",
        // "gu" to "noto_sans_gu_language_regular"
    )

    suspend fun init() {
        defaultFontName = getString(Res.string.pref_default_translation_typeface)
        defaultFontSize = getString(Res.string.pref_default_typeface_size)
    }

    fun getFormatConfig(
        translationType: TranslationType,
        style: TextStyleType = TextStyleType.NORMAL,
        languageCode: String? = null,
        direction: String? = null
    ): TextFormatConfig {
        val baseFontSize = getFontSize(translationType)
        val fontName = languageSubstituteFonts[languageCode] ?: getFontName(translationType)

        return TextFormatConfig(
            fontAssetPath = "fonts/$fontName",
            fontSizeSp = baseFontSize * style.sizeMultiplier,
            isBold = style == TextStyleType.TITLE,
            directionString = direction
        )
    }

    fun getAssetPath(translationType: TranslationType): String {
        return "font/${getFontName(translationType)}"
    }

    fun getFontSize(translationType: TranslationType): Float {
        val prefKey = if (translationType == TranslationType.SOURCE) {
            Preference.KEY_PREF_SOURCE_TYPEFACE_SIZE
        } else {
            Preference.KEY_PREF_TRANSLATION_TYPEFACE_SIZE
        }
        return preference.getPref(prefKey, defaultFontSize).toFloat()
    }

    private fun getFontName(translationType: TranslationType): String {
        val prefKey = if (translationType == TranslationType.SOURCE) {
            Preference.KEY_PREF_SOURCE_TYPEFACE
        } else {
            Preference.KEY_PREF_TRANSLATION_TYPEFACE
        }
        return preference.getPref(prefKey, defaultFontName)
    }

    fun getBestFontKeyForLanguage(languageCode: String?): String =
        languageSubstituteFonts[languageCode]
            ?: languageSubstituteFonts.getValue("default")

    fun getStyle(translationType: TranslationType): String =
        "<style type=\"text/css\">body { font-size: ${getFontSize(translationType)}; }</style>"

    private fun stripExtension(fileName: String): String =
        fileName.substringBeforeLast('.').lowercase().replace('-', '_')
}

data class TextFormatConfig(
    val fontAssetPath: String,
    val fontSizeSp: Float,
    val isBold: Boolean = false,
    val directionString: String? = null
) {
    val isRtl: Boolean get() = directionString.equals("rtl", ignoreCase = true)
}
