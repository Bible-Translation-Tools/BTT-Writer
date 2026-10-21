package org.bibletranslationtools.writer.ui.translate.components

import androidx.compose.foundation.text.contextmenu.builder.item
import androidx.compose.foundation.text.contextmenu.data.TextContextMenuKeys
import androidx.compose.foundation.text.contextmenu.modifier.appendTextContextMenuComponents
import androidx.compose.foundation.text.contextmenu.modifier.filterTextContextMenuComponents
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.launch
import org.bibletranslationtools.writer.textClipEntry

private object RawCopyKey
private object RawCutKey

/**
 * Foundation 1.12 requires [LocalClipboard] to be the platform implementation, so the clipboard
 * cannot be wrapped. Instead, the default Copy/Cut toolbar items are replaced with ones that read
 * the untransformed text from [textFieldState].
 */
@Composable
internal actual fun RawUsfmCopyHandler(
    textFieldState: TextFieldState,
    content: @Composable (Modifier) -> Unit
) {
    val clipboard = LocalClipboard.current
    val cutLabel = stringResource(android.R.string.cut)
    val copyLabel = stringResource(android.R.string.copy)
    val scope = rememberCoroutineScope()

    fun copySelection() {
        val selection = textFieldState.selection
        val raw = textFieldState.text.substring(selection.min, selection.max)
        scope.launch { clipboard.setClipEntry(textClipEntry(raw)) }
    }

    content(
        Modifier
            .filterTextContextMenuComponents {
                it.key != TextContextMenuKeys.CopyKey && it.key != TextContextMenuKeys.CutKey
            }
            .appendTextContextMenuComponents {
                val selection = textFieldState.selection
                if (selection.collapsed) return@appendTextContextMenuComponents
                item(RawCutKey, cutLabel) {
                    copySelection()
                    textFieldState.edit { replace(selection.min, selection.max, "") }
                    close()
                }
                item(RawCopyKey, copyLabel) {
                    copySelection()
                    close()
                }
            }
    )
}
