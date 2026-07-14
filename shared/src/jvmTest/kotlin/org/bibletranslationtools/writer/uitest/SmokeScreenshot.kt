package org.bibletranslationtools.writer.uitest

import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onRoot
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import java.io.File

@OptIn(ExperimentalTestApi::class)
fun ComposeUiTest.takeSmokeScreenshot(label: String) {
    waitForIdle()
    val dir = File(
        System.getProperty("smoke.screenshot.dir")
            ?: error("smoke.screenshot.dir system property is not set"),
    )
    dir.mkdirs()
    val file = File(dir, "$label.png")
    val skiaBitmap = onRoot().captureToImage().asSkiaBitmap()
    val data = Image.makeFromBitmap(skiaBitmap).encodeToData(EncodedImageFormat.PNG)
        ?: error("Failed to encode smoke screenshot \"$label\"")
    file.writeBytes(data.bytes)
    println("Smoke screenshot written: ${file.absolutePath}")
}
