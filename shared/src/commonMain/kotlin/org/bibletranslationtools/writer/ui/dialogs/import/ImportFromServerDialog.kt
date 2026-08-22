package org.bibletranslationtools.writer.ui.dialogs.import

import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import btt_writer.shared.generated.resources.Project
import btt_writer.shared.generated.resources.Res
import btt_writer.shared.generated.resources.action_search
import btt_writer.shared.generated.resources.dismiss
import btt_writer.shared.generated.resources.import_from_door43
import btt_writer.shared.generated.resources.language
import btt_writer.shared.generated.resources.translation_name
import btt_writer.shared.generated.resources.unsupported_project_name
import btt_writer.shared.generated.resources.username
import org.bibletranslationtools.writer.ui.dialogs.OverlayDialog
import org.bibletranslationtools.writer.core.TextStyleType
import org.bibletranslationtools.writer.core.TranslationType
import org.bibletranslationtools.writer.core.Typography
import org.bibletranslationtools.writer.ui.home.RepositoryItem
import org.bibletranslationtools.writer.utils.getComposeTextStyle
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

@Composable
fun ImportFromServerDialog(
    repositories: List<RepositoryItem>,
    onSearch: (username: String, translationId: String) -> Unit,
    onRepoSelected: (RepositoryItem) -> Unit,
    onDismiss: () -> Unit
) {
    val typography: Typography = koinInject()

    var username by rememberSaveable { mutableStateOf("") }
    var translationId by rememberSaveable { mutableStateOf("") }

    val snackbarHostState = remember { SnackbarHostState() }
    val translationIdFocus = remember { FocusRequester() }
    val searchButtonFocus = remember { FocusRequester() }

    OverlayDialog(
        onDismiss = onDismiss,
        maxWidth = 1000.dp,
        maxHeight = 1000.dp,
        snackbarHostState = snackbarHostState,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(top = 8.dp)
        ) {
            Text(
                text = stringResource(Res.string.import_from_door43),
                fontSize = 24.sp
            )

            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextField(
                        value = username,
                        onValueChange = { username = it },
                        placeholder = { Text(stringResource(Res.string.username)) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(
                            onNext = { translationIdFocus.requestFocus() }
                        )
                    )
                    TextField(
                        value = translationId,
                        onValueChange = { translationId = it },
                        placeholder = { Text(stringResource(Res.string.translation_name)) },
                        modifier = Modifier.weight(1f).focusRequester(translationIdFocus),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                onSearch(username, translationId)
                                searchButtonFocus.requestFocus()
                            }
                        )
                    )
                }

                Button(
                    onClick = {
                        onSearch(username, translationId)
                    },
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(vertical = 8.dp)
                        .focusable(true)
                        .focusRequester(searchButtonFocus),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(stringResource(Res.string.action_search))
                }
            }

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(stringResource(Res.string.Project))
                Text(stringResource(Res.string.language))
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                itemsIndexed(repositories) { index, repo ->
                    RepositoryCard(
                        repo = repo,
                        typography = typography,
                        onClick = { onRepoSelected(repo) }
                    )
                    if (index < repositories.lastIndex) {
                        HorizontalDivider()
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = onDismiss,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(stringResource(Res.string.dismiss))
                }
            }
        }
    }
}

@Composable
private fun RepositoryCard(
    repo: RepositoryItem,
    typography: Typography,
    onClick: () -> Unit
) {
    val languageStyle = typography.getComposeTextStyle(
        translationType = TranslationType.TARGET,
        style = TextStyleType.SUB,
        languageCode = repo.languageCode,
        direction = repo.languageDirection
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 70.dp)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.LibraryBooks,
                contentDescription = "repo",
                modifier = Modifier.size(24.dp)
            )

            Text(
                text = if (repo.isSupported) {
                    repo.projectName
                } else stringResource(
                    Res.string.unsupported_project_name,
                    repo.projectName,
                    repo.unsupportedTag
                ),
                fontWeight = if (repo.isSupported) FontWeight.Bold else FontWeight.Normal,
                color = if (repo.isSupported) {
                    MaterialTheme.colorScheme.onSurface
                } else MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = repo.languageName,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.End,
                style = languageStyle
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (repo.isPrivate) Icons.Default.Lock else Icons.Default.LockOpen,
                contentDescription = "private",
                modifier = Modifier.size(18.dp)
            )

            Text(
                text = repo.url,
                modifier = Modifier
                    .padding(horizontal = 2.dp)
                    .fillMaxWidth(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 12.sp
            )
        }
    }
}