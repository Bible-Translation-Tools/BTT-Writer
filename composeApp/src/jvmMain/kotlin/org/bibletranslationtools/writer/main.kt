package org.bibletranslationtools.writer

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import btt_writer.composeapp.generated.resources.Res
import btt_writer.composeapp.generated.resources.app_icon
import btt_writer.composeapp.generated.resources.app_name
import btt_writer.composeapp.generated.resources.pref_default_backup_interval
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.decompose.extensions.compose.lifecycle.LifecycleController
import com.arkivanov.essenty.backhandler.BackDispatcher
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.bibletranslationtools.writer.core.BackupScheduler
import org.bibletranslationtools.writer.core.Typography
import org.bibletranslationtools.writer.data.Preference
import org.bibletranslationtools.writer.data.getPref
import org.bibletranslationtools.writer.di.initKoin
import org.bibletranslationtools.writer.ui.navigation.DefaultRootComponent
import org.bibletranslationtools.writer.ui.navigation.RootContent
import org.bibletranslationtools.writer.utils.getStringBlocking
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.core.context.GlobalContext
import org.koin.mp.KoinPlatform

fun main() {

    initKoin()

    val platform: Platform = KoinPlatform.getKoin().get()
    val preference: Preference = KoinPlatform.getKoin().get()
    val directoryProvider: DirectoryProvider = KoinPlatform.getKoin().get()
    val backupScheduler: BackupScheduler = KoinPlatform.getKoin().get()

    platform.initLogger(preference, directoryProvider)

    fun startBackupService() {
        val interval = preference.getPref(
            Preference.KEY_PREF_BACKUP_INTERVAL,
            getStringBlocking(Res.string.pref_default_backup_interval)
        ).toInt()

        backupScheduler.start(interval)
    }

    startBackupService()

    CoroutineScope(Dispatchers.Default).launch {
        GlobalContext.get().get<Typography>().init()
    }

    val lifecycle = LifecycleRegistry()
    val backDispatcher = BackDispatcher()

    application {
        val windowState = rememberWindowState(size = DpSize(1280.dp, 800.dp))
        LifecycleController(lifecycle, windowState)

        val root = DefaultRootComponent(
            componentContext = DefaultComponentContext(
                lifecycle = lifecycle,
                backHandler = backDispatcher
            ),
            onExitApp = ::exitApplication
        )

        Window(
            onCloseRequest = ::exitApplication,
            title = stringResource(Res.string.app_name),
            icon = painterResource(Res.drawable.app_icon),
            state = windowState,
            onKeyEvent = { event ->
                if (event.key == Key.Escape && event.type == KeyEventType.KeyDown) {
                    backDispatcher.back()
                    true
                } else false
            }
        ) {
            AppTheme {
                RootContent(component = root)
            }
        }
    }
}
