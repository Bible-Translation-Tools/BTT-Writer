package org.bibletranslationtools.writer.ui.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import btt_writer.shared.generated.resources.Res
import btt_writer.shared.generated.resources.dismiss
import btt_writer.shared.generated.resources.label_continue
import btt_writer.shared.generated.resources.privacy_notice
import btt_writer.shared.generated.resources.publishing_privacy_notice
import btt_writer.shared.generated.resources.title_cancel
import org.jetbrains.compose.resources.stringResource

@Composable
fun PrivacyNoticeDialog(
    onConfirm: (() -> Unit)? = null,
    onDismissRequest: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(stringResource(Res.string.privacy_notice))
            }
        },
        text = {
            Text(
                stringResource(Res.string.publishing_privacy_notice),
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        confirmButton = {
            if (onConfirm != null) {
                Button(
                    onClick = {
                        onConfirm()
                        onDismissRequest()
                    }
                ) {
                    Text(stringResource(Res.string.label_continue))
                }
            } else {
                TextButton(onClick = onDismissRequest) {
                    Text(text = stringResource(Res.string.dismiss))
                }
            }
        },
        dismissButton = {
            if (onConfirm != null) {
                TextButton(onClick = onDismissRequest) {
                    Text(text = stringResource(Res.string.title_cancel))
                }
            }
        }
    )
}