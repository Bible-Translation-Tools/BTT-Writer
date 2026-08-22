package org.bibletranslationtools.writer.unit.core

import androidx.compose.ui.text.font.FontFamily
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.bibletranslationtools.writer.core.SystemFont
import org.bibletranslationtools.writer.core.SystemFontProvider
import org.bibletranslationtools.writer.core.Typography
import org.bibletranslationtools.writer.data.Preference
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TypographyTest {

    private val preference: Preference = mockk(relaxed = true)

    private fun typography(
        systemFonts: List<SystemFont> = emptyList(),
        loaded: FontFamily? = null
    ): Pair<Typography, SystemFontProvider> {
        val provider: SystemFontProvider = mockk(relaxed = true)
        every { provider.listSystemFonts() } returns systemFonts
        every { provider.loadFontFamily(any()) } returns loaded
        return Typography(preference, provider) to provider
    }

    @Test
    fun getSystemFonts_delegatesToProvider() {
        val fonts = listOf(SystemFont("Arial", "/system/fonts/Arial.ttf"))
        val (typography, _) = typography(systemFonts = fonts)

        assertEquals(fonts, typography.getSystemFonts())
    }

    @Test
    fun getFontNames_appendsSystemFontPathsAfterBundled() {
        val (typography, _) = typography(
            systemFonts = listOf(SystemFont("Arial", "/system/fonts/Arial.ttf"))
        )

        val names = typography.getFontNames()

        assertTrue("andika.ttf" in names, "bundled fonts retained")
        assertTrue("/system/fonts/Arial.ttf" in names, "system font path appended")
        assertTrue(
            names.indexOf("/system/fonts/Arial.ttf") > names.indexOf("andika.ttf"),
            "system fonts come after bundled"
        )
    }

    @Test
    fun isBundledFont_trueForBundledKey_falseForSystemPath() {
        val (typography, _) = typography()

        assertTrue(typography.isBundledFont("andika.ttf"))
        assertFalse(typography.isBundledFont("/system/fonts/Arial.ttf"))
    }

    @Test
    fun resolveSystemFontFamily_delegatesToProvider() {
        val (typography, provider) = typography(loaded = FontFamily.Default)

        val result = typography.resolveSystemFontFamily("/system/fonts/Arial.ttf")

        assertEquals(FontFamily.Default, result)
        verify { provider.loadFontFamily("/system/fonts/Arial.ttf") }
    }
}
