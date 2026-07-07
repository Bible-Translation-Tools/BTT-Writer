package org.bibletranslationtools.writer.uitest

import androidx.compose.ui.test.ExperimentalTestApi
import kotlin.test.Test

class SmokeProfileTest {

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun smoke_profile_to_settings() = runWriterUiTest {
        completeSmokeLaunch()
        completeSmokeProfileToSettings()
    }
}
