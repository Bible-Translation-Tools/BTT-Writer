package org.bibletranslationtools.writer

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import org.bibletranslationtools.writer.di.initKoin

fun main() {

    initKoin()

    // TODO Decompose back handler
//    val backDispatcher = BackDispatcher()
//    val lifecycle = LifecycleRegistry()

    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "BTT-Writer",
        ) {
            App()
        }
    }
}