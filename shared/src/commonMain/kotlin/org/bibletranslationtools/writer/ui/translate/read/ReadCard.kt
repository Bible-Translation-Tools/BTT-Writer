package org.bibletranslationtools.writer.ui.translate.read

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.bibletranslationtools.resourcecontainer.ResourceContainer
import org.bibletranslationtools.writer.core.TargetTranslation
import org.bibletranslationtools.writer.core.Typography
import org.bibletranslationtools.writer.ui.dialogs.source.SourceTabItem
import org.bibletranslationtools.writer.ui.translate.ReadItem
import org.bibletranslationtools.writer.ui.translate.components.StackedCardFlipper

@Composable
fun ReadCard(
    item: ReadItem,
    sourceTabs: List<SourceTabItem>,
    typography: Typography,
    selectedSource: ResourceContainer?,
    targetTranslation: TargetTranslation,
    onSourceTabClick: (String) -> Unit,
    onAddNewSourceClick: () -> Unit,
    onRemoveSourceClick: (String) -> Unit,
    onCardsSwiped: (Boolean) -> Unit,
    onBeginTranslation: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    StackedCardFlipper(
        modifier = modifier,
        containerPadding = 8.dp,
        stackOffset = 32.dp,
        frontOnTop = item.sourceOnTop,
        frontCard = {
            ReadSourceCard(
                title = item.sourceTitle,
                text = item.renderedSourceText,
                sourceTabs = sourceTabs,
                typography = typography,
                selectedSource = selectedSource,
                onSourceTabClick = onSourceTabClick,
                onAddNewSourceClick = onAddNewSourceClick,
                onRemoveSourceClick = onRemoveSourceClick
            )
        },
        backCard = {
            ReadTargetCard(
                title = item.targetTitle,
                text = item.renderedTargetText,
                targetTranslation = targetTranslation,
                typography = typography,
                onBeginTranslationClick = {
                    onBeginTranslation(item.chunk.chapterSlug)
                }
            )
        },
        onAnimationEnd = onCardsSwiped
    )
}
