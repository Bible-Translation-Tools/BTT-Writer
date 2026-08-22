package org.bibletranslationtools.writer.integration.ui.home

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import btt_writer.shared.generated.resources.Res
import btt_writer.shared.generated.resources.title_activity_target_translations
import btt_writer.shared.generated.resources.translations_welcome
import io.mockk.mockk
import org.bibletranslationtools.writer.integration.ui.ScreenTestBase
import org.bibletranslationtools.writer.ui.home.HomeComponent
import org.bibletranslationtools.writer.ui.home.HomeScreen
import org.bibletranslationtools.writer.utils.getStringBlocking
import org.junit.Test
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class HomeScreenTest : ScreenTestBase() {

    @Test
    fun shows_welcome_screen_when_no_translations() = runComposeUiTest {
        val component = FakeHomeComponent()
        component._state.value = HomeComponent.HomeState(translations = emptyList())
        setContent { HomeScreen(component) }
        onNodeWithText(getStringBlocking(Res.string.translations_welcome), substring = true).assertIsDisplayed()
    }

    @Test
    fun shows_translation_list_when_translations_present() = runComposeUiTest {
        val component = FakeHomeComponent()
        component._state.value = HomeComponent.HomeState(
            translations = listOf(mockk(relaxed = true))
        )
        setContent { HomeScreen(component) }
        onNodeWithText(getStringBlocking(Res.string.title_activity_target_translations), substring = true).assertIsDisplayed()
    }

    @Test
    fun add_button_is_displayed() = runComposeUiTest {
        val component = FakeHomeComponent()
        setContent { HomeScreen(component) }
        onNodeWithContentDescription("Add").assertIsDisplayed()
    }

    @Test
    fun clicking_add_button_calls_onNewTranslation() = runComposeUiTest {
        val component = FakeHomeComponent()
        setContent { HomeScreen(component) }
        onNodeWithContentDescription("Add").performClick()
        assertTrue(component.onNewTranslationCalled)
    }
}
