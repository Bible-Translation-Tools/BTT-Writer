package org.bibletranslationtools.writer.unit.usecases

import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.bibletranslationtools.writer.core.TargetTranslation
import org.bibletranslationtools.writer.core.Translator
import org.bibletranslationtools.writer.data.Preference
import org.bibletranslationtools.writer.usecases.BackupRC
import org.bibletranslationtools.writer.usecases.MergeTargetTranslation
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

class MergeTargetTranslationTest {

    @MockK private lateinit var translator: Translator
    @MockK private lateinit var backupRC: BackupRC
    @MockK private lateinit var preference: Preference

    @Before
    fun setup() {
        MockKAnnotations.init(this)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `test merge target translation successfully`() = runTest {
        val sourceFile: File = mockk()
        val sourceTranslation: TargetTranslation = mockk {
            every { path }.returns(sourceFile)
        }
        val destinationTranslation: TargetTranslation = mockk {
            coEvery { merge(any(), any()) }.returns(true)
        }

        val result = MergeTargetTranslation(
            translator,
            backupRC,
            preference
        ).execute(destinationTranslation, sourceTranslation, false)

        assertTrue(result.success)
        assertEquals(MergeTargetTranslation.Status.SUCCESS, result.status)
        assertEquals(destinationTranslation, result.destinationTranslation)
        assertEquals(sourceTranslation, result.sourceTranslation)

        verify { sourceTranslation.path }
        coVerify { destinationTranslation.merge(sourceFile, any()) }
    }

    @Test
    fun `test merge target translation with conflict`() = runTest {
        val sourceFile: File = mockk()
        val sourceTranslation: TargetTranslation = mockk {
            every { path }.returns(sourceFile)
        }
        val destinationTranslation: TargetTranslation = mockk {
            coEvery { merge(any(), any()) }.returns(false)
        }

        val result = MergeTargetTranslation(
            translator,
            backupRC,
            preference
        ).execute(destinationTranslation, sourceTranslation, false)

        assertTrue(result.success)
        assertEquals(MergeTargetTranslation.Status.MERGE_CONFLICTS, result.status)
        assertEquals(destinationTranslation, result.destinationTranslation)
        assertEquals(sourceTranslation, result.sourceTranslation)

        verify { sourceTranslation.path }
        coVerify { destinationTranslation.merge(sourceFile, any()) }
    }

    @Test
    fun `test merge target translation deleting source`() = runTest {
        val sourceFile: File = mockk()
        val sourceTranslation: TargetTranslation = mockk {
            every { id }.returns("aa_mrk_text_ulb")
            every { path }.returns(sourceFile)
        }
        val destinationTranslation: TargetTranslation = mockk {
            coEvery { merge(any(), any()) }.returns(true)
        }

        every { translator.deleteTargetTranslation(any<String>()) }.just(runs)
        every { preference.clearTargetTranslationSettings(any()) }.just(runs)

        val result = MergeTargetTranslation(
            translator,
            backupRC,
            preference
        ).execute(destinationTranslation, sourceTranslation, true)

        assertTrue(result.success)
        assertEquals(MergeTargetTranslation.Status.SUCCESS, result.status)
        assertEquals(destinationTranslation, result.destinationTranslation)
        assertEquals(sourceTranslation, result.sourceTranslation)

        verify { sourceTranslation.id }
        verify { sourceTranslation.path }
        coVerify { destinationTranslation.merge(sourceFile, any()) }
        verify { translator.deleteTargetTranslation(any<String>()) }
        verify { preference.clearTargetTranslationSettings(any()) }
    }

    @Test
    fun `test merge target translation with merge exception`() = runTest {
        val sourceFile: File = mockk()
        val sourceTranslation: TargetTranslation = mockk {
            every { path }.returns(sourceFile)
        }
        val destinationTranslation: TargetTranslation = mockk {
            coEvery { merge(any(), any()) }.throws(Exception("Merge error."))
        }

        val result = MergeTargetTranslation(
            translator,
            backupRC,
            preference
        ).execute(destinationTranslation, sourceTranslation, true)

        assertFalse(result.success)
        assertEquals(MergeTargetTranslation.Status.MERGE_ERROR, result.status)
        assertEquals(destinationTranslation, result.destinationTranslation)
        assertEquals(sourceTranslation, result.sourceTranslation)

        // Delete source should not happen if merge fails
        verify { sourceTranslation.path }
        coVerify { destinationTranslation.merge(sourceFile, any()) }
        verify(exactly = 0) { sourceTranslation.id }
        verify(exactly = 0) { translator.deleteTargetTranslation(any<String>()) }
        verify(exactly = 0) { preference.clearTargetTranslationSettings(any()) }
    }
}