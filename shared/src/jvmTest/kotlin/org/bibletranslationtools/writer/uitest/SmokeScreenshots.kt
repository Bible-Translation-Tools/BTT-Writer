package org.bibletranslationtools.writer.uitest

import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onRoot
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import java.io.File

private val screenshotRoot: File by lazy {
    File(
        System.getProperty("btt.writer.smoke.screenshots.dir")
            ?: System.getenv("BTT_WRITER_SMOKE_SCREENSHOTS_DIR")
            ?: "build/smoke-screenshots",
    ).also { it.mkdirs() }
}

@OptIn(ExperimentalTestApi::class)
fun ComposeUiTest.takeSmokeScreenshot(name: String) {
    waitForIdle()
    val pngBytes = Image.makeFromBitmap(onRoot().captureToImage().asSkiaBitmap())
        .encodeToData(EncodedImageFormat.PNG)
        ?.bytes
        ?: error("Failed to encode screenshot as PNG: $name")
    val file = File(screenshotRoot, "$name.png")
    file.parentFile?.mkdirs()
    file.writeBytes(pngBytes)
}
