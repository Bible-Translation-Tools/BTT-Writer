package org.bibletranslationtools.writer.integration.ui.draft

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import btt_writer.shared.generated.resources.Res
import btt_writer.shared.generated.resources.import_draft
import btt_writer.shared.generated.resources.import_draft_confirmation
import btt_writer.shared.generated.resources.label_import
import btt_writer.shared.generated.resources.preview
import btt_writer.shared.generated.resources.title_cancel
import io.mockk.mockk
import org.bibletranslationtools.resourcecatalog.library.models.Translation
import org.bibletranslationtools.writer.core.Progress
import org.bibletranslationtools.writer.integration.ui.ScreenTestBase
import org.bibletranslationtools.writer.ui.draft.DraftScreen
import org.bibletranslationtools.writer.utils.getStringBlocking
import org.junit.Test
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class DraftScreenTest : ScreenTestBase() {

    @Test
    fun empty_draft_translations_calls_onFinish() = runComposeUiTest {
        val component = FakeDraftComponent()
        setContent { DraftScreen(component) }

        assertTrue(component.onFinishCalled)
    }

    @Test
    fun title_and_back_button_displayed() = runComposeUiTest {
        val component = FakeDraftComponent()
        val translation = mockk<Translation>(relaxed = true)
        component.state.value = component.state.value.copy(draftTranslations = listOf(translation))

        setContent { DraftScreen(component) }

        onNodeWithText(getStringBlocking(Res.string.preview), substring = true).assertIsDisplayed()

        onNodeWithContentDescription("back").performClick()
        assertTrue(component.onNavigateBackCalled)
    }

    @Test
    fun clicking_fab_shows_import_dialog_and_confirm_imports() = runComposeUiTest {
        val component = FakeDraftComponent()
        val translation = mockk<Translation>(relaxed = true)
        component.state.value = component.state.value.copy(draftTranslations = listOf(translation))

        setContent { DraftScreen(component) }

        onNodeWithContentDescription("Import Draft").performClick()

        onNodeWithText(getStringBlocking(Res.string.import_draft), substring = true).assertIsDisplayed()
        onNodeWithText(getStringBlocking(Res.string.import_draft_confirmation), substring = true).assertIsDisplayed()

        // Exact match to target the confirm button instead of the "Import Draft" FAB
        onNodeWithText(getStringBlocking(Res.string.label_import)).performClick()

        assertTrue(component.importDraftCalledWith != null)
    }

    @Test
    fun clicking_cancel_in_import_dialog_does_not_import() = runComposeUiTest {
        val component = FakeDraftComponent()
        val translation = mockk<Translation>(relaxed = true)
        component.state.value = component.state.value.copy(draftTranslations = listOf(translation))

        setContent { DraftScreen(component) }

        onNodeWithContentDescription("Import Draft").performClick()
        onNodeWithText(getStringBlocking(Res.string.title_cancel), substring = true).performClick()

        assertNull(component.importDraftCalledWith)
    }

    @Test
    fun progress_dialog_shown_when_progress_not_null() = runComposeUiTest {
        val component = FakeDraftComponent()
        val translation = mockk<Translation>(relaxed = true)
        component.state.value = component.state.value.copy(draftTranslations = listOf(translation))
        component.progress.value = Progress(message = "Importing draft...")

        setContent { DraftScreen(component) }

        onNodeWithText("Importing draft...").assertIsDisplayed()
    }
}
