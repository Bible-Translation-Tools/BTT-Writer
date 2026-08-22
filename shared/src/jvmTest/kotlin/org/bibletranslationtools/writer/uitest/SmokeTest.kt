package org.bibletranslationtools.writer.uitest

import androidx.compose.ui.test.ExperimentalTestApi
import kotlin.test.Test

class SmokeTest {

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun smoke_full_flow() = runWriterUiTest {
        completeSmokeLaunch()
        completeSmokeSettings()
        completeSmokeNewTranslation()
        completeSmokeDraftChunk()
        completeSmokeProjectMenu()
    }
}
