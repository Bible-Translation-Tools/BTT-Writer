package org.bibletranslationtools.writer.ui.translate.chunk

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import org.bibletranslationtools.resourcecontainer.ResourceContainer
import org.bibletranslationtools.writer.core.TargetTranslation
import org.bibletranslationtools.writer.core.Typography
import org.bibletranslationtools.writer.ui.dialogs.source.SourceTabItem
import org.bibletranslationtools.writer.ui.translate.ChunkItem
import org.bibletranslationtools.writer.ui.translate.components.StackedCardFlipper

@Composable
fun ChunkCard(
    item: ChunkItem,
    sourceTabs: List<SourceTabItem>,
    typography: Typography,
    selectedSource: ResourceContainer?,
    targetTranslation: TargetTranslation,
    onSourceTabClick: (String) -> Unit,
    onAddNewSourceClick: () -> Unit,
    onRemoveSourceClick: (String) -> Unit,
    onTextChange: (String) -> Unit,
    onCardsSwiped: (Boolean) -> Unit,
    onOpenChunkClick: () -> Unit,
    onConflictClick: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(item.sourceOnTop) {
        if (item.sourceOnTop) {
            focusManager.clearFocus()
            keyboardController?.hide()
        }
    }

    StackedCardFlipper(
        modifier = modifier,
        containerPadding = 8.dp,
        stackOffset = 32.dp,
        frontOnTop = item.sourceOnTop,
        frontCard = {
            ChunkSourceCard(
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
            ChunkTargetCard(
                item = item,
                targetTranslation = targetTranslation,
                typography = typography,
                onTextChange = onTextChange,
                onCompleteItemClick = onOpenChunkClick,
                onConflictClick = onConflictClick
            )
        },
        onAnimationEnd = onCardsSwiped
    )
}
