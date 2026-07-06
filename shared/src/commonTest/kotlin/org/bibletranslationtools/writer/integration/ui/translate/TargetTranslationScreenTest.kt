package org.bibletranslationtools.writer.integration.ui.translate

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import btt_writer.shared.generated.resources.Res
import btt_writer.shared.generated.resources.action_settings
import btt_writer.shared.generated.resources.action_translations
import btt_writer.shared.generated.resources.choose_first_source_translation
import btt_writer.shared.generated.resources.choose_source_translations
import btt_writer.shared.generated.resources.feedback
import com.arkivanov.decompose.Child
import com.arkivanov.decompose.router.slot.ChildSlot
import com.arkivanov.decompose.value.MutableValue
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.bibletranslationtools.writer.integration.ui.ScreenTestBase
import org.bibletranslationtools.writer.ui.dialogs.feedback.FeedbackComponent
import org.bibletranslationtools.writer.ui.dialogs.source.SelectSourcesComponent
import org.bibletranslationtools.writer.ui.translate.TargetTranslationScreen
import org.bibletranslationtools.writer.ui.translate.TranslateComponent
import org.bibletranslationtools.writer.utils.getStringBlocking
import org.junit.Test
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class TargetTranslationScreenTest : ScreenTestBase() {

    @Test
    fun no_source_screen_displayed_when_resource_container_null() = runComposeUiTest {
        val component = FakeTranslateComponent()
        component.sharedState.value = component.sharedState.value.copy(
            resourceContainer = null,
            sourceTabs = emptyList()
        )
        component.state.value = component.state.value.copy(
            projectTitle = "Test Project Title"
        )

        setContent { TargetTranslationScreen(component) }

        onNodeWithText("Test Project Title").assertIsDisplayed()
        onNodeWithText(getStringBlocking(Res.string.choose_first_source_translation), substring = true).assertIsDisplayed()

        onNodeWithContentDescription("Edit Source").performClick()
        assertTrue(component.showSelectSourcesDialogCalled)
    }

    @Test
    fun clicking_sidebar_icons_triggers_correct_actions() = runComposeUiTest {
        val component = FakeTranslateComponent()
        component.sharedState.value = component.sharedState.value.copy(
            resourceContainer = mockk(relaxed = true),
            sourceTabs = emptyList()
        )

        setContent { TargetTranslationScreen(component) }

        onNodeWithContentDescription("Chunk Mode").performClick()
        assertTrue(component.openChunkModeCalled)

        onNodeWithContentDescription("Review Mode").performClick()
        assertTrue(component.openReviewModeCalledWith != null)

        onNodeWithContentDescription("Read Mode").performClick()
        assertTrue(component.openReadModeCalled)
    }

    @Test
    fun clicking_menu_items_triggers_correct_actions() = runComposeUiTest {
        val component = FakeTranslateComponent()
        component.sharedState.value = component.sharedState.value.copy(
            resourceContainer = mockk(relaxed = true),
            sourceTabs = emptyList()
        )

        setContent { TargetTranslationScreen(component) }

        onNodeWithContentDescription("More Options").performClick()
        onNodeWithText(getStringBlocking(Res.string.feedback)).performClick()
        assertTrue(component.showFeedbackDialogCalled)

        onNodeWithContentDescription("More Options").performClick()
        onNodeWithText(getStringBlocking(Res.string.action_settings)).performClick()
        assertTrue(component.openSettingsCalled)

        onNodeWithContentDescription("More Options").performClick()
        onNodeWithText(getStringBlocking(Res.string.action_translations)).performClick()
        assertTrue(component.openHomeCalledWith != null)
    }

    @Test
    fun feedback_dialog_displays_when_dialog_slot_has_feedback_child() = runComposeUiTest {
        val component = FakeTranslateComponent()
        component.sharedState.value = component.sharedState.value.copy(
            resourceContainer = mockk(relaxed = true)
        )

        val dialogComponent = mockk<FeedbackComponent>(relaxed = true) {
            every { state } returns MutableStateFlow(FeedbackComponent.State())
            every { progress } returns MutableStateFlow(null)
            every { initialMessage } returns ""
        }

        setContent { TargetTranslationScreen(component) }

        (component.dialogSlot as MutableValue).value = ChildSlot(
            child = Child.Created(
                configuration = TranslateComponent.DialogConfig.Feedback,
                instance = TranslateComponent.DialogChild.Feedback(dialogComponent)
            )
        )

        onNodeWithText(getStringBlocking(Res.string.feedback)).assertIsDisplayed()
    }

    @Test
    fun select_sources_dialog_displays_when_dialog_slot_has_select_sources_child() = runComposeUiTest {
        val component = FakeTranslateComponent()
        component.sharedState.value = component.sharedState.value.copy(
            resourceContainer = mockk(relaxed = true)
        )

        val dialogComponent = mockk<SelectSourcesComponent>(relaxed = true) {
            every { state } returns MutableStateFlow(SelectSourcesComponent.State())
            every { progress } returns MutableStateFlow(null)
        }

        setContent { TargetTranslationScreen(component) }

        (component.dialogSlot as MutableValue).value = ChildSlot(
            child = Child.Created(
                configuration = TranslateComponent.DialogConfig.SelectSources("test_id"),
                instance = TranslateComponent.DialogChild.SelectSources(dialogComponent)
            )
        )

        onNodeWithText(getStringBlocking(Res.string.choose_source_translations)).assertIsDisplayed()
    }
}
