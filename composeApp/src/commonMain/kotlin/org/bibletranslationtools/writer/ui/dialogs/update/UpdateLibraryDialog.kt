package org.bibletranslationtools.writer.ui.dialogs.update

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import btt_writer.composeapp.generated.resources.Res
import btt_writer.composeapp.generated.resources.apk_update_available
import btt_writer.composeapp.generated.resources.check_app_update
import btt_writer.composeapp.generated.resources.dismiss
import btt_writer.composeapp.generated.resources.download_index
import btt_writer.composeapp.generated.resources.download_index_success
import btt_writer.composeapp.generated.resources.download_latest_apk
import btt_writer.composeapp.generated.resources.download_sources
import btt_writer.composeapp.generated.resources.import_index
import btt_writer.composeapp.generated.resources.success
import btt_writer.composeapp.generated.resources.title_cancel
import btt_writer.composeapp.generated.resources.update_languages
import btt_writer.composeapp.generated.resources.update_menu_requires_internet
import btt_writer.composeapp.generated.resources.update_options
import btt_writer.composeapp.generated.resources.update_source
import btt_writer.composeapp.generated.resources.update_sources_success
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import org.bibletranslationtools.writer.ui.dialogs.BaseDialog
import org.bibletranslationtools.writer.ui.dialogs.ConfirmDialog
import org.bibletranslationtools.writer.ui.dialogs.ProgressDialog
import org.jetbrains.compose.resources.stringResource

private const val UPDATE_OPTIONS_HELP_URL =
    "http://help.door43.org/en/knowledgebase/9-translationstudio/docs/5-update-options"

@Composable
fun UpdateLibraryDialog(
    component: UpdateLibraryComponent,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit
) {
    val state by component.state.collectAsStateWithLifecycle()
    val progress by component.progress.collectAsStateWithLifecycle()

    val uriHandler = LocalUriHandler.current

    val openIndexLauncher = rememberFilePickerLauncher(
        type = FileKitType.File(extensions = listOf("sqlite", "db"))
    ) { file ->
        file?.let(component::importIndex)
    }

    var showIndexUpdatedDialog by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(component) {
        component.event.collect { event ->
            when (event) {
                is UpdateLibraryComponent.Event.IndexUpdated -> showIndexUpdatedDialog = true
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(
                modifier = modifier
                    .fillMaxWidth()
                    .widthIn(max = 700.dp)
                    .animateContentSize()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(Res.string.update_options),
                            fontSize = 24.sp
                        )
                        IconButton(
                            onClick = {
                                uriHandler.openUri(UPDATE_OPTIONS_HELP_URL)
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Info"
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(32.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(Res.string.update_menu_requires_internet),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 16.sp
                        )
                        Icon(
                            imageVector = Icons.Default.Wifi,
                            contentDescription = "internet",
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    HorizontalDivider()

                    Column(modifier = Modifier.fillMaxWidth()) {
                        UpdateOptionItem(
                            stringResource(Res.string.update_source)
                        ) {
                            component.updateSources()
                        }
                        UpdateOptionItem(
                            stringResource(Res.string.import_index)
                        ) {
                            openIndexLauncher.launch()
                        }
                        UpdateOptionItem(
                            stringResource(Res.string.download_index)
                        ) {
                            component.downloadIndex()
                        }
                        UpdateOptionItem(
                            stringResource(Res.string.download_sources)
                        ) {
                            component.openDownloadSources()
                        }
                        UpdateOptionItem(
                            stringResource(Res.string.update_languages)
                        ) {
                            component.updateLanguages()
                        }
                        UpdateOptionItem(
                            text = stringResource(Res.string.check_app_update),
                            textColor = MaterialTheme.colorScheme.tertiary
                        ) {
                            component.checkAppUpdate()
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onDismiss,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(stringResource(Res.string.title_cancel))
                    }
                }
            }
        }
    }

    if (showIndexUpdatedDialog) {
        ConfirmDialog(
            title = stringResource(Res.string.success),
            message = stringResource(Res.string.download_index_success),
            onDismiss = { showIndexUpdatedDialog = false },
            onConfirm = { showIndexUpdatedDialog = false }
        )
    }

    state.resultMessage?.let { (title, message) ->
        BaseDialog(
            title = title,
            message = message,
            onDismiss = component::clearResult
        ) { onBaseDismiss ->
            TextButton(
                onClick = onBaseDismiss
            ) {
                Text(stringResource(Res.string.dismiss))
            }
        }
    }

    state.latestRelease?.let { release ->
        ConfirmDialog(
            title = stringResource(Res.string.apk_update_available),
            message = stringResource(Res.string.download_latest_apk),
            onDismiss = component::clearLatestRelease,
            onConfirm = {
                component.clearLatestRelease()
                uriHandler.openUri(release.downloadUrl)
            }
        )
    }

    state.updateSourceResult?.let { result ->
        ConfirmDialog(
            title = stringResource(Res.string.success),
            message = stringResource(
                Res.string.update_sources_success,
                result.addedCount,
                result.updatedCount
            ),
            onDismiss = component::clearUpdateSourceResult,
            onConfirm = component::openDownloadSources
        )
    }

    progress?.let {
        ProgressDialog(
            message = it.message,
            progress = it.value,
            details = it.details
        )
    }
}

@Composable
private fun UpdateOptionItem(
    text: String,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        TextButton(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp),
            shape = RectangleShape,
            colors = ButtonDefaults.textButtonColors(
                contentColor = MaterialTheme.colorScheme.onSurface
            )
        ) {
            Text(
                text = text,
                color = textColor,
                modifier = Modifier.fillMaxWidth()
            )
        }

        HorizontalDivider()
    }
}