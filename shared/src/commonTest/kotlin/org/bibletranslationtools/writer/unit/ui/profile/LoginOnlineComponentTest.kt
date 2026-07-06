package org.bibletranslationtools.writer.unit.ui.profile

import btt_writer.shared.generated.resources.Res
import btt_writer.shared.generated.resources.double_check_credentials
import btt_writer.shared.generated.resources.internet_not_available
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.bibletranslationtools.gogsclient.User
import org.bibletranslationtools.writer.Platform
import org.bibletranslationtools.writer.core.Profile
import org.bibletranslationtools.writer.ui.profile.DefaultLoginOnlineComponent
import org.bibletranslationtools.writer.ui.profile.LoginOnlineComponent
import org.bibletranslationtools.writer.unit.ui.BaseComponentTest
import org.bibletranslationtools.writer.unit.ui.awaitEvent
import org.bibletranslationtools.writer.usecases.GogsLogin
import org.junit.Before
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.dsl.module
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LoginOnlineComponentTest : BaseComponentTest() {

    private val profile: Profile = mockk(relaxed = true)
    private val gogsLogin: GogsLogin = mockk(relaxed = true)
    private val platform: Platform = mockk(relaxed = true)

    private var resultReceived: LoginOnlineComponent.Result? = null

    @Before
    fun setUpComponent() {
        startKoin {
            modules(
                module {
                    single { profile }
                    single { gogsLogin }
                    single { platform }
                }
            )
        }
        resultReceived = null
    }

    private fun createComponent(): DefaultLoginOnlineComponent = createComponent { context ->
        DefaultLoginOnlineComponent(
            componentContext = context,
            onResult = { resultReceived = it }
        )
    }

    @Test
    fun testOnCancel() {
        val component = createComponent()
        component.onCancel()

        assertEquals(LoginOnlineComponent.Result.Back, resultReceived)
    }

    @Test
    fun testIsNetworkAvailable() {
        every { platform.isNetworkAvailable } returns true
        val component = createComponent()
        assertTrue(component.isNetworkAvailable)
    }

    @Test
    fun testOnLoginSuccess() {
        runBlocking {
            val mockUser = mockk<User>(relaxed = true) {
                every { fullName } returns "Online User"
                every { username } returns "online_user"
            }
            coEvery { gogsLogin.execute("online_user", "password", any()) } returns GogsLogin.LoginResult(mockUser)

            val component = createComponent()
            component.onLogin("online_user", "password")

            // Allow flow and coroutine scope to process
            var count = 0
            while (resultReceived == null && count < 20) {
                kotlinx.coroutines.delay(50)
                count++
            }
            assertEquals(LoginOnlineComponent.Result.LoggedIn, resultReceived)
            verify { profile.login("Online User", mockUser) }
        }
    }

    @Test
    fun testOnLoginFailureNetworkAvailable() {
        runBlocking {
            coEvery { gogsLogin.execute(any(), any(), any()) } returns GogsLogin.LoginResult(null)
            every { platform.isNetworkAvailable } returns true

            val component = createComponent()
            component.onLogin("username", "password")

            val event = component.event.awaitEvent()
            assertTrue(event is LoginOnlineComponent.Event.ShowError)
            assertEquals(Res.string.double_check_credentials, event.errorResId)
        }
    }

    @Test
    fun testOnLoginFailureNetworkUnavailable() {
        runBlocking {
            coEvery { gogsLogin.execute(any(), any(), any()) } returns GogsLogin.LoginResult(null)
            every { platform.isNetworkAvailable } returns false

            val component = createComponent()
            component.onLogin("username", "password")

            val event = component.event.awaitEvent()
            assertTrue(event is LoginOnlineComponent.Event.ShowError)
            assertEquals(Res.string.internet_not_available, event.errorResId)
        }
    }
}
