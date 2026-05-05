package org.bibletranslationtools.writer

import org.bibletranslationtools.logger.Logger
import org.bibletranslationtools.writer.core.BackupNotifier

class DesktopBackupNotifier : BackupNotifier {
    override fun onBackupComplete() {
        Logger.i("DesktopBackup", "Translations backed up")
    }
}