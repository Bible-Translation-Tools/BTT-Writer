package org.bibletranslationtools.writer.unit.ui.profile

import btt_writer.shared.generated.resources.Res
import btt_writer.shared.generated.resources.terms_of_use_version
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.bibletranslationtools.writer.core.Profile
import org.bibletranslationtools.writer.ui.profile.DefaultTermsOfUseComponent
import org.bibletranslationtools.writer.ui.profile.TermsOfUseComponent
import org.bibletranslationtools.writer.unit.ui.BaseComponentTest
import org.bibletranslationtools.writer.usecases.GogsLogout
import org.jetbrains.compose.resources.getString
import org.junit.Before
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.dsl.module
import kotlin.test.assertEquals

class TermsOfUseComponentTest : BaseComponentTest() {

    private val profile: Profile = mockk(relaxed = true)
    private val logoutUseCase: GogsLogout = mockk(relaxed = true)

    private var resultReceived: TermsOfUseComponent.Result? = null

    @Before
    fun setUpComponent() {
        mockkStatic("org.jetbrains.compose.resources.StringResourcesKt")
        coEvery { getString(Res.string.terms_of_use_version) } returns "2"

        startKoin {
            modules(
                module {
                    single { profile }
                    single { logoutUseCase }
                }
            )
        }
        resultReceived = null
    }

    private fun createComponent(): DefaultTermsOfUseComponent = createComponent { context ->
        DefaultTermsOfUseComponent(
            componentContext = context,
            onResult = { resultReceived = it }
        )
    }

    @Test
    fun testAcceptTerms() {
        runBlocking {
            val component = createComponent()
            component.acceptTerms()

            // Wait for coroutine in component
            delay(100)

            verify { profile.termsOfUseLastAccepted = 2 }
            assertEquals(TermsOfUseComponent.Result.Accepted, resultReceived)
        }
    }

    @Test
    fun testRejectTerms() {
        runBlocking {
            val component = createComponent()
            component.rejectTerms()

            // Wait for coroutine in component
            delay(100)

            coVerify { logoutUseCase.execute() }
            verify { profile.logout() }
            assertEquals(TermsOfUseComponent.Result.Rejected, resultReceived)
        }
    }
}
