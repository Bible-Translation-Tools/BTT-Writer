package org.bibletranslationtools.writer.ui.profile

import btt_writer.shared.generated.resources.Res
import btt_writer.shared.generated.resources.log_out
import btt_writer.shared.generated.resources.terms_of_use_version
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.doOnDestroy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.bibletranslationtools.writer.core.ComponentScope
import org.bibletranslationtools.writer.core.Profile
import org.bibletranslationtools.writer.core.Progress
import org.bibletranslationtools.writer.core.ProgressManager
import org.bibletranslationtools.writer.core.ProgressOwner
import org.bibletranslationtools.writer.core.TaskHandle
import org.bibletranslationtools.writer.core.launchWithProgress
import org.bibletranslationtools.writer.usecases.GogsLogout
import org.jetbrains.compose.resources.getString
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

interface TermsOfUseComponent {

    val progress: StateFlow<Progress?>

    fun acceptTerms()
    fun rejectTerms()

    sealed interface Result {
        data object Rejected : Result
        data object Accepted : Result
    }
}

class DefaultTermsOfUseComponent(
    componentContext: ComponentContext,
    private val onResult: (TermsOfUseComponent.Result) -> Unit
) : TermsOfUseComponent,
    ComponentContext by componentContext,
    KoinComponent, ComponentScope, ProgressOwner {

    private val profile: Profile by inject()
    private val logoutUseCase: GogsLogout by inject()

    override val coroutineScope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())

    private val progressManager = ProgressManager(coroutineScope)
    override val progress get() = progressManager.progress

    init {
        lifecycle.doOnDestroy {
            coroutineScope.cancel()
        }
    }

    override suspend fun runTask(message: String?, block: suspend (TaskHandle) -> Unit) {
        progressManager.runTask(message, block)
    }

    override fun acceptTerms() {
        coroutineScope.launch {
            profile.termsOfUseLastAccepted = getString(Res.string.terms_of_use_version)
                .toInt()
            onResult(TermsOfUseComponent.Result.Accepted)
        }
    }

    override fun rejectTerms() {
        launchWithProgress(Res.string.log_out) {
            withContext(Dispatchers.IO) {
                logoutUseCase.execute()
                profile.logout()
            }
            onResult(TermsOfUseComponent.Result.Rejected)
        }
    }
}