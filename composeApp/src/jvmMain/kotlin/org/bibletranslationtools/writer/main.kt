package org.bibletranslationtools.writer

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.bibletranslationtools.writer.core.Typography
import org.bibletranslationtools.writer.di.initKoin
import org.koin.core.context.GlobalContext

fun main() {

    AppConfig.init(
        versionName = "1.0.0",
        versionCode = 1,
        githubToken = ""
    )

    initKoin()

    CoroutineScope(Dispatchers.Default).launch {
        GlobalContext.get().get<Typography>().init()
    }

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