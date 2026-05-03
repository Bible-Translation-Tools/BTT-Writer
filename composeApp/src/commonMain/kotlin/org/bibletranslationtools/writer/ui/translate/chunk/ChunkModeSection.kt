package org.bibletranslationtools.writer.ui.translate.chunk

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import btt_writer.composeapp.generated.resources.Res
import btt_writer.composeapp.generated.resources.chunk_done_prompt
import btt_writer.composeapp.generated.resources.chunk_done_title
import btt_writer.composeapp.generated.resources.edit
import org.bibletranslationtools.writer.core.Typography
import org.bibletranslationtools.writer.ui.dialogs.ConfirmDialog
import org.bibletranslationtools.writer.ui.dialogs.ProgressDialog
import org.bibletranslationtools.writer.ui.translate.ModeScreenTemplate
import org.bibletranslationtools.writer.ui.translate.ScrollBindingEffect
import org.bibletranslationtools.writer.ui.translate.ScrollCoordinator
import org.bibletranslationtools.writer.ui.translate.TranslateComponent
import org.jetbrains.compose.resources.stringResource

@Composable
fun ChunkModeSection(
    component: ChunkModeComponent,
    parentComponent: TranslateComponent,
    typography: Typography,
    scrollCoordinator: ScrollCoordinator,
    onSourceDialogOpen: () -> Unit,
    onHasMergeConflicts: (Boolean) -> Unit,
    onConflictClick: (String, String) -> Unit
) {
    val sharedState by parentComponent.sharedState.collectAsStateWithLifecycle()
    val chunkState by component.state.collectAsStateWithLifecycle()
    val progress by component.progress.collectAsStateWithLifecycle()
    val items by component.items.collectAsStateWithLifecycle()

    ScrollBindingEffect(scrollCoordinator, items, parentComponent)

    val hasConflicts = items.any { it.hasMergeConflict }

    LaunchedEffect(hasConflicts) {
        onHasMergeConflicts(hasConflicts)
    }

    ModeScreenTemplate(
        component = component,
        items = items.toList(),
        listState = scrollCoordinator.listState,
        dialogs = {
            if (chunkState.chunkToReopen != null) {
                ConfirmDialog(
                    title = stringResource(Res.string.chunk_done_title),
                    message = stringResource(Res.string.chunk_done_prompt),
                    onDismiss = {
                        component.onReopenChunkConfirmed(false)
                    },
                    onConfirm = {
                        component.onReopenChunkConfirmed(true)
                    },
                    confirmText = stringResource(Res.string.edit)
                )
            }
        }
    ) { item ->
        ChunkCard(
            item = item,
            sourceTabs = sharedState.sourceTabs,
            typography = typography,
            selectedSource = sharedState.resourceContainer,
            targetTranslation = parentComponent.targetTranslation,
            onSourceTabClick = parentComponent::selectSource,
            onAddNewSourceClick = onSourceDialogOpen,
            onRemoveSourceClick = parentComponent::removeSource,
            onTextChange = {
                component.onItemTextChanged(item, it)
            },
            onCardsSwiped = { sourceOnTop ->
                component.onCardsSwiped(item, sourceOnTop)
            },
            onOpenChunkClick = { component.reopenChunk(item) },
            onConflictClick = onConflictClick
        )
    }

    progress?.let {
        ProgressDialog(
            message = it.message,
            progress = it.value
        )
    }
}
