package org.bibletranslationtools.writer.integration.ui.dialogs.import

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import btt_writer.shared.generated.resources.Res
import btt_writer.shared.generated.resources.import_from_backup
import btt_writer.shared.generated.resources.import_from_door43
import btt_writer.shared.generated.resources.import_project_file
import btt_writer.shared.generated.resources.import_source_text
import btt_writer.shared.generated.resources.import_usfm_file
import btt_writer.shared.generated.resources.title_cancel
import org.bibletranslationtools.writer.core.Progress
import org.bibletranslationtools.writer.integration.ui.ScreenTestBase
import org.bibletranslationtools.writer.ui.dialogs.import.ImportDialog
import org.bibletranslationtools.writer.utils.getStringBlocking
import org.junit.Test
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class ImportDialogTest : ScreenTestBase() {

    @Test
    fun import_options_displayed() = runComposeUiTest {
        val component = FakeImportComponent()
        setContent { ImportDialog(component, onDismiss = {}) }

        onNodeWithText(getStringBlocking(Res.string.import_from_door43), substring = true).assertIsDisplayed()
        onNodeWithText(getStringBlocking(Res.string.import_project_file), substring = true).assertIsDisplayed()
        onNodeWithText(getStringBlocking(Res.string.import_usfm_file), substring = true).assertIsDisplayed()
        onNodeWithText(getStringBlocking(Res.string.import_source_text), substring = true).assertIsDisplayed()
        onNodeWithText(getStringBlocking(Res.string.import_from_backup), substring = true).assertIsDisplayed()
    }

    @Test
    fun cancel_button_triggers_onDismiss() = runComposeUiTest {
        val component = FakeImportComponent()
        var dismissed = false
        setContent { ImportDialog(component, onDismiss = { dismissed = true }) }

        onNodeWithText(getStringBlocking(Res.string.title_cancel), substring = true).performClick()

        assertTrue(dismissed)
    }

    @Test
    fun progress_shown_when_not_null() = runComposeUiTest {
        val component = FakeImportComponent()
        component.progress.value = Progress(message = "Importing file...")
        setContent { ImportDialog(component, onDismiss = {}) }

        onNodeWithText("Importing file...").assertIsDisplayed()
    }
}
