package org.bibletranslationtools.writer.ui.translate.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlin.time.Duration.Companion.milliseconds

/**
 * A borderless text field for a single helps entry (title or body).
 * Keeps local state so the cursor survives the save round trip.
 */
@OptIn(FlowPreview::class)
@Composable
fun HelpEntryField(
    value: String,
    placeholder: String,
    readOnly: Boolean,
    textStyle: TextStyle,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    lines: Boolean = false
) {
    val textFieldState = remember { TextFieldState(value) }
    var lastEmittedText by remember { mutableStateOf(value) }
    val currentOnTextChange by rememberUpdatedState(onValueChange)
    val focusManager = LocalFocusManager.current

    val density = LocalDensity.current
    val lineHeightPx = with(density) { textStyle.lineHeight.toPx() }
    val lineColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
    var textLayout by remember { mutableStateOf<TextLayoutResult?>(null) }

    if (readOnly) {
        if (value.isNotEmpty()) {
            Text(
                text = value,
                style = textStyle,
                modifier = modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
        return
    }

    // Sync external value changes (reorder, delete, undo) into the field.
    // Own edits echo back with value == lastEmittedText and are skipped,
    // so the cursor is never disturbed while typing.
    LaunchedEffect(value) {
        if (value != lastEmittedText && value != textFieldState.text.toString()) {
            lastEmittedText = value
            textFieldState.setTextAndPlaceCursorAtEnd(value)
        }
    }

    // Observe TextFieldState changes and notify parent
    LaunchedEffect(Unit) {
        snapshotFlow { textFieldState.text.toString() }
            .distinctUntilChanged()
            .debounce(500L.milliseconds)
            .collect { newRaw ->
                if (newRaw != lastEmittedText) {
                    lastEmittedText = newRaw
                    currentOnTextChange(newRaw)
                }
            }
    }

    BasicTextField(
        state = textFieldState,
        textStyle = textStyle,
        cursorBrush = SolidColor(textStyle.color),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        onKeyboardAction = { focusManager.moveFocus(FocusDirection.Next) },
        onTextLayout = { getResult -> textLayout = getResult() },
        decorator = { innerTextField ->
            Box(contentAlignment = Alignment.TopStart) {
                if (textFieldState.text.isEmpty()) {
                    Text(
                        text = placeholder,
                        color = MaterialTheme.colorScheme.outline,
                        style = textStyle
                    )
                }
                innerTextField()
            }
        },
        modifier = modifier.fillMaxWidth()
            // Tab / Shift+Tab move between fields instead of inserting \t
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && event.key == Key.Tab) {
                    if (event.isShiftPressed) {
                        focusManager.moveFocus(FocusDirection.Previous)
                        true
                    } else {
                        focusManager.moveFocus(FocusDirection.Next)
                        true
                    }
                } else false
            }
            .drawWithContent {
                drawContent()

                if (lines) {
                    val strokeWidth = 1.dp.toPx()
                    val topPadding = 8.dp.toPx()
                    val layout = textLayout

                    var y = topPadding
                    var step = lineHeightPx
                    if (layout != null && layout.lineCount > 0) {
                        for (i in 0 until layout.lineCount) {
                            y = topPadding + layout.getLineBottom(i)
                            step = layout.getLineBottom(i) - layout.getLineTop(i)
                            drawLine(
                                color = lineColor,
                                start = Offset(0f, y),
                                end = Offset(size.width, y),
                                strokeWidth = strokeWidth
                            )
                        }
                    }
                    if (step > 0f) {
                        y += step
                        while (y <= size.height) {
                            drawLine(
                                color = lineColor,
                                start = Offset(0f, y),
                                end = Offset(size.width, y),
                                strokeWidth = strokeWidth
                            )
                            y += step
                        }
                    }
                }
            }
            .padding(horizontal = 16.dp, vertical = 8.dp)
    )
}
