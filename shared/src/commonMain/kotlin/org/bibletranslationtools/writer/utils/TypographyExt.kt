@file:JvmName("TypographyUtils")
package org.bibletranslationtools.writer.utils

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.sp
import org.bibletranslationtools.writer.core.TextStyleType
import org.bibletranslationtools.writer.core.TranslationType
import org.bibletranslationtools.writer.core.Typography
import org.jetbrains.compose.resources.Font

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

    val fontFamily = if (isBundledFont(config.fontAssetPath)) {
        FontFamily(Font(resolveFontResource(config.fontAssetPath)))
    } else {
        remember(config.fontAssetPath) {
            resolveSystemFontFamily(config.fontAssetPath)
        } ?: FontFamily(Font(resolveFontResource(config.fontAssetPath)))
    }
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
