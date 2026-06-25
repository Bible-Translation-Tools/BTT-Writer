package org.bibletranslationtools.writer.ui.dialogs.project

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastRoundToInt
import androidx.compose.ui.window.Dialog
import btt_writer.shared.generated.resources.Project
import btt_writer.shared.generated.resources.Res
import btt_writer.shared.generated.resources.backup
import btt_writer.shared.generated.resources.confirm_delete_target_translation
import btt_writer.shared.generated.resources.label_change
import btt_writer.shared.generated.resources.label_delete
import btt_writer.shared.generated.resources.label_last_backup
import btt_writer.shared.generated.resources.label_last_uploaded
import btt_writer.shared.generated.resources.label_unknown
import btt_writer.shared.generated.resources.print
import btt_writer.shared.generated.resources.progress
import btt_writer.shared.generated.resources.publish
import btt_writer.shared.generated.resources.target_language
import btt_writer.shared.generated.resources.translators
import org.bibletranslationtools.writer.DirectoryProvider
import org.bibletranslationtools.writer.core.NativeSpeaker
import org.bibletranslationtools.writer.core.TextStyleType
import org.bibletranslationtools.writer.core.TranslationType
import org.bibletranslationtools.writer.core.Typography
import org.bibletranslationtools.writer.data.Preference
import org.bibletranslationtools.writer.data.getPrefOrNull
import org.bibletranslationtools.writer.ui.dialogs.ConfirmDialog
import org.bibletranslationtools.writer.ui.dialogs.ContributorsDialog
import org.bibletranslationtools.writer.ui.home.TranslationItem
import org.bibletranslationtools.writer.utils.DateUtils
import org.bibletranslationtools.writer.utils.getComposeTextStyle
import org.bibletranslationtools.writer.utils.getLastModified
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import java.io.File

@Composable
fun ProjectDetailsDialog(
    project: TranslationItem,
    onDismiss: () -> Unit,
    onChangeLanguage: () -> Unit,
    onDelete: () -> Unit,
    onPublish: () -> Unit,
    onExport: (Boolean) -> Unit
) {
    val typography: Typography = koinInject()
    val preference: Preference = koinInject()
    val directoryProvider: DirectoryProvider = koinInject()

    val savedBackup = preference.getPrefOrNull<String>(
        Preference.LAST_BACKUP + project.translation.id
    )
    var displayBackupTime: String? = null
    if (!savedBackup.isNullOrEmpty()) {
        try {
            val date = DateUtils.parseDateString(savedBackup)
            if (date != null) {
                displayBackupTime = DateUtils.dateToDateTime(date)
            }
        } catch (_: Exception) {
            displayBackupTime = savedBackup
        }
    }

    if (displayBackupTime == null) {
        val backupFile = File(
            directoryProvider.backupsDir,
            "${project.translation.id}.tstudio"
        )
        if (backupFile.exists() && backupFile.isFile) {
            val date = backupFile.getLastModified()
            displayBackupTime = DateUtils.dateToDateTime(date)
        }
    }

    val savedUpload = preference.getPrefOrNull<String>(
        Preference.LAST_UPLOADED + project.translation.id
    )
    var displayUploadTime: String? = null
    if (!savedUpload.isNullOrEmpty()) {
        try {
            val date = DateUtils.parseDateString(savedUpload)
            if (date != null) {
                displayUploadTime = DateUtils.dateToDateTime(date)
            }
        } catch (_: Exception) {
            displayUploadTime = savedUpload
        }
    }

    val titleStyle = typography.getComposeTextStyle(
        translationType = TranslationType.TARGET,
        style = TextStyleType.NORMAL,
        languageCode = project.translation.targetLanguage.slug,
        direction = project.translation.targetLanguage.direction
    )
    val languageStyle = typography.getComposeTextStyle(
        translationType = TranslationType.TARGET,
        style = TextStyleType.SUB,
        languageCode = project.translation.targetLanguage.slug,
        direction = project.translation.targetLanguage.direction
    )

    var contributors by remember {
        mutableStateOf(project.translation.contributors.sortedBy { it.name.lowercase() })
    }
    var showContributorsDialog by rememberSaveable { mutableStateOf(false) }
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth()
                .heightIn(max = 700.dp)
                .padding(32.dp),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                    modifier = Modifier
                        .weight(1f)
                        .padding(24.dp)
                ) {
                    Text(
                        text = project.name + " - " + project.translation.targetLanguageName,
                        style = titleStyle
                    )

                    Column(modifier = Modifier.fillMaxWidth()) {
                        DetailRow(
                            label = stringResource(Res.string.Project),
                            value = project.formattedProjectName
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(Res.string.target_language),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Text(
                                text = project.translation.targetLanguageName + " (" + project.translation.targetLanguageId + ")",
                                modifier = Modifier.weight(1f),
                                style = languageStyle,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )

                            TextButton(onClick = onChangeLanguage) {
                                Text(
                                    text = stringResource(Res.string.label_change),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        DetailRow(
                            label = stringResource(Res.string.progress),
                            value = "${(project.progress * 100).fastRoundToInt()}%"
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                text = stringResource(Res.string.translators),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = getTranslatorNames(contributors),
                                color = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.clickable {
                                    showContributorsDialog = true
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        DetailRow(
                            label = stringResource(Res.string.label_last_backup),
                            value = displayBackupTime ?: stringResource(Res.string.label_unknown)
                        )

                        DetailRow(
                            label = stringResource(Res.string.label_last_uploaded),
                            value = displayUploadTime ?: stringResource(Res.string.label_unknown)
                        )
                    }
                }

                Column(modifier = Modifier.fillMaxWidth()) {
                    HorizontalDivider()

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete"
                            )
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        ActionIconButton(
                            imageVector = Icons.Default.Print,
                            contentDesc = stringResource(Res.string.print),
                            onClick = { onExport(true) }
                        )
                        ActionIconButton(
                            imageVector = Icons.Default.DoneAll,
                            contentDesc = stringResource(Res.string.publish),
                            onClick = onPublish
                        )
                        ActionIconButton(
                            imageVector = Icons.Default.Upload,
                            contentDesc = stringResource(Res.string.backup),
                            onClick = { onExport(false) }
                        )
                    }
                }
            }
        }
    }

    if (showContributorsDialog) {
        ContributorsDialog(
            contributors = contributors,
            targetTranslation = project.translation,
            onContributorsChanged = {
                contributors = project.translation.contributors.sortedBy {
                    it.name.lowercase()
                }
            },
            onDismiss = { showContributorsDialog = false }
        )
    }

    if (showDeleteDialog) {
        ConfirmDialog(
            title = stringResource(Res.string.label_delete),
            message = stringResource(Res.string.confirm_delete_target_translation),
            onConfirm = {
                showDeleteDialog = false
                onDismiss()
                onDelete()
            },
            onDismiss = { showDeleteDialog = false }
        )
    }
}

private fun getTranslatorNames(contributors: List<NativeSpeaker>): String {
    return contributors.joinToString(separator = "\n") { it.name }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(end = 10.dp)
        )
        Text(text = value)
    }
}

@Composable
private fun ActionIconButton(
    imageVector: ImageVector,
    contentDesc: String,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = contentDesc
        )
    }
}