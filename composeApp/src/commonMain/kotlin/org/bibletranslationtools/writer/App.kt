package org.bibletranslationtools.writer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import btt_writer.composeapp.generated.resources.Res
import btt_writer.composeapp.generated.resources.compose_multiplatform
import org.bibletranslationtools.logger.LogLevel
import org.bibletranslationtools.logger.Logger
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject
import java.io.File

@Composable
fun App() {
    val directoryProvider: DirectoryProvider = koinInject()

    val dir = File(directoryProvider.externalAppDir, "crashes")
    println(dir)
    println(directoryProvider.logFile)

    Logger.configure(directoryProvider.logFile, LogLevel.Info)
    Logger.registerGlobalExceptionHandler(dir)

    println(Logger.getStacktraceDir())
    println(Logger.listStacktraces())
    println(Logger.getLogEntries())

//    Logger.i("App", "App is loaded")
//
//    try {
//        throw Exception("Warning test")
//    } catch (e: Exception) {
//        Logger.w("App", "Warning exception caught", e)
//    }
//
//    try {
//        throw Exception("Error test")
//    } catch (e: Exception) {
//        Logger.e("App", "Error exception caught", e)
//    }
//
//    throw Exception("Uncaught error exception")

    MaterialTheme {
        var showContent by remember { mutableStateOf(false) }
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.primaryContainer)
                .safeContentPadding()
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Button(onClick = { showContent = !showContent }) {
                Text("Click me!")
            }
            AnimatedVisibility(showContent) {
                val greeting = remember { Greeting(directoryProvider).greet() }
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Image(painterResource(Res.drawable.compose_multiplatform), null)
                    Text("Compose: $greeting")
                }
            }
        }
    }
}