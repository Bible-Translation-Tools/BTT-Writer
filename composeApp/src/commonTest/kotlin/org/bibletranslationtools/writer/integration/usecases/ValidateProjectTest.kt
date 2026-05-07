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
import org.bibletranslationtools.writer.core.Validation
import org.bibletranslationtools.writer.usecases.ImportProjects
import org.bibletranslationtools.writer.usecases.ValidateProject
import org.junit.After
import org.junit.Test
import org.koin.core.component.inject

class ValidateProjectTest : BaseIntegrationTest() {

    private val importProjects: ImportProjects by inject()
    private val catalogClient: ResourceCatalogClient by inject()
    private val translator: Translator by inject()
    private val profile: Profile by inject()
    private val validateProject: ValidateProject by inject()
    private val platform: Platform by inject()

    override val needsLibrary = true

    @After
    fun tearDown() {
        runBlocking { directoryProvider.clearCache() }
    }

    @Test
    fun testValidateProject() = runTest {
        val sourceTranslationId = "en_mrk_ulb"
        val targetTranslation = importTargetTranslation("aa")

        assertNotNull("Target translation should not be null", targetTranslation)

        val validate = validateProject.execute(targetTranslation!!.id, sourceTranslationId)
        val invalidItems = validate.filter {
            it is Validation.InvalidFrame || it is Validation.InvalidGroup
        }

        assertEquals("All items should be invalid", invalidItems.size, validate.size)

        // Mark some chunks as done
        targetTranslation.finishFrame("01", "01")
        targetTranslation.finishFrame("02", "01")
        targetTranslation.finishFrame("03", "01")

        val validate2 = validateProject.execute(targetTranslation.id, sourceTranslationId)
        val invalidItems2 = validate2.filter {
            it is Validation.InvalidFrame || it is Validation.InvalidGroup
        }
        val validItems = validate2.filter {
            it is Validation.ValidFrame || it is Validation.ValidGroup
        }

        assertEquals(
            "There should be ${invalidItems.size} errors",
            invalidItems.size - 3,
            invalidItems2.size
        )
        assertEquals(
            "There should be ${validItems.size} valid items",
            3,
            validItems.size
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
