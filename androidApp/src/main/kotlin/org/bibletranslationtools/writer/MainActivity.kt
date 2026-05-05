package org.bibletranslationtools.writer

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.arkivanov.decompose.retainedComponent
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.init
import kotlinx.coroutines.launch
import org.bibletranslationtools.writer.core.BackupScheduler
import org.bibletranslationtools.writer.core.Typography
import org.bibletranslationtools.writer.data.Preference
import org.bibletranslationtools.writer.data.getPref
import org.bibletranslationtools.writer.ui.navigation.DefaultRootComponent
import org.bibletranslationtools.writer.ui.navigation.RootComponent
import org.bibletranslationtools.writer.ui.navigation.RootContent
import org.koin.android.ext.android.get
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {

    private val platform: Platform by inject()
    private val preference: Preference by inject()
    private val directoryProvider: DirectoryProvider by inject()
    private val backupScheduler: BackupScheduler by inject()

    private lateinit var root: RootComponent

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        platform.initLogger(preference, directoryProvider)

        FileKit.init(this)
        startBackupService()
        handleIntent(intent)

        lifecycleScope.launch {
            get<Typography>().init()
        }

        root = retainedComponent { componentContext ->
            DefaultRootComponent(
                componentContext = componentContext,
                onExitApp = ::finishAffinity
            )
        }

        setContent {
            AppTheme {
                RootContent(component = root)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        intent ?: return
        intent.data?.let { root.onDeepLink(PlatformFile(it)) }
    }

    private fun startBackupService() {
        val interval = preference.getPref(
            Preference.KEY_PREF_BACKUP_INTERVAL,
            "5"
        ).toInt()
        backupScheduler.start(interval)
    }
}
