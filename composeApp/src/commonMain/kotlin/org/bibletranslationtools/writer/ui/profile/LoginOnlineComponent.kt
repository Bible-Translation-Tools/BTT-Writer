package org.bibletranslationtools.writer.ui.profile

import btt_writer.composeapp.generated.resources.Res
import btt_writer.composeapp.generated.resources.double_check_credentials
import btt_writer.composeapp.generated.resources.internet_not_available
import btt_writer.composeapp.generated.resources.logging_in
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.doOnDestroy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.withContext
import org.bibletranslationtools.writer.Platform
import org.bibletranslationtools.writer.core.ComponentScope
import org.bibletranslationtools.writer.core.Profile
import org.bibletranslationtools.writer.core.Progress
import org.bibletranslationtools.writer.core.ProgressManager
import org.bibletranslationtools.writer.core.ProgressOwner
import org.bibletranslationtools.writer.core.TaskHandle
import org.bibletranslationtools.writer.core.launchWithProgress
import org.bibletranslationtools.writer.usecases.GogsLogin
import org.jetbrains.compose.resources.StringResource
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

interface LoginOnlineComponent {
    val progress: StateFlow<Progress?>
    val event: Flow<Event>

    val isNetworkAvailable: Boolean

    fun onLogin(username: String, password: String)
    fun onCancel()

    sealed interface Event {
        data class ShowError(val errorResId: StringResource) : Event
    }

    sealed interface Result {
        data object Back : Result
        data object LoggedIn : Result
    }
}

class DefaultLoginOnlineComponent(
    componentContext: ComponentContext,
    private val onResult: (LoginOnlineComponent.Result) -> Unit,
) : LoginOnlineComponent,
    ComponentContext by componentContext,
    ComponentScope, ProgressOwner, KoinComponent {

    private val profile: Profile by inject()
    private val gogsLogin: GogsLogin by inject()
    private val platform: Platform by inject()

    private val _event = Channel<LoginOnlineComponent.Event>(Channel.BUFFERED)
    override val event = _event.receiveAsFlow()

    override val coroutineScope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())

    private val progressManager = ProgressManager(coroutineScope)
    override val progress get() = progressManager.progress

    override val isNetworkAvailable: Boolean
        get() = platform.isNetworkAvailable

    init {
        lifecycle.doOnDestroy {
            coroutineScope.cancel()
        }
    }

    override suspend fun runTask(message: String?, block: suspend (TaskHandle) -> Unit) {
        progressManager.runTask(message, block)
    }

    override fun onLogin(username: String, password: String) {
        launchWithProgress(Res.string.logging_in) {
            val loginResult = withContext(Dispatchers.IO) {
                gogsLogin.execute(
                    username.trim(),
                    password,
                    profile.fullName.takeIf { it.isNotEmpty() }
                )
            }
            var user = loginResult.user
            if (user != null) {
                if (user.fullName.isNullOrEmpty()) {
                    user = user.copy(fullName = user.username)
                }
                profile.login(user.fullName!!, user)
                onResult(LoginOnlineComponent.Result.LoggedIn)
            } else {
                val errorRes = if (platform.isNetworkAvailable) {
                    Res.string.double_check_credentials
                } else {
                    Res.string.internet_not_available
                }
                _event.trySend(LoginOnlineComponent.Event.ShowError(errorRes))
            }
        }
    }

    override fun onCancel() {
        onResult(LoginOnlineComponent.Result.Back)
    }
}
