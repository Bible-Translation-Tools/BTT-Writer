package org.bibletranslationtools.writer.integration.ui.dialogs

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import btt_writer.shared.generated.resources.Res
import btt_writer.shared.generated.resources.dismiss
import btt_writer.shared.generated.resources.label_continue
import btt_writer.shared.generated.resources.privacy_notice
import btt_writer.shared.generated.resources.title_cancel
import org.bibletranslationtools.writer.integration.ui.ScreenTestBase
import org.bibletranslationtools.writer.ui.dialogs.PrivacyNoticeDialog
import org.bibletranslationtools.writer.utils.getStringBlocking
import org.junit.Test
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class PrivacyNoticeDialogTest : ScreenTestBase() {

    @Test
    fun shows_privacy_notice_title() = runComposeUiTest {
        setContent { PrivacyNoticeDialog(onDismissRequest = {}) }

        onNodeWithText(getStringBlocking(Res.string.privacy_notice), substring = true).assertIsDisplayed()
    }

    @Test
    fun with_confirm_shows_continue_and_cancel_buttons() = runComposeUiTest {
        setContent { PrivacyNoticeDialog(onConfirm = {}, onDismissRequest = {}) }

        onNodeWithText(getStringBlocking(Res.string.label_continue), substring = true).assertIsDisplayed()
        onNodeWithText(getStringBlocking(Res.string.title_cancel), substring = true).assertIsDisplayed()
    }

    @Test
    fun clicking_continue_calls_onConfirm() = runComposeUiTest {
        var confirmed = false
        setContent {
            PrivacyNoticeDialog(
                onConfirm = { confirmed = true },
                onDismissRequest = {}
            )
        }

        onNodeWithText(getStringBlocking(Res.string.label_continue), substring = true).performClick()

        assertTrue(confirmed)
    }

    @Test
    fun without_confirm_shows_dismiss_button() = runComposeUiTest {
        setContent { PrivacyNoticeDialog(onDismissRequest = {}) }

        onNodeWithText(getStringBlocking(Res.string.dismiss), substring = true).assertIsDisplayed()
    }

    @Test
    fun clicking_dismiss_calls_onDismissRequest() = runComposeUiTest {
        var dismissed = false
        setContent { PrivacyNoticeDialog(onDismissRequest = { dismissed = true }) }

        onNodeWithText(getStringBlocking(Res.string.dismiss), substring = true).performClick()

        assertTrue(dismissed)
    }
}
