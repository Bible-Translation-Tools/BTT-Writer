package org.bibletranslationtools.writer.ui.translate.review

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.unit.dp
import org.bibletranslationtools.writer.core.TextStyleType
import org.bibletranslationtools.writer.core.TranslationHelp
import org.bibletranslationtools.writer.core.TranslationType
import org.bibletranslationtools.writer.core.Typography
import org.bibletranslationtools.writer.ui.dialogs.source.SourceTabItem
import org.bibletranslationtools.writer.ui.translate.ReviewItem
import org.bibletranslationtools.writer.ui.translate.chunk.ChunkSourceCard
import org.bibletranslationtools.writer.utils.getComposeTextStyle

/**
 * Review layout for notes/questions (tn/tq) projects: book source,
 * the user's own book translation, the helps editor and the resources pane.
 */
@Composable
fun HelpsReviewCard(
    item: ReviewItem,
    sourceTabs: List<SourceTabItem>,
    typography: Typography,
    onSourceTabClick: (String) -> Unit,
    onAddNewSourceClick: () -> Unit,
    onRemoveSourceClick: (String) -> Unit,
    onHelpsChanged: (List<TranslationHelp>) -> Unit,
    onDoneToggle: (Boolean) -> Unit,
    onRenderHelps: () -> Unit,
    onHelpClick: (HelpItem) -> Unit,
    onExpandedChange: (Boolean) -> Unit,
    onUndoClick: () -> Unit,
    onRedoClick: () -> Unit,
    onConflictSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    resourcesOpen: Boolean = false,
    sourceSearchQuery: String? = null,
    targetSearchQuery: String? = null
) {
    val mainWeight by animateFloatAsState(
        targetValue = if (resourcesOpen) 0.245f else 0.326f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy),
        label = "weight"
    )
    val peekWeight by animateFloatAsState(
        targetValue = if (resourcesOpen) 0.245f else 0.02f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy),
        label = "peekWeight"
    )
    val endPadding by animateDpAsState(
        targetValue = if (resourcesOpen) 16.dp else 0.dp,
        label = "endPadding"
    )

    LaunchedEffect(resourcesOpen, item.chunk.source, item.helps) {
        if (resourcesOpen) onRenderHelps()
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Max)
            .padding(start = 16.dp)
            .padding(end = endPadding)
            .pointerInput(Unit) {
                var totalDrag = 0f
                detectHorizontalDragGestures(
                    onDragStart = { totalDrag = 0f },
                    onDragEnd = { totalDrag = 0f },
                    onDragCancel = { totalDrag = 0f }
                ) { _, dragAmount ->
                    totalDrag += dragAmount
                    when {
                        totalDrag < -200f -> {
                            onExpandedChange(true)
                            totalDrag = 0f
                        }
                        totalDrag > 200f -> {
                            onExpandedChange(false)
                            totalDrag = 0f
                        }
                    }
                }
            }
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
            modifier = Modifier.weight(mainWeight)
                .fillMaxSize()
        )

        BookTranslationCard(
            item = item,
            typography = typography,
            modifier = Modifier.weight(mainWeight)
                .fillMaxSize()
        )

        if (!item.hasMergeConflict) {
            HelpsEditorCard(
                item = item,
                typography = typography,
                onHelpsChanged = onHelpsChanged,
                onDoneToggle = onDoneToggle,
                modifier = Modifier.weight(mainWeight)
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
                modifier = Modifier.weight(mainWeight)
                    .fillMaxSize()
            )
        }

        ResourcesCard(
            helps = item.helps,
            sourceLanguage = item.chunk.source.language,
            typography = typography,
            resourcesOpen = resourcesOpen,
            onHelpClick = onHelpClick,
            modifier = Modifier.weight(peekWeight)
                .fillMaxSize()
        )
    }
}

/**
 * Read-only view of the user's own book translation of this chunk.
 */
@Composable
fun BookTranslationCard(
    item: ReviewItem,
    typography: Typography,
    modifier: Modifier = Modifier
) {
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
                    text = item.targetTitle,
                    style = titleStyle,
                    modifier = Modifier.weight(1f)
                )
            }

            val inlineContentMap = mapOf(
                "note_icon" to InlineTextContent(
                    Placeholder(
                        width = bodyStyle.fontSize,
                        height = bodyStyle.fontSize,
                        placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = "Note",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            )

            Text(
                text = item.renderedBookTranslationText,
                inlineContent = inlineContentMap,
                style = bodyStyle,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }
}
