package org.bibletranslationtools.writer.ui.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import btt_writer.composeapp.generated.resources.Res
import btt_writer.composeapp.generated.resources.action_settings
import btt_writer.composeapp.generated.resources.create_account_title
import btt_writer.composeapp.generated.resources.create_offline_profile
import btt_writer.composeapp.generated.resources.login_doo43
import btt_writer.composeapp.generated.resources.register_door43
import btt_writer.composeapp.generated.resources.requires_internet
import btt_writer.composeapp.generated.resources.still_possible_to_register_door43
import btt_writer.composeapp.generated.resources.title_cancel
import org.bibletranslationtools.writer.ui.components.HomeSidebar
import org.bibletranslationtools.writer.ui.components.SidebarAction
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileIndexScreen(
    component: ProfileIndexComponent,
    registerUrl: String
) {
    val uriHandler = LocalUriHandler.current

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
    ) { paddingValues ->
        Row(
            modifier = Modifier.fillMaxSize()
                .padding(paddingValues)
        ) {
            HomeSidebar(
                listOf(
                    SidebarAction(
                        title = stringResource(Res.string.action_settings),
                        icon = Icons.Default.Settings,
                        onClick = component::settings
                    )
                )
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = stringResource(Res.string.create_account_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 16.dp, start = 4.dp)
                )

                ProfileOptionCard(
                    title = stringResource(Res.string.login_doo43),
                    subtitle = stringResource(Res.string.requires_internet),
                    onClick = component::loginOnline
                )

                ProfileOptionCard(
                    title = stringResource(Res.string.register_door43),
                    subtitle = stringResource(Res.string.requires_internet),
                    onClick = {
                        uriHandler.openUri(registerUrl)
                    }
                )

                ProfileOptionCard(
                    title = stringResource(Res.string.create_offline_profile),
                    subtitle = stringResource(Res.string.still_possible_to_register_door43),
                    onClick = component::loginOffline
                )

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = component::cancel,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 24.dp)
                ) {
                    Text(stringResource(Res.string.title_cancel))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileOptionCard(title: String, subtitle: String, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title, 
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle, 
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}