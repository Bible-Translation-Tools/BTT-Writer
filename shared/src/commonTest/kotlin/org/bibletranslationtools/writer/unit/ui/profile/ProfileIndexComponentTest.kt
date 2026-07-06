package org.bibletranslationtools.writer.unit.ui.profile

import org.bibletranslationtools.writer.ui.profile.DefaultProfileIndexComponent
import org.bibletranslationtools.writer.ui.profile.ProfileIndexComponent
import org.bibletranslationtools.writer.unit.ui.BaseComponentTest
import org.junit.Test
import kotlin.test.assertEquals

class ProfileIndexComponentTest : BaseComponentTest() {

    private var resultReceived: ProfileIndexComponent.Result? = null

    private fun createComponent(): DefaultProfileIndexComponent = createComponent { context ->
        DefaultProfileIndexComponent(
            componentContext = context,
            onResult = { resultReceived = it }
        )
    }

    @Test
    fun testLoginOnline() {
        val component = createComponent()
        component.loginOnline()
        assertEquals(ProfileIndexComponent.Result.LoginOnline, resultReceived)
    }

    @Test
    fun testLoginOffline() {
        val component = createComponent()
        component.loginOffline()
        assertEquals(ProfileIndexComponent.Result.LoginOffline, resultReceived)
    }

    @Test
    fun testSettings() {
        val component = createComponent()
        component.settings()
        assertEquals(ProfileIndexComponent.Result.Settings, resultReceived)
    }

    @Test
    fun testCancel() {
        val component = createComponent()
        component.cancel()
        assertEquals(ProfileIndexComponent.Result.Cancel, resultReceived)
    }
}
