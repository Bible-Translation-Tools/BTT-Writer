package org.bibletranslationtools.writer.ui.translate.review

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import be.digitalia.compose.htmlconverter.htmlToAnnotatedString
import btt_writer.shared.generated.resources.Res
import btt_writer.shared.generated.resources.chunk_checklist_body
import btt_writer.shared.generated.resources.chunk_checklist_title
import btt_writer.shared.generated.resources.dismiss
import btt_writer.shared.generated.resources.mark_chunks_done_result
import btt_writer.shared.generated.resources.project_checklist_body
import btt_writer.shared.generated.resources.project_checklist_title
import btt_writer.shared.generated.resources.result
import org.bibletranslationtools.resourcecontainer.Language
import org.bibletranslationtools.writer.core.ProjectTypeClass
import org.bibletranslationtools.writer.core.Typography
import org.bibletranslationtools.writer.ui.dialogs.BaseDialog
import org.bibletranslationtools.writer.ui.dialogs.ConfirmDialog
import org.bibletranslationtools.writer.ui.dialogs.ProgressDialog
import org.bibletranslationtools.writer.ui.translate.ModeScreenTemplate
import org.bibletranslationtools.writer.ui.translate.ScrollBindingEffect
import org.bibletranslationtools.writer.ui.translate.ScrollCoordinator
import org.bibletranslationtools.writer.ui.translate.TranslateComponent
import org.jetbrains.compose.resources.stringResource

@Composable
fun ReviewModeSection(
    component: ReviewModeComponent,
    parentComponent: TranslateComponent,
    typography: Typography,
    scrollCoordinator: ScrollCoordinator,
    searchRequested: Boolean,
    onSearchConsumed: () -> Unit,
    onSourceDialogOpen: () -> Unit,
    onHasMergeConflicts: (Boolean) -> Unit,
    conflictFilterOn: Boolean = false,
    onConflictFilterReset: () -> Unit,
    chunksDoneRequested: Boolean,
    onChunksDoneConsumed: () -> Unit
) {
    val sharedState by parentComponent.sharedState.collectAsStateWithLifecycle()
    val state by component.state.collectAsStateWithLifecycle()
    val items by component.items.collectAsStateWithLifecycle()
    val filteredItems by component.filteredItems.collectAsStateWithLifecycle()
    val progress by component.progress.collectAsStateWithLifecycle()
    val urlHandler = LocalUriHandler.current

    ScrollBindingEffect(scrollCoordinator, filteredItems, parentComponent)

    val hasConflicts = items.any { it.hasMergeConflict }

    // Auto-disable conflict filter when no conflicts remain
    LaunchedEffect(hasConflicts) {
        onHasMergeConflicts(hasConflicts)
        if (items.isNotEmpty() && !hasConflicts && conflictFilterOn) {
            onConflictFilterReset()
            component.setConflictFilterOn(false)
        }
    }

    LaunchedEffect(conflictFilterOn) {
        component.setConflictFilterOn(conflictFilterOn)
    }

    // Open search when requested from sidebar
    LaunchedEffect(searchRequested) {
        if (searchRequested) {
            component.openSearch()
            onSearchConsumed()
        }
    }

    // Scroll to matching item when search navigates
    LaunchedEffect(state.search?.currentItemId) {
        val targetId = state.search?.currentItemId ?: return@LaunchedEffect
        val index = filteredItems.indexOfFirst { it.id == targetId }
        if (index >= 0) {
            scrollCoordinator.listState.scrollToItem(index)
        }
    }

    LaunchedEffect(chunksDoneRequested) {
        if (chunksDoneRequested) {
            component.toggleMarkAllDone()
            onChunksDoneConsumed()
        }
    }

    Box {
        Column {
            state.search?.let { search ->
                SearchBar(
                    searchState = search,
                    onQueryChange = component::updateSearchQuery,
                    onSubjectChange = component::setSearchSubject,
                    onNext = component::nextMatch,
                    onPrev = component::prevMatch,
                    onClose = component::closeSearch
                )
            }
            ModeScreenTemplate(
                component = component,
                items = filteredItems,
                listState = scrollCoordinator.listState,
                dialogs = {
                    // URL handler
                    LaunchedEffect(state.url) {
                        if (state.url != null) {
                            urlHandler.openUri(state.url!!)
                            component.cleanUrl()
                        }
                    }

                    // Mark done confirmation
                    if (state.chunkToDone != null) {
                        ConfirmDialog(
                            title = stringResource(Res.string.chunk_checklist_title),
                            message = htmlToAnnotatedString(
                                stringResource(Res.string.chunk_checklist_body)
                            ),
                            onDismiss = { component.onDoneConfirmed(false) },
                            onConfirm = { component.onDoneConfirmed(true) }
                        )
                    }

                    // Mark all chunks done confirmation
                    when (val dialogState = state.markAllDoneState) {
                        is MarkAllDialogState.Confirm -> {
                            ConfirmDialog(
                                title = stringResource(Res.string.project_checklist_title),
                                message = htmlToAnnotatedString(
                                    stringResource(Res.string.project_checklist_body)
                                ),
                                onDismiss = {
                                    component.onMarkAllDoneConfirmed(false)
                                },
                                onConfirm = {
                                    component.onMarkAllDoneConfirmed(true)
                                }
                            )
                        }
                        is MarkAllDialogState.Result -> {
                            BaseDialog(
                                onDismiss = {
                                    component.onMarkAllDoneConfirmed(false)
                                },
                                title = stringResource(Res.string.result),
                                message = htmlToAnnotatedString(
                                    stringResource(
                                        Res.string.mark_chunks_done_result,
                                        dialogState.marked,
                                        dialogState.total
                                    )
                                ).text,
                                buttons = { onDismiss ->
                                    TextButton(onClick = onDismiss) {
                                        Text(stringResource(Res.string.dismiss))
                                    }
                                }
                            )
                        }
                        null -> { /* No dialog */ }
                    }
                }
            ) { item ->
                when (item.projectTypeClass) {
                    ProjectTypeClass.HELPS -> HelpsReviewCard(
                        item = item,
                        sourceTabs = sharedState.sourceTabs,
                        typography = typography,
                        resourcesOpen = state.resourcesOpen,
                        onSourceTabClick = parentComponent::selectSource,
                        onAddNewSourceClick = onSourceDialogOpen,
                        onRemoveSourceClick = parentComponent::removeSource,
                        onHelpsChanged = { component.onHelpsChanged(item, it) },
                        onDoneToggle = { component.onToggleDone(item) },
                        onExpandedChange = { expanded ->
                            component.openResources(expanded)
                            if (!expanded) component.clearHelp()
                        },
                        onRenderHelps = { component.renderHelps(item) },
                        onHelpClick = component::openHelp,
                        onUndoClick = { component.undo(item) },
                        onRedoClick = { component.redo(item) },
                        onConflictSelected = { component.selectConflict(item, it) },
                        modifier = Modifier.padding(start = 16.dp)
                    )
                    ProjectTypeClass.EXTANT -> WordsReviewCard(
                        item = item,
                        sourceTabs = sharedState.sourceTabs,
                        typography = typography,
                        onSourceTabClick = parentComponent::selectSource,
                        onAddNewSourceClick = onSourceDialogOpen,
                        onRemoveSourceClick = parentComponent::removeSource,
                        onHelpsChanged = { component.onHelpsChanged(item, it) },
                        onDoneToggle = { component.onToggleDone(item) },
                        onUndoClick = { component.undo(item) },
                        onRedoClick = { component.redo(item) },
                        onConflictSelected = { component.selectConflict(item, it) },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    ProjectTypeClass.STANDARD -> ReviewCard(
                        item = item,
                        sourceTabs = sharedState.sourceTabs,
                        typography = typography,
                        resourcesOpen = state.resourcesOpen,
                        onSourceTabClick = parentComponent::selectSource,
                        onAddNewSourceClick = onSourceDialogOpen,
                        onRemoveSourceClick = parentComponent::removeSource,
                        onTextChange = { component.onItemTextChanged(item, it) },
                        onExpandedChange = { expanded ->
                            component.openResources(expanded)
                            if (!expanded) component.clearHelp()
                        },
                        onRenderHelps = { component.renderHelps(item) },
                        onHelpClick = component::openHelp,
                        onEditToggle = { component.toggleEdit(item) },
                        onDoneToggle = { component.onToggleDone(item) },
                        onUndoClick = { component.undo(item) },
                        onRedoClick = { component.redo(item) },
                        onAddNoteClick = { caretPos ->
                            component.onAddNote(item, caretPos)
                        },
                        onDragDropVerse = { machineReadable, verseRawStart, verseRawEnd, targetRawPosition ->
                            component.onDragDropVerse(
                                item, machineReadable, verseRawStart, verseRawEnd, targetRawPosition
                            )
                        },
                        onConflictSelected = { component.selectConflict(item, it) },
                        sourceSearchQuery = state.search?.let { search ->
                            if (search.query.length >= 2 && search.subject == SearchSubject.SOURCE) {
                                search.query
                            } else null
                        },
                        targetSearchQuery = state.search?.let { search ->
                            if (search.query.length >= 2 && search.subject == SearchSubject.TARGET) {
                                search.query
                            } else null
                        },
                        modifier = Modifier.padding(start = 16.dp)
                    )
                }
            }
        }

        val helpPanelFraction = if (
            filteredItems.firstOrNull()?.projectTypeClass == ProjectTypeClass.HELPS
        ) 1f / 4.08f else 1f / 3.08f

        HelpPanel(
            help = state.help,
            typography = typography,
            sourceLanguage = sharedState.resourceContainer?.language,
            onClearHelp = component::clearHelp,
            onOpenIndex = component::openIndex,
            onOpenWord = component::openWord,
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(helpPanelFraction)
                .align(Alignment.CenterEnd)
        )
    }

    progress?.let {
        ProgressDialog(
            message = it.message,
            progress = it.value
        )
    }
}

@Composable
private fun HelpPanel(
    help: Help?,
    typography: Typography,
    sourceLanguage: Language?,
    onClearHelp: () -> Unit,
    onOpenIndex: (rcSlug: String) -> Unit,
    onOpenWord: (rcSlug: String, slug: String) -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = help != null,
        modifier = modifier,
        enter = slideInHorizontally(
            initialOffsetX = { it },
            animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy)
        ),
        exit = slideOutHorizontally(
            targetOffsetX = { it },
            animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy)
        )
    ) {
        help?.let { currentHelp ->
            when (currentHelp) {
                is Help.Notes -> NotesCard(
                    title = currentHelp.title,
                    body = currentHelp.body,
                    typography = typography,
                    sourceLanguage = sourceLanguage,
                    onClose = onClearHelp
                )
                is Help.Words -> WordsCard(
                    title = currentHelp.title,
                    body = currentHelp.body,
                    typography = typography,
                    sourceLanguage = sourceLanguage,
                    onCloseClick = onClearHelp,
                    onIndexClick = {
                        onOpenIndex(currentHelp.rcSlug)
                    }
                )
                is Help.Questions -> QuestionsCard(
                    title = currentHelp.title,
                    body = currentHelp.body,
                    typography = typography,
                    sourceLanguage = sourceLanguage,
                    onClose = onClearHelp
                )
                is Help.Index -> IndexCard(
                    words = currentHelp.words,
                    typography = typography,
                    sourceLanguage = sourceLanguage,
                    onCloseClick = onClearHelp,
                    onItemClick = {
                        onOpenWord(currentHelp.rcSlug, it.slug)
                    }
                )
            }
        }
    }
}
