package org.bibletranslationtools.writer.integration.ui.profile

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.v2.runComposeUiTest
import btt_writer.shared.generated.resources.Res
import btt_writer.shared.generated.resources.dismiss
import btt_writer.shared.generated.resources.label_continue
import btt_writer.shared.generated.resources.names_will_be_public
import btt_writer.shared.generated.resources.privacy_notice
import btt_writer.shared.generated.resources.title_cancel
import btt_writer.shared.generated.resources.your_name
import org.bibletranslationtools.writer.integration.ui.ScreenTestBase
import org.bibletranslationtools.writer.ui.profile.LoginOfflineScreen
import org.bibletranslationtools.writer.utils.getStringBlocking
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class LoginOfflineScreenTest : ScreenTestBase() {

    @Test
    fun name_field_and_buttons_displayed() = runComposeUiTest {
        val component = FakeLoginOfflineComponent()
        setContent { LoginOfflineScreen(component) }
        onNodeWithText(getStringBlocking(Res.string.your_name), substring = true).assertIsDisplayed()
        onNodeWithText(getStringBlocking(Res.string.title_cancel), substring = true).assertIsDisplayed()
        onNodeWithText(getStringBlocking(Res.string.label_continue), substring = true).assertIsDisplayed()
    }

    @Test
    fun clicking_cancel_calls_onCancel() = runComposeUiTest {
        val component = FakeLoginOfflineComponent()
        setContent { LoginOfflineScreen(component) }
        onNodeWithText(getStringBlocking(Res.string.title_cancel), substring = true).performClick()
        assertTrue(component.onCancelCalled)
    }

    @Test
    fun submitting_valid_name_shows_privacy_dialog_and_continue_submits() = runComposeUiTest {
        val component = FakeLoginOfflineComponent()
        setContent { LoginOfflineScreen(component) }

        onNode(hasSetTextAction()).performTextInput("John Doe")
        onNodeWithText(getStringBlocking(Res.string.label_continue), substring = true).performClick()

        onNodeWithText(getStringBlocking(Res.string.privacy_notice), substring = true).assertIsDisplayed()

        // Use onLast() to click dialog's Continue instead of screen's
        onAllNodes(hasText(getStringBlocking(Res.string.label_continue), substring = true)).onLast().performClick()

        assertTrue(component.onContinueCalled)
        assertEquals("John Doe", component.lastFullName)
    }

    @Test
    fun clicking_info_icon_shows_privacy_info_dialog() = runComposeUiTest {
        val component = FakeLoginOfflineComponent()
        setContent { LoginOfflineScreen(component) }

        onNodeWithText(getStringBlocking(Res.string.names_will_be_public), substring = true).performClick()

        onNodeWithText(getStringBlocking(Res.string.privacy_notice), substring = true).assertIsDisplayed()

        onNodeWithText(getStringBlocking(Res.string.dismiss), substring = true).performClick()

        onNodeWithText(getStringBlocking(Res.string.privacy_notice), substring = true).assertDoesNotExist()
    }
}
