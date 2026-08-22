package org.bibletranslationtools.writer.integration.ui.dialogs.export

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import btt_writer.shared.generated.resources.Res
import btt_writer.shared.generated.resources.backup_to_app
import btt_writer.shared.generated.resources.backup_to_door43
import btt_writer.shared.generated.resources.backup_to_sd
import btt_writer.shared.generated.resources.dismiss
import btt_writer.shared.generated.resources.export_to_pdf
import btt_writer.shared.generated.resources.title_upload_export
import org.bibletranslationtools.writer.core.Progress
import org.bibletranslationtools.writer.integration.ui.ScreenTestBase
import org.bibletranslationtools.writer.ui.dialogs.export.ExportDialog
import org.bibletranslationtools.writer.utils.getStringBlocking
import org.junit.Test
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class ExportDialogTest : ScreenTestBase() {

    @Test
    fun export_options_displayed() = runComposeUiTest {
        val component = FakeExportComponent()
        setContent { ExportDialog(component, onDismiss = {}) }

        onNodeWithText(getStringBlocking(Res.string.title_upload_export), substring = true).assertIsDisplayed()
        onNodeWithText(getStringBlocking(Res.string.backup_to_door43), substring = true).assertIsDisplayed()
        onNodeWithText(getStringBlocking(Res.string.backup_to_sd), substring = true).assertIsDisplayed()
        onNodeWithText(getStringBlocking(Res.string.export_to_pdf), substring = true).assertIsDisplayed()
        onNodeWithText(getStringBlocking(Res.string.backup_to_app), substring = true).assertIsDisplayed()
    }

    @Test
    fun dismiss_button_triggers_onDismiss() = runComposeUiTest {
        val component = FakeExportComponent()
        var dismissed = false
        setContent { ExportDialog(component, onDismiss = { dismissed = true }) }

        onNodeWithText(getStringBlocking(Res.string.dismiss), substring = true).performClick()

        assertTrue(dismissed)
    }

    @Test
    fun progress_shown_when_not_null() = runComposeUiTest {
        val component = FakeExportComponent()
        component.progress.value = Progress(message = "Exporting project...")
        setContent { ExportDialog(component, onDismiss = {}) }

        onNodeWithText("Exporting project...").assertIsDisplayed()
    }
}
