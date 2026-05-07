package org.bibletranslationtools.writer.ui.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import btt_writer.composeapp.generated.resources.Res
import btt_writer.composeapp.generated.resources.do_not_show_again
import btt_writer.composeapp.generated.resources.label_continue
import btt_writer.composeapp.generated.resources.loading
import btt_writer.composeapp.generated.resources.migrate_from_old_app
import btt_writer.composeapp.generated.resources.migrate_from_old_app_description
import btt_writer.composeapp.generated.resources.min_hardware_req_not_met
import btt_writer.composeapp.generated.resources.no
import btt_writer.composeapp.generated.resources.slow_device
import btt_writer.composeapp.generated.resources.welcome
import btt_writer.composeapp.generated.resources.yes
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.FileKitDialogSettings
import io.github.vinceglb.filekit.dialogs.compose.rememberDirectoryPickerLauncher
import org.bibletranslationtools.writer.ui.dialogs.BaseDialog
import org.jetbrains.compose.resources.stringResource

@Composable
fun SplashScreen(
    component: SplashComponent,
) {
    val state by component.state.collectAsStateWithLifecycle()
    val progress by component.progress.collectAsStateWithLifecycle()

    val openDirToMigrateLauncher = rememberDirectoryPickerLauncher(
        directory = PlatformFile("Downloads"),
        dialogSettings = FileKitDialogSettings.createDefault()
    ) { directory: PlatformFile? ->
        component.performMigrate(directory)
    }

    LaunchedEffect(component) {
        component.event.collect { event ->
            when (event) {
                is SplashComponent.Event.OpenDirToMigrate -> {
                    openDirToMigrateLauncher.launch()
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(Res.string.welcome),
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.displaySmall
        )

        Spacer(modifier = Modifier.height(32.dp))

        if (progress == null || progress!!.value < 0) {
            LinearProgressIndicator(
                gapSize = 0.dp,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            LinearProgressIndicator(
                progress = { progress!!.value },
                gapSize = 0.dp,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = progress?.message ?: stringResource(Res.string.loading),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 2,
            style = MaterialTheme.typography.bodyLarge
        )
    }

    if (state.showHardwareWarning) {
        BaseDialog(
            onDismiss = { /* Cannot cancel */ },
            title = stringResource(Res.string.slow_device),
            message = stringResource(Res.string.min_hardware_req_not_met),
        ) {
            TextButton(onClick = component::onHardwareWarningDismissedAndSaved) {
                Text(stringResource(Res.string.do_not_show_again))
            }
            TextButton(onClick = component::onHardwareWarningContinued) {
                Text(stringResource(Res.string.label_continue))
            }
        }
    }

    if (state.showMigrationDialog) {
        BaseDialog(
            onDismiss = { /* Cannot cancel */ },
            title = stringResource(Res.string.migrate_from_old_app),
            message = stringResource(Res.string.migrate_from_old_app_description)
        ) {
            Row {
                TextButton(onClick = component::onMigrationDeclined) {
                    Text(stringResource(Res.string.no))
                }
                TextButton(onClick = component::onMigrationAccepted) {
                    Text(stringResource(Res.string.yes))
                }
            }
        }
    }
}
