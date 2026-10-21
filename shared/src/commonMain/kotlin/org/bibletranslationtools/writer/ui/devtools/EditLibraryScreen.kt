package org.bibletranslationtools.writer.ui.devtools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
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
import btt_writer.shared.generated.resources.Res
import btt_writer.shared.generated.resources.action_save
import btt_writer.shared.generated.resources.direction_ltr
import btt_writer.shared.generated.resources.direction_rtl
import btt_writer.shared.generated.resources.dismiss
import btt_writer.shared.generated.resources.edit_library
import btt_writer.shared.generated.resources.edit_library_confirm_save
import btt_writer.shared.generated.resources.edit_library_save_failed
import btt_writer.shared.generated.resources.edit_library_save_success
import btt_writer.shared.generated.resources.error
import btt_writer.shared.generated.resources.field_anglicized_name
import btt_writer.shared.generated.resources.field_direction
import btt_writer.shared.generated.resources.field_is_gateway_language
import btt_writer.shared.generated.resources.field_name
import btt_writer.shared.generated.resources.field_region
import btt_writer.shared.generated.resources.field_slug
import btt_writer.shared.generated.resources.label_continue
import btt_writer.shared.generated.resources.label_ok
import btt_writer.shared.generated.resources.search_hint
import org.bibletranslationtools.resourcecatalog.library.models.TargetLanguage
import org.bibletranslationtools.writer.ui.components.LabeledCheckbox
import org.bibletranslationtools.writer.ui.components.SearchBar
import org.bibletranslationtools.writer.ui.dialogs.BaseDialog
import org.bibletranslationtools.writer.ui.dialogs.ConfirmDialog
import org.bibletranslationtools.writer.ui.dialogs.ProgressDialog
import org.bibletranslationtools.writer.ui.newtranslation.LanguagesList
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditLibraryScreen(component: EditLibraryComponent) {
    val state by component.state.collectAsStateWithLifecycle()
    val progress by component.progress.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.edit_library)) },
                navigationIcon = {
                    IconButton(onClick = {
                        if (state.selected != null) component.clearSelected() else component.navigateBack()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "back",
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }
                },
                actions = {
                    if (state.selected == null) {
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
            modifier = Modifier.fillMaxWidth().padding(paddingValues)
        ) {
            val selected = state.selected
            if (selected == null) {
                LanguagesList(
                    languages = state.filteredLanguages,
                    disabledLanguages = emptyList(),
                    onLanguageSelected = component::onLanguageSelected,
                    modifier = Modifier.width(800.dp)
                )
            } else {
                TargetLanguageForm(
                    language = selected,
                    regions = state.regions,
                    onSave = component::save,
                    modifier = Modifier.width(800.dp)
                )
            }
        }
    }

    when (state.saveResult) {
        true -> BaseDialog(
            onDismiss = component::restart,
            title = stringResource(Res.string.edit_library),
            message = stringResource(Res.string.edit_library_save_success)
        ) {
            TextButton(onClick = component::restart) {
                Text(stringResource(Res.string.dismiss))
            }
        }
        false -> BaseDialog(
            onDismiss = component::clearSaveResult,
            title = stringResource(Res.string.error),
            message = stringResource(Res.string.edit_library_save_failed)
        ) {
            TextButton(onClick = component::clearSaveResult) {
                Text(stringResource(Res.string.label_ok))
            }
        }
        null -> {}
    }

    progress?.let { progress ->
        ProgressDialog(
            message = progress.message,
            progress = progress.value
        )
    }
}

@Composable
private fun TargetLanguageForm(
    language: TargetLanguage,
    regions: List<String>,
    onSave: (TargetLanguage) -> Unit,
    modifier: Modifier = Modifier
) {
    var name by rememberSaveable(language) { mutableStateOf(language.name) }
    var anglicizedName by rememberSaveable(language) { mutableStateOf(language.anglicizedName) }
    var direction by rememberSaveable(language) { mutableStateOf(language.direction) }
    var region by rememberSaveable(language) { mutableStateOf(language.region) }
    var isGateway by rememberSaveable(language) { mutableStateOf(language.isGatewayLanguage) }
    var confirmSave by rememberSaveable { mutableStateOf(false) }

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        OutlinedTextField(
            value = language.slug,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(Res.string.field_slug)) },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            singleLine = true,
            label = { Text(stringResource(Res.string.field_name)) },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = anglicizedName,
            onValueChange = { anglicizedName = it },
            singleLine = true,
            label = { Text(stringResource(Res.string.field_anglicized_name)) },
            modifier = Modifier.fillMaxWidth()
        )

        Text(stringResource(Res.string.field_direction), style = MaterialTheme.typography.titleSmall)
        RadioOption(stringResource(Res.string.direction_ltr), direction == "ltr") { direction = "ltr" }
        RadioOption(stringResource(Res.string.direction_rtl), direction == "rtl") { direction = "rtl" }

        Text(stringResource(Res.string.field_region), style = MaterialTheme.typography.titleSmall)
        regions.forEach { option ->
            RadioOption(option, region == option) { region = option }
        }

        LabeledCheckbox(
            label = stringResource(Res.string.field_is_gateway_language),
            checked = isGateway,
            onCheckedChange = { isGateway = it }
        )

        Button(
            onClick = { confirmSave = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(Res.string.action_save))
        }
    }

    if (confirmSave) {
        ConfirmDialog(
            title = stringResource(Res.string.edit_library),
            message = stringResource(Res.string.edit_library_confirm_save),
            confirmText = stringResource(Res.string.label_continue),
            onConfirm = {
                confirmSave = false
                onSave(
                    language.copy(
                        name = name,
                        anglicizedName = anglicizedName,
                        direction = direction,
                        region = region,
                        isGatewayLanguage = isGateway
                    )
                )
            },
            onDismiss = { confirmSave = false }
        )
    }
}

@Composable
private fun RadioOption(label: String, selected: Boolean, onSelect: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onSelect)
    ) {
        RadioButton(selected = selected, onClick = onSelect)
        Text(label)
    }
}
