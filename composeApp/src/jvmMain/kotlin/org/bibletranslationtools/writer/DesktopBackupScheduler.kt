package org.bibletranslationtools.writer

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.bibletranslationtools.logger.Logger
import org.bibletranslationtools.writer.core.BackupNotifier
import org.bibletranslationtools.writer.core.BackupRunner
import org.bibletranslationtools.writer.core.BackupScheduler

class DesktopBackupScheduler(
    private val backupRunner: BackupRunner,
    private val notifier: BackupNotifier? = null
) : BackupScheduler {
    private var job: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    override var isRunning: Boolean = false
        private set

    override fun start(intervalMinutes: Int) {
        if (isRunning) return
        if (intervalMinutes <= 0) {
            Logger.i(TAG, "Backups are disabled")
            return
        }

        isRunning = true
        val intervalMs = intervalMinutes * 60 * 1000L
        Logger.i(TAG, "Backups running every $intervalMinutes minute/s")

        job = scope.launch {
            while (isActive) {
                delay(intervalMs)
                try {
                    val performed = backupRunner.runBackup()
                    if (performed) {
                        notifier?.onBackupComplete()
                    }
                } catch (e: Exception) {
                    Logger.e(TAG, "Backup failed", e)
                }
            }
        }
    }

    override fun stop() {
        job?.cancel()
        job = null
        isRunning = false
        Logger.i(TAG, "Backups stopped")
    }

    companion object {
        private const val TAG = "DesktopBackupScheduler"
    }
}