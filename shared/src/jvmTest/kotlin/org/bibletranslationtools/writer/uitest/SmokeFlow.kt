package org.bibletranslationtools.writer.uitest

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp

@OptIn(ExperimentalTestApi::class)
fun ComposeUiTest.completeSmokeLaunch() {
    tapIfVisible("Migrate from Old App", thenTap = "No", timeoutMillis = 10_000)
    if (tapIfVisible("Don't show again", timeoutMillis = 3_000)) {
        onNodeWithText("Continue").performClick()
    }
    waitUntilVisible("Create offline Account", timeoutMillis = 45_000)
}

@OptIn(ExperimentalTestApi::class)
fun ComposeUiTest.completeSmokeSettings() {
    onNodeWithText("Create offline Account").assertIsDisplayed()
    onNodeWithText("Create offline Account").performClick()

    onNodeWithText("Your Name or Pseudonym").assertIsDisplayed()
    onNode(hasSetTextAction()).performTextInput("Test User")

    onNodeWithText("Continue").performClick()
    onNodeWithText("Privacy Notice").assertIsDisplayed()
    onAllNodesWithText("Continue")[1].performClick()

    waitUntilVisible("Terms of Use", timeoutMillis = 5_000)
    scrollUntilVisible("I Agree")
    onNodeWithText("I Agree").performClick()

    waitUntilVisible("Your Translation Projects", timeoutMillis = 3_000)
    takeSmokeScreenshot("smoke-settings/projects-home")

    onNodeWithContentDescription("More Options").performClick()
    onNodeWithText("Settings").performClick()

    onNodeWithText("General").assertIsDisplayed()
    takeSmokeScreenshot("smoke-settings/settings-general")
    onNodeWithContentDescription("back").performClick()
    waitUntilVisible("Your Translation Projects", timeoutMillis = 5_000)
    takeSmokeScreenshot("smoke-settings/projects-home-after-settings")
}

@OptIn(ExperimentalTestApi::class)
fun ComposeUiTest.completeSmokeNewTranslation() {
    waitUntilVisible("Start a new translation", timeoutMillis = 5_000)
    onNodeWithText("Start a new translation").performClick()

    waitUntilVisible("Choose Target", timeoutMillis = 10_000)
    scrollUntilVisible("aaa", timeoutMillis = 15_000)
    onNodeWithText("aaa").performClick()

    waitUntilVisible("bible-nt", timeoutMillis = 5_000)
    onNodeWithText("bible-nt").performClick()

    scrollUntilVisible("John")
    onNodeWithText("John").performClick()

    waitUntilVisible("Your Translation Projects", timeoutMillis = 15_000)
    waitUntilVisible("John", timeoutMillis = 15_000)
    onNodeWithText("John").performClick()

    waitUntilContentDescription("Edit Source", timeoutMillis = 15_000)
    onNodeWithContentDescription("Edit Source").performClick()

    waitUntilVisible("Choose source translations", timeoutMillis = 5_000)
    onNodeWithText("Choose source translations").performClick()
    onNode(hasSetTextAction()).performTextInput("English")

    waitUntilVisible("English (en) - Unlocked Literal Bible", timeoutMillis = 10_000)
    onNodeWithText("English (en) - Unlocked Literal Bible").performClick()
    onNodeWithText("Confirm").performClick()

    waitUntilContentDescription("Review Mode", timeoutMillis = 30_000)
}

@OptIn(ExperimentalTestApi::class)
fun ComposeUiTest.completeSmokeDraftChunk() {
    onNodeWithContentDescription("Review Mode").performClick()
    waitUntilContentDescription("toggle edit", timeoutMillis = 60_000)

    onAllNodesWithContentDescription("toggle edit")[0].performClick()
    waitUntilSetTextAction(timeoutMillis = 5_000)
    onNode(hasSetTextAction()).performTextReplacement("\\v 4 In him was life \\v 5 The light shine")
    onAllNodesWithContentDescription("toggle edit")[0].performClick()
    onAllNodesWithContentDescription("toggle done")[1].performClick()
    onNodeWithText("Confirm").performClick()

    onNodeWithContentDescription("More Options").performClick()
    onNodeWithText("Home").performClick()
}

@OptIn(ExperimentalTestApi::class)
fun ComposeUiTest.completeSmokeProjectMenu() {
    waitUntilVisible("Your Translation Projects", timeoutMillis = 5_000)

    onNodeWithContentDescription("Info").performClick()
    onNodeWithText("John - Ghotuo", substring = true).assertIsDisplayed()

    onNodeWithContentDescription("Upload").performClick()
    onNodeWithText("Upload/Export Options").assertIsDisplayed()
    onNodeWithText("Dismiss").performClick()
    waitUntilVisible("Your Translation Projects", timeoutMillis = 5_000)

    onNodeWithContentDescription("Info").performClick()
    onNodeWithText("John - Ghotuo", substring = true).assertIsDisplayed()
    onNodeWithContentDescription("Publish").performClick()
    onNodeWithText("Publish translation").assertIsDisplayed()
    onNodeWithContentDescription("back").performClick()
    waitUntilVisible("Your Translation Projects", timeoutMillis = 5_000)

    onNodeWithContentDescription("Info").performClick()
    onNodeWithText("John - Ghotuo", substring = true).assertIsDisplayed()
    onNodeWithContentDescription("Delete").performClick()
    onNodeWithText("Confirm").performClick()

    waitUntilVisible("Start a new translation", timeoutMillis = 10_000)
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
private fun ComposeUiTest.scrollUntilVisible(text: String, timeoutMillis: Long = 5_000) {
    val deadline = System.nanoTime() + timeoutMillis * 1_000_000L
    while (System.nanoTime() < deadline) {
        if (isTextVisible(text)) return
        try {
            onNode(hasScrollAction()).performTouchInput { swipeUp() }
        } catch (_: AssertionError) {
            // Review/lists may not expose scroll semantics yet; keep polling.
        }
        waitForIdle()
        mainClock.advanceTimeByFrame()
    }
    throw AssertionError("Timed out after ${timeoutMillis}ms waiting for \"$text\" while scrolling")
}

@OptIn(ExperimentalTestApi::class)
private fun ComposeUiTest.waitUntilVisible(text: String, timeoutMillis: Long) {
    val deadline = System.nanoTime() + timeoutMillis * 1_000_000L
    while (System.nanoTime() < deadline) {
        if (isTextVisible(text)) return
        waitForIdle()
        mainClock.advanceTimeByFrame()
    }
    throw AssertionError("Timed out after ${timeoutMillis}ms waiting for \"$text\"")
}

@OptIn(ExperimentalTestApi::class)
private fun ComposeUiTest.waitUntilSetTextAction(timeoutMillis: Long) {
    val deadline = System.nanoTime() + timeoutMillis * 1_000_000L
    while (System.nanoTime() < deadline) {
        if (try {
                onAllNodes(hasSetTextAction()).fetchSemanticsNodes().isNotEmpty()
            } catch (_: AssertionError) {
                false
            }
        ) {
            return
        }
        waitForIdle()
        mainClock.advanceTimeByFrame()
    }
    throw AssertionError("Timed out after ${timeoutMillis}ms waiting for editable text field")
}

@OptIn(ExperimentalTestApi::class)
private fun ComposeUiTest.waitUntilContentDescription(description: String, timeoutMillis: Long) {
    val deadline = System.nanoTime() + timeoutMillis * 1_000_000L
    while (System.nanoTime() < deadline) {
        if (isContentDescriptionVisible(description)) return
        waitForIdle()
        mainClock.advanceTimeByFrame()
    }
    throw AssertionError("Timed out after ${timeoutMillis}ms waiting for content description \"$description\"")
}

@OptIn(ExperimentalTestApi::class)
private fun ComposeUiTest.isContentDescriptionVisible(description: String): Boolean {
    return try {
        onAllNodesWithContentDescription(description).fetchSemanticsNodes().isNotEmpty()
    } catch (_: AssertionError) {
        false
    }
}

@OptIn(ExperimentalTestApi::class)
private fun ComposeUiTest.isTextVisible(text: String): Boolean {
    return try {
        onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty()
    } catch (_: AssertionError) {
        false
    }
}
