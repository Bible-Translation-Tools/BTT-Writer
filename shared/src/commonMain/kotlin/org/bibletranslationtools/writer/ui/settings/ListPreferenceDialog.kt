package org.bibletranslationtools.writer.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import btt_writer.shared.generated.resources.Res
import btt_writer.shared.generated.resources.search_hint
import org.bibletranslationtools.writer.ui.dialogs.OverlayDialog
import org.jetbrains.compose.resources.stringResource

@Composable
fun ListPreferenceDialog(
    title: String,
    entries: List<String>,
    entryValues: List<String>,
    selectedValue: String,
    onValueSelected: (String) -> Unit,
    onDismissRequest: () -> Unit,
    searchable: Boolean = false
) {
    var query by remember { mutableStateOf("") }

    // Pair each display name with its value, then filter by the search query.
    val visible = entries.mapIndexedNotNull { index, name ->
        val value = entryValues.getOrNull(index) ?: return@mapIndexedNotNull null
        name to value
    }.filter { (name, _) ->
        query.isBlank() || name.contains(query.trim(), ignoreCase = true)
    }

    OverlayDialog(
        onDismiss = onDismissRequest
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp
            )

            if (searchable) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    placeholder = { Text(stringResource(Res.string.search_hint)) },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            val listState = rememberLazyListState()
            LaunchedEffect(Unit) {
                val selectedIndex = visible.indexOfFirst { it.second == selectedValue }
                if (selectedIndex >= 0) listState.scrollToItem(selectedIndex)
            }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .selectableGroup()
                    .padding(vertical = 8.dp)
            ) {
                items(visible, key = { it.second }) { (entryDisplayName, entryValue) ->
                    val isSelected = entryValue == selectedValue

                    Row(
                        Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = isSelected,
                                onClick = { onValueSelected(entryValue) },
                                role = Role.RadioButton
                            )
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = null
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = entryDisplayName,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}