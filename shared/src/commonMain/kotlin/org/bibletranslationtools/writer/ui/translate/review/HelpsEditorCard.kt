package org.bibletranslationtools.writer.ui.translate.review

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import btt_writer.shared.generated.resources.Res
import btt_writer.shared.generated.resources.add_answer_here
import btt_writer.shared.generated.resources.add_question_here
import btt_writer.shared.generated.resources.click_to_add_resource
import btt_writer.shared.generated.resources.mark_done
import btt_writer.shared.generated.resources.note_text
import btt_writer.shared.generated.resources.reference_text
import btt_writer.shared.generated.resources.tn_type
import btt_writer.shared.generated.resources.tq_type
import org.bibletranslationtools.writer.core.ResourceType
import org.bibletranslationtools.writer.core.TextStyleType
import org.bibletranslationtools.writer.core.TranslationHelp
import org.bibletranslationtools.writer.core.TranslationType
import org.bibletranslationtools.writer.core.Typography
import org.bibletranslationtools.writer.ui.translate.ReviewItem
import org.bibletranslationtools.writer.ui.translate.components.HelpEntryField
import org.bibletranslationtools.writer.utils.getComposeTextStyle
import org.jetbrains.compose.resources.stringResource

/**
 * Editor for notes (tn) and questions (tq): a list of title/body entries
 * that can be added, removed and reordered.
 */
@Composable
fun HelpsEditorCard(
    item: ReviewItem,
    typography: Typography,
    onHelpsChanged: (List<TranslationHelp>) -> Unit,
    onDoneToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val helps = item.helpsContent
    val complete = item.targetMode == TargetMode.COMPLETE
    val isNotes = item.chunk.target.translationType == ResourceType.TRANSLATION_NOTE

    val outlineColor = MaterialTheme.colorScheme.outline

    val typeName = stringResource(
        if (isNotes) Res.string.tn_type else Res.string.tq_type
    )
    val titlePlaceholder = stringResource(
        if (isNotes) Res.string.reference_text else Res.string.add_question_here
    )
    val bodyPlaceholder = stringResource(
        if (isNotes) Res.string.note_text else Res.string.add_answer_here
    )

    val titleStyle = typography.getComposeTextStyle(
        translationType = TranslationType.TARGET,
        style = TextStyleType.SUB,
        languageCode = item.chunk.target.targetLanguage.slug,
        direction = item.chunk.target.targetLanguage.direction
    )
    val bodyStyle = typography.getComposeTextStyle(
        translationType = TranslationType.TARGET,
        style = TextStyleType.NORMAL,
        languageCode = item.chunk.target.targetLanguage.slug,
        direction = item.chunk.target.targetLanguage.direction
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
    ) {
        Card(
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${item.sourceTitle} - $typeName",
                    style = titleStyle,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                )
                if (!complete) {
                    IconButton(onClick = {
                        onHelpsChanged(helps + TranslationHelp("", ""))
                    }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add help",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (helps.isEmpty() && !complete) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(
                                Res.string.click_to_add_resource,
                                typeName
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        IconButton(onClick = {
                            onHelpsChanged(listOf(TranslationHelp("", "")))
                        }) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add help",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            helps.forEachIndexed { index, help ->
                Card(
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .height(IntrinsicSize.Max)
                    ) {
                        Column(
                            modifier = Modifier.weight(1f)
                                .fillMaxHeight()
                        ) {
                            HelpEntryField(
                                value = help.title,
                                placeholder = titlePlaceholder,
                                readOnly = complete,
                                textStyle = bodyStyle.copy(
                                    color = MaterialTheme.colorScheme.outline
                                ),
                                onValueChange = { text ->
                                    onHelpsChanged(
                                        helps.toMutableList().apply {
                                            this[index] = help.copy(title = text)
                                        }
                                    )
                                }
                            )
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.error
                            )
                            HelpEntryField(
                                value = help.body,
                                placeholder = bodyPlaceholder,
                                readOnly = complete,
                                textStyle = bodyStyle,
                                lines = true,
                                onValueChange = { text ->
                                    onHelpsChanged(
                                        helps.toMutableList().apply {
                                            this[index] = help.copy(body = text)
                                        }
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (!complete) {
                            Column(
                                modifier = Modifier.fillMaxHeight()
                                    .drawBehind {
                                        drawLine(
                                            color = outlineColor,
                                            start = Offset(x = 0f, y = 0f),
                                            end = Offset(x = 0f, y = size.height),
                                            strokeWidth = 1.dp.toPx()
                                        )
                                    }
                            ) {
                                IconButton(onClick = {
                                    onHelpsChanged(
                                        helps.toMutableList().apply { removeAt(index) }
                                    )
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Delete help"
                                    )
                                }
                                if (index > 0) {
                                    IconButton(onClick = {
                                        onHelpsChanged(helps.swap(index, index - 1))
                                    }) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowDropUp,
                                            contentDescription = "Move up"
                                        )
                                    }
                                }
                                if (index < helps.size - 1) {
                                    IconButton(onClick = {
                                        onHelpsChanged(helps.swap(index, index + 1))
                                    }) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowDropDown,
                                            contentDescription = "Move down"
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }

        Card(
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(Res.string.mark_done),
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = complete,
                    onCheckedChange = onDoneToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }
    }
}

private fun List<TranslationHelp>.swap(from: Int, to: Int): List<TranslationHelp> {
    return toMutableList().apply {
        val removed = removeAt(from)
        add(to, removed)
    }
}