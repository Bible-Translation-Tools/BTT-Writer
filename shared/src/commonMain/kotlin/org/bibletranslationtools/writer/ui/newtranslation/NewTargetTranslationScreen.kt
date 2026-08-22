package org.bibletranslationtools.writer.ui.newtranslation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import btt_writer.shared.generated.resources.Res
import btt_writer.shared.generated.resources.no
import btt_writer.shared.generated.resources.search_hint
import btt_writer.shared.generated.resources.title_activity_new_target_translation
import btt_writer.shared.generated.resources.warn_existing_target_translation_label
import btt_writer.shared.generated.resources.yes
import org.bibletranslationtools.writer.ui.components.SearchBar
import org.bibletranslationtools.writer.ui.dialogs.ConfirmDialog
import org.bibletranslationtools.writer.ui.dialogs.ProgressDialog
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewTargetTranslationScreen(
    component: NewTranslationComponent
) {
    val state by component.state.collectAsStateWithLifecycle()
    val progress by component.progress.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(Res.string.title_activity_new_target_translation))
                },
                navigationIcon = {
                    IconButton(onClick = {
                        when (state.screenStep) {
                            ScreenStep.LANGUAGE -> component.navigateBack()
                            ScreenStep.PROJECT -> component.onCategoryBack()
                            ScreenStep.TYPE -> component.onTypeBack()
                        }
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "back",
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }
                },
                actions = {
                    if (state.screenStep != ScreenStep.TYPE) {
                        SearchBar(
                            query = state.searchQuery,
                            onQueryChanged = component::onSearch,
                            placeholder = stringResource(Res.string.search_hint)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    titleContentColor = MaterialTheme.colorScheme.onSecondary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSecondary
                )
            )
        }
    ) { paddingValues ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(paddingValues)
        ) {
            val animationKey = when (state.screenStep) {
                ScreenStep.LANGUAGE -> 0
                ScreenStep.PROJECT -> state.categoryStack.size
                ScreenStep.TYPE -> state.categoryStack.size + 1
            }
            val forward = state.navigatingForward

            AnimatedContent(
                targetState = animationKey,
                transitionSpec = {
                    if (forward) {
                        slideInHorizontally { it } togetherWith slideOutHorizontally { -it }
                    } else {
                        slideInHorizontally { -it } togetherWith slideOutHorizontally { it }
                    }
                },
                label = "screen_transition"
            ) { _ ->
                when (state.screenStep) {
                    ScreenStep.LANGUAGE -> {
                        LanguagesList(
                            languages = state.filteredLanguages,
                            disabledLanguages = state.disabledLanguages,
                            onLanguageSelected = component::onLanguageSelected,
                            modifier = Modifier.width(800.dp)
                        )
                    }
                    ScreenStep.PROJECT -> {
                        ProjectList(
                            categories = state.filteredCategories,
                            onProjectSelected = component::onProjectSelected,
                            onCategorySelected = component::onCategorySelected,
                            modifier = Modifier.width(800.dp)
                        )
                    }
                    ScreenStep.TYPE -> {
                        TypeList(
                            options = state.typeOptions,
                            onTypeSelected = component::onTypeSelected,
                            modifier = Modifier.width(800.dp)
                        )
                    }
                }
            }
        }
    }

    state.mergeConflict?.let { conflict ->
        ConfirmDialog(
            title = stringResource(Res.string.warn_existing_target_translation_label),
            message = conflict.message,
            onDismiss = component::clearMergeConflict,
            onConfirm = {
                component.mergeTranslation(conflict)
            },
            confirmText = stringResource(Res.string.yes),
            dismissText = stringResource(Res.string.no)
        )
    }

    progress?.let {
        ProgressDialog(
            message = it.message,
            progress = it.value
        )
    }
}
