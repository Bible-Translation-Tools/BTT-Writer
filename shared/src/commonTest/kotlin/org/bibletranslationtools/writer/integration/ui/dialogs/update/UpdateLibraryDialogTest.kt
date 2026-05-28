package org.bibletranslationtools.writer.integration.ui.dialogs.update

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import btt_writer.shared.generated.resources.Res
import btt_writer.shared.generated.resources.check_app_update
import btt_writer.shared.generated.resources.download_index
import btt_writer.shared.generated.resources.download_sources
import btt_writer.shared.generated.resources.import_index
import btt_writer.shared.generated.resources.title_cancel
import btt_writer.shared.generated.resources.update_languages
import btt_writer.shared.generated.resources.update_options
import btt_writer.shared.generated.resources.update_source
import org.bibletranslationtools.writer.core.Progress
import org.bibletranslationtools.writer.integration.ui.ScreenTestBase
import org.bibletranslationtools.writer.ui.dialogs.update.UpdateLibraryDialog
import org.bibletranslationtools.writer.utils.getStringBlocking
import org.junit.Test
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class UpdateLibraryDialogTest : ScreenTestBase() {

    @Test
    fun update_options_displayed() = runComposeUiTest {
        val component = FakeUpdateLibraryComponent()
        setContent { UpdateLibraryDialog(component, onDismiss = {}) }

        onNodeWithText(getStringBlocking(Res.string.update_options), substring = true).assertIsDisplayed()
        onNodeWithText(getStringBlocking(Res.string.update_source), substring = true).assertIsDisplayed()
        onNodeWithText(getStringBlocking(Res.string.import_index), substring = true).assertIsDisplayed()
        onNodeWithText(getStringBlocking(Res.string.download_index), substring = true).assertIsDisplayed()
        onNodeWithText(getStringBlocking(Res.string.download_sources), substring = true).assertIsDisplayed()
        onNodeWithText(getStringBlocking(Res.string.update_languages), substring = true).assertIsDisplayed()
        onNodeWithText(getStringBlocking(Res.string.check_app_update), substring = true).assertIsDisplayed()
    }

    @Test
    fun cancel_button_triggers_onDismiss() = runComposeUiTest {
        val component = FakeUpdateLibraryComponent()
        var dismissed = false
        setContent { UpdateLibraryDialog(component, onDismiss = { dismissed = true }) }

        onNodeWithText(getStringBlocking(Res.string.title_cancel), substring = true).performClick()

        assertTrue(dismissed)
    }

    @Test
    fun clicking_update_source_calls_updateSources() = runComposeUiTest {
        val component = FakeUpdateLibraryComponent()
        setContent { UpdateLibraryDialog(component, onDismiss = {}) }

        onNodeWithText(getStringBlocking(Res.string.update_source), substring = true).performClick()

        assertTrue(component.updateSourcesCalled)
    }

    @Test
    fun clicking_update_languages_calls_updateLanguages() = runComposeUiTest {
        val component = FakeUpdateLibraryComponent()
        setContent { UpdateLibraryDialog(component, onDismiss = {}) }

        onNodeWithText(getStringBlocking(Res.string.update_languages), substring = true).performClick()

        assertTrue(component.updateLanguagesCalled)
    }

    @Test
    fun progress_shown_when_not_null() = runComposeUiTest {
        val component = FakeUpdateLibraryComponent()
        component.progress.value = Progress(message = "Updating sources...")
        setContent { UpdateLibraryDialog(component, onDismiss = {}) }

        onNodeWithText("Updating sources...").assertIsDisplayed()
    }
}
