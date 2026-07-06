package org.bibletranslationtools.writer.integration.ui.devtools

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import btt_writer.shared.generated.resources.Res
import btt_writer.shared.generated.resources.title_activity_developer
import org.bibletranslationtools.writer.integration.ui.ScreenTestBase
import org.bibletranslationtools.writer.ui.devtools.DevToolsScreen
import org.bibletranslationtools.writer.ui.devtools.ToolItem
import org.bibletranslationtools.writer.utils.getStringBlocking
import org.junit.Test
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class DevToolsScreenTest : ScreenTestBase() {

    @Test
    fun loads_tools_and_displays_meta_info() = runComposeUiTest {
        val component = FakeDevToolsComponent()
        setContent { DevToolsScreen(component) }

        assertTrue(component.loadToolsCalled)
        onNodeWithText(getStringBlocking(Res.string.title_activity_developer), substring = true).assertIsDisplayed()
        onNodeWithText("1.0.0-mock", substring = true).assertIsDisplayed()
        onNodeWithText("mock-udid-1234", substring = true).assertIsDisplayed()
    }

    @Test
    fun clicking_back_calls_navigateBack() = runComposeUiTest {
        val component = FakeDevToolsComponent()
        setContent { DevToolsScreen(component) }

        onNodeWithContentDescription("back").performClick()
        assertTrue(component.navigateBackCalled)
    }

    @Test
    fun clicking_tool_item_triggers_action() = runComposeUiTest {
        val component = FakeDevToolsComponent()
        var actionTriggered = false
        val tool = ToolItem(
            name = "Click Me",
            description = "This is a test tool",
            action = { actionTriggered = true }
        )
        component.state.value = component.state.value.copy(tools = listOf(tool))

        setContent { DevToolsScreen(component) }

        onNodeWithText("Click Me", substring = true).performClick()
        assertTrue(actionTriggered)
    }
}
