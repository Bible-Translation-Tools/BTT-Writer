package org.bibletranslationtools.writer.integration.ui.newtranslation

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.v2.runComposeUiTest
import btt_writer.shared.generated.resources.Res
import btt_writer.shared.generated.resources.title_activity_new_target_translation
import org.bibletranslationtools.writer.integration.ui.ScreenTestBase
import org.bibletranslationtools.writer.ui.newtranslation.NewTargetTranslationScreen
import org.bibletranslationtools.writer.ui.newtranslation.ScreenStep
import org.bibletranslationtools.writer.utils.getStringBlocking
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class NewTranslationScreenTest : ScreenTestBase() {

    @Test
    fun title_and_languages_list_displayed() = runComposeUiTest {
        val component = FakeNewTranslationComponent()
        setContent { NewTargetTranslationScreen(component) }

        onNodeWithText(getStringBlocking(Res.string.title_activity_new_target_translation), substring = true).assertIsDisplayed()
    }

    @Test
    fun clicking_back_in_language_step_calls_navigateBack() = runComposeUiTest {
        val component = FakeNewTranslationComponent()
        component.state.value = component.state.value.copy(screenStep = ScreenStep.LANGUAGE)
        setContent { NewTargetTranslationScreen(component) }

        onNodeWithContentDescription("back").performClick()
        assertTrue(component.navigateBackCalled)
    }

    @Test
    fun clicking_back_in_project_step_calls_onCategoryBack() = runComposeUiTest {
        val component = FakeNewTranslationComponent()
        component.state.value = component.state.value.copy(screenStep = ScreenStep.PROJECT)
        setContent { NewTargetTranslationScreen(component) }

        onNodeWithContentDescription("back").performClick()
        assertTrue(component.onCategoryBackCalled)
    }

    @Test
    fun typing_in_search_bar_calls_onSearch() = runComposeUiTest {
        val component = FakeNewTranslationComponent()
        setContent { NewTargetTranslationScreen(component) }

        onNode(hasSetTextAction()).performTextInput("English")
        assertEquals("English", component.onSearchCalledWith)
    }
}
