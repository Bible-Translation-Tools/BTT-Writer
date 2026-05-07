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
import org.bibletranslationtools.writer.usecases.MergeTargetTranslation
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.koin.core.component.inject
import kotlin.test.DefaultAsserter.assertTrue


class MergeTargetTranslationTest : BaseIntegrationTest() {

    private val catalogClient: ResourceCatalogClient by inject()
    private val profile: Profile by inject()
    private val importProjects: ImportProjects by inject()
    private val translator: Translator by inject()
    private val mergeTargetTranslation: MergeTargetTranslation by inject()
    private val platform: Platform by inject()

    private var sourceTranslation: TargetTranslation? = null
    private var destinationTranslation: TargetTranslation? = null

    override val needsLibrary = true

    @Before
    fun setUp() {
        runBlocking { setupTranslations() }
    }

    @After
    fun tearDown() {
        runBlocking { directoryProvider.clearCache() }
    }

    @Test
    fun testMergeTargetTranslationWithDeletion() = runTest {
        val result = mergeTargetTranslation.execute(
            destinationTranslation!!,
            sourceTranslation!!,
            true
        )

        assertTrue("Merge should have succeeded", result.success)
        assertEquals("Status should be MERGE_CONFLICTS", MergeTargetTranslation.Status.MERGE_CONFLICTS, result.status)
        assertEquals(
            "Source translation should match result",
            sourceTranslation,
            result.sourceTranslation
        )
        assertEquals(
            "Destination translation should match result",
            destinationTranslation,
            result.destinationTranslation
        )

        assertTrue(
            "Destination translation should exist",
            translator.getTargetTranslations().map { it.id }.contains(destinationTranslation!!.id)
        )

        assertFalse(
            "Source translation should not exist",
            translator.getTargetTranslations().map { it.id }.contains(sourceTranslation!!.id)
        )
    }

    @Test
    fun testMergeTargetTranslationWithoutDeletion() = runTest {
        val result = mergeTargetTranslation.execute(
            destinationTranslation!!,
            sourceTranslation!!,
            false
        )

        assertTrue("Merge should have succeeded", result.success)
        assertEquals("Status should be MERGE_CONFLICTS", MergeTargetTranslation.Status.MERGE_CONFLICTS, result.status)
        assertEquals(
            "Source translation should match result",
            sourceTranslation,
            result.sourceTranslation
        )
        assertEquals(
            "Destination translation should match result",
            destinationTranslation,
            result.destinationTranslation
        )

        assertTrue(
            "Destination translation should exist",
            translator.getTargetTranslations().map { it.id }.contains(destinationTranslation!!.id)
        )

        assertTrue(
            "Source translation should exist",
            translator.getTargetTranslations().map { it.id }.contains(sourceTranslation!!.id)
        )
    }

    private suspend fun setupTranslations() {
        sourceTranslation = TestUtils.importTargetTranslation(
            catalogClient,
            platform,
            directoryProvider,
            profile,
            importProjects,
            translator,
            "aa",
            "usfm/mrk.usfm"
        )

        assertNotNull("Source translation is null", sourceTranslation)

        destinationTranslation = TestUtils.importTargetTranslation(
            catalogClient,
            platform,
            directoryProvider,
            profile,
            importProjects,
            translator,
            "aae",
            "usfm/mrk.usfm"
        )

        assertNotNull("Destination translation is null", destinationTranslation)
    }
}