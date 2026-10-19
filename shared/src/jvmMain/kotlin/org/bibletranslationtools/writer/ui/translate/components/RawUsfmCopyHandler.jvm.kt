package org.bibletranslationtools.writer.ui.translate.components

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.LocalClipboard
import org.bibletranslationtools.writer.label
import org.bibletranslationtools.writer.textClipEntry
import org.bibletranslationtools.writer.textOrNull
import org.bibletranslationtools.writer.ui.translate.components.footnote.NOTE_CHAR
import org.bibletranslationtools.writer.ui.translate.components.footnote.visualToRaw

/**
 * Desktop wraps the clipboard so both the context menu and keyboard shortcuts copy raw USFM.
 */
@Composable
internal actual fun RawUsfmCopyHandler(
    textFieldState: TextFieldState,
    content: @Composable (Modifier) -> Unit
) {
    val platformClipboard = LocalClipboard.current
    val rawClipboard = remember(platformClipboard) {
        RawUsfmClipboard(platformClipboard, textFieldState)
    }
    CompositionLocalProvider(LocalClipboard provides rawClipboard) {
        content(Modifier)
    }
}

/**
 * Clipboard wrapper that intercepts copy/cut operations to preserve raw USFM
 * footnote blocks instead of the visual placeholder characters.
 */
private class RawUsfmClipboard(
    private val delegate: Clipboard,
    private val textFieldState: TextFieldState
) : Clipboard {

    override suspend fun setClipEntry(clipEntry: ClipEntry?) {
        if (clipEntry == null) {
            delegate.setClipEntry(null)
            return
        }

        val visualText = clipEntry.textOrNull()
        if (visualText != null && NOTE_CHAR in visualText) {
            val rawText = textFieldState.text.toString()
            val reconstructed = visualToRaw(visualText, rawText)
            delegate.setClipEntry(textClipEntry(reconstructed, clipEntry.label))
            return
        }

        delegate.setClipEntry(clipEntry)
    }

    override suspend fun getClipEntry(): ClipEntry? = delegate.getClipEntry()
}
