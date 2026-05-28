package org.bibletranslationtools.writer.integration.ui.crash

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.v2.runComposeUiTest
import btt_writer.shared.generated.resources.Res
import btt_writer.shared.generated.resources.crash_details
import btt_writer.shared.generated.resources.email_optional
import btt_writer.shared.generated.resources.label_close
import btt_writer.shared.generated.resources.label_continue
import btt_writer.shared.generated.resources.title_cancel
import btt_writer.shared.generated.resources.title_upload
import kotlinx.coroutines.flow.MutableStateFlow
import org.bibletranslationtools.writer.core.Progress
import org.bibletranslationtools.writer.integration.ui.ScreenTestBase
import org.bibletranslationtools.writer.ui.crash.CrashReporterScreen
import org.bibletranslationtools.writer.utils.getStringBlocking
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class CrashScreenTest : ScreenTestBase() {

    @Test
    fun text_fields_and_buttons_displayed() = runComposeUiTest {
        val component = FakeCrashComponent()
        setContent { CrashReporterScreen(component) }
        onNodeWithText(getStringBlocking(Res.string.email_optional), substring = true).assertIsDisplayed()
        onNodeWithText(getStringBlocking(Res.string.crash_details), substring = true).assertIsDisplayed()
        onNodeWithText(getStringBlocking(Res.string.title_cancel), substring = true).assertIsDisplayed()
        onNodeWithText(getStringBlocking(Res.string.title_upload), substring = true).assertIsDisplayed()
    }

    @Test
    fun clicking_cancel_calls_flushAndRestart() = runComposeUiTest {
        val component = FakeCrashComponent()
        setContent { CrashReporterScreen(component) }
        onNodeWithText(getStringBlocking(Res.string.title_cancel), substring = true).performClick()
        assertTrue(component.flushAndRestartCalled)
    }

    @Test
    fun clicking_upload_shows_confirm_dialog_and_sends_report() = runComposeUiTest {
        val component = FakeCrashComponent()
        setContent { CrashReporterScreen(component) }

        onNodeWithText(getStringBlocking(Res.string.email_optional), substring = true).performTextInput("test@example.com")
        onNodeWithText(getStringBlocking(Res.string.crash_details), substring = true).performTextInput("App crashed on startup")

        onNodeWithText(getStringBlocking(Res.string.title_upload), substring = true).performClick()
        onNodeWithText(getStringBlocking(Res.string.label_continue), substring = true).performClick()

        assertTrue(component.sendCrashReportCalled)
        assertEquals("App crashed on startup", component.lastNotes)
        assertEquals("test@example.com", component.lastEmail)
    }

    @Test
    fun clicking_close_in_confirm_dialog_calls_flush_and_restart() = runComposeUiTest {
        val component = FakeCrashComponent()
        setContent { CrashReporterScreen(component) }

        onNodeWithText(getStringBlocking(Res.string.title_upload), substring = true).performClick()
        onNodeWithText(getStringBlocking(Res.string.label_close), substring = true).performClick()

        assertTrue(component.flushAndRestartCalled)
    }

    @Test
    fun progress_dialog_shown_when_progress_not_null() = runComposeUiTest {
        val component = FakeCrashComponent()
        (component.progress as MutableStateFlow<Progress?>).value = Progress(message = "Uploading report...")
        setContent { CrashReporterScreen(component) }

        onNodeWithText("Uploading report...").assertIsDisplayed()
    }
}
