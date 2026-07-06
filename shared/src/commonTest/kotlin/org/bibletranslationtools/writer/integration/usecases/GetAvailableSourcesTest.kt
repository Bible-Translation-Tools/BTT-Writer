package org.bibletranslationtools.writer.integration.usecases

import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import org.bibletranslationtools.writer.BaseIntegrationTest
import org.bibletranslationtools.writer.usecases.GetAvailableSources
import org.junit.Test
import org.koin.core.component.inject

class GetAvailableSourcesTest : BaseIntegrationTest() {

    private val getAvailableSources: GetAvailableSources by inject()

    override val needsLibrary = true

    @Test
    fun testAvailableResources() {
        var progressMessage: String? = null
        val onProgress: (Float, String?) -> Unit = { _, message ->
            progressMessage = message
        }

        val result = getAvailableSources.execute(onProgress)

        assertEquals("Prefix message should be equal to progress message", null, progressMessage)

        // Test that some gateway languages exist
        assertTrue("English should be available", result.byLanguage.containsKey("en"))
        assertTrue("Spanish should be available", result.byLanguage.containsKey("es-419"))
        assertTrue("French should be available", result.byLanguage.containsKey("fr"))

        assertTrue("English resources should be available", !result.byLanguage["en"].isNullOrEmpty())
        assertTrue("Spanish resources should be available", !result.byLanguage["es-419"].isNullOrEmpty())
        assertTrue("French resources should be available", !result.byLanguage["fr"].isNullOrEmpty())

        // Test that some books exist
        assertTrue("Genesis should be available", result.otBooks.containsKey("gen"))
        assertTrue("Malachi should be available", result.otBooks.containsKey("mal"))
        assertTrue("Matthew should be available", result.ntBooks.containsKey("mat"))
        assertTrue("Revelation should be available", result.ntBooks.containsKey("rev"))

        assertTrue("Genesis resources should be available", !result.otBooks["gen"].isNullOrEmpty())
        assertTrue("Malachi resources should be available", !result.otBooks["mal"].isNullOrEmpty())
        assertTrue("Matthew resources should be available", !result.ntBooks["mat"].isNullOrEmpty())
        assertTrue("Revelation resources should be available", !result.ntBooks["rev"].isNullOrEmpty())

        assertTrue("tW (bible) resources should be available", result.otherBooks.containsKey("bible"))
        assertTrue("tW (bible) resources should be available", !result.otherBooks["bible"].isNullOrEmpty())

        assertEquals("There should be 39 OT books", 39, result.otBooks.size)
        assertEquals("There should be 27 NT books", 27, result.ntBooks.size)
    }
}
