package org.bibletranslationtools.writer.ui.dialogs

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import btt_writer.composeapp.generated.resources.Res
import btt_writer.composeapp.generated.resources.confirm
import btt_writer.composeapp.generated.resources.title_cancel
import org.jetbrains.compose.resources.stringResource

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    confirmText: String = stringResource(Res.string.confirm),
    dismissText: String = stringResource(Res.string.title_cancel)
) {
    ConfirmDialog(
        title = title,
        message = AnnotatedString(message),
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        modifier = modifier,
        confirmText = confirmText,
        dismissText = dismissText
    )
}

@Composable
fun ConfirmDialog(
    title: String,
    message: AnnotatedString,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    confirmText: String = stringResource(Res.string.confirm),
    dismissText: String = stringResource(Res.string.title_cancel)
) {
    BaseDialog(
        onDismiss = onDismiss,
        modifier = modifier,
        title = title,
        message = message
    ) {
        Row {
            TextButton(onClick = onDismiss) {
                Text(text = dismissText)
            }
            TextButton(onClick = onConfirm) {
                Text(text = confirmText)
            }
        }
    }
}