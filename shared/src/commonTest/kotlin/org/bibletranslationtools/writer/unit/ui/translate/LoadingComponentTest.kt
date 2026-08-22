package org.bibletranslationtools.writer.unit.ui.translate

import org.bibletranslationtools.writer.ui.translate.DefaultLoadingComponent
import org.junit.Test
import kotlin.test.assertNotNull

class LoadingComponentTest {

    @Test
    fun testLoadingComponentInstantiation() {
        val component = DefaultLoadingComponent()
        assertNotNull(component)
    }
}
