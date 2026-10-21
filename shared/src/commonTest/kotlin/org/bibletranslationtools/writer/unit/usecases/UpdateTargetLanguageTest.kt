package org.bibletranslationtools.writer.unit.usecases

import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.bibletranslationtools.resourcecatalog.ResourceCatalogClient
import org.bibletranslationtools.resourcecatalog.library.Index
import org.bibletranslationtools.resourcecatalog.library.models.TargetLanguage
import org.bibletranslationtools.writer.core.TargetTranslation
import org.bibletranslationtools.writer.core.Translator
import org.bibletranslationtools.writer.usecases.UpdateTargetLanguage
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class UpdateTargetLanguageTest {

    @MockK private lateinit var catalogClient: ResourceCatalogClient
    @MockK private lateinit var index: Index
    @MockK private lateinit var translator: Translator

    private val language = TargetLanguage("aa", "Afar", "rtl", "Afar", "Africa", false)

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        every { catalogClient.library } returns index
        every { index.addTargetLanguage(any()) } returns true
        every { index.addTempTargetLanguage(any()) } returns true
        every { index.isTempTargetLanguage(any()) } returns false
    }

    private fun project(languageSlug: String): TargetTranslation = mockk {
        every { id } returns "${languageSlug}_mrk"
        every { targetLanguageId } returns languageSlug
        every { changeTargetLanguage(any()) } just runs
    }

    @Test
    fun updatesRegularLanguageAndOnlyMatchingProjects() = runTest {
        val matching = project("aa")
        val other = project("bb")
        coEvery { translator.getTargetTranslations() } returns listOf(matching, other)

        assertTrue(UpdateTargetLanguage(catalogClient, translator).execute(language))

        verify { index.addTargetLanguage(language) }
        verify(exactly = 0) { index.addTempTargetLanguage(any()) }
        verify { matching.changeTargetLanguage(language) }
        verify(exactly = 0) { other.changeTargetLanguage(any()) }
    }

    @Test
    fun updatesTempLanguageInTempTable() = runTest {
        every { index.isTempTargetLanguage("aa") } returns true
        coEvery { translator.getTargetTranslations() } returns emptyList()

        assertTrue(UpdateTargetLanguage(catalogClient, translator).execute(language))

        verify { index.addTempTargetLanguage(language) }
        verify(exactly = 0) { index.addTargetLanguage(any()) }
    }

    @Test
    fun brokenProjectDoesNotBlockOthers() = runTest {
        val broken = project("aa")
        every { broken.changeTargetLanguage(any()) } throws RuntimeException("disk")
        val healthy = project("aa")
        coEvery { translator.getTargetTranslations() } returns listOf(broken, healthy)

        assertTrue(UpdateTargetLanguage(catalogClient, translator).execute(language))

        verify { healthy.changeTargetLanguage(language) }
    }

    @Test
    fun failedLibraryWriteStopsBeforeProjects() = runTest {
        every { index.addTargetLanguage(any()) } returns false

        assertFalse(UpdateTargetLanguage(catalogClient, translator).execute(language))

        coVerify(exactly = 0) { translator.getTargetTranslations() }
    }
}
