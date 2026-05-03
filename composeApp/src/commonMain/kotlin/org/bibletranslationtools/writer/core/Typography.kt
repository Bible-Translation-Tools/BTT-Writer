package org.bibletranslationtools.writer.core

import btt_writer.composeapp.generated.resources.Res
import btt_writer.composeapp.generated.resources.abyssinica_sil_burji_dpwa_regular
import btt_writer.composeapp.generated.resources.abyssinica_sil_r
import btt_writer.composeapp.generated.resources.adinatha_tamil_brahmi
import btt_writer.composeapp.generated.resources.andika
import btt_writer.composeapp.generated.resources.annapurna_sil_r
import btt_writer.composeapp.generated.resources.charis_sil_r
import btt_writer.composeapp.generated.resources.chiangsaenalif_v1_00
import btt_writer.composeapp.generated.resources.dbsillr
import btt_writer.composeapp.generated.resources.doulos_sil_cipher_r
import btt_writer.composeapp.generated.resources.doulos_sil_r
import btt_writer.composeapp.generated.resources.free_mono_tengwar_embedding
import btt_writer.composeapp.generated.resources.gentium_plus
import btt_writer.composeapp.generated.resources.lannaalif_v1_03
import btt_writer.composeapp.generated.resources.lannauni_gr
import btt_writer.composeapp.generated.resources.lin_biolinum_r_g
import btt_writer.composeapp.generated.resources.lin_libertine_r_g
import btt_writer.composeapp.generated.resources.miao_unicode_regular
import btt_writer.composeapp.generated.resources.noto_nastaliq_urdu_regular
import btt_writer.composeapp.generated.resources.noto_sans_multi_language_regular
import btt_writer.composeapp.generated.resources.padauk
import btt_writer.composeapp.generated.resources.pref_default_translation_typeface
import btt_writer.composeapp.generated.resources.pref_default_typeface_size
import btt_writer.composeapp.generated.resources.scheherazade_r
import btt_writer.composeapp.generated.resources.snr
import btt_writer.composeapp.generated.resources.tai_heritage_pro_r
import btt_writer.composeapp.generated.resources.tuladha_jejeg_gr
import org.bibletranslationtools.writer.data.Preference
import org.bibletranslationtools.writer.data.getPref
import org.jetbrains.compose.resources.FontResource
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

    private val defaultFont: FontResource = Res.font.noto_sans_multi_language_regular

    private val fontResources: Map<String, FontResource> = mapOf(
        "abyssinicasil-r.ttf"                  to Res.font.abyssinica_sil_r,
        "abyssinicasilburji-dpwa-regular.otf"  to Res.font.abyssinica_sil_burji_dpwa_regular,
        "adinatha-tamil-brahmi.otf"            to Res.font.adinatha_tamil_brahmi,
        "andika.ttf"                           to Res.font.andika,
        "annapurnasil-r.ttf"                   to Res.font.annapurna_sil_r,
        "charissil-r.ttf"                      to Res.font.charis_sil_r,
        "chiangsaenalif-v1-00.ttf"             to Res.font.chiangsaenalif_v1_00,
        "dbsillr.ttf"                          to Res.font.dbsillr,
        "doulossil-r.ttf"                      to Res.font.doulos_sil_r,
        "doulossilcipher-r.ttf"                to Res.font.doulos_sil_cipher_r,
        "freemonotengwar-embedding.ttf"        to Res.font.free_mono_tengwar_embedding,
        "gentiumplus.ttf"                      to Res.font.gentium_plus,
        "lannaalif-v1-03.ttf"                  to Res.font.lannaalif_v1_03,
        "lannauni_gr.otf"                      to Res.font.lannauni_gr,
        "linbiolinum_r_g.ttf"                  to Res.font.lin_biolinum_r_g,
        "linlibertine_r_g.ttf"                 to Res.font.lin_libertine_r_g,
        "miaounicode-regular.ttf"              to Res.font.miao_unicode_regular,
        "notonastaliqurdu-regular.ttf"         to Res.font.noto_nastaliq_urdu_regular,
        "notosansmultilanguage-regular.ttf"    to Res.font.noto_sans_multi_language_regular,
        "padauk.ttf"                           to Res.font.padauk,
        "scheherazade-r.ttf"                   to Res.font.scheherazade_r,
        "snr.ttf"                              to Res.font.snr,
        "taiheritagepro-r.ttf"                 to Res.font.tai_heritage_pro_r,
        "tuladhajejeg_gr.ttf"                  to Res.font.tuladha_jejeg_gr,
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

    fun getFontNames(): List<String> {
        return fontResources.keys.toList()
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

    /**
     * Maps a font filename (as stored in user preferences or the substitute-font map)
     * to the corresponding Compose Resource. The lookup is case-insensitive and accepts
     * either a bare filename ("AbyssinicaSIL-R.ttf") or a path ("font/AbyssinicaSIL-R.ttf").
     *
     * Falls back to [defaultFont] if the filename isn't recognized.
     */
    internal fun resolveFontResource(fontFileName: String): FontResource {
        val key = fontFileName.substringAfterLast('/')   // strip "fonts/" prefix if present
            .lowercase()
        return fontResources[key] ?: defaultFont
    }
}

data class TextFormatConfig(
    val fontAssetPath: String,
    val fontSizeSp: Float,
    val isBold: Boolean = false,
    val directionString: String? = null
) {
    val isRtl: Boolean get() = directionString.equals("rtl", ignoreCase = true)
}
