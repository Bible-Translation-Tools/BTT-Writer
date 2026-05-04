package org.bibletranslationtools.writer.ui.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import be.digitalia.compose.htmlconverter.htmlToAnnotatedString
import btt_writer.composeapp.generated.resources.Res
import btt_writer.composeapp.generated.resources.label_close
import org.bibletranslationtools.writer.utils.resolveStringResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun LegalDocumentDialog(
    htmlResourceId: String,
    onDismissRequest: () -> Unit
) {
    val htmlString = stringResource(resolveStringResource(htmlResourceId))
    val parsedHtml = remember { htmlToAnnotatedString(htmlString) }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize()
                .padding(16.dp),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Text(
                    text = parsedHtml,
                    color = MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                )

                TextButton(
                    onClick = onDismissRequest,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                ) {
                    Text(
                        text = stringResource(Res.string.label_close)
                    )
                }
            }
        }
    }
}