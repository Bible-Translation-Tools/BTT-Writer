package org.bibletranslationtools.writer.integration.ui.translate.chunk

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.remember
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.text.AnnotatedString
import btt_writer.shared.generated.resources.Res
import btt_writer.shared.generated.resources.chunk_done_title
import btt_writer.shared.generated.resources.conflict_exists
import btt_writer.shared.generated.resources.edit
import btt_writer.shared.generated.resources.title_cancel
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.bibletranslationtools.writer.core.Progress
import org.bibletranslationtools.writer.integration.ui.ScreenTestBase
import org.bibletranslationtools.writer.integration.ui.translate.FakeTranslateComponent
import org.bibletranslationtools.writer.ui.dialogs.source.SourceTabItem
import org.bibletranslationtools.writer.ui.translate.ChunkItem
import org.bibletranslationtools.writer.ui.translate.ScrollCoordinator
import org.bibletranslationtools.writer.ui.translate.chunk.ChunkModeComponent
import org.bibletranslationtools.writer.ui.translate.chunk.ChunkModeSection
import org.bibletranslationtools.writer.utils.getStringBlocking
import org.junit.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class ChunkModeSectionTest : ScreenTestBase() {

    @Test
    fun displays_conflict_exists_button_and_triggers_conflict_navigation() = runComposeUiTest {
        val component = mockk<ChunkModeComponent>(relaxed = true)
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

        val mockChunkItem = mockk<ChunkItem>(relaxed = true) {
            every { id } returns "chunk_item_1"
            every { sourceTitle } returns "Mock Source Title"
            every { targetTitle } returns "Mock Target Title"
            every { renderedSourceText } returns AnnotatedString("Mock Source Text")
            every { renderedTargetText } returns AnnotatedString("")
            every { sourceOnTop } returns false
            every { chunk.chapterSlug } returns "4"
            every { chunk.chunkSlug } returns "chunk_01"
            every { hasMergeConflict } returns true
        }

        every { component.state } returns MutableStateFlow(ChunkModeComponent.State())
        every { component.items } returns MutableStateFlow(listOf(mockChunkItem))
        every { component.progress } returns MutableStateFlow(null)

        var conflictClickCalledWith: Pair<String, String>? = null

        setContent {
            val listState = rememberLazyListState()
            val scrollCoordinator = remember { ScrollCoordinator(listState) }
            ChunkModeSection(
                component = component,
                parentComponent = parentComponent,
                typography = fakeTypography,
                scrollCoordinator = scrollCoordinator,
                onSourceDialogOpen = {},
                onHasMergeConflicts = {},
                onConflictClick = { chapter, chunk ->
                    conflictClickCalledWith = Pair(chapter, chunk)
                }
            )
        }

        onNodeWithText(getStringBlocking(Res.string.conflict_exists)).assertIsDisplayed()
        onNodeWithText(getStringBlocking(Res.string.conflict_exists)).performClick()

        assertEquals(Pair("4", "chunk_01"), conflictClickCalledWith)
    }

    @Test
    fun displays_reopen_chunk_dialog_and_confirms() = runComposeUiTest {
        val component = mockk<ChunkModeComponent>(relaxed = true)
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

        val mockChunkItem = mockk<ChunkItem>(relaxed = true) {
            every { id } returns "chunk_item_1"
            every { sourceTitle } returns "Mock Source Title"
            every { targetTitle } returns "Mock Target Title"
            every { renderedSourceText } returns AnnotatedString("Mock Source Text")
            every { renderedTargetText } returns AnnotatedString("")
            every { sourceOnTop } returns false
            every { chunk.chapterSlug } returns "4"
            every { chunk.chunkSlug } returns "chunk_01"
            every { hasMergeConflict } returns false
        }

        var reopenChunkConfirmedVal: Boolean? = null
        every { component.onReopenChunkConfirmed(any()) } answers {
            reopenChunkConfirmedVal = firstArg()
        }

        val stateFlow = MutableStateFlow(
            ChunkModeComponent.State(chunkToReopen = mockChunkItem)
        )
        every { component.state } returns stateFlow
        every { component.items } returns MutableStateFlow(listOf(mockChunkItem))
        every { component.progress } returns MutableStateFlow(null)

        setContent {
            val listState = rememberLazyListState()
            val scrollCoordinator = remember { ScrollCoordinator(listState) }
            ChunkModeSection(
                component = component,
                parentComponent = parentComponent,
                typography = fakeTypography,
                scrollCoordinator = scrollCoordinator,
                onSourceDialogOpen = {},
                onHasMergeConflicts = {},
                onConflictClick = { _, _ -> }
            )
        }

        onNodeWithText(getStringBlocking(Res.string.chunk_done_title), substring = true).assertIsDisplayed()

        // Exact match to target confirm button
        onNodeWithText(getStringBlocking(Res.string.edit)).performClick()

        assertEquals(reopenChunkConfirmedVal, true)
    }

    @Test
    fun reopen_chunk_dialog_dismisses() = runComposeUiTest {
        val component = mockk<ChunkModeComponent>(relaxed = true)
        val parentComponent = FakeTranslateComponent()
        parentComponent.sharedState.value = parentComponent.sharedState.value.copy(
            resourceContainer = mockk(relaxed = true),
            sourceTabs = listOf(SourceTabItem(tag = "en_ulb", title = "ULB", language = "en", direction = "ltr"))
        )

        val mockChunkItem = mockk<ChunkItem>(relaxed = true) {
            every { id } returns "chunk_item_1"
            every { renderedSourceText } returns AnnotatedString("Mock Source Text")
            every { renderedTargetText } returns AnnotatedString("")
            every { sourceOnTop } returns false
            every { hasMergeConflict } returns false
        }

        var reopenChunkConfirmedVal: Boolean? = null
        every { component.onReopenChunkConfirmed(any()) } answers {
            reopenChunkConfirmedVal = firstArg()
        }

        every { component.state } returns MutableStateFlow(
            ChunkModeComponent.State(chunkToReopen = mockChunkItem)
        )
        every { component.items } returns MutableStateFlow(listOf(mockChunkItem))
        every { component.progress } returns MutableStateFlow(null)

        setContent {
            val listState = rememberLazyListState()
            val scrollCoordinator = remember { ScrollCoordinator(listState) }
            ChunkModeSection(
                component = component,
                parentComponent = parentComponent,
                typography = fakeTypography,
                scrollCoordinator = scrollCoordinator,
                onSourceDialogOpen = {},
                onHasMergeConflicts = {},
                onConflictClick = { _, _ -> }
            )
        }

        onNodeWithText(getStringBlocking(Res.string.chunk_done_title), substring = true).assertIsDisplayed()
        onNodeWithText(getStringBlocking(Res.string.title_cancel)).performClick()

        assertEquals(false, reopenChunkConfirmedVal)
    }

    @Test
    fun has_merge_conflicts_notified_when_item_has_conflict() = runComposeUiTest {
        val component = mockk<ChunkModeComponent>(relaxed = true)
        val parentComponent = FakeTranslateComponent()
        parentComponent.sharedState.value = parentComponent.sharedState.value.copy(
            resourceContainer = mockk(relaxed = true),
            sourceTabs = listOf(SourceTabItem(tag = "en_ulb", title = "ULB", language = "en", direction = "ltr"))
        )

        val mockChunkItem = mockk<ChunkItem>(relaxed = true) {
            every { id } returns "chunk_item_1"
            every { renderedSourceText } returns AnnotatedString("Source")
            every { renderedTargetText } returns AnnotatedString("")
            every { sourceOnTop } returns false
            every { hasMergeConflict } returns true
            every { chunk.chapterSlug } returns "1"
            every { chunk.chunkSlug } returns "chunk_01"
        }

        every { component.state } returns MutableStateFlow(ChunkModeComponent.State())
        every { component.items } returns MutableStateFlow(listOf(mockChunkItem))
        every { component.progress } returns MutableStateFlow(null)

        var hasMergeConflictsValue: Boolean? = null

        setContent {
            val listState = rememberLazyListState()
            val scrollCoordinator = remember { ScrollCoordinator(listState) }
            ChunkModeSection(
                component = component,
                parentComponent = parentComponent,
                typography = fakeTypography,
                scrollCoordinator = scrollCoordinator,
                onSourceDialogOpen = {},
                onHasMergeConflicts = { hasMergeConflictsValue = it },
                onConflictClick = { _, _ -> }
            )
        }

        assertEquals(hasMergeConflictsValue, true)
    }

    @Test
    fun progress_dialog_shown_when_progress_not_null() = runComposeUiTest {
        val component = mockk<ChunkModeComponent>(relaxed = true)
        val parentComponent = FakeTranslateComponent()
        parentComponent.sharedState.value = parentComponent.sharedState.value.copy(
            resourceContainer = mockk(relaxed = true),
            sourceTabs = listOf(SourceTabItem(tag = "en_ulb", title = "ULB", language = "en", direction = "ltr"))
        )

        every { component.state } returns MutableStateFlow(ChunkModeComponent.State())
        every { component.items } returns MutableStateFlow(emptyList())
        every { component.progress } returns MutableStateFlow(Progress(message = "Saving changes..."))

        setContent {
            val listState = rememberLazyListState()
            val scrollCoordinator = remember { ScrollCoordinator(listState) }
            ChunkModeSection(
                component = component,
                parentComponent = parentComponent,
                typography = fakeTypography,
                scrollCoordinator = scrollCoordinator,
                onSourceDialogOpen = {},
                onHasMergeConflicts = {},
                onConflictClick = { _, _ -> }
            )
        }

        onNodeWithText("Saving changes...").assertIsDisplayed()
    }
}
