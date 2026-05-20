package org.bibletranslationtools.writer.ui.dialogs.feedback

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import btt_writer.composeapp.generated.resources.Res
import btt_writer.composeapp.generated.resources.apk_update_available
import btt_writer.composeapp.generated.resources.bug_report
import btt_writer.composeapp.generated.resources.confirm
import btt_writer.composeapp.generated.resources.download_latest_apk
import btt_writer.composeapp.generated.resources.email_optional
import btt_writer.composeapp.generated.resources.feedback
import btt_writer.composeapp.generated.resources.label_close
import btt_writer.composeapp.generated.resources.requires_internet
import btt_writer.composeapp.generated.resources.retry_label
import btt_writer.composeapp.generated.resources.success
import btt_writer.composeapp.generated.resources.title_cancel
import btt_writer.composeapp.generated.resources.upload_complete
import btt_writer.composeapp.generated.resources.upload_failed
import org.bibletranslationtools.writer.ui.dialogs.BaseDialog
import org.bibletranslationtools.writer.ui.dialogs.ConfirmDialog
import org.bibletranslationtools.writer.ui.dialogs.OverlayDialog
import org.bibletranslationtools.writer.ui.dialogs.ProgressDialog
import org.jetbrains.compose.resources.stringResource

@Composable
fun FeedbackDialog(
    component: FeedbackComponent,
    onDismiss: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf(component.initialMessage) }

    val progress by component.progress.collectAsStateWithLifecycle()
    val state by component.state.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val uriHandler = LocalUriHandler.current

    LaunchedEffect(component) {
        component.event.collect { event ->
            when (event) {
                is FeedbackComponent.Event.SnackbarMessage -> {
                    snackbarHostState.showSnackbar(event.message)
                }
            }
        }
    }

    OverlayDialog(
        snackbarHostState = snackbarHostState,
        onDismiss = onDismiss
    ) { dismissWithKeyboard ->
        Text(
            text = stringResource(Res.string.feedback),
            fontSize = 24.sp,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Wifi,
                contentDescription = "requires internet",
                modifier = Modifier.padding(end = 5.dp)
            )
            Text(
                text = stringResource(Res.string.requires_internet),
                fontSize = 12.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            placeholder = {
                Text(stringResource(Res.string.email_optional))
            },
            maxLines = 1,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            placeholder = {
                Text(stringResource(Res.string.bug_report))
            },
            modifier = Modifier.fillMaxWidth()
                .height(150.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 8.dp),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = { dismissWithKeyboard(onDismiss) }) {
                Text(
                    text = stringResource(Res.string.title_cancel),
                    fontSize = 14.sp
                )
            }
            TextButton(
                onClick = { component.reportBug(notes, email) }
            ) {
                Text(
                    text = stringResource(Res.string.confirm),
                    fontSize = 14.sp
                )
            }
        }
    }

    state.uploadError?.let { error ->
        BaseDialog(
            title = stringResource(Res.string.upload_failed),
            message = error,
            onDismiss = component::clearError
        ) { onDismiss ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(Res.string.label_close))
                }
                TextButton(
                    onClick = {
                        onDismiss()
                        component.reportBug(notes, email)
                    }
                ) {
                    Text(stringResource(Res.string.retry_label))
                }
            }
        }
    }

    state.release?.let { release ->
        ConfirmDialog(
            title = stringResource(Res.string.apk_update_available),
            message = stringResource(Res.string.download_latest_apk),
            onDismiss = component::clearRelease,
            onConfirm = {
                uriHandler.openUri(release.downloadUrl)
            }
        )
    }

    if (state.success) {
        BaseDialog(
            title = stringResource(Res.string.success),
            message = stringResource(Res.string.upload_complete),
            onDismiss = onDismiss
        ) { onDismiss ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(Res.string.label_close))
                }
            }
        }
    }

    progress?.let {
        ProgressDialog(
            message = it.message,
            progress = it.value
        )
    }
}