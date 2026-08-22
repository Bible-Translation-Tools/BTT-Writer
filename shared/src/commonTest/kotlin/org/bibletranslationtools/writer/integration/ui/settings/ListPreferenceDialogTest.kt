package org.bibletranslationtools.writer.integration.ui.settings

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.v2.runComposeUiTest
import org.bibletranslationtools.writer.integration.ui.ScreenTestBase
import org.bibletranslationtools.writer.ui.settings.ListPreferenceDialog
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class ListPreferenceDialogTest : ScreenTestBase() {

    private val entries = listOf("Charis SIL Regular", "Padauk", "Noto Sans")
    private val values = listOf("a.ttf", "b.ttf", "c.ttf")

    @Test
    fun searchable_filtersEntriesByQuery() = runComposeUiTest {
        setContent {
            ListPreferenceDialog(
                title = "Font",
                entries = entries,
                entryValues = values,
                selectedValue = "a.ttf",
                searchable = true,
                onValueSelected = {},
                onDismissRequest = {}
            )
        }

        onNode(hasSetTextAction()).performTextInput("padauk")

        onNodeWithText("Padauk").assertIsDisplayed()
        onNodeWithText("Charis SIL Regular").assertDoesNotExist()
        onNodeWithText("Noto Sans").assertDoesNotExist()
    }

    @Test
    fun scrollsToSelectedFont_onOpen() = runComposeUiTest {
        val many = (1..60).map { "Font $it" }
        val manyValues = (1..60).map { "f$it.ttf" }
        setContent {
            ListPreferenceDialog(
                title = "Font",
                entries = many,
                entryValues = manyValues,
                selectedValue = "f55.ttf",
                searchable = true,
                onValueSelected = {},
                onDismissRequest = {}
            )
        }

        onNodeWithText("Font 55").assertIsDisplayed()
    }

    @Test
    fun notSearchable_hasNoSearchField() = runComposeUiTest {
        setContent {
            ListPreferenceDialog(
                title = "Theme",
                entries = entries,
                entryValues = values,
                selectedValue = "a.ttf",
                onValueSelected = {},
                onDismissRequest = {}
            )
        }

        onNode(hasSetTextAction()).assertDoesNotExist()
        onNodeWithText("Charis SIL Regular").assertIsDisplayed()
    }
}
