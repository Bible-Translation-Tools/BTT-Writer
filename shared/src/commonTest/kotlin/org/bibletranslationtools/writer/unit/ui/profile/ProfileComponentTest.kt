package org.bibletranslationtools.writer.unit.ui.profile

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.bibletranslationtools.gogsclient.User
import org.bibletranslationtools.writer.Platform
import org.bibletranslationtools.writer.core.Profile
import org.bibletranslationtools.writer.ui.profile.DefaultProfileComponent
import org.bibletranslationtools.writer.ui.profile.ProfileComponent
import org.bibletranslationtools.writer.unit.ui.BaseComponentTest
import org.bibletranslationtools.writer.usecases.GogsLogin
import org.bibletranslationtools.writer.usecases.GogsLogout
import org.jetbrains.compose.resources.getString
import org.junit.Before
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.dsl.module
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ProfileComponentTest : BaseComponentTest() {

    private val profile: Profile = mockk(relaxed = true)
    private val gogsLogin: GogsLogin = mockk(relaxed = true)
    private val logoutUseCase: GogsLogout = mockk(relaxed = true)
    private val platform: Platform = mockk(relaxed = true)

    private var resultReceived: ProfileComponent.Result? = null

    @Before
    fun setUpComponent() {
        mockkStatic("org.jetbrains.compose.resources.StringResourcesKt")
        coEvery { getString(any()) } returns "10"

        startKoin {
            modules(
                module {
                    single { profile }
                    single { gogsLogin }
                    single { logoutUseCase }
                    single { platform }
                }
            )
        }
        resultReceived = null
    }

    private fun createComponent(goLogin: Boolean = false): DefaultProfileComponent =
        createComponent { context ->
            DefaultProfileComponent(
                componentContext = context,
                goLogin = goLogin,
                onResult = { resultReceived = it }
            )
        }

    @Test
    fun testInitializationLoggedInAndTermsAccepted() {
        every { profile.loggedIn } returns true
        every { profile.termsOfUseLastAccepted } returns 10

        createComponent()

        assertEquals(ProfileComponent.Result.LoggedIn, resultReceived)
    }

    @Test
    fun testInitializationNotLoggedIn() {
        every { profile.loggedIn } returns false

        val component = createComponent(goLogin = false)

        // Stack should start with only ProfileIndex
        val stack = component.stack.value
        assertEquals(1, stack.items.size)
        assertTrue(stack.active.instance is ProfileComponent.Child.Profile)
        assertEquals(null, resultReceived)
    }

    @Test
    fun testInitializationWithGoLogin() {
        every { profile.loggedIn } returns false

        val component = createComponent(goLogin = true)

        // Stack should contain Profile and LoginOnline
        val stack = component.stack.value
        assertEquals(2, stack.items.size)
        assertTrue(stack.active.instance is ProfileComponent.Child.LoginOnline)
    }

    @Test
    fun testProfileIndexNavigationOnline() {
        val component = createComponent(goLogin = false)
        val profileChild = component.stack.value.active.instance as ProfileComponent.Child.Profile
        
        // Trigger LoginOnline result from ProfileIndex
        profileChild.component.loginOnline()

        val activeChild = component.stack.value.active.instance
        assertTrue(activeChild is ProfileComponent.Child.LoginOnline)
    }

    @Test
    fun testProfileIndexNavigationOffline() {
        val component = createComponent(goLogin = false)
        val profileChild = component.stack.value.active.instance as ProfileComponent.Child.Profile
        
        // Trigger LoginOffline result from ProfileIndex
        profileChild.component.loginOffline()

        val activeChild = component.stack.value.active.instance
        assertTrue(activeChild is ProfileComponent.Child.LoginOffline)
    }

    @Test
    fun testProfileIndexSettings() {
        val component = createComponent(goLogin = false)
        val profileChild = component.stack.value.active.instance as ProfileComponent.Child.Profile
        
        profileChild.component.settings()

        assertEquals(ProfileComponent.Result.OpenSettings, resultReceived)
    }

    @Test
    fun testProfileIndexCancel() {
        val component = createComponent(goLogin = false)
        val profileChild = component.stack.value.active.instance as ProfileComponent.Child.Profile
        
        profileChild.component.cancel()

        assertEquals(ProfileComponent.Result.Back, resultReceived)
    }

    @Test
    fun testLoginOnlineBack() {
        val component = createComponent(goLogin = true)
        val loginOnlineChild = component.stack.value.active.instance as ProfileComponent.Child.LoginOnline
        
        loginOnlineChild.component.onCancel()

        // Should pop and go back to Profile
        assertEquals(1, component.stack.value.items.size)
        assertTrue(component.stack.value.active.instance is ProfileComponent.Child.Profile)
    }

    @Test
    fun testLoginOfflineBack() {
        val component = createComponent(goLogin = false)
        val profileChild = component.stack.value.active.instance as ProfileComponent.Child.Profile
        profileChild.component.loginOffline()
        
        val loginOfflineChild = component.stack.value.active.instance as ProfileComponent.Child.LoginOffline
        loginOfflineChild.component.onCancel()

        // Should pop and go back to Profile
        assertEquals(1, component.stack.value.items.size)
        assertTrue(component.stack.value.active.instance is ProfileComponent.Child.Profile)
    }

    @Test
    fun testLoginOnlineLoggedInNavigatesToTerms() {
        every { profile.loggedIn } returns false
        val mockUser = mockk<User>(relaxed = true) {
            every { username } returns "testuser"
            every { fullName } returns "Test User"
        }
        coEvery { gogsLogin.execute(any(), any(), any()) } returns GogsLogin.LoginResult(mockUser)

        val component = createComponent(goLogin = true)
        val loginOnlineChild = component.stack.value.active.instance as ProfileComponent.Child.LoginOnline

        loginOnlineChild.component.onLogin("testuser", "password")

        runBlocking {
            delay(300)
        }

        val activeChild = component.stack.value.active.instance
        assertTrue(activeChild is ProfileComponent.Child.TermsOfUse)
    }

    @Test
    fun testLoginOfflineLoggedInNavigatesToTerms() {
        every { profile.loggedIn } returns false

        val component = createComponent(goLogin = false)
        val profileChild = component.stack.value.active.instance as ProfileComponent.Child.Profile
        profileChild.component.loginOffline()

        val loginOfflineChild = component.stack.value.active.instance as ProfileComponent.Child.LoginOffline
        loginOfflineChild.component.onContinue("Test User")

        val activeChild = component.stack.value.active.instance
        assertTrue(activeChild is ProfileComponent.Child.TermsOfUse)
    }

    @Test
    fun testTermsOfUseAcceptedTriggersLoggedIn() {
        every { profile.loggedIn } returns false

        val component = createComponent(goLogin = false)
        val profileChild = component.stack.value.active.instance as ProfileComponent.Child.Profile
        profileChild.component.loginOffline()

        val loginOfflineChild = component.stack.value.active.instance as ProfileComponent.Child.LoginOffline
        loginOfflineChild.component.onContinue("Test User")

        val termsOfUseChild = component.stack.value.active.instance as ProfileComponent.Child.TermsOfUse
        termsOfUseChild.component.acceptTerms()

        runBlocking {
            delay(300)
        }

        assertEquals(ProfileComponent.Result.LoggedIn, resultReceived)
    }

    @Test
    fun testTermsOfUseRejectedClearsStackToProfile() {
        every { profile.loggedIn } returns false

        val component = createComponent(goLogin = false)
        val profileChild = component.stack.value.active.instance as ProfileComponent.Child.Profile
        profileChild.component.loginOffline()

        val loginOfflineChild = component.stack.value.active.instance as ProfileComponent.Child.LoginOffline
        loginOfflineChild.component.onContinue("Test User")

        val termsOfUseChild = component.stack.value.active.instance as ProfileComponent.Child.TermsOfUse
        termsOfUseChild.component.rejectTerms()

        runBlocking {
            delay(300)
        }

        val stack = component.stack.value
        assertEquals(1, stack.items.size)
        assertTrue(stack.active.instance is ProfileComponent.Child.Profile)
    }
}
