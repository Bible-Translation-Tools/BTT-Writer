package org.bibletranslationtools.writer.integration.ui.translate.read

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.remember
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.text.AnnotatedString
import btt_writer.shared.generated.resources.Res
import btt_writer.shared.generated.resources.begin_translating
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.bibletranslationtools.writer.core.Progress
import org.bibletranslationtools.writer.integration.ui.ScreenTestBase
import org.bibletranslationtools.writer.integration.ui.translate.FakeTranslateComponent
import org.bibletranslationtools.writer.ui.dialogs.source.SourceTabItem
import org.bibletranslationtools.writer.ui.translate.ReadItem
import org.bibletranslationtools.writer.ui.translate.ScrollCoordinator
import org.bibletranslationtools.writer.ui.translate.read.ReadModeComponent
import org.bibletranslationtools.writer.ui.translate.read.ReadModeSection
import org.bibletranslationtools.writer.utils.getStringBlocking
import org.junit.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class ReadModeSectionTest : ScreenTestBase() {

    @Test
    fun displays_read_item_and_triggers_begin_translation() = runComposeUiTest {
        val component = mockk<ReadModeComponent>(relaxed = true)
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

        val mockReadItem = mockk<ReadItem>(relaxed = true) {
            every { id } returns "read_item_1"
            every { sourceTitle } returns "Mock Source Title"
            every { targetTitle } returns "Mock Target Title"
            every { renderedSourceText } returns AnnotatedString("Mock Source Text")
            every { renderedTargetText } returns AnnotatedString("") // blank to trigger begin translating button
            every { sourceOnTop } returns false
            every { chunk.chapterSlug } returns "3"
            every { hasMergeConflict } returns false
        }

        every { component.state } returns MutableStateFlow(ReadModeComponent.State())
        every { component.items } returns MutableStateFlow(listOf(mockReadItem))
        every { component.progress } returns MutableStateFlow(null)

        var beginTranslationCalledWith: String? = null

        setContent {
            val listState = rememberLazyListState()
            val scrollCoordinator = remember { ScrollCoordinator(listState) }
            ReadModeSection(
                component = component,
                parentComponent = parentComponent,
                typography = fakeTypography,
                scrollCoordinator = scrollCoordinator,
                onSourceDialogOpen = {},
                onHasMergeConflicts = {},
                onBeginTranslation = { beginTranslationCalledWith = it }
            )
        }

        onNodeWithText("Mock Target Title", substring = true).assertIsDisplayed()

        onNodeWithText(getStringBlocking(Res.string.begin_translating)).performClick()
        assertEquals("3", beginTranslationCalledWith)
    }

    @Test
    fun target_text_shown_instead_of_begin_button_when_not_blank() = runComposeUiTest {
        val component = mockk<ReadModeComponent>(relaxed = true)
        val parentComponent = FakeTranslateComponent()
        parentComponent.sharedState.value = parentComponent.sharedState.value.copy(
            resourceContainer = mockk(relaxed = true),
            sourceTabs = listOf(SourceTabItem(tag = "en_ulb", title = "ULB", language = "en", direction = "ltr"))
        )

        val mockReadItem = mockk<ReadItem>(relaxed = true) {
            every { id } returns "read_item_1"
            every { sourceTitle } returns "Mock Source Title"
            every { targetTitle } returns "Mock Target Title"
            every { renderedSourceText } returns AnnotatedString("Mock Source Text")
            every { renderedTargetText } returns AnnotatedString("Existing translation text")
            every { sourceOnTop } returns false
            every { chunk.chapterSlug } returns "3"
            every { hasMergeConflict } returns false
        }

        every { component.state } returns MutableStateFlow(ReadModeComponent.State())
        every { component.items } returns MutableStateFlow(listOf(mockReadItem))
        every { component.progress } returns MutableStateFlow(null)

        setContent {
            val listState = rememberLazyListState()
            val scrollCoordinator = remember { ScrollCoordinator(listState) }
            ReadModeSection(
                component = component,
                parentComponent = parentComponent,
                typography = fakeTypography,
                scrollCoordinator = scrollCoordinator,
                onSourceDialogOpen = {},
                onHasMergeConflicts = {},
                onBeginTranslation = {}
            )
        }

        onNodeWithText(getStringBlocking(Res.string.begin_translating)).assertDoesNotExist()
        onNodeWithText("Existing translation text", substring = true).assertIsDisplayed()
    }

    @Test
    fun has_merge_conflicts_notified_when_item_has_conflict() = runComposeUiTest {
        val component = mockk<ReadModeComponent>(relaxed = true)
        val parentComponent = FakeTranslateComponent()
        parentComponent.sharedState.value = parentComponent.sharedState.value.copy(
            resourceContainer = mockk(relaxed = true),
            sourceTabs = listOf(SourceTabItem(tag = "en_ulb", title = "ULB", language = "en", direction = "ltr"))
        )

        val mockReadItem = mockk<ReadItem>(relaxed = true) {
            every { id } returns "read_item_1"
            every { renderedSourceText } returns AnnotatedString("Source")
            every { renderedTargetText } returns AnnotatedString("Target")
            every { sourceOnTop } returns false
            every { hasMergeConflict } returns true
        }

        every { component.state } returns MutableStateFlow(ReadModeComponent.State())
        every { component.items } returns MutableStateFlow(listOf(mockReadItem))
        every { component.progress } returns MutableStateFlow(null)

        var hasMergeConflictsValue: Boolean? = null

        setContent {
            val listState = rememberLazyListState()
            val scrollCoordinator = remember { ScrollCoordinator(listState) }
            ReadModeSection(
                component = component,
                parentComponent = parentComponent,
                typography = fakeTypography,
                scrollCoordinator = scrollCoordinator,
                onSourceDialogOpen = {},
                onHasMergeConflicts = { hasMergeConflictsValue = it },
                onBeginTranslation = {}
            )
        }

        assertEquals(hasMergeConflictsValue, true)
    }

    @Test
    fun progress_dialog_shown_when_progress_not_null() = runComposeUiTest {
        val component = mockk<ReadModeComponent>(relaxed = true)
        val parentComponent = FakeTranslateComponent()
        parentComponent.sharedState.value = parentComponent.sharedState.value.copy(
            resourceContainer = mockk(relaxed = true),
            sourceTabs = listOf(SourceTabItem(tag = "en_ulb", title = "ULB", language = "en", direction = "ltr"))
        )

        every { component.state } returns MutableStateFlow(ReadModeComponent.State())
        every { component.items } returns MutableStateFlow(emptyList())
        every { component.progress } returns MutableStateFlow(Progress(message = "Loading content..."))

        setContent {
            val listState = rememberLazyListState()
            val scrollCoordinator = remember { ScrollCoordinator(listState) }
            ReadModeSection(
                component = component,
                parentComponent = parentComponent,
                typography = fakeTypography,
                scrollCoordinator = scrollCoordinator,
                onSourceDialogOpen = {},
                onHasMergeConflicts = {},
                onBeginTranslation = {}
            )
        }

        onNodeWithText("Loading content...").assertIsDisplayed()
    }
}
