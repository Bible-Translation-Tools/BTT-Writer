package org.bibletranslationtools.writer.ui.translate.read

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.bibletranslationtools.writer.core.Typography
import org.bibletranslationtools.writer.ui.dialogs.ProgressDialog
import org.bibletranslationtools.writer.ui.translate.ModeScreenTemplate
import org.bibletranslationtools.writer.ui.translate.ScrollBindingEffect
import org.bibletranslationtools.writer.ui.translate.ScrollCoordinator
import org.bibletranslationtools.writer.ui.translate.TranslateComponent

@Composable
fun ReadModeSection(
    component: ReadModeComponent,
    parentComponent: TranslateComponent,
    typography: Typography,
    scrollCoordinator: ScrollCoordinator,
    onSourceDialogOpen: () -> Unit,
    onHasMergeConflicts: (Boolean) -> Unit,
    onBeginTranslation: (chapterSlug: String) -> Unit
) {
    val sharedState by parentComponent.sharedState.collectAsStateWithLifecycle()
    val progress by component.progress.collectAsStateWithLifecycle()
    val items by component.items.collectAsStateWithLifecycle()
    val hasConflicts = items.any { it.hasMergeConflict }

    ScrollBindingEffect(scrollCoordinator, items, parentComponent)

    LaunchedEffect(hasConflicts) {
        onHasMergeConflicts(hasConflicts)
    }

    ModeScreenTemplate(
        component = component,
        items = items.toList(),
        listState = scrollCoordinator.listState
    ) { item ->
        ReadCard(
            item = item,
            sourceTabs = sharedState.sourceTabs,
            typography = typography,
            selectedSource = sharedState.resourceContainer,
            targetTranslation = parentComponent.targetTranslation,
            onSourceTabClick = parentComponent::selectSource,
            onAddNewSourceClick = onSourceDialogOpen,
            onRemoveSourceClick = parentComponent::removeSource,
            onCardsSwiped = { component.onCardsSwiped(item, it) },
            onBeginTranslation = onBeginTranslation
        )
    }

    progress?.let {
        ProgressDialog(
            message = it.message,
            progress = it.value
        )
    }
}
