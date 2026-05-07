package org.bibletranslationtools.writer.integration.usecases

import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.bibletranslationtools.resourcecatalog.ResourceCatalogClient
import org.bibletranslationtools.writer.BaseIntegrationTest
import org.bibletranslationtools.writer.Platform
import org.bibletranslationtools.writer.TestUtils
import org.bibletranslationtools.writer.core.Profile
import org.bibletranslationtools.writer.core.TargetTranslation
import org.bibletranslationtools.writer.core.Translator
import org.bibletranslationtools.writer.usecases.ImportProjects
import org.bibletranslationtools.writer.usecases.TranslationProgress
import org.junit.After
import org.junit.Test
import org.koin.core.component.inject


class TranslationProgressTest : BaseIntegrationTest() {

    private val importProjects: ImportProjects by inject()
    private val catalogClient: ResourceCatalogClient by inject()
    private val translator: Translator by inject()
    private val profile: Profile by inject()
    private val translationProgress: TranslationProgress by inject()
    private val platform: Platform by inject()

    override val needsLibrary = true

    @After
    fun tearDown() {
        runBlocking { directoryProvider.clearCache() }
    }

    @Test
    fun testTranslationProgress() = runTest {
        val targetTranslation = importTargetTranslation("aa")

        assertNotNull("Target translation should not be null", targetTranslation)

        val progress = translationProgress.execute(targetTranslation!!)

        assertEquals("No finished chunks", 0, targetTranslation.numFinished)
        assertEquals(
            "Progress should be 0 because no chunks are marked as done",
            0f,
            progress
        )

        val totalTranslated = targetTranslation.numTranslated
        targetTranslation.finishFrame("01", "01")

        val progress2 = translationProgress.execute(targetTranslation)

        assertEquals("Has one finished chunk", 1, targetTranslation.numFinished)

        val expectedProgress = 1 / totalTranslated.toFloat()

        assertEquals(286, totalTranslated)
        assertEquals(
            "Progress should be 0 because no chunks are marked as done",
            expectedProgress,
            progress2
        )

        targetTranslation.finishChapterTitle("01")
        targetTranslation.finishFrame("02", "03")
        targetTranslation.finishFrame("03", "01")
        targetTranslation.finishFrame("04", "06")

        // Also try to finish non-existent chunk, that should not add to finished chunks number
        targetTranslation.finishFrame("99", "99")

        val progress3 = translationProgress.execute(targetTranslation)

        assertEquals("Has five finished chunks", 5, targetTranslation.numFinished)

        val expectedProgress2 = 5 / totalTranslated.toFloat()

        assertEquals(286, totalTranslated)
        assertEquals(
            "Progress should be not be 0",
            expectedProgress2,
            progress3
        )
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
