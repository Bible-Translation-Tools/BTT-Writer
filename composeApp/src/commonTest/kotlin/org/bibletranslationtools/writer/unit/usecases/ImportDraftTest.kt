package org.bibletranslationtools.writer.unit.usecases

import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.bibletranslationtools.resourcecontainer.ResourceContainer
import org.bibletranslationtools.writer.core.Profile
import org.bibletranslationtools.writer.core.TargetTranslation
import org.bibletranslationtools.writer.core.Translator
import org.bibletranslationtools.writer.usecases.ImportDraft
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

class ImportDraftTest {

    @MockK private lateinit var translator: Translator
    @MockK private lateinit var profile: Profile
    @MockK private lateinit var draftTranslator: ResourceContainer

    private val onProgress = mockk<(Float, String?) -> Unit>(relaxed = true)

    @Before
    fun setup() {
        MockKAnnotations.init(this)

        every { onProgress(any(), any()) }.just(runs)
        every { profile.nativeSpeaker }.returns(mockk())
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `test import draft`() = runTest {
        val targetTranslation: TargetTranslation = mockk()
        coEvery { translator.importDraftTranslation(any(), any()) }
            .returns(targetTranslation)

        val result = ImportDraft(translator, profile)
            .execute(draftTranslator, onProgress)

        assertNotNull(result.targetTranslation)
        assertEquals(targetTranslation, result.targetTranslation)

        verify { onProgress(any(), any()) }
    }
}