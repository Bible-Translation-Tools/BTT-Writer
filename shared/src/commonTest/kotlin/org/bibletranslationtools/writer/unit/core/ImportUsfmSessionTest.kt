package org.bibletranslationtools.writer.unit.core

import org.bibletranslationtools.writer.core.ImportUsfmSession
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class ImportUsfmSessionTest {

    @Test
    fun `strips word entry to bare term`() {
        val result = ImportUsfmSession.stripWordMarkup(
            """the \w earth|strong="H776"\w* was"""
        )
        assertEquals("the earth was", result)
    }

    @Test
    fun `strips multiple attributes`() {
        val result = ImportUsfmSession.stripWordMarkup(
            """God \w created|strong="H1254" x-morph="strongMorph:TH8804"\w* them"""
        )
        assertEquals("God created them", result)
    }

    @Test
    fun `strips word entry without attributes`() {
        val result = ImportUsfmSession.stripWordMarkup(
            """a \w gracious \w* word"""
        )
        assertEquals("a gracious word", result)
    }

    @Test
    fun `removes attribute-only word entries`() {
        val result = ImportUsfmSession.stripWordMarkup(
            """they \w came|strong="H935" x-morph="strongMorph:TH8802"\w*\w |strong="H935" x-morph="strongMorph:TH8804"\w* to Egypt"""
        )
        assertEquals("they came to Egypt", result)
    }

    @Test
    fun `removes empty word entries`() {
        val result = ImportUsfmSession.stripWordMarkup(
            """they \w served|strong="H5647"\w*\w \w* the king"""
        )
        assertEquals("they served the king", result)
    }

    @Test
    fun `keeps punctuation and apostrophes attached`() {
        val result = ImportUsfmSession.stripWordMarkup(
            """de l'\w abîme|strong="H8415"\w*, et l'\w Esprit|strong="H7307"\w* de Dieu."""
        )
        assertEquals("de l'abîme, et l'Esprit de Dieu.", result)
    }

    @Test
    fun `keeps hyphenated words glued`() {
        val result = ImportUsfmSession.stripWordMarkup(
            """de soixante-\w dix|strong="H7657"\w*\w |strong="H5315"\w* en tout"""
        )
        assertEquals("de soixante-dix en tout", result)
    }

    @Test
    fun `preserves space between adjacent word entries`() {
        val result = ImportUsfmSession.stripWordMarkup(
            """\w Dieu|strong="H430"\w* \w créa|strong="H1254"\w* les cieux"""
        )
        assertEquals("Dieu créa les cieux", result)
    }

    @Test
    fun `strips nested word entries`() {
        val result = ImportUsfmSession.stripWordMarkup(
            """\f + \ft see \+w grace|strong="G5485"\+w* here\f*"""
        )
        assertEquals("""\f + \ft see grace here\f*""", result)
    }

    @Test
    fun `leaves other markers untouched`() {
        val text = "\\c 1\n\\p\n\\v 1 In the beginning \\w God|strong=\"H430\"\\w* created\n\\q2 a poetry line\n"
        val result = ImportUsfmSession.stripWordMarkup(text)
        assertEquals("\\c 1\n\\p\n\\v 1 In the beginning God created\n\\q2 a poetry line\n", result)
    }

    @Test
    fun `returns same instance when no word entries present`() {
        val text = "\\c 1\n\\v 1 In the beginning God created\n"
        assertSame(text, ImportUsfmSession.stripWordMarkup(text))
    }
}
