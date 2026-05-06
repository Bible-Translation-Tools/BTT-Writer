package org.bibletranslationtools.writer.integration.core

import org.bibletranslationtools.writer.rendering.spannables.USXNoteSpan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class USXNoteSpanTest {
    @Test
    fun testParseNote() {
        val usx = """
                <note caller="+" style="f">
                  <char style="ft">Leading text </char>
                  <char style="fqa">Quoted Text </char>trailing text 
                  <char style="fqa">More quoted text </char>more trailing text
                </note>
                """.trimIndent()
        val text =
            "Leading text \"Quoted Text\" trailing text \"More quoted text\" more trailing text"
        val span = USXNoteSpan.parseNote(usx)
        assertNotNull(span)
        assertEquals(text, span!!.notes)
    }
}
