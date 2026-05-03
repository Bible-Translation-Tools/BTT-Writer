package org.bibletranslationtools.writer.ui.crash

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import btt_writer.composeapp.generated.resources.Res
import btt_writer.composeapp.generated.resources.apk_update_available
import btt_writer.composeapp.generated.resources.crash_details
import btt_writer.composeapp.generated.resources.download_update
import btt_writer.composeapp.generated.resources.internet_not_available
import btt_writer.composeapp.generated.resources.label_close
import btt_writer.composeapp.generated.resources.label_continue
import btt_writer.composeapp.generated.resources.label_ok
import btt_writer.composeapp.generated.resources.success
import btt_writer.composeapp.generated.resources.title_cancel
import btt_writer.composeapp.generated.resources.title_upload
import btt_writer.composeapp.generated.resources.upload_complete
import btt_writer.composeapp.generated.resources.upload_crash_report_failed
import btt_writer.composeapp.generated.resources.upload_failed
import btt_writer.composeapp.generated.resources.upload_report_or_download_latest_apk
import btt_writer.composeapp.generated.resources.use_internet_confirmation
import org.bibletranslationtools.writer.ui.dialogs.BaseDialog
import org.bibletranslationtools.writer.ui.dialogs.ConfirmDialog
import org.bibletranslationtools.writer.ui.dialogs.ProgressDialog
import org.jetbrains.compose.resources.stringResource

@Composable
fun CrashReporterScreen(
    component: CrashComponent
) {
    var showConfirmDialog by rememberSaveable { mutableStateOf(false) }
    var showUpdateAvailableDialog by rememberSaveable { mutableStateOf(false) }
    var showUploadErrorDialog by rememberSaveable { mutableStateOf(false) }

    val state by component.state.collectAsStateWithLifecycle()
    val progress by component.progress.collectAsStateWithLifecycle()

    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(component) {
        component.event.collect { event ->
            when (event) {
                is CrashComponent.Event.UpdateAvailable -> {
                    showUpdateAvailableDialog = true
                }
                is CrashComponent.Event.UploadError -> {
                    showUploadErrorDialog = true
                }
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 24.dp)
                .imePadding()
        ) {
            OutlinedTextField(
                value = state.notes,
                onValueChange = component::updateNotes,
                colors = TextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                placeholder = {
                    Text(stringResource(Res.string.crash_details))
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(bottom = 8.dp)
                    .focusRequester(focusRequester)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = component::flushAndRestart,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 4.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Text(stringResource(Res.string.title_cancel))
                }

                Button(
                    onClick = {
                        focusManager.clearFocus()
                        showConfirmDialog = true
                    },
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 4.dp)
                ) {
                    Text(stringResource(Res.string.title_upload))
                }
            }
        }
    }

    if (showConfirmDialog) {
        ConfirmDialog(
            title = stringResource(Res.string.title_upload),
            message = stringResource(Res.string.use_internet_confirmation),
            onConfirm = {
                showConfirmDialog = false
                component.checkForLatestRelease()
            },
            onDismiss = {
                showConfirmDialog = false
                component.flushAndRestart()
            },
            confirmText = stringResource(Res.string.label_continue),
            dismissText = stringResource(Res.string.label_close)
        )
    }

    if (showUpdateAvailableDialog) {
        BaseDialog(
            title = stringResource(Res.string.apk_update_available),
            message = stringResource(Res.string.upload_report_or_download_latest_apk),
            onDismiss = { showUpdateAvailableDialog = false }
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                TextButton(onClick = {
                    showUpdateAvailableDialog = false
                    component.flushAndRestart()
                }) {
                    Text(stringResource(Res.string.title_cancel))
                }
                TextButton(onClick = {
                    showUpdateAvailableDialog = false
                    component.downloadLatestRelease()
                }) {
                    Text(stringResource(Res.string.download_update))
                }

                Spacer(modifier = Modifier.weight(1f))

                Button(onClick = {
                    showUpdateAvailableDialog = false
                    component.uploadCrashReport()
                }) {
                    Text(stringResource(Res.string.label_continue))
                }
            }
        }
    }

    if (showUploadErrorDialog) {
        val messageId = if (component.isNetworkAvailable) {
            Res.string.upload_crash_report_failed
        } else {
            Res.string.internet_not_available
        }
        
        BaseDialog(
            onDismiss = { showUploadErrorDialog = false },
            title = stringResource(Res.string.upload_failed),
            message = stringResource(messageId)
        ) {
            Button(onClick = { showUploadErrorDialog = false }) {
                Text(stringResource(Res.string.label_ok))
            }
        }
    }

    if (state.success) {
        BaseDialog(
            title = stringResource(Res.string.success),
            message = stringResource(Res.string.upload_complete),
            onDismiss = component::flushAndRestart
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

    progress?.let { progress ->
        ProgressDialog(
            message = progress.message,
            progress = progress.value
        )
    }
}