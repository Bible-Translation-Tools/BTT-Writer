package org.bibletranslationtools.writer.integration.ui.settings

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.test.v2.runComposeUiTest
import btt_writer.shared.generated.resources.Res
import btt_writer.shared.generated.resources.action_settings
import btt_writer.shared.generated.resources.check_for_updates
import btt_writer.shared.generated.resources.header_general
import btt_writer.shared.generated.resources.pref_title_check_hardware_requirements
import btt_writer.shared.generated.resources.pref_title_enable_tm_links
import btt_writer.shared.generated.resources.title_color_theme
import btt_writer.shared.generated.resources.title_developer_tools
import org.bibletranslationtools.writer.core.Progress
import org.bibletranslationtools.writer.integration.ui.ScreenTestBase
import org.bibletranslationtools.writer.ui.settings.SettingsScreen
import org.bibletranslationtools.writer.utils.getStringBlocking
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class SettingsScreenTest : ScreenTestBase() {

    @Test
    fun settings_elements_are_displayed() = runComposeUiTest {
        val component = FakeSettingsComponent()
        component.appVersion = "2.4.1"
        setContent { SettingsScreen(component) }

        onNodeWithText(getStringBlocking(Res.string.action_settings), substring = true).assertIsDisplayed()
        onNodeWithText(getStringBlocking(Res.string.header_general), substring = true).assertIsDisplayed()
        onNodeWithText(getStringBlocking(Res.string.title_color_theme), substring = true).assertIsDisplayed()
        onNodeWithText("2.4.1", substring = true).assertIsDisplayed()
    }

    @Test
    fun clicking_back_calls_onNavigateBack() = runComposeUiTest {
        val component = FakeSettingsComponent()
        setContent { SettingsScreen(component) }

        onNodeWithContentDescription("back").performClick()
        assertTrue(component.onNavigateBackCalled)
    }

    @Test
    fun clicking_check_for_updates_calls_checkForLatestRelease() = runComposeUiTest {
        val component = FakeSettingsComponent()
        setContent { SettingsScreen(component) }

        onNodeWithText(getStringBlocking(Res.string.check_for_updates), substring = true).performClick()
        assertTrue(component.checkForLatestReleaseCalled)
    }

    @Test
    fun toggling_check_hardware_requirements_calls_setCheckHardwareEnabled() = runComposeUiTest {
        val component = FakeSettingsComponent()
        setContent { SettingsScreen(component) }

        onNode(hasScrollAction()).performTouchInput { swipeUp() }

        onNodeWithText(getStringBlocking(Res.string.pref_title_check_hardware_requirements), substring = true).performClick()
        assertEquals(true, component.setCheckHardwareEnabledCalledWith)
    }

    @Test
    fun toggling_enable_tm_links_calls_setTmLinksEnabled() = runComposeUiTest {
        val component = FakeSettingsComponent()
        setContent { SettingsScreen(component) }

        onNode(hasScrollAction()).performTouchInput { swipeUp() }

        onNodeWithText(getStringBlocking(Res.string.pref_title_enable_tm_links), substring = true).performClick()
        assertEquals(true, component.setTmLinksEnabledCalledWith)
    }

    @Test
    fun clicking_developer_tools_calls_openDeveloperTools() = runComposeUiTest {
        val component = FakeSettingsComponent()
        setContent { SettingsScreen(component) }

        onNode(hasScrollAction()).performTouchInput {
            swipeUp()
            swipeUp()
        }

        onNodeWithText(getStringBlocking(Res.string.title_developer_tools), substring = true).performClick()
        assertTrue(component.openDeveloperToolsCalled)
    }

    @Test
    fun progress_dialog_shown_when_progress_not_null() = runComposeUiTest {
        val component = FakeSettingsComponent()
        component.progress.value = Progress(message = "Updating...")

        setContent { SettingsScreen(component) }

        onNodeWithText("Updating...").assertIsDisplayed()
    }
}
