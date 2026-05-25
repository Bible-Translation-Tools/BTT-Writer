package org.bibletranslationtools.writer.integration.usecases

import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.bibletranslationtools.resourcecatalog.ResourceCatalogClient
import org.bibletranslationtools.writer.BaseIntegrationTest
import org.bibletranslationtools.writer.core.Translator
import org.bibletranslationtools.writer.usecases.ImportDraft
import org.junit.After
import org.junit.Test
import org.koin.core.component.inject


class ImportDraftTest : BaseIntegrationTest() {

    private val importDraft: ImportDraft by inject()
    private val catalogClient: ResourceCatalogClient by inject()
    private val translator: Translator by inject()

    override val needsLibrary = true

    @After
    fun tearDown() {
        runBlocking { directoryProvider.clearCache() }
    }

    @Test
    fun testImportDraftTranslation() = runTest {
        val draftId = "en_gen_ulb"
        val draftTranslation = try {
            catalogClient.openResourceContainer(draftId)
        } catch (_: Exception) {
            null
        }

        assertNotNull("Draft translation should not be null", draftTranslation)
        assertEquals("Draft translation should be correct", draftId, draftTranslation!!.slug)
        assertEquals(
            "Draft language should be correct",
            "en",
            draftTranslation.language.slug
        )
        assertEquals(
            "Draft project should be correct",
            "gen",
            draftTranslation.project.slug
        )
        assertEquals(
            "Draft resource should be correct",
            "ulb",
            draftTranslation.resource.slug
        )

        val result = importDraft.execute(draftTranslation)

        assertNotNull("Target translation should not be null", result.targetTranslation)
        assertEquals(
            "Languages should be correct",
            draftTranslation.project.languageSlug,
            result.targetTranslation!!.targetLanguageId
        )
        assertEquals(
            "Projects should be correct",
            draftTranslation.project.slug,
            result.targetTranslation.projectId
        )
        assertEquals(
            "Target resource should be correct",
            "reg",
            result.targetTranslation.resourceSlug
        )
        assertEquals(
            "Target translation id should be correct",
            "en_gen_text_reg",
            result.targetTranslation.id
        )

        val targetTranslation = translator.getTargetTranslation(result.targetTranslation.id)

        assertEquals(
            "Target translation should be correctly opened",
            targetTranslation!!.id,
            result.targetTranslation.id
        )
    }
}