package org.bibletranslationtools.writer.ui.dialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import btt_writer.shared.generated.resources.Res
import btt_writer.shared.generated.resources.action_save
import btt_writer.shared.generated.resources.add_contributor
import btt_writer.shared.generated.resources.confirm_delete_translator
import btt_writer.shared.generated.resources.delete_translator_title
import btt_writer.shared.generated.resources.duplicate_native_speaker
import btt_writer.shared.generated.resources.label_delete
import btt_writer.shared.generated.resources.license_pdf
import btt_writer.shared.generated.resources.name
import btt_writer.shared.generated.resources.person_agrees_with_licenses
import btt_writer.shared.generated.resources.statement_of_faith
import btt_writer.shared.generated.resources.title_cancel
import btt_writer.shared.generated.resources.translation_guidlines
import btt_writer.shared.generated.resources.view_license_agreement
import btt_writer.shared.generated.resources.view_statement_of_faith
import btt_writer.shared.generated.resources.view_translation_guidelines
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.bibletranslationtools.writer.core.NativeSpeaker
import org.bibletranslationtools.writer.core.TargetTranslation
import org.jetbrains.compose.resources.stringResource

@Composable
fun ContributorDialog(
    contributor: NativeSpeaker,
    targetTranslation: TargetTranslation,
    onDismiss: () -> Unit,
    onContributorsChanged: () -> Unit
) {
    val isNew by rememberUpdatedState(contributor.name.isEmpty())
    var openLegalDocumentId by rememberSaveable { mutableStateOf<String?>(null) }
    var showDeleteContributorDialog by rememberSaveable { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val duplicateSpeakerMessage = stringResource(Res.string.duplicate_native_speaker)
    var name by remember { mutableStateOf(contributor.name) }
    var hasAgreed by remember { mutableStateOf(!isNew) }

    OverlayDialog(
        snackbarHostState = snackbarHostState,
        onDismiss = onDismiss
    ) {
        Text(
            text = stringResource(Res.string.add_contributor),
            fontSize = 24.sp,
            modifier = Modifier
        )

        Spacer(modifier = Modifier.height(24.dp))

        Column(modifier = Modifier.fillMaxWidth()) {
            TextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(Res.string.name)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (isNew) hasAgreed = !hasAgreed
                    }
                    .padding(vertical = 8.dp)
            ) {
                Checkbox(
                    enabled = isNew,
                    checked = hasAgreed,
                    onCheckedChange = { hasAgreed = it }
                )
                Text(
                    text = stringResource(Res.string.person_agrees_with_licenses),
                    fontSize = 16.sp,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            if (isNew) {
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    TagButton(
                        text = stringResource(Res.string.view_license_agreement),
                        onClick = { openLegalDocumentId = Res.string.license_pdf.key }
                    )
                    TagButton(
                        text = stringResource(Res.string.view_statement_of_faith),
                        onClick = { openLegalDocumentId = Res.string.statement_of_faith.key }
                    )
                    TagButton(
                        text = stringResource(Res.string.view_translation_guidelines),
                        onClick = { openLegalDocumentId = Res.string.translation_guidlines.key }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!isNew) {
                TextButton(onClick = { showDeleteContributorDialog = true }) {
                    Text(
                        text = stringResource(Res.string.label_delete),
                        color = Color.Red
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Row {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(Res.string.title_cancel))
                }

                Button(
                    onClick = {
                        coroutineScope.launch {
                            withContext(Dispatchers.IO) {
                                val duplicate = targetTranslation.getContributor(name)
                                when (duplicate) {
                                    null -> {
                                        targetTranslation.removeContributor(contributor)
                                        targetTranslation.addContributor(NativeSpeaker(name))
                                        onContributorsChanged()
                                    }

                                    contributor -> {
                                        onDismiss()
                                    }

                                    else -> {
                                        snackbarHostState.showSnackbar(duplicateSpeakerMessage)
                                    }
                                }
                            }
                        }
                    },
                    enabled = name.isNotBlank() && hasAgreed,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(stringResource(Res.string.action_save))
                }
            }
        }
    }

    openLegalDocumentId?.let { resourceId ->
        LegalDocumentDialog(
            htmlResourceId = resourceId,
            onDismissRequest = { openLegalDocumentId = null }
        )
    }

    if (showDeleteContributorDialog) {
        ConfirmDialog(
            title = stringResource(Res.string.delete_translator_title),
            message = stringResource(Res.string.confirm_delete_translator),
            onDismiss = { showDeleteContributorDialog = false },
            onConfirm = {
                coroutineScope.launch {
                    withContext(Dispatchers.IO) {
                        targetTranslation.removeContributor(contributor)
                    }
                    onContributorsChanged()
                }
            }
        )
    }
}

@Composable
fun TagButton(text: String, onClick: () -> Unit) {
    TextButton(onClick = onClick) {
        Text(text = text, fontSize = 14.sp)
    }
}