package org.bibletranslationtools.writer

import android.content.Context
import android.content.Intent
import org.bibletranslationtools.writer.core.BackupScheduler

class AndroidBackupScheduler(
    private val context: Context,
) : BackupScheduler {
    override var isRunning: Boolean = false
        private set

    override fun start(intervalMinutes: Int) {
        val intent = Intent().apply {
            setClassName(
                context,
                "org.bibletranslationtools.writer.BackupService"
            )
            putExtra(EXTRA_INTERVAL, intervalMinutes)
        }
        context.startService(intent)
        isRunning = true
    }

    override fun stop() {
        val intent = Intent().apply {
            setClassName(
                context,
                "org.bibletranslationtools.writer.BackupService"
            )
        }
        context.stopService(intent)
        isRunning = false
    }

    companion object {
        const val EXTRA_INTERVAL = "backup_interval"
    }
}