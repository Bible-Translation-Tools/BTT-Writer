@file:JvmName("TypographyUtils")
package org.bibletranslationtools.writer.utils

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.sp
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
import btt_writer.composeapp.generated.resources.scheherazade_r
import btt_writer.composeapp.generated.resources.snr
import btt_writer.composeapp.generated.resources.tai_heritage_pro_r
import btt_writer.composeapp.generated.resources.tuladha_jejeg_gr
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.readBytes
import org.bibletranslationtools.writer.core.TextStyleType
import org.bibletranslationtools.writer.core.TranslationType
import org.bibletranslationtools.writer.core.Typography
import org.jetbrains.compose.resources.Font
import org.jetbrains.compose.resources.FontResource

@Composable
fun Typography.getComposeTextStyle(
    translationType: TranslationType,
    style: TextStyleType = TextStyleType.NORMAL,
    languageCode: String? = null,
    direction: String? = null,
    isCenterAligned: Boolean = false
): TextStyle {
    val config = remember(translationType, style, languageCode, direction) {
        getFormatConfig(translationType, style, languageCode, direction)
    }

    val fontFamily = FontFamily(Font(resolveFontResource(config.fontAssetPath)))
    val safeSize = if (config.fontSizeSp > 0f) config.fontSizeSp else 18f

    return TextStyle(
        fontFamily = fontFamily,
        fontSize = safeSize.sp,
        fontWeight = if (config.isBold) FontWeight.Bold else FontWeight.Normal,
        textDirection = if (config.isRtl) TextDirection.Rtl else TextDirection.Ltr,
        textAlign = if (isCenterAligned) TextAlign.Center else TextAlign.Start,
        lineHeight = (safeSize * 1.4f).sp,
        color = MaterialTheme.colorScheme.onSurface
    )
}

@Composable
private fun rememberFontFamily(absolutePath: String): FontFamily {
    var family by remember(absolutePath) {
        mutableStateOf<FontFamily>(FontFamily.Default)
    }

    LaunchedEffect(absolutePath) {
        runCatching {
            val bytes = PlatformFile(absolutePath).readBytes()
            family = FontFamily(Font(identity = absolutePath, data = bytes))
        }
    }
    return family
}

/**
 * Maps a font filename (as stored in user preferences or the substitute-font map)
 * to the corresponding Compose Resource. The lookup is case-insensitive and accepts
 * either a bare filename ("AbyssinicaSIL-R.ttf") or a path ("fonts/AbyssinicaSIL-R.ttf").
 *
 * Falls back to [defaultFont] if the filename isn't recognized.
 */
internal fun resolveFontResource(fontFileName: String): FontResource {
    val key = fontFileName.substringAfterLast('/')   // strip "fonts/" prefix if present
        .lowercase()
    return fontResources[key] ?: defaultFont
}

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