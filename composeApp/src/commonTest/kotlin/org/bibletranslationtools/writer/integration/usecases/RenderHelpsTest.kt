package org.bibletranslationtools.writer.integration.usecases

import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.test.runTest
import org.bibletranslationtools.resourcecatalog.ResourceCatalogClient
import org.bibletranslationtools.resourcecontainer.Link
import org.bibletranslationtools.writer.BaseIntegrationTest
import org.bibletranslationtools.writer.Platform
import org.bibletranslationtools.writer.TestUtils
import org.bibletranslationtools.writer.core.Chunk
import org.bibletranslationtools.writer.core.Profile
import org.bibletranslationtools.writer.core.TargetTranslation
import org.bibletranslationtools.writer.core.TranslationHelp
import org.bibletranslationtools.writer.core.Translator
import org.bibletranslationtools.writer.usecases.ImportProjects
import org.bibletranslationtools.writer.usecases.RenderHelps
import org.junit.Test
import org.koin.core.component.inject

class RenderHelpsTest : BaseIntegrationTest() {

    private val importProjects: ImportProjects by inject()
    private val catalogClient: ResourceCatalogClient by inject()
    private val translator: Translator by inject()
    private val profile: Profile by inject()
    private val renderHelps: RenderHelps by inject()
    private val platform: Platform by inject()

    override val needsLibrary = true

    @Test
    fun testRenderHelps() = runTest {
        val targetTranslation = importTargetTranslation("aa")

        assertNotNull("Target translation should not be null", targetTranslation)

        val rc = catalogClient.openResourceContainer("en_mrk_ulb")

        val chunk = Chunk(
            "01",
            "01",
            rc,
            targetTranslation!!,
        )

        val result = renderHelps.execute(chunk)

        assertTrue("Helps should not be empty", result.isNotEmpty())
        assertEquals("There should be 3 helps", 3, result.size)
        assertTrue("There should be a notes help", result.containsKey("notes"))
        assertEquals("There should be 10 notes", 10, (result["notes"] as List<*>).size)
        assertTrue("There should be a questions help", result.containsKey("questions"))
        assertEquals("There should be 17 questions", 17, (result["questions"] as List<*>).size)
        assertTrue("There should be a words help", result.containsKey("words"))
        assertEquals("There should be 9 words", 9, (result["words"] as List<*>).size)

        val note = (result["notes"] as List<*>).firstOrNull {
            (it as TranslationHelp).title == "Son of God"
        }
        assertNotNull(note)
        assertTrue((note!! as TranslationHelp).body.contains("Jesus"))

        val question = (result["questions"] as List<*>).firstOrNull {
            (it as TranslationHelp).title.contains("in the wilderness", ignoreCase = true)
        }
        assertNotNull(question)
        assertTrue((question!! as TranslationHelp).body.contains("tempted by Satan", ignoreCase = true))

        val word = (result["words"] as List<*>).firstOrNull {
            (it as Link).chapter == "goodnews"
        }
        assertNotNull(word)
        assertTrue((word as? Link)?.title?.contains("good news", ignoreCase = true) == true)
    }

    private suspend fun importTargetTranslation(lang: String): TargetTranslation? {
        return TestUtils.importTargetTranslation(
            catalogClient,
            platform,
            directoryProvider,
            profile,
            importProjects,
            translator,
            lang,
            "usfm/mrk.usfm"
        )
    }
}
