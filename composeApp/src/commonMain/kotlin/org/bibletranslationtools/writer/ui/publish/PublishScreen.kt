package org.bibletranslationtools.writer.ui.publish

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import btt_writer.composeapp.generated.resources.Res
import btt_writer.composeapp.generated.resources.menu_upload_export
import btt_writer.composeapp.generated.resources.need_translator_notice
import btt_writer.composeapp.generated.resources.publish_translation
import btt_writer.composeapp.generated.resources.title_book
import btt_writer.composeapp.generated.resources.translators
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import kotlinx.coroutines.launch
import org.bibletranslationtools.writer.core.Typography
import org.bibletranslationtools.writer.ui.components.CardsSkeletonList
import org.bibletranslationtools.writer.ui.components.LocalSnackbarHostState
import org.bibletranslationtools.writer.ui.dialogs.export.ExportDialog
import org.bibletranslationtools.writer.ui.dialogs.feedback.FeedbackDialog
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

private enum class PublishSection {
    VALIDATION,
    TRANSLATORS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublishScreen(
    component: PublishComponent
) {
    val typography: Typography = koinInject()

    val state by component.state.collectAsStateWithLifecycle()
    val dialogSlot by component.dialogSlot.subscribeAsState()

    var publishSection by rememberSaveable {
        mutableStateOf(PublishSection.VALIDATION)
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val noTranslatorsMessage = stringResource(Res.string.need_translator_notice)

    CompositionLocalProvider(LocalSnackbarHostState provides snackbarHostState) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(stringResource(Res.string.publish_translation))
                    },
                    navigationIcon = {
                        IconButton(onClick = component::navigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "back",
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        titleContentColor = MaterialTheme.colorScheme.onSecondary,
                        navigationIconContentColor = MaterialTheme.colorScheme.onSecondary
                    )
                )
            },
            snackbarHost = {
                SnackbarHost(snackbarHostState)
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.secondary)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val buttonModifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp)

                        PublishButton(
                            text = stringResource(Res.string.title_book),
                            selected = publishSection == PublishSection.VALIDATION,
                            modifier = buttonModifier,
                            onClick = { publishSection = PublishSection.VALIDATION }
                        )

                        PublishButton(
                            text = stringResource(Res.string.translators),
                            selected = publishSection == PublishSection.TRANSLATORS,
                            modifier = buttonModifier,
                            onClick = { publishSection = PublishSection.TRANSLATORS }
                        )

                        PublishButton(
                            text = stringResource(Res.string.menu_upload_export),
                            selected = false,
                            modifier = buttonModifier,
                            onClick = component::showExportDialog
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    if (state.isLoading) {
                        CardsSkeletonList()
                    } else {
                        when (publishSection) {
                            PublishSection.VALIDATION -> ValidationSection(
                                items = state.validations,
                                typography = typography,
                                onNext = { publishSection = PublishSection.TRANSLATORS },
                                onReview = component::openReview
                            )
                            PublishSection.TRANSLATORS -> TranslatorsSection(
                                translators = state.translators,
                                targetTranslation = component.targetTranslation,
                                onNextClick = {
                                    if (state.translators.isNotEmpty()) {
                                        component.showExportDialog()
                                    } else {
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar(
                                                noTranslatorsMessage
                                            )
                                        }
                                    }
                                },
                                onContributorsChanged = component::refreshContributors
                            )
                        }
                    }
                }
            }
        }

        dialogSlot.child?.instance?.let { child ->
            when (child) {
                is PublishComponent.DialogChild.Export -> ExportDialog(
                    component = child.component,
                    onDismiss = component::dismissDialog
                )
                is PublishComponent.DialogChild.Feedback -> FeedbackDialog(
                    component = child.component,
                    onDismiss = component::dismissDialog
                )
            }
        }
    }
}

@Composable
fun PublishButton(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(4.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) MaterialTheme.colorScheme.secondaryFixed else {
                MaterialTheme.colorScheme.surfaceVariant
            },
            contentColor = if (selected) Color.White else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    ) {
        Text(
            text = text,
            fontSize = 18.sp,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}