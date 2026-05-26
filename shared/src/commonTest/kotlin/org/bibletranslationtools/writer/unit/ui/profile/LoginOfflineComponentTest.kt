package org.bibletranslationtools.writer.unit.ui.profile

import io.mockk.mockk
import io.mockk.verify
import org.bibletranslationtools.writer.core.Profile
import org.bibletranslationtools.writer.ui.profile.DefaultLoginOfflineComponent
import org.bibletranslationtools.writer.ui.profile.LoginOfflineComponent
import org.bibletranslationtools.writer.unit.ui.BaseComponentTest
import org.junit.Before
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.dsl.module
import kotlin.test.assertEquals

class LoginOfflineComponentTest : BaseComponentTest() {

    private val profile: Profile = mockk(relaxed = true)
    private var resultReceived: LoginOfflineComponent.Result? = null

    @Before
    fun setUpComponent() {
        startKoin {
            modules(
                module {
                    single { profile }
                }
            )
        }
        resultReceived = null
    }

    private fun createComponent(): DefaultLoginOfflineComponent = createComponent { context ->
        DefaultLoginOfflineComponent(
            componentContext = context,
            onResult = { resultReceived = it }
        )
    }

    @Test
    fun testOnContinue() {
        val component = createComponent()
        component.onContinue("John Doe")

        verify { profile.login("John Doe") }
        assertEquals(LoginOfflineComponent.Result.LoggedIn, resultReceived)
    }

    @Test
    fun testOnCancel() {
        val component = createComponent()
        component.onCancel()

        assertEquals(LoginOfflineComponent.Result.Back, resultReceived)
    }
}
