package org.bibletranslationtools.writer.unit.ui.translate

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.verify
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.bibletranslationtools.resourcecatalog.ResourceCatalogClient
import org.bibletranslationtools.resourcecontainer.Project
import org.bibletranslationtools.resourcecontainer.ResourceContainer
import org.bibletranslationtools.writer.core.ChapterTranslation
import org.bibletranslationtools.writer.core.ContainerCache
import org.bibletranslationtools.writer.core.FileHistory
import org.bibletranslationtools.writer.core.FrameTranslation
import org.bibletranslationtools.writer.core.ProjectTranslation
import org.bibletranslationtools.writer.core.TargetTranslation
import org.bibletranslationtools.writer.core.TranslationFormat
import org.bibletranslationtools.writer.data.Preference
import org.bibletranslationtools.writer.rendering.RenderingProvider
import org.bibletranslationtools.writer.rendering.model.NoteStyle
import org.bibletranslationtools.writer.rendering.model.RenderNode
import org.bibletranslationtools.writer.ui.translate.Footnote
import org.bibletranslationtools.writer.ui.translate.FootnoteAction
import org.bibletranslationtools.writer.ui.translate.TranslateComponent
import org.bibletranslationtools.writer.ui.translate.review.DefaultReviewModeComponent
import org.bibletranslationtools.writer.ui.translate.review.Help
import org.bibletranslationtools.writer.ui.translate.review.MarkAllDialogState
import org.bibletranslationtools.writer.ui.translate.review.SearchSubject
import org.bibletranslationtools.writer.ui.translate.review.TargetMode
import org.bibletranslationtools.writer.unit.ui.BaseComponentTest
import org.bibletranslationtools.writer.unit.ui.awaitState
import org.bibletranslationtools.writer.usecases.RenderHelps
import org.eclipse.jgit.revwalk.RevCommit
import org.junit.Before
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.dsl.module
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ReviewModeComponentTest : BaseComponentTest() {

    private val mockContainer = mockk<ResourceContainer>(relaxed = true)
    private val mockTarget = mockk<TargetTranslation>(relaxed = true)
    
    private val sharedState = MutableStateFlow(TranslateComponent.SharedState(resourceContainer = mockContainer))
    private val eventChannel = Channel<TranslateComponent.Event>(Channel.UNLIMITED)

    private val preference: Preference = mockk(relaxed = true)
    private val renderHelps: RenderHelps = mockk(relaxed = true)
    private val renderingProvider: RenderingProvider = mockk(relaxed = true)
    private val catalogClient: ResourceCatalogClient = mockk(relaxed = true)

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
        every { body } returns "Target Review Body"
        every { format } returns TranslationFormat.USFM
    }

    @Before
    fun setUpComponent() {
        every { mockContainer.contentMimeType } returns "text/usfm"
        every { mockContainer.project } returns mockProject
        every { mockContainer.chapters() } returns listOf("01")
        every { mockContainer.chunks("01") } returns listOf("01")
        every { mockContainer.readChunk("01", "01") } returns "In the beginning"
        
        every { mockTarget.format } returns TranslationFormat.USFM
        every { mockTarget.projectTranslation } returns mockProjectTranslation
        every { mockTarget.getChapterTranslation("01") } returns mockChapterTranslation
        every { mockTarget.getFrameTranslation("01", "01", any()) } returns mockFrameTranslation
        // Stub finishFrame to return true so chunk.close() succeeds in markChunkCompleted
        every { mockTarget.finishFrame(any(), any()) } returns true
        // Stub preference boolean reads so renderHelpContents doesn't ClassCastException
        every { preference.getPref(any(), false, Boolean::class) } returns false
        every { preference.getPref(any(), true, Boolean::class) } returns false

        startKoin {
            modules(
                module {
                    single { preference }
                    single { renderHelps }
                    single { renderingProvider }
                    single { catalogClient }
                }
            )
        }
    }

    private fun createComponent(): DefaultReviewModeComponent = createComponent { context ->
        DefaultReviewModeComponent(
            componentContext = context,
            sharedState = sharedState,
            eventSender = eventChannel,
            targetTranslation = mockTarget
        )
    }

    @Test
    fun testInitializationLoadsReviewItems() = runBlocking {
        val component = createComponent()
        component.items.awaitState { it.isNotEmpty() }

        assertEquals(1, component.items.value.size)
        val item = component.items.value.first()
        assertEquals("01-01", item.id)
        assertEquals("In the beginning", item.sourceText.trim())
        assertEquals(TargetMode.MARKER, item.targetMode)
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
    fun testToggleEdit() = runBlocking {
        val component = createComponent()
        component.items.awaitState { it.isNotEmpty() }
        val item = component.items.value.first()

        component.toggleEdit(item)

        // Since it runs withContext(Dispatchers.IO) inside doToggleEdit, we delay
        component.items.awaitState { it.first().targetMode == TargetMode.EDIT }

        assertEquals(TargetMode.EDIT, component.items.value.first().targetMode)
    }

    @Test
    fun testToggleDoneFlow() = runBlocking {
        val component = createComponent()
        component.items.awaitState { it.isNotEmpty() }
        val item = component.items.value.first()

        component.onToggleDone(item)
        assertEquals(item, component.state.value.chunkToDone)

        component.onDoneConfirmed(true)

        // Wait for async task to process commit/done action
        kotlinx.coroutines.delay(100)

        verify { mockTarget.finishFrame("01", "01") }
        assertNull(component.state.value.chunkToDone)
    }

    @Test
    fun testSearchAndNavigation() = runBlocking {
        val component = createComponent()
        component.openSearch()
        assertNotNull(component.state.value.search)

        component.updateSearchQuery("target")
        assertEquals("target", component.state.value.search?.query)

        component.closeSearch()
        assertNull(component.state.value.search)
    }

    @Test
    fun testConflictFiltering() = runBlocking {
        val component = createComponent()
        component.setConflictFilterOn(true)
        assertTrue(component.state.value.conflictFilterOn)
    }

    @Test
    fun testDeleteFootnote() = runBlocking {
        val component = createComponent()
        val note = Footnote("Test note", "\\f + Test note\\f*", "01-01", 0, 5, 0, FootnoteAction.VIEW)

        // Stub getFrameTranslation to return text with footnote
        every { mockTarget.getFrameTranslation("01", "01", any()) } returns mockk(relaxed = true) {
            every { id } returns "01"
            every { chapterId } returns "01"
            every { body } returns "In the beginning \\f + Test note\\f*"
            every { format } returns TranslationFormat.USFM
        }

        component.handleResourceChange(mockContainer)
        component.items.awaitState { it.isNotEmpty() }

        component.deleteNote(note)

        // Should trigger item text change and save the cleaned version without footnote
        verify { mockTarget.getFrameTranslation("01", "01", any()) }
    }

    @Test
    fun testValidationTranslateFirst() = runBlocking {
        // Stub mockFrameTranslation body to be empty to trigger validation error
        every { mockFrameTranslation.body } returns ""

        val component = createComponent()
        component.items.awaitState { it.isNotEmpty() }
        val item = component.items.value.first()

        component.onToggleDone(item)
        assertEquals(item, component.state.value.chunkToDone)

        component.onDoneConfirmed(true)

        // Verify a snackbar message event is sent
        val event = eventChannel.receive()
        assertTrue(event is TranslateComponent.Event.SnackbarMessage)
    }

    @Test
    fun testSaveFootnote() = runBlocking {
        val component = createComponent()
        component.items.awaitState { it.isNotEmpty() }

        // Test inserting footnote (machineReadable is empty)
        val insertNote = Footnote(
            text = "New footnote text",
            machineReadable = "",
            chunkId = "01-01",
            insertPosition = 5,
            action = FootnoteAction.EDIT
        )
        component.saveFootnote(insertNote)

        // wait for async saveFootnote
        kotlinx.coroutines.delay(100)
        verify { mockTarget.applyFrameTranslation(any(), any()) }

        // Test replacing footnote (machineReadable is present)
        val replaceNote = Footnote(
            text = "Updated footnote text",
            machineReadable = "\\f + Old note\\f*",
            chunkId = "01-01",
            start = 0,
            end = 15,
            action = FootnoteAction.EDIT
        )
        component.saveFootnote(replaceNote)
        kotlinx.coroutines.delay(100)
        verify(atLeast = 2) { mockTarget.applyFrameTranslation(any(), any()) }
    }

    @Test
    fun testUndoRedoHistoryNavigation() = runBlocking {
        val component = createComponent()
        component.items.awaitState { it.isNotEmpty() }
        val item = component.items.value.first()

        val mockHistory = mockk<FileHistory>(relaxed = true)
        every { mockTarget.getFrameHistory(any()) } returns mockHistory
        every { mockHistory.atHead } returns false

        val mockCommit1 = mockk<RevCommit>(relaxed = true)
        every { mockHistory.previous } returns mockCommit1
        every { mockHistory.read(mockCommit1) } returns "Previous Translation Version"

        // Toggle edit to load history
        component.toggleEdit(item)
        component.items.awaitState { it.first().targetMode == TargetMode.EDIT }

        val updatedItem = component.items.value.first()
        assertNotNull(updatedItem.fileHistory)

        // Call undo
        component.undo(updatedItem)

        // wait for async history navigation
        component.items.awaitState { it.first().targetText == "Previous Translation Version" }
        assertEquals("Previous Translation Version", component.items.value.first().targetText)

        // Verify redo
        val mockCommit2 = mockk<RevCommit>(relaxed = true)
        every { mockHistory.next } returns mockCommit2
        every { mockHistory.read(mockCommit2) } returns "Next Translation Version"

        component.redo(component.items.value.first())
        component.items.awaitState { it.first().targetText == "Next Translation Version" }
        assertEquals("Next Translation Version", component.items.value.first().targetText)
    }

    @Test
    fun testReopenDoneItem() = runBlocking {
        val component = createComponent()
        component.items.awaitState { it.isNotEmpty() }

        // Stub mockFrameTranslation finished to return true so item starts as COMPLETE
        every { mockFrameTranslation.finished } returns true

        // Trigger resource change to reload and prepare the item as COMPLETE
        component.handleResourceChange(mockContainer)
        component.items.awaitState { it.first().targetMode == TargetMode.COMPLETE }

        val item = component.items.value.first()
        assertEquals(TargetMode.COMPLETE, item.targetMode)

        // Toggle done, which should call updateDoneStatus(item, false) -> reopen
        component.onToggleDone(item)

        // wait for background reopen and commit
        kotlinx.coroutines.delay(100)
        verify { mockTarget.reopenFrame("01", "01") }
        verify { mockTarget.commit() }
    }

    @Test
    fun testMarkAllDoneFlow() = runBlocking {
        val component = createComponent()
        component.items.awaitState { it.isNotEmpty() }

        // Test toggling and confirming "Mark All Done"
        component.toggleMarkAllDone()
        assertEquals(MarkAllDialogState.Confirm, component.state.value.markAllDoneState)

        // Stub target text to be non-empty so validation passes,
        // and finishFrame returns true (already set in setUp)
        every { mockFrameTranslation.body } returns "\\v 1 Some target text"
        component.handleResourceChange(mockContainer)
        component.items.awaitState { it.first().targetText.isNotEmpty() }

        // Confirm mark all done
        component.onMarkAllDoneConfirmed(true)

        // wait for async progress & marking tasks to complete
        component.state.awaitState { it.markAllDoneState is MarkAllDialogState.Result }

        val resultState = component.state.value.markAllDoneState as MarkAllDialogState.Result
        assertEquals(1, resultState.total)
        // marked == 1 when finishFrame returns true and target text is non-empty
        verify { mockTarget.finishFrame("01", "01") }
        verify { mockTarget.commit() }

        // Test cancel confirmation
        component.toggleMarkAllDone()
        component.onMarkAllDoneConfirmed(false)
        assertNull(component.state.value.markAllDoneState)
    }

    @Test
    fun testSearchSubjectAndNextPrevMatch() = runBlocking {
        val component = createComponent()
        component.items.awaitState { it.isNotEmpty() }

        component.openSearch()

        // Set subject to TARGET
        component.setSearchSubject(SearchSubject.TARGET)
        assertEquals(SearchSubject.TARGET, component.state.value.search?.subject)

        // Update search query matching target text "target"
        component.updateSearchQuery("target")

        // Wait for matchingItemIds to update
        component.state.awaitState { it.search?.matchingItemIds?.isNotEmpty() == true }

        val searchState = component.state.value.search
        assertNotNull(searchState)
        assertEquals(listOf("01-01"), searchState.matchingItemIds)
        assertEquals(0, searchState.currentMatchIndex)

        // Test nextMatch/prevMatch
        component.nextMatch()
        assertEquals(0, component.state.value.search?.currentMatchIndex)

        component.prevMatch()
        assertEquals(0, component.state.value.search?.currentMatchIndex)
    }

    @Test
    fun testConflictResolution() = runBlocking {
        // Stub mockFrameTranslation to have merge conflict markers
        every { mockFrameTranslation.body } returns "<<<<<<< HEAD\nConflicted Option A\n=======\nConflicted Option B\n>>>>>>>\n"

        val component = createComponent()
        component.items.awaitState { it.isNotEmpty() }
        val item = component.items.value.first()

        assertTrue(item.hasMergeConflict)
        assertEquals(2, item.mergeItems.size)
        assertEquals("Conflicted Option A\n", item.mergeItems[0].toString())
        assertEquals("Conflicted Option B\n", item.mergeItems[1].toString())

        // Select conflict index 0
        component.selectConflict(item, 0)

        // wait for async save/refresh
        kotlinx.coroutines.delay(100)
        verify { mockTarget.applyFrameTranslation(any(), "Conflicted Option A\n") }
    }

    @Test
    fun testResourcesAndHelpStateMutations() {
        val component = createComponent()
        
        component.openResources(true)
        assertTrue(component.state.value.resourcesOpen)
        
        component.openResources(false)
        assertTrue(!component.state.value.resourcesOpen)

        // Clear help
        component.clearHelp()
        assertNull(component.state.value.help)

        // Clean url
        component.cleanUrl()
        assertNull(component.state.value.url)
    }

    @Test
    fun testOpenIndex() = runBlocking {
        mockkObject(ContainerCache)
        val mockRc = mockk<ResourceContainer>(relaxed = true) {
            every { chapters() } returns listOf("word1")
            every { readChunk("word1", "01") } returns "# Title of word1\nbody"
        }
        every { ContainerCache.get("tw") } returns mockRc

        val component = createComponent()
        component.openIndex("tw")

        // Wait for help state to update
        component.state.awaitState { it.help is Help.Index }

        val indexHelp = component.state.value.help as Help.Index
        assertEquals("tw", indexHelp.rcSlug)
        assertEquals(1, indexHelp.words.size)
        assertEquals("word1", indexHelp.words.first().slug)
        assertEquals(" Title of word1", indexHelp.words.first().title)
    }

    @Test
    fun testOpenWord() = runBlocking {
        mockkObject(ContainerCache)
        val mockRc = mockk<ResourceContainer>(relaxed = true) {
            every { readChunk("word_slug", "01") } returns "# Word Title\nWord Description Text"
        }
        every { ContainerCache.get("tw") } returns mockRc

        val component = createComponent()
        component.openWord("tw", "word_slug")

        component.state.awaitState { it.help is Help.Words }

        val wordHelp = component.state.value.help as Help.Words
        assertEquals("Word Title", wordHelp.title.trim())
        assertEquals("tw", wordHelp.rcSlug)
    }

    @Test
    fun testDragDropVerse() = runBlocking {
        val component = createComponent()
        component.items.awaitState { it.isNotEmpty() }
        val item = component.items.value.first()

        // Call onDragDropVerse
        component.onDragDropVerse(
            item = item,
            machineReadable = "\\v 2 ",
            verseRawStart = 0,
            verseRawEnd = 5,
            targetRawPosition = 10
        )

        // Wait for it to process
        kotlinx.coroutines.delay(100)
        verify { mockTarget.applyFrameTranslation(any(), any()) }
    }

    @Test
    fun testOnNoteClicked() {
        val component = createComponent()
        val noteNode = RenderNode.Note(
            caller = "+",
            passage = "Gen 1:1",
            notes = "Test Note Content",
            noteStyle = NoteStyle.FOOTNOTE,
            machineReadable = "\\f + Test Note Content\\f*",
            startPos = 10,
            endPos = 20
        )

        component.onNoteClicked(noteNode, "01-01", FootnoteAction.EDIT)

        val footnote = component.state.value.footnote
        assertNotNull(footnote)
        assertEquals("Test Note Content", footnote.text)
        assertEquals("\\f + Test Note Content\\f*", footnote.machineReadable)
        assertEquals("01-01", footnote.chunkId)
        assertEquals(10, footnote.start)
        assertEquals(20, footnote.end)
        assertEquals(FootnoteAction.EDIT, footnote.action)
    }
}
