package org.bibletranslationtools.writer.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.slide
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import org.bibletranslationtools.writer.ui.newtranslation.NewTargetTranslationScreen
import org.bibletranslationtools.writer.ui.profile.ProfileRouter
import org.bibletranslationtools.writer.ui.publish.PublishScreen
import org.bibletranslationtools.writer.ui.settings.SettingsScreen
import org.bibletranslationtools.writer.ui.splash.SplashScreen
import org.bibletranslationtools.writer.ui.translate.TargetTranslationScreen
import org.bibletranslationtools.writer.ui.crash.CrashReporterScreen
import org.bibletranslationtools.writer.ui.devtools.DevToolsScreen
import org.bibletranslationtools.writer.ui.draft.DraftScreen
import org.bibletranslationtools.writer.ui.home.HomeScreen

@Composable
fun RootContent(
    component: RootComponent,
    modifier: Modifier = Modifier,
) {
    Children(
        stack = component.stack,
        modifier = modifier.fillMaxSize(),
        animation = stackAnimation(slide()),
    ) { child ->
        when (val instance = child.instance) {
            is RootComponent.Child.Splash -> SplashScreen(component = instance.component)
            is RootComponent.Child.Home -> HomeScreen(
                component = instance.component
            )
            is RootComponent.Child.NewTranslation -> NewTargetTranslationScreen(
                component = instance.component
            )
            is RootComponent.Child.Translate -> TargetTranslationScreen(
                component = instance.component
            )
            is RootComponent.Child.Profile -> ProfileRouter(
                component = instance.component
            )
            is RootComponent.Child.Settings -> SettingsScreen(
                component = instance.component
            )
            is RootComponent.Child.DevTools -> DevToolsScreen(
                component = instance.component
            )
            is RootComponent.Child.Draft -> DraftScreen(
                component = instance.component
            )
            is RootComponent.Child.Publish -> PublishScreen(
                component = instance.component
            )
            is RootComponent.Child.Crash -> CrashReporterScreen(
                component = instance.component
            )
        }
    }
}
