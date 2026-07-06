package org.bibletranslationtools.writer.integration.ui.splash

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import btt_writer.shared.generated.resources.Res
import btt_writer.shared.generated.resources.do_not_show_again
import btt_writer.shared.generated.resources.label_continue
import btt_writer.shared.generated.resources.no
import btt_writer.shared.generated.resources.welcome
import btt_writer.shared.generated.resources.yes
import org.bibletranslationtools.writer.core.Progress
import org.bibletranslationtools.writer.integration.ui.ScreenTestBase
import org.bibletranslationtools.writer.ui.splash.SplashComponent
import org.bibletranslationtools.writer.ui.splash.SplashScreen
import org.bibletranslationtools.writer.utils.getStringBlocking
import org.junit.Test
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class SplashScreenTest : ScreenTestBase() {

    @Test
    fun welcome_text_is_displayed() = runComposeUiTest {
        val component = FakeSplashComponent()
        setContent { SplashScreen(component) }
        onNodeWithText(getStringBlocking(Res.string.welcome), substring = true).assertIsDisplayed()
    }

    @Test
    fun loading_indicator_shown_when_progress_not_null() = runComposeUiTest {
        val component = FakeSplashComponent()
        component._progress.value = Progress(message = "Loading...", value = 0.5f)
        setContent { SplashScreen(component) }
        onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo)).assertIsDisplayed()
    }

    @Test
    fun migration_dialog_accept_calls_onMigrationAccepted() = runComposeUiTest {
        val component = FakeSplashComponent()
        component._state.value = SplashComponent.State(showMigrationDialog = true)
        setContent { SplashScreen(component) }
        onNodeWithText(getStringBlocking(Res.string.yes), substring = true).performClick()
        assertTrue(component.onMigrationAcceptedCalled)
    }

    @Test
    fun migration_dialog_decline_calls_onMigrationDeclined() = runComposeUiTest {
        val component = FakeSplashComponent()
        component._state.value = SplashComponent.State(showMigrationDialog = true)
        setContent { SplashScreen(component) }
        onNodeWithText(getStringBlocking(Res.string.no), substring = true).performClick()
        assertTrue(component.onMigrationDeclinedCalled)
    }

    @Test
    fun hardware_warning_do_not_show_again_calls_dismiss_and_saved() = runComposeUiTest {
        val component = FakeSplashComponent()
        component._state.value = SplashComponent.State(showHardwareWarning = true)
        setContent { SplashScreen(component) }

        onNodeWithText(getStringBlocking(Res.string.do_not_show_again), substring = true).performClick()

        assertTrue(component.onHardwareWarningDismissedAndSavedCalled)
    }

    @Test
    fun hardware_warning_continue_calls_on_hardware_warning_continued() = runComposeUiTest {
        val component = FakeSplashComponent()
        component._state.value = SplashComponent.State(showHardwareWarning = true)
        setContent { SplashScreen(component) }

        onNodeWithText(getStringBlocking(Res.string.label_continue), substring = true).performClick()

        assertTrue(component.onHardwareWarningContinuedCalled)
    }
}
