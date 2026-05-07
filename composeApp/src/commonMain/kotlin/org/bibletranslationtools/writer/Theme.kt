package org.bibletranslationtools.writer

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import btt_writer.composeapp.generated.resources.Res
import btt_writer.composeapp.generated.resources.pref_default_color_theme
import org.bibletranslationtools.writer.data.Preference
import org.bibletranslationtools.writer.data.getPrefFlow
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

val PrimaryFixed = Color(0xFF003389)
val TertiaryFixed = Color(0xFF00A56C)

val BackgroundLight = Color(0xFFE0E0E0)
val SurfaceLight = Color(0xFFE9E9E9)
val PrimaryLight = Color(0xFF0250D3)
val TertiaryLight = Color(0xFF00BAFF)
val OnTertiaryLight = Color(0xFF272727)
val TertiaryContainerLight = Color(0xFFE2F0FF)
val ErrorContainerLight = Color(0xFFFF9800)
val OnErrorContainerLight = Color(0xFFE9E9E9)

val BackgroundDark = Color(0xFF1C1C1C)
val SurfaceDark = Color(0xFF272727)
val PrimaryDark = Color(0xFF6A91D3)
val TertiaryDark = Color(0xFF51AFC7)
val OnTertiaryDark = Color(0xFF272727)
val TertiaryContainerDark = Color(0xFF92B4CE)
val ErrorContainerDark = Color(0xFFC49C54)
val OnErrorContainerDark = Color(0xFFE9E9E9)

val AppTypography = Typography(
    titleLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    ),
    titleSmall = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
    ),

    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),

    labelLarge = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)

@Composable
fun AppTheme(
    content: @Composable () -> Unit
) {
    val preference: Preference = koinInject()

    val initialTheme = stringResource(Res.string.pref_default_color_theme)
    val currentTheme by preference.getPrefFlow(
        Preference.KEY_PREF_COLOR_THEME,
        initialTheme
    ).collectAsStateWithLifecycle(initialTheme)

    val isDark = when (Preference.Theme.of(currentTheme)) {
        Preference.Theme.LIGHT -> false
        Preference.Theme.DARK -> true
        else -> isSystemInDarkTheme()
    }

    val colorScheme = when {
        isDark -> darkColorScheme(
            background = BackgroundDark,
            surface = SurfaceDark,
            primary = PrimaryDark,
            primaryFixed = PrimaryFixed,
            tertiary = TertiaryDark,
            tertiaryFixed = TertiaryFixed,
            onTertiary = OnTertiaryDark,
            tertiaryContainer = TertiaryContainerDark,
            errorContainer = ErrorContainerDark,
            onErrorContainer = OnErrorContainerDark
        )
        else -> lightColorScheme(
            background = BackgroundLight,
            surface = SurfaceLight,
            primary = PrimaryLight,
            primaryFixed = PrimaryFixed,
            tertiary = TertiaryLight,
            tertiaryFixed = TertiaryFixed,
            onTertiary = OnTertiaryLight,
            tertiaryContainer = TertiaryContainerLight,
            errorContainer = ErrorContainerLight,
            onErrorContainer = OnErrorContainerLight
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = Shapes(),
        content = content
    )
}