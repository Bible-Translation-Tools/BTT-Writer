package org.bibletranslationtools.writer.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import btt_writer.composeapp.generated.resources.Res
import btt_writer.composeapp.generated.resources.license_accept
import btt_writer.composeapp.generated.resources.license_deny
import btt_writer.composeapp.generated.resources.license_pdf
import btt_writer.composeapp.generated.resources.statement_of_faith
import btt_writer.composeapp.generated.resources.terms
import btt_writer.composeapp.generated.resources.terms_title
import btt_writer.composeapp.generated.resources.translation_guidlines
import btt_writer.composeapp.generated.resources.view_license_agreement
import btt_writer.composeapp.generated.resources.view_statement_of_faith
import btt_writer.composeapp.generated.resources.view_translation_guidelines
import org.bibletranslationtools.writer.ui.dialogs.LegalDocumentDialog
import org.bibletranslationtools.writer.ui.dialogs.ProgressDialog
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TermsOfUseScreen(component: TermsOfUseComponent) {
    var openLegalDocumentId by rememberSaveable { mutableStateOf<String?>(null) }

    val progress by component.progress.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = stringResource(Res.string.terms_title),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onPrimary
                    ) 
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(Res.string.terms),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            Button(
                onClick = { openLegalDocumentId = Res.string.license_pdf.key },
                modifier = Modifier.fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Text(stringResource(Res.string.view_license_agreement))
            }

            Button(
                onClick = { openLegalDocumentId = Res.string.translation_guidlines.key },
                modifier = Modifier.fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Text(stringResource(Res.string.view_translation_guidelines))
            }

            Button(
                onClick = { openLegalDocumentId = Res.string.statement_of_faith.key },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(Res.string.view_statement_of_faith))
            }

            Spacer(modifier = Modifier.weight(1f))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = component::rejectTerms,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(Res.string.license_deny))
                }

                Button(
                    onClick = component::acceptTerms,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(Res.string.license_accept))
                }
            }
        }
    }

    openLegalDocumentId?.let { resourceId ->
        LegalDocumentDialog(
            htmlResourceId = resourceId,
            onDismissRequest = { openLegalDocumentId = null }
        )
    }

    progress?.let { progress ->
        ProgressDialog(
            message = progress.message,
            progress = progress.value
        )
    }
}