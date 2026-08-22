package org.bibletranslationtools.writer.integration.ui.dialogs

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import btt_writer.shared.generated.resources.Res
import btt_writer.shared.generated.resources.action_save
import btt_writer.shared.generated.resources.dismiss
import btt_writer.shared.generated.resources.edit
import btt_writer.shared.generated.resources.footnote_label
import btt_writer.shared.generated.resources.label_delete
import btt_writer.shared.generated.resources.title_add_footnote
import btt_writer.shared.generated.resources.title_cancel
import org.bibletranslationtools.writer.integration.ui.ScreenTestBase
import org.bibletranslationtools.writer.ui.dialogs.FootnoteDialog
import org.bibletranslationtools.writer.ui.translate.FootnoteAction
import org.bibletranslationtools.writer.utils.getStringBlocking
import org.junit.Test
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class FootnoteDialogTest : ScreenTestBase() {

    @Test
    fun view_mode_shows_footnote_text_and_dismiss_button() = runComposeUiTest {
        setContent {
            FootnoteDialog(
                text = "This is a footnote.",
                action = FootnoteAction.VIEW,
                onDismissRequest = {}
            )
        }

        onNodeWithText(getStringBlocking(Res.string.footnote_label), substring = true).assertIsDisplayed()
        onNodeWithText("This is a footnote.", substring = true).assertIsDisplayed()
        onNodeWithText(getStringBlocking(Res.string.dismiss), substring = true).assertIsDisplayed()
    }

    @Test
    fun view_mode_dismiss_calls_onDismissRequest() = runComposeUiTest {
        var dismissed = false
        setContent {
            FootnoteDialog(
                text = "Some note",
                action = FootnoteAction.VIEW,
                onDismissRequest = { dismissed = true }
            )
        }

        onNodeWithText(getStringBlocking(Res.string.dismiss), substring = true).performClick()

        assertTrue(dismissed)
    }

    @Test
    fun actions_mode_shows_edit_delete_and_dismiss_buttons() = runComposeUiTest {
        setContent {
            FootnoteDialog(
                text = "Editable note",
                action = FootnoteAction.ACTIONS,
                onDismissRequest = {}
            )
        }

        onNodeWithText(getStringBlocking(Res.string.footnote_label), substring = true).assertIsDisplayed()
        onNodeWithText("Editable note", substring = true).assertIsDisplayed()
        onNodeWithText(getStringBlocking(Res.string.label_delete), substring = true).assertIsDisplayed()
        onNodeWithText(getStringBlocking(Res.string.edit)).assertIsDisplayed()
        onNodeWithText(getStringBlocking(Res.string.dismiss), substring = true).assertIsDisplayed()
    }

    @Test
    fun actions_mode_delete_calls_onDeleteNote() = runComposeUiTest {
        var deleted = false
        setContent {
            FootnoteDialog(
                text = "Note to delete",
                action = FootnoteAction.ACTIONS,
                onDismissRequest = {},
                onDeleteNote = { deleted = true }
            )
        }

        onNodeWithText(getStringBlocking(Res.string.label_delete), substring = true).performClick()

        assertTrue(deleted)
    }

    @Test
    fun edit_mode_shows_save_and_cancel_buttons() = runComposeUiTest {
        setContent {
            FootnoteDialog(
                text = "Existing note",
                action = FootnoteAction.EDIT,
                onDismissRequest = {}
            )
        }

        onNodeWithText(getStringBlocking(Res.string.title_add_footnote), substring = true).assertIsDisplayed()
        onNodeWithText(getStringBlocking(Res.string.action_save), substring = true).assertIsDisplayed()
        onNodeWithText(getStringBlocking(Res.string.title_cancel), substring = true).assertIsDisplayed()
    }
}
