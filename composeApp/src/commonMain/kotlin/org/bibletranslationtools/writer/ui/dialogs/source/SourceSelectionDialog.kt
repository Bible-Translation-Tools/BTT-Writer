package org.bibletranslationtools.writer.ui.dialogs.source

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import btt_writer.composeapp.generated.resources.Res
import btt_writer.composeapp.generated.resources.available
import btt_writer.composeapp.generated.resources.available_online
import btt_writer.composeapp.generated.resources.choose_source_translations
import btt_writer.composeapp.generated.resources.confirm
import btt_writer.composeapp.generated.resources.confirm_delete_project
import btt_writer.composeapp.generated.resources.download_source_language
import btt_writer.composeapp.generated.resources.label_delete
import btt_writer.composeapp.generated.resources.selected
import btt_writer.composeapp.generated.resources.title_cancel
import btt_writer.composeapp.generated.resources.title_download_source_language
import btt_writer.composeapp.generated.resources.update_sources_label
import btt_writer.composeapp.generated.resources.update_warning
import btt_writer.composeapp.generated.resources.warning_title
import org.bibletranslationtools.writer.ui.dialogs.ConfirmDialog
import org.bibletranslationtools.writer.ui.dialogs.OverlayDialog
import org.bibletranslationtools.writer.ui.dialogs.ProgressDialog
import org.bibletranslationtools.writer.ui.translate.components.SourceHeaderRow
import org.bibletranslationtools.writer.ui.translate.components.SourceItemRow
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SourceSelectionDialog(
    component: SelectSourcesComponent,
    onDismiss: () -> Unit
) {
    val state by component.state.collectAsStateWithLifecycle()
    val progress by component.progress.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    var searchQuery by rememberSaveable { mutableStateOf("") }

    val selectedString = stringResource(Res.string.selected)
    val availableString = stringResource(Res.string.available)
    val onlineString = stringResource(Res.string.available_online)

    var sourceToDownload by rememberSaveable { mutableStateOf<RCItem?>(null) }
    var sourceToDelete by rememberSaveable { mutableStateOf<RCItem?>(null) }
    var showUpdateSourcesDialog by rememberSaveable { mutableStateOf(false) }

    val uiState by remember(state.sources, searchQuery) {
        derivedStateOf {
            prepareSourceState(
                sources = state.sources,
                searchText = searchQuery,
                selectedString = selectedString,
                availableString = availableString,
                availableOnlineString = onlineString
            )
        }
    }

    LaunchedEffect(component) {
        component.event.collect {
            when (it) {
                is SelectSourcesComponent.Event.SnackbarMessage -> {
                    snackbarHostState.showSnackbar(it.message)
                }
            }
        }
    }

    OverlayDialog(
        snackbarHostState = snackbarHostState,
        onDismiss = onDismiss
    ) { dismissWithKeyboard ->
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            placeholder = { Text(stringResource(Res.string.choose_source_translations)) },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = "Search")
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(
                            Icons.Default.Clear,
                            contentDescription = "Clear search"
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(50),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            )
        )

        LazyColumn(
            modifier = Modifier.fillMaxWidth()
                .weight(1f)
        ) {
            itemsIndexed(uiState.filteredList) { index, item ->
                if (uiState.headerIndices.contains(index)) {
                    SourceHeaderRow(
                        title = item.title,
                        showStatusIcons = item.hasUpdates
                    )
                } else {
                    SourceItemRow(
                        item = item,
                        onTriggerSelected = component::toggleSelection,
                        onTriggerDownload = { sourceToDownload = it },
                        onTriggerDelete = {
                            if (it.downloaded) {
                                sourceToDelete = it
                            }
                        }
                    )
                }
            }
        }

        HorizontalDivider()

        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = { dismissWithKeyboard(component::onUpdateSources) },
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = stringResource(Res.string.update_sources_label),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary
                )
            }
            TextButton(onClick = { dismissWithKeyboard(onDismiss) }) {
                Text(
                    stringResource(Res.string.title_cancel),
                    color = MaterialTheme.colorScheme.primary
                )
            }
            TextButton(
                onClick = {
                    dismissWithKeyboard(component::onConfirmSources)
                }
            ) {
                Text(
                    stringResource(Res.string.confirm),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }

    if (showUpdateSourcesDialog) {
        ConfirmDialog(
            title = stringResource(Res.string.warning_title),
            message = stringResource(Res.string.update_warning),
            onConfirm = component::onUpdateSources,
            onDismiss = { showUpdateSourcesDialog = false }
        )
    }

    sourceToDownload?.let { source ->
        ConfirmDialog(
            title = stringResource(Res.string.title_download_source_language),
            message = stringResource(Res.string.download_source_language, source.title),
            onConfirm = {
                sourceToDownload = null
                component.downloadSource(source)
            },
            onDismiss = { sourceToDownload = null }
        )
    }

    sourceToDelete?.let { source ->
        ConfirmDialog(
            title = stringResource(Res.string.label_delete),
            message = stringResource(Res.string.confirm_delete_project),
            onConfirm = {
                sourceToDelete = null
                component.deleteSource(source)
            },
            onDismiss = { sourceToDelete = null }
        )
    }

    progress?.let {
        ProgressDialog(
            message = it.message,
            progress = it.value
        )
    }
}

private fun prepareSourceState(
    sources: List<RCItem>,
    searchText: String,
    selectedString: String,
    availableString: String,
    availableOnlineString: String
): SourceSelectionState {
    val filtered = if (searchText.isBlank()) {
        sources
    } else {
        sources.filter { item ->
            val langCode = item.sourceTranslation?.language?.slug ?: ""
            val langName = item.sourceTranslation?.language?.name ?: ""
            val resourceParts = item.sourceTranslation?.resource?.name
                ?.split("-")?.map { it.trim() } ?: emptyList()

            val matchesCode = langCode.startsWith(searchText, ignoreCase = true)
            val matchesName = langName.startsWith(searchText, ignoreCase = true)
            val matchesResource = resourceParts.any { it.startsWith(searchText, ignoreCase = true) }

            matchesCode || matchesName || matchesResource
        }
    }

    val sortedItems = filtered.sortedBy { it.sourceTranslation?.language?.slug ?: "" }

    val selected = sortedItems.filter { it.selected }
    val available = sortedItems.filter { !it.selected && it.downloaded }
    val downloadable = sortedItems.filter { !it.selected && !it.downloaded }

    val flatList = mutableListOf<RCItem>()
    val headers = mutableSetOf<Int>()

    // Helper to add sections
    fun addSection(title: String, items: List<RCItem>, needsInternet: Boolean) {
        if (items.isEmpty()) return

        headers.add(flatList.size)

        val headerItem = RCItem(
            title,
            null,
            selected = false,
            downloaded = false,
            hasUpdates = needsInternet
        )

        flatList.add(headerItem)
        flatList.addAll(items)
    }

    val selectedNeedsInternet = selected.any { it.hasUpdates }
    addSection(selectedString, selected, selectedNeedsInternet)

    addSection(availableString, available, false)
    addSection(availableOnlineString, downloadable, true)

    return SourceSelectionState(flatList, headers)
}

private data class SourceSelectionState(
    val filteredList: List<RCItem> = emptyList(),
    val headerIndices: Set<Int> = emptySet()
)