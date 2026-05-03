package org.bibletranslationtools.writer.ui.dialogs.import

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import btt_writer.composeapp.generated.resources.Res
import btt_writer.composeapp.generated.resources.dismiss
import btt_writer.composeapp.generated.resources.import_merge_conflict_project_name
import btt_writer.composeapp.generated.resources.import_merge_conflicts
import btt_writer.composeapp.generated.resources.import_usfm_failed
import btt_writer.composeapp.generated.resources.import_usfm_success
import btt_writer.composeapp.generated.resources.label_continue
import btt_writer.composeapp.generated.resources.menu_cancel
import btt_writer.composeapp.generated.resources.merge_conflict_title
import btt_writer.composeapp.generated.resources.merge_projects_label
import btt_writer.composeapp.generated.resources.overwrite_projects_label
import btt_writer.composeapp.generated.resources.search_for_language
import btt_writer.composeapp.generated.resources.title_activity_import_usfm_book
import btt_writer.composeapp.generated.resources.title_activity_import_usfm_language
import btt_writer.composeapp.generated.resources.title_cancel
import btt_writer.composeapp.generated.resources.title_import_usfm_error
import btt_writer.composeapp.generated.resources.title_import_usfm_results
import btt_writer.composeapp.generated.resources.title_processing_usfm_summary
import org.bibletranslationtools.writer.ui.dialogs.BaseDialog
import org.bibletranslationtools.writer.ui.dialogs.ConfirmDialog
import org.bibletranslationtools.writer.ui.dialogs.OverlayDialog
import org.bibletranslationtools.writer.ui.dialogs.ProgressDialog
import org.bibletranslationtools.writer.ui.newtranslation.LanguagesList
import org.bibletranslationtools.writer.ui.newtranslation.ProjectList
import org.bibletranslationtools.resourcecatalog.library.models.CategoryEntry
import org.bibletranslationtools.resourcecatalog.library.models.TargetLanguage
import org.bibletranslationtools.writer.ui.components.SearchBar
import org.jetbrains.compose.resources.stringResource

@Composable
fun ImportUsfmDialog(
    component: ImportUsfmComponent,
    onDismiss: () -> Unit
) {
    val state by component.state.collectAsStateWithLifecycle()
    val progress by component.progress.collectAsStateWithLifecycle()

    if (!state.started) return

    when (state.step) {
        UsfmStep.LANGUAGE -> {
            UsfmLanguageSelectionDialog(
                languages = state.filteredLanguages,
                onLanguageSelected = component::languageSelected,
                onSearch = component::search,
                onDismiss = onDismiss
            )
        }

        UsfmStep.PROMPT_BOOK_NAME -> {
            UsfmBookNameDialog(
                prompt = state.missingNamePrompt ?: "",
                description = state.currentMissingDescription,
                categories = state.filteredCategories,
                isAtRootCategory = state.categoryStack.size <= 1,
                onProjectSelected = component::bookSelected,
                onCategorySelected = component::categorySelected,
                onNavigateBack = component::navigateBack,
                onSkip = component::skipBook
            )
        }

        UsfmStep.PROCESSED -> {
            if (state.existentTranslations.isNotEmpty()) {
                UsfmMergeConflictDialog(
                    message = state.processedResult,
                    conflictIds = state.existentTranslations.map { it.id },
                    onMerge = { component.mergeImport(false) },
                    onOverwrite = { component.mergeImport(true) },
                    onCancel = onDismiss
                )
            } else {
                ConfirmDialog(
                    title = stringResource(Res.string.title_processing_usfm_summary),
                    message = state.processedResult,
                    onConfirm = component::confirmImport,
                    onDismiss = onDismiss,
                    confirmText = stringResource(Res.string.label_continue),
                    dismissText = stringResource(Res.string.menu_cancel)
                )
            }
        }

        UsfmStep.DONE -> {
            BaseDialog(
                title = stringResource(
                    if (state.importSuccess) Res.string.title_import_usfm_results
                    else Res.string.title_import_usfm_error
                ),
                message = stringResource(
                    if (state.importSuccess) Res.string.import_usfm_success
                    else Res.string.import_usfm_failed
                ),
                onDismiss = {
                    if (state.importSuccess) {
                        component.onProjectsImported(state.importedTranslationIds)
                    } else {
                        onDismiss()
                    }
                }
            ) { onBaseDismiss ->
                TextButton(onClick = onBaseDismiss) {
                    Text(stringResource(Res.string.label_continue))
                }
            }
        }
    }

    state.infoMessage?.let { (title, message) ->
        BaseDialog(
            title = title,
            message = message,
            onDismiss = onDismiss
        ) { onBaseDismiss ->
            TextButton(onClick = onBaseDismiss) {
                Text(stringResource(Res.string.dismiss))
            }
        }
        return
    }

    progress?.let {
        ProgressDialog(
            message = it.message,
            progress = it.value
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UsfmLanguageSelectionDialog(
    languages: List<TargetLanguage>,
    onLanguageSelected: (TargetLanguage) -> Unit,
    onSearch: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }

    OverlayDialog(
        onDismiss = onDismiss,
        maxWidth = 900.dp,
        maxHeight = 900.dp,
        contentPadding = 0.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            TopAppBar(
                title = {
                    Text(stringResource(Res.string.title_activity_import_usfm_language))
                },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "back"
                        )
                    }
                },
                actions = {
                    SearchBar(
                        query = searchQuery,
                        onQueryChanged = {
                            searchQuery = it
                            onSearch(it)
                        },
                        placeholder = stringResource(Res.string.search_for_language)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                LanguagesList(
                    languages = languages,
                    disabledLanguages = emptyList(),
                    onLanguageSelected = onLanguageSelected,
                    modifier = Modifier.width(800.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UsfmBookNameDialog(
    prompt: String,
    description: String,
    categories: List<CategoryEntry>,
    isAtRootCategory: Boolean,
    onProjectSelected: (String) -> Unit,
    onCategorySelected: (Long) -> Unit,
    onNavigateBack: () -> Unit,
    onSkip: () -> Unit
) {
    var showProjectList by rememberSaveable { mutableStateOf(false) }

    if (!showProjectList) {
        ConfirmDialog(
            title = stringResource(Res.string.title_activity_import_usfm_language),
            message = prompt,
            onConfirm = { showProjectList = true },
            onDismiss = onSkip,
            confirmText = stringResource(Res.string.label_continue),
            dismissText = stringResource(Res.string.menu_cancel)
        )
    } else {
        OverlayDialog(
            onDismiss = {
                if (isAtRootCategory) {
                    showProjectList = false
                } else {
                    onNavigateBack()
                }
            },
            maxWidth = 900.dp,
            maxHeight = 900.dp,
            contentPadding = 0.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                TopAppBar(
                    title = {
                        Text(
                            stringResource(
                                Res.string.title_activity_import_usfm_book,
                                description
                            )
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            if (isAtRootCategory) {
                                showProjectList = false
                            } else {
                                onNavigateBack()
                            }
                        }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "back"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary,
                        navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                    )
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ProjectList(
                        categories = categories,
                        onProjectSelected = onProjectSelected,
                        onCategorySelected = { onCategorySelected(it) },
                        modifier = Modifier.width(800.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun UsfmMergeConflictDialog(
    message: String,
    conflictIds: List<String>,
    onMerge: () -> Unit,
    onOverwrite: () -> Unit,
    onCancel: () -> Unit
) {
    val warning = conflictIds.singleOrNull()?.let { conflictId ->
        stringResource(
            Res.string.import_merge_conflict_project_name,
            conflictId
        )
    } ?: stringResource(Res.string.import_merge_conflicts)

    val fullMessage = "$message\n$warning"

    BaseDialog(
        title = stringResource(Res.string.merge_conflict_title),
        message = fullMessage,
        onDismiss = onCancel
    ) { onBaseDismiss ->
        TextButton(onClick = onMerge) {
            Text(stringResource(Res.string.merge_projects_label))
        }
        TextButton(onClick = onOverwrite) {
            Text(stringResource(Res.string.overwrite_projects_label))
        }
        TextButton(onClick = {
            onCancel()
            onBaseDismiss()
        }) {
            Text(stringResource(Res.string.title_cancel))
        }
    }
}
