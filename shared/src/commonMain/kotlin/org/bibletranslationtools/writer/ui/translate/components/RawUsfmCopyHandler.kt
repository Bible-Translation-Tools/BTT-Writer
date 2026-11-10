package org.bibletranslationtools.writer.ui.translate.components

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Makes copy/cut from the USFM editor put the raw text (with `\f ... \f*` footnote blocks)
 * on the clipboard instead of the displayed placeholder characters.
 *
 * [content] receives a modifier that must be applied to the text field.
 */
@Composable
internal expect fun RawUsfmCopyHandler(
    textFieldState: TextFieldState,
    content: @Composable (Modifier) -> Unit
)
