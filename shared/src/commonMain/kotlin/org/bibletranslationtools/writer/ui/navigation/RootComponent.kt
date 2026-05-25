package org.bibletranslationtools.writer.ui.navigation

import btt_writer.shared.generated.resources.Res
import btt_writer.shared.generated.resources.pref_default_color_theme
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.backStack
import com.arkivanov.decompose.router.stack.bringToFront
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.replaceAll
import com.arkivanov.decompose.router.stack.replaceCurrent
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.lifecycle.doOnDestroy
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.bibletranslationtools.writer.DirectoryProvider
import org.bibletranslationtools.writer.Platform
import org.bibletranslationtools.writer.core.ComponentScope
import org.bibletranslationtools.writer.data.Preference
import org.bibletranslationtools.writer.data.getPref
import org.bibletranslationtools.writer.ui.crash.CrashComponent
import org.bibletranslationtools.writer.ui.crash.DefaultCrashComponent
import org.bibletranslationtools.writer.ui.devtools.DefaultDevToolsComponent
import org.bibletranslationtools.writer.ui.devtools.DevToolsComponent
import org.bibletranslationtools.writer.ui.draft.DefaultDraftComponent
import org.bibletranslationtools.writer.ui.draft.DraftComponent
import org.bibletranslationtools.writer.ui.home.DefaultHomeComponent
import org.bibletranslationtools.writer.ui.home.HomeComponent
import org.bibletranslationtools.writer.ui.navigation.RootComponent.Config
import org.bibletranslationtools.writer.ui.newtranslation.DefaultNewTranslationComponent
import org.bibletranslationtools.writer.ui.newtranslation.NewTranslationComponent
import org.bibletranslationtools.writer.ui.profile.DefaultProfileComponent
import org.bibletranslationtools.writer.ui.profile.ProfileComponent
import org.bibletranslationtools.writer.ui.publish.DefaultPublishComponent
import org.bibletranslationtools.writer.ui.publish.PublishComponent
import org.bibletranslationtools.writer.ui.settings.DefaultSettingsComponent
import org.bibletranslationtools.writer.ui.settings.SettingsComponent
import org.bibletranslationtools.writer.ui.splash.DefaultSplashComponent
import org.bibletranslationtools.writer.ui.splash.SplashComponent
import org.bibletranslationtools.writer.ui.translate.DefaultTranslateComponent
import org.bibletranslationtools.writer.ui.translate.TranslateComponent
import org.jetbrains.compose.resources.getString
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

interface RootComponent {

    val stack: Value<ChildStack<*, Child>>
    val sharedFlow: SharedFlow<SharedEvent>

    fun onBackPressed()
    fun onDeepLink(file: PlatformFile)

    fun openTranslate(translationId: String, startWithMergeFilter: Boolean)
    fun openProfile(thenLogin: Boolean)

    sealed interface Child {
        data class Splash(val component: SplashComponent) : Child
        data class Home(val component: HomeComponent) : Child
        data class Translate(val component: TranslateComponent) : Child
        data class Profile(val component: ProfileComponent) : Child
        data class Settings(val component: SettingsComponent) : Child
        data class DevTools(val component: DevToolsComponent) : Child
        data class NewTranslation(val component: NewTranslationComponent) : Child
        data class Draft(val component: DraftComponent) : Child
        data class Publish(val component: PublishComponent) : Child
        data class Crash(val component: CrashComponent) : Child
    }

    @Serializable
    sealed interface Config {
        @Serializable
        data object Splash : Config

        @Serializable
        data class Home(val importUri: String? = null) : Config

        @Serializable
        data class Translate(
            val translationId: String,
            val conflictFilterOn: Boolean = false,
        ) : Config

        @Serializable
        data class NewTranslation(
            val translationId: String? = null,
            val disabledLanguages: List<String> = emptyList()
        ) : Config

        @Serializable
        data class Publish(val translationId: String) : Config

        @Serializable
        data class Draft(val translationId: String) : Config

        @Serializable
        data object Settings : Config

        @Serializable
        data object DevTools : Config

        @Serializable
        data class Profile(val thenLogin: Boolean = false) : Config

        @Serializable
        data object Crash : Config
    }

    sealed interface SharedEvent {
        data object LoadProjects : SharedEvent
        data class SnackbarMessage(val message: String) : SharedEvent
        data class DuplicateProject(val translationId: String) : SharedEvent
        data object RequestLibraryUpdate : SharedEvent
        data class ImportProject(val file: PlatformFile) : SharedEvent
    }
}

class DefaultRootComponent(
    componentContext: ComponentContext,
    private val onExitApp: () -> Unit
) : RootComponent, ComponentContext by componentContext,
    KoinComponent, ComponentScope {

    private val preference: Preference by inject()
    private val platform: Platform by inject()
    private val directoryProvider: DirectoryProvider by inject()

    private val navigation = StackNavigation<Config>()

    private val _sharedFlow = MutableSharedFlow<RootComponent.SharedEvent>(extraBufferCapacity = 1)
    override val sharedFlow = _sharedFlow.asSharedFlow()

    private val _currentTheme = MutableStateFlow("")

    override val coroutineScope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())

    override val stack: Value<ChildStack<*, RootComponent.Child>> = childStack(
        source = navigation,
        serializer = Config.serializer(),
        initialConfiguration = Config.Splash,
        handleBackButton = true,
        childFactory = ::child,
    )

    init {
        coroutineScope.launch {
            val theme = preference.getPref(
                Preference.KEY_PREF_COLOR_THEME,
                getString(Res.string.pref_default_color_theme)
            )
            _currentTheme.value = theme
        }

        lifecycle.doOnDestroy {
            coroutineScope.cancel()
        }
    }

    override fun onBackPressed() {
        navigation.pop()
    }

    override fun onDeepLink(file: PlatformFile) {
        _sharedFlow.tryEmit(RootComponent.SharedEvent.ImportProject(file))
    }

    private fun child(
        config: Config,
        componentContext: ComponentContext,
    ): RootComponent.Child = when (config) {
        is Config.Splash -> RootComponent.Child.Splash(
            component = DefaultSplashComponent(
                componentContext = componentContext,
                onResult = ::onSplashResult,
            )
        )
        is Config.Home -> RootComponent.Child.Home(
            component = DefaultHomeComponent(
                componentContext = componentContext,
                sharedFlow = sharedFlow,
                onResult = ::onHomeResult
            )
        )
        is Config.NewTranslation -> RootComponent.Child.NewTranslation(
            component = DefaultNewTranslationComponent(
                componentContext = componentContext,
                disabledLanguages = config.disabledLanguages,
                translationId = config.translationId,
                onResult = ::onNewTranslationResult
            )
        )
        is Config.Translate -> RootComponent.Child.Translate(
            component = DefaultTranslateComponent(
                componentContext = componentContext,
                translationId = config.translationId,
                initialViewMode = null,
                conflictFilterOn = config.conflictFilterOn,
                sharedFlow = sharedFlow,
                onResult = ::onTranslateResult
            )
        )
        is Config.Profile -> RootComponent.Child.Profile(
            component = DefaultProfileComponent(
                componentContext = componentContext,
                goLogin = config.thenLogin,
                onResult = ::onProfileResult,
            )
        )
        is Config.Settings -> RootComponent.Child.Settings(
            component = DefaultSettingsComponent(
                componentContext = componentContext,
                onResult = ::onSettingsResult
            )
        )
        is Config.DevTools -> RootComponent.Child.DevTools(
            component = DefaultDevToolsComponent(
                componentContext = componentContext,
                onResult = ::onDevToolsResult
            )
        )
        is Config.Draft -> RootComponent.Child.Draft(
            component = DefaultDraftComponent(
                componentContext = componentContext,
                translationId = config.translationId,
                onResult = ::onDraftResult
            )
        )
        is Config.Publish -> RootComponent.Child.Publish(
            component = DefaultPublishComponent(
                componentContext = componentContext,
                translationId = config.translationId,
                onResult = ::onPublishResult
            )
        )
        is Config.Crash -> RootComponent.Child.Crash(
            component = DefaultCrashComponent(
                componentContext = componentContext,
                onResult = ::onCrashResult
            )
        )
    }

    private fun onSplashResult(result: SplashComponent.Result) {
        when (result) {
            SplashComponent.Result.NavigateToProfile -> openProfile(false)
            SplashComponent.Result.NavigateToCrashReporter -> openCrashReporter()
        }
    }

    private fun onHomeResult(result: HomeComponent.Result) {
        when (result) {
            is HomeComponent.Result.OpenLogin -> openProfile(true)
            is HomeComponent.Result.Logout -> openProfile(false)
            is HomeComponent.Result.OpenSettings -> openSettings()
            is HomeComponent.Result.PublishProject -> openPublishPreview(result.translationId)
            is HomeComponent.Result.OpenProject -> {
                openTranslate(result.translationId, result.mergeConflictFilterOn)
            }
            is HomeComponent.Result.ExitApp -> { onExitApp() }
            is HomeComponent.Result.OpenNewTranslation -> openNewTranslation()
            is HomeComponent.Result.ChangeTranslationLanguage -> {
                openNewTranslation(result.disabledLanguages, result.translationId)
            }
        }
    }

    private fun onNewTranslationResult(result: NewTranslationComponent.Result) {
        when (result) {
            is NewTranslationComponent.Result.NavigateBack -> navigation.pop()
            is NewTranslationComponent.Result.Success -> {
                navigation.pop {
                    _sharedFlow.tryEmit(RootComponent.SharedEvent.LoadProjects)
                }
            }
            is NewTranslationComponent.Result.Error -> {
                navigation.pop {
                    _sharedFlow.tryEmit(RootComponent.SharedEvent.SnackbarMessage(result.text))
                }
            }
            is NewTranslationComponent.Result.Duplicate -> {
                navigation.pop {
                    _sharedFlow.tryEmit(RootComponent.SharedEvent.DuplicateProject(result.translationId))
                }
            }
            is NewTranslationComponent.Result.MergeConflict -> {
                _sharedFlow.tryEmit(RootComponent.SharedEvent.LoadProjects)
                navigation.pop {
                    openTranslate(result.translationId, true)
                }
            }
        }
    }

    private fun onTranslateResult(result: TranslateComponent.Result) {
        when (result) {
            is TranslateComponent.Result.OpenHome -> openHome(result.withUpdate)
            is TranslateComponent.Result.OpenDraft -> openDraft(result.translationId)
            is TranslateComponent.Result.OpenPublishProject -> openPublishPreview(result.translationId)
            is TranslateComponent.Result.OpenLogin -> openProfile(true)
            is TranslateComponent.Result.Logout -> openProfile(false)
            is TranslateComponent.Result.OpenSettings -> openSettings()
            is TranslateComponent.Result.Error -> exitAndShowError(result.message)
        }
    }

    private fun onProfileResult(result: ProfileComponent.Result) {
        when (result) {
            is ProfileComponent.Result.Back -> {
                if (stack.value.backStack.isEmpty()) {
                    onExitApp()
                } else {
                    navigation.pop()
                }
            }
            is ProfileComponent.Result.OpenSettings -> openSettings()
            is ProfileComponent.Result.LoggedIn -> openHome()
        }
    }

    private fun onSettingsResult(result: SettingsComponent.Result) {
        when (result) {
            is SettingsComponent.Result.NavigateBack -> navigation.pop()
            is SettingsComponent.Result.OpenDeveloperTools -> openDevTools()
            is SettingsComponent.Result.MigrationFinished -> restart()
            is SettingsComponent.Result.Logout -> openProfile(false)
            is SettingsComponent.Result.ThemeUpdated -> _currentTheme.value = result.theme
        }
    }

    private fun onDevToolsResult(result: DevToolsComponent.Result) {
        when (result) {
            is DevToolsComponent.Result.NavigateBack -> navigation.pop()
        }
    }

    private fun onDraftResult(result: DraftComponent.Result) {
        when (result) {
            is DraftComponent.Result.NavigateBack -> navigation.pop()
        }
    }

    private fun onPublishResult(result: PublishComponent.Result) {
        when (result) {
            is PublishComponent.Result.Error -> exitAndShowError(result.message)
            is PublishComponent.Result.OpenReview -> {
                val last = stack.backStack.lastOrNull()?.instance
                if (last is RootComponent.Child.Home) {
                    navigation.replaceCurrent(
                        Config.Translate(result.translationId, false)
                    )
                } else {
                    navigation.pop {
                        openTranslate(result.translationId, false)
                    }
                }
            }
            is PublishComponent.Result.Login -> openProfile(true)
            is PublishComponent.Result.Logout -> openProfile(false)
            is PublishComponent.Result.MergeConflict -> {
                openTranslate(result.translationId, true)
            }
            is PublishComponent.Result.NavigateBack -> navigation.pop()
        }
    }

    private fun onCrashResult(result: CrashComponent.Result) {
        when (result) {
            CrashComponent.Result.Restart -> restart()
            CrashComponent.Result.Exit -> platform.exit()
        }
    }

    override fun openTranslate(translationId: String, startWithMergeFilter: Boolean) {
        navigation.bringToFront(
            Config.Translate(
                translationId = translationId,
                conflictFilterOn = startWithMergeFilter,
            )
        )
    }

    override fun openProfile(thenLogin: Boolean) {
        navigation.replaceAll(Config.Profile(thenLogin))
    }

    private fun exitAndShowError(error: String) {
        navigation.pop {
            _sharedFlow.tryEmit(RootComponent.SharedEvent.SnackbarMessage(error))
        }
    }

    private fun openHome(withUpdate: Boolean = false) {
        navigation.replaceAll(Config.Home()) {
            if (withUpdate) {
                _sharedFlow.tryEmit(RootComponent.SharedEvent.RequestLibraryUpdate)
            }
        }
    }

    private fun openDraft(translationId: String) {
        navigation.pop {
            navigation.bringToFront(Config.Draft(translationId))
        }
    }

    private fun openPublishPreview(translationId: String) {
        navigation.bringToFront(Config.Publish(translationId))
    }

    private fun openSettings() {
        navigation.bringToFront(Config.Settings)
    }

    private fun openDevTools() {
        navigation.bringToFront(Config.DevTools)
    }

    private fun openNewTranslation(
        disabledLanguages: List<String> = emptyList(),
        translationId: String? = null
    ) {
        navigation.bringToFront(Config.NewTranslation(
            disabledLanguages = disabledLanguages,
            translationId = translationId
        ))
    }

    private fun openCrashReporter() {
        navigation.replaceAll(Config.Crash)
    }

    private fun restart() {
        navigation.replaceAll(Config.Splash)
    }
}
