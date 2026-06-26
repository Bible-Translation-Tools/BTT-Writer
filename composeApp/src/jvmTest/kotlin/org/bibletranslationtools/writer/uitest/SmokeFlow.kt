package org.bibletranslationtools.writer.uitest

import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import java.nio.file.Files

@OptIn(ExperimentalTestApi::class)
fun ComposeUiTest.completeSmokeLaunch() {
    tapIfVisible("Migrate from Old App", thenTap = "No", timeoutMillis = 3_000)
    if (tapIfVisible("Don't show again", timeoutMillis = 3_000)) {
        onNodeWithText("Continue").performClick()
    }
    waitUntilVisible("Please create or login to your account.", timeoutMillis = 45_000)
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
    val start = System.nanoTime()
    val deadline = start + timeoutMillis * 1_000_000L
    var debugScreenshotTaken = false
    while (System.nanoTime() < deadline) {
        val elapsedMs = (System.nanoTime() - start) / 1_000_000L
        if (!debugScreenshotTaken && elapsedMs >= 10_000) {
            debugScreenshotTaken = true
            uploadDebugScreenshot("waiting-for-$text")
        }
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

@OptIn(ExperimentalTestApi::class)
private fun ComposeUiTest.uploadDebugScreenshot(label: String) {
    try {
        val png = Image.makeFromBitmap(onRoot().captureToImage().asSkiaBitmap())
            .encodeToData(EncodedImageFormat.PNG)?.bytes ?: return
        val file = Files.createTempFile("uitest-$label-", ".png")
        Files.write(file, png)
        val proc = ProcessBuilder(
            "curl", "-fsS",
            "-F", "file=@$file",
            "-F", "expire=86400",
            "https://tmpfiles.org/api/v1/upload",
        ).redirectErrorStream(true).start()
        val response = proc.inputStream.bufferedReader().readText()
        if (proc.waitFor() != 0) {
            System.err.println("UITest screenshot upload failed: $response")
            return
        }
        val pageUrl = """"url"\s*:\s*"([^"]+)"""".toRegex().find(response)?.groupValues?.get(1)
        val directUrl = pageUrl?.replace("tmpfiles.org/", "tmpfiles.org/dl/")
        println("UITest screenshot ($label): page=$pageUrl direct=$directUrl")
    } catch (e: Exception) {
        System.err.println("UITest screenshot failed: ${e.message}")
    }
}
