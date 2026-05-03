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
import org.bibletranslationtools.writer.core.Typography
import org.bibletranslationtools.writer.ui.navigation.DefaultRootComponent
import org.bibletranslationtools.writer.ui.navigation.RootComponent
import org.bibletranslationtools.writer.ui.navigation.RootContent
import org.koin.android.ext.android.get

class MainActivity : ComponentActivity() {

    private lateinit var root: RootComponent

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

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
//        val backupIntent = Intent(baseContext, BackupService::class.java)
//        baseContext.startService(backupIntent)
    }
}
