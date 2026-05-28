package org.bibletranslationtools.writer.integration.ui.dialogs.source

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import btt_writer.shared.generated.resources.Res
import btt_writer.shared.generated.resources.choose_source_translations
import btt_writer.shared.generated.resources.confirm
import btt_writer.shared.generated.resources.title_cancel
import btt_writer.shared.generated.resources.update_sources_label
import org.bibletranslationtools.writer.core.Progress
import org.bibletranslationtools.writer.integration.ui.ScreenTestBase
import org.bibletranslationtools.writer.ui.dialogs.source.SourceSelectionDialog
import org.bibletranslationtools.writer.utils.getStringBlocking
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class SourceSelectionDialogTest : ScreenTestBase() {

    @Test
    fun search_field_and_buttons_displayed() = runComposeUiTest {
        val component = FakeSelectSourcesComponent()
        setContent { SourceSelectionDialog(component, onDismiss = {}) }

        onNodeWithText(getStringBlocking(Res.string.choose_source_translations), substring = true).assertIsDisplayed()
        onNodeWithText(getStringBlocking(Res.string.title_cancel), substring = true).assertIsDisplayed()
        onNodeWithText(getStringBlocking(Res.string.confirm), substring = true).assertIsDisplayed()
        onNodeWithText(getStringBlocking(Res.string.update_sources_label), substring = true).assertIsDisplayed()
    }

    @Test
    fun dismiss_button_triggers_onDismiss() = runComposeUiTest {
        val component = FakeSelectSourcesComponent()
        var dismissed = false
        setContent { SourceSelectionDialog(component, onDismiss = { dismissed = true }) }

        onNodeWithText(getStringBlocking(Res.string.title_cancel), substring = true).performClick()

        waitUntil(timeoutMillis = 1000) { dismissed }
    }

    @Test
    fun progress_shown_when_not_null() = runComposeUiTest {
        val component = FakeSelectSourcesComponent()
        component.progress.value = Progress(message = "Downloading sources...")
        setContent { SourceSelectionDialog(component, onDismiss = {}) }

        onNodeWithText("Downloading sources...").assertIsDisplayed()
    }
}
