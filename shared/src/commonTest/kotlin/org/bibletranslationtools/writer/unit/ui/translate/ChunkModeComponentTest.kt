package org.bibletranslationtools.writer.unit.ui.translate

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.bibletranslationtools.resourcecontainer.Project
import org.bibletranslationtools.resourcecontainer.ResourceContainer
import org.bibletranslationtools.writer.core.ChapterTranslation
import org.bibletranslationtools.writer.core.FrameTranslation
import org.bibletranslationtools.writer.core.ProjectTranslation
import org.bibletranslationtools.writer.core.TargetTranslation
import org.bibletranslationtools.writer.core.TranslationFormat
import org.bibletranslationtools.writer.ui.translate.Footnote
import org.bibletranslationtools.writer.ui.translate.FootnoteAction
import org.bibletranslationtools.writer.ui.translate.TranslateComponent
import org.bibletranslationtools.writer.ui.translate.chunk.DefaultChunkModeComponent
import org.bibletranslationtools.writer.unit.ui.BaseComponentTest
import org.bibletranslationtools.writer.unit.ui.awaitState
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ChunkModeComponentTest : BaseComponentTest() {

    private val mockContainer = mockk<ResourceContainer>(relaxed = true)
    private val mockTarget = mockk<TargetTranslation>(relaxed = true)
    
    private val sharedState = MutableStateFlow(TranslateComponent.SharedState(resourceContainer = mockContainer))

    private val mockProject = mockk<Project>(relaxed = true) {
        every { name } returns "Genesis"
    }

    private val mockProjectTranslation = mockk<ProjectTranslation>(relaxed = true) {
        every { title } returns "Genesis Target"
        every { isTitleFinished } returns true
    }

    private val mockChapterTranslation = mockk<ChapterTranslation>(relaxed = true) {
        every { title } returns "Chapter 1"
        every { id } returns "01"
    }

    private val mockFrameTranslation = mockk<FrameTranslation>(relaxed = true) {
        every { id } returns "01"
        every { chapterId } returns "01"
        every { body } returns "Target Frame Body"
        every { format } returns TranslationFormat.USFM
    }

    @Before
    fun setUpMocks() {
        every { mockContainer.contentMimeType } returns "text/usfm"
        every { mockContainer.project } returns mockProject
        every { mockContainer.chapters() } returns listOf("01")
        every { mockContainer.chunks("01") } returns listOf("01")
        every { mockContainer.readChunk("01", "01") } returns "In the beginning"
        
        every { mockTarget.format } returns TranslationFormat.USFM
        every { mockTarget.projectTranslation } returns mockProjectTranslation
        every { mockTarget.getChapterTranslation("01") } returns mockChapterTranslation
        every { mockTarget.getFrameTranslation("01", "01", any()) } returns mockFrameTranslation
    }

    private fun createComponent(): DefaultChunkModeComponent = createComponent { context ->
        DefaultChunkModeComponent(
            componentContext = context,
            sharedState = sharedState,
            targetTranslation = mockTarget
        )
    }

    @Test
    fun testInitializationLoadsChunks() {
        runBlocking {
            val component = createComponent()
            component.items.awaitState { it.isNotEmpty() }

            assertEquals(1, component.items.value.size)
            val item = component.items.value.first()
            assertEquals("01-01", item.id)
            assertEquals("In the beginning", item.sourceText)
            assertEquals("Target Frame Body", item.targetText)
            assertTrue(item.sourceOnTop)
        }
    }

    @Test
    fun testOnCardsSwiped() {
        runBlocking {
            val component = createComponent()
            component.items.awaitState { it.isNotEmpty() }
            val item = component.items.value.first()

            component.onCardsSwiped(item, sourceOnTop = false)
            assertEquals(false, component.items.value.first().sourceOnTop)
        }
    }

    @Test
    fun testOpenAndClearFootnote() {
        val component = createComponent()
        val note = Footnote("Test note", "notes", "01-01", 0, 5, 0, FootnoteAction.VIEW)

        component.openFootnote(note)
        assertEquals(note, component.state.value.footnote)

        component.clearFootnote()
        assertNull(component.state.value.footnote)
    }

    @Test
    fun testOnItemTextChanged() {
        runBlocking {
            val component = createComponent()
            component.items.awaitState { it.isNotEmpty() }
            val item = component.items.value.first()

            component.onItemTextChanged(item, "New translation text")

            // Wait a brief moment for the coroutine to launch and save the translation
            kotlinx.coroutines.delay(100)

            verify { mockTarget.applyFrameTranslation(mockFrameTranslation, "New translation text") }
        }
    }

    @Test
    fun testReopenChunkFlow() {
        runBlocking {
            val component = createComponent()
            component.items.awaitState { it.isNotEmpty() }
            val item = component.items.value.first()

            component.reopenChunk(item)
            assertEquals(item, component.state.value.chunkToReopen)

            component.onReopenChunkConfirmed(true)
            
            // Wait for coroutine to finish
            kotlinx.coroutines.delay(100)
            
            verify { mockTarget.reopenFrame("01", "01") }
            assertNull(component.state.value.chunkToReopen)
        }
    }
}
