package org.bibletranslationtools.writer.ui.profile

import androidx.compose.runtime.Composable
import btt_writer.composeapp.generated.resources.Res
import btt_writer.composeapp.generated.resources.pref_default_create_account_url
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.slide
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import org.bibletranslationtools.writer.data.Preference
import org.bibletranslationtools.writer.data.getPref
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

@Composable
fun ProfileRouter(component: ProfileComponent) {
    val preference: Preference = koinInject()

    val registerUrl = preference.getPref(
        Preference.KEY_PREF_CREATE_ACCOUNT_URL,
        stringResource(Res.string.pref_default_create_account_url)
    )

    Children(
        stack = component.stack,
        animation = stackAnimation(slide()),
    ) { child ->
        when (val instance = child.instance) {
            is ProfileComponent.Child.Profile -> ProfileIndexScreen(
                component = instance.component,
                registerUrl = registerUrl
            )
            is ProfileComponent.Child.LoginOnline -> LoginOnlineScreen(
                component = instance.component
            )
            is ProfileComponent.Child.LoginOffline -> LoginOfflineScreen(
                component = instance.component
            )
            is ProfileComponent.Child.TermsOfUse -> TermsOfUseScreen(
                component = instance.component
            )
        }
    }
}
