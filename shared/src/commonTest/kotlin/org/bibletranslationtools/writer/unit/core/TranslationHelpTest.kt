package org.bibletranslationtools.writer.unit.core

import org.bibletranslationtools.writer.core.TranslationHelp
import kotlin.test.Test
import kotlin.test.assertEquals

class TranslationHelpTest {

    @Test
    fun `toJson matches desktop JSON stringify format`() {
        val helps = listOf(
            TranslationHelp("Where?", "There."),
            TranslationHelp("t2", "b2")
        )
        // JSON.stringify(helps, null, '\t') output from the desktop app
        val expected = "[\n\t{\n\t\t\"title\": \"Where?\",\n\t\t\"body\": \"There.\"\n\t},\n\t{\n\t\t\"title\": \"t2\",\n\t\t\"body\": \"b2\"\n\t}\n]"
        assertEquals(expected, TranslationHelp.toJson(helps))
    }

    @Test
    fun `empty list produces empty string so frame file gets deleted`() {
        assertEquals("", TranslationHelp.toJson(emptyList()))
    }

    @Test
    fun `fromJson round trips`() {
        val helps = listOf(
            TranslationHelp("Question here", "Answer here"),
            TranslationHelp("", "")
        )
        assertEquals(helps, TranslationHelp.fromJson(TranslationHelp.toJson(helps)))
    }

    @Test
    fun `fromJson parses desktop written content`() {
        val desktopJson = "[\n\t{\n\t\t\"title\": \"Where?\",\n\t\t\"body\": \"There.\"\n\t}\n]"
        assertEquals(
            listOf(TranslationHelp("Where?", "There.")),
            TranslationHelp.fromJson(desktopJson)
        )
    }

    @Test
    fun `fromJson returns empty list for blank text`() {
        assertEquals(emptyList(), TranslationHelp.fromJson(""))
        assertEquals(emptyList(), TranslationHelp.fromJson("  \n"))
    }

    @Test
    fun `fromJson returns parse error sentinel for invalid json`() {
        assertEquals(
            listOf(TranslationHelp("Data Parsing Error", "Data Parsing Error")),
            TranslationHelp.fromJson("not json")
        )
    }
}
