package org.bibletranslationtools.writer.ui.translate.review

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import btt_writer.shared.generated.resources.Res
import btt_writer.shared.generated.resources.mark_done
import btt_writer.shared.generated.resources.translate_definition_here
import btt_writer.shared.generated.resources.translate_word_here
import org.bibletranslationtools.writer.core.TextStyleType
import org.bibletranslationtools.writer.core.TranslationHelp
import org.bibletranslationtools.writer.core.TranslationType
import org.bibletranslationtools.writer.core.Typography
import org.bibletranslationtools.writer.ui.dialogs.source.SourceTabItem
import org.bibletranslationtools.writer.ui.translate.ReviewItem
import org.bibletranslationtools.writer.ui.translate.chunk.ChunkSourceCard
import org.bibletranslationtools.writer.ui.translate.components.HelpEntryField
import org.bibletranslationtools.writer.utils.getComposeTextStyle
import org.jetbrains.compose.resources.stringResource

/**
 * Review layout for translationWords (tw) projects: the source word entry
 * next to a single title/definition editor. No helps pane.
 */
@Composable
fun WordsReviewCard(
    item: ReviewItem,
    sourceTabs: List<SourceTabItem>,
    typography: Typography,
    onSourceTabClick: (String) -> Unit,
    onAddNewSourceClick: () -> Unit,
    onRemoveSourceClick: (String) -> Unit,
    onHelpsChanged: (List<TranslationHelp>) -> Unit,
    onDoneToggle: (Boolean) -> Unit,
    onUndoClick: () -> Unit,
    onRedoClick: () -> Unit,
    onConflictSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    sourceSearchQuery: String? = null,
    targetSearchQuery: String? = null
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Max)
            .padding(horizontal = 16.dp)
    ) {
        ChunkSourceCard(
            title = item.sourceTitle,
            text = item.renderedSourceText,
            sourceTabs = sourceTabs,
            typography = typography,
            selectedSource = item.chunk.source,
            onSourceTabClick = onSourceTabClick,
            onAddNewSourceClick = onAddNewSourceClick,
            onRemoveSourceClick = onRemoveSourceClick,
            searchQuery = sourceSearchQuery,
            modifier = Modifier.weight(1f)
                .fillMaxSize()
        )

        if (!item.hasMergeConflict) {
            WordsEditorCard(
                item = item,
                typography = typography,
                onHelpsChanged = onHelpsChanged,
                onDoneToggle = onDoneToggle,
                modifier = Modifier.weight(1f)
                    .fillMaxSize()
            )
        } else {
            MergeConflictCard(
                item = item,
                typography = typography,
                searchQuery = targetSearchQuery,
                onUndoClick = onUndoClick,
                onRedoClick = onRedoClick,
                onConfirmClick = onConflictSelected,
                modifier = Modifier.weight(1f)
                    .fillMaxSize()
            )
        }
    }
}

/**
 * Editor for a single translationWords entry: word title and definition.
 */
@Composable
fun WordsEditorCard(
    item: ReviewItem,
    typography: Typography,
    onHelpsChanged: (List<TranslationHelp>) -> Unit,
    onDoneToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val help = item.helpsContent.firstOrNull() ?: TranslationHelp("", "")
    val complete = item.targetMode == TargetMode.COMPLETE

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

    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${item.sourceTitle} — ${item.chunk.target.targetLanguageName}",
                    style = titleStyle,
                    modifier = Modifier.weight(1f)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                HelpEntryField(
                    value = help.title,
                    placeholder = stringResource(Res.string.translate_word_here),
                    readOnly = complete,
                    textStyle = bodyStyle.copy(
                        color = MaterialTheme.colorScheme.outline
                    ),
                    onValueChange = { text ->
                        onHelpsChanged(listOf(help.copy(title = text)))
                    }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.error)

                HelpEntryField(
                    value = help.body,
                    placeholder = stringResource(Res.string.translate_definition_here),
                    readOnly = complete,
                    textStyle = bodyStyle,
                    lines = true,
                    onValueChange = { text ->
                        onHelpsChanged(listOf(help.copy(body = text)))
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
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
