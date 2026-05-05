package org.bibletranslationtools.writer

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.bibletranslationtools.logger.Logger
import org.bibletranslationtools.writer.android.R
import org.bibletranslationtools.writer.core.BackupRunner
import org.koin.android.ext.android.inject
import org.unfoldingword.tools.foreground.Foreground
import java.util.Timer
import java.util.TimerTask

class BackupService : Service(), Foreground.Listener {
    private val backupRunner: BackupRunner by inject()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val sTimer = Timer()
    private var isPaused = false
    private var foreground: Foreground? = null
    private lateinit var handler: Handler
    private var pendingRunnable: Runnable? = null
    private lateinit var handlerThread: HandlerThread

    override fun onBind(intent: Intent): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Logger.i(TAG, "starting backup service")
        handlerThread = HandlerThread("BackupServiceHandler").apply {
            start()
            handler = Handler(looper)
        }

        try {
            foreground = Foreground.get().apply {
                addListener(this@BackupService)
            }
        } catch (e: IllegalStateException) {
            Logger.i(TAG, "Foreground was not initialized")
        }
    }

    override fun onDestroy() {
        foreground?.removeListener(this)
        sTimer.cancel()
        scope.cancel()
        handlerThread.quitSafely()
        Logger.i(TAG, "stopping backup service")
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val intervalMinutes = intent?.getIntExtra(
            AndroidBackupScheduler.EXTRA_INTERVAL, 0
        ) ?: 0

        if (intervalMinutes > 0) {
            val intervalMs = intervalMinutes * 60 * 1000L
            Logger.i(TAG, "Backups running every $intervalMinutes minute/s")

            sTimer.schedule(object : TimerTask() {
                override fun run() {
                    if (!isPaused) {
                        scope.launch {
                            try {
                                val performed = backupRunner.runBackup()
                                if (performed) {
                                    withContext(Dispatchers.Main) {
                                        showBackupNotification()
                                    }
                                }
                            } catch (e: Exception) {
                                Logger.e(TAG, "Backup failed", e)
                            }
                        }
                    }
                }
            }, intervalMs, intervalMs)
        } else {
            Logger.i(TAG, "Backups are disabled")
        }

        return START_STICKY
    }

    private fun showBackupNotification() {
        val noticeText = "Translations backed up"

        val notificationIntent = Intent(
            applicationContext,
            MainActivity::class.java
        ).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = "backup_notification"
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(
            channelId,
            "Backup Notification",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        notificationManager.createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_stat_notify_msg)
            .setContentTitle(noticeText)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setStyle(
                NotificationCompat.InboxStyle().setBigContentTitle(noticeText)
            )
            .build()

        notificationManager.notify(0, notification)
    }

    override fun onBecameForeground() {
        isPaused = false
        Logger.i(TAG, "backups resumed")
        pendingRunnable?.let { handler.removeCallbacks(it) }
    }

    override fun onBecameBackground() {
        pendingRunnable?.let { handler.removeCallbacks(it) }
        Logger.i(TAG, "backups paused")
        isPaused = true
        handler.postDelayed(Runnable {
            Logger.i(TAG, "performing single run before pause")
            scope.launch {
                try {
                    val performed = backupRunner.runBackup()
                    if (performed) {
                        withContext(Dispatchers.Main) {
                            showBackupNotification()
                        }
                    }
                } catch (e: Exception) {
                    Logger.e(TAG, "Backup failed", e)
                }
            }
        }.also { pendingRunnable = it }, 1000)
    }

    companion object {
        private const val TAG = "BackupService"
    }
}