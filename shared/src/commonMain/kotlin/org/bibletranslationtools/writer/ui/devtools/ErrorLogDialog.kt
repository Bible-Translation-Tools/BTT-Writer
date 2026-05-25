package org.bibletranslationtools.writer.ui.devtools

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import btt_writer.shared.generated.resources.Res
import btt_writer.shared.generated.resources.label_close
import btt_writer.shared.generated.resources.label_details
import btt_writer.shared.generated.resources.log_details
import org.bibletranslationtools.logger.LogEntry
import org.bibletranslationtools.logger.LogLevel
import org.bibletranslationtools.writer.ui.dialogs.BaseDialog
import org.bibletranslationtools.writer.ui.dialogs.OverlayDialog
import org.jetbrains.compose.resources.stringResource

@Composable
fun ErrorLogDialog(
    logs: List<LogEntry>,
    onEmptyLog: () -> Unit,
    onDismiss: () -> Unit
) {
    var selectedLogDetails by remember { mutableStateOf<String?>(null) }

    OverlayDialog(
        onDismiss = onDismiss,
        maxWidth = 1000.dp,
        maxHeight = 1000.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Logs",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            LazyColumn(modifier = Modifier.weight(1f)) {
                items(logs) { log ->
                    LogItemRow(log = log) {
                        if (log.details.isNotEmpty()) {
                            selectedLogDetails = log.details
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onEmptyLog,
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text("Empty Log")
                }

                Button(onClick = onDismiss) {
                    Text("Dismiss")
                }
            }
        }
    }

    selectedLogDetails?.let { details ->
        BaseDialog(
            onDismiss = { selectedLogDetails = null },
            title = stringResource(Res.string.log_details),
            message = details
        ) {
            TextButton(onClick = { selectedLogDetails = null }) {
                Text(stringResource(Res.string.label_close))
            }
        }
    }
}

@Composable
fun LogItemRow(log: LogEntry, onClick: () -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = log.date.toString())

            Spacer(modifier = Modifier.weight(1f))

            if (log.details.isNotEmpty()) {
                Text(
                    text = stringResource(Res.string.label_details),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(),
                            onClick = onClick
                        )
                        .padding(horizontal = 4.dp)
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LevelIcon(log.level)

            Text(log.message)
        }
        Text(text = log.tag)
    }
}

@Composable
private fun LevelIcon(level: LogLevel) {
    when (level) {
        LogLevel.Info -> Icon(
            imageVector = Icons.Default.Info,
            contentDescription = "info"
        )
        LogLevel.Warning -> Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = "warning",
            tint = MaterialTheme.colorScheme.errorContainer
        )
        LogLevel.Error -> Icon(
            imageVector = Icons.Default.Error,
            contentDescription = "error",
            tint = MaterialTheme.colorScheme.error
        )
    }
}