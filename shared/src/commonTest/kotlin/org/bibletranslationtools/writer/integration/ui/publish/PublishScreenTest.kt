package org.bibletranslationtools.writer.integration.ui.publish

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import btt_writer.shared.generated.resources.Res
import btt_writer.shared.generated.resources.menu_upload_export
import btt_writer.shared.generated.resources.publish_translation
import btt_writer.shared.generated.resources.title_book
import btt_writer.shared.generated.resources.translators
import org.bibletranslationtools.writer.integration.ui.ScreenTestBase
import org.bibletranslationtools.writer.ui.publish.PublishScreen
import org.bibletranslationtools.writer.utils.getStringBlocking
import org.junit.Test
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class PublishScreenTest : ScreenTestBase() {

    @Test
    fun title_and_tabs_displayed() = runComposeUiTest {
        val component = FakePublishComponent()
        setContent { PublishScreen(component) }

        onNodeWithText(getStringBlocking(Res.string.publish_translation), substring = true).assertIsDisplayed()
        onNodeWithText(getStringBlocking(Res.string.title_book), substring = true).assertIsDisplayed()
        onNodeWithText(getStringBlocking(Res.string.translators), substring = true).assertIsDisplayed()
        onNodeWithText(getStringBlocking(Res.string.menu_upload_export), substring = true).assertIsDisplayed()
    }

    @Test
    fun clicking_back_calls_navigateBack() = runComposeUiTest {
        val component = FakePublishComponent()
        setContent { PublishScreen(component) }

        onNodeWithContentDescription("back").performClick()
        assertTrue(component.navigateBackCalled)
    }

    @Test
    fun clicking_upload_export_calls_showExportDialog() = runComposeUiTest {
        val component = FakePublishComponent()
        setContent { PublishScreen(component) }

        onNodeWithText(getStringBlocking(Res.string.menu_upload_export), substring = true).performClick()
        assertTrue(component.showExportDialogCalled)
    }
}
