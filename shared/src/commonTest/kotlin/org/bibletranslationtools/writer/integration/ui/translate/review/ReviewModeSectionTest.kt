package org.bibletranslationtools.writer.integration.ui.translate.review

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.remember
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.text.AnnotatedString
import btt_writer.shared.generated.resources.Res
import btt_writer.shared.generated.resources.chunk_checklist_title
import btt_writer.shared.generated.resources.confirm
import btt_writer.shared.generated.resources.dismiss
import btt_writer.shared.generated.resources.project_checklist_title
import btt_writer.shared.generated.resources.result
import btt_writer.shared.generated.resources.title_cancel
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.bibletranslationtools.writer.core.Progress
import org.bibletranslationtools.writer.integration.ui.ScreenTestBase
import org.bibletranslationtools.writer.integration.ui.translate.FakeTranslateComponent
import org.bibletranslationtools.writer.ui.dialogs.source.SourceTabItem
import org.bibletranslationtools.writer.ui.translate.ReviewItem
import org.bibletranslationtools.writer.ui.translate.ScrollCoordinator
import org.bibletranslationtools.writer.ui.translate.review.MarkAllDialogState
import org.bibletranslationtools.writer.ui.translate.review.ReviewModeComponent
import org.bibletranslationtools.writer.ui.translate.review.ReviewModeSection
import org.bibletranslationtools.writer.ui.translate.review.SearchState
import org.bibletranslationtools.writer.ui.translate.review.TargetMode
import org.bibletranslationtools.writer.utils.getStringBlocking
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class ReviewModeSectionTest : ScreenTestBase() {

    @Test
    fun displays_review_item_and_handles_edit_toggle() = runComposeUiTest {
        val component = mockk<ReviewModeComponent>(relaxed = true)
        val parentComponent = FakeTranslateComponent()
        parentComponent.sharedState.value = parentComponent.sharedState.value.copy(
            resourceContainer = mockk(relaxed = true),
            sourceTabs = listOf(
                SourceTabItem(
                    tag = "en_ulb",
                    title = "ULB",
                    language = "en",
                    direction = "ltr"
                )
            )
        )

        val mockReviewItem = mockk<ReviewItem>(relaxed = true) {
            every { id } returns "review_item_1"
            every { sourceTitle } returns "Mock Source Title"
            every { renderedSourceText } returns AnnotatedString("Mock Source Text")
            every { renderedTargetText } returns AnnotatedString("Mock Target Text")
            every { targetMode } returns TargetMode.MARKER
            every { hasMergeConflict } returns false
        }

        every { component.state } returns MutableStateFlow(ReviewModeComponent.State())
        every { component.items } returns MutableStateFlow(listOf(mockReviewItem))
        every { component.filteredItems } returns MutableStateFlow(listOf(mockReviewItem))
        every { component.progress } returns MutableStateFlow(null)

        var toggleEditCalled = false
        every { component.toggleEdit(mockReviewItem) } answers {
            toggleEditCalled = true
        }

        setContent {
            val listState = rememberLazyListState()
            val scrollCoordinator = remember { ScrollCoordinator(listState) }
            ReviewModeSection(
                component = component,
                parentComponent = parentComponent,
                typography = fakeTypography,
                scrollCoordinator = scrollCoordinator,
                searchRequested = false,
                onSearchConsumed = {},
                onSourceDialogOpen = {},
                onHasMergeConflicts = {},
                onConflictFilterReset = {},
                chunksDoneRequested = false,
                onChunksDoneConsumed = {}
            )
        }

        onNodeWithText("Mock Source Title", substring = true).assertIsDisplayed()

        onNodeWithContentDescription("toggle edit").performClick()
        assertTrue(toggleEditCalled)
    }

    @Test
    fun displays_undo_and_triggers_undo_action_when_in_edit_mode() = runComposeUiTest {
        val component = mockk<ReviewModeComponent>(relaxed = true)
        val parentComponent = FakeTranslateComponent()
        parentComponent.sharedState.value = parentComponent.sharedState.value.copy(
            resourceContainer = mockk(relaxed = true),
            sourceTabs = listOf(
                SourceTabItem(
                    tag = "en_ulb",
                    title = "ULB",
                    language = "en",
                    direction = "ltr"
                )
            )
        )

        val mockReviewItem = mockk<ReviewItem>(relaxed = true) {
            every { id } returns "review_item_1"
            every { sourceTitle } returns "Mock Source Title"
            every { renderedSourceText } returns AnnotatedString("Mock Source Text")
            every { renderedTargetText } returns AnnotatedString("Mock Target Text")
            every { targetMode } returns TargetMode.EDIT
            every { hasMergeConflict } returns false
            every { fileHistory?.hasPrevious } returns true
        }

        every { component.state } returns MutableStateFlow(ReviewModeComponent.State())
        every { component.items } returns MutableStateFlow(listOf(mockReviewItem))
        every { component.filteredItems } returns MutableStateFlow(listOf(mockReviewItem))
        every { component.progress } returns MutableStateFlow(null)

        var undoCalled = false
        every { component.undo(mockReviewItem) } answers {
            undoCalled = true
        }

        setContent {
            val listState = rememberLazyListState()
            val scrollCoordinator = remember { ScrollCoordinator(listState) }
            ReviewModeSection(
                component = component,
                parentComponent = parentComponent,
                typography = fakeTypography,
                scrollCoordinator = scrollCoordinator,
                searchRequested = false,
                onSearchConsumed = {},
                onSourceDialogOpen = {},
                onHasMergeConflicts = {},
                onConflictFilterReset = {},
                chunksDoneRequested = false,
                onChunksDoneConsumed = {}
            )
        }

        onNodeWithContentDescription("Undo").performClick()
        assertTrue(undoCalled)
    }

    @Test
    fun displays_search_bar_when_search_state_is_not_null() = runComposeUiTest {
        val component = mockk<ReviewModeComponent>(relaxed = true)
        val parentComponent = FakeTranslateComponent()
        parentComponent.sharedState.value = parentComponent.sharedState.value.copy(
            resourceContainer = mockk(relaxed = true),
            sourceTabs = listOf(
                SourceTabItem(
                    tag = "en_ulb",
                    title = "ULB",
                    language = "en",
                    direction = "ltr"
                )
            )
        )

        val mockReviewItem = mockk<ReviewItem>(relaxed = true) {
            every { id } returns "review_item_1"
            every { sourceTitle } returns "Mock Source Title"
            every { renderedSourceText } returns AnnotatedString("Mock Source Text")
            every { renderedTargetText } returns AnnotatedString("Mock Target Text")
            every { targetMode } returns TargetMode.MARKER
            every { hasMergeConflict } returns false
        }

        val stateFlow = MutableStateFlow(
            ReviewModeComponent.State(search = SearchState(query = "test query"))
        )
        every { component.state } returns stateFlow
        every { component.items } returns MutableStateFlow(listOf(mockReviewItem))
        every { component.filteredItems } returns MutableStateFlow(listOf(mockReviewItem))
        every { component.progress } returns MutableStateFlow(null)

        var closeSearchCalled = false
        every { component.closeSearch() } answers {
            closeSearchCalled = true
        }

        setContent {
            val listState = rememberLazyListState()
            val scrollCoordinator = remember { ScrollCoordinator(listState) }
            ReviewModeSection(
                component = component,
                parentComponent = parentComponent,
                typography = fakeTypography,
                scrollCoordinator = scrollCoordinator,
                searchRequested = false,
                onSearchConsumed = {},
                onSourceDialogOpen = {},
                onHasMergeConflicts = {},
                onConflictFilterReset = {},
                chunksDoneRequested = false,
                onChunksDoneConsumed = {}
            )
        }

        onNodeWithContentDescription("Close search").assertIsDisplayed()
        onNodeWithContentDescription("Close search").performClick()
        assertTrue(closeSearchCalled)
    }

    @Test
    fun shows_chunk_done_dialog_and_confirms() = runComposeUiTest {
        val component = mockk<ReviewModeComponent>(relaxed = true)
        val parentComponent = FakeTranslateComponent()
        parentComponent.sharedState.value = parentComponent.sharedState.value.copy(
            resourceContainer = mockk(relaxed = true),
            sourceTabs = listOf(SourceTabItem(tag = "en_ulb", title = "ULB", language = "en", direction = "ltr"))
        )

        val mockItem = mockk<ReviewItem>(relaxed = true) {
            every { id } returns "item_1"
            every { targetMode } returns TargetMode.MARKER
            every { hasMergeConflict } returns false
        }

        every { component.state } returns MutableStateFlow(
            ReviewModeComponent.State(chunkToDone = mockItem)
        )
        every { component.items } returns MutableStateFlow(listOf(mockItem))
        every { component.filteredItems } returns MutableStateFlow(listOf(mockItem))
        every { component.progress } returns MutableStateFlow(null)

        var confirmedWith: Boolean? = null
        every { component.onDoneConfirmed(any()) } answers { confirmedWith = firstArg() }

        setContent {
            val listState = rememberLazyListState()
            val scrollCoordinator = remember { ScrollCoordinator(listState) }
            ReviewModeSection(
                component = component,
                parentComponent = parentComponent,
                typography = fakeTypography,
                scrollCoordinator = scrollCoordinator,
                searchRequested = false,
                onSearchConsumed = {},
                onSourceDialogOpen = {},
                onHasMergeConflicts = {},
                onConflictFilterReset = {},
                chunksDoneRequested = false,
                onChunksDoneConsumed = {}
            )
        }

        onNodeWithText(getStringBlocking(Res.string.chunk_checklist_title), substring = true).assertIsDisplayed()
        onNodeWithText(getStringBlocking(Res.string.confirm)).performClick()

        assertEquals(true, confirmedWith)
    }

    @Test
    fun shows_chunk_done_dialog_and_dismisses() = runComposeUiTest {
        val component = mockk<ReviewModeComponent>(relaxed = true)
        val parentComponent = FakeTranslateComponent()
        parentComponent.sharedState.value = parentComponent.sharedState.value.copy(
            resourceContainer = mockk(relaxed = true),
            sourceTabs = listOf(SourceTabItem(tag = "en_ulb", title = "ULB", language = "en", direction = "ltr"))
        )

        val mockItem = mockk<ReviewItem>(relaxed = true) {
            every { id } returns "item_1"
            every { targetMode } returns TargetMode.MARKER
            every { hasMergeConflict } returns false
        }

        every { component.state } returns MutableStateFlow(
            ReviewModeComponent.State(chunkToDone = mockItem)
        )
        every { component.items } returns MutableStateFlow(listOf(mockItem))
        every { component.filteredItems } returns MutableStateFlow(listOf(mockItem))
        every { component.progress } returns MutableStateFlow(null)

        var confirmedWith: Boolean? = null
        every { component.onDoneConfirmed(any()) } answers { confirmedWith = firstArg() }

        setContent {
            val listState = rememberLazyListState()
            val scrollCoordinator = remember { ScrollCoordinator(listState) }
            ReviewModeSection(
                component = component,
                parentComponent = parentComponent,
                typography = fakeTypography,
                scrollCoordinator = scrollCoordinator,
                searchRequested = false,
                onSearchConsumed = {},
                onSourceDialogOpen = {},
                onHasMergeConflicts = {},
                onConflictFilterReset = {},
                chunksDoneRequested = false,
                onChunksDoneConsumed = {}
            )
        }

        onNodeWithText(getStringBlocking(Res.string.chunk_checklist_title), substring = true).assertIsDisplayed()
        onNodeWithText(getStringBlocking(Res.string.title_cancel)).performClick()

        assertEquals(false, confirmedWith)
    }

    @Test
    fun shows_mark_all_done_confirm_dialog_and_confirms() = runComposeUiTest {
        val component = mockk<ReviewModeComponent>(relaxed = true)
        val parentComponent = FakeTranslateComponent()
        parentComponent.sharedState.value = parentComponent.sharedState.value.copy(
            resourceContainer = mockk(relaxed = true),
            sourceTabs = listOf(SourceTabItem(tag = "en_ulb", title = "ULB", language = "en", direction = "ltr"))
        )

        val mockItem = mockk<ReviewItem>(relaxed = true) {
            every { id } returns "item_1"
            every { targetMode } returns TargetMode.MARKER
            every { hasMergeConflict } returns false
        }

        every { component.state } returns MutableStateFlow(
            ReviewModeComponent.State(markAllDoneState = MarkAllDialogState.Confirm)
        )
        every { component.items } returns MutableStateFlow(listOf(mockItem))
        every { component.filteredItems } returns MutableStateFlow(listOf(mockItem))
        every { component.progress } returns MutableStateFlow(null)

        var confirmedWith: Boolean? = null
        every { component.onMarkAllDoneConfirmed(any()) } answers { confirmedWith = firstArg() }

        setContent {
            val listState = rememberLazyListState()
            val scrollCoordinator = remember { ScrollCoordinator(listState) }
            ReviewModeSection(
                component = component,
                parentComponent = parentComponent,
                typography = fakeTypography,
                scrollCoordinator = scrollCoordinator,
                searchRequested = false,
                onSearchConsumed = {},
                onSourceDialogOpen = {},
                onHasMergeConflicts = {},
                onConflictFilterReset = {},
                chunksDoneRequested = false,
                onChunksDoneConsumed = {}
            )
        }

        onNodeWithText(getStringBlocking(Res.string.project_checklist_title), substring = true).assertIsDisplayed()
        onNodeWithText(getStringBlocking(Res.string.confirm)).performClick()

        assertEquals(true, confirmedWith)
    }

    @Test
    fun shows_mark_all_done_result_dialog_and_dismisses() = runComposeUiTest {
        val component = mockk<ReviewModeComponent>(relaxed = true)
        val parentComponent = FakeTranslateComponent()
        parentComponent.sharedState.value = parentComponent.sharedState.value.copy(
            resourceContainer = mockk(relaxed = true),
            sourceTabs = listOf(SourceTabItem(tag = "en_ulb", title = "ULB", language = "en", direction = "ltr"))
        )

        val mockItem = mockk<ReviewItem>(relaxed = true) {
            every { id } returns "item_1"
            every { targetMode } returns TargetMode.MARKER
            every { hasMergeConflict } returns false
        }

        every { component.state } returns MutableStateFlow(
            ReviewModeComponent.State(markAllDoneState = MarkAllDialogState.Result(marked = 3, total = 10))
        )
        every { component.items } returns MutableStateFlow(listOf(mockItem))
        every { component.filteredItems } returns MutableStateFlow(listOf(mockItem))
        every { component.progress } returns MutableStateFlow(null)

        var dismissCalled = false
        every { component.onMarkAllDoneConfirmed(false) } answers { dismissCalled = true }

        setContent {
            val listState = rememberLazyListState()
            val scrollCoordinator = remember { ScrollCoordinator(listState) }
            ReviewModeSection(
                component = component,
                parentComponent = parentComponent,
                typography = fakeTypography,
                scrollCoordinator = scrollCoordinator,
                searchRequested = false,
                onSearchConsumed = {},
                onSourceDialogOpen = {},
                onHasMergeConflicts = {},
                onConflictFilterReset = {},
                chunksDoneRequested = false,
                onChunksDoneConsumed = {}
            )
        }

        onNodeWithText(getStringBlocking(Res.string.result), substring = true).assertIsDisplayed()
        onNodeWithText(getStringBlocking(Res.string.dismiss)).performClick()

        assertTrue(dismissCalled)
    }

    @Test
    fun search_requested_calls_open_search_and_consumed() = runComposeUiTest {
        val component = mockk<ReviewModeComponent>(relaxed = true)
        val parentComponent = FakeTranslateComponent()
        parentComponent.sharedState.value = parentComponent.sharedState.value.copy(
            resourceContainer = mockk(relaxed = true),
            sourceTabs = listOf(SourceTabItem(tag = "en_ulb", title = "ULB", language = "en", direction = "ltr"))
        )

        every { component.state } returns MutableStateFlow(ReviewModeComponent.State())
        every { component.items } returns MutableStateFlow(emptyList())
        every { component.filteredItems } returns MutableStateFlow(emptyList())
        every { component.progress } returns MutableStateFlow(null)

        var searchConsumedCalled = false

        setContent {
            val listState = rememberLazyListState()
            val scrollCoordinator = remember { ScrollCoordinator(listState) }
            ReviewModeSection(
                component = component,
                parentComponent = parentComponent,
                typography = fakeTypography,
                scrollCoordinator = scrollCoordinator,
                searchRequested = true,
                onSearchConsumed = { searchConsumedCalled = true },
                onSourceDialogOpen = {},
                onHasMergeConflicts = {},
                onConflictFilterReset = {},
                chunksDoneRequested = false,
                onChunksDoneConsumed = {}
            )
        }

        verify { component.openSearch() }
        assertTrue(searchConsumedCalled)
    }

    @Test
    fun chunks_done_requested_calls_toggle_and_consumed() = runComposeUiTest {
        val component = mockk<ReviewModeComponent>(relaxed = true)
        val parentComponent = FakeTranslateComponent()
        parentComponent.sharedState.value = parentComponent.sharedState.value.copy(
            resourceContainer = mockk(relaxed = true),
            sourceTabs = listOf(SourceTabItem(tag = "en_ulb", title = "ULB", language = "en", direction = "ltr"))
        )

        every { component.state } returns MutableStateFlow(ReviewModeComponent.State())
        every { component.items } returns MutableStateFlow(emptyList())
        every { component.filteredItems } returns MutableStateFlow(emptyList())
        every { component.progress } returns MutableStateFlow(null)

        var chunksDoneConsumedCalled = false

        setContent {
            val listState = rememberLazyListState()
            val scrollCoordinator = remember { ScrollCoordinator(listState) }
            ReviewModeSection(
                component = component,
                parentComponent = parentComponent,
                typography = fakeTypography,
                scrollCoordinator = scrollCoordinator,
                searchRequested = false,
                onSearchConsumed = {},
                onSourceDialogOpen = {},
                onHasMergeConflicts = {},
                onConflictFilterReset = {},
                chunksDoneRequested = true,
                onChunksDoneConsumed = { chunksDoneConsumedCalled = true }
            )
        }

        verify { component.toggleMarkAllDone() }
        assertTrue(chunksDoneConsumedCalled)
    }

    @Test
    fun conflict_filter_resets_when_no_conflicts_remain() = runComposeUiTest {
        val component = mockk<ReviewModeComponent>(relaxed = true)
        val parentComponent = FakeTranslateComponent()
        parentComponent.sharedState.value = parentComponent.sharedState.value.copy(
            resourceContainer = mockk(relaxed = true),
            sourceTabs = listOf(SourceTabItem(tag = "en_ulb", title = "ULB", language = "en", direction = "ltr"))
        )

        val mockItem = mockk<ReviewItem>(relaxed = true) {
            every { id } returns "item_1"
            every { targetMode } returns TargetMode.MARKER
            every { hasMergeConflict } returns false
        }

        every { component.state } returns MutableStateFlow(ReviewModeComponent.State())
        every { component.items } returns MutableStateFlow(listOf(mockItem))
        every { component.filteredItems } returns MutableStateFlow(listOf(mockItem))
        every { component.progress } returns MutableStateFlow(null)

        var conflictFilterResetCalled = false

        setContent {
            val listState = rememberLazyListState()
            val scrollCoordinator = remember { ScrollCoordinator(listState) }
            ReviewModeSection(
                component = component,
                parentComponent = parentComponent,
                typography = fakeTypography,
                scrollCoordinator = scrollCoordinator,
                searchRequested = false,
                onSearchConsumed = {},
                onSourceDialogOpen = {},
                onHasMergeConflicts = {},
                conflictFilterOn = true,
                onConflictFilterReset = { conflictFilterResetCalled = true },
                chunksDoneRequested = false,
                onChunksDoneConsumed = {}
            )
        }

        assertTrue(conflictFilterResetCalled)
        verify { component.setConflictFilterOn(false) }
    }

    @Test
    fun redo_button_visible_and_triggers_redo_in_edit_mode() = runComposeUiTest {
        val component = mockk<ReviewModeComponent>(relaxed = true)
        val parentComponent = FakeTranslateComponent()
        parentComponent.sharedState.value = parentComponent.sharedState.value.copy(
            resourceContainer = mockk(relaxed = true),
            sourceTabs = listOf(SourceTabItem(tag = "en_ulb", title = "ULB", language = "en", direction = "ltr"))
        )

        val mockItem = mockk<ReviewItem>(relaxed = true) {
            every { id } returns "item_1"
            every { targetMode } returns TargetMode.EDIT
            every { hasMergeConflict } returns false
            every { fileHistory?.hasNext } returns true
        }

        every { component.state } returns MutableStateFlow(ReviewModeComponent.State())
        every { component.items } returns MutableStateFlow(listOf(mockItem))
        every { component.filteredItems } returns MutableStateFlow(listOf(mockItem))
        every { component.progress } returns MutableStateFlow(null)

        var redoCalled = false
        every { component.redo(mockItem) } answers { redoCalled = true }

        setContent {
            val listState = rememberLazyListState()
            val scrollCoordinator = remember { ScrollCoordinator(listState) }
            ReviewModeSection(
                component = component,
                parentComponent = parentComponent,
                typography = fakeTypography,
                scrollCoordinator = scrollCoordinator,
                searchRequested = false,
                onSearchConsumed = {},
                onSourceDialogOpen = {},
                onHasMergeConflicts = {},
                onConflictFilterReset = {},
                chunksDoneRequested = false,
                onChunksDoneConsumed = {}
            )
        }

        onNodeWithContentDescription("Redo").assertIsDisplayed()
        onNodeWithContentDescription("Redo").performClick()

        assertTrue(redoCalled)
    }

    @Test
    fun mark_all_done_confirm_dialog_dismisses() = runComposeUiTest {
        val component = mockk<ReviewModeComponent>(relaxed = true)
        val parentComponent = FakeTranslateComponent()
        parentComponent.sharedState.value = parentComponent.sharedState.value.copy(
            resourceContainer = mockk(relaxed = true),
            sourceTabs = listOf(SourceTabItem(tag = "en_ulb", title = "ULB", language = "en", direction = "ltr"))
        )

        val mockItem = mockk<ReviewItem>(relaxed = true) {
            every { id } returns "item_1"
            every { targetMode } returns TargetMode.MARKER
            every { hasMergeConflict } returns false
        }

        every { component.state } returns MutableStateFlow(
            ReviewModeComponent.State(markAllDoneState = MarkAllDialogState.Confirm)
        )
        every { component.items } returns MutableStateFlow(listOf(mockItem))
        every { component.filteredItems } returns MutableStateFlow(listOf(mockItem))
        every { component.progress } returns MutableStateFlow(null)

        var confirmedWith: Boolean? = null
        every { component.onMarkAllDoneConfirmed(any()) } answers { confirmedWith = firstArg() }

        setContent {
            val listState = rememberLazyListState()
            val scrollCoordinator = remember { ScrollCoordinator(listState) }
            ReviewModeSection(
                component = component,
                parentComponent = parentComponent,
                typography = fakeTypography,
                scrollCoordinator = scrollCoordinator,
                searchRequested = false,
                onSearchConsumed = {},
                onSourceDialogOpen = {},
                onHasMergeConflicts = {},
                onConflictFilterReset = {},
                chunksDoneRequested = false,
                onChunksDoneConsumed = {}
            )
        }

        onNodeWithText(getStringBlocking(Res.string.project_checklist_title), substring = true).assertIsDisplayed()
        onNodeWithText(getStringBlocking(Res.string.title_cancel)).performClick()

        assertEquals(false, confirmedWith)
    }

    @Test
    fun progress_dialog_shown_when_progress_not_null() = runComposeUiTest {
        val component = mockk<ReviewModeComponent>(relaxed = true)
        val parentComponent = FakeTranslateComponent()
        parentComponent.sharedState.value = parentComponent.sharedState.value.copy(
            resourceContainer = mockk(relaxed = true),
            sourceTabs = listOf(SourceTabItem(tag = "en_ulb", title = "ULB", language = "en", direction = "ltr"))
        )

        every { component.state } returns MutableStateFlow(ReviewModeComponent.State())
        every { component.items } returns MutableStateFlow(emptyList())
        every { component.filteredItems } returns MutableStateFlow(emptyList())
        every { component.progress } returns MutableStateFlow(Progress(message = "Processing..."))

        setContent {
            val listState = rememberLazyListState()
            val scrollCoordinator = remember { ScrollCoordinator(listState) }
            ReviewModeSection(
                component = component,
                parentComponent = parentComponent,
                typography = fakeTypography,
                scrollCoordinator = scrollCoordinator,
                searchRequested = false,
                onSearchConsumed = {},
                onSourceDialogOpen = {},
                onHasMergeConflicts = {},
                onConflictFilterReset = {},
                chunksDoneRequested = false,
                onChunksDoneConsumed = {}
            )
        }

        onNodeWithText("Processing...").assertIsDisplayed()
    }
}
