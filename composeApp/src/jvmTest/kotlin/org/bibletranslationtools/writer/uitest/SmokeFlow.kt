package org.bibletranslationtools.writer.uitest

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput

@OptIn(ExperimentalTestApi::class)
fun ComposeUiTest.completeSmokeLaunch() {
    tapIfVisible("Migrate from Old App", thenTap = "No", timeoutMillis = 3_000)
    if (tapIfVisible("Don't show again", timeoutMillis = 3_000)) {
        onNodeWithText("Continue").performClick()
    }
    waitUntilVisible("Create offline Account", timeoutMillis = 45_000)
}

@OptIn(ExperimentalTestApi::class)
fun ComposeUiTest.completeSmokeProfileToSettings() {
    onNodeWithText("Create offline Account").assertIsDisplayed()
    onNodeWithText("Create offline Account").performClick()

    onNodeWithText("Your Name or Pseudonym").assertIsDisplayed()
    onNode(hasSetTextAction()).performTextInput("Test User")

    onNodeWithText("Continue").performClick()
    onNodeWithText("Privacy Notice").assertIsDisplayed()
    onAllNodesWithText("Continue")[1].performClick()

    onNodeWithText("I Agree").performClick()

    onNodeWithText("Your Translation Projects").assertIsDisplayed()

    onNodeWithContentDescription("More Options").performClick()
    onNodeWithText("Settings").performClick()

    onNodeWithText("General").assertIsDisplayed()
}

@OptIn(ExperimentalTestApi::class)
private fun ComposeUiTest.tapIfVisible(
    visibleText: String,
    thenTap: String = visibleText,
    timeoutMillis: Long,
): Boolean {
    return try {
        waitUntilVisible(visibleText, timeoutMillis)
        onNodeWithText(thenTap).performClick()
        true
    } catch (_: AssertionError) {
        false
    }
}

@OptIn(ExperimentalTestApi::class)
private fun ComposeUiTest.waitUntilVisible(text: String, timeoutMillis: Long) {
    val deadline = System.nanoTime() + timeoutMillis * 1_000_000L
    while (System.nanoTime() < deadline) {
        val visible = try {
            onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        } catch (_: AssertionError) {
            false
        }
        if (visible) return
        waitForIdle()
        mainClock.advanceTimeByFrame()
    }
    throw AssertionError("Timed out after ${timeoutMillis}ms waiting for \"$text\"")
}
