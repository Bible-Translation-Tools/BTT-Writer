package org.bibletranslationtools.writer.ui.draft

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import btt_writer.composeapp.generated.resources.Res
import btt_writer.composeapp.generated.resources.dismiss
import btt_writer.composeapp.generated.resources.error
import btt_writer.composeapp.generated.resources.import_draft
import btt_writer.composeapp.generated.resources.import_draft_confirmation
import btt_writer.composeapp.generated.resources.label_import
import btt_writer.composeapp.generated.resources.preview
import btt_writer.composeapp.generated.resources.title_footnote
import btt_writer.composeapp.generated.resources.translation_import_failed
import org.bibletranslationtools.writer.core.Typography
import org.bibletranslationtools.writer.rendering.RenderingProvider
import org.bibletranslationtools.writer.ui.dialogs.BaseDialog
import org.bibletranslationtools.writer.ui.dialogs.ConfirmDialog
import org.bibletranslationtools.writer.ui.dialogs.ProgressDialog
import org.bibletranslationtools.writer.utils.sortNumerically
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DraftScreen(
    component: DraftComponent
) {
    val typography: Typography = koinInject()
    val renderingProvider: RenderingProvider = koinInject()

    val state by component.state.collectAsStateWithLifecycle()
    val progress by component.progress.collectAsStateWithLifecycle()

    var showConfirmDialog by remember { mutableStateOf(false) }
    var showErrorDialog by remember { mutableStateOf(false) }
    var showNoteDialog by remember { mutableStateOf<String?>(null) }

    val draftData = remember(state.draftTranslations) {
        state.draftTranslations.firstOrNull()?.let { draft ->
            val container = component.getResourceContainer(draft.resourceContainerSlug)
            val language = container?.let { component.getSourceLanguage(it) }
            if (container != null && language != null) {
                Pair(container, language)
            } else null
        }
    }

    LaunchedEffect(state.importResult) {
        state.importResult?.let { result ->
            if (result.targetTranslation != null) {
                component.onFinish()
            } else {
                showErrorDialog = true
            }
        }
    }

    LaunchedEffect(state.draftTranslations) {
        if (state.draftTranslations.isEmpty()) {
            component.onFinish()
        }
    }

   Scaffold(
       topBar = {
           TopAppBar(
               title = {
                   Text(stringResource(Res.string.preview))
               },
               navigationIcon = {
                   IconButton(onClick = component::onNavigateBack) {
                       Icon(
                           imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                           contentDescription = "back",
                           modifier = Modifier.padding(horizontal = 8.dp)
                       )
                   }
               },
               colors = TopAppBarDefaults.topAppBarColors(
                   containerColor = MaterialTheme.colorScheme.primary,
                   titleContentColor = MaterialTheme.colorScheme.onPrimary,
                   navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
               )
           )
       },
       floatingActionButton = {
           if (draftData != null) {
               FloatingActionButton(
                   onClick = { showConfirmDialog = true }
               ) {
                   Icon(
                       imageVector = Icons.Filled.Edit,
                       contentDescription = "Import Draft"
                   )
               }
           }
       }
    ) { paddingValues ->
        if (draftData != null) {
            val (container, language) = draftData
            val chapters = remember(container) {
                container.chapters().apply { sortNumerically() }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(chapters) { chapterSlug ->
                    var chapterContent by remember { mutableStateOf<ChapterContent?>(null) }

                    LaunchedEffect(chapterSlug) {
                        chapterContent = component.parseChapterContent(
                            chapterSlug = chapterSlug,
                            container = container,
                            renderingProvider = renderingProvider
                        )
                    }

                    DraftChapterCard(
                        chapterContent = chapterContent,
                        language = language,
                        typography = typography,
                        onNoteClick = { notes -> showNoteDialog = notes }
                    )
                }
            }
        }
    }

    if (showConfirmDialog && draftData != null) {
        ConfirmDialog(
            title = stringResource(Res.string.import_draft),
            message = stringResource(Res.string.import_draft_confirmation),
            onConfirm = {
                showConfirmDialog = false
                component.importDraft(draftData.first)
            },
            onDismiss = { showConfirmDialog = false },
            confirmText = stringResource(Res.string.label_import)
        )
    }

    if (showErrorDialog) {
        BaseDialog(
            onDismiss = { showErrorDialog = false },
            title = stringResource(Res.string.error),
            message = stringResource(Res.string.translation_import_failed)
        ) {
            TextButton(onClick = { showErrorDialog = false }) {
                Text(stringResource(Res.string.dismiss))
            }
        }
    }

    showNoteDialog?.let { notes ->
        BaseDialog(
            onDismiss = { showNoteDialog = null },
            title = stringResource(Res.string.title_footnote),
            message = notes
        ) {
            TextButton(onClick = { showNoteDialog = null }) {
                Text(stringResource(Res.string.dismiss))
            }
        }
    }

    progress?.let { progress ->
        ProgressDialog(
            message = progress.message,
            progress = progress.value
        )
    }
}