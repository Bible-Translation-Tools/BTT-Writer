package org.bibletranslationtools.writer.integration.ui.dialogs.feedback

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import btt_writer.shared.generated.resources.Res
import btt_writer.shared.generated.resources.confirm
import btt_writer.shared.generated.resources.email_optional
import btt_writer.shared.generated.resources.feedback
import btt_writer.shared.generated.resources.title_cancel
import btt_writer.shared.generated.resources.upload_complete
import btt_writer.shared.generated.resources.upload_failed
import org.bibletranslationtools.writer.core.Progress
import org.bibletranslationtools.writer.integration.ui.ScreenTestBase
import org.bibletranslationtools.writer.ui.dialogs.feedback.FeedbackDialog
import org.bibletranslationtools.writer.utils.getStringBlocking
import org.junit.Test
import kotlin.test.assertNotNull

@OptIn(ExperimentalTestApi::class)
class FeedbackDialogTest : ScreenTestBase() {

    @Test
    fun title_and_fields_displayed() = runComposeUiTest {
        val component = FakeFeedbackComponent()
        setContent { FeedbackDialog(component, onDismiss = {}) }

        onNodeWithText(getStringBlocking(Res.string.feedback), substring = true).assertIsDisplayed()
        onNodeWithText(getStringBlocking(Res.string.email_optional), substring = true).assertIsDisplayed()
        onNodeWithText(getStringBlocking(Res.string.title_cancel), substring = true).assertIsDisplayed()
        onNodeWithText(getStringBlocking(Res.string.confirm), substring = true).assertIsDisplayed()
    }

    @Test
    fun confirm_calls_reportBug() = runComposeUiTest {
        val component = FakeFeedbackComponent()
        setContent { FeedbackDialog(component, onDismiss = {}) }

        onNodeWithText(getStringBlocking(Res.string.confirm), substring = true).performClick()

        assertNotNull(component.reportBugCalledWith)
    }

    @Test
    fun progress_shown_when_not_null() = runComposeUiTest {
        val component = FakeFeedbackComponent()
        component.progress.value = Progress(message = "Uploading feedback...")
        setContent { FeedbackDialog(component, onDismiss = {}) }

        onNodeWithText("Uploading feedback...").assertIsDisplayed()
    }

    @Test
    fun success_dialog_shown_when_state_success() = runComposeUiTest {
        val component = FakeFeedbackComponent()
        component.state.value = component.state.value.copy(success = true)
        setContent { FeedbackDialog(component, onDismiss = {}) }

        onNodeWithText(getStringBlocking(Res.string.upload_complete), substring = true).assertIsDisplayed()
    }

    @Test
    fun upload_error_dialog_shown_when_state_has_error() = runComposeUiTest {
        val component = FakeFeedbackComponent()
        component.state.value = component.state.value.copy(uploadError = "Network error occurred")
        setContent { FeedbackDialog(component, onDismiss = {}) }

        onNodeWithText(getStringBlocking(Res.string.upload_failed), substring = true).assertIsDisplayed()
        onNodeWithText("Network error occurred", substring = true).assertIsDisplayed()
    }
}
