package org.bibletranslationtools.writer.integration.ui.translate.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.test.withKeyDown
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import org.bibletranslationtools.writer.ui.translate.components.HelpEntryField
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class HelpEntryFieldFocusTest {

    @Test
    fun tabMovesFocusForward() = runComposeUiTest {
        setContent {
            MaterialTheme {
                Column {
                    HelpEntryField(
                        value = "first",
                        placeholder = "",
                        readOnly = false,
                        textStyle = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
                        onValueChange = {},
                        modifier = Modifier.testTag("field1")
                    )
                    HelpEntryField(
                        value = "second",
                        placeholder = "",
                        readOnly = false,
                        textStyle = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
                        onValueChange = {},
                        modifier = Modifier.testTag("field2"),
                        lines = true
                    )
                }
            }
        }

        onNodeWithTag("field1").performClick()
        onNodeWithTag("field1").assertIsFocused()

        onNodeWithTag("field1").performKeyInput { pressKey(Key.Tab) }
        onNodeWithTag("field2").assertIsFocused()
    }

    @Test
    fun shiftTabMovesFocusBack() = runComposeUiTest {
        setContent {
            MaterialTheme {
                Column {
                    HelpEntryField(
                        value = "first",
                        placeholder = "",
                        readOnly = false,
                        textStyle = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
                        onValueChange = {},
                        modifier = Modifier.testTag("field1")
                    )
                    HelpEntryField(
                        value = "second",
                        placeholder = "",
                        readOnly = false,
                        textStyle = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
                        onValueChange = {},
                        modifier = Modifier.testTag("field2"),
                        lines = true
                    )
                }
            }
        }

        onNodeWithTag("field2").performClick()
        onNodeWithTag("field2").assertIsFocused()

        onNodeWithTag("field2").performKeyInput {
            withKeyDown(Key.ShiftLeft) { pressKey(Key.Tab) }
        }
        onNodeWithTag("field1").assertIsFocused()
    }
}
