package org.bibletranslationtools.writer.ui.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import btt_writer.composeapp.generated.resources.Res
import btt_writer.composeapp.generated.resources.door43_account_required
import btt_writer.composeapp.generated.resources.login_doo43
import btt_writer.composeapp.generated.resources.pref_default_create_account_url
import btt_writer.composeapp.generated.resources.register_door43
import btt_writer.composeapp.generated.resources.requires_internet
import btt_writer.composeapp.generated.resources.title_cancel
import btt_writer.composeapp.generated.resources.title_connect_door43
import com.door43.translationstudio.ui.profile.ProfileOptionCard
import org.bibletranslationtools.writer.data.Preference
import org.bibletranslationtools.writer.data.getPref
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

@Composable
fun LoginOnlineDialog(
    onLogin: () -> Unit,
    onDismiss: () -> Unit
) {
    val preference: Preference = koinInject()

    val defaultRegisterUrl = stringResource(Res.string.pref_default_create_account_url)

    Dialog(
        onDismissRequest = onDismiss
    ) {
        val scrollState = rememberScrollState()
        val uriHandler = LocalUriHandler.current

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState),
            color = MaterialTheme.colorScheme.background,
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = stringResource(Res.string.title_connect_door43),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = stringResource(Res.string.door43_account_required),
                    fontSize = 16.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                ProfileOptionCard(
                    title = stringResource(Res.string.login_doo43),
                    subtitle = stringResource(Res.string.requires_internet),
                    onClick = {
                        onLogin()
                        onDismiss()
                    }
                )

                ProfileOptionCard(
                    title = stringResource(Res.string.register_door43),
                    subtitle = stringResource(Res.string.requires_internet),
                    onClick = {
                        val registerUrl = preference.getPref(
                            Preference.KEY_PREF_CREATE_ACCOUNT_URL,
                            defaultRegisterUrl
                        )
                        uriHandler.openUri(registerUrl)
                        onDismiss()
                    }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(
                            text = stringResource(Res.string.title_cancel)
                        )
                    }
                }
            }
        }
    }
}